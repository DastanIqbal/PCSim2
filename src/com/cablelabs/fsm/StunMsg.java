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

import com.cablelabs.common.Transport;
import com.cablelabs.stun.RawData;
import com.cablelabs.stun.StunEvent;
import com.cablelabs.stun.StunMessage;

/**
 * Container class for the STUN events sent/received by the platform.
 *  
 *  @author ghassler
 */
public class StunMsg extends MsgEvent {

	/**
	 * The STUN Event
	 */
	private StunEvent event = null;
	
	/**
	 * IP address is IPv6 format
	 */
	boolean ipV6 = false;
	
	/**
	 * The StunMessageProcessor ID that received/sent the original
	 * request
	 */
	private int processorID = 0;
	
	/**
	 * Constructor
	 * 
	 * @param uid - FSM's unique id that processed this event
	 * @param time - the time this class was constructed
	 * @param seq - the sequence number for the event
	 * @param t - the transport protocol used for the event
	 * @param srcIP - the source IP address that sent the message
	 * @param srcPort - the source port that sent the message
	 * @param destIP - the destination IP address that received the message
	 * @param destPort- the destination port that received the message
	 * @param event - the STUN event
	 * @param v6 - flag indicating whether the event is for IPv6 or not
	 * @param sent - whether this message was transmitted off the system
	 * 
	 */
	public StunMsg(int uid, long time, int seq, 
			Transport t, String srcIP,
			int srcPort, String destIP, int destPort,
			int id, StunEvent event, boolean v6, Boolean sent) {
		super(uid, time, seq, t, srcIP,
				srcPort, destIP, destPort, 
				event.getEvent().getName(), sent);
		this.processorID = id;
		this.event = event;
		this.ipV6 = v6;
	}
	
	/**
	 * Constructor
	 * 
	 * @param uid - FSM's unique id that processed this event
	 * @param time - the time this class was constructed
	 * @param seq - the sequence number for the event
	 * @param t - the transport protocol used for the event
	 * @param event - the STUN event
	 * @param id -
	 * @param v6 - flag indicating whether the event is for IPv6 or not
	 * @param sent - whether this message was transmitted off the system
	 * 
	 */
	public StunMsg(int uid, long time, int seq, 
			Transport t, StunEvent event,
			int id, boolean v6, Boolean sent) {
		super(uid, time, seq, t, event.getRawData().getSrcIP(),
				event.getRawData().getSrcPort(), 
				event.getRawData().getDestIP(), 
				event.getRawData().getDestPort(), 
				event.getEvent().getName(), sent);
		this.processorID = id;
		this.event = event;
		this.ipV6 = v6;
	}
	
	public StunEvent getEvent() {
		return this.event;
	}
	
	/**
	 * Gets the stun event
	 * @return
	 */
	public StunMessage getMessage() {
		return event.getEvent();
	}
	
	public RawData getRawData() {
		return event.getRawData();
	}
	
	/**
	 * Gets the stun event's message type
	 * @return
	 */
	public String getMsgType() {
		return super.getEventName();
	}
	
	public void setUID(int uid) {
		this.fsmUID = uid;
	}
	
	public int getID() {
		return processorID;
	}
	
	public boolean useIPV6() {
		return ipV6;
	}
}
