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
 * Container class for the msg_ref element of a PC 2.0 Simulator
 * XML document with the type attribute set to "event".
 * 
 * @author ghassler
 *
 */
public class EventRef extends MsgRef {

	/**
	 * The name of the header being referred to.
	 * For an event this can be TIMESTAMP
	 */
	private String hdr = null; 
	/**
	 * Constructor
	 * @param type - type of msg_ref
	 */
	public EventRef(String type) {
		super(type);
	}

	public String getHeader() {
		return hdr;
	}
	/**
	 * Obtains a string representation of internal platform event being referenced.
	 */
	@Override
	public String toString() {
		return "event";
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		EventRef retval = (EventRef)super.clone();
		return retval;
	}
	
	@Override
	public String display() {
		return "Internal event " + type;
	}
	
	public void setHeader(String hdr) {
		this.hdr = hdr;
		
	}
	
	public void setType(String t) {
		this.type = t;
		
	}
}