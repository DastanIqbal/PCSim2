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

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

/**
 * The class performs the operation of processing the No
 * Response Timer upon expiration. It's task is to add the
 * NoResponseTimeout event to the internal queue of the 
 * FSM for processing in order.
 * @author ghassler
 *
 */
public class FSMNoResponseTask extends TimerTask {

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
	
	private long startTime = 0;
	private long maxTime = 0;

	private LogAPI logger = LogAPI.getInstance();
	/**
	 * Constructor.
	 * @param fsm - the FSM that owns the timer and that will
	 * 		process the timeout event.
	 */
	public FSMNoResponseTask(FSM fsm, State owner, long time) {
		this.fsm = fsm;
		this.owner = owner;
		this.startTime = System.currentTimeMillis();
		this.maxTime = startTime + time;
//		logger.info(PC2LogCategory.FSM , "", 
//				"Starting No Response Timer for " + owner 
//				+ " at " + System.currentTimeMillis());
	}
	
	/**
	 * The run method will be invoked if the associated timer
	 * expires.
	 */
	@Override
	public void run() {
		if (fsm != null) {
			long curTime = System.currentTimeMillis();
//			if (curTime < maxTime - 10 || curTime > maxTime + 10) {
//				logger.info(PC2LogCategory.FSM, "", 
//					"No Response Timer(" + (curTime - startTime)+ ") expired for " + owner 
//					+ " at " + curTime + " and was expected to fire at " + maxTime);
//			}
			if (logger.isDebugEnabled(PC2LogCategory.FSM, "")) {
				logger.debug(PC2LogCategory.FSM, "", 
						"No Response Timer expired (" + (curTime - startTime)+ ") for " + owner.getName() 
						+ " at " + curTime + " and was expected to fire at " + maxTime);
			}	
			else {
				logger.info(PC2LogCategory.FSM, "", 
					"No Response Timer expired (" + (curTime - startTime)+ ") for " + owner.getName());
			}
			fsm.noResponseTimeout(this, owner);
			
		}
	}
}
