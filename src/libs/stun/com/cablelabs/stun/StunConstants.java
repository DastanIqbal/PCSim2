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

import java.util.StringTokenizer;
import com.cablelabs.log.*;

/**
 * This class defines all of the STUN message types allowed within a
 * PC 2.0 XML document and provides a method to validate each entry as it
 * is processed by the parser.
 * 
 * @author ghassler
 *
 */
public class StunConstants {
	
	public static final int STUN_ATTRIBUTE_HEADER_LENGTH = 4;
	public static final int STUN_ATTRIBUTE_TYPE_LENGTH = 2;
	public static final int STUN_FINGERPRINT_LENGTH = 4;
	public static final int STUN_MD5_HASH_LENGTH = 16;
	public static final int STUN_IPv4_ADDRESS_LENGTH = 8;
	public static final int STUN_IPv6_ADDRESS_LENGTH = 20;
	public static final int STUN_X_PORT_LENGTH = 2;
	// Get the number of bytes used by the system
	public static final int INT_SIZE = (Integer.SIZE / 8);
	public static final int LONG_SIZE = (Long.SIZE / 8);
	public static final int STUN_MESSAGE_INTEGRITY_LENGTH = 20;
	
	public static final int MESSAGE_INTEGRITY_LENGTH = 24;
	// Name of Logger
	public static final String loggerName = "Stun";
	
	// ATTRIBUTE HEX VALUES
	// From: draft-ietf-behave-rfc3489bis-15
	// Comprehension-required range (0x0000-0x7FFF):
	//       0x0000: (Reserved)
	//       0x0001: MAPPED-ADDRESS
	//       0x0002: (Reserved; was RESPONSE-ADDRESS)
	//       0x0006: USERNAME
	//       0x0007: (Reserved; was PASSWORD)
	//       0x0008: MESSAGE-INTEGRITY
	//       0x0009: ERROR-CODE
	//       0x000A: UNKNOWN-ATTRIBUTES
	//       0x0014: REALM
	//       0x0015: NONCE
	//       0x0020: XOR-MAPPED-ADDRESS
	//
	//       TURN Attributes
	//	    0x000C: CHANNEL-NUMBER
	//	    0x000D: LIFETIME
	//	    0x0010: BANDWIDTH
	//	    0x0012: PEER-ADDRESS
	//	    0x0013: DATA
	//	    0x0016: RELAY-ADDRESS
	//	    0x0018: REQUESTED-PROPS
	//	    0x0019: REQUESTED-TRANSPORT
	//	    0x0022: RESERVATION-TOKEN
	//
	//       ICE Attributes
	//      0x0024: PRIORITY
	//      0x0025: USE-CANDIDATE
	//      0x8029 ICE-CONTROLLED
    //      0x802A ICE-CONTROLLING

	
    // Comprehension-optional range (0x8000-0xFFFF)
    //       0x8022: SOFTWARE
    //       0x8023: ALTERNATE-SERVER
    //       0x8028: FINGERPRINT
	public static final char MAPPED_ADDRESS_TYPE = 0x0001;
	public static final char USERNAME_TYPE = 0x0006;
	public static final char PASSWORD_TYPE = 0x0007;
    public static final char MESSAGE_INTEGRITY_TYPE = 0x0008;
    public static final char ERROR_CODE_TYPE = 0x0009;
    public static final char UNKNOWN_ATTRIBUTES_TYPE = 0x000A;
    public static final char REALM_TYPE = 0x000E;
    public static final char NONCE_TYPE = 0x000F;
    public static final char XOR_MAPPED_ADDRESS_TYPE = 0x0020;
    public static final char CHANNEL_NUMBER_TYPE = 0x000C;
    public static final char LIFETIME_TYPE = 0x000D;
    public static final char BANDWIDTH_TYPE = 0x0010;
    public static final char PEER_ADDRESS_TYPE = 0x0012;
    public static final char DATA_TYPE = 0x0013;
    public static final char RELAY_ADDRESS_TYPE = 0x0016;
    public static final char REQUESTED_PROPS_TYPE = 0x0018;
    public static final char REQUESTED_TRANSPORT_TYPE = 0x0019;
    public static final char RESERVATION_TOKEN_TYPE = 0x0022;
    public static final char PRIORITY_TYPE = 0x0024;
    public static final char USE_CANDIDATE_TYPE = 0x0025;
    public static final char ICE_CONTROLLED_TYPE = 0x8029;
    public static final char ICE_CONTROLLING_TYPE = 0x802A;
    public static final char SOFTWARE_TYPE = 0x8022;
    public static final char ALTERNATE_SERVER_TYPE = 0x8023;
    public static final char FINGERPRINT_TYPE = 0x8028;
    
    
    
