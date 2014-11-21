/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.utility;

import java.io.Serializable;


public class UtilityEvent implements Cloneable, Serializable{

	private UtilityMessage message = null;
	private String srcIP = null;
	private int srcPort = 0;
	private String destIP = null;
	private int destPort = 0;
	private static final long serialVersionUID = 1;
	private int sequencer = 0;
	
	public UtilityEvent() {
		
	}
	public UtilityEvent(UtilityMessage msg, int seq,
			String srcIP, 
			int srcPort, String destIP, int destPort) {
		this.message = msg;
		this.sequencer = seq;
		this.srcIP = srcIP;
		this.srcPort = srcPort;
		this.destIP = destIP;
		this.destPort = destPort;
	}
	
	public String getDestIP() {
		return destIP;
	}
	public void setDestIP(String destIP) {
		this.destIP = destIP;
	}
	public int getDestPort() {
		return destPort;
	}
	public void setDestPort(int destPort) {
		this.destPort = destPort;
	}
	public UtilityMessage getMessage() {
		return message;
	}
	public void setMessage(UtilityMessage message) {
		this.message = message;
	}
	public int getSequencer() {
		return this.sequencer;
	}
	
	public void setSequencer(int seq) {
		this.sequencer = seq;
	}
	
	public String getSrcIP() {
		return srcIP;
	}
	public void setSrcIP(String srcIP) {
		this.srcIP = srcIP;
	}
	public int getSrcPort() {
		return srcPort;
	}
	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}
	
	public boolean equals(Object other) {
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		UtilityEvent that = (UtilityEvent) other;
		
		// First check the type, version and transactionID. 
		// This is were most objects will fail to match
		if (!(this.message.equals(that.getMessage()) &&
				this.srcIP.equals(that.getSrcIP()) &&
				this.destIP.equals(that.getDestIP()) &&
				this.srcPort == that.getSrcPort() &&
				this.destPort == that.getDestPort() &&
				this.sequencer == that.getSequencer())) {
			return false;
		}
		
		return true;
	}
	
	public UtilityEvent clone() {
		UtilityMessage um = (UtilityMessage)this.message.clone();
		String sIP = new String(this.srcIP);
		String dIP = new String(this.destIP);
		UtilityEvent ue = new UtilityEvent();
		ue.setMessage(um);
		ue.setSrcIP(sIP);
		ue.setDestIP(dIP);
		ue.setSrcPort(this.srcPort);
		ue.setDestPort(this.destPort);
		// Don't clone sequencer.
		return ue;
		
	}
	public String toString() {
		String result = message.toString() + " srcIP=" + srcIP + "|" + 
			srcPort + " destIP=" + destIP + "|" + destPort;
		if (sequencer > 0) 
			result += " sequencer=" + sequencer;
		
		return result;
	}
}
