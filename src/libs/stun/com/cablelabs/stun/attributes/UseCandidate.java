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
 * The USE-CANDIDATE attribute indicates that the candidate pair
 * resulting from this check should be used for transmission of media.
 * The attribute has no content (the Length field of the attribute is
 * zero); it serves as a flag.  It has an attribute value of 0x0025.
 * @author ghassler
 *
 */
public class UseCandidate extends StunAttribute {

	public UseCandidate(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		super.setValue(val);
		if (val != null) 
			throw new IllegalArgumentException("The val parameter must be null for the UseCandidate attribute.");
	}
	
	public UseCandidate(byte [] val) throws IllegalArgumentException {
		super(StunConstants.USE_CANDIDATE_TYPE, null);
		super.setValue(null);
		if (val != null && val.length != 0) {
			throw new IllegalArgumentException("The val parameter must be null for the UseCandidate attribute.");
		}
	}
	
	public UseCandidate() {
		super(StunConstants.USE_CANDIDATE_TYPE, null);
//		byte [] val = new byte[4];
//		super.setValue(val);
		
	}
	
	
	public void setValue(byte [] val) {
		super.setValue(val);
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data. 
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length;
			if (value != null)
				result += "] value=[" + Conversion.byteArrayToInt(value);
						
			result += "] padding=[" + padding + "]";
		return result;
	}
}