    // Static string representations of the attribure types
    public static final String MAPPED_ADDRESS = "MAPPED-ADDRESS";
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";
    public static final String MESSAGE_INTEGRITY = "MESSAGE-INTEGRITY";
    public static final String ERROR_CODE = "ERROR-CODE";
    public static final String UNKNOWN_ATTRIBUTES = "UKNOWN-ATTRIBUTES";
    public static final String REALM = "REALM";
    public static final String NONCE = "NONCE";
    public static final String XOR_MAPPED_ADDRESS = "XOR-MAPPED-ADDRESS";
    public static final String SOFTWARE = "SOFTWARE";
    public static final String ALTERNATE_SERVER = "ALTERNATE-SERVER";
    public static final String FINGERPRINT = "FINGERPRINT";
    public static final String CHANNEL_NUMBER = "CHANNEL-NUMBER";
    public static final String BANDWIDTH = "BANDWIDTH";
    public static final String PEER_ADDRESS = "PEER-ADDRESS";
    public static final String DATA = "DATA";
    public static final String RELAY_ADDRESS = "RELAY-ADDRESS";
    public static final String REQUESTED_PROPS = "REQUESTED-PROPS";
    public static final String REQUESTED_TRANSPORT = "REQUESTED-TRANSPORT";
    public static final String RESERVATION_TOKEN = "RESERVATION-TOKEN";
    public static final String LIFETIME = "LIFETIME";
    public static final String PRIORITY = "PRIORITY";
    public static final String USE_CANDIDATE = "USE-CANDIDATE";
    public static final String ICE_CONTROLLED = "ICE-CONTROLLED";
    public static final String ICE_CONTROLLING = "ICE-CONTROLLING";

	public static LogAPI logger = LogAPI.getInstance(); 
	public static final int STUN_HEADER_LENGTH = 20;
	public static final int STUN_TRANSACTION_ID_LENGTH = 12;	
	public static final int STUN_MESSAGE_TYPE_LENGTH = 2;
	public static final int STUN_LENGTH_LENGTH = 2;
	
	public static final int STUN_FULL_TRANSACTION_ID_LENGTH = 16;
	// Supported Stun messages
//    0                 1
//    2  3  4 5 6 7 8 9 0 1 2 3 4 5
//
//   +--+--+-+-+-+-+-+-+-+-+-+-+-+-+
//   |M |M |M|M|M|C|M|M|M|C|M|M|M|M|
//   |11|10|9|8|7|1|6|5|4|0|3|2|1|0|
//   +--+--+-+-+-+-+-+-+-+-+-+-+-+-+
//
//Figure 3: Format of STUN Message Type Field
//	Here the bits in the message type field are shown as most-significant
//	   (M11) through least-significant (M0).  M11 through M0 represent a 12-
//	   bit encoding of the method.  C1 and C0 represent a 2 bit encoding of
//	   the class.  A class of 0b00 is a Request, a class of 0b01 is an
//	   indication, a class of 0b10 is a success response, and a class of
//	   0b11 is an error response.  This specification defines a single
//	   method, Binding.  The method and class are orthogonal, so that for
//	   each method, a request, success response, error response and
//	   indication are defined for that method.

	public static final char BINDING_REQUEST_MSG_TYPE = 0x0001;
	public static final char BINDING_RESPONSE_MSG_TYPE = 0x0101;
	public static final char BINDING_ERROR_RESPONSE_MSG_TYPE = 0x0111;
	public static final char ALLOCATE_REQUEST_MSG_TYPE = 0x0003;
	public static final char ALLOCATE_RESPONSE_MSG_TYPE = 0x0103;
	public static final char ALLOCATE_ERROR_RESPONSE_MSG_TYPE = 0x0113;
	public static final char REFRESH_REQUEST_MSG_TYPE = 0x0004;
	public static final char REFRESH_RESPONSE_MSG_TYPE = 0x0104;
	public static final char REFRESH_ERROR_RESPONSE_MSG_TYPE = 0x0114;
	public static final char SEND_INDICATION_MSG_TYPE = 0x0006;
	public static final char DATA_INDICATION_MSG_TYPE = 0x0007;
	public static final char CHANNEL_BIND_REQUEST_MSG_TYPE = 0x0009;
	public static final char CHANNEL_BIND_RESPONSE_MSG_TYPE = 0x0109;
	public static final char CHANNEL_BIND_ERROR_RESPONSE_MSG_TYPE = 0x0119;
	
