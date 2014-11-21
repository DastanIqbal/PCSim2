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
 * This is implements the SLEEP Action defined within PC 2.0 Simulator
 * XML documents.
 * 
 * @author ghassler
 *
 */
public class Sleep implements Action { 

	/**
	 * The amount of time in milliseconds for the finite state machine
	 * to wait before continuing.
	 */
	private int time;

	/**
	 * Gets the time to sleep.
	 * @return time
	 */
	public int getTime() {
		return time;
	}

	/**
	 * Constructor
	 * @param time
	 */
	public Sleep(int time) {
		super();
		this.time = time;
	}
	
	
	/**
	 * Performs the sleep operation through the FSM API
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.sleep(this);
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = "\tsleep time=\"" + time + "\"\n";
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
		Sleep retval = (Sleep)super.clone();
		if (retval != null ) {
			retval.time = this.time;
		}	

		return retval;
	}
	
//	 This method was added as a validator for cloning
//	public String me() {
//		return "Sl " + super.toString();
//	}
}
