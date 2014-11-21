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


/**
 * This class allows a script change the status of a 
 * @author ghassler
 *
 */
public class ChangeStatus implements Action {

	/**
	 * The network element label representing the 
	 * device upon whom's status to change.
	 */
	private String neLabel = null;
	
	/**
	 * The new status for the network element
	 */
	private PresenceStatus newStatus = null;
	

	public ChangeStatus(String ne, PresenceStatus ns) {
		this.neLabel = ne;
		this.newStatus = ns;
	}
	
	/**
	 * Common operation to perform the action described by the
	 * derived class.
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.changeStatus(neLabel, newStatus);
	}

	public String getLabel() {
		return this.neLabel;
	}
	
	public PresenceStatus getStatus() {
		return this.newStatus;
	}
	
//	public void setFSM(String fsm) {
//		this.fsm = fsm;
//	}
//	
//	public String getFSM() {
//		return this.fsm;
//	}
	
	/**
	 * Forces derived classes to implement this method for logging.
	 */
	@Override
	public String toString() {
		String result =	"\tchangeStatus label=" + neLabel + " status=" + newStatus;
		
		return result;
	}

	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		ChangeStatus retval = (ChangeStatus)super.clone();
		if (retval != null) {
			if (this.neLabel != null) 
				retval.neLabel = new String(this.neLabel);
			if (this.newStatus != null)
				retval.newStatus = this.newStatus;
//			if (this.fsm != null)
//				retval.fsm = new String(this.fsm);
		}	
		
		return retval;
	}

}
