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

/**
 * The DATA attribute is present in all Data Indications and most Send
 * Indications.  It contains raw payload data that is to be sent (in the
 * case of a Send Request) or was received (in the case of a Data
 * Indication).
 *
 * @author ghassler
 *
 */
public class Data extends StunAttribute {

	public Data(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		super.setValue(val);
	}
	
	public Data(byte [] val) throws IllegalArgumentException {
		super(StunConstants.DATA_TYPE, val);
		super.setValue(val);
	}
	
}
