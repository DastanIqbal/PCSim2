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

/**
 * This class defines all of the SDP header and parameter types allowed within a
 * PC 2.0 XML document and provides a method to validate each entry as it
 * is processed by the parser.
 * 
 * @author ghassler
 *
 */
public class SDPConstants {
	
	// Valid headers that can appear in a test case document
	// Some of these will be replaces by the Header Keys when
	// the test case document is referencing it. The reason for this is to 
	// reduce the complexity within the test document to refer to a 
	// specific instance of a header. Only headers of the same
	// type will need the hdr_instance element set when it is 
	// likely to appear more than once.
	public static final String VERSION = "v";
	public static final String ORIGIN = "o";
	public static final String SESSION = "s";
	public static final String CONNECTION_TYPE = "c";
	//public static final char BANDWIDTH = 'b';
	// Bandwidth has been replaced by the following 
	public static final String MEDIA_MODIFIER = "media-modifier";
	public static final String TIAS_MODIFIER = "TIAS-modifier";
	public static final String MAX_PACKET_RATE = "max-packet-rate";
	public static final String TIME = "t";
	// public static final char MEDIA = 'm';
	// Media (m) has been replaced by
	public static final String AUDIO = "audio";
	public static final String VIDEO = "video";
	public static final String IMAGE = "image";
    //public static final char ATTRIBUTE = 'a';
	// Attribute (a) has been replaced with the following
	public static final String RTPMAP = "rtpmap";
	public static final String MODE = "mode";
	public static final String PTIME = "ptime";
	public static final String FMTP = "fmtp";
	public static final String QOS_CURRENT_REMOTE = "qos-curr-remote";
	public static final String QOS_CURRENT_LOCAL = "qos-curr-local";
	public static final String QOS_CURRENT_E2E = "qos-curr-e2e";
	public static final String QOS_DESIRED_REMOTE = "qos-des-remote";
	public static final String QOS_DESIRED_LOCAL = "qos-des-local";
	public static final String QOS_DESIRED_E2E = "qos-des-e2e";
	public static final String QOS_CONF_REMOTE = "qos-conf-remote";
	public static final String QOS_CONF_LOCAL = "qos-conf-local";
	public static final String QOS_CONF_E2E = "qos-conf-e2e";
	public static final String FAX_VERSION = "fax-version";
	public static final String FAX_MAX_DATAGRAM = "fax-max-datagram";
	public static final String FAX_RATE_MGMT = "fax-rate-management";
	public static final String FAX_UDP_EC = "fax-udp-ec";
	public static final String FMTP_FAX = "fmtp-fax";
	
	// Header keys
	// In order to keep the test scripts cleaner and more managable when
	// dealing with some of the headers that can and probably will appear
	// multiple times in a single message, the platform will map some of
	// the header names above to the corresponding string key to use when
	// searching for the location of the header. The items below are used
	// in the msg_ref class to replace the value read in the xml file with
	// the value that will be used in the platform attempting to locate the
	// header in a message.
	// 
	public static final String V = "v=";
	public static final String O = "o=";
	public static final String S = "s=";
	public static final String C = "c=";
	public static final String T= "t=";
	public static final String BW = "b=";
	public static final String BW_AS = "b=AS:";
	public static final String BW_CT = "b=CT:";
	public static final String BW_TIAS = "b=TIAS:";
	public static final String BW_MAXRATE = "a=maxprate:";
	public static final String A_RTPMAP = "a=rtpmap:";
	public static final String A_PTIME = "a=ptime:";
	public static final String A_MODE = "a=";
	public static final String M_AUDIO = "m=audio ";
	public static final String M_VIDEO = "m=video ";
	public static final String M_IMAGE = "m=image ";
	public static final String A_CUR_QOS_L = "a=curr:qos local ";
	public static final String A_CUR_QOS_R = "a=curr:qos remote ";
	public static final String A_CUR_QOS_E = "a=curr:qos e2e ";
	public static final String LOCAL_SENDRECV = "a=curr:qos local sendrecv";
	public static final String REMOTE_SENDRECV = "a=curr:qos remote sendrecv";
	public static final String A_FAX_VERSION = "a=T38FaxVersion:";
	public static final String A_FAX_MAX_DATAGRAM = "a=T38FaxMaxDatagram:";
	public static final String A_FAX_RATE_MGMT = "a=T38FaxRateManagement:";
	public static final String A_FAX_UDP_EC = "a=T38FaxUdpEC:";
	public static final String A_FMTP ="a=fmtp:";

