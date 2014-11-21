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
 * The ICE-CONTROLLED attribute is present in a Binding Request, and
 * indicates that the client believes it is currently in the controlled
 * role.  The content of the attribute is a 64 bit unsigned integer in
 * network byte ordering, which contains a random number used for tie-
 * breaking of role conflicts.
 * 
 * @author ghassler
 *
 */
public class IceControlled extends StunAttribute {

	public IceControlled(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		super.setValue(val);
	}
	
	public IceControlled(byte [] val) throws IllegalArgumentException {
		super(StunConstants.ICE_CONTROLLED_TYPE, val);
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
