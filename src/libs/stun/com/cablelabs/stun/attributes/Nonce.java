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

import java.security.MessageDigest;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.stun.StunConstants;
import com.cablelabs.common.*;

public class Nonce extends StunAttribute {

	public Nonce(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		super.setValue(val);
	}
	
	public Nonce(byte [] val) throws IllegalArgumentException {
		super(StunConstants.NONCE_TYPE, val);
		super.setValue(val);
	}
	
	public Nonce() throws IllegalArgumentException {
		super(StunConstants.NONCE_TYPE, null);
		setValue();
	}
	
	public void setValue() {
		String nonce = ((Long)System.currentTimeMillis()).toString();
		try {
			MessageDigest digester = MessageDigest.getInstance("MD5");
			byte [] nvDigest = digester.digest(nonce.getBytes());
			//StringBuffer hexNV = StunConstants.asHexString(nvDigest);
			//byte [] hex = hexNV.toString().getBytes();
			super.setValue(nvDigest);
		}
		catch (Exception e) {
			StunConstants.logger.warn(PC2LogCategory.STUN, subCat,
					"Nonce value not set because MessageDigest encountered error.");
		}
		
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data. This method also assumes that the value is string based.
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "] value=[" + Conversion.hexString(value)
			+ "] padding=[" + padding + "]";

		return result;
	}
	
}
