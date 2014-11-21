/**
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Unpublished - rights reserved under the Copyright Laws of the United States.
 * Copyright © 2003 Sun Microsystems, Inc. All rights reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are subject 
 * to the Sun Microsystems, Inc. standard license agreement and applicable 
 * provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties. Sun, 
 * Sun Microsystems, the Sun logo, Java, Jini and JAIN are trademarks or 
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other 
 * countries.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Module Name   : JAIN SIP Specification
 * File Name     : RequestEvent.java
 * Author        : Phelim O'Doherty
 *
 *  HISTORY
 *  Version   Date      Author              Comments
 *  1.1     08/10/2002  Phelim O'Doherty    Initial version
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
package javax.sip;

import java.util.*;
import javax.sip.message.Request;

/**
 * This class represents an Request event that is passed from a SipProvider to
 * its SipListener. This specification handles the passing of request messages to the
 * application use the event model. An application (SipListener) will register
 * with the SIP protocol stack (SipProvider) and listen for Request events
 * from the SipProvider.
 * <p>
 * This specification defines a single Request event object to handle all Request
 * messages. The Request event encapsulates the Request message that can be 
 * retrieved from {@link RequestEvent#getRequest()}. Therefore the event type 
 * of a Request event can be determined as follows:
 * <p>
 * <i>eventType == RequestEvent.getRequest().getMethod();</i>
 * <p>
 * A Request event also encapsulates the server transaction which handles the
 * Request.
 * <p>
 * RequestEvent contains the following elements:
 * <ul>
 * <li>source - the source of the event i.e. the SipProvider sending the
 * RequestEvent
 * <li>serverTransaction - the server transaction this RequestEvent is 
 * associated with.
 * <li>Request - the Request message received on the SipProvider
 * that needs passed to the application encapsulated in a RequestEvent.
 * </ul>
 *
 * @author Sun Microsystems
 * @since v1.1
 */
public class RequestEvent extends EventObject {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
    * Constructs a RequestEvent encapsulating the Request that has been received
    * by the underlying SipProvider. This RequestEvent once created is passed to
    * {@link javax.sip.SipListener#processRequest(RequestEvent)} method of the SipListener
    * for application processing.
    *
    * @param source - the source of ResponseEvent i.e. the SipProvider
    * @param serverTransaction - server transaction upon which
    * this Request was sent
    * @param request - the Request message received by the SipProvider
    * @since PC 2.0
    * @param peerAddr - the source address for this message
    * @param peerPort - the source port for this message
    */
    public RequestEvent(Object source, ServerTransaction serverTransaction, Request request, 
    		String peerAddr, int port, int seq, 
    		String trans) {
        super(source);
        m_transaction = serverTransaction;
        m_request = request;
        peerAddress = peerAddr;
        peerPort = port;
        sequencer = seq;
        transport = trans;
   }

    /**
    * Gets the server transaction associated with this RequestEvent
    *
    * @return the server transaction associated with this RequestEvent
    */
    public ServerTransaction getServerTransaction(){
        return m_transaction;
    }

    /**
    * Gets the Request message associated with this RequestEvent.
    *
    * @return the message associated with this RequestEvent.
    */
    public Request getRequest() {
        return m_request;
    }

    /**
     * Gets the source InetAddress of the Request message
     * @since PC 2.0
     */
    public String getPeerAddress() {
    	return peerAddress;
    }
    
    /**
     * Gets the source port of the Request message
     * @since PC 2.0
     */
    public int getPeerPort() {
    	return peerPort;
    }
    
    /**
     * Gets the sequence number of the Request message
     * @since PC 2.0
     */
    public int getSequencer() {
    	return sequencer;
    }
    
    /**
     * Gets the transport protocol of the Request message
     * @since PC 2.0
     */
    public String getTransport() {
    	return transport;
    }
    
    // internal variables
    private Request m_request;
    private ServerTransaction m_transaction;
    // PC 2.0 added variables 
    private String peerAddress;
    private int peerPort;
    private int sequencer;
    private String transport;
}