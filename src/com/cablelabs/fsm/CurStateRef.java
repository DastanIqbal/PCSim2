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
 * The container class for the cur_state child
 * element for the coun elements of
 * a PC 2.0 Simulator's XML document.
 * 
 * @author ghassler
 *
 */
public class CurStateRef implements Reference {

	protected State state = null;
	
	public CurStateRef(State s) {
		this.state = s;
	}
	@Override
	public String toString() {
		String result = "cur_state";
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
		CurStateRef retval = (CurStateRef)super.clone();
		return retval;
	}
	
	@Override
	public String display() {
		String result = " Current state's[" + state.getName() 
		+ "] count is " + state.getCounter();
		return result;
	}
}
