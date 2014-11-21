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
 * An interface for operations that can be performed while
 * in the active phase of a State.
 * 
 * @author Garey Hassler
 *
 */
public interface ActiveOp {

	/**
	 * This method is called by the Responses when an event is received
	 * by the State
	 * 
	 * @param api - the class to call when needing actions executed.
	 * @param ce - the class to perform the conditional's evaluation.
	 * @param event - the current event that cause the condition to be evaluated.
	 * 
	 * @return - true if the task completes successfully, false otherwise.
	 * 
	 * @throws PC2Exception
	 */
	public boolean performOp(FSMAPI api, ComparisonEvaluator ce, MsgEvent event) throws PC2Exception;

}