	// Desired QOS is more difficult because the strength-tag field
	// appears before the status-type field. The SDPLocator will handle
	// obtaining the correct one based upon the name 
	public static final String A_DES_QOS = "a=des:qos ";
	public static final String A_DES_QOS_L = A_DES_QOS; // status-type = local
	public static final String A_DES_QOS_R = A_DES_QOS; // status-type = remote
	public static final String A_DES_QOS_E = A_DES_QOS; // status-type = e2e
	public static final String A_CONF_QOS_L = "a=conf:qos local ";
	public static final String A_CONF_QOS_R = "a=conf:qos remote ";
	public static final String A_CONF_QOS_E = "a=conf:qos e2e ";

	// Headers and Header keys for ICE elements of the SDP body
	// Headers
	public static final String ICE_PWD = "ice-pwd";
	public static final String ICE_UFRAG = "ice-ufrag";
	public static final String ICE_LITE = "ice-lite";
	public static final String ICE_MISMATCH = "ice-mismatch";
	public static final String ICE_OPTIONS = "ice-options";
	public static final String ICE_HOST_1 = "host-1";
	public static final String ICE_HOST_2 = "host-2";
	public static final String ICE_SRFLX_1 = "srflx-1";
	public static final String ICE_SRFLX_2 = "srflx-2";
	public static final String ICE_PRFLX_1 = "prflx-1";
	public static final String ICE_PRFLX_2 = "prflx-2";
	public static final String ICE_RELAY_1 = "relay-1";
	public static final String ICE_RELAY_2 = "relay-2";
	public static final String ICE_REMOTE_1 = "remote-1";
	public static final String ICE_REMOTE_2 = "remote-2";
		
	// Header keys
	public static final String A_ICE_LITE = "a=ice-lite";
	public static final String A_ICE_PWD = "a=ice-pwd:";
	public static final String A_ICE_UFRAG = "a=ice-ufrag:";
	public static final String A_ICE_MISMATCH = "a=ice-mismatch";
	public static final String A_ICE_OPTIONS = "a=ice-options";
	public static final String A_ICE_HOST = "typ host";
	public static final String A_ICE_SRFLX = "typ srflx";
	public static final String A_ICE_PRFLX = "typ prflx";
	public static final String A_ICE_RELAY = "typ relay";
	public static final String A_ICE_REMOTE_1 = "a=remote-candidate:1";
	public static final String A_ICE_REMOTE_2 = "a=remote-candidate:2";
	public static final String A_ICE_CANDIDATE = "a=candidate:";
	// Valid parameters
	public static final String NUMBER = "number";
	public static final String USER = "user";
	public static final String SESSION_ID = "session-id";
	public static final String SESSION_VERSION = "session-version";
	public static final String NET_TYPE = "net-type";
	public static final String ADDRESS_TYPE = "address-type";
	public static final String ADDRESS = "address";
	public static final String NAME = "name";
	public static final String START = "start";
	public static final String STOP = "stop";
	public static final String TYPE = "type";
	public static final String PARAMS = "params";
	// Note this is used in multiple headers
	public static final String PORT = "port";
	
	public static final String PROTOCOL = "protocol";
	
	// Note this is used in multiple headers
	public static final String PAYLOAD_TYPE = "payload-type";
	
	public static final String CLOCKRATE = "clockrate";
	public static final String DIRECTION = "direction";
	public static final String STRENGTH = "strength";
	public static final String VALUE = "value";
	public static final String CODEC_NAME = "codec-name";
	public static final String EXTENSION = "ext";
	
