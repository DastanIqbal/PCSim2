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

public class MappedAddress extends AddressAttribute {

	public MappedAddress(char type) throws IllegalArgumentException {
		super(type, null);
	}
	
	public MappedAddress(byte [] val) throws IllegalArgumentException {
		super(StunConstants.MAPPED_ADDRESS_TYPE, null);
	}
	
	public MappedAddress(char type, byte [] val ) throws IllegalArgumentException {
		super(type, val);
	}
	
	public MappedAddress(char type, char family, int port, byte [] addr ) throws IllegalArgumentException {
		super(type, family, port, addr);
	}
	
	public MappedAddress(char type, char family, int port, String addr ) throws IllegalArgumentException {
		super(type, family, port, addr);
	}
}
