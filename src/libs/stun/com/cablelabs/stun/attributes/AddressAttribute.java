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

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.stun.StunConstants;
import com.cablelabs.common.*;
import java.net.*;

public class AddressAttribute extends StunAttribute {

	public AddressAttribute(char type) throws IllegalArgumentException {
		super(type, null);
	}
	
	public AddressAttribute(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);
	}
	
	public AddressAttribute(char type, char family, int port, byte [] addr ) throws IllegalArgumentException {
		super(type,null);
		setValue(family, port, addr);
	}
	
	public AddressAttribute(char type, char family, int port, String addr ) throws IllegalArgumentException {
		super(type,null);
		try {
			InetAddress ia = InetAddress.getByName(addr);
			setValue(family,port,ia.getAddress());
		}
		catch (UnknownHostException uhe) {
			
		}
	}
	
	public void setValue(char family, int port, byte [] addr) {
		if (family == StunConstants.FAMILY_IPv4) {
			// The -4 is for the family and port lengths
			if  (addr.length == (StunConstants.STUN_IPv4_ADDRESS_LENGTH - 4)) {
				value = new byte[StunConstants.STUN_IPv4_ADDRESS_LENGTH];
				if (value != null) {
					byte [] fam = Conversion.charToByteArray(family);
					byte [] portNum = StunConstants.lengthToByteArray(port);
					System.arraycopy(fam, 0, value, 0, fam.length);
					System.arraycopy(portNum, 0, value, 2, portNum.length);
					System.arraycopy(addr, 0, value, 4, addr.length);
				}
			}
			else {
				StunConstants.logger.warn(PC2LogCategory.STUN, "",
						"The length of the value field [" + addr.length + " doesn't equal " 
						+ StunConstants.STUN_IPv4_ADDRESS_LENGTH + ". Can't build address attribute.");

			}
		}	
		else if (family == StunConstants.FAMILY_IPv6) {
			// The -4 is for the family and port lengths
			if (addr.length == (StunConstants.STUN_IPv6_ADDRESS_LENGTH - 4)) {
				value = new byte[StunConstants.STUN_IPv6_ADDRESS_LENGTH];

				if (value != null) {
					byte [] fam = Conversion.charToByteArray(family);
					byte [] portNum = StunConstants.lengthToByteArray(port);
					System.arraycopy(fam, 0, value, 0, fam.length);
					System.arraycopy(portNum, 0, value, 2, portNum.length);
					System.arraycopy(addr, 0, value, 4, addr.length);
				}
			}
			else {
				StunConstants.logger.warn(PC2LogCategory.STUN, "",
						"The length of the value field [" + addr.length + " doesn't equal " 
						+ StunConstants.STUN_IPv6_ADDRESS_LENGTH + ". Can't build address attribute.");

			}
		}
		else  {
			StunConstants.logger.warn(PC2LogCategory.STUN, "",
					"The family argument " +
				 	(int)family + " is not valid. It must be either " 
				 + (int)StunConstants.FAMILY_IPv4 + " or " 
				 + (int)StunConstants.FAMILY_IPv6
				 + ". Constructor received [" + family + "].");
		}
	}
	
	/** 
	 * Gets the address portion of the value
	 * or null if it doesn't exist.
	 * @return
	 */
	public byte [] getAddress() {
		if (value != null) {
			byte [] temp = new byte [value.length - 4];
			System.arraycopy(value, 4, temp, 0, (value.length-4));
			return temp;
		}
		return null;
	}
	
	/**
	 * Gets the family field from the value or
	 * returns null if it doesn't exist
	 * @return
	 */
	public Character getFamily() {
		if (value != null) {
			char family = (char)(((value[0] << 8) & 0xFF00) | (value[1] & 0xFF));
			return family;
		}
		return null;
	}
	
	/**
	 * Gets the port field from the value or 
	 * returns null if it doesn't exist
	 * @return
	 */
	public Integer getPort() {
		if (value != null) {
			String temp = new String(value, 2, 2);
			Integer port = new Integer(temp);
			if (port != null)
				return port;
		}
		return null;
	}
	
	/**
	 * Sets the address field of the value.
	 * @param address
	 */
	public void setAddress(byte [] address) {
		if (value != null) {
			if ((address.length - 4) == value.length)
				System.arraycopy(address, 0, value, 4, address.length);
			else {
				// Since the size of value doesn't match create a new
				// one
				byte [] temp = new byte [address.length + 4];
				// Copy the family and port from value to the new array
				System.arraycopy(value, 0, temp, 0, 4);
				System.arraycopy(address, 0, temp, 4, address.length);
				// Now assign temp to value
				value = temp;
			}
		}
		else {
			StunConstants.logger.warn(PC2LogCategory.STUN, subCat, 
					"The value of the address TLV has not been initialized. Family field of value not changed.");
		}
	}
	
	public void setAddress(String addr) {
		try {
			InetAddress ia = InetAddress.getByName(addr);
			setAddress(ia.getAddress());
		}
		catch (UnknownHostException uhe) {
			
		}
	}
	/**
	 * Sets the family field of the value.
	 * @param family
	 */
	public void setFamily(char family) {
		if (value != null) {
			byte [] fam = Conversion.charToByteArray(family);
			System.arraycopy(fam, 0, value, 0, fam.length);
		}
		else {
			StunConstants.logger.warn(PC2LogCategory.STUN, subCat, 
					"The value of the address TLV has not been initialized. Family field of value not changed.");
		}
	}
	
	/**
	 * Sets the port field of the value.
	 * @param port
	 */
	public void setPort(int port) {
		if (value != null) {
			byte [] portNum = StunConstants.lengthToByteArray(port);
			System.arraycopy(portNum, 0, value, 2, portNum.length);
		}
		else {
			StunConstants.logger.warn(PC2LogCategory.STUN, subCat, 
					"The value of the address TLV has not been initialized. Port field of value not changed.");
		}
	}
	
	/**
	 * This method converts the attribute into a string representation of the 
	 * data.
	 */
	public String toString() {
		String family = "unknown address type";
		if (getFamily() != null) {
			if (getFamily() == StunConstants.FAMILY_IPv4) 
				family = "IPv4 Address";
			else if (getFamily() == StunConstants.FAMILY_IPv6) 
				family = "IPv6 Address";
		}
		String address = null;
		try {
			address  = InetAddress.getByAddress(getAddress()).toString();
		}
		catch (UnknownHostException uhe) {
			address = "????";
		}
		
		int port = getPort();
		String result = " " + StunConstants.getAttributeName(type) 
			+ "=[" + Conversion.hexString(type)
			+ "] valueLen=[" + length 
			+ "]\n\t " + family + " [" + (int)getFamily() + "]\n"
			+ "\t address=[" + address + "]\n" + "\t port=[" + port
			+ "]\n\t value=[" + Conversion.hexString(value) + "]";
//		result += "] valueLen=[" + length 
//		+ "] value=[" + Conversion.hexString(value) + "]";
		return result;
	}
}
