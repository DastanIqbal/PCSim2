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
import com.cablelabs.utility.UtilityEvent;
/**
 * A container class to hold a Utility Event received/sent by the platform.
 * 
 * @author ghassler
 *
 */
public class UtilityMsg extends MsgEvent {

	/**
	 * The event
	 */
	protected UtilityEvent utilityEvent = null;

	/**
	 * Constructor
	 * @param uid - FSM's unique id that processed this event
	 * @param time - the time this class was constructed
	 * @param srcIP - the source IP address that sent the message
	 * @param srcPort - the source port that sent the message
	 * @param destIP - the destination IP address that received the message
	 * @param destPort- the destination port that received the message
	 * @param event - the Utility event type
	 * @param sent - whether this message was transmitted off the system
	 */
	public UtilityMsg(int uid, long time, int seq, Transport t, String srcIP, 
			int srcPort, String destIP, int destPort, String event, Boolean sent) {
		super(uid, time, seq, t, srcIP, srcPort, destIP, destPort,
				event, sent);
	}
	
	/**
	 * Constructor
	 * @param uid - FSM's unique id that processed this event
	 * @param time - the time this class was constructed
	 * @param srcIP - the source IP address that sent the message
	 * @param srcPort - the source port that sent the message
	 * @param destIP - the destination IP address that received the message
	 * @param destPort- the destination port that received the message
	 * @param event - the Utility event type
	 * @param sent - whether this message was transmitted off the system
	 */
	public UtilityMsg(int uid, long time, int seq, 
			Transport t, String srcIP, 
			int srcPort, String destIP, int destPort, UtilityEvent event, Boolean sent) {
		super(uid, time, seq, t, srcIP, srcPort, destIP, destPort, event.getMessage().getType(), sent);
		this.utilityEvent = event;
	}
	
	/**
	 * Constructor
	 * @param uid - FSM's unique id that processed this event
	 * @param time - the time this class was constructed
	 * @param event - the Utility event
	 * @param sent - whether this message was transmitted off the system
	 * 
	 */
	public UtilityMsg(int uid, long time, int seq, 
			Transport t, UtilityEvent event, Boolean sent) {
		super(uid, time, seq, t, event.getSrcIP(), event.getSrcPort(), 
				event.getDestIP(), event.getDestPort(),
				 event.getMessage().getType(), sent);
		this.utilityEvent = event;
	}
	
	/**
	 * Gets the event
	 * @return
	 */
	public String getEventType() {
		return super.getEventName();
	}

	public UtilityEvent getUtilityEvent() {
		return this.utilityEvent;
	}
}
