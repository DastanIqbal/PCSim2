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

public class PDMLTags {

	public static final String PDML = "pdml";
	public static final String PACKET = "packet";
	public static final String PROTO = "proto";
	public static final String FIELD = "field";

	
	
	public static final String BOOTP_PROTOCOL = "bootp";
	public static final String DHCPv6_PROTOCOL = "dhcpv6";
	
	public static final String DNS_PROTOCOL = "dns";
	public static final String ETH_PROTOCOL = "eth";
	public static final String FAKE_PROTOCOL = "data";
	public static final String FRAME_PROTOCOL = "frame";
	public static final String GENINFO_PROTOCOL = "geninfo";
	public static final String ICMPV6_PROTOCOL = "icmpv6";
	public static final String IP_PROTOCOL = "ip";
	public static final String IPV6_PROTOCOL = "ipv6";
	public static final String KERBEROS_PROTOCOL = "kerberos";
	public static final String PACKET_CABLE_PROTOCOL = "pktc";
	public static final String RTCP_PROTOCOL = "rtcp";
	public static final String RTP_PROTOCOL = "rtp";
	public static final String SNMP_PROTOCOL = "snmp";
	public static final String SYSLOG_PROTOCOL = "syslog";
	public static final String TCP_PROTOCOL = "tcp";
	public static final String TFTP_PROTOCOL = "tftp";
	public static final String TOD_PROTOCOL = "time";
	public static final String UDP_PROTOCOL = "udp";
	
	public static final String NAME_ATTR = "name";
	public static final String NUM_ATTR = "num";
	public static final String SHOW_NAME_ATTR = "showname";
	public static final String SHOW_NAME_ATTR2 = "show_name";
	public static final String LEN_ATTR = "len";
	public static final String CAP_LEN_ATTR = "caplen";
	public static final String POS_ATTR = "pos";
	public static final String SHOW_ATTR = "show";
	public static final String SIZE_ATTR = "size";
	public static final String VALUE_ATTR = "value";
	public static final String TIMESTAMP_ATTR = "timestamp";
	public static final String HIDE_ATTR = "hide";
	public static final String UNMASKED_VALUE_ATTR = "unmaskedvalue";
	
	// IP protocol specific constants
	public static final String IP_SOURCE_ADDR = "Source: ";
	public static final String IP_DESTINATION_ADDR = "Destination: ";
	public static final String IP_SOURCE_PORT = "Source port: ";
	public static final String IP_DESTINATION_PORT = "Destination port: ";
	
	// Bootp protocol specific constants
	public static final String BOOTP_MSG_TYPE = "DHCP Message Type = DHCP ";
	public static final String BOOTP_MSG_TYPE_FIELD = "bootp.option.dhcp";
	public static final String BOOTP_OPTION_TYPE_FIELD = "bootp.option.type";
	public static final String BOOTP_MSG_TYPE_SHOWNAME = "DHCP: ";
	public static final String BOOTP_OPTION_VALUE = "Option: ";
	public static final String DHCP_DISCOVER = "Discover";
	public static final String DHCP_OFFER = "Offer";
	public static final String DHCP_REQUEST = "Request";
	public static final String DHCP_ACK = "ACK";
	public static final String DHCP_NAK = "NAK";
	
	// DHCPv6 protocol specific constants
    public static final String RELAY_MSG_TYPE = "Message type: ";
	public static final String DHCPv6_SOLICIT = "Solicit";
	public static final String DHCPv6_ADVERTISE = "Advertise";
	public static final String DHCPv6_REQUEST = DHCP_REQUEST;
	public static final String DHCPv6_REPLY = "Reply";
	public static final String DHCPv6_RENEW = "Renew";
	public static final String DHCPv6_REBIND = "Rebind";
	public static final String DHCPv6_RELAY = "Relay";
	public static final String DHCPv6_RELAY_REPLY = "Relay-Reply";
	public static final String DHCPv6_RELEASE = "Release";
	public static final String RELAY_MSG_VALUE = "12";
	public static final String RELAY_REPLY_MSG_VALUE = "13";
	