	// List of currently supported STUN messages
	public static final String BINDING_REQUEST = "BindingRequest";
	public static final String BINDING_RESPONSE = "BindingResponse";
	public static final String BINDING_ERROR_RESPONSE = "BindingErrorResponse";
	public static final String ALLOCATE_REQUEST = "AllocateRequest";
	public static final String ALLOCATE_RESPONSE = "AllocateResponse";
	public static final String ALLOCATE_ERROR_RESPONSE = "AllocateErrorResponse";
	public static final String CHANNEL_BIND_REQUEST = "ChannelBindRequest";
	public static final String CHANNEL_BIND_RESPONSE ="ChannelBindResponse";
	public static final String CHANNEL_BIND_ERROR_RESPONSE = "ChannelBindErrorResponse";
	public static final String REFRESH_REQUEST = "RefreshRequest";
	public static final String REFRESH_RESPONSE = "RefreshResponse";
	public static final String REFRESH_ERROR_RESPONSE = "RefreshErrorReesponse";
	public static final String SEND_INDICATION = "SendIndication";
	public static final String DATA_INDICATION = "DataIndication";
	
	public static final char FAMILY_IPv4 = 0x0001;
	public static final char FAMILY_IPv6 = 0x0002;
	
	/**
	 * STUN Magic cookie for delivering STUN requests received on the
	 * at the MessageProcessor port
	 */
	public static final byte [] STUN_MAGIC_COOKIE = { 0x21, 0x12, (byte)0xA4, 0x42};
	
	/**
	 * STUN Magic cookie offset from start of packet
	 */
	public static int STUN_MAGIC_COOKIE_OFFSET = 4;
	
	/**
	 * STUN Magic cookie length
	 */
	public static int STUN_MAGIC_COOKIE_LENGTH = 4;
	
	public static int STUN_FINGERPRINT_XOR_VALUE = 0x5354554e;
	
	public static byte [] STUN_EMPTY_MESSAGE_INTEGRITY = new byte[20];
	
