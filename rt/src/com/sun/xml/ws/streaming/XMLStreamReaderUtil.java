/*
 * $Id: XMLStreamReaderUtil.java,v 1.1 2005-05-23 22:59:37 bbissett Exp $
 */

/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.xml.ws.streaming;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import com.sun.xml.ws.util.exception.LocalizableExceptionAdapter;
import com.sun.xml.ws.util.xml.XmlUtil;

import com.sun.xml.ws.encoding.soap.streaming.SOAPNamespaceConstants;
import com.sun.xml.ws.encoding.soap.SOAPConstants;

/**
 * <p> XMLStreamReaderUtil provides some utility methods intended to be used
 * in conjunction with a StAX XMLStreamReader. </p>
 *
 * @author JAX-RPC Development Team
 */
public class XMLStreamReaderUtil {

    private XMLStreamReaderUtil() {
    }

    public static int next(XMLStreamReader reader) {
        try {
            int readerEvent = reader.next();
            
            while (readerEvent != XMLStreamConstants.END_DOCUMENT) {            
                switch (readerEvent) {
                    case XMLStreamConstants.START_ELEMENT:
                    case XMLStreamConstants.END_ELEMENT:
                    case XMLStreamConstants.CDATA:
                    case XMLStreamConstants.CHARACTERS:
                    case XMLStreamConstants.PROCESSING_INSTRUCTION:
                        return readerEvent;
                    default:
                        // falls through ignoring event
                }
                readerEvent = reader.next();
            }
            
            return readerEvent;
        }
        catch (XMLStreamException e) {
            throw wrapException(e);
        }        
    }

    public static int nextElementContent(XMLStreamReader reader) {
        int state = nextContent(reader);
        if (state == XMLStreamConstants.CHARACTERS) {
            throw new XMLStreamReaderException(
                "xmlreader.unexpectedCharacterContent", reader.getText());
        }
        return state;
    }

    public static int nextContent(XMLStreamReader reader) {
        for (;;) {
            int state = next(reader);
            switch (state) {
                case XMLStreamConstants.START_ELEMENT:
                case XMLStreamConstants.END_ELEMENT:
                case XMLStreamConstants.END_DOCUMENT:
                    return state;
                case XMLStreamConstants.CHARACTERS:
                    if (!reader.isWhiteSpace()) {
                        return XMLStreamConstants.CHARACTERS;
                    }
            }
        }
    }    
    
    public static void skipElement(XMLStreamReader reader) {
        try {
            int state = reader.next();
            while (state != XMLStreamConstants.END_DOCUMENT && 
                    state != XMLStreamConstants.END_ELEMENT) {
                state = reader.next();
            }
        }
        catch (XMLStreamException e) {
            throw wrapException(e);
        }        
    }
    
    /*
     * Get the text of an element
     */
    public static String getElementText(XMLStreamReader reader) {
        try {
            return reader.getElementText();
        } catch (XMLStreamException e) {
            throw wrapException(e);
        }
    }
    
    /**
     * Read all attributes into an data structure. Note that this method cannot
     * be called multiple times to get the same list of attributes. 
     */
    public static Attributes getAttributes(XMLStreamReader reader) {
        return (reader.getEventType() == reader.START_ELEMENT ||
                reader.getEventType() == reader.ATTRIBUTE) ?
                new AttributesImpl(reader) : null;
    }
        
    public static void verifyReaderState(XMLStreamReader reader, int expectedState) {
        if (reader.getEventType() != expectedState) {
            throw new XMLStreamReaderException(
                "xmlreader.unexpectedState",
                new Object[] {
                    getStateName(expectedState) });
        }
    }

    public static void verifyTag(XMLStreamReader reader, QName name) {
        if (!name.equals(reader.getName())) {
            throw new XMLStreamReaderException(
                "xmlreader.unexpectedState.tag",
                new Object[] {
                    name,
                    reader.getName()});
        }
    }

    public static String getStateName(XMLStreamReader reader) {
        return getStateName(reader.getEventType());
    }
    
    public static String getStateName(int state) {
        switch (state) {
            case XMLStreamConstants.ATTRIBUTE:
                return "ATTRIBUTE";
            case XMLStreamConstants.CDATA:
                return "CDATA";
            case XMLStreamConstants.CHARACTERS:
                return "CHARACTERS";
            case XMLStreamConstants.COMMENT:
                return "COMMENT";
            case XMLStreamConstants.DTD:
                return "DTD";
            case XMLStreamConstants.END_DOCUMENT:
                return "END_DOCUMENT";
            case XMLStreamConstants.END_ELEMENT:
                return "END_ELEMENT";
            case XMLStreamConstants.ENTITY_DECLARATION:
                return "ENTITY_DECLARATION";
            case XMLStreamConstants.ENTITY_REFERENCE:
                return "ENTITY_REFERENCE";
            case XMLStreamConstants.NAMESPACE:
                return "NAMESPACE";
            case XMLStreamConstants.NOTATION_DECLARATION:
                return "NOTATION_DECLARATION";
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                return "PROCESSING_INSTRUCTION";
            case XMLStreamConstants.SPACE:
                return "SPACE";
            case XMLStreamConstants.START_DOCUMENT:
                return "START_DOCUMENT";
            case XMLStreamConstants.START_ELEMENT:
                return "START_ELEMENT";
            default :
                return "UNKNOWN";
        }
    }
    
