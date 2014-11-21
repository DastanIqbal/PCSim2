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

/**
 * The lifetime attribute represents the duration for which the server
 * will maintain an allocation in the absence of a refresh.  It is a 32-
 * bit unsigned integral value representing the number of seconds
 * remaining until expiration.
 *
 * @author ghassler
 *
 */
public class Password extends StunAttribute {

	public Password(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		super.setValue(val);
	}
	
	public Password(byte [] val) throws IllegalArgumentException {
		super(StunConstants.PASSWORD_TYPE, val);
		super.setValue(val);
	}
	
	public void setValue(byte [] value) {
		super.setValue(value);
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data. 
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "] value=[" + new String(value, 0, value.length) 
			+ "] padding=[" + padding + "]";
		return result;
	}
}

