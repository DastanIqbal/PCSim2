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
 * Container class for the elseif element of a PC 2.0 Simulation
 * XML document.
 * 
 * @author ghassler
 *
 */
public class ElseIf extends FlowControl {

	/**
	 * Constructor
	 *
	 */
	public ElseIf() {
		super();
	}
	
	/**
	 * A string representation of the information the container
	 * holds.
	 */
	@Override
	public String toString() {
		String result = new String("\nelse if ") + super.toString();
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
		ElseIf retval = (ElseIf)super.clone();
		return retval;
	}
}
