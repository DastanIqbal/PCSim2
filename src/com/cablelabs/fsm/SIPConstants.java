/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

import java.util.StringTokenizer;

/**
 * This class defines all of the SIP message types  allowed within a
 * PC 2.0 XML document and provides a method to validate each entry as it
 * is processed by the parser.
 * 
 * @author ghassler
 *
 */
public class SIPConstants {

	
	// SIP Request message 
	public static final String ACK = "ACK";
	public static final String BYE = "BYE";
	public static final String CANCEL = "CANCEL";
	public static final String INFO = "INFO";
	public static final String INVITE = "INVITE";
	public static final String MESSAGE = "MESSAGE";
	public static final String NOTIFY = "NOTIFY";
	public static final String OPTIONS = "OPTIONS";
	public static final String PRACK = "PRACK";
	public static final String PUBLISH = "PUBLISH";
	public static final String REFER = "REFER";
	public static final String REGISTER = "REGISTER";
	public static final String SUBSCRIBE = "SUBSCRIBE";
	public static final String UPDATE = "UPDATE";
	public static final String REINVITE = "REINVITE";
	
	// Generic SIP message types
	public static final String REQUEST = "Request";
	public static final String RESPONSE = "Response";
	
	
	// MWI message-summary constants
	public static final String MSG_WAITING = "Messages-Waiting";
	public static final String MSG_ACCOUNT = "Message-Account";
	public static final String VOICE_MSG = "Voice-Message";
	public static final String FAX_MSG = "Fax-Message";
	public static final String PAGER_MSG = "Pager-Message";
	public static final String TEXT_MSG = "Text-Message";
	public static final String MWI_TO = "To";
	public static final String MWI_FROM = "From";
	public static final String MWI_SUBJECT = "Subject";
	public static final String MWI_DATE = "Date";
	public static final String MWI_PRIORITY = "Priority";
	public static final String MWI_MSG_ID = "Message-Id";
	
	// SIP Responses take the form 'status code'-'Request method'
	// e.g. 200-Invite or a range can be specified 
	// e.g. 18x-Invite.  

	/**
	 * Determines if the event is a recognizable SIP event
	 */
	static public boolean isSIPEvent(String event) {
	    return isValidType(event);
	}
	/**
	 * Validates the format of the message type
	 * @param name
	 * @return
	 */
	static public boolean isValidType(String name) {

		if (isRequestType(name) || isGenericType(name) || isResponseType(name)) {
			return true;
		}
		return false;
	}
 
