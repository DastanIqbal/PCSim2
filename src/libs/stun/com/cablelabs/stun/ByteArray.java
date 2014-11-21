/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun;

import com.cablelabs.common.Conversion;

/**
 * A simple class to hold a byte [] for the RawData
 * container.
 * @author ghassler
 *
 */
public class ByteArray {

	private byte [] buffer = null;
	
	public ByteArray (byte [] data) {
		if (data != null) {
			buffer = new byte [data.length];
			System.arraycopy(data, 0, buffer, 0, data.length);
		}
	}
	
	public ByteArray (byte [] data, int length) {
		if (data != null &&
				data.length >= length) {
			buffer = new byte [length];
			System.arraycopy(data, 0, buffer, 0, length);
		}
	}
	
	public ByteArray (byte [] data, int offset, int length) {
		if (data != null &&
				(data.length - offset) >= length) {
			buffer = new byte [length];
			System.arraycopy(data, offset, buffer, 0, length);
		}
	}
	
	
	public byte [] getBuffer() {
		return this.buffer;
	}
	
	public int length() {
		return this.buffer.length;
	}
	public String toString() {
		if (buffer != null) {
			return Conversion.hexString(buffer).toString();
		}
		return null;
	}
}
