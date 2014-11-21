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
 * The class performs the operation of processing the Sleep
 * Timer upon expiration. It's task is to unlock the FSM
 * from queueing all events and to begin processing them.
 * 
 * @author ghassler
 *
 */
public class FSMSleepTask extends TimerTask {
	
	/**
	 * The state that owns the Sleep timer.
	 */
	private State state = null;
	
	/**
	 * Constructor
	 * @param s - the state that owns the timer.
	 */
	public FSMSleepTask(State s) {
		this.state = s;
	}
	
	/**
	 * The run method will be invoked if the associated timer
	 * expires.
	 */
	@Override
	public void run() {
		if (state != null) {
			if (state.validTimerTask(this, state))
				state.sleepCancel();
		}
	}
}
