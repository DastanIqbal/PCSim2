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
 * This Action class allows the user to change the value stored in
 * a variable with a new reference.
 * @author Garey Hassler
 *
 */
public class Assign implements Reference, Action {

	/**
	 * The FSM that the reference is located in 
	 */
	private String fsm = null;
	
	/**
	 * The global variable name that we are going to
	 * change.
	 */
	private String name = null;
	/**
	 * The reference information to obtain the new value for the
	 * variable.
	 */
	private Reference newValue = null;

	public Assign(String name, String fsm) {
		this.name = name;
		this.fsm = fsm;
	}

	/**
	 * Common operation to perform the action described by the
	 * derived class.
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.assign(this, msgQueueIndex);
	}
	
	public String getFSM() {
		return this.fsm;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Reference getRef() {
		return this.newValue;
	}
	
	public void setRef(Reference r) {
		this.newValue = r;
	}
	
	/**
	 * The declaration for defining a toString method for all references.
	 */
	@Override
	public String toString() {
		String result =	"\tassign " + name + " = ";
		if (newValue != null) 
			result += newValue.toString();
		else 
			result += newValue;
		return result;
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		Assign retval = (Assign)super.clone();
		if (retval != null) {
			if (this.name != null) 
				retval.name = new String(this.name);
			if (this.newValue != null)
				retval.newValue = (Reference)this.newValue.clone();
		}	
		
		return retval;
	}
	
	/**
	 * The declaration for defining a toString method for all references.
	 */
	@Override
	public String display() {
		if (newValue != null) 
			return newValue.display();

		return null;

	}
}
