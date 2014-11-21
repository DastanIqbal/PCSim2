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

public class AlternateServer extends AddressAttribute {

	
	public AlternateServer(char type) throws IllegalArgumentException {
		super(type, null);
	}
	
	public AlternateServer(byte [] val) throws IllegalArgumentException {
		super(StunConstants.ALTERNATE_SERVER_TYPE, val);
	}
	
	// This constructor is for decoding 
	public AlternateServer(char type, byte [] val ) throws IllegalArgumentException {
		super(type, val);
	}
	
	public AlternateServer(char type, char family, int port, byte [] addr ) throws IllegalArgumentException {
		super(type, family, port, addr);
	}
}