	public static byte [] STUN_EMPTY_FINGER_PRINT = new byte [4];
	

	
	public static byte [] createErrorPhrase(int errorCode) {
		String phrase = new String();
		switch (errorCode) {
		case 300 :
			phrase = "(Try Alternate): The client should contact an alternate server for "
				+ "this request.  This error response MUST only be sent if the "
				+ "request included a USERNAME attribute and a valid MESSAGE- "
				+ "INTEGRITY attribute; otherwise it MUST NOT be sent and error "
				+ "code 400 (Bad Request) is suggested.  This error response MUST "
				+ "be protected with the MESSAGE-INTEGRITY attribute, and receivers "
				+ "MUST validate the MESSAGE-INTEGRITY of this response before "
				+ "redirecting themselves to an alternate server.";

		case 400 :
			phrase = "(Bad Request): The request was malformed."
				+ " The client should not retry the request without " 
				+ "modification from the previous attempt. The server may not be able to generate a valid "
	            + "MESSAGE-INTEGRITY for this error, so the client MUST NOT expect "
	            + "a valid MESSAGE-INTEGRITY attribute on this response.";
			break;
		case 401 :
			phrase = "(Unauthorized): The request did not contain the correct "
				+ "credentials to proceed. The client should retry the request "
				+ "with proper credentials.";
			break;
		case 420 :
			phrase = "(Unknown Attribute): The server received STUN packet containing a "
				+ "comprehension-required attribute which it did not understand."
				+ "The server MUST put this unknown attribute in the UNKNOWN-"
				+ "ATTRIBUTE attribute of its error response.";
			break;
		case 430 :
			phrase = "(Stale Credentials): The request did contain a MESSAGE-INTEGRITY attribute, " 
				+ "but it used a shared secret that has expired. The client should obtain a new " 
				+ "shared secret and try again.";
			break;
		case 431 : 
			phrase = "(Integrity Check Failure): The request contained a MESSAGE-INTEGRITY attribute, " 
				+ "but the HMAC failed verification. This could be a sign of a potential attack, or client " 
				+ "implementation error.";
			break;
		case 432 :
			phrase = "(Missing Username): The request contained a MESSAGE-INTEGRITY attribute, " 
				+ "but not a USERNAME attribute. Both USERNAME and MESSAGE-INTEGRITY must be "
				+ "present for integrity checks";
			break;
//		case 433: 
//			phrase = "(Use TLS): The Shared Secret request has to be sent over TLS, but was not received over TLS.";
//			break;
//		case 434 :
//			phrase = "(Missing Realm): The REALM attribute was not present in the request.";
//			break;
//		case 435 :
//			phrase = "(Missing Nonce): The NONCE attribute was not present in the request.";
//			break;
//		case 436 :
//			phrase = "(Unknown Username): The USERNAME supplied in the " 
//				+ "request is not known or is not known to the server.";
//			break;
		case  437 :
			phrase = "(Allocation Mismatch): A request was received by the server that "
		      + "requires an allocation to be in place, but there is none, or a "
		      + "request was received which requires no allocation, but there is "
		      + "one.";
			break;
		case 438 :
			phrase = "(Wrong Credentials): The credentials in the (non-Allocate) "
		      + "request, though otherwise acceptable to the server, do not match "
		      + "those used to create the allocation.";
			break;
		case 442 :
			phrase = "(Unsupported Transport Protocol): The Allocate request asked the "
				+ "server to use a transport protocol between the server and the peer "
				+ "that the server does not support.  NOTE: This does NOT refer to "
				+ "the transport protocol used in the 5-tuple.";
			break;
		case 486:
			phrase = "(Allocation Quota Reached): No more allocations using this "
		      + "username can be created at the present time.";
			break;
		case 500 :
			phrase = "(Server Error): The server has suffered a temporary error. " 
				+ "The client should try again.";
			break;
		case 507 :
			phrase = "(Insufficient Bandwidth Capacity): The server cannot create an "
		      + "allocation with the requested bandwidth right now as it has "
		      + "exhausted its capacity.";
			break;
		case 508 :
			phrase = "(Insufficient Port Capacity): The server has no more relayed "
		      + "transport addresses available right now, or has none with the "
		      + "requested properties, or the one that corresponds to the specified "
		      + "token is not available.";
			break;
		case 600 :
			phrase = "(Global Failure): The server is refusing to fulfill the request. " 
				+ "The client should not retry.";
			break;
		}
		return phrase.getBytes();
	}
	
	public static String getMethodName(char type) {
		switch (type) {
		case BINDING_REQUEST_MSG_TYPE : 
			return BINDING_REQUEST;
		case BINDING_RESPONSE_MSG_TYPE : 
			return BINDING_RESPONSE;
		case BINDING_ERROR_RESPONSE_MSG_TYPE : 
			return BINDING_ERROR_RESPONSE;
		case ALLOCATE_REQUEST_MSG_TYPE : 
			return ALLOCATE_REQUEST;
		case ALLOCATE_RESPONSE_MSG_TYPE : 
			return ALLOCATE_RESPONSE;
		case ALLOCATE_ERROR_RESPONSE_MSG_TYPE : 
			return ALLOCATE_ERROR_RESPONSE;
		case CHANNEL_BIND_REQUEST_MSG_TYPE : 
			return CHANNEL_BIND_REQUEST;
		case CHANNEL_BIND_RESPONSE_MSG_TYPE : 
			return CHANNEL_BIND_RESPONSE;
		case CHANNEL_BIND_ERROR_RESPONSE_MSG_TYPE : 
			return CHANNEL_BIND_ERROR_RESPONSE;
		case REFRESH_REQUEST_MSG_TYPE : 
			return REFRESH_REQUEST;
		case REFRESH_RESPONSE_MSG_TYPE : 
			return REFRESH_RESPONSE;
		case REFRESH_ERROR_RESPONSE_MSG_TYPE : 
			return REFRESH_ERROR_RESPONSE;
		case SEND_INDICATION_MSG_TYPE : 
			return SEND_INDICATION;
		case DATA_INDICATION_MSG_TYPE : 
			return DATA_INDICATION;
		default :
			return "Unknown";
		}
	}


