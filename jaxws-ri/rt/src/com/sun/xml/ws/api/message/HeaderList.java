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
package com.sun.xml.ws.api.message;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.addressing.WsaPipeHelper;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.addressing.OneWayReplyToFeature;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.message.StringHeader;
import com.sun.xml.ws.protocol.soap.ClientMUPipe;
import com.sun.xml.ws.protocol.soap.ServerMUPipe;
import com.sun.xml.ws.resources.AddressingMessages;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A list of {@link Header}s on a {@link Message}.
 *
 * <p>
 * This list can be modified to add headers
 * from outside a {@link Message}, this is necessary
 * since intermediate processing layers often need to
 * put additional headers.
 *
 * <p>
 * Following the SOAP convention, the order among headers
 * are not significant. However, {@link Decoder}s are
 * expected to preserve the order of headers in the input
 * message as much as possible.
 *
 *
 * <a name="MU"></a>
 * <h3>MustUnderstand Processing</h3>
 * <p>
 * To perform SOAP mustUnderstang processing correctly, we need to keep
 * track of headers that are understood and headers that are not.
 * This is a collaborative process among {@link Pipe}s, thus it's something
 * a {@link Pipe} author needs to keep in mind.
 *
 * <p>
 * Specifically, when a {@link Pipe} sees a header and processes it
 * (that is, if it did enough computing with the header to claim that
 * the header is understood), then it should mark the corresponding
 * header as "understood". For example, when a pipe that handles JAX-WSA
 * examins the &lt;wsa:To> header, it can claim that it understood the header.
 * But for example, if a pipe that does the signature verification checks
 * &lt;wsa:To> for a signature, that would not be considered as "understood".
 *
 * <p>
 * There are two ways to mark a header as understood:
 *
 * <ol>
 *  <li>Use one of the <tt>getXXX</tt> methods that take a
 *      boolean <tt>markAsUnderstood</tt> parameter.
 *      Most often, a {@link Pipe} knows it's going to understand a header
 *      as long as it's present, so this is the easiest and thus the preferred way.
 *
 *      For example, if JAX-WSA looks for &lt;wsa:To>, then it can set
 *      <tt>markAsUnderstand</tt> to true, to do the obtaining of a header
 *      and marking at the same time.
 *
 *  <li>Call {@link #understood(int)}.
 *      If under a rare circumstance, a pipe cannot determine whether
 *      it can understand it or not when you are fetching a header, then
 *      you can use this method afterward to mark it as understood.
 * </ol>
 *
 * <p>
 * Intuitively speaking, at the end of the day, if a header is not
 * understood but {@link Header#isIgnorable(SOAPVersion, Set)} is false, a bad thing
 * will happen. The actual implementation of the checking is more complicated,
 * for that see {@link ClientMUPipe}/{@link ServerMUPipe}.
 *
 * @see Message#getHeaders()
 */
public final class HeaderList extends ArrayList<Header> {

    /**
     * Bit set to keep track of which headers are understood.
     * <p>
     * The first 32 headers use this field, and the rest will use
     * {@link #moreUnderstoodBits}. The expectation is that
     * most of the time a SOAP message will only have up to 32 headers,
     * so we can avoid allocating separate objects for {@link BitSet}.
     */
    private int understoodBits;
    /**
     * If there are more than 32 headers, we use this {@link BitSet}
     * to keep track of whether those headers are understood.
     * Lazily allocated.
     */
    private BitSet moreUnderstoodBits = null;

    private String to = null;
    private String action = null;
    private WSEndpointReference replyTo = null;
    private WSEndpointReference faultTo = null;
    private String messageId;


    /**
     * Creates an empty {@link HeaderList}.
     */
    public HeaderList() {
    }

    /**
     * Copy constructor.
     */
    public HeaderList(HeaderList that) {
        super(that);
        this.understoodBits = that.understoodBits;
        if(that.moreUnderstoodBits!=null)
            this.moreUnderstoodBits = (BitSet)that.moreUnderstoodBits.clone();
        this.to = that.to;
        this.action = that.action;
        this.replyTo = that.replyTo;
        this.faultTo = that.faultTo;
        this.messageId = that.messageId;
    }

    /**
     * The number of total headers.
     */
    public int size() {
        return super.size();
    }

    /**
     * Gets the {@link Header} at the specified index.
     *
     * <p>
     * This method does not mark the returned {@link Header} as understood.
     *
     * @see #understood(int)
     */
    public Header get(int index) {
        return super.get(index);
    }

    /**
     * Marks the {@link Header} at the specified index as
     * <a href="#MU">"understood"</a>.
     */
    public void understood(int index) {
        assert index<size();    // check that index is in range
        if(index<32)
            understoodBits |= 1<<index;
        else {
            if(moreUnderstoodBits==null)
                moreUnderstoodBits = new BitSet();
            moreUnderstoodBits.set(index-32);
        }
    }

    /**
     * Returns true if a {@link Header} at the given index
     * was <a href="#MU">"understood"</a>.
     */
    public boolean isUnderstood(int index) {
        assert index<size();    // check that index is in range
        if(index<32)
            return understoodBits == (understoodBits|(1<<index));
        else {
            if(moreUnderstoodBits==null)
                return false;
            return moreUnderstoodBits.get(index-32);
        }
    }

    /**
     * Marks the specified {@link Header} as <a href="#MU">"understood"</a>.
     *
     * @deprecated
     * By the deifnition of {@link ArrayList}, this operation requires
     * O(n) search of the array, and thus inherently inefficient.
     *
     * Because of this, if you are developing a {@link Pipe} for
     * a performance sensitive environment, do not use this method.
     *
     * @throws IllegalArgumentException
     *      if the given header is not {@link #contains(Object) contained}
     *      in this header.
     */
    public void understood(@NotNull Header header) {
        int sz = size();
        for( int i=0; i<sz; i++ ) {
            if(get(i)==header) {
                understood(i);
                return;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Gets the first {@link Header} of the specified name.
     *
     * @param markAsUnderstood
     *      If this parameter is true, the returned header will
     *      be marked as <a href="#MU">"understood"</a>.
     * @return null if not found.
     */
    public @Nullable Header get(@NotNull String nsUri, @NotNull String localName, boolean markAsUnderstood) {
        int len = size();
        for( int i=0; i<len; i++ ) {
            Header h = get(i);
            if(h.getLocalPart().equals(localName) && h.getNamespaceURI().equals(nsUri)) {
                if(markAsUnderstood)
                    understood(i);
                return h;
            }
        }
        return null;
    }

    /**
     * @deprecated
     *      Use {@link #get(String, String, boolean)}
     */
    public Header get(String nsUri, String localName) {
        return get(nsUri,localName,true);
    }

    /**
     * Gets the first {@link Header} of the specified name.
     *
     * @param markAsUnderstood
     *      If this parameter is true, the returned header will
     *      be marked as <a href="#MU">"understood"</a>.
     * @return null
     *      if not found.
     */
    public @Nullable Header get(@NotNull QName name, boolean markAsUnderstood) {
        return get(name.getNamespaceURI(),name.getLocalPart(),markAsUnderstood);
    }

    /**
     * @deprecated
     *      Use {@link #get(QName)}
     */
    public @Nullable Header get(@NotNull QName name) {
        return get(name,true);
    }

    /**
     * @deprecated
     *      Use {@link #getHeaders(String, String, boolean)}
     */
    public Iterator<Header> getHeaders(final String nsUri, final String localName) {
        return getHeaders(nsUri,localName,true);
    }

    /**
     * Gets all the {@link Header}s of the specified name,
     * including duplicates (if any.)
     *
     * @param markAsUnderstood
     *      If this parameter is true, the returned headers will
     *      be marked as <a href="#MU">"understood"</a> when they are returned
     *      from {@link Iterator#next()}.
     * @return empty iterator if not found.
     */
    public @NotNull Iterator<Header> getHeaders(@NotNull final String nsUri, @NotNull final String localName, final boolean markAsUnderstood) {
        return new Iterator<Header>() {
            int idx = 0;
            Header next;
            public boolean hasNext() {
                if(next==null)
                    fetch();
                return next!=null;
            }

            public Header next() {
                if(next==null) {
                    fetch();
                    if(next==null)
                        throw new NoSuchElementException();
                }

                if(markAsUnderstood) {
                    assert get(idx-1)==next;
                    understood(idx-1);
                }

                Header r = next;
                next = null;
                return r;
            }

            private void fetch() {
                while(idx<size()) {
                    Header h = get(idx++);
                    if(h.getLocalPart().equals(localName) && h.getNamespaceURI().equals(nsUri)) {
                        next = h;
                        break;
                    }
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * @see #getHeaders(String, String, boolean)
     */
    public @NotNull Iterator<Header> getHeaders(@NotNull QName headerName, final boolean markAsUnderstood) {
        return getHeaders(headerName.getNamespaceURI(),headerName.getLocalPart(),markAsUnderstood);
    }

    /**
     * @deprecated
     *      use {@link #getHeaders(String, boolean)}.
     */
    public @NotNull Iterator<Header> getHeaders(@NotNull final String nsUri) {
        return getHeaders(nsUri,true);
    }

    /**
     * Gets an iteration of headers {@link Header} in the specified namespace,
     * including duplicates (if any.)
     *
     * @param markAsUnderstood
     *      If this parameter is true, the returned headers will
     *      be marked as <a href="#MU">"understood"</a> when they are returned
     *      from {@link Iterator#next()}.
     * @return
     *      empty iterator if not found.
     */
    public @NotNull Iterator<Header> getHeaders(@NotNull final String nsUri, final boolean markAsUnderstood) {
        return new Iterator<Header>() {
            int idx = 0;
            Header next;
            public boolean hasNext() {
                if(next==null)
                    fetch();
                return next!=null;
            }

            public Header next() {
                if(next==null) {
                    fetch();
                    if(next==null)
                        throw new NoSuchElementException();
                }

                if(markAsUnderstood) {
                    assert get(idx-1)==next;
                    understood(idx-1);
                }

                Header r = next;
                next = null;
                return r;
            }

            private void fetch() {
                while(idx<size()) {
                    Header h = get(idx++);
                    if(h.getNamespaceURI().equals(nsUri)) {
                        next = h;
                        break;
                    }
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Gets the first {@link Header} of the specified name targeted at the
     * current implicit role.
     *
     * @param name name of the header
     * @param markUnderstood
     *      If this parameter is true, the returned headers will
     *      be marked as <a href="#MU">"understood"</a> when they are returned
     *      from {@link Iterator#next()}.
     * @return null if header not found
     */
    private Header getFirstHeader(QName name, boolean markUnderstood, SOAPVersion soapVersion) {
        Iterator<Header> iter = getHeaders(name.getNamespaceURI(), name.getLocalPart(), markUnderstood);
        while (iter.hasNext()) {
            Header h = iter.next();
            if (h.getRole(soapVersion) == soapVersion.implicitRole)
                return h;
        }

        return null;
    }

    /**
     * Returns the value of WS-Addressing <code>To</code> header. The <code>version</code>
     * identifies the WS-Addressing version and the header returned is targeted at
     * the current implicit role. Caches the value for subsequent invocation.
     * Duplicate <code>To</code> headers are detected earlier.
     *
     * @param version WS-Addressing version
     * @param soapVersion SOAP version
     * @return Value of WS-Addressing To header, null if no header is present
     */
    public String getTo(AddressingVersion version, SOAPVersion soapVersion) {
        if (to != null)
            return to;
        Header h = getFirstHeader(version.toTag, true, soapVersion);
        if (h != null) {
            to = h.getStringContent();
        }

        return to;
    }

    /**
     * Returns the value of WS-Addressing <code>Action</code> header. The <code>version</code>
     * identifies the WS-Addressing version and the header returned is targeted at
     * the current implicit role. Caches the value for subsequent invocation.
     * Duplicate <code>Action</code> headers are detected earlier.
     *
     * @param version WS-Addressing version
     * @param soapVersion SOAP version
     * @return Value of WS-Addressing Action header, null if no header is present
     */
    public String getAction(AddressingVersion version, SOAPVersion soapVersion) {
        if (action!= null)
            return action;
        Header h = getFirstHeader(version.actionTag, true, soapVersion);
        if (h != null) {
            action = h.getStringContent();
        }

        return action;
    }

    /**
     * Returns the value of WS-Addressing <code>ReplyTo</code> header. The <code>version</code>
     * identifies the WS-Addressing version and the header returned is targeted at
     * the current implicit role. Caches the value for subsequent invocation.
     * Duplicate <code>ReplyTo</code> headers are detected earlier.
     *
     * @param version WS-Addressing version
     * @param soapVersion SOAP version
     * @return Value of WS-Addressing ReplyTo header, null if no header is present
     */
    public WSEndpointReference getReplyTo(AddressingVersion version, SOAPVersion soapVersion) {
        if (replyTo!=null)
            return replyTo;
        Header h = getFirstHeader(version.replyToTag, true, soapVersion);
        if (h != null) {
            try {
                replyTo = h.readAsEPR(version);
            } catch (XMLStreamException e) {
                throw new WebServiceException(AddressingMessages.REPLY_TO_CANNOT_PARSE(), e);
            }
        }

        return replyTo;
    }

    /**
     * Returns the value of WS-Addressing <code>FaultTo</code> header. The <code>version</code>
     * identifies the WS-Addressing version and the header returned is targeted at
     * the current implicit role. Caches the value for subsequent invocation.
     * Duplicate <code>FaultTo</code> headers are detected earlier.
     *
     * @param version WS-Addressing version
     * @param soapVersion SOAP version
     * @return Value of WS-Addressing FaultTo header, null if no header is present
     */
    public WSEndpointReference getFaultTo(AddressingVersion version, SOAPVersion soapVersion) {
        if (faultTo != null)
            return faultTo;

        Header h = getFirstHeader(version.faultToTag, true, soapVersion);
        if (h != null) {
            try {
                faultTo = h.readAsEPR(version);
            } catch (XMLStreamException e) {
                throw new WebServiceException(AddressingMessages.FAULT_TO_CANNOT_PARSE(), e);
            }
        }

        return faultTo;
    }

    /**
     * Returns the value of WS-Addressing <code>MessageID</code> header. The <code>version</code>
     * identifies the WS-Addressing version and the header returned is targeted at
     * the current implicit role. Caches the value for subsequent invocation.
     * Duplicate <code>MessageID</code> headers are detected earlier.
     *
     * @param version WS-Addressing version
     * @param soapVersion SOAP version
     * @return Value of WS-Addressing MessageID header, null if no header is present
     */
    public String getMessageID(AddressingVersion version, SOAPVersion soapVersion) {
        if (messageId != null)
            return messageId;

        Header h = getFirstHeader(version.messageIDTag, true, soapVersion);
        if (h != null) {
            messageId = h.getStringContent();
        }

        return messageId;
    }

    /**
     * Creates a set of outbound WS-Addressing headers on the client with the
     * default Action Message Addressing Property value.
     * <p><p>
     * This method needs to be invoked right after such a Message is
     * created which is error prone but so far only MEX, RM and JAX-WS
     * creates a request so this ugliness is acceptable. If more components
     * are identified using this, then we may revisit this.
     * <p><p>
     * This method is used if default Action Message Addressing Property is to
     * be used. See
     * {@link #fillRequestAddressingHeaders(Packet, com.sun.xml.ws.api.addressing.AddressingVersion, com.sun.xml.ws.api.SOAPVersion, boolean, String)}
     * if non-default Action is to be used, for example when creating a protocol message not
     * associated with {@link WSBinding} and {@link WSDLPort}.
     *
     * @param wsdlPort request WSDL port
     * @param binding request WSBinding
     * @param packet request packet
     */
    public void fillRequestAddressingHeaders(WSDLPort wsdlPort, WSBinding binding, Packet packet) {
        AddressingVersion ver = binding.getAddressingVersion();
        WsaPipeHelper wsaHelper = ver.getWsaHelper(wsdlPort, binding);

        // wsa:Action
        // todo: abstract the algorithm of getting input action in getInputAction
        String action = wsaHelper.getInputAction(packet);
        if (wsaHelper.isInputActionDefault(packet) && (packet.soapAction != null && !packet.soapAction.equals(""))) {
            action = packet.soapAction;
        }
        if (action == null)
            action = AddressingVersion.UNSET_INPUT_ACTION;

        boolean oneway = !(wsdlPort != null && !packet.getMessage().isOneWay(wsdlPort));
        OneWayReplyToFeature onewayFeature = null;
        if (binding.getFeature(OneWayReplyToFeature.ID) != null)
            onewayFeature = (OneWayReplyToFeature) binding.getFeature(OneWayReplyToFeature.ID);

        if (onewayFeature == null)
            fillRequestAddressingHeaders(packet, binding.getAddressingVersion(), binding.getSOAPVersion(), oneway, action);
        else {
            fillRequestAddressingHeaders(packet, binding.getAddressingVersion(), binding.getSOAPVersion(), onewayFeature.getAddress(), action);
        }
    }

    /**
     * Creates a set of outbound WS-Addressing headers on the client with the
     * specified Action Message Addressing Property value.
     * <p><p>
     * This method needs to be invoked right after such a Message is
     * created which is error prone but so far only MEX, RM and JAX-WS
     * creates a request so this ugliness is acceptable. This method is used
     * to create protocol messages that are not associated with
     * any {@link WSBinding} and {@link WSDLPort}.
     *
     * @param packet request packet
     * @param av WS-Addressing version
     * @param sv SOAP version
     * @param oneway Indicates if the message exchange pattern is oneway
     * @param action Action Message Addressing Property value
     */
    public void fillRequestAddressingHeaders(Packet packet, AddressingVersion av, SOAPVersion sv, boolean oneway, String action) {
        fillCommonAddressingHeaders(packet, av, sv, action);

        // wsa:ReplyTo
        // null or "true" is equivalent to request/response MEP
        if (!oneway) {
            WSEndpointReference epr = av.anonymousEpr;
            add(epr.createHeader(av.replyToTag));
        }
    }

    private void fillRequestAddressingHeaders(Packet packet, AddressingVersion av, SOAPVersion sv, String onewayReplyToAddress, String action) {
        fillCommonAddressingHeaders(packet, av, sv, action);

        WSEndpointReference epr = av.anonymousEpr;
        // TODO: replace anonymous EPR with onewayReplyToAddress value
        add(epr.createHeader(av.replyToTag));
    }

    /**
     * Creates wsa:To, wsa:Action and wsa:MessageID header on the client
     *
     * @param packet request packet
     * @param av WS-Addressing version
     * @param sv SOAP version
     * @param action Action Message Addressing Property value
     */
    private void fillCommonAddressingHeaders(Packet packet, AddressingVersion av, SOAPVersion sv, String action) {
        // wsa:To
        StringHeader h = new StringHeader(av.toTag, packet.endpointAddress.toString());
        add(h);

        // wsa:Action
        packet.soapAction = action;
        h = new StringHeader(av.actionTag, action);
        add(h);

        // wsa:MessageID
        h = new StringHeader(av.messageIDTag, packet.getMessage().getID(av, sv));
        add(h);
    }

    /**
     * Adds a new {@link Header}.
     *
     * <p>
     * Order doesn't matter in headers, so this method
     * does not make any guarantee as to where the new header
     * is inserted.
     *
     * @return
     *      always true. Don't use the return value.      
     */
    public boolean add(Header header) {
        return super.add(header);
    }

    /**
     * @deprecated
     *      {@link HeaderList} is monotonic and you can't remove anything.
     */
    // to allow this, we need to implement the resizing of understoodBits
    public Header remove(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated
     *      {@link HeaderList} is monotonic and you can't remove anything.
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated
     *      {@link HeaderList} is monotonic and you can't remove anything.
     */
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated
     *      {@link HeaderList} is monotonic and you can't remove anything.
     */
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a copy.
     *
     * This handles null {@link HeaderList} correctly.
     *
     * @param original
     *      Can be null, in which case null will be returned.
     */
    public static HeaderList copy(HeaderList original) {
        if(original==null)
            return null;
        else
            return new HeaderList(original);
    }
}
