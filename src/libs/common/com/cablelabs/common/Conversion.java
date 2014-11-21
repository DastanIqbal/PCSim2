/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;


/**
 * This class provides common conversion routines for 
 * converting data types between basic data types. An
 * example of a common routing would be converting a
 * integer value into a byte [].
 */
public class Conversion {

	public static final int BYTE = 8;
	public static final int CHAR_SIZE = (Character.SIZE / BYTE);
	public static final int INT_SIZE = (Integer.SIZE / BYTE);
	public static final int LONG_SIZE = (Long.SIZE / BYTE);
	public static final int FLOAT_SIZE = (Float.SIZE / BYTE);
	public static final int DOUBLE_SIZE = (Double.SIZE / BYTE);
	public static final String ZONE_DELIMITER = "%";
	public static final String IPv6_SHORT_FORM_MARKER = "::";
	
	public static final String addZone(String addr, String zone) throws IllegalArgumentException {
		if (addr == null) 
			throw new IllegalArgumentException("The addr argument is null.");
		else if (zone == null)
			throw new IllegalArgumentException("The zone argument is null.");
		else if (zone.equals("0"))
			return addr;
		else 
			return addr + ZONE_DELIMITER + zone;
		
	}
	/** 
	 * Gets the address portion of the value
	 * or null if it doesn't exist.
	 * @return
	 */
	public static String address2String(byte [] addr) {
		if (addr != null) {
			try {
				InetAddress ia = InetAddress.getByAddress(addr);
				return ia.toString();
			}
			catch (UnknownHostException uhe) {
				return null;
			}
		}
		return null;
	}
	
	
	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * The format of the string is a single string of hex
	 * without any leading '0x' or spacing between bytes.
	 * 
	 * @param buffer - the bytes to convert
	 * @return - the string representation
	 */
	public static StringBuffer asHex(byte [] buffer) {
		StringBuffer iStr = new StringBuffer();
	       for(int i = 0; i < buffer.length; i++) {
	            if((buffer[i]&0xFF) <= 15)
	            {
	                iStr.append("0");
	            }
	            iStr.append(Integer.toHexString(buffer[i]&0xff).toLowerCase());
 	        }
	       return iStr;
	}
	
	public static int byteArrayToInt(byte [] val) {
		int i = -1;
		if (val.length == INT_SIZE) {
			i =  (((val[0] & 0xFF) << 24) |
					((val[1] & 0xFF) << 16) |
					((val[2] & 0xFF) << 8) | 
					(val[3] & 0xFF));
			
		}
		return i;
	}
	
	public static int byteArrayToInt(byte [] val, int offset, int length) {
		int i = -1;
		if (length == INT_SIZE) {
			i =  (((val[offset] & 0xFF) << 24) |
					((val[offset+1] & 0xFF) << 16) |
					((val[offset+2] & 0xFF) << 8) | 
					(val[offset+3] & 0xFF));
			
		}
		else if (length == 3) {
			i =  (((0x00 & 0xFF) << 24) |
					((val[offset] & 0xFF) << 16) |
					((val[offset+1] & 0xFF) << 8) | 
					(val[offset+2] & 0xFF));
		}
		else if (length == CHAR_SIZE) {
			i =  (((0x00 & 0xFF) << 24) |
					((0x00 & 0xFF) << 16) |
					((val[offset] & 0xFF) << 8) | 
					(val[offset+1] & 0xFF));
		}
		else if (length == 1) {
			i =  (((0x00 & 0xFF) << 24) |
					((0x00 & 0xFF) << 16) |
					((0x00 & 0xFF) << 8) | 
					(val[offset] & 0xFF));
		}
		return i;
	}
	
	public static long byteArrayToLong(byte [] val) {
		long i = -1;
		if (val.length == LONG_SIZE) {
			i =  (((val[0] & 0xFF) << 56) |
					((val[1] & 0xFF) << 48) |
					((val[2] & 0xFF) << 40) |
					((val[3] & 0xFF) << 32) |
					((val[4] & 0xFF) << 24) |
					((val[5] & 0xFF) << 16) |
					((val[6] & 0xFF) << 8) |
					(val[7] & 0xFF));
			
		}
		return i;
	}
	
	public static byte charToByte(char x) {
		switch (x) {
		case '0' :
			return 0x00;
		case '1' :
			return 0x01;
		case '2' :
			return 0x02;
		case '3' :
			return 0x03;
		case '4' :
			return 0x04;
		case '5' :
			return 0x05;
		case '6' :
			return 0x06;
		case '7' :
			return 0x07;
		case '8' :
			return 0x08;
		case '9' :
			return 0x09;
		case 'a' :
		case 'A' :
			return 0x0a;
		case 'b' :
		case 'B' :
			return 0x0b;
		case 'c' :
		case 'C' :
			return 0x0c;
		case 'd' :
		case 'D' :
			return 0x0d;
		case 'e' :
		case 'E' :
			return 0x0e;
		case 'f' :
		case 'F' :
			return 0x0f;
		default :
			return 0x00;
		}
	}
	