	public static String getAttributeName(char type) {
		switch (type) {
		case MAPPED_ADDRESS_TYPE :
			return MAPPED_ADDRESS;
		case USERNAME_TYPE :
			return USERNAME;
		case PASSWORD_TYPE:
			return PASSWORD;
		case MESSAGE_INTEGRITY_TYPE :
			return MESSAGE_INTEGRITY;
		case ERROR_CODE_TYPE :
			return ERROR_CODE;
		case UNKNOWN_ATTRIBUTES_TYPE :
			return UNKNOWN_ATTRIBUTES;
		case REALM_TYPE :
			return REALM;
		case NONCE_TYPE :
			return NONCE;
		case XOR_MAPPED_ADDRESS_TYPE :
			return XOR_MAPPED_ADDRESS;
		case SOFTWARE_TYPE :
			return SOFTWARE;
		case ALTERNATE_SERVER_TYPE :
			return ALTERNATE_SERVER;
		case FINGERPRINT_TYPE :
			return FINGERPRINT;
		case CHANNEL_NUMBER_TYPE : 
			return CHANNEL_NUMBER;
		case BANDWIDTH_TYPE : 
			return BANDWIDTH;
		case PEER_ADDRESS_TYPE : 
			return PEER_ADDRESS;
		case DATA_TYPE : 
			return DATA;
		case RELAY_ADDRESS_TYPE : 
			return RELAY_ADDRESS;
		case REQUESTED_PROPS_TYPE : 
			return REQUESTED_PROPS;
		case REQUESTED_TRANSPORT_TYPE : 
			return REQUESTED_TRANSPORT;
		case RESERVATION_TOKEN_TYPE : 
			return RESERVATION_TOKEN;
		case LIFETIME_TYPE :
			return LIFETIME;
		case PRIORITY_TYPE :
			return PRIORITY;
		case USE_CANDIDATE_TYPE :
			return USE_CANDIDATE;
		case ICE_CONTROLLED_TYPE :
			return ICE_CONTROLLED;
		case ICE_CONTROLLING_TYPE :
			return ICE_CONTROLLING;
		default :
			return "unknown";
		}
	}
	
	public static char getAttributeValue(String name) {
		if (name.equals(MAPPED_ADDRESS))
			return MAPPED_ADDRESS_TYPE;
		else if (name.equals(USERNAME))
			return USERNAME_TYPE;
		else if (name.equals(MESSAGE_INTEGRITY))
			return MESSAGE_INTEGRITY_TYPE;
		else if (name.equals(ERROR_CODE))
			return ERROR_CODE_TYPE;
		else if (name.equals(UNKNOWN_ATTRIBUTES))
			return UNKNOWN_ATTRIBUTES_TYPE;
		else if (name.equals(REALM))
			return REALM_TYPE;
		else if (name.equals(NONCE))
			return NONCE_TYPE;
		else if (name.equals(XOR_MAPPED_ADDRESS))
			return XOR_MAPPED_ADDRESS_TYPE;
		else if (name.equals(SOFTWARE))
			return SOFTWARE_TYPE;
		else if (name.equals(ALTERNATE_SERVER))
			return ALTERNATE_SERVER_TYPE;
		else if (name.equals(FINGERPRINT))
			return FINGERPRINT_TYPE;
		else if (name.equals(CHANNEL_NUMBER))
			return CHANNEL_NUMBER_TYPE;
		else if (name.equals(BANDWIDTH))
			return BANDWIDTH_TYPE;
		else if (name.equals(PEER_ADDRESS))
			return PEER_ADDRESS_TYPE;
		else if (name.equals(DATA))
			return DATA_TYPE;
		else if (name.equals(RELAY_ADDRESS))
			return RELAY_ADDRESS_TYPE;
		else if (name.equals(REQUESTED_PROPS))
			return REQUESTED_PROPS_TYPE;
		else if (name.equals(REQUESTED_TRANSPORT))
			return REQUESTED_TRANSPORT_TYPE;
		else if (name.equals(RESERVATION_TOKEN))
			return RESERVATION_TOKEN_TYPE;
		else if (name.equals(LIFETIME))
			return LIFETIME_TYPE;
		else if (name.equals(PRIORITY))
			return PRIORITY_TYPE;
		else if (name.equals(USE_CANDIDATE))
			return USE_CANDIDATE_TYPE;
		else if (name.equals(ICE_CONTROLLED))
			return ICE_CONTROLLED_TYPE;
		else if (name.equals(ICE_CONTROLLING))
			return ICE_CONTROLLING_TYPE;
		else 
			return 0x0000;
	}
	