	// DNS protocol specific constants
//	public static final String NAPTR_REQUEST = "NAPTR Request";
//	public static final String NAPTR_RESPONSE = "NAPTR Response";
//	public static final String SRV_REQUEST = "SRV Request";
//	public static final String SRV_RESPONSE = "SRV Response";
//	public static final String A_REQUEST= "A Request";
//	public static final String A_RESPONSE = "A Response";
	public static final String DNS_MSG_TYPE_RESPONSE_FIELD = "dns.flags.response";
	public static final String DNS_MSG_TYPE_FIELD = "dns.qry.type";
	public static final String DNS_MSG_TYPE = "Domain Name System (";
	public static final String DNS_QUERIES = "Queries";
	public static final String DNS_ANSWER = "Answers";
	public static final String DNS_AUTHORITATIVE = "Authoritative";
	public static final String DNS_ADDITIONAL = "Additional";
	public static final String DNS_NEIGHBOR_SOLICITATION = "Neighbor Solicitation";
	public static final String DNS_ROUTER_SOLICITATION = "Router Solicitation";
	public static final String DNS_NEIGHBOR_ADVERTISEMENT = "Neighbor Advertisement";
	public static final String DNS_ROUTER_ADVERTISEMENT = "Router Advertisement";
	public static final String DNS_RESP = "resp";
	public static final String DNS_QRY = "qry";
	public static final String DNS_AUTH = "auth";
	public static final String DNS_ADDL = "addl";
	public static final String DNS_TYPE = " type ";
	public static final String DNS_MSGTYPE = "msgtype";
	public static final String DNS_QUERY = "Query";
	public static final String DNS_RESPONSE = "Response";
	public static final String DNS_ELEMENT_FIELD_NAME = "element";
	
	// ICMPv6 protocol specific constants
	public static final String ICMPV6_NEIGHBOR_SOLICITATION = "Neighbor Solicitation";
	public static final String ICMPV6_NEIGHBOR_ADVERTISEMENT = "Neighbor Advertisement";
	public static final String ICMPV6_ROUTER_SOLICITATION = "Router Solicitation";
	public static final String ICMPV6_ROUTER_ADVERTISEMENT = "Router Advertisement";
	public static final String ICMPV6_MSG_TYPE = "Type: ";
	
	// Kerberos protocol specific constants
	public static final String AS_REQ = "AS-REQ";
	public static final String KRB_SAFE = "KRB_SAFE";
	public static final String AS_REP = "AS-REP";
	public static final String TGS_REQ = "TGS REQ";
	public static final String TGS_REP = "TGS_REP";
	public static final String AP_REQ = "AP-REQ";
	public static final String AP_REP = "AP-REP";
	public static final String KERBEROS_MSG_TYPE_FIELD = "kerberos.msg.type";
	public static final String KERBEROS_MSG_TYPE = "MSG Type: ";
	
	// SNMP protocol specific constants
	public static final String INFORM_REQUEST = "Inform Request";
	public static final String TRAP = "Trap";
	public static final String INFORM_RESPONSE = "Inform Response";
	public static final String GET_REQUEST = "Get Request";
	public static final String GET_RESPONSE = "Get Response";
	public static final String SET_REQUEST = "Set Request";
	public static final String SET_RESPONSE = "Set Response";
	public static final String SNMP_MSG_TYPE_FIELD = "snmp.data";
	public static final String SNMP_MSG_TYPE_2 = "2"; // GET_RESPONSE
	public static final String SNMP_MSG_TYPE_3 = "3"; // SET_REQUEST
	public static final String SNMP_MSG_TYPE_6 = "6"; // INFORM_REQUEST
	public static final String SNMP_MSG_TYPE_7 = "7"; // TRAP
	
	// Syslog protocol specific constants
	//public static final String NOTICE = "NOTICE";
	//public static final String ALERT = "ALERT";
	public static final String SYSLOG_MSG_TYPE = "syslog";
	
	// TFTP protocol specific constants
	public static final String READ_REQUEST = "Read Request";
	public static final String DATA_PACKET = "Data Packet";
	public static final String ACKNOWLEDGEMENT = "Acknowledgement";
	public static final String TFTP_MSG_TYPE_FIELD = "tftp.opcode";
	public static final String TFTP_MSG_TYPE_1 = "1";  // READ_REQUEST
	public static final String TFTP_MSG_TYPE_3 = "3";  // DATA_PACKET
	public static final String TFTP_MSG_TYPE_4 = "4";  // ACKNOWLEDGEMENT
	
	
	// TOD protocol specific constants
	public static final String TIME_REQUEST = "Request";
	public static final String TIME_RESPONSE = "Response";
	public static final String TOD_MSG_TYPE = "Type: ";
	
	public static boolean isCaptureType(String type) {
		if (type != null && 
				(type.equals(BOOTP_PROTOCOL) ||
						type.equals(DHCPv6_PROTOCOL) ||
						type.equals(DNS_PROTOCOL) ||
						type.equals(ETH_PROTOCOL) ||
						type.equals(FRAME_PROTOCOL) ||
						type.equals(GENINFO_PROTOCOL) ||
						type.equals(ICMPV6_PROTOCOL) ||
						type.equals(IP_PROTOCOL) ||
						type.equals(IPV6_PROTOCOL) ||
						type.equals(KERBEROS_PROTOCOL) ||
						type.equals(PACKET_CABLE_PROTOCOL) ||
						type.equals(RTCP_PROTOCOL) ||
						type.equals(RTP_PROTOCOL) ||
						type.equals(SNMP_PROTOCOL) ||
						type.equals(SYSLOG_PROTOCOL) ||
						type.equals(TCP_PROTOCOL) ||
						type.equals(TFTP_PROTOCOL) ||
						type.equals(TOD_PROTOCOL) ||
						type.equals(UDP_PROTOCOL))) {
			return true;
		}
		return false;
	}
	