	public static byte[] charToByteArray(char c) {
//		byte[] byteArray = new byte[2];
//		byteArray[0] = (byte)((c & 0x0000FF00)>>>8);
//		byteArray[1] = (byte)((c & 0x000000FF));
//		return (byteArray);
		ByteBuffer bb = ByteBuffer.allocate(CHAR_SIZE);
			bb.putChar(c);
			return bb.array();
	}
	

	
	public static byte [] doubleToByteArray(double i) {
		ByteBuffer bb = ByteBuffer.allocate(DOUBLE_SIZE);
		bb.putDouble(i);
		return bb.array();
	}
	
	public static byte [] floatToByteArray(float i) {
		ByteBuffer bb = ByteBuffer.allocate(FLOAT_SIZE);
		bb.putFloat(i);
		return bb.array();
	}
	
	/**
	 * Converts a hex string representation into a byte array. It will
	 * remove any leading 0x from any and all bytes as well as accept
	 * spaces between bytes.
	 * @return - the byte array that the string represents
	 */
	public static byte[] hexStringToByteArray(String s) { 
	    String tmp = null;
	    if (s.startsWith("0x"))
	    	tmp = s.replaceAll("0x", "");
	    else
	    	tmp = s;
	    if (tmp.contains(" "))
	    	tmp = tmp.replace(" ", "");
	    int len = tmp.length(); 
	    byte[] data = new byte[len / 2]; 
	    for (int i = 0; i < len; i += 2) { 
	        data[i / 2] = (byte) ((Character.digit(tmp.charAt(i), 16) << 4) 
	                             + Character.digit(tmp.charAt(i+1), 16)); 
	    } 
	    return data; 
	} 

	
	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * The format of the string is "length" bytes of hex then a new line.
	 * 
	 * @param buffer - the bytes to convert
	 * @param length - number of bytes per line
	 * @return - the string representation
	 */
	public static StringBuffer formattedHexString(byte [] buffer, int length) {
		StringBuffer iStr = new StringBuffer();
		for(int i = 0; i < buffer.length; i++) {
			iStr.append("0x");
			if((buffer[i]&0xFF) <= 15)
				iStr.append("0");

			iStr.append(Integer.toHexString(buffer[i]&0xff).toLowerCase());

			if(i < buffer.length)
				iStr.append(" ");

			if (i != 0 && i % length == (length-1))
				iStr.append("\n");

		}
		return iStr;
	}
	
	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * The format of the string is 4 bytes of hex then a new line.
	 * 
	 * @param buffer - the bytes to convert
	 * @return - the string representation
	 */
	public static StringBuffer formattedHexString(byte [] buffer) {
		StringBuffer iStr = new StringBuffer();
		for(int i = 0; i < buffer.length; i++) {
			iStr.append("0x");
			if((buffer[i]&0xFF) <= 15)
				iStr.append("0");

			iStr.append(Integer.toHexString(buffer[i]&0xff).toLowerCase());

			if(i < buffer.length)
				iStr.append(" ");

			if (i != 0 && i % 4 == 3)
				iStr.append("\n");

		}
		return iStr;
	}
	
	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * The format of the string is a four bytes of hex per line.
	 * 
	 * @param buffer - the bytes to convert
	 * @return - the string representation
	 */
	public static StringBuffer formatHexString(byte [] buffer) {
		StringBuffer iStr = new StringBuffer();
	       for(int i = 0; i < buffer.length; i++) {
	            if((buffer[i]&0xFF) <= 15)
	            {
	                iStr.append("0");
	            }
	            iStr.append(Integer.toHexString(buffer[i]&0xff).toLowerCase());
 	        }
	       return iStr;
	}
	
	public static char getChar(byte [] buffer, int offset) {
		char preview = 0x0000;
		if (buffer.length >= CHAR_SIZE &&
				((buffer.length - offset) >= 1))
			preview = (char)(((buffer[offset] & 0xFF) << 8) | 
					(buffer[offset+1]&0xFF));
		return preview;
	}
	
	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * The format of the string is a single string of hex with
	 * each byte separated by a space.
	 * 
	 * @param buffer - the bytes to convert
	 * @return - the string representation
	 */
	public static StringBuffer hexString(byte [] buffer) {
		StringBuffer iStr = new StringBuffer();
	       for(int i = 0; i < buffer.length; i++)  {
	            iStr.append("0x");
	            if((buffer[i]&0xFF) <= 15)
	            {
	                iStr.append("0");
	            }

	            iStr.append(Integer.toHexString(buffer[i]&0xff).toLowerCase());

	            if(i < buffer.length)
	                iStr.append(" ");
	          
	        }
	       return iStr;
	}
	
	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * The format of the string is a single string of hex with
	 * each byte separated by a space.
	 * 
	 * @param buffer - the bytes to convert
	 * @param offset - the offset to begin the value
	 * @param length - the number of bytes to process
	 * 	from the starting position defined by the offset
	 * 	parameter.
	 * 
	 * @return - the string representation
	 */
	public static StringBuffer hexString(byte [] buffer, int offset, int length) {
		StringBuffer iStr = new StringBuffer();
	     if ((buffer.length - offset) >= length) {
	    	 for(int i = offset; i < (offset+length); i++)  {
	   
	            iStr.append("0x");
	            if((buffer[i]&0xFF) <= 15)
	                iStr.append("0");
	            

	            iStr.append(Integer.toHexString(buffer[i]&0xff).toLowerCase());

	            if (i < buffer.length)
	                iStr.append(" ");
	          
	        }
	     }
	     return iStr;
	     
	}
	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * The format of the string is a single string of hex with
	 * each byte separated by a space.
	 * 
	 * @param buffer - the bytes to convert
	 * @return - the string representation
	 */
	public static StringBuffer hexString(char buffer) {
		byte [] twoBytes = { (byte)(buffer >> 8 & 0xff), (byte)(buffer & 0xff)  };
		return hexString(twoBytes);
	}
	