	public static boolean isAddressAttribute(char type) {
		switch(type) {
		case MAPPED_ADDRESS_TYPE :
		case XOR_MAPPED_ADDRESS_TYPE :
		case ALTERNATE_SERVER_TYPE :
			return true;
		}
		return false;
     }
	
	/**
	 * A convenient method to determine if the Stun Attribute
	 * requires comprehension on the part of the receiver of the
	 * request or not
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isComprehensionRequired(char type) {
		if (type < 0x8000)
			return true;
		else 
			return false;
	}
	
	/**
	 * Tests the given input is a valid generic STUN message name
	 * @param name
	 * @return true if it matchs a case sensitive STUN message name,]
	 *         false otherwise
	 */
	static public boolean isGenericType(String name) {
		if (BINDING_RESPONSE.equals(name) ||
			BINDING_REQUEST.equals(name) ||
			ALLOCATE_REQUEST.equals(name) ||
			ALLOCATE_RESPONSE.equals(name) ||
			REFRESH_REQUEST.equals(name) ||
			REFRESH_RESPONSE.equals(name) ||
			CHANNEL_BIND_RESPONSE.equals(name) ||
			CHANNEL_BIND_REQUEST.equals(name) ||
			name.equals("Request") ||
			name.equals("Response") ||
			name.equals("Error")) {
			return true;
		}
		return false;
    }
    
	/**
	 * Tests the given input is in a recognizable Stun Error string format.
	 * The format is "status-code"-"Associated Request method name".
	 * e.g 434-BindingRequest or for more generial matches 
	 * e.g 4xx-BindingRequest where x is any valid number 0-9
	 * @param name
	 * @return true if it matchs a case sensitive Stun Error message name,]
	 *         false otherwise
	 */
	static public boolean isErrorType(String name) {
		StringTokenizer tokens = new StringTokenizer(name, "-");
		// Valid syntax consists of two tokens
		if (tokens.countTokens() == 2) {
			String pattern = "[3-6][0-9x][0-9x]";
			// Now determine if the status-code is an exact value or a range
			String token = tokens.nextToken();
			if (token.matches(pattern) && 
				(BINDING_REQUEST.equals(tokens.nextToken()) ||
				ALLOCATE_REQUEST.equals(tokens.nextToken()) ||
				CHANNEL_BIND_REQUEST.equals(tokens.nextToken()) ||
				REFRESH_REQUEST.equals(tokens.nextToken()))) {
				return true;
			}
		}
		return false;
	}
	
	static public boolean isErrorResponse(char type) {
		if (type == BINDING_ERROR_RESPONSE_MSG_TYPE ||
				type == ALLOCATE_ERROR_RESPONSE_MSG_TYPE ||
				type == REFRESH_ERROR_RESPONSE_MSG_TYPE ||
				type == CHANNEL_BIND_ERROR_RESPONSE_MSG_TYPE) {
			return true;
		}
		return false;
	}
	
	static public boolean isIndication(char type) {
		if (type == SEND_INDICATION_MSG_TYPE ||
				type == DATA_INDICATION_MSG_TYPE) {
			return true;
		}
		return false;
	}
	