	// Valid ICE Parameters
	public static final String FOUNDATION = "foundation";
	public static final String COMPONENT_ID = "component-id";
	public static final String TRANSPORT = "transport";
	public static final String PRIORITY = "priority";
	public static final String CONNECTION_ADDRESS = "connection-address";
	public static final String REL_ADDR = "rel-addr";
	public static final String REL_PORT = "rel-port";
	/**
	 * This method verifies that the argument is an understood
	 * SDP header type.
	 * @param hdr - header type to test
	 * @return true if it is a known header type, false otherwise
	 */
	public static boolean isValidSDPHeader(String hdr) {
		if (hdr != null && 
				(VERSION.equals(hdr) ||
						ORIGIN.equals(hdr) ||
						SESSION.equals(hdr) ||
						CONNECTION_TYPE.equals(hdr) ||
						TIME.equals(hdr) ||
						RTPMAP.equals(hdr) ||
						MODE.equals(hdr) ||
						PTIME.equals(hdr) ||
						MEDIA_MODIFIER.equals(hdr) ||
						TIAS_MODIFIER.equals(hdr) ||
						MAX_PACKET_RATE.equals(hdr) ||
						QOS_CURRENT_REMOTE.equals(hdr) ||
						QOS_CURRENT_LOCAL.equals(hdr) ||
						QOS_CURRENT_E2E.equals(hdr) ||
						QOS_DESIRED_REMOTE.equals(hdr) ||
						QOS_DESIRED_LOCAL.equals(hdr) ||
						QOS_DESIRED_E2E.equals(hdr) ||
						QOS_CONF_REMOTE.equals(hdr) ||
						QOS_CONF_LOCAL.equals(hdr) ||
						QOS_CONF_E2E.equals(hdr) ||
						AUDIO.equals(hdr) ||
						VIDEO.equals(hdr) ||
						IMAGE.equals(hdr) ||
						FAX_VERSION.equals(hdr) ||
						FAX_MAX_DATAGRAM.equals(hdr) ||
						FAX_RATE_MGMT.equals(hdr) ||
						FAX_UDP_EC.equals(hdr) ||
						FMTP.equals(hdr) ||
						FMTP_FAX.equals(hdr) ||
						isValidICEHeader(hdr))) {
			return true;
		}
		return false;
	}
	
	/**
	 * This method verifies that the argument is an understood
	 * ICE header type.
	 * @param hdr - header type to test
	 * @return true if it is a known header type, false otherwise
	 */
	public static boolean isValidICEHeader(String hdr) {
		if (hdr != null && 
				(ICE_PWD.equals(hdr) ||
						ICE_UFRAG.equals(hdr) ||
						ICE_LITE.equals(hdr) ||
						ICE_MISMATCH.equals(hdr) ||
						ICE_OPTIONS.equals(hdr) ||
						ICE_HOST_1.equals(hdr) ||
						ICE_HOST_2.equals(hdr) ||
						ICE_SRFLX_1.equals(hdr) ||
						ICE_SRFLX_2.equals(hdr) ||
						ICE_PRFLX_1.equals(hdr) ||
						ICE_PRFLX_2.equals(hdr) ||
						ICE_RELAY_1.equals(hdr) ||
						ICE_RELAY_2.equals(hdr) ||
						ICE_REMOTE_1.equals(hdr) ||
						ICE_REMOTE_2.equals(hdr))) {
				
			return true;
		}
		return false;
	}
	
	/**
	 * This method determines if the Header key is one of the ICE candidate
	 * header keys.
	 * 
	 * @param hdr - header type to test
	 * @return true if it is a known header key type, false otherwise
	 */
	public static boolean isICECandidateHeader(String hdr) {
		if (hdr != null && 
				(ICE_HOST_1.equals(hdr) ||
						ICE_HOST_2.equals(hdr) ||
						ICE_SRFLX_1.equals(hdr) ||
						ICE_SRFLX_2.equals(hdr) ||
						ICE_PRFLX_1.equals(hdr) ||
						ICE_PRFLX_2.equals(hdr) ||
						ICE_RELAY_1.equals(hdr) ||
						ICE_RELAY_2.equals(hdr))) {
				
			return true;
		}
		return false;
	}
	/**
	 * This method determines if the Header key is one of the ICE candidate
	 * header keys.
	 * 
	 * @param key - header type to test
	 * @return true if it is a known header key type, false otherwise
	 */
	public static boolean isICECandidateHeaderKey(String key) {
		if (key != null && 
				(A_ICE_HOST.equals(key) ||
						A_ICE_SRFLX.equals(key) ||
						A_ICE_PRFLX.equals(key) ||
						A_ICE_RELAY.equals(key))) {
				
			return true;
		}
		return false;
	}
	
