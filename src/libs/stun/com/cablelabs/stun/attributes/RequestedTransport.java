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
 * This attribute is used by the client to request a specific transport
 * protocol for the allocated transport address.  It has the following
 * format:
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |    Protocol   |                  Reserved = 0                 |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * The Protocol field specifies the desired protocol.  The codepoints
 * used in this field are taken from those allowed in the Protocol field
 * in the IPv4 header and the NextHeader field in the IPv6 header
 * [Protocol-Numbers].  This specification only allows the use of
 * codepoint 17 (User Datagram Protocol).
 *
 * The RFFU field is set to zero on transmission and ignored on
 * receiption.  It is reserved for future uses.
 *
 *
 * @author ghassler
 *
 */
public class RequestedTransport extends StunAttribute {

	public static int DEFAULT_VALUE_SIZE = 4;
	public static byte UDP_TRANSPORT = 0x11; // Integer 17
	
	public RequestedTransport(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
		setValue(val);
	}
	
	public RequestedTransport(byte [] val) throws IllegalArgumentException {
		super(StunConstants.REQUESTED_TRANSPORT_TYPE, val);
		setValue(val);
	}
	
	public RequestedTransport() throws IllegalArgumentException {
		super(StunConstants.REQUESTED_TRANSPORT_TYPE, null);
		setValue(UDP_TRANSPORT);
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
	
	public boolean isTransportUDP() {
		if (value != null && value[0] == UDP_TRANSPORT)
			return true;
		return false;
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
