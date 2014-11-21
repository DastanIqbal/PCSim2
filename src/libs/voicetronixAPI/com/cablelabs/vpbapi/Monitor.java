/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.vpbapi;

import java.util.*;
import java.text.DateFormat;
import org.apache.log4j.helpers.ISO8601DateFormat;

public class Monitor {

	protected VpbEventType type = null;
	protected int transactionID = -1;
	protected long start = System.currentTimeMillis();
	private static DateFormat df = (DateFormat)new ISO8601DateFormat();
	
	protected long expires = -1;
	protected final static long DEFAULT_WAIT = 10000L;
	protected boolean expected = true;
	
	protected boolean twoWayVoiceVerify = false;
	
	public Monitor(VpbEventType et, int tid, boolean negative) {
		this.type = et;
		this.transactionID = tid;
		this.expected = negative;
		this.expires = start + DEFAULT_WAIT;
	}
	
	public Monitor(VpbEventType et, int tid, int wait) {
		this.type = et;
		this.transactionID = tid;
		this.expires = start + wait;
	}
	
	public Monitor(VpbEventType et, int tid, boolean negative, int wait) {
		this.type = et;
		this.transactionID = tid;
		this.expected = negative;
		this.expires = start + wait;
	}
	
	public String toString() {
		Date d = new Date(start);
		return "Monitor for " + type + "(" + transactionID + ") started at " + df.format(d) + ".";
	}
}
