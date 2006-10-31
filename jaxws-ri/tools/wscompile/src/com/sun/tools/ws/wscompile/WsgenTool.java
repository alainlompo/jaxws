/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

package com.sun.tools.ws.wscompile;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.tools.ws.ToolVersion;
import com.sun.tools.ws.processor.modeler.annotation.AnnotationProcessorContext;
import com.sun.tools.ws.processor.modeler.annotation.WebServiceAP;
import com.sun.tools.ws.processor.modeler.wsdl.ConsoleErrorReporter;
import com.sun.tools.ws.processor.util.ProcessorEnvironmentBase;
import com.sun.tools.ws.resources.WscompileMessages;
import com.sun.tools.xjc.util.NullStream;
import com.sun.xml.txw2.TXW;
import com.sun.xml.txw2.TypedXmlWriter;
import com.sun.xml.txw2.annotation.XmlAttribute;
import com.sun.xml.txw2.annotation.XmlElement;
import com.sun.xml.txw2.output.StreamSerializer;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.wsdl.writer.WSDLGeneratorExtension;
import com.sun.xml.ws.binding.WebServiceFeatureList;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.util.ServiceFinder;
import com.sun.xml.ws.wsdl.writer.WSDLGenerator;
import com.sun.xml.ws.wsdl.writer.WSDLResolver;
import org.xml.sax.SAXParseException;

import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Holder;
import java.io.*;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author Vivek Pandey
 */
public class WsgenTool implements AnnotationProcessorFactory {
    private final PrintStream out;
    private final WsgenOptions options = new WsgenOptions();


    public WsgenTool(OutputStream out, Container container) {
        this.out = (out instanceof PrintStream)?(PrintStream)out:new PrintStream(out);
        this.container = container;
    }


    public WsgenTool(OutputStream out) {
        this(out, null);
    }

    public boolean run(String[] args){
        final Listener listener = new Listener();
        for (String arg : args) {
            if (arg.equals("-version")) {
                listener.message(ToolVersion.VERSION.BUILD_VERSION);
                return true;
            }
        }
        try {
            options.parseArguments(args);
            options.validate();
            if(!buildModel(options.endpoint.getName(), listener)){
                return false;
            }
        }catch (Options.WeAreDone done){
            usage(done.getOptions());
        }catch (BadCommandLineException e) {
            if(e.getMessage()!=null) {
                System.out.println(e.getMessage());
                System.out.println();
            }
            usage(e.getOptions());
            return false;
        }finally{
            if(!options.keep){
                options.removeGeneratedFiles();
            }
        }
        return true;
    }

    private AnnotationProcessorContext context;
    private final Container container;

    private WebServiceAP webServiceAP;

    private int round = 0;

    public boolean buildModel(String endpoint, Listener listener) {
        final ErrorReceiverFilter errReceiver = new ErrorReceiverFilter(listener);
        context = new AnnotationProcessorContext();
        webServiceAP = new WebServiceAP(options, context, errReceiver, out);

        String[] args = new String[8];
        args[0] = "-d";
        args[1] = options.destDir.getAbsolutePath();
        args[2] = "-classpath";
        args[3] = options.classpath;
        args[4] = "-s";
        args[5] = options.sourceDir.getAbsolutePath();
        args[6] = "-XclassesAsDecls";
        args[7] = endpoint;

        int result = com.sun.tools.apt.Main.process(this, args);
        if (result != 0) {
            out.println(WscompileMessages.WSCOMPILE_ERROR(WscompileMessages.WSCOMPILE_COMPILATION_FAILED()));
            return false;
        }
        if (options.genWsdl) {
            String tmpPath = options.destDir.getAbsolutePath()+ File.pathSeparator+options.classpath;
            ClassLoader classLoader = new URLClassLoader(ProcessorEnvironmentBase.pathToURLs(tmpPath),
                    this.getClass().getClassLoader());
            Class<?> endpointClass = null;

            BindingID bindingID = WsgenOptions.getBindingID(options.protocol);
            if (!options.protocolSet) {
                bindingID = BindingID.parse(endpointClass);
            }
            RuntimeModeler rtModeler = new RuntimeModeler(endpointClass, options.serviceName, bindingID);
            rtModeler.setClassLoader(classLoader);
            if (options.portName != null)
                rtModeler.setPortName(options.portName);
            AbstractSEIModelImpl rtModel = rtModeler.buildRuntimeModel();

            final File[] wsdlFileName = new File[1]; // used to capture the generated WSDL file.
            final Map<String,File> schemaFiles = new HashMap<String,File>();
            WebServiceFeatureList wsfeatures = new WebServiceFeatureList(endpointClass);
            WSDLGenerator wsdlGenerator = new WSDLGenerator(rtModel,
                    new WSDLResolver() {
                        private File toFile(String suggestedFilename) {
                            return new File(options.nonclassDestDir, suggestedFilename);
                        }
                        private Result toResult(File file) {
                            Result result = new StreamResult();
                            try {
                                result = new StreamResult(new FileOutputStream(file));
                                result.setSystemId(file.getPath().replace('\\', '/'));
                            } catch (FileNotFoundException e) {
                                errReceiver.error(e);
                                return null;
                            }
                            return result;
                        }

                        public Result getWSDL(String suggestedFilename) {
                            File f = toFile(suggestedFilename);
                            wsdlFileName[0] = f;
                            return toResult(f);
                        }
                        public Result getSchemaOutput(String namespace, String suggestedFilename) {
                            if (namespace.equals(""))
                                return null;
                            File f = toFile(suggestedFilename);
                            schemaFiles.put(namespace,f);
                            return toResult(f);
                        }
                        public Result getAbstractWSDL(Holder<String> filename) {
                            return toResult(toFile(filename.value));
                        }
                        public Result getSchemaOutput(String namespace, Holder<String> filename) {
                            return getSchemaOutput(namespace, filename.value);
                        }
                        // TODO pass correct impl's class name
                    }, wsfeatures == null ? bindingID.createBinding() : bindingID.createBinding(wsfeatures.toArray()), container, endpointClass, ServiceFinder.find(WSDLGeneratorExtension.class).toArray());
            wsdlGenerator.doGeneration();

            if(options.wsgenReport!=null)
                generateWsgenReport(endpointClass,rtModel,wsdlFileName[0],schemaFiles);
        }
        return true;
    }

