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

public class PC2DiameterData {

	private FSMListener listener = null;
	private int lastRecNum = -1;
	
	/**
	 * This contains the network label that was the
	 * source of the message.
	 * 
	 */
	private String sourceLabel = null;
	
	/**
	 * The destination network label
	 * 
	 */
	private String destLabel = null;
	
	/**
	 * This is the Session-Id field within the
	 * various Diameter message that un
	 */
	private String sessionId = null;
	
	public PC2DiameterData(String src, String key) {
		this.sourceLabel = src;
		this.sessionId = key;
	}
	
	public PC2DiameterData(String src, String dest, String key) {
		this.sourceLabel = src;
		this.destLabel = dest;
		this.sessionId = key;
	}
	
	public PC2DiameterData(FSMListener listener, String key) {
		this.listener = listener;
		this.sessionId = key;
	}
	
	public String getDestLabel() {
		return destLabel;
	}
	
	public FSMListener getListener() {
		return listener;
	}
	
	public int getLastRecNum() {
		return lastRecNum;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public String getSourceLabel() {
		return sourceLabel;
	}
	public int incLastRecNum() {
		return ++lastRecNum;
	}
	
	public void setDestLabel(String dest) {
		destLabel = dest;
	}
	
	public void setListener(FSMListener l) {
		this.listener = l;
	}
	
	public void setSessionId(String sessId) {
		this.sessionId = sessId;
	}
	
	public void setSourceLabel(String src) {
		this.sourceLabel = src;
	}
}