	public static boolean componentIdMatch(String hdr, String value) {
		if (hdr != null) {
				if (value.equals("1")) {
					if (ICE_HOST_1.equals(hdr) ||
							ICE_SRFLX_1.equals(hdr) ||
							ICE_PRFLX_1.equals(hdr) ||
							ICE_RELAY_1.equals(hdr)) {
						return true;
					}
				}
				else if (value.equals("2")) {
					if (ICE_HOST_2.equals(hdr) ||
							ICE_SRFLX_2.equals(hdr) ||
							ICE_PRFLX_2.equals(hdr) ||
							ICE_RELAY_2.equals(hdr))
						return true;
				}
		}
		return false;
	}
	
	/**
	 * This method verifies that the argument is an understood
	 * SDP parameter type.
	 * @param param - parameter type to test
	 * @return true if it is a known parameter type, false otherwise
	 */
	public static boolean isValidSDPParameter(String param) {
		if (NUMBER.equals(param) ||
				USER.equals(param) ||
				SESSION_ID.equals(param) ||
				SESSION_VERSION.equals(param) ||
				NET_TYPE.equals(param) ||
				ADDRESS_TYPE.equals(param) ||
				ADDRESS.equals(param) ||
				NAME.equals(param) ||
				START.equals(param) ||
				STOP.equals(param) ||
				TYPE.equals(param) ||
				PORT.equals(param) ||
				PROTOCOL.equals(param) ||
				PAYLOAD_TYPE.equals(param) ||
				CODEC_NAME.equals(param) ||
				CLOCKRATE.equals(param) ||
				DIRECTION.equals(param) ||
				STRENGTH.equals(param) ||
				VALUE.equals(param) ||
				PARAMS.equals(param) ||
				isValidICEParameter(param)) {
			return true;
		}
		return false;
	}
	
	/**
	 * This method verifies that the argument is an understood
	 * ICE parameter type. Some of the ICE parameters are the same
	 * value as those that appear in SDP, e.g. VALUE
	 * @param param - parameter type to test
	 * @return true if it is a known parameter type, false otherwise
	 */
	
	public static boolean isValidICEParameter(String param) {
		if (FOUNDATION.equals(param) ||
				COMPONENT_ID.equals(param) ||
				TRANSPORT.equals(param) ||
				PRIORITY.equals(param) ||
				CONNECTION_ADDRESS.equals(param) ||
				REL_ADDR.equals(param) ||
				REL_PORT.equals(param)) {
			return true;
		}
		return false;
	}
	
