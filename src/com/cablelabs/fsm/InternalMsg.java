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


/**
 * The container class for a STRING event for processing by the FSM.
 * 
 * @author ghassler
 *
 */
public class InternalMsg extends MsgEvent {

	/**
	 * The event
	 */
	private String sender = null;

	/**
	 * Constructor
	 * @param uid
	 * @param time
	 * @param name
	 */
	public InternalMsg(int uid, long time, int seq, String name) {
		super(uid, time, seq, null, null, 0, null, 0, name, null);
//		this.event = name;
	}
	
	/**
	 * Constructor
	 * @param uid
	 * @param time
	 * @param name
	 */
	public InternalMsg(int uid, long time, int seq, String name, String sender) {
		super(uid, time, seq, null, null, 0, null, 0, name, null);
		this.sender = sender;
	}
	
	/**
	 * Gets the event
	 * @return
	 */
	public String getEvent() {
		return super.getEventName();
	}
	
	/**
	 * Gets the name of the FSM that send this event
	 */
	public String getSender() {
	 	return this.sender;
	}
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = " Event=" + eventName + " Timestamp=" + timestamp 
			+ " msgQueueIndex=" + msgQueueIndex 
			+ " fsmUID=" + fsmUID 
			+ " sequencer=" + sequencer
			+ " transport=" + transport
			+ " srcIP=" + srcIP 
			+ " srcPort=" + srcPort 
			+ " destIP=" + destIP 
			+ " destPort=" + destPort 
			+ " duplicate=" + duplicate
		    + " sender=" + sender;
		return result;
	}
}
