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

import java.text.Normalizer;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.stun.*;
import com.cablelabs.common.*;

public class MessageIntegrity extends StunAttribute {
	

	public MessageIntegrity(char type, byte [] val ) throws IllegalArgumentException {
		super(type,val);

	}
	
	public MessageIntegrity(byte [] val) throws IllegalArgumentException {
		super(StunConstants.MESSAGE_INTEGRITY_TYPE, val);
	}
	public void calculate(StunMessage msg, String username, String realm, String password) {
		try {
				String key = username + ":" + realm + ":" + normalizePassword(password);
				
				byte [] encoding = msg.encodeForDigest();
				StringBuffer calcMsg = null;
				if (StunConstants.logger.isTraceEnabled(PC2LogCategory.STUN, subCat)) {
					calcMsg = Conversion.hexString(encoding);
				}
				calculate(encoding, key);
	
			
				if (StunConstants.logger.isTraceEnabled(PC2LogCategory.STUN, subCat)) {
					StunConstants.logger.trace(PC2LogCategory.STUN, subCat, 
							" STUN MessageIntegrity calculation details\n message length=" + msg.getLength() 
							+ " msg=" + calcMsg 
							+ "\n input=[" + calcMsg + "]\n" 
							+ " key=[" + Conversion.hexString(key.getBytes())
							+ "]\n");
				}

		}
		catch (IllegalArgumentException ia) {
			StunConstants.logger.warn(PC2LogCategory.STUN, subCat,
					"MessageIntegrity could not be addede to the message because an exception was encountered.\n" 
					+ ia.getMessage() + "\n" + ia.getStackTrace());
		}
		
	}

	private void calculate (byte [] value, String key) {
		try {
			byte[] keyBytes = key.getBytes(); 
		
		SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1"); 
	
		// Get an hmac_sha1 Mac instance and initialize with the key 
		Mac mac = Mac.getInstance("HmacSHA1"); 
		mac.init(signingKey); 
	
		// Compute the hmac on input data bytes 
		byte[] hmac = mac.doFinal(value);
		if (StunConstants.logger.isTraceEnabled(PC2LogCategory.STUN, subCat)) {
			StunConstants.logger.trace(PC2LogCategory.STUN, subCat, "HMAC = " + Conversion.formattedHexString(hmac));
		}
		
		setValue(hmac);
		} catch (Exception e) { 
			StunConstants.logger.trace(PC2LogCategory.STUN, subCat, 
					"Encountered exception while calculating HMAC for MESSAGE-INTEGRITY attribute" + e);
        } 
	}
	public void calculate(StunMessage msg, String password) {
		try {
			
			byte [] encoding = msg.encodeForDigest();
			StringBuffer calcMsg = null;
			if (StunConstants.logger.isTraceEnabled(PC2LogCategory.STUN, subCat)) {
				calcMsg = Conversion.hexString(encoding);
			}
			calculate(encoding, normalizePassword(password));


			if (StunConstants.logger.isTraceEnabled(PC2LogCategory.STUN, subCat)) {
				StunConstants.logger.trace(PC2LogCategory.STUN, subCat, 
						" STUN MessageIntegrity calculation details\n message length=" + msg.getLength() 
						+ " msg=" + calcMsg 
						+ "\n input=[" + calcMsg + "]\n" 
						+ " key=[" + Conversion.hexString(password.getBytes())
						+ "]\n");
			}
			
		}
		catch (IllegalArgumentException ia) {
			StunConstants.logger.warn(PC2LogCategory.STUN, subCat,
					"MessageIntegrity could not be addede to the message because an exception was encountered.\n" 
					+ ia.getMessage() + "\n" + ia.getStackTrace());
		}
		
	}
	
	
	private String normalizePassword(String password) {
		String strNFKC = null;
		// Make sure the password has been normalized
		if (!Normalizer.isNormalized(password, Normalizer.Form.NFKC)) {
			strNFKC = Normalizer.normalize(password, Normalizer.Form.NFKC);
		}
		else {
			strNFKC = password;
		}
		
		return strNFKC;
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
