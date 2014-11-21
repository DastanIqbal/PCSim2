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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import com.cablelabs.common.*;
import com.cablelabs.stun.*;


public class FingerPrint extends StunAttribute {

	public static int DEFAULT_VALUE_SIZE = 4;
	
	public FingerPrint(char type, byte [] val ) throws IllegalArgumentException {
		// This creates an empty value for FingerPrint
		super(type, new byte [StunConstants.STUN_FINGERPRINT_LENGTH]);
		super.setValue(val);
	}
	
	public FingerPrint(byte [] val) throws IllegalArgumentException {
		super(StunConstants.FINGERPRINT_TYPE, val);
		super.setValue(val);
	}
	
	public void calculate(StunMessage msg) {	
		byte [] msgBytes = msg.encodeForFingerPrint();
		//System.out.println("Msg: " + toHex(msgBytes));
		Checksum checksum = new CRC32();
		checksum.update(msgBytes, 0, msgBytes.length);
		int crc = (int)checksum.getValue();
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putInt(crc);
//	    System.out.println("Calc'd CRC32      : " + Conversion.formattedHexString(buf.array()));
		
		// calculate the finger print
		int fp = crc ^ 0x5354554e;
		buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putInt(fp);
		super.setValue(buf.array());
//	    byte [] fingerprint = { (byte) 0xe5, (byte) 0x7a, (byte) 0x3b, (byte) 0xcf };
//		System.out.println("Calc'd CRC32 FP   : " + Conversion.formattedHexString(buf.array()));
//		System.out.println("Msg FP and Calc'd match? " + Arrays.equals(buf.array(), fingerprint));
//        System.out.println();
	}
	
	public String toHex(byte[] b){
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < b.length; i++) {
            String hex = Integer.toHexString(0xFF & b[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
         }

        return sb.toString().toUpperCase();
    }
	
//	public byte [] getValue() {
//		byte [] temp = new byte [value.length];
//		for (int i=0; i< StunConstants.STUN_FINGERPRINT_LENGTH; i++)
//			temp[i] = (byte)((int)value[i] ^ (int)StunConstants.STUN_FINGERPRINT_XOR_VALUE[i]);
//		return temp;
//	}
	
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
