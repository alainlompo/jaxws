/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.tools.ws.processor.generator;

import com.sun.codemodel.*;
import com.sun.tools.ws.api.TJavaGeneratorExtension;
import com.sun.tools.ws.processor.ProcessorAction;
import com.sun.tools.ws.processor.model.*;
import com.sun.tools.ws.processor.model.java.JavaInterface;
import com.sun.tools.ws.processor.model.java.JavaMethod;
import com.sun.tools.ws.processor.model.java.JavaParameter;
import com.sun.tools.ws.processor.model.jaxb.JAXBType;
import com.sun.tools.ws.processor.model.jaxb.JAXBTypeAndAnnotation;
import com.sun.tools.ws.wscompile.ErrorReceiver;
import com.sun.tools.ws.wscompile.Options;
import com.sun.tools.ws.wscompile.WsimportOptions;
import com.sun.tools.ws.wsdl.document.soap.SOAPStyle;
import com.sun.xml.ws.util.ServiceFinder;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SeiGenerator extends GeneratorBase implements ProcessorAction {
    private String serviceNS;
    private TJavaGeneratorExtension extension;
    List<TJavaGeneratorExtension> extensionHandlers;


    protected void doGeneration() {
        try {
            model.accept(this);
        } catch (Exception e) {
            if (options.verbose)
                e.printStackTrace();
            throw new GeneratorException(
                "generator.nestedGeneratorError",
                e);
        }
    }


    public  GeneratorBase getGenerator(Model model, WsimportOptions options, ErrorReceiver receiver) {
        return new SeiGenerator(model, options, receiver, ServiceFinder.find(TJavaGeneratorExtension.class).toArray());
    }

    public static final void generate(Model model, WsimportOptions options, ErrorReceiver receiver, TJavaGeneratorExtension... extensions){
        SeiGenerator seiGenerator = new SeiGenerator(model, options, receiver, extensions);
        seiGenerator.doGeneration();
    }

    protected SeiGenerator(Model model, WsimportOptions options, ErrorReceiver receiver, TJavaGeneratorExtension... extensions) {
        super(model, options, receiver);
        this.model = model;
        extensionHandlers = new ArrayList<TJavaGeneratorExtension>();

        // register handlers for default extensions
        register(new W3CAddressingJavaGeneratorExtension());
        
        for (TJavaGeneratorExtension j : extensions)
            register(j);

        this.extension = new JavaGeneratorExtensionFacade(extensionHandlers.toArray(new TJavaGeneratorExtension[0]));
    }

    private void write(Service service, Port port) throws Exception{
        JavaInterface intf = port.getJavaInterface();
        String className = Names.customJavaTypeClassName(intf);

        if (donotOverride && GeneratorUtil.classExists(options, className)) {
            log("Class " + className + " exists. Not overriding.");
            return;
        }


        JDefinedClass cls = getClass(className, ClassType.INTERFACE);
        if (cls == null)
            return;

        // If the class has methods it has already been defined
        // so skip it.
        if (!cls.methods().isEmpty())
            return;

        //write class comment - JAXWS warning
        JDocComment comment = cls.javadoc();

        String ptDoc = intf.getJavaDoc();
        if(ptDoc != null){
            comment.add(ptDoc);
            comment.add("\n\n");
        }

        for(String doc:getJAXWSClassComment()){
                comment.add(doc);
        }


        //@WebService
        JAnnotationUse webServiceAnn = cls.annotate(cm.ref(WebService.class));
        writeWebServiceAnnotation(port, webServiceAnn);

        //@HandlerChain
        writeHandlerConfig(Names.customJavaTypeClassName(port.getJavaInterface()), cls, options);

        //@SOAPBinding
        writeSOAPBinding(port, cls);

        //@XmlSeeAlso
        if(options.target.isLaterThan(Options.Target.V2_1))
            writeXmlSeeAlso(port, cls);

        for (Operation operation: port.getOperations()) {
            JavaMethod method = operation.getJavaMethod();

            //@WebMethod
            JMethod m = null;
            JDocComment methodDoc = null;
            String methodJavaDoc = operation.getJavaDoc();
            if(method.getReturnType().getName().equals("void")){
                m = cls.method(JMod.PUBLIC, void.class, method.getName());
                methodDoc = m.javadoc();
            }else {
                JAXBTypeAndAnnotation retType = method.getReturnType().getType();
                m = cls.method(JMod.PUBLIC, retType.getType(), method.getName());
                retType.annotate(m);
                methodDoc = m.javadoc();
                JCommentPart ret = methodDoc.addReturn();
                ret.add("returns "+retType.getName());
            }
            if(methodJavaDoc != null)
                methodDoc.add(methodJavaDoc);

            writeWebMethod(operation, m);
            JClass holder = cm.ref(Holder.class);
            for (JavaParameter parameter: method.getParametersList()) {
                JVar var = null;
                JAXBTypeAndAnnotation paramType = parameter.getType().getType();
                if (parameter.isHolder()) {
                    var = m.param(holder.narrow(paramType.getType().boxify()), parameter.getName());
                }else{
                    var = m.param(paramType.getType(), parameter.getName());
                }

                //annotate parameter with JAXB annotations
                paramType.annotate(var);
                methodDoc.addParam(var);
                JAnnotationUse paramAnn = var.annotate(cm.ref(WebParam.class));
                writeWebParam(operation, parameter, paramAnn);
            }
            com.sun.tools.ws.wsdl.document.Operation wsdlOp = operation.getWSDLPortTypeOperation();
            for(Fault fault:operation.getFaultsSet()){
                m._throws(fault.getExceptionClass());
                methodDoc.addThrows(fault.getExceptionClass());
                wsdlOp.putFault(fault.getWsdlFaultName(), fault.getExceptionClass());
            }

            //It should be the last thing to invoke after JMethod is built completely
            extension.writeMethodAnnotations(wsdlOp, m);
        }
    }

    private void writeXmlSeeAlso(Port port, JDefinedClass cls) {        
        if(model.getJAXBModel().getS2JJAXBModel() != null){
            JAnnotationUse xmlSeeAlso = cls.annotate(cm.ref(XmlSeeAlso.class));
            JAnnotationArrayMember paramArray = xmlSeeAlso.paramArray("value");
           for(JClass of:model.getJAXBModel().getS2JJAXBModel().getAllObjectFactories()){
               paramArray = paramArray.param(of);
           }
        }

    }

    private void writeWebMethod(Operation operation, JMethod m) {
        Response response = operation.getResponse();
        JAnnotationUse webMethodAnn = m.annotate(cm.ref(WebMethod.class));;
        String operationName = (operation instanceof AsyncOperation)?
                ((AsyncOperation)operation).getNormalOperation().getName().getLocalPart():
                operation.getName().getLocalPart();

        if(!m.name().equals(operationName)){
            webMethodAnn.param("operationName", operationName);
        }

        if (operation.getSOAPAction() != null && operation.getSOAPAction().length() > 0){
            webMethodAnn.param("action", operation.getSOAPAction());
        }

        if (operation.getResponse() == null){
            m.annotate(javax.jws.Oneway.class);
        }else if (!operation.getJavaMethod().getReturnType().getName().equals("void") &&
                 operation.getResponse().getParametersList().size() > 0){
            Block block = null;
            String resultName = null;
            String nsURI = null;
            if (operation.getResponse().getBodyBlocks().hasNext()) {
                block = operation.getResponse().getBodyBlocks().next();
                resultName = block.getName().getLocalPart();
                nsURI = block.getName().getNamespaceURI();
            }

            for (Parameter parameter : operation.getResponse().getParametersList()) {
                if (parameter.getParameterIndex() == -1) {
                    if(operation.isWrapped()||!isDocStyle){
                        if(parameter.getBlock().getLocation() == Block.HEADER){
                            resultName = parameter.getBlock().getName().getLocalPart();
                        }else{
                            resultName = parameter.getName();
                        }
                        if (isDocStyle || (parameter.getBlock().getLocation() == Block.HEADER)) {
                            nsURI = parameter.getType().getName().getNamespaceURI();
                        }
                    }else if(isDocStyle){
                        JAXBType t = (JAXBType)parameter.getType();
                        resultName = t.getName().getLocalPart();
                        nsURI = t.getName().getNamespaceURI();
                    }
                    if(!(operation instanceof AsyncOperation)){
                        JAnnotationUse wr = null;

                        if(!resultName.equals("return")){
                            if(wr == null)
                                wr = m.annotate(javax.jws.WebResult.class);
                            wr.param("name", resultName);
                        }
                        //if (operation.getStyle().equals(SOAPStyle.DOCUMENT) && !(nsURI.equals(serviceNS))) {
                        if((nsURI != null) && (!nsURI.equals(serviceNS) || (isDocStyle && operation.isWrapped()))){
                            if(wr == null)
                                wr = m.annotate(javax.jws.WebResult.class);
                            wr.param("targetNamespace", nsURI);
                        }
                        //doclit wrapped could have additional headers
                        if(!(isDocStyle && operation.isWrapped()) ||
                                (parameter.getBlock().getLocation() == Block.HEADER)){
                            if(wr == null)
                                wr = m.annotate(javax.jws.WebResult.class);
                            wr.param("partName", parameter.getName());
                        }
                        if(parameter.getBlock().getLocation() == Block.HEADER){
                            if(wr == null)
                                wr = m.annotate(javax.jws.WebResult.class);
                            wr.param("header",true);
                        }
                    }
                }

            }
        }

        //DOC/BARE
        if (!sameParamStyle) {
            if(!operation.isWrapped()) {
               JAnnotationUse sb = m.annotate(SOAPBinding.class);
               sb.param("parameterStyle", SOAPBinding.ParameterStyle.BARE);
            }
        }

        if (operation.isWrapped() && operation.getStyle().equals(SOAPStyle.DOCUMENT)) {
            Block reqBlock = operation.getRequest().getBodyBlocks().next();
            JAnnotationUse reqW = m.annotate(javax.xml.ws.RequestWrapper.class);
            reqW.param("localName", reqBlock.getName().getLocalPart());
            reqW.param("targetNamespace", reqBlock.getName().getNamespaceURI());
            reqW.param("className", reqBlock.getType().getJavaType().getName());

            if (response != null) {
                JAnnotationUse resW = m.annotate(javax.xml.ws.ResponseWrapper.class);
                Block resBlock = response.getBodyBlocks().next();
                resW.param("localName", resBlock.getName().getLocalPart());
                resW.param("targetNamespace", resBlock.getName().getNamespaceURI());
                resW.param("className", resBlock.getType().getJavaType().getName());
            }
        }
    }

    //TODO: JAXB should expose the annotations so that it can be added to JAnnotationUse
    protected void writeJAXBTypeAnnotations(JAnnotationUse annUse, Parameter param) throws IOException{
        List<String> annotations = param.getAnnotations();
        if(annotations == null)
            return;

        for(String annotation:param.getAnnotations()){
            //p.pln(annotation);
            //annUse.
        }
    }

    private boolean isMessageParam(Parameter param, Message message) {
        Block block = param.getBlock();

        return (message.getBodyBlockCount() > 0 && block.equals(message.getBodyBlocks().next())) ||
               (message.getHeaderBlockCount() > 0 &&
               block.equals(message.getHeaderBlocks().next()));
    }

    private boolean isHeaderParam(Parameter param, Message message) {
        if (message.getHeaderBlockCount() == 0)
            return false;

        for (Block headerBlock : message.getHeaderBlocksMap().values())
            if (param.getBlock().equals(headerBlock))
                return true;

        return false;
    }

    private boolean isAttachmentParam(Parameter param, Message message){
        if (message.getAttachmentBlockCount() == 0)
            return false;

        for (Block attBlock : message.getAttachmentBlocksMap().values())
            if (param.getBlock().equals(attBlock))
                return true;

        return false;
    }

    private boolean isUnboundParam(Parameter param, Message message){
        if (message.getUnboundBlocksCount() == 0)
            return false;

        for (Block unboundBlock : message.getUnboundBlocksMap().values())
            if (param.getBlock().equals(unboundBlock))
                return true;

        return false;
    }

    private void writeWebParam(Operation operation, JavaParameter javaParameter, JAnnotationUse paramAnno) {
        Parameter param = javaParameter.getParameter();
        Request req = operation.getRequest();
        Response res = operation.getResponse();

        boolean header = isHeaderParam(param, req) ||
            (res != null ? isHeaderParam(param, res) : false);

        String name;
        boolean isWrapped = operation.isWrapped();

        if((param.getBlock().getLocation() == Block.HEADER) || (isDocStyle && !isWrapped))
            name = param.getBlock().getName().getLocalPart();
        else
            name = param.getName();

        paramAnno.param("name", name);

        String ns= null;

        if (isDocStyle) {
            ns = param.getBlock().getName().getNamespaceURI(); // its bare nsuri
            if(isWrapped){
                ns = ((JAXBType)param.getType()).getName().getNamespaceURI();
            }
        }else if(!isDocStyle && header){
            ns = param.getBlock().getName().getNamespaceURI();
        }

        if((ns != null) && (!ns.equals(serviceNS) || (isDocStyle && isWrapped)))
            paramAnno.param("targetNamespace", ns);

        if (header) {
            paramAnno.param("header", true);
        }

        if (param.isINOUT()){
            paramAnno.param("mode", javax.jws.WebParam.Mode.INOUT);
        }else if ((res != null) && (isMessageParam(param, res) || isHeaderParam(param, res) || isAttachmentParam(param, res) ||
                isUnboundParam(param,res))){
            paramAnno.param("mode", javax.jws.WebParam.Mode.OUT);
        }

        //doclit wrapped could have additional headers
        if(!(isDocStyle && isWrapped) || header)
            paramAnno.param("partName", javaParameter.getParameter().getName());
    }

    boolean isDocStyle = true;
    boolean sameParamStyle = true;
    private void writeSOAPBinding(Port port, JDefinedClass cls) {
        JAnnotationUse soapBindingAnn = null;
        isDocStyle = port.getStyle() != null ? port.getStyle().equals(SOAPStyle.DOCUMENT) : true;
        if(!isDocStyle){
            if(soapBindingAnn == null)
                soapBindingAnn = cls.annotate(SOAPBinding.class);
            soapBindingAnn.param("style", SOAPBinding.Style.RPC);
            port.setWrapped(true);
        }
        if(isDocStyle){
            boolean first = true;
            boolean isWrapper = true;
            for(Operation operation:port.getOperations()){
                if(first){
                    isWrapper = operation.isWrapped();
                    first = false;
                    continue;
                }
                sameParamStyle = (isWrapper == operation.isWrapped());
                if(!sameParamStyle)
                    break;
            }
            if(sameParamStyle)
                port.setWrapped(isWrapper);
        }
        if(sameParamStyle && !port.isWrapped()){
            if(soapBindingAnn == null)
                soapBindingAnn = cls.annotate(SOAPBinding.class);
            soapBindingAnn.param("parameterStyle", SOAPBinding.ParameterStyle.BARE);
        }
    }

    private void writeWebServiceAnnotation(Port port, JAnnotationUse wsa) {
        QName name = (QName) port.getProperty(ModelProperties.PROPERTY_WSDL_PORT_TYPE_NAME);
        wsa.param("name", name.getLocalPart());
        wsa.param("targetNamespace", name.getNamespaceURI());
    }




    public void visit(Model model) throws Exception {
        for(Service s:model.getServices()){
            s.accept(this);
        }
    }

    public void visit(Service service) throws Exception {
        String jd = model.getJavaDoc();
        if(jd != null){
            JPackage pkg = cm._package(options.defaultPackage);
            pkg.javadoc().add(jd);
        }

        for(Port p:service.getPorts()){
            visitPort(service, p);
        }
    }

    private void visitPort(Service service, Port port) {
        if (port.isProvider()) {
            return;                // Not generating for Provider based endpoint
        }


        try {
            write(service, port);
        } catch (Exception e) {
            throw new GeneratorException(
                "generator.nestedGeneratorError",
                e);
        }
    }

    private void register(TJavaGeneratorExtension h) {
        extensionHandlers.add(h);
    }
}
