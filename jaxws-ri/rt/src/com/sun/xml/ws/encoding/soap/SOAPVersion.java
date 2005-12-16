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

package com.sun.xml.ws.encoding.soap;

import javax.xml.ws.soap.SOAPBinding;
import javax.xml.soap.*;


/**
 * Represents various constants of SOAP 1.1 and SOAP 1.2.
 *
 * @author Kohsuke Kawaguchi
 */
public enum SOAPVersion {
    SOAP_11(SOAPBinding.SOAP11HTTP_BINDING,
            SOAPConstants.URI_ENVELOPE,
            javax.xml.soap.SOAPConstants.SOAP_1_1_PROTOCOL),

    SOAP_12(SOAPBinding.SOAP12HTTP_BINDING,
            SOAP12Constants.URI_ENVELOPE,
            javax.xml.soap.SOAPConstants.SOAP_1_2_PROTOCOL);

    /**
     * Either {@link SOAPBinding#SOAP11HTTP_BINDING} or
     *  {@link SOAPBinding#SOAP12HTTP_BINDING}
     */
    public final String binding;

    /**
     * SOAP envelope namespace URI.
     */
    public final String nsUri;

    /**
     * SAAJ {@link MessageFactory} for this SOAP version.
     */
    public final MessageFactory saajFactory;

    /**
     * SAAJ {@link SOAPFactory} for this SOAP version.
     */
    public final SOAPFactory saajSoapFactory;

    private SOAPVersion(String binding, String nsUri, String saajFactoryString) {
        this.binding = binding;
        this.nsUri = nsUri;
        try {
            saajFactory = MessageFactory.newInstance(saajFactoryString);
            saajSoapFactory = SOAPFactory.newInstance(saajFactoryString);
        } catch (SOAPException e) {
            throw new Error(e);
        }
    }

    public String toString() {
        return binding;
    }

    /**
     * Returns {@link SOAPVersion} whose {@link #binding} equals to
     * the given string.
     *
     * This method does not perform input string validation.
     *
     * @param binding
     *      for historical reason, we treat null as {@link #SOAP_11},
     *      but you really shouldn't be passing null.
     * @return always non-null.
     */
    public static SOAPVersion fromBinding(String binding) {
        if(binding==null)
            return SOAP_11;
        
        if(binding.equals(SOAP_12.binding))
            return SOAP_12;
        else
            return SOAP_11;
    }
}
