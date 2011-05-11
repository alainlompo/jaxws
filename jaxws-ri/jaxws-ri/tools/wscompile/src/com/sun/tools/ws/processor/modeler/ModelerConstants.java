/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.ws.processor.modeler;


/**
 *
 * @author WS Development Team
 */
public interface ModelerConstants {

    public static final String BRACKETS  = "[]";
    public static final String FALSE_STR = "false";
    public static final String ZERO_STR  = "0";
    public static final String NULL_STR  = "null";
    public static final String ARRAY_STR = "Array";

    /*
     * Java ClassNames
     */
    /*
      * Java ClassNames
      */
     public static final String IOEXCEPTION_CLASSNAME         = "java.io.IOException";
     public static final String BOOLEAN_CLASSNAME             = "boolean";
     public static final String BOXED_BOOLEAN_CLASSNAME       = "java.lang.Boolean";
     public static final String BYTE_CLASSNAME                = "byte";
     public static final String BYTE_ARRAY_CLASSNAME          = BYTE_CLASSNAME+BRACKETS;
     public static final String BOXED_BYTE_CLASSNAME          = "java.lang.Byte";
     public static final String BOXED_BYTE_ARRAY_CLASSNAME    = BOXED_BYTE_CLASSNAME+BRACKETS;
     public static final String CLASS_CLASSNAME               = "java.lang.Class";
     public static final String CHAR_CLASSNAME                = "char";
     public static final String BOXED_CHAR_CLASSNAME          = "java.lang.Character";
     public static final String DOUBLE_CLASSNAME              = "double";
     public static final String BOXED_DOUBLE_CLASSNAME        = "java.lang.Double";
     public static final String FLOAT_CLASSNAME               = "float";
     public static final String BOXED_FLOAT_CLASSNAME         = "java.lang.Float";
     public static final String INT_CLASSNAME                 = "int";
     public static final String BOXED_INTEGER_CLASSNAME       = "java.lang.Integer";
     public static final String LONG_CLASSNAME                = "long";
     public static final String BOXED_LONG_CLASSNAME          = "java.lang.Long";
     public static final String SHORT_CLASSNAME               = "short";
     public static final String BOXED_SHORT_CLASSNAME         = "java.lang.Short";
     public static final String BIGDECIMAL_CLASSNAME          = "java.math.BigDecimal";
     public static final String BIGINTEGER_CLASSNAME          = "java.math.BigInteger";
     public static final String CALENDAR_CLASSNAME            = "java.util.Calendar";
     public static final String DATE_CLASSNAME                = "java.util.Date";
     public static final String STRING_CLASSNAME              = "java.lang.String";
     public static final String STRING_ARRAY_CLASSNAME        = STRING_CLASSNAME+BRACKETS;
     public static final String QNAME_CLASSNAME               = "javax.xml.namespace.QName";
     public static final String VOID_CLASSNAME                = "void";
     public static final String OBJECT_CLASSNAME              = "java.lang.Object";
     public static final String SOAPELEMENT_CLASSNAME         = "javax.xml.soap.SOAPElement";
     public static final String IMAGE_CLASSNAME               = "java.awt.Image";
     public static final String MIME_MULTIPART_CLASSNAME      = "javax.mail.internet.MimeMultipart";
     public static final String SOURCE_CLASSNAME              = "javax.xml.transform.Source";
     public static final String DATA_HANDLER_CLASSNAME        = "javax.activation.DataHandler";
     public static final String URI_CLASSNAME                 = "java.net.URI";
//     public static final String URI_CLASSNAME                  = "java.lang.String";
     // Collections
     public static final String COLLECTION_CLASSNAME          = "java.util.Collection";
     public static final String LIST_CLASSNAME                = "java.util.List";
     public static final String SET_CLASSNAME                 = "java.util.Set";
     public static final String VECTOR_CLASSNAME              = "java.util.Vector";
     public static final String STACK_CLASSNAME               = "java.util.Stack";
     public static final String LINKED_LIST_CLASSNAME         = "java.util.LinkedList";
     public static final String ARRAY_LIST_CLASSNAME          = "java.util.ArrayList";
     public static final String HASH_SET_CLASSNAME            = "java.util.HashSet";
     public static final String TREE_SET_CLASSNAME            = "java.util.TreeSet";

     // Maps
     public static final String MAP_CLASSNAME                 = "java.util.Map";
     public static final String HASH_MAP_CLASSNAME            = "java.util.HashMap";
     public static final String TREE_MAP_CLASSNAME            = "java.util.TreeMap";
     public static final String HASHTABLE_CLASSNAME           = "java.util.Hashtable";
     public static final String PROPERTIES_CLASSNAME          = "java.util.Properties";
//     public static final String WEAK_HASH_MAP_CLASSNAME       = "java.util.WeakHashMap";
     public static final String JAX_WS_MAP_ENTRY_CLASSNAME   = "com.sun.xml.ws.encoding.soap.JAXWSMapEntry";

}
