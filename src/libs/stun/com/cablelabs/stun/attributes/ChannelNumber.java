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
 * The CHANNEL-NUMBER attribute contains the number of the channel.  It
 * is a 16-bit unsigned integer, followed by a two-octet RFFU (Reserved
 * For Future Use) field which MUST be set to 0 on transmission and
 * ignored on reception.
 *
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |        Channel Number         |         Reserved = 0          |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   
 * @author ghassler
 *
 */
public class ChannelNumber extends StunAttribute {

	public static int DEFAULT_VALUE_SIZE = 4;
	
	public ChannelNumber(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
	}
	
	public ChannelNumber(byte [] val) throws IllegalArgumentException {
		super(StunConstants.CHANNEL_NUMBER_TYPE, val);
	}
	
	public ChannelNumber(int val) throws IllegalArgumentException {
		super(StunConstants.CHANNEL_NUMBER_TYPE, null);
		setValue(val);
	}
	
	public void setValue(int channel) {
		byte [] channelNum = StunConstants.lengthToByteArray(channel);
		byte [] rffu = new byte [2];
		super.length = DEFAULT_VALUE_SIZE;
		value = new byte [DEFAULT_VALUE_SIZE];
		System.arraycopy(channelNum,0, value,0, channelNum.length);
		System.arraycopy(rffu,0, value,2, rffu.length);
	}
	
	public Character getChannel() {
		if (value != null) {
			char channel = Conversion.getChar(value, 0);
			//(char)(((value[0]<<8) & 0xFF00) | (value[1]&0xFF));
			return channel;
		}
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
			+ "] value=[" + Conversion.hexString(value, 0, 2)
			+ "] padding=[" + padding + "]";
		return result;
	}
}
