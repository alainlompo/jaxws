/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.jvnet.ws.message;

import java.util.List;

import javax.activation.DataHandler;
import javax.xml.soap.SOAPMessage;

/**
 * MessageContext represents a container of a SOAP message and all the properties
 * including the transport headers.
 *
 * MessageContext is a {@link PropertySet} that combines properties exposed from multiple
 * {@link PropertySet}s into one.
 *
 * <p>
 * This implementation allows one {@link PropertySet} to assemble
 * all properties exposed from other "satellite" {@link PropertySet}s.
 * (A satellite may itself be a {@link DistributedPropertySet}, so
 * in general this can form a tree.)
 * 
 * @author shih-chang.chen@oracle.com
 */
public interface MessageContext extends PropertySet {
	
	/**
	 * Gets the SAAJ SOAPMessage representation of the SOAP message.
	 * 
	 * @return The SOAPMessage
	 */
	SOAPMessage getSOAPMessage();

	/**
	 * Sets the SAAJ SOAPMessage to be the SOAP message.
	 * 
	 * @param message The SOAP message to set
	 */
	void setSOAPMessage(SOAPMessage message);

    void addSatellite(PropertySet satellite);

    void removeSatellite(PropertySet satellite);

    void copySatelliteInto(MessageContext r);

    <T extends PropertySet> T getSatellite(Class<T> satelliteClass);

	void addTransportHeader(String name, List<String> values);
	
	void addAttachment(String name, DataHandler dh);
    
//    public interface Builder {
//    	MessageContext build();
//    	Builder message(InputStream is);
//    	Builder message(Source s);
//    	Builder message(SOAPMessage m);
//    	Builder transportHeader(String name, List<String> values);
//    	Builder attachment(String name, DataHandler dh);
//    }
}