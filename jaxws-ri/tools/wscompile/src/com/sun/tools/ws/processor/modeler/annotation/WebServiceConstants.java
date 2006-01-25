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
package com.sun.tools.ws.processor.modeler.annotation;


/**
 *
 * @author  dkohlert
 */
public interface WebServiceConstants { //extends RmiConstants {

    public static final String RETURN                       = "return";
    public static final String RETURN_CAPPED                = "Return";
    public static final String RETURN_VALUE                 = "_return";
    public static final String SERVICE                      = "Service";
    public static final String PD                           = ".";
    public static final String JAXWS                        = "jaxws";
    public static final String JAXWS_PACKAGE_PD             = JAXWS+PD;
    public static final String PD_JAXWS_PACKAGE_PD          = PD+JAXWS+PD;
    public static final String BEAN                         = "Bean";
    public static final String GET_PREFIX                   = "get";
    public static final String IS_PREFIX                    = "is";
    public static final String FAULT_INFO                   = "faultInfo";
    public static final String GET_FAULT_INFO               = "getFaultInfo";
    public static final String HTTP_PREFIX                  = "http://";
    public static final String JAVA_LANG_OBJECT             = "java.lang.Object";
    public static final String EMTPY_NAMESPACE_ID           = "";
    

    public static final char SIGC_INNERCLASS  = '$';
    public static final char SIGC_UNDERSCORE  = '_';
    
    public static final String DOT = ".";    
    public static final String PORT = "WSDLPort";
    public static final String BINDING = "Binding";
    public static final String RESPONSE = "Response";
    
    /*
     * Identifiers potentially useful for all Generators
     */
    public static final String EXCEPTION_CLASSNAME =
        java.lang.Exception.class.getName();
    public static final String REMOTE_CLASSNAME =
        java.rmi.Remote.class.getName();
    public static final String REMOTE_EXCEPTION_CLASSNAME =
        java.rmi.RemoteException.class.getName();
    public static final String RUNTIME_EXCEPTION_CLASSNAME =
        java.lang.RuntimeException.class.getName();
    public static final String SERIALIZABLE_CLASSNAME =
        java.io.Serializable.class.getName();
    public static final String HOLDER_CLASSNAME =
        javax.xml.ws.Holder.class.getName();
    

    // 181 constants
    public static final String WEBSERVICE_NAMESPACE         = "http://www.bea.com/xml/ns/jws";
    public static final String HANDLER_CONFIG               = "handler-config";
    public static final String HANDLER_CHAIN                = "handler-chain";
    public static final String HANDLER_CHAIN_NAME           = "handler-chain-name";
    public static final String HANDLER                      = "handler";
    public static final String HANDLER_NAME                 = "handler-name";
    public static final String HANDLER_CLASS                = "handler-class";
    public static final String INIT_PARAM                   = "init-param";
    public static final String SOAP_ROLE                    = "soap-role";
    public static final String SOAP_HEADER                  = "soap-header";
    public static final String PARAM_NAME                   = "param-name";
    public static final String PARAM_VALUE                  = "param-value";
}
