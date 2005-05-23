/*
 * $Id: LogicalMessageImpl.java,v 1.1 2005-05-23 22:37:25 bbissett Exp $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc.
 * All rights reserved.
 */
package com.sun.xml.ws.handler;

import javax.xml.bind.JAXBContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.LogicalMessage;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Node;

import com.sun.pept.ept.MessageInfo;
import com.sun.xml.ws.encoding.jaxb.JAXBBeanInfo;
import com.sun.xml.ws.encoding.jaxb.JAXBBridgeInfo;
import com.sun.xml.ws.encoding.jaxb.LogicalEPTFactory;
import com.sun.xml.ws.encoding.jaxb.LogicalEncoder;
import com.sun.xml.ws.encoding.jaxb.RpcLitPayload;
import com.sun.xml.ws.encoding.soap.SOAPEncoder;
import com.sun.xml.ws.encoding.soap.internal.BodyBlock;
import com.sun.xml.ws.encoding.soap.internal.InternalMessage;
import com.sun.xml.ws.util.xml.XmlUtil;
import com.sun.xml.ws.encoding.soap.message.SOAPFaultInfo;

public class LogicalMessageImpl implements LogicalMessage {

    private HandlerContext ctxt;

    public LogicalMessageImpl(HandlerContext ctxt) {
        this.ctxt = ctxt;
    }

    /*
     * If the payload is DOMSource, return it
     * If the payload is Source/SOAPFaultInfo/JAXBBridgeInfo/JAXBBeanInfo,
     * convert to DOMSource and return it. DOMSource is also stored in BodyBlock
     */
    public Source getPayload() {
        try {
            InternalMessage internalMessage = ctxt.getInternalMessage();
            if (internalMessage == null) {
                SOAPMessage soapMessage = ctxt.getSOAPMessage();
                if (soapMessage == null) {
                    return null;
                } else {
                    Node node = soapMessage.getSOAPBody().getFirstChild();
                    if (node != null) {
                        setSource(new DOMSource(node));
                    } else {
                        return null;
                    }
                }
            }
            internalMessage = ctxt.getInternalMessage();
            BodyBlock bodyBlock = internalMessage.getBody();
            if (bodyBlock == null) {
                return null;
            } else {
                Object obj = bodyBlock.getValue();
                if (obj instanceof DOMSource) {
                    return (Source)obj;
                } else if (obj instanceof Source) {
                    Source source = (Source)obj;
                    Transformer transformer = XmlUtil.newTransformer();
                    DOMResult domResult = new DOMResult();
                    transformer.transform(source, domResult);
                    DOMSource domSource = new DOMSource(domResult.getNode());
                    bodyBlock.setSource(domSource);
                    return domSource;
                } else if (obj instanceof JAXBBridgeInfo) {
                    MessageInfo messageInfo = ctxt.getMessageInfo();
                    LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
                    SOAPEncoder encoder = eptf.getSOAPEncoder();
                    DOMSource domSource = encoder.toDOMSource((JAXBBridgeInfo)obj, messageInfo);
                    bodyBlock.setSource(domSource);
                    return domSource;
                } else if (obj instanceof JAXBBeanInfo) {
                    MessageInfo messageInfo = ctxt.getMessageInfo();
                    LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
                    LogicalEncoder encoder = eptf.getLogicalEncoder();
                    DOMSource domSource = encoder.toDOMSource((JAXBBeanInfo)obj);
                    bodyBlock.setSource(domSource);
                    return domSource;
                } else if (obj instanceof RpcLitPayload) {
                    MessageInfo messageInfo = ctxt.getMessageInfo();
                    LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
                    SOAPEncoder encoder = eptf.getSOAPEncoder();
                    DOMSource domSource = encoder.toDOMSource((RpcLitPayload)obj, messageInfo);
                    bodyBlock.setSource(domSource);
                    return domSource;
                } else if (obj instanceof SOAPFaultInfo) {
                    MessageInfo messageInfo = ctxt.getMessageInfo();
                    LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
                    SOAPEncoder encoder = eptf.getSOAPEncoder();
                    DOMSource domSource = encoder.toDOMSource((SOAPFaultInfo)obj, messageInfo);
                    bodyBlock.setSource(domSource);
                    return domSource;
                } else {
                    System.out.println("***** unknown type in BodyBlock *****"+obj.getClass());
                }
            }
        } catch(TransformerException te) {
            throw new WebServiceException(te);
        } catch(SOAPException se) {
            throw new WebServiceException(se);
        }
        return null;
    }

    /*
     * Sets the Source as payload in the BodyBlock of InternalMessage.
     */
    public void setPayload(Source source) {
        setSource(source);
    }

    /*
     * Converts to DOMSource and keeps it in BodyBlock. Then it unmarshalls this
     * DOMSource to a jaxb object. Any changes done in jaxb object are lost if
     * the object isn't set again.
     */
    public Object getPayload(JAXBContext jaxbContext) {
        DOMSource source = (DOMSource)getPayload();
        MessageInfo messageInfo = ctxt.getMessageInfo();
        LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
        LogicalEncoder encoder = eptf.getLogicalEncoder();
        JAXBBeanInfo beanInfo = encoder.toJAXBBeanInfo(source, jaxbContext);
        return beanInfo.getBean();
    }

    /*
     * The object is marshalled into DOMSource and stored in BodyBlock. If an
     * error occurs when using the supplied JAXBContext to marshall the
     * payload, it throws a JAXRPCException.
     */
    public void setPayload(Object bean, JAXBContext jaxbContext) {
        Source source = null;
        try {
            MessageInfo messageInfo = ctxt.getMessageInfo();
            LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
            LogicalEncoder encoder = eptf.getLogicalEncoder();
            source = encoder.toDOMSource(new JAXBBeanInfo(bean, jaxbContext));
        } catch(Exception e) {
            throw new WebServiceException(e);
        }
        setSource(source);              // set Source in BodyBlock
    }

    public HandlerContext getHandlerContext() {
        return ctxt;
    }

    /*
     * It stores Source in the BodyBlock. If necessary, it creates
     * InternalMessage, and BodyBlock
     */
    private void setSource(Source source) {
        InternalMessage internalMessage = ctxt.getInternalMessage();
        if (internalMessage == null) {
            internalMessage = new InternalMessage();
            ctxt.setInternalMessage(internalMessage);
        }
        BodyBlock bodyBlock = internalMessage.getBody();
        if (bodyBlock == null) {
            bodyBlock = new BodyBlock(source);
            internalMessage.setBody(bodyBlock);
        } else {
            bodyBlock.setSource(source);
        }
    }

}