    private static XMLStreamReaderException wrapException(XMLStreamException e) {
        return new XMLStreamReaderException(
            "xmlreader.ioException",
            new LocalizableExceptionAdapter(e));
    }
    
    // -- Auxiliary classes ----------------------------------------------
    
    /**
     * AttributesImpl class copied from old StAXReader. This class is used to implement
     * getAttributes() on a StAX Reader.
     */
    public static class AttributesImpl implements Attributes {
        
        static final String XMLNS_NAMESPACE_URI = "http://www.w3.org/2000/xmlns/";

        static class AttributeInfo {

            private QName name;
            private String value;

            public AttributeInfo(QName name, String value) {
                this.name = name;
                if (value == null) {
                    // e.g., <return xmlns=""> -- stax returns null
                    this.value = "";
                } else {
                    this.value = value;
                }
            }

            QName getName() {
                return name;
            }

            String getValue() {
                return value;
            }

            /*
             * Return "xmlns:" as part of name if namespace.
             */
            String getLocalName() {
                if (isNamespaceDeclaration()) {
                    if (name.getLocalPart().equals("")) {
                        return "xmlns";
                    }
                    return "xmlns:" + name.getLocalPart();
                }
                return name.getLocalPart();
            }

            boolean isNamespaceDeclaration() {
                return (name.getNamespaceURI() == XMLNS_NAMESPACE_URI);
            }
        }
    
        // stores qname and value for each attribute
        AttributeInfo [] atInfos;

        /*
         * Will create a list that contains the namespace declarations
         * as well as the other attributes.
         */
        public AttributesImpl(XMLStreamReader reader) {
            if (reader == null) {

                // this is the case when we call getAttributes() on the
                // reader when it is not on a start tag
                atInfos = new AttributeInfo[0];
            } else {

                // this is the normal case
                int index = 0;
                String namespacePrefix = null;
                int namespaceCount = reader.getNamespaceCount();
                int attributeCount = reader.getAttributeCount();
                atInfos = new AttributeInfo[namespaceCount + attributeCount];
                for (int i=0; i<namespaceCount; i++) {
                    namespacePrefix = reader.getNamespacePrefix(i);

                    // will be null if default prefix. QName can't take null
                    if (namespacePrefix == null) {
                        namespacePrefix = "";
                    }
                    atInfos[index++] = new AttributeInfo(
                        new QName(XMLNS_NAMESPACE_URI,
                            namespacePrefix,
                            "xmlns"),
                        reader.getNamespaceURI(i));
                }
                for (int i=0; i<attributeCount; i++) {
                    atInfos[index++] = new AttributeInfo(
                        reader.getAttributeName(i),
                        reader.getAttributeValue(i));
                }
            }
        }

        public int getLength() {
            return atInfos.length;
        }

        public String getLocalName(int index) {
            if (index >= 0 && index < atInfos.length) {
                return atInfos[index].getLocalName();
            }
            return null;
        }

        public QName getName(int index) {
            if (index >= 0 && index < atInfos.length) {
                return atInfos[index].getName();
            }
            return null;
        }

        public String getPrefix(int index) {
            if (index >= 0 && index < atInfos.length) {
                return atInfos[index].getName().getPrefix();
            }
            return null;
        }

        public String getURI(int index) {
            if (index >= 0 && index < atInfos.length) {
                return atInfos[index].getName().getNamespaceURI();
            }
            return null;
        }

        public String getValue(int index) {
            if (index >= 0 && index < atInfos.length) {
                return atInfos[index].getValue();
            }
            return null;
        }

        public String getValue(QName name) {
            int index = getIndex(name);
            if (index != -1) {
                return atInfos[index].getValue();
            }
            return null;
        }

        public String getValue(String localName) {
            int index = getIndex(localName);
            if (index != -1) {
                return atInfos[index].getValue();
            }
            return null;
        }

        public String getValue(String uri, String localName) {
            int index = getIndex(uri, localName);
            if (index != -1) {
                return atInfos[index].getValue();
            }
            return null;
        }

        public boolean isNamespaceDeclaration(int index) {
            if (index >= 0 && index < atInfos.length) {
                return atInfos[index].isNamespaceDeclaration();
            }
            return false;
        }

        public int getIndex(QName name) {
            for (int i=0; i<atInfos.length; i++) {
                if (atInfos[i].getName().equals(name)) {
                    return i;
                }
            }
            return -1;
        }

        public int getIndex(String localName) {
            for (int i=0; i<atInfos.length; i++) {
                if (atInfos[i].getName().getLocalPart().equals(localName)) {
                    return i;
                }
            }
            return -1;
        }

        public int getIndex(String uri, String localName) {
            QName qName;
            for (int i=0; i<atInfos.length; i++) {
                qName = atInfos[i].getName();
                if (qName.getNamespaceURI().equals(uri) &&
                    qName.getLocalPart().equals(localName)) {

                    return i;
                }
            }
            return -1;
        }
    }
}
