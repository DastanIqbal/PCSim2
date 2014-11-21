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

public class ProvTimerTask extends TimerTask {

	/**
	 * The state that owns this timer.
	 */
	private Object owner = null;
	
	private long startTime = 0;
	private long maxTime = 0;

	private LogAPI logger = LogAPI.getInstance();
	/**
	 * Constructor.
	 * @param fsm - the FSM that owns the timer and that will
	 * 		process the timeout event.
	 */
	public ProvTimerTask(ProvListener owner, long time) {
		this.owner = owner;
		this.startTime = System.currentTimeMillis();
		this.maxTime = startTime + time;
//		logger.info(PC2LogCategory.FSM , "", 
//				"Starting No Response Timer for " + owner 
//				+ " at " + System.currentTimeMillis());
	}
	
	public ProvTimerTask(RecordProvFileListener owner, long time) {
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
		if (owner != null) {
			long curTime = System.currentTimeMillis();
//			if (curTime < maxTime - 10 || curTime > maxTime + 10) {
//				logger.info(PC2LogCategory.FSM, "", 
//					"No Response Timer(" + (curTime - startTime)+ ") expired for " + owner 
//					+ " at " + curTime + " and was expected to fire at " + maxTime);
//			}
			if (logger.isDebugEnabled(PC2LogCategory.FSM, "")) {
				logger.debug(PC2LogCategory.FSM, "", 
						"Prov Listener Timer expired (" + (curTime - startTime)+ ") at "  
						+ curTime + " and was expected to fire at " + maxTime);
			}	
			else {
				logger.info(PC2LogCategory.FSM, "", 
					"Prov Listener Timer expired (" + (curTime - startTime)+ ").");
			}
			if (owner instanceof ProvListener)
				((ProvListener)owner).timerExpired(this);
			else if (owner instanceof RecordProvFileListener)
				((RecordProvFileListener)owner).timerExpired(this);
			else
				// As a precaution terminate if we can't determine the owner
				this.cancel();
			
		}
	}
}
