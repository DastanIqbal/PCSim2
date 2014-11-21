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

public interface Reference extends Cloneable {

	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 */ 
	public Object clone() throws CloneNotSupportedException;

	/**
	 * The declaration for defining a toString method for all references.
	 */
	@Override
	public String toString();

	/**
	 * The display method is different because it attempts to try and format 
	 * the information in a more user friendly format than the toString
	 * method. 
	 * 
	 * Class that implement this method should attempt to display the
	 * information in a msg[msg_instance].hdr[hdr_instance].field format
	 * to keep it consistent.
	 * @return
	 */
	public String display();
}
	
