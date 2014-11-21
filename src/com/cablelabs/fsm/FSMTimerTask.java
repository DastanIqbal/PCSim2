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
 * The class performs the operation of processing the State's
 * internal Timer upon expiration. It's task is to add the
 * TimerExpired event to the internal queue of the 
 * FSM for processing in order.
 * 
 * @author ghassler
 *
 */
public class FSMTimerTask extends TimerTask {

	/**
	 * The FSM that is the owner of the state which 
	 * created the timer and will process the event if the
	 * timer expires. 
	 */
	private FSM fsm = null;

	/**
	 * The state that owns this timer.
	 */
	private State owner = null;
	/**
	 * Constructor.
	 * @param fsm - the FSM that owns the timer and that will
	 * 		process the timeout event.
	 */
	public FSMTimerTask(FSM fsm, State owner) {
		this.fsm = fsm;
		this.owner = owner;
	}
	
	/**
	 * The run method will be invoked if the associated timer
	 * expires.
	 */
	@Override
	public void run() {
		if (fsm != null) {
			fsm.stateTimerExpired(this, owner);
		}
	}
}
