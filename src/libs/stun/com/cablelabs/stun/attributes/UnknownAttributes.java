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

import com.cablelabs.stun.StunConstants;
import com.cablelabs.common.*;

public class UnknownAttributes extends StunAttribute {

	public UnknownAttributes(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
	}
	
	public UnknownAttributes(byte [] val) throws IllegalArgumentException {
		super(StunConstants.UNKNOWN_ATTRIBUTES_TYPE, val);
		super.setValue(val);
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data.
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "] value=[" + Conversion.hexString(value) + "] padding=[" + padding + "]";
		return result;
	}
}
