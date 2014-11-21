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
 * The RESERVATION-TOKEN attribute contains a token that uniquely
 * identifies a relayed transport address being held in reserve by the
 * server.  The server includes this attribute in a success response to
 * tell the client about the token, and the client includes this
 * attribute in a subsequent Allocate request to request the server use
 * that relayed transport address for the allocation.
 *
 * The attribute value is a 64-bit-long field containing the token
 * value.
 *
 * @author ghassler
 *
 */
public class ReservationToken extends StunAttribute {

	public static int DEFAULT_VALUE_SIZE = 8;

	public ReservationToken(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		setValue(val);
	}
	
	public ReservationToken(byte [] val) throws IllegalArgumentException {
		super(StunConstants.RESERVATION_TOKEN_TYPE, val);
		setValue(val);
	}
	
	public void setValue(byte [] val) {
		length = DEFAULT_VALUE_SIZE;
		value = new byte [val.length];
		System.arraycopy(val,0, value,0, val.length);
		
	}
}