	/**
	 * This method retrieves the actual string to search for inside
	 * of the SDP header type when performing locate operation
	 * @param c - header type to test
	 * @return true if it is a known header type, false otherwise
	 */
	public static String getKey(String hdr) {
		if (VERSION.equals(hdr)) 
			return V;
		else if (ORIGIN.equals(hdr))
			return O;
		else if (SESSION.equals(hdr))
			return S;
		else if (CONNECTION_TYPE.equals(hdr))
			return C;
		else if (TIME.equals(hdr))
			return T;
		else if (AUDIO.equals(hdr))
			return M_AUDIO;
		else if (VIDEO.equals(hdr))
			return M_VIDEO;
		else if (IMAGE.equals(hdr))
			return M_IMAGE;
		else if (RTPMAP.equals(hdr))
			return A_RTPMAP;
		else if (MODE.equals(hdr))
			return A_MODE;
		else if (PTIME.equals(hdr))
			return A_PTIME;
		else if (MEDIA_MODIFIER.equals(hdr))
			return BW;
		else if (TIAS_MODIFIER.equals(hdr))
			return BW_TIAS;
		else if (MAX_PACKET_RATE.equals(hdr))
			return BW_MAXRATE;
		else if (FAX_VERSION.equals(hdr))
			return A_FAX_VERSION;
		else if (FAX_MAX_DATAGRAM.equals(hdr))
			return A_FAX_MAX_DATAGRAM;
		else if (FAX_RATE_MGMT.equals(hdr))
			return A_FAX_RATE_MGMT;
		else if (FAX_UDP_EC.equals(hdr))
			return A_FAX_UDP_EC;
		else if (FMTP.equals(hdr))
			return A_FMTP;
		else if (FMTP_FAX.equals(hdr))
			return A_FMTP;
		else if (QOS_CURRENT_REMOTE.equals(hdr))
			return A_CUR_QOS_R;
		else if (QOS_CURRENT_LOCAL.equals(hdr))
			return A_CUR_QOS_L;
		else if (QOS_CURRENT_E2E.equals(hdr))
			return A_CUR_QOS_E;
		else if (QOS_DESIRED_REMOTE.equals(hdr))
			return A_DES_QOS_R;
		else if (QOS_DESIRED_LOCAL.equals(hdr))
			return A_DES_QOS_L;
		else if (QOS_DESIRED_E2E.equals(hdr))
			return A_DES_QOS_E;
		else if (QOS_CONF_REMOTE.equals(hdr))
			return A_CONF_QOS_R;
		else if (QOS_CONF_LOCAL.equals(hdr))
			return A_CONF_QOS_L;
		else if (QOS_CONF_E2E.equals(hdr)) 
			return A_CONF_QOS_E;
		else if (ICE_PWD.equals(hdr)) 
			return A_ICE_PWD;
		else if (ICE_UFRAG.equals(hdr)) 
			return A_ICE_UFRAG;
		else if (ICE_MISMATCH.equals(hdr)) 
			return A_ICE_MISMATCH;
		else if (ICE_OPTIONS.equals(hdr)) 
			return A_ICE_OPTIONS;
		else if (ICE_LITE.equals(hdr)) 
			return A_ICE_LITE;
		else if (ICE_HOST_1.equals(hdr)) 
			return A_ICE_HOST;
		else if (ICE_HOST_2.equals(hdr)) 
			return A_ICE_HOST;
		else if (ICE_SRFLX_1.equals(hdr)) 
			return A_ICE_SRFLX;
		else if (ICE_SRFLX_2.equals(hdr)) 
			return A_ICE_SRFLX;
		else if (ICE_PRFLX_1.equals(hdr)) 
			return A_ICE_PRFLX;
		else if (ICE_PRFLX_2.equals(hdr)) 
			return A_ICE_PRFLX;
		else if (ICE_RELAY_1.equals(hdr)) 
			return A_ICE_RELAY;
		else if (ICE_RELAY_2.equals(hdr)) 
			return A_ICE_RELAY;
		else if (ICE_REMOTE_1.equals(hdr)) 
			return A_ICE_REMOTE_1;
		else if (ICE_REMOTE_2.equals(hdr)) 
			return A_ICE_REMOTE_2;
		
		return null;
	}
	
	public static boolean isAttributeMode(String hdr) {
		if (hdr.charAt(2) == ' ') {
			if (hdr.substring(0,11).equals("a= sendrecv") ||
					hdr.substring(0,11).equals("a= recvonly") ||
					hdr.substring(0,11).equals("a= sendonly") ||
					hdr.substring(0,11).equals("a= inactive")) {
				return true;
			}
		}
		else if (hdr.substring(0,10).equals("a=sendrecv") ||
					hdr.substring(0,10).equals("a=sendonly") ||
					hdr.substring(0,10).equals("a=recvonly") ||
					hdr.substring(0,10).equals("a=inactive")) {

				return true;
		}
		
		return false;
	}

	public static boolean isMediaModifier(String hdr) {
		if (hdr.startsWith(BW_AS) || hdr.startsWith(BW_CT))
				return true;
		return false;
	}
	
	public static boolean isFMTP(String hdr) {
		if (hdr.startsWith(A_FMTP))
				return true;
		return false;
	}
}
