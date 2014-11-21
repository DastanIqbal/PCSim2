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
 * This is a contain for the PASS or FAIL Actions of a PC 2.0 XML 
 * document.
 * 
 * @author ghassler
 *
 */

public class Result implements Action { 

	/**
	 * A flag defining if this is a PASS action (when true) or
	 * a FAIL action (when set to false). Default is that it is
	 * a PASS.
	 */
	private boolean pass = true;
	
	/**
	 * Constructor
	 * @param pass
	 */
	public Result(boolean pass) {
		this.pass = pass;
	}
	
	/**
	 * Gets the pass flag
	 * @return
	 */
	public boolean getPass() {
	 
	 
		return pass;
	}
	
	/**
	 * Performs the PASS or FAIL Action
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.pass(this);
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = null;
		if (pass)
			result = "\tpass\n";
		else
			result = "\tfail\n";
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
		Result retval = (Result)super.clone();
		if (retval != null ) {
			retval.pass = this.pass;
		}	

		return retval;
	}
	
//	 This method was added as a validator for cloning
//	public String me() {
//		return "R " + super.toString();
//	}
}
