/**
 * $Id: JAXBType.java,v 1.1 2005-05-23 23:18:53 bbissett Exp $
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.tools.ws.processor.model.jaxb;

import com.sun.tools.ws.processor.model.AbstractType;
import com.sun.tools.ws.processor.model.java.JavaType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level binding between JAXB generated Java type
 * and XML Schema element declaration.
 *
 * @author
 *     Vivek Pandey
 */
public class JAXBType extends AbstractType {
    public JAXBType(JAXBType jaxbType){
        setName(jaxbType.getName());
        this.jaxbMapping = jaxbType.getJaxbMapping();
        this.jaxbModel = jaxbType.getJaxbModel();
        init();
    }

    public JAXBType(){}

    public JAXBType(QName name, JavaType javaType){
        super(name, javaType);
    }

    public JAXBType(QName name, JavaType javaType, JAXBMapping jaxbMapping, JAXBModel jaxbModel){
        super(name, javaType);
        this.jaxbMapping = jaxbMapping;
        this.jaxbModel = jaxbModel;
        init();
    }

    public void accept(JAXBTypeVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    private void init() {
        if (jaxbMapping != null)
            wrapperChildren = jaxbMapping.getWrapperStyleDrilldown();
        else
            wrapperChildren =  new ArrayList<JAXBProperty>();
    }

    public boolean isUnwrappable(){
        return getJaxbMapping().getWrapperStyleDrilldown() != null;
    }

    public boolean hasWrapperChildren(){
        return (getWrapperChildren().size() > 0) ? true : false;
    }

    public boolean isLiteralType() {
        return true;
    }

    public List<JAXBProperty> getWrapperChildren(){
/*        List<JAXBProperty> props = null;
        if (jaxbMapping != null)
            props = jaxbMapping.getWrapperStyleDrilldown();
        if (props == null)
            return new ArrayList<JAXBProperty>();
        return props;*/
        return wrapperChildren;
    }

    public void setWrapperChildren(List<JAXBProperty> children) {
        wrapperChildren = children;
    }

    public JAXBMapping getJaxbMapping() {
        //return jaxbModel.get(getName());
        return jaxbMapping;
    }

    public void setJaxbMapping(JAXBMapping jaxbMapping) {
        this.jaxbMapping = jaxbMapping;
        init();
    }

    public void setUnwrapped(boolean unwrapped) {
        this.unwrapped = unwrapped;
    }

    public boolean isUnwrapped() {
        return unwrapped;
    }

    private JAXBMapping jaxbMapping;

    public JAXBModel getJaxbModel() {
        return jaxbModel;
    }

    public void setJaxbModel(JAXBModel jaxbModel) {
        this.jaxbModel = jaxbModel;
    }

    private JAXBModel jaxbModel;
    private boolean unwrapped = false;
    private List<JAXBProperty> wrapperChildren;
}
