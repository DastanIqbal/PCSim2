/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.tools;

import com.cablelabs.common.Conversion;
import com.cablelabs.stun.StunConstants;
import com.cablelabs.stun.StunMessage;
import com.cablelabs.stun.attributes.ErrorCode;
import com.cablelabs.stun.attributes.StunAttribute;
import com.cablelabs.stun.attributes.Username;
import com.cablelabs.stun.attributes.XorMappedAddress;

public class STUNLocator {

	/**
	 * Private logger for the class
	 */
//	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger("Locators");

	private static STUNLocator locator = null;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
//	private String subCat = "Locator";
	
	/**
	 * Private Constructor
	 *
	 */
	private STUNLocator() {

	}
	
	/**
	 * Retrieves the single instance of the STUNLocator if it 
	 * already exists. If it doesn't exist it will create it prior
	 * to returning it.
	 *
	 */
	public synchronized static STUNLocator getInstance() {
		if (locator == null) {
			locator = new STUNLocator();
		}
		return locator;
	}
	
	/**
	 * This method retrieves the data for an attribute within a STUN message.
		 *
	 * @param hdr - The attribute to find
	 * @param param - The parameter to locate.
	 * @param hdrInstance - the instance of the header to extract the parameter from.
	 * @param msg - The message to search for the header.
	 * 
	 * @return the contents of the parameter or the parameter itself if it is a presence
	 * parameter.
	 * 		
	 * 
	 */
	public synchronized String getSIPParameter(String attr, String param, 
			String attrInstance, StunMessage msg) {

		if (attr.equals("Header")) {
			if (param != null) {
				if (param.equals("transaction_id")) {
					byte [] ti = msg.getTransactionID();
					if (ti != null) {
						String value = Conversion.hexString(ti).toString();
						return value;
					}
				}
			}
		}
		else if (attr.equals("message-length")) {
			int length = msg.getLength();
			return Integer.toString(length);
		}
		else {
			StunAttribute sa = msg.getAttribute(attr);
			if (sa != null) {
				char type = sa.getType();
				switch (type) {
				case StunConstants.XOR_MAPPED_ADDRESS_TYPE:
					XorMappedAddress xma = (XorMappedAddress)sa;
					if (param.equals("IP")) {
						String value = Conversion.hexString(xma.getAddress()).toString();
						return value;	
					}
					else if (param.equals("port")) {
						Integer port = xma.getPort();
						return port.toString();
					}
					break;
				case StunConstants.ERROR_CODE_TYPE:
					ErrorCode ec = (ErrorCode)sa;
					if (param.equals("class")) {
						Integer sc = ec.getStatusCode();
						String value = sc.toString();
						// Only the upper most charater
						// defines the class;
						return value.substring(0,1);
					}
					if (param.equals("number")) {
						Integer sc = ec.getStatusCode();
						String value = sc.toString();
						// Only the final 2 digits
						// defines the number;
						return value.substring(1,3);
					}
					if (param.equals("reason")) {
						byte [] reason = ec.getValue();
						// The first four bytes are the code,
						// everything else is the phrase.
						if (reason.length > 4) {
							String value = new String(reason,4,reason.length-4);
						// Only the upper most charater
						// defines the class;
							return value;
						}
					}
					break;
				case StunConstants.USERNAME_TYPE:
					Username un = (Username)sa;
					if (param.equals("length")) {
						int length = un.size();
						return Integer.toString(length);
					}
       				break;
				default :
					return null;
				}
			}
		}
		return null;
	}
}
