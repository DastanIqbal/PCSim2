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

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.stun.StunConstants;

public class XorMappedAddress extends AddressAttribute {

	protected byte [] transactionId = null;
	
	public XorMappedAddress(char type, byte [] transactionID) throws IllegalArgumentException {
		super(type, null);
		this.transactionId = transactionID;
	}
	
	public XorMappedAddress(byte [] val, byte [] transactionID) throws IllegalArgumentException {
		super(StunConstants.XOR_MAPPED_ADDRESS_TYPE, val);
		this.transactionId = transactionID;
	}
	public XorMappedAddress(char type, byte [] val, byte [] transactionID ) throws IllegalArgumentException {
		super(type, val);
		this.transactionId = transactionID;
	}
	
	public XorMappedAddress(char type, char family, int port, byte [] addr, byte [] transactionID ) throws IllegalArgumentException {
		super(type, family, port, addr);
		this.transactionId = transactionID;
		setPort(port);
		setAddress(addr, transactionID);
		length = value.length;
	}
	
	public XorMappedAddress(char type, char family, int port, 
			String addr, byte [] transactionID ) throws IllegalArgumentException {
		super(type, family, port, addr);
		this.transactionId = transactionID;
		setPort(port);
		setAddress(addr, transactionID);
		length = value.length;
	}
	
	/**
	 * Gets the address field from the X-Address value or 
	 * returns null if it doesn't exist
	 * @return
	 */
	public byte [] getAddress() {
		if (value != null && transactionId != null) {
			byte [] temp = new byte [value.length - 4];
			int bytes = value.length - 4;
			for(int i = 0; i < bytes; i++)
				temp[i] = (byte)((int)value[i+4] ^ (int)transactionId[i]);
			return temp;
		}
		return null;
	}
	
	/**
	 * Gets the port field from the X-Port value or 
	 * returns null if it doesn't exist
	 * @return
	 */
	public Integer getPort() {
		if (value != null && value.length >= 4) {
			// Before we can return the value, we 
			// must Xor it with the first two bytes of
			// the magic cookie.
			byte [] xport = new byte [StunConstants.STUN_X_PORT_LENGTH];
        	int b0 = ((int)value[2]) ^ ((int)StunConstants.STUN_MAGIC_COOKIE[0]);
        	int b1 = ((int)value[3]) ^ ((int)StunConstants.STUN_MAGIC_COOKIE[1]);
        	xport[0] = (byte)((b0 & 0x000000FF));
        	xport[1] = (byte)((b1 & 0x000000FF));
	        int port = (((xport[0] << 8) & 0xFF00) | (xport[1] & 0xFF));
			
			return port;
		}
		return null;
	}
	
	/**
	 * Converts the address field to a X-Address for transmission to
	 * the client.
	 */
	public void setAddress(byte [] address, byte [] transactionID) {
		if (value != null) {
			if (address.length == ( value.length -4) &&
					transactionID.length >= address.length) {
				byte [] temp = new byte [address.length];
				for(int i = 0; i <address.length; i++)
					temp[i] = (byte)((int)address[i] ^ (int)transactionID[i]);
				System.arraycopy(temp, 0, value, 4, address.length);
				// Retain a copy of the transactionID for requests
				// to retrieve the value
				setTransactionID(transactionID);
			}
			else {
				// Since the size of value doesn't match create a new
				// one
				byte [] temp = new byte [address.length + 4];
				// Copy the family and port from value to the new array
				System.arraycopy(value, 0, temp, 0, 4);
				for(int i = 0; i <address.length; i++)
					temp[i+4] = (byte)((int)address[i] ^ (int)transactionID[i]);
				// Now assign temp to value
				value = temp;
//				 Retain a copy of the transactionID for requests
				// to retrieve the value
				setTransactionID(transactionID);
			}
		}
		else {
			StunConstants.logger.warn(PC2LogCategory.STUN, subCat, 
			"The value of the address TLV has not been initialized. Family field of value not changed.");
		}
	}
	
	/**
	 * Converts the address field to a X-Address for transmission to
	 * the client.
	 */
	public void setAddress(String address, byte [] transactionID) {
		try {
			InetAddress ia = InetAddress.getByName(address);
			setAddress(ia.getAddress(), transactionID);
		}
		catch (UnknownHostException uhe) {
			
		}
	}
	/**
	 * Converts the port value to a X-Port for transmission to
	 * the client.
	 */
	public void setPort(int port) {
		byte [] temp = StunConstants.lengthToByteArray(port);
        if (temp != null && 
        		temp.length == 2 &&
        		value != null) {
        	int b0 = ((int)temp[0]) ^ ((int)StunConstants.STUN_MAGIC_COOKIE[0]);
        	int b1 = ((int)temp[1]) ^ ((int)StunConstants.STUN_MAGIC_COOKIE[1]);
        	value[2] = (byte)((b0 & 0x000000FF));
        	value[3] = (byte)((b1 & 0x000000FF));
        }
        else if (value == null) {
        	StunConstants.logger.warn(PC2LogCategory.STUN, subCat,
        	  "The value of the address TLV has not been initialized. Port field of value not changed.");
        }
	}
	
	public void setTransactionID(byte [] transactionID) {
		transactionId = new byte [transactionID.length];
		//Retain a copy of the transactionID for requests
		// to retrieve the value
		System.arraycopy(transactionID, 0, transactionId, 0, transactionID.length);
	}
	
}