	public static boolean supportedProtocol(String type) {
		return isCaptureType(type);
	}
	
	public static boolean isBootpMsgType(String type) {
		if (type != null && 
				(type.equals(DHCP_DISCOVER) ||
						type.equals(DHCP_OFFER) ||
						type.equals(DHCP_REQUEST) ||
						type.equals(DHCP_ACK) ||
						type.equals(DHCP_NAK))) {
			return true;
		}
		return false;
	}
	
	public static boolean isDHCPv6MsgType(String type) {
		if (type != null && 
				(type.equals(DHCPv6_SOLICIT) ||
						type.equals(DHCPv6_ADVERTISE) ||
						type.equals(DHCPv6_REQUEST) ||
						type.equals(DHCPv6_REPLY) ||
						type.equals(DHCPv6_RELEASE) ||
						type.equals(DHCPv6_RENEW) ||
						type.equals(DHCPv6_RELAY)||
						type.equals(DHCPv6_REBIND) )) {
			return true;
		}
		return false;
	}
	
	public static boolean isDNSMsgType(String type) {
		if (type != null && 
				(type.equals(DNS_QUERY) ||
						type.equals(DNS_NEIGHBOR_SOLICITATION) ||
						type.equals(DNS_ROUTER_SOLICITATION) ||
						type.equals(DNS_NEIGHBOR_ADVERTISEMENT) ||
						type.equals(DNS_ROUTER_ADVERTISEMENT) ||
						type.equals(DNS_RESPONSE))) {
			return true;
		}
		return false;
	}
	
	public static boolean isICMPv6MsgType(String type) {
		if (type != null && 
				(type.equals(ICMPV6_NEIGHBOR_SOLICITATION) ||
						type.equals(ICMPV6_NEIGHBOR_ADVERTISEMENT) ||
								type.equals(ICMPV6_ROUTER_SOLICITATION) ||
						type.equals(ICMPV6_ROUTER_ADVERTISEMENT))) {
			return true;
		}
		return false;
	}
	
	public static boolean isKerberosMsgType(String type) {
		if (type != null && 
				(type.equals(AS_REQ) ||
						type.equals(KRB_SAFE) ||
						type.equals(AS_REP) ||
						type.equals(TGS_REQ) ||
						type.equals(TGS_REP) ||
						type.equals(AP_REQ) ||
						type.equals(AP_REP))) {
			return true;
		}
		return false;
	}
	
	public static boolean isSNMPMsgType(String type) {
		if (type != null && 
				(type.equals(INFORM_REQUEST) ||
						type.equals(INFORM_RESPONSE) ||
						type.equals(TRAP) ||
						type.equals(GET_REQUEST) ||
						type.equals(GET_RESPONSE) ||
						type.equals(SET_REQUEST) ||
						type.equals(SET_RESPONSE))) {
			return true;
		}
		return false;
		
	}
	
	public static boolean isSyslogMsgType(String type) {
		if (type != null && 
				//(type.equals(NOTICE) ||
					//	type.equals(ALERT))
						type.equals(SYSLOG_MSG_TYPE)) {
			return true;
		}
		return false;
	}
	
	public static boolean isTFTPMsgType(String type) {
		if (type != null && 
				(type.equals(READ_REQUEST) ||
						type.equals(DATA_PACKET) ||
						type.equals(ACKNOWLEDGEMENT))) {
			return true;
		}
		return false;
	}
	
	public static boolean isTODMsgType(String type) {
		if (type != null && 
				(type.equals(TIME_REQUEST) ||
						type.equals(TIME_RESPONSE))) {
			return true;
		}
		return false;
	}
	
	public static boolean validMsgType(String type) {
		if (type != null && 
				(isBootpMsgType(type) ||
						isDHCPv6MsgType(type) ||
						isDNSMsgType(type) ||
						isICMPv6MsgType(type) ||
						isKerberosMsgType(type) ||
						isSNMPMsgType(type) ||
						isSyslogMsgType(type) ||
						isTFTPMsgType(type) ||
						isTODMsgType(type) ||
						type.equals(IP_PROTOCOL) ||
						type.equals(GENINFO_PROTOCOL) ||
						type.equals(PACKET_CABLE_PROTOCOL) ||
						type.equals(RTCP_PROTOCOL) ||
						type.equals(RTP_PROTOCOL) ||
						type.equals(FRAME_PROTOCOL) ||
						type.equals(TCP_PROTOCOL) ||
						type.equals(ETH_PROTOCOL) ||
						type.equals(UDP_PROTOCOL))) {
			return true;
		}
		return false;
	}
	
	public static boolean isTunnelingMsgType(String type) {
		if (type.equals(DHCPv6_RELAY))
			return true;
		return false;
	}
}
