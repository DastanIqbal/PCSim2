/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.cablelabs.common.Transport;

/**
 * A container class to hold a SIP Event received/sent by the platform.
 * 
 * @author ghassler
 *
 */
public class SIPMsg extends MsgEvent {

	/**
	 * The SIP request message sent/received
	 */
	private Request req = null;
	
	/**
	 * The SIP response messge sent/received
	 */
	private Response resp = null;
	
	/**
	 * The modified version of the message sent to the peer
	 * network element.
	 */
	private String sentMsg = null;
	
	/**
	 * The SIP Dialog key is a unique id for the stack to
	 * maintain information about this leg of a dialog. Some
	 * of the things is the specific instance of SIP stack
	 * being used, the call id, and the transport protocol to
	 * name a few.
	 */
	private String dialogKey = null;
	
	/**
	 * This indicates the event type of the a Subscribe request.
	 * This allows the system to match Notifies to the correct 
	 * type of Subscribe.
	 */
	private String subscribeType = null;
	
	/**
	 * Constructor
	 * @param uid - FSM's unique id that processed this event
	 * @param time - the time this class was constructed
	 * @param srcIP - the source IP address that sent the message
	 * @param srcPort - the source port that sent the message
	 * @param destIP - the destination IP address that received the message
	 * @param destPort- the destination port that received the message
	 * @param req - the request message
	 * @param sentMsg - the String representation of the actual message event
	 * @param subscribeType - The subscribe's event type.
	 * @param sent - whether this message was transmitted off the system
	 */
	public SIPMsg(int uid, long time, int seq,
				String key, Transport t, String srcIP, 
				int srcPort, String destIP, int destPort, 
				Request req, String sentMsg, String subscribeType, Boolean sent) {
		super(uid, time, seq, t, srcIP, srcPort, destIP, destPort, req.getMethod(), sent);
		this.req = req;
		this.sentMsg = sentMsg;
		this.dialogKey = key;
		this.subscribeType = subscribeType;
	}

	/**
	 * Constructor allows the platform to set the event to a different value
	 * then the value received in the method. This is currently only used in the
	 * case of REINVITE.
	 * @param uid - FSM's unique id that processed this event
	 * @param time - the time this class was constructed
	 * @param srcIP - the source IP address that sent the message
	 * @param srcPort - the source port that sent the message
	 * @param destIP - the destination IP address that received the message
	 * @param destPort- the destination port that received the message
	 * @param req - the request message]
	 * @param sentMsg - the String representation of the actual message event
	 * @param subscribeType - The subscribe's event type.
	 * @param event - the name of the event that the system should use (eg Reinvite)
	 * @param sent - whether this message was transmitted off the system
	 */
	public SIPMsg(int uid, long time, int seq,
				String key, Transport t, String srcIP, 
				int srcPort, String destIP, int destPort, 
				Request req, String sentMsg, String subscribeType,  
				String event, Boolean sent) {
		super(uid, time, seq, t, srcIP, srcPort, destIP, destPort, event, sent);
		this.req = req;
		this.sentMsg = sentMsg;
		this.dialogKey = key;
		this.subscribeType = subscribeType;
	}
	
	/**
	 * Constructor
	 * @param uid - FSM's unique id that processed this event
	 * @param time - the time this class was constructed
	 * @param srcIP - the source IP address that sent the message
	 * @param srcPort - the source port that sent the message
	 * @param destIP - the destination IP address that received the message
	 * @param destPort- the destination port that received the message
	 * @param resp - the response message
	 * @param sentMsg - the String representation of the actual message event
	 * @param sent - whether this message was transmitted off the system
	 */
	public SIPMsg(int uid, long time, int seq, String key, 
			Transport t, String srcIP, 
			int srcPort, String destIP, int destPort, 
			Response resp, String sentMsg, Boolean sent) {
		super(uid, time, seq, t, srcIP, srcPort, destIP, destPort, 
				Integer.toString(resp.getStatusCode()) + "-" + 
				  ((CSeqHeader) resp.getHeader(CSeqHeader.NAME)).getMethod(), sent);
		this.resp = resp;
		this.sentMsg = sentMsg;
		this.dialogKey = key;
	}

	/**
	 * Gets the request message
	 */
	public Request getRequest() {
		return req;
	}
	
	/**
	 * Gets the response message
	 */
	public Response getResponse() {
	 
		return resp;
	}
	
	/**
	 * Gets the dialog key associated with this message
	 * @return
	 */
	public String getDialogKey() {
		return dialogKey;
	}

	/**
	 * Determines if this encapulates a SIP Request message
	 * @return
	 */
	public boolean isRequestMsg() {
		if (req != null) 
		    return true;
		return false;
	}
	
	/**
	 * Determines if this encapulates a SIP Request message
	 * @return
	 */
	public boolean isResponseMsg() {
		if (resp != null)
			return true;
		return false;
	}
	
	public String getSentMsg() {
		return sentMsg;
	}
	
	public boolean hasSentMsg() {
		if (sentMsg != null) 
			return true;
		return false;
	}
	
	public String getEvent() {
		return super.getEventName();
	}
	
	public void setSubscribeType(String type) {
		this.subscribeType = type;
	}
	
	public String getSubscribeType() {
		return this.subscribeType;
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = "SIPMsg " + super.toString() 
			+ " with key( " + dialogKey + ") ";
		
		if (subscribeType != null)
			result += "and subscribeType=" + subscribeType;
		
		result += 		"for \n";
		if (sentMsg != null)
			result += "\nSentMsg :\n" + sentMsg;
		else if (req != null)
			result += "\nRequest :\n" + req;
		else if (resp != null)
			result += "\nResponse :\n" + resp;
		
		
		return result;
	}
}

