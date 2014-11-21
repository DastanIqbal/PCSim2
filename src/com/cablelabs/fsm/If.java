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
 * This is the containter class for the IF conditional 
 * defined within the PC 2.0 Simulator's XML 
 * document. It holds the logical and comparison operations
 * to be evaluated and the ELSEIF conditionals. In addition
 * it holds all of the actions that could be generated as
 * a result of the evaluation of the conditions.
 * 
 * @author ghassler
 *
 */
public class If extends FlowControl {

	/**
	 * Constructor.
	 *
	 */
	public If() {
		super();
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = new String("if ") + super.toString();
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
		If retval = (If)super.clone();
		return retval;
	}
}
