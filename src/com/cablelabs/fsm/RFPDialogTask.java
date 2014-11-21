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

public class RFPDialogTask extends TimerTask {

	/**
	 * The FSM that is the owner of the state which 
	 * created the timer and will process the event if the
	 * timer expires. 
	 */
	private ReferencePointsFactory rfp = null;
	
	public RFPDialogTask(ReferencePointsFactory r) {
		this.rfp = r;
	}

	@Override
	public void run() {
		if (rfp != null) {
			rfp.dialogEstTimerExpired(this);
		}

	}

}
