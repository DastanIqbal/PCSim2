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
 * This attribute allows the client to request certain properties for
 * the relayed transport address that is allocated by the server.  The
 * attribute is 32 bits long.  Its format is:
 *
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |   Prop-type   |                  Reserved = 0                 |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * The field labeled "Prop-type" is an 8-bit field specifying the
 * desired property.  The rest of the attribute is RFFU (Reserved For
 * Future Use) and MUST be set to 0 on transmission and ignored on
 * reception.  The values of the "Prop-type" field are:
 *
 *   0x00  (Reserved)
 *   0x01  Even port number
 *   0x02  Pair of ports
 *
 *
 * If the value of the "Prop-type" field is 0x01, then the client is
 * requesting the server allocate an even-numbered port for the relayed
 * transport address.
 *
 * If the value of the "Prop-type" field is 0x02, then client is
 * requesting the server allocate an even-numbered port for the relayed
 * transport address, and in addition reserve the next-highest port for
 * a subsequent allocation.
 *
 * All other values of the "Prop-type" field are reserved.
 *
 *
 * @author ghassler
 *
 */
public class RequestedProps extends StunAttribute {

	public static int DEFAULT_VALUE_SIZE = 4;
	public static byte RESERVED = 0x00;
	public static byte EVEN_PORT_NUMBER = 0x01;
	public static byte PAIR_OF_PORTS = 0x02;
	
	public RequestedProps(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
	}
	
	public RequestedProps(byte [] val) throws IllegalArgumentException {
		super(StunConstants.REQUESTED_PROPS_TYPE, val);
	}
	
	public RequestedProps(byte val) throws IllegalArgumentException {
		super(StunConstants.REQUESTED_PROPS_TYPE, null);
		setValue(val);
	}
	public void setValue(byte [] val) {
		length = DEFAULT_VALUE_SIZE;
		value = new byte [val.length];
		System.arraycopy(val,0, value,0, val.length);
		
	}
	
	public void setValue(byte prop) {
		byte [] rffu = new byte [3];
		length = DEFAULT_VALUE_SIZE;
		value = new byte [DEFAULT_VALUE_SIZE];
		value[0] = prop;
		System.arraycopy(rffu,0, value,1, rffu.length);
	}
	
	public boolean isValidProp() {
		if (value != null && 
				(value[0] == EVEN_PORT_NUMBER || 
						value[0] == PAIR_OF_PORTS)) {
			return true;
		}
		return false;
		
	}
	
	public Byte getProp() {
		if (value != null) 
			return (Byte)value[0];
		return null;
		
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data. 
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "] value=[" + Conversion.hexString(value) + "] padding=[" + padding + "]";
		return result;
	}
}
