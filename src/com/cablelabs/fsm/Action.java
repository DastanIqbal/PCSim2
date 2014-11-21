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
 * This is interface class defines the operation that an actions can execute
 * from within the FSM. Currently the only actions that
 * can be performed are send, proxy, log, pass, fail, generate and
 * sleep.
 *
 * See the PC 2.0 Simulator XML Defintion for more details
 * were this class is constructed and its use.
 *
 * @author ghassler
 *
 */
public interface Action extends Cloneable {

	/**
	 * Common operation to perform the action described by the
	 * derived class.
	 * @param api 
	 * @param msgQueueIndex 
	 * @return 
	 * @throws PC2Exception 
	 */
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception;

	/**
	 * Forces derived classes to implement this method for logging.
	 */
	@Override
    public abstract String toString();

	/** This implements a deep copy of the class for replicating
	 * FSM information.
	 *
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */
	public abstract Object clone() throws CloneNotSupportedException;

//	 This method was added as a validator for cloning
//	public String me();

}
