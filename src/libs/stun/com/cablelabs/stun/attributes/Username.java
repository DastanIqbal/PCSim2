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

import java.text.Normalizer;

import com.cablelabs.stun.StunConstants;

public class Username extends StunAttribute {


	public Username(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		setValue(val);
	}
	
	public Username(byte [] val) throws IllegalArgumentException {
		super(StunConstants.USERNAME_TYPE, val);
		setValue(val);
	}
	
	public void setValue(byte [] val) {
		String newValue = new String (val, 0, val.length);
		if (!Normalizer.isNormalized(newValue, Normalizer.Form.NFKC)) {
			String strNFKC = Normalizer.normalize(newValue, Normalizer.Form.NFKC);
			super.setValue(strNFKC.getBytes());			
		}
		else {
			super.setValue(newValue.getBytes());
		}
	}
	
	public String getUsername() {
		if (value != null)
			return new String (value, 0, value.length);
		return null;
	}
	
}
