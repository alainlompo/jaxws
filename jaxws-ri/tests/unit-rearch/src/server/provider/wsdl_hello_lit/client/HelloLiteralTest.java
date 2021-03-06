/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2004-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
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

package server.provider.wsdl_hello_lit.client;

import junit.framework.*;
import testutil.ClientServerTestUtil;
import javax.xml.ws.Service;
import javax.xml.namespace.QName;
import java.io.PrintStream;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.Random;

/**
 *
 * @author Jitendra Kotamraju
 */
public class HelloLiteralTest extends TestCase {
    private Hello stub;
    private Hello asyncStub;

    public HelloLiteralTest(String name) {
        super(name);
    }

    private Hello getStub(){
        if (stub != null) {
            return stub;
        }
        try {
            Hello_Service service = new Hello_Service();
            stub = service.getHelloPort();
            ClientServerTestUtil.setTransport(stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stub;
    }

    private Hello getAsyncStub(){
        if (asyncStub != null) {
            return asyncStub;
        }
        try {
            Hello_Service service = new Hello_Service();
            asyncStub = service.getHelloAsyncPort();
            ClientServerTestUtil.setTransport(asyncStub);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return asyncStub;
    }

    public void testHello() throws Exception {
        Hello stub = getStub();
        Hello_Type req = new Hello_Type();
        req.setArgument("arg");req.setExtra("extra");

        for(int i=0; i < 5; i++) {
            HelloResponse response = stub.hello(req, req);
            assertEquals("arg", response.getArgument());
            assertEquals("extra", response.getExtra());
        }
    }

    public void testHelloAsync() throws Exception {
        Hello asyncStub = getAsyncStub();
        Hello_Type req = new Hello_Type();
        req.setArgument("arg");req.setExtra("extra");

        for(int i=0; i < 5; i++) {
            HelloResponse response = asyncStub.hello(req, req);
            assertEquals("arg", response.getArgument());
            assertEquals("extra", response.getExtra());
        }
    }

    public void testFault() throws Exception {
        Hello stub = getStub();
        Hello_Type req = new Hello_Type();
        req.setArgument("fault");req.setExtra("extra");

        try {
            HelloResponse response = stub.hello(req, req);
        } catch (SOAPFaultException e) {
            SOAPFaultException se = (SOAPFaultException)e;
            assertEquals("Server was unable to process request. ---> Not a valid accountnumber.", se.getFault().getFaultString());
        }
    }

}
