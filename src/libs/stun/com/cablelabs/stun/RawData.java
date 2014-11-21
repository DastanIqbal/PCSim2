/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun;

import java.util.*;

import com.cablelabs.common.Transport;

public class RawData extends EventObject {

	private static final long serialVersionUID = 1;
	private Object data = null;
	private int dataLength = -1;
	private int sequencer = 0;
	private Transport transport = null;

	/**
	 * The source IP address of the event when it is an external
	 * event.
	 */
	private String srcIP = null;

	/**
	 * The source port of the event when it is an external event.
	 */
	private int srcPort = 0;

	/**
	 * The destination IP address of the event when it is an external
	 * event.
	 */
	private String destIP = null;

	/**
	 * The destination port of the event when it is an external event.
	 */
	private int destPort = 0;

	private long timestamp = 0;
	
	public RawData(Object source, Object event,
			int length,
			String srcIP, int srcPort, String destIP,
			int destPort, Transport t,  
			int sequencer) {
		super(source);
		this.sequencer = sequencer;
		this.data = event;
		this.dataLength = length;
		this.srcIP = srcIP;
		this.srcPort = srcPort;
		this.destIP = destIP;
		this.destPort = destPort;
		this.transport = t;
		this.timestamp = System.currentTimeMillis();
	}

	public Object getData() {
		return data;
	}

	public int length() {
		return this.dataLength;
	}
	
	/**
	 * Gets the destination IP address
	 * @return
	 */
	public String getDestIP() {
		return destIP;
	}

	/**
	 * Gets the destination port
	 * @return
	 */
	public int getDestPort() {
		return destPort;
	}

	/**
	 * Gets the source IP address
	 * @return
	 */
	public String getSrcIP() {
		return srcIP;
	}

	/**
	 * Gets the source port
	 */
	public int getSrcPort() {
		return srcPort;
	}


	/**
	 * Gets the time stamp of the event.
	 * @return
	 */
	public long getTimeStamp() {
		return this.timestamp;
	}

	public Transport getTransport() {
		return this.transport;
	}
	/**
	 * Gets the sequencer for this message event.
	 * @return
	 */
	public int getSequencer() {
		return this.sequencer;
	}
	
	/**
	 * Sets the destination IP address
	 */
	public void setDestIP(String destIP) {
		this.destIP = destIP;
	}
	
	
	/**
	 * Sets the source IP address
	 */
	public void setSrcIP(String srcIP) {
		this.srcIP = srcIP;
	}
	
	/**
	 * Sets the source port
	 */
	public void setSrcPort(int port) {
		this.srcPort = port;
	}
	
	/**
	 * Sets the source port
	 */
	public void setDestPort(int port) {
		this.destPort = port;
	}
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	public String toString() {
		String result = " RawData [Timestamp=" + timestamp 
		+ " sequencer=" + sequencer
		+ " srcIP=" + srcIP 
		+ " srcPort=" + srcPort 
		+ " destIP=" + destIP 
		+ " destPort=" + destPort
		+ " transport=" + transport
		+ " eventLen=" + dataLength + "]\n event=[" +
		data.toString() + "].\n";
		
		
		return result;
	}
}
