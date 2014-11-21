/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun;

import java.util.TimerTask;


public class AllocationTask extends TimerTask {
	/**
	 * The owner of this timer task.
	 */
	private Allocation owner = null;

	/**
	 * Constructor
	 * @param s - the state that owns the timer.
	 */
	public AllocationTask(Allocation a) {
		this.owner = a;
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