	/**
	 * Tests the given input is a valid SIP Request method name
	 * @param name
	 * @return true if it matchs a case insensitive SIP Request method name,
	 *         false otherwise
	 */
	static public boolean isRequestType(String name)    {
		if (ACK.equalsIgnoreCase(name)  ||
				BYE.equalsIgnoreCase(name) ||
				CANCEL.equalsIgnoreCase(name) ||
				INFO.equalsIgnoreCase(name) ||
				INVITE.equalsIgnoreCase(name) ||
				MESSAGE.equalsIgnoreCase(name) ||
				NOTIFY.equalsIgnoreCase(name) ||
				OPTIONS.equalsIgnoreCase(name) ||
				PRACK.equalsIgnoreCase(name) ||
				PUBLISH.equalsIgnoreCase(name) ||
				REFER.equalsIgnoreCase(name) ||
				REGISTER.equalsIgnoreCase(name) ||
				REINVITE.equalsIgnoreCase(name) ||
				SUBSCRIBE.equalsIgnoreCase(name) ||
				UPDATE.equalsIgnoreCase(name)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Tests the given input is a valid generic SIP method name
	 * @param name
	 * @return true if it matchs a case insensitive SIP Request method name,
	 *         false otherwise
	 */
	static public boolean isGenericType(String name) {
		if (RESPONSE.equalsIgnoreCase(name) ||
				REQUEST.equalsIgnoreCase(name)) {
			return true;
		}
		return false;
    }
    
	/**
	 * Tests the given input is in a recognizable SIP Response string format.
	 * The format is "status-code"-"Associated Request method name".
	 * e.g 200-Invite or for more generial matches 
	 * e.g 18x-Invite where x is any valid number 0-9
	 * @param name
	 * @return true if it matchs a case insensitive SIP Request method name,
	 *         false otherwise
	 */
	static public boolean isResponseType(String name) {
		// First separate the string into status-code and method name
		StringTokenizer tokens = new StringTokenizer(name, "-");
		
		// Valid syntax consists of two tokens
		if (tokens.countTokens() == 2) {
			String pattern = "[1-6x][0-9x][0-9x]";
			// Now determine if the status-code is an exact value or a range
			String token = tokens.nextToken();
			if (token.matches(pattern) && isRequestType(tokens.nextToken())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Validates the instance value is understood by the platform.
	 * @param instance
	 * @return
	 */
	static public boolean isValidInstance(String instance) {
		if (instance != null && 
				(instance.equals(MsgQueue.FIRST) ||
				instance.equals(MsgQueue.CURRENT) ||
				instance.equals(MsgQueue.LAST) ||
				instance.equals(MsgQueue.ANY) ||
				instance.equals(MsgQueue.FIRST))) {
			return true;
		}
		else {
			try {
				int number = Integer.parseInt(instance);
				if (number > 0)
					return true;
				else 
					return false;
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

	}
	
	static public String hasCompactForm(String hdr) {
		// Allow-Events = u
		// Call-ID = i
		// Contact = m
		// Content-Encoding = e
		// Content-Length = l
		// Content-Type = c
		// Event = o
		// From = f
		// Refer-To = r
		// Subject = s
		// Supported = k
		// To = t
		// Via = v
		if (hdr == null)
			return hdr;
		if (hdr.equals("Allow-Events"))
			return "u";
		else if (hdr.equals("Call-ID"))
			return "i";
		else if (hdr.equals("Contact"))
			return "m";
		else if (hdr.equals("Content-Encoding"))
			return "e";
		else if (hdr.equals("Content-Length"))
			return "l";
		else if (hdr.equals("Content-Type"))
			return "c";
		else if (hdr.equals("Event"))
			return "o";
		else if (hdr.equals("From"))
			return "f";
		else if (hdr.equals("Refer-To"))
			return "r";
		else if (hdr.equals("Subject"))
			return "s";
		else if (hdr.equals("Supported"))
			return "k";
		else if (hdr.equals("To"))
			return "t";
		else if (hdr.equals("Via")) 
			return "v";;
		return null;
	}
	
	/**
	 * Test whether the specified header is allowed to occur
	 * multiple times on the same line and simply separated
	 * by a comma or not.
	 * 
	 * @param hdr
	 * @return true if the header can have multiple headers
	 * 	separated by a comma, false otherwise.
	 */
	static public boolean multipleHeadersAllowed(String hdr) {
		if (hdr.equals("Authorization") ||
				hdr.equals("Proxy-Authenticate") ||
				hdr.equals("Proxy-Authorization") ||
				hdr.equals("Authentication-Info") ||
						hdr.equals("WWW-Authenticate")) {
			return false;
		}
		return true;
	}
	
	static public boolean canAppearInBody(String hdr) {
		if (hdr != null && 
				(hdr.equals("Content-Type") ||
						hdr.equals("Content-Disposition") ||
						hdr.equals("Content-ID"))) {
			return true;
			
		}
		return false;
	}

	public static boolean isMWIHeader(String hdr) {
		if (hdr != null && 
				(hdr.equals(MSG_WAITING) ||
				hdr.equals(MSG_ACCOUNT) ||
				hdr.equals(VOICE_MSG) ||
				hdr.equals(FAX_MSG) ||
				hdr.equals(PAGER_MSG) ||
				hdr.equals(TEXT_MSG) ||
				hdr.equals(MWI_TO) ||
				hdr.equals(MWI_FROM) ||
				hdr.equals(MWI_SUBJECT) ||
				hdr.equals(MWI_DATE) ||
				hdr.equals(MWI_PRIORITY) ||
				hdr.equals(MWI_MSG_ID))) {
			return true;

		}
		return false;
	}
	
	public static boolean isMWISIPHeader(String hdr) {
		if (hdr.equals(MWI_TO) ||
				hdr.equals(MWI_FROM) ||
				hdr.equals(MWI_SUBJECT) ||
				hdr.equals(MWI_DATE) ||
				hdr.equals(MWI_PRIORITY) ||
				hdr.equals(MWI_MSG_ID)) {
			return true;

		}
		return false;
	}
}
