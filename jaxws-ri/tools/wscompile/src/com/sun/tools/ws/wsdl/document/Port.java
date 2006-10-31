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

package com.sun.tools.ws.wsdl.document;

import com.sun.tools.ws.api.wsdl.TWSDLExtensible;
import com.sun.tools.ws.api.wsdl.TWSDLExtension;
import com.sun.tools.ws.wsdl.framework.*;
import org.xml.sax.Locator;

import javax.xml.namespace.QName;

/**
 * Entity corresponding to the "port" WSDL element.
 *
 * @author WS Development Team
 */
public class Port extends GlobalEntity implements TWSDLExtensible {

    public Port(Defining defining, Locator locator) {
        super(defining, locator);
        _helper = new ExtensibilityHelper();
    }

    public Service getService() {
        return _service;
    }

    public void setService(Service s) {
        _service = s;
    }

    public QName getBinding() {
        return _binding;
    }

    public void setBinding(QName n) {
        _binding = n;
    }

    public Binding resolveBinding(AbstractDocument document) {
        return (Binding) document.find(Kinds.BINDING, _binding);
    }

    public Kind getKind() {
        return Kinds.PORT;
    }

    public String getNameValue() {
        return getName();
    }

    public String getNamespaceURI() {
        return getDefining().getTargetNamespaceURI();
    }

    public QName getWSDLElementName() {
        return WSDLConstants.QNAME_PORT;
    }

    public Documentation getDocumentation() {
        return _documentation;
    }

    public void setDocumentation(Documentation d) {
        _documentation = d;
    }

    public void withAllQNamesDo(QNameAction action) {
        super.withAllQNamesDo(action);

        if (_binding != null) {
            action.perform(_binding);
        }
    }

    public void withAllEntityReferencesDo(EntityReferenceAction action) {
        super.withAllEntityReferencesDo(action);
        if (_binding != null) {
            action.perform(Kinds.BINDING, _binding);
        }
    }

    public void accept(WSDLDocumentVisitor visitor) throws Exception {
        visitor.preVisit(this);
        _helper.accept(visitor);
        visitor.postVisit(this);
    }

    public void validateThis() {
        if (getName() == null) {
            failValidation("validation.missingRequiredAttribute", "name");
        }
        if (_binding == null) {
            failValidation("validation.missingRequiredAttribute", "binding");
        }
    }

    public void addExtension(TWSDLExtension e) {
        _helper.addExtension(e);
    }

    public Iterable<TWSDLExtension> extensions() {
        return _helper.extensions();
    }

    public TWSDLExtensible getParent() {
        return parent;
    }

    public void setParent(TWSDLExtensible parent) {
        this.parent = parent;
    }

    public void withAllSubEntitiesDo(EntityAction action) {
        _helper.withAllSubEntitiesDo(action);
    }

    private ExtensibilityHelper _helper;
    private Documentation _documentation;
    private Service _service;
    private QName _binding;

    public QName getElementName() {
        return getWSDLElementName();
    }

    private TWSDLExtensible parent;
}
