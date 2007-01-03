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

package com.sun.xml.ws.streaming;

import com.sun.xml.ws.util.FastInfosetReflection;
import com.sun.xml.ws.util.SunStAXReflection;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * <p>A factory to create XML and FI parsers.</p>
 *
 * @author Santiago.PericasGeertsen@sun.com
 */
public class XMLStreamReaderFactory {
    
    /**
     * StAX input factory shared by all threads.
     */
    static final XMLInputFactory xmlInputFactory;

    static {
        // Use StAX pluggability layer to get factory instance
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);

        try {
            // Turn OFF internal factory caching in Zephyr -- not thread safe
            xmlInputFactory.setProperty("reuse-instance", Boolean.FALSE);
        }
        catch (IllegalArgumentException e) {
            // falls through
        }
    }
    
    // -- XML ------------------------------------------------------------
    
    /**
     * Returns a fresh StAX parser created from an InputSource. Use this 
     * method when concurrent instances are needed within a single thread.
     *
     * TODO: Reject DTDs?
     */
    public static XMLStreamReader createFreshXMLStreamReader(InputSource source,
        boolean rejectDTDs) {
        try {
            // Char stream available?
            if (source.getCharacterStream() != null) {
                return createXMLStreamReader(source.getSystemId(), source.getCharacterStream(), rejectDTDs);
            }

            // Byte stream available?
            if (source.getByteStream() != null) {
                return createXMLStreamReader(source.getSystemId(), source.getByteStream(), rejectDTDs);
            }
            
            synchronized (xmlInputFactory) {
                // Otherwise, open URI                
                return xmlInputFactory.createXMLStreamReader(source.getSystemId(),
                    new URL(source.getSystemId()).openStream());
            }
        }
        catch (Exception e) {
            throw new XMLReaderException("stax.cantCreate",e);
        }
    }
    
    /**
     * Returns a StAX parser from an InputStream.
     *
     * TODO: Reject DTDs?
     */
    public static XMLStreamReader createXMLStreamReader(InputStream in,
        boolean rejectDTDs) {
        return createXMLStreamReader(null, in, rejectDTDs);
    }
    
    /**
     * Returns a StAX parser from an InputStream. Attemps to re-use parsers if
     * underlying representation is Zephyr.
     *
     * TODO: Reject DTDs?
     */
    public static XMLStreamReader createXMLStreamReader(String systemId, 
        InputStream in, boolean rejectDTDs) {
        try {
            synchronized (xmlInputFactory) {
                // Otherwise, open URI
                return xmlInputFactory.createXMLStreamReader(systemId,
                    in);
            }
        } catch (Exception e) {
            throw new XMLReaderException("stax.cantCreate",e);
        }
    }
    
    /**
     * Returns a StAX parser from a Reader. Attemps to re-use parsers if
     * underlying representation is Zephyr.
     *
     * TODO: Reject DTDs?
     */
    public static XMLStreamReader createXMLStreamReader(String systemId, Reader reader, boolean rejectDTDs) {
        try {
            synchronized (xmlInputFactory) {
                // Otherwise, open URI
                return xmlInputFactory.createXMLStreamReader(systemId,reader);
            }
        } catch (Exception e) {
            throw new XMLReaderException("stax.cantCreate",e);
        }

    }
    
    // -- Fast Infoset ---------------------------------------------------
    
    public static XMLStreamReader createFIStreamReader(InputSource source) {
        return createFIStreamReader(source.getByteStream());
    }
    
    /**
     * Returns the FI parser allocated for this thread.
     */
    public static XMLStreamReader createFIStreamReader(InputStream in) {
        // Check if compatible implementation of FI was found
        if (FastInfosetReflection.fiStAXDocumentParser_new == null) {
            throw new XMLReaderException("fastinfoset.noImplementation");
        }
        
        try {
            // Do not use StAX pluggable layer for FI
            Object sdp = FastInfosetReflection.fiStAXDocumentParser_new.newInstance();
            FastInfosetReflection.fiStAXDocumentParser_setStringInterning.invoke(sdp, Boolean.TRUE);
            FastInfosetReflection.fiStAXDocumentParser_setInputStream.invoke(sdp, in);
            return (XMLStreamReader) sdp;
        } catch (Exception e) {
            throw new XMLStreamReaderException(e);
        }
    }
    
}
