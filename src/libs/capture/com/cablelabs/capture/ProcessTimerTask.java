/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.capture;

import java.util.TimerTask;

public class ProcessTimerTask extends TimerTask {

	/**
	 * The state that owns this timer.
	 */
	private TimerListener owner = null;
	/**
	 * Constructor.
	 * @param fsm - the FSM that owns the timer and that will
	 * 		process the timeout event.
	 */
	public ProcessTimerTask(TimerListener owner) {
		this.owner = owner;
	}
	
	/**
	 * The run method will be invoked if the associated timer
	 * expires.
	 */
	public void run() {
		if (owner != null) {
			owner.expired();
		}
	}

}