    /**
     * Generates a small XML file that captures the key activity of wsgen,
     * so that test harness can pick up artifacts.
     */
    private void generateWsgenReport(Class<?> endpointClass, AbstractSEIModelImpl rtModel, File wsdlFile, Map<String,File> schemaFiles) {
        try {
            ReportOutput.Report report = TXW.create(ReportOutput.Report.class,
                new StreamSerializer(new BufferedOutputStream(new FileOutputStream(options.wsgenReport))));

            report.wsdl(wsdlFile.getAbsolutePath());
            ReportOutput.writeQName(rtModel.getServiceQName(), report.service());
            ReportOutput.writeQName(rtModel.getPortName(), report.port());
            ReportOutput.writeQName(rtModel.getPortTypeName(), report.portType());

            report.implClass(endpointClass.getName());

            for (Map.Entry<String,File> e : schemaFiles.entrySet()) {
                ReportOutput.Schema s = report.schema();
                s.ns(e.getKey());
                s.location(e.getValue().getAbsolutePath());
            }

            report.commit();
        } catch (IOException e) {
            // this is code for the test, so we can be lousy in the error handling
            throw new Error(e);
        }
    }

    /**
     * "Namespace" for code needed to generate the report file.
     */
    static class ReportOutput {
        @XmlElement("report")
        interface Report extends TypedXmlWriter {
            @XmlElement
            void wsdl(String file); // location of WSDL
            @XmlElement
            QualifiedName portType();
            @XmlElement
            QualifiedName service();
            @XmlElement
            QualifiedName port();

            /**
             * Name of the class that has {@link javax.jws.WebService}.
             */
            @XmlElement
            void implClass(String name);

            @XmlElement
            Schema schema();
        }

        interface QualifiedName extends TypedXmlWriter {
            @XmlAttribute
            void uri(String ns);
            @XmlAttribute
            void localName(String localName);
        }

        interface Schema extends TypedXmlWriter {
            @XmlAttribute
            void ns(String ns);
            @XmlAttribute
            void location(String filePath);
        }

        private static void writeQName( QName n, QualifiedName w ) {
            w.uri(n.getNamespaceURI());
            w.localName(n.getLocalPart());
        }
    }

    protected void usage(Options options) {
        System.out.println(WscompileMessages.WSGEN_HELP("WSGEN"));
        System.out.println(WscompileMessages.WSGEN_USAGE_EXAMPLES());
    }

    public Collection<String> supportedOptions() {
        return supportedOptions;
    }

    public Collection<String> supportedAnnotationTypes() {
        return supportedAnnotations;
    }

    public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> set, AnnotationProcessorEnvironment apEnv) {
        if (options.verbose)
            apEnv.getMessager().printNotice("\tap round: " + ++round);
        webServiceAP.init(apEnv);
        return webServiceAP;
    }

    class Listener extends WsimportListener {
        ConsoleErrorReporter cer = new ConsoleErrorReporter(out == null ? new PrintStream(new NullStream()) : out);

        @Override
        public void generatedFile(String fileName) {
            message(fileName);
        }

        @Override
        public void message(String msg) {
            out.println(msg);
        }

        @Override
        public void error(SAXParseException exception) {
            cer.error(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) {
            cer.fatalError(exception);
        }

        @Override
        public void warning(SAXParseException exception) {
            cer.warning(exception);
        }

        @Override
        public void info(SAXParseException exception) {
            cer.info(exception);
        }
    }

    /*
     * Processor doesn't examine any options.
     */
    static final Collection<String> supportedOptions = Collections
            .unmodifiableSet(new HashSet<String>());

    /*
     * All annotation types are supported.
     */
    static final Collection<String> supportedAnnotations;
    static {
        Collection<String> types = new HashSet<String>();
        types.add("*");
        types.add("javax.jws.*");
        types.add("javax.jws.soap.*");
        supportedAnnotations = Collections.unmodifiableCollection(types);
    }
}
