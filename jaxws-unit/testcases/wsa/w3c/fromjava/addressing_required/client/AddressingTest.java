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
 * Copyright 2010 Sun Microsystems Inc. All Rights Reserved
 */

package wsa.w3c.fromjava.addressing_required.client;

import junit.framework.TestCase;

import java.net.URL;

/**
 * @author Rama Pulavarthi
 */
public class AddressingTest extends TestCase {
    public AddressingTest(String name) {
            super(name);
        }

        private AddNumbers createStub() throws Exception {
            return new AddNumbersService().getAddNumbersPort();
        }

        public void testBasic() throws Exception {
            int result = createStub().addNumbers(10, 10);
            assertEquals(20, result);
        }

}
