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
 * This is the base class for all Message Events that will be
 * processed by the FSM. It contains information that will be
 * supplied by the creator of the event such as the time the 
 * event was created, the originating FSM's unique id, as well
 * as the source address and destination address information.
 * 
 * @author ghassler
 *
 */
import com.cablelabs.common.Transport;

public class MsgEvent {

	/**
	 * The name of this event.
	 */
	protected String eventName = null;

	/**
	 * The timestamp the event was created.
	 */
	protected long timestamp = 0;
	
	/**
	 * The FSM's unique ID that will process or that generated
	 * the event.
	 */
	protected int fsmUID = 0;
	
	/**
	 * The source IP address of the event when it is an external
	 * event.
	 */
	protected String srcIP = null;
	
	/**
	 * The source port of the event when it is an external event.
	 */
	protected int srcPort = 0;
	
	/**
	 * The destination IP address of the event when it is an external
	 * event.
	 */
	protected String destIP = null;
	
	/**
	 * The destination port of the event when it is an external event.
	 */
	protected int destPort = 0;

	/**
	 * The physical index location of this event within the message
	 * queue.
	 */
	protected int msgQueueIndex = -1;
	
	/**
	 * A flag indicating that this message is a duplicate of a previous
	 * message
	 */
	protected boolean duplicate = false;
	
	/**
	 * The message sequencer value assigned by the logger at the time the
	 * event is received/sent by the platform.
	 */
	protected int sequencer = 0;
	
	/*
	 * The transport protocol to use for this message
	 */ 
	protected Transport transport = null;

	/*
	 * This indicates whether PCSim2 sent the message event (value true) or
	 * received the message event (value false). If the value is null, the
	 * event was an internal event.
	 */
	protected Boolean sent = null;
	
	/**
	 * Constructor
	 * @param uid - the unique id of the FSM the event is associated with.
	 * @param time - the time the event was created.
	 * @param srcIP - the source IP address of the event.
	 * @param srcPort - the source port of event
	 * @param destIP - the destination IP address of the event.
	 * @param destPort- the destination port of the event.
	 * @param name - the name of this event.
	 * @param sent - whether this message was transmitted off the system
	 */
	public MsgEvent(int uid, long time, int seq,
			Transport t,
			String srcIP, int srcPort, 
			String destIP, int destPort,
			String name, Boolean sent) {
		this.timestamp = time;
		this.fsmUID = uid;
		this.sequencer = seq;
		this.srcIP = srcIP;
		this.srcPort = srcPort;
		this.destIP = destIP;
		this.destPort = destPort;
		this.eventName = name;
		this.transport = t;
		this.sent = sent;
	}
	
	/**
	 * Gets the destination IP address
	 * @return
	 */
	public String getDestIP() {
		return destIP;
	}

	/**
	 *Sets the destination IP Address
	 * @param destIP
	 */
	public void setDestIP(String destIP) {
		this.destIP = destIP;
	}

	/**
	 * Gets the destination port
	 * @return
	 */
	public int getDestPort() {
		return destPort;
	}

	/**
	 * Sets the destination port
	 * @param destPort
	 */
	public void setDestPort(int destPort) {
		this.destPort = destPort;
	}

	/**
	 * Gets the name of the event
	 * @return
	 */
	public String getEventName() {
		 return this.eventName;
	}
	
	/**
	 * Gets the source IP address
	 * @return
	 */
	public String getSrcIP() {
		return srcIP;
	}

	/**
	 * Sets the source IP Address
	 * @param srcIP
	 */
	public void setSrcIP(String srcIP) {
		this.srcIP = srcIP;
	}

	/**
	 * Gets the source port
	 */
	public int getSrcPort() {
		return srcPort;
	}

	/**
	 * sets the source port
	 * @param srcPort
	 */
	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	/**
	 * Gets the FSM's unique ID
	 * @return
	 */
	public int getUID() {
		return fsmUID;
	}
	
	/**
	 * Gets the time stamp of the event.
	 * @return
	 */
	public long getTimeStamp() {
		return timestamp;
	}
	
	/**
	 * Gets the index location of the message event
	 * within the MsgQueue.
	 */
	public int getMsgQueueIndex() {
		return this.msgQueueIndex;
	}
	
	/**
	 * Sets the index location of this message event
	 * within the MsgQueue.
	 */
	public void setMsgQueueIndex(int index) {
		this.msgQueueIndex = index;
	}
	
	/**
	 * Sets the duplicate message flag
	 */
	public void setDuplicate(boolean flag) {
		this.duplicate = flag;
	}
	
	/**
	 * Gets the duplicate message flag
	 */
	public boolean isDuplicate() {
		return this.duplicate;
	}

	/**
	 * Gets the sequencer for this message event.
	 * @return
	 */
	public int getSequencer() {
		return this.sequencer;
	}
	
	public Transport getTransport() {
		return this.transport;
	}
	
	public boolean platformSent() {
		if (sent != null && sent) {
			return true;
		}
		
		return false;
	}
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = " eventName=" + eventName 
			+ " Timestamp=" + timestamp 
			+ " msgQueueIndex=" + msgQueueIndex 
			+ " fsmUID=" + fsmUID 
			+ " sequencer=" + sequencer
			+ " transport=" + transport
			+ " srcIP=" + srcIP 
			+ " srcPort=" + srcPort 
			+ " destIP=" + destIP 
			+ " destPort=" + destPort 
			+ " duplicate=" + duplicate;
		return result;
	}
}
