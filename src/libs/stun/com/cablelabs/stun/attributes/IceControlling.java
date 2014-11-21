/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun.attributes;

import com.cablelabs.common.Conversion;
import com.cablelabs.stun.StunConstants;

/**
 * The ICE-CONTROLLING attribute is present in a Binding Request, and
 * indicates that the client believes it is currently in the controlling
 * role.  The content of the attribute is a 64 bit unsigned integer in
 * network byte ordering, which contains a random number used for tie-
 * breaking of role conflicts.
 *
 * @author ghassler
 *
 */
public class IceControlling extends StunAttribute {

	public IceControlling(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		super.setValue(val);
	}
	
	public IceControlling(byte [] val) throws IllegalArgumentException {
		super(StunConstants.ICE_CONTROLLING_TYPE, val);
		super.setValue(val);
	}
	
	public void setValue(long time) {
		super.setValue(Conversion.longToByteArray(time));
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data. 
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "] value=[" + Conversion.byteArrayToLong(value) 
			+ "] padding=[" + padding + "]";
		return result;
	}
}
