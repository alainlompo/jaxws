package com.sun.xml.ws.api.model;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;

import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Provider;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Represents the relationship between an endpoint interface (SEI) and a {@link WSDLModel}.
 *
 * <p>
 * This interface would be used to access which Java concepts correspond to
 * which WSDL concepts, such as which <code>wsdl:port</code> corresponds to
 * a SEI, or which <code>wsdl:operation</code> corresponds to {@link JavaMethod}.
 *
 * <P>
 * It also retains information about the databinding done for a SEI;
 * such as {@link JAXBRIContext} and {@link Bridge}.
 *
 * <p>
 * This model is constructed only when there is a Java SEI. Therefore it's
 * not available with {@link Dispatch} or {@link Provider}. Technologies that
 * need to work regardless of such surface API difference shall not be using
 * this model.
 *
 * @author Vivek Pandey
 */
public interface RuntimeModel {
    /**
     * {@link BridgeContext} is not thread safe, the RuntimeModel must return a thread local {@link BridgeContext}
     *
     * @return the <code>{@link BridgeContext}</code>
     */
    BridgeContext getBridgeContext();

    /**
     * JAXBContext that will be used to marshall/unmarshall the java classes found in the SEI.
     *
     * @return the <code>{@link JAXBRIContext}</code>
     */
    JAXBRIContext getJAXBContext();

    /**
     * Get the Bridge associated with the {@link TypeReference}
     *
     * @param type
     * @return the <code>{@link Bridge}</code> for the <code>type</code>
     */
    Bridge getBridge(TypeReference type);

    /**
     * Its a known fault if the exception thrown by {@link Method} is annotated with the
     * {@link javax.xml.ws.WebFault#name()} thas equal to the name passed as parameter.
     *
     * @param name   is the qualified name of fault detail element as specified by wsdl:fault@element value.
     * @param method is the Java {@link Method}
     * @return true if <code>name</code> is the name
     *         of a known fault name for the <code>method</code>
     */
    boolean isKnownFault(QName name, Method method);

    /**
     * Checks if the {@link JavaMethod} for the {@link Method} knowns the exception class.
     *
     * @param m  {@link Method} to pickup the right {@link JavaMethod} model
     * @param ex exception class
     * @return true if <code>ex</code> is a Checked Exception
     *         for <code>m</code>
     */
    boolean isCheckedException(Method m, Class ex);

    /**
     * This method will be useful to get the {@link JavaMethod} corrrespondiong to
     * a {@link Method} - such as on the client side.
     *
     * @param method for which {@link JavaMethod} is asked for
     * @return the {@link JavaMethod} representing the <code>method</code>
     */
    JavaMethod getJavaMethod(Method method);

    /**
     * Gives a {@link JavaMethod} for a given {@link QName}. The {@link QName} will
     * be equivalent to the SOAP Body or Header block or can simply be the name of an
     * infoset that corresponds to the payload.
     *
     * @param name
     * @return the <code>JavaMethod</code> associated with the
     *         operation named name
     */
    JavaMethod getJavaMethod(QName name);

    /**
     * Gives all the {@link JavaMethod} for a wsdl:port for which this {@link RuntimeModel} is
     * created.
     *
     * @return a {@link Collection} of {@link JavaMethod}
     *         associated with the {@link RuntimeModel}
     */
    Collection<JavaMethod> getJavaMethods();

    /**
     * Location of the WSDL that defines the port associated with the {@link RuntimeModel}
     *
     * @return wsdl location uri - always non-null
     */
    String getWSDLLocation();

    /**
     * wsdl:service qualified name for the port associated with the {@link RuntimeModel)
     *
     * @return wsdl:service@name value - always non-null
     */
    QName getServiceQName();

    /**
     * Value of the wsdl:port name associated with the {@link RuntimeModel)
     *
     * @return wsdl:service/wsdl:port@name value, always non-null
     */
    QName getPortName();

    /**
     * Value of wsdl:portType bound to the port associated with the {@link RuntimeModel)
     *
     * @return
     */
    QName getPortTypeName();

    /**
     * Namespace of the wsd;:port associated with the {@link RuntimeModel)
     */
    String getTargetNamespace();
}
