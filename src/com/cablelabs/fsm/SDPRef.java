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
 * A container for the message reference information necessary
 * pertaining to information in the SDP protocol.
 * 
 * @author ghassler
 *
 */
public class SDPRef extends SIPRef {


	/**
	 * Constructor
	 * @param type
	 */
	public SDPRef(String type) {
		super(type);
	}


	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = super.toString();
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
		SDPRef retval = (SDPRef)super.clone();
		return retval;
	}
	
	@Override
	public String display() {
		String result = "";
		if (response) {
			if (super.addRef || super.subRef)
				result += "(" + statusCode + "-" + method;
			else 
				result += statusCode + "-" + method;
		}
		else {
			if (super.addRef || super.subRef)
				result += "(" + method;
			else
				result += method;
		}
		if (msgInstance != null)
			result += "[" + msgInstance + "]";
		if (hdr != null)
			result += "." + hdr + "[" + hdrInstance + "]";
		if (parameter != null) 
			result += "." + parameter;
		
		result += " SDP body";
		
		if (super.addRef) {
			result += " + " + super.getArithmeticMod() + ")";
		}
		else if (super.subRef) {
			result += " - " + super.getArithmeticMod() + ")";
		}
		
		
		return result;
	}
}
