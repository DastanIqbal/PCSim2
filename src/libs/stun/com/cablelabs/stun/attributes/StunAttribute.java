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

import com.cablelabs.stun.*;
import com.cablelabs.common.*;

/** 
 * This is the base calls for all stun attributes. It holds the
 * actual type, length and value fields of the attribute.
 * @author ghassler
 *
 */public class StunAttribute {
    
	protected char type = 0x0000;
	protected int length = 0;
	protected byte [] value = null;
	protected static final String subCat = "Stack";
	protected int padding = 0;
	public static final int WORD_SIZE = 4;
	
	// This constructor is for decoding 
	public StunAttribute(byte [] type, byte [] val ) throws IllegalArgumentException {
		this.type = Conversion.getChar(type, 0);
		// (char)(((type[0]<<8) & 0xFF00) | (type[1]&0xFF));
//		this.length = StunConstants.STUN_ATTRIBUTE_HEADER_LENGTH; 
		if (val != null) {
			this.length = val.length;
			value = new byte [val.length];
			System.arraycopy(val,0, value,0, val.length);
		}
	}
	public StunAttribute(char type, byte [] val ) throws IllegalArgumentException {
		this.type = type;
//		this.length = StunConstants.STUN_ATTRIBUTE_HEADER_LENGTH;
		if (val != null) {
			this.length = val.length;
			value = new byte [val.length];
			System.arraycopy(val,0, value,0, val.length);
		}
	}
	
	public boolean comprehensionRequired() {
		return StunConstants.isComprehensionRequired(type);
	}
	public String getName() {
		return StunConstants.getAttributeName(type);
	}
	
	public char getType() {
		return this.type;
	}
	
	public void setType(char type) {
		this.type = type;
	}
	
	public byte [] getValue() {
		return value;
	}
	
	/**
	 * This method will populate the length and value portion of the attribute
	 * as well as any padding bytes that are necessary.
	 * @param val
	 */
	public void setValue(byte [] val) {
		if (val != null) {
			length = val.length;

			padding = WORD_SIZE - (length % WORD_SIZE);
// DONT ADD THE PADDING TO THE VALUE UNTIL THE ATTRIBUTE
// IS ENCODED FOR TRANSMITTING
			//int len = length;
//			if (padding != 4) {
//				len += padding;
//			}
//			else 
			if (padding == 4)
				padding = 0;

			value = new byte [length];
			System.arraycopy(val, 0, value,0, val.length);
//			if (padding != 0) {
//				int offset = length;
//				for (int i = 0; i< padding;i++)
//					value[offset+i] = 0x00;
//			}
		}
	}
	
	/**
	 * This method will populate the length and value portion of the attribute,
	 * but will force the padding operation to not occur.
	 * @param val
	 */
	public void forceValue(byte [] val) {
		//length = StunConstants.STUN_ATTRIBUTE_TYPE_LENGTH + val.length;
		// First the length is based upon the value before padding
		length = val.length;
		value = new byte [length];
		System.arraycopy(val, 0, value,0, val.length);
	}

	public int encode(byte [] msgBuf, int offset) {
		int attributeSize = StunConstants.STUN_ATTRIBUTE_HEADER_LENGTH;
		byte [] temp = Conversion.charToByteArray(type);
		System.arraycopy(temp, 0, msgBuf, offset, StunConstants.STUN_ATTRIBUTE_TYPE_LENGTH);
		offset += StunConstants.STUN_ATTRIBUTE_TYPE_LENGTH;
		System.arraycopy(StunConstants.lengthToByteArray(length), 0, msgBuf, 
				offset, StunConstants.STUN_LENGTH_LENGTH);
		offset += StunConstants.STUN_LENGTH_LENGTH;
		
		// Value may be null for some attributes like USE-CANDIDATE which has no data
		if (value != null) {
			System.arraycopy(value, 0, msgBuf, offset, value.length);
			attributeSize += value.length;
		}
		// Set the padding to something printable like spaces
		if (padding != 0) {
			int index = offset + value.length;
			for (int i = 0; i < padding ;i++)
				msgBuf[index+i] = 0x20;
			attributeSize += padding;
		}


		return attributeSize;
	}

	/**
	 * The size is the length of the attribute header plus the length
	 * and any padding.
	 * @return
	 */
	public int size() {
		//return length;
		return StunConstants.STUN_ATTRIBUTE_HEADER_LENGTH + length + padding;
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data. This method also assumes that the value is string based.
	 */
	public String toString() {
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "] value=[" + new String(value,0,length) 
			+ "] padding=[" + padding + "]";
		return result;
	}
}
