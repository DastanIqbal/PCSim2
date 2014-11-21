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

public interface Conditional extends Cloneable {
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	public Object clone() throws CloneNotSupportedException;
	
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
//	public String display();
	
	/**
	 * Definition for the evaluate method. Its intent is
	 * to provide a common interface for all of the logical,
	 * comparison and binary operations
	 */
	public boolean evaluate(ComparisonEvaluator ce, MsgEvent event);

	public int getWildcardIndex();
	
    public boolean hasWildcardIndex();

	/**
	 * This method sets all of the wildcard attributes in each ArrayIndex back
	 * to the wildcard value of -1. 
	 */
	public void resetWildcardIndex();
	
	/**
	 * Require implementation of a printable represention of the derived
	 * classes information.
	 */
	@Override
	public String toString();
	
	/**
	 * This implements the deep updating of the fsmUID for all of the message
	 * and variable expression references in a FSM.
	 *
	 */
	public void updateUIDs(int newUID, int origUID);

	/**
	 * This method sets the value defined by the wildcard attribute in each
	 * ArrayIndex reference to the new value defined by the newIndex.
	 * 
	 */
	public void updateWildcardIndex(int newIndex);
	
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
