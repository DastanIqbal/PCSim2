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
 * The PRIORITY attribute indicates the priority that is to be
 * associated with a peer reflexive candidate, should one be discovered
 * by this check.  It is a 32 bit unsigned integer, and has an attribute
 * value of 0x0024.
 *
 */
public class Priority extends StunAttribute {

	public Priority(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		super.setValue(val);
	}
	
	public Priority(byte [] val) throws IllegalArgumentException {
		super(StunConstants.PRIORITY_TYPE, val);
		super.setValue(val);
	}
	
	public void setValue(long val) {
		super.setValue(Conversion.longToByteArray(val));
	}
	
	public long getValueAsLong() {
		return Conversion.byteArrayToLong(super.getValue());
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data. 
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "] value=[" + Conversion.byteArrayToInt(value) 
			+ "] padding=[" + padding + "]";
		return result;
	}
}
