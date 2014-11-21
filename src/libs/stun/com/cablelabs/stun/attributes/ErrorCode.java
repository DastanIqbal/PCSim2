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

public class ErrorCode extends StunAttribute {


	public ErrorCode(char type, byte [] val ) throws IllegalArgumentException {
		super(type,null);
		super.setValue(val);
	}
	
	public ErrorCode(byte [] val) throws IllegalArgumentException {
		super(StunConstants.ERROR_CODE_TYPE, val);
		super.setValue(val);
	}
	
	public int getStatusCode() {
		if (value != null) {
			return (value[2] * 100) + value[3];
		}
		return -1;
	}
	public void setValue(int errorCode) {
		byte [] phrase = StunConstants.createErrorPhrase(errorCode);
		byte [] val = new byte [4 + phrase.length];
		val[0] = 0;
		val[1] = 0;
		val[2] = (byte)(errorCode/100);
		val[3] = (byte)(errorCode % 100);
		if (phrase.length > 0) 
			System.arraycopy(phrase,0, val,4,phrase.length);
		super.setValue(val);
//		if (mod > 0) 
//			for (int i = 0; i < mod; i++)
//				value[phrase.length+4+i] = 0x00;
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data. This method also assumes that the value is string based.
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "] value=[" + getStatusCode() + " " + new String(value,4,length-4) 
			+ "] padding=[" + padding + "]";

		return result;
	}
}
