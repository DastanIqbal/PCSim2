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
 * The bandwidth attribute represents the peak bandwidth that the client
 * expects to use on the client to server connection.  It is a 32-bit
 * unsigned integral value and is measured in kilobits per second.
 *
 * @author ghassler
 *
 */
public class Bandwidth extends StunAttribute {

	public Bandwidth(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		super.setValue(val);
	}
	
	public Bandwidth(byte [] val) throws IllegalArgumentException {
		super(StunConstants.BANDWIDTH_TYPE, val);
		super.setValue(val);
	}
	
	public void setValue(int time) {
		super.setValue(Conversion.intToByteArray(time));
	}
	
	public boolean inRange() {
		// TODO determine if we should have some maximum bandwidth value or not
		return true;
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
			+ "] Kbits/sec. padding=[" + padding + "]";
		return result;
	}
}