	public static String hexStringToString(String val) {
		String result;
		Integer bytes = val.length()/2;
		byte [] hex = new byte [bytes];
		int pos = 0;
		for (int i=0; i<val.length(); i=i+2) {
			hex[pos]= (byte)((charToByte(val.charAt(i)) << 4) | 
					(charToByte(val.charAt(i+1)) & 0x0F));
			pos++;
		 }
		result = new String(hex);
		return result;
	}
	
	/**
	 * A common routine to determine if a string represents an IPv6 address
	 * or not. This is only done by examining if the string contains the 
	 * ':' character or not. It doesn't make any distinction whether the string
	 * is a valid address nor even if it is syntacticly correct.
	 * 
	 * @param addr
	 * @return - true if the String contains ':', false otherwise.
	 */
	public static boolean isIPv6Address(String addr) {
		if (addr.contains(":")) {
			return true;
		}
		return false;
	}

	public static byte [] intToByteArray(int i) {
		ByteBuffer bb = ByteBuffer.allocate(INT_SIZE);
		bb.putInt(i);
		return bb.array();
	}

	public static byte [] longToByteArray(long i) {
		ByteBuffer bb = ByteBuffer.allocate(LONG_SIZE);
		bb.putLong(i);
		return bb.array();
	}
	
	public static String makeAddrURL(String addr, String zone) throws IllegalArgumentException {
		if (addr == null) 
			throw new IllegalArgumentException("The addr argument is null.");
		else if (zone == null)
			throw new IllegalArgumentException("The zone argument is null.");
		else if (zone.equals("0"))
			return "[" + addr + "]";
		else 
			return "[" + addr + ZONE_DELIMITER + zone + "]";
			
		
	}
	
	/**
	 * Converts a String representation of an IP v6 address into its long form 
	 *  
	 * @param addr  - the string representation of the address e.g "fec0::21e:a142"
	 * 
	 * @return     - the long form of the string e.g fec0:0:0:0:0:21e:a142
	 */
	public static String ipv6LongForm(String addr) throws IllegalArgumentException {
	
		if (addr == null)
			throw new IllegalArgumentException("The addr argument is null.");
		String ipv6Addr = addr;
		int index = addr.indexOf(IPv6_SHORT_FORM_MARKER);
		if (index != -1) {
			StringTokenizer tokens = new StringTokenizer(addr, ":");
			int count = tokens.countTokens();
			if (count <= 7) {
				int missing = 7 - count;
				String insertion = "0";
				for (int i=0; i<missing; i++)
					insertion += ":0";
				ipv6Addr = addr.substring(0, index+1) 
					+ insertion 
					+ addr.substring(index+1);
			}
    	}
		return ipv6Addr;
	}
	
	/**
	 * Converts a String representation of an IP v6 address into its long form 
	 *
	 * @param addr  - the string representation of the address e.g "fec0:0:0:0:0:21e:a142"
	 * 
	 * @return     - the long form of the string e.g fec0::21e:a142
	 *  
	 */
	public static String ipv6ShortForm(String addr) throws IllegalArgumentException {
	
		if (addr == null)
			throw new IllegalArgumentException("The addr argument is null.");
		String ipv6Addr = addr;
		if (addr.contains(":")) {
			int start = -1;
			String pattern = ":0:";
			int length = 0;
			int index = addr.indexOf(pattern);
			if (index != -1) {
				// We only convert to short form if there are at least two or more consecutive :0:0: zeros
				boolean reduce = false;
				start = index;
				length = 3;
				// move past the initial ":0" to look for the next pattern match
				index += 2;
				// make sure the zeros are consecutive
				int prevIndex = index;
				index = addr.indexOf(pattern, index);
				while (index != -1 && index == prevIndex) {
					length += 2;
					index += 2;
					prevIndex = index;
					index = addr.indexOf(pattern, index);
					reduce = true;
				}
										
				if (reduce)
					ipv6Addr = addr.substring(0, start) + "::" + addr.substring((start+length));
			}
		}
		return ipv6Addr;
	}

}
