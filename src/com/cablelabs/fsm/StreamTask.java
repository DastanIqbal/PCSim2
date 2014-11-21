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

import java.util.TimerTask;

/**
 * The class performs to the Stream class when to send the next
 * media packet
 * 
 * @author ghassler
 *
 */
public class StreamTask extends TimerTask {
	
	/**
	 * The Stream that owns the timer.
	 */
	private Stream stream = null;
	
	/**
	 * Constructor
	 * @param s - the state that owns the timer.
	 */
	public StreamTask(Stream s) {
		this.stream = s;
	}
	
	/**
	 * The run method will be invoked when the associated timer
	 * expires.
	 */
	@Override
	public void run() {
		if (stream != null) {
			stream.timerExpired();
		}
		else {
			this.cancel();
		}
	}
}