	static public boolean isKnownComprehension(char attr) {
		if (attr == MAPPED_ADDRESS_TYPE ||
				attr == USERNAME_TYPE ||
				attr == MESSAGE_INTEGRITY_TYPE ||
				attr == ERROR_CODE_TYPE ||
				attr == UNKNOWN_ATTRIBUTES_TYPE ||
				attr == REALM_TYPE ||
				attr == NONCE_TYPE ||
				attr == XOR_MAPPED_ADDRESS_TYPE ||
				attr == CHANNEL_NUMBER_TYPE ||
				attr == LIFETIME_TYPE ||
				attr == BANDWIDTH_TYPE ||
				attr == PEER_ADDRESS_TYPE ||
				attr == DATA_TYPE ||
				attr == RELAY_ADDRESS_TYPE ||
				attr == REQUESTED_PROPS_TYPE ||
				attr == REQUESTED_TRANSPORT_TYPE ||
				attr == RESERVATION_TOKEN_TYPE ||
				attr == PRIORITY_TYPE	||
				attr == USE_CANDIDATE_TYPE	||
				attr == ICE_CONTROLLED_TYPE	||
				attr == ICE_CONTROLLING_TYPE) {
			return true;
		}
		return false;
	}
	
	/**
	 * Tests the given input is in a recognizable RTP Payload string format.
	 * eg. PCMU, PCMA
	 * 
	 * @param name
	 * @return true if it matchs a case sensitive RTP Payload type 
	 *         false otherwise
	 */
	static public boolean isRTPType(String name) {
		if (name.equals("PCMU")) {
			return true;
		}
		return false;
	}
	
	/**
	 * Tests whether the Stun message is a response message
	 * @param name - message type to test
	 * @return - true if it is a Stun response message, false otherwise
	 */
	static public boolean isStunIndication(String name) {
		if (SEND_INDICATION.equals(name) ||
				DATA_INDICATION.equals(name)) {
			return true;
		}
		return false;
	}
	public static boolean isRequest(char type) {
		if (type == BINDING_REQUEST_MSG_TYPE || 
				type == ALLOCATE_REQUEST_MSG_TYPE ||
				type == REFRESH_REQUEST_MSG_TYPE ||
				type == CHANNEL_BIND_REQUEST_MSG_TYPE) {
			return true;
		}
		return false;
	}
	
	public static boolean isResponse(char type) {
		if (type == BINDING_RESPONSE_MSG_TYPE ||
				type == ALLOCATE_RESPONSE_MSG_TYPE ||
				type == REFRESH_RESPONSE_MSG_TYPE ||
				type == CHANNEL_BIND_RESPONSE_MSG_TYPE) {
			return true;
		}
		return false;
	}
	/**
	 * Tests whether the Stun message is a response message
	 * @param name - message type to test
	 * @return - true if it is a Stun response message, false otherwise
	 */
	static public boolean isStunRequest(String name) {
		if (BINDING_REQUEST.equals(name) ||
				ALLOCATE_REQUEST.equals(name) ||
				CHANNEL_BIND_REQUEST.equals(name) ||
				REFRESH_REQUEST.equals(name) ||
				isErrorType(name)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Tests whether the Stun message is a response message
	 * @param name - message type to test
	 * @return - true if it is a Stun response message, false otherwise
	 */
	static public boolean isStunResponse(String name) {
		if (BINDING_RESPONSE.equals(name) ||
				ALLOCATE_RESPONSE.equals(name) ||
				CHANNEL_BIND_RESPONSE.equals(name) ||
				REFRESH_RESPONSE.equals(name) ||
				isErrorType(name)) {
			return true;
		}
		return false;
	}
	/**
	 * Tests the given input for request, response or error type of syntax
	 * @param name - string to compare for valid type
	 * @return - true if it is recognized as a valid case sensitive Stun Message Type,
	 * 			false otherwise.
	 */
	static public boolean isStunType(String name) {
		StringTokenizer tokens = new StringTokenizer(name, "-");
		if (tokens.countTokens() == 1) {
			if ( isGenericType(name))
				return true;
			else if (isRTPType(name)) 
				return true;
			else if (isStunIndication(name))
				return true;
			else
				return false;
		}
		else if (tokens.countTokens() == 2) {
			return isErrorType(name);
		}
		else
			return false;
	}
	
	static public boolean isStunEvent(String event) {
	    return (isStunType(event) || event.equals("RTP"));
	}
	

	static public boolean isValidChannelNumber(char channel) {
		if (channel >= 0x4000 && channel <= 0xFFFE)
			return true;
		return false;
	}
	
	public static byte[] lengthToByteArray(int length) {
		byte[] byteArray = new byte[2];
		byteArray[0] = (byte)((length & 0x0000FF00)>>>8);
		byteArray[1] = (byte)((length & 0x000000FF));
		return (byteArray);
	}
	

}
