/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.sim;

import com.cablelabs.fsm.FSMListener;

public class RTPData {
	private FSMListener listener = null;
	private long initTimeStamp = 0;
	private long lastTimeStamp = 0;
	private long approxPacketRate = 0;
	private String srcIP = null;
	private int srcPort = 0;
	private boolean streamComplete = false;
	
	public RTPData(FSMListener l, long startTime, String ip, int port) {
		this.listener = l;
		this.initTimeStamp = startTime;
		this.srcIP = ip;
		this.srcPort = port;
	}
	
	public void updateTime(long time) {
		if (lastTimeStamp == 0) {
			approxPacketRate = time - initTimeStamp;
		}
		lastTimeStamp = time;
	}
	
	public long getPacketRate() {
		return approxPacketRate;
	}
	
	public long getInitialTime() {
		return initTimeStamp;
	}
	
	public FSMListener getFSM() {
		return listener;
	}
	
	public long getLastTime() {
		return lastTimeStamp;
	}
	
	public String getSrcIP() {
		return this.srcIP;
	}
	
	public int getSrcPort() {
		return this.srcPort;
	}
	
	public void setComplete() {
		this.streamComplete = true;
	}
	
	public boolean isComplete() {
		return this.streamComplete;
	}
}
