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

public class SettingConstants {

	static final public String ADD_MOD_TYPE = "add";
	static final public String ADDR_FORMAT = "Address Format";
	static final public String AUTO_GENERATE = "Auto Generate";
	static final public String AUTO_GENERATE_PHONE_NUMBER_1 = "7205551301";
	static final public String AUTO_GENERATE_PHONE_NUMBER_2 = "7205551302";
	static final public String AUTO_PROVISION = "Auto Provision";
	static final public String AUTO_PROVISIONING_DATABASE = "Auto Provisioning Database";
	static final public String AUTO_PROV_FILE_DIRECTORY = "../config/prov_db/cfg_files/";
	
	static final public String AS = "AS";
	static final public String CABLE_MODEM_MAC_ADDRESS = "CM MAC Address";
	static final public String CABLE_MODEM_IP_ADDRESS = "CM IP Address";
	static final public String COMPOSITION = "composition";
	static final public String CONFIG_EDITOR = "Configuration Editor";
	static final public String CN = "CN";
	static final public String CW = "CW";
	static final public String CW_NUMBER = "Certification Wave Number";
	//static final public String DEVICE_LINE = "line number";
	//static final public String DEVICE_NUMBER = "device number";
	static final public String DELETE_MOD_TYPE = "delete";
	static final public String DEVICE_TYPE = "Device Type";
	static final public String DIALOG_ID = "dialog id";
	static final public String DIALOG_ID_PARAM = "dialog id parameter";
	static final public String DIAMETER_STACK_NAME = "DIAStack";
	
	static final public String DISPLAY_NAME = "display name";
	static final public String DOMAIN = "domain";
	static final public String DUT = "DUT";
	static final public String DUT_VERSION = "Version"; 
	static final public String DUT_SUBGROUP = "Subgroup"; 
	static final public String DUT_VENDOR = "Vendor Name";
	static final public String FROM_DISPLAY_NAME = "From Include Display Name";
	static final public String FSM_PROCESS_DUPLICATE_MSGS = "FSM Process Duplicate Messages";
	static final public String FSM_END_IMMEDIATELY = "FSM End Immediately";
	static final public String FQDN = "FQDN";
	static final public String GLOBAL_REGISTRAR = "Global Registrar";
	static final public String GLOBAL_REGISTRAR_FSM = "Global Registrar FSM";
	static final public String GLOBAL_REGISTRAR_TIMEOUT = "Global Registrar No Response Timeout";
	static final public String GR = "gr";
	static final public String GRUU_FORMAT = "GRUU Format";
	static final public String IP = "IP";
	static final public String IP_COMP_FORM = "::";
	static final public String IPv6_ZONE = "IPv6 Zone";
	static final public String IP2 = "IP2";
	
	static final public String LAB_FQDN = "LabFQDN";
	static final public String LOG_DIRECTORY = "Log Directory";
	static final public String MAC_ADDRESS = "MAC Address";
	static final public String NE = "NE";
	static final public String NO_RESP_TIMEOUT = "No Response Timeout";
	static final public String NUM_DIAMETER_STACKS = "Diameter Number of Stacks";
	static final public String OPAQUE_UUID = "opaque uuid";

	static final public String PACT_SERVER_IP = "PACT Server IP";
	static final public String PACT_SERVER_PORT = "PACT Server Port";
	static final public String PASSWORD = "password";
	static final public String PCSCF = "PCSCF";
	static final public String PLATFORM = "Platform";
	static final public String PHONE_LINES = "phone lines";
	static final public String PHONE_NUMBER = "phone number ";
	static final public String PHONE_NUMBER_1 = "phone number 1";
	static final public String PHONE_NUMBER_2 = "phone number 2";
	static final public String PORT = "port";
	static final public String PUBLIC_GRUU = "pub-gruu";
	
	static final public String PUI = "pui";
	static final public String PUI2 = "pui2";
	static final public String PRESENCE_SERVER = "Presence Server";
	static final public String PRESENCE_SERVER_FSM = "Presence Server FSM";
	static final public String PRESENCE_SERVER_TIMEOUT = "Presence Server No Response Timeout";
	
	static final public String PRODUCT_UNIT = "Product Unit";
	static final public String PRUI = "prui";
	static final public String RECORD_PROVISIONING_FILE = "Record Provisioning File";
	static final public String REPLACE_MOD_TYPE = "replace";
	
	static final public String RTP = "RTP";
	static final public String RTCP_PORT1 = "RTCP Port1";
	static final public String RTP_PORT1 = "RTP Port1";
	static final public String RTCP_PORT2 = "RTCP Port2";
	static final public String RTP_PORT2 = "RTP Port2";
	static final public String SCRIPT_EDITOR = "Script Editor";
	static final public String SCSCF = "SCSCF";
	static final public String SCTP = "SCTP";
	static final public String SDP_PORT = "SDPPort";
	static final public String SDP_PORT2 = "SDPPort2";
	static final public String SIMULATED = "simulated";
	static final public String SIP_DEF_STACK_NAME = "SIP Default Stack Name";
	static final public String SIP_DEF_TRANPORT_PROTOCOL = "SIP Default Transport Protocol";
	static final public String SIP_INSTANCE_UUID = "sip instance uuid";
	static final public String SIP_INSPECTOR = "SIP Inspector";
	static final public String SIP_INSPECTOR_TYPE = "SIP Inspector Type";
	
	//static final public String SNMP_COMMUNITY = "SNMP Community";
	static final public String SNMP_IP = "SNMP Server IP";
	static final public String SNMP_PORT = "SNMP Server Port";
	//static final public String SNMP_PUBLIC_COMMUNITY = "public";
	//static final public String SNMP_PRIVACY_KEY = "SNMP Privacy Key";
	//static final public String SNMP_PRIVACY_PROTOCOL = "SNMP Privacy Protocol";
	//static final public String SNMP_PRIVACY_DES = "DES";
	//static final public String SNMP_AUTHENTICATION_KEY = "SNMP Authentication Key";
	//static final public String SNMP_AUTHENTICATION_MD5 = "MD5";
	//static final public String SNMP_AUTHENTICATION_SHA = "SHA";
	//static final public String SNMP_AUTHENTICATION_PROTOCOL = "SNMP Authentication Protocol";
	//static final public String SNMP_SECURITY_LEVEL = "SNMP Security Level";
	//static final public String SNMP_SECURITY_NONE = "noAuthNoPriv";
	//static final public String SNMP_SECURITY_AUTH = "authNoPriv";
	//static final public String SNMP_SECURITY_PRIV = "authPriv";
	//static final public String SNMP_PRIVATE_COMMUNITY = "private";
	static final public String SNMP_REBOOT_ARRAY = 
		"varBind 1 1 3 \r\n ][ , PKTC-IETF-MTA-MIB::pktcMtaDevResetNow.0,INTEGER,1][\r\n";
	static final public String SNMP_CM_REBOOT_ARRAY = 
		"varBind 1 1 3 \r\n ][ , DOCS-CABLE-DEVICE-MIB::docsDevResetNow.0,INTEGER,1][\r\n";
	static final public String SNMP_VERSION = "SNMP Version";
	
	static final public String SNMP_V1 = "1";
	static final public String SNMP_V2 = "2c";
	static final public String SNMP_V3 = "3";

	static final public String STUN_COMPRESS_FORM = "STUN Message Compressed Form";
	static final public String SUBSCRIBE_TYPE_DIALOG = "dialog";
	static final public String SUBSCRIBE_TYPE_REG = "reg";
	static final public String SUBSCRIBE_TYPE_MSG_SUMMARY = "message-summary";
	static final public String SUBSCRIBE_TYPE_UA_PROFILE = "ua-profile";
	static final public String SUBSCRIBE_TYPE_PRESENCE = "presence";
	static final public String NUM_SIP_STACKS = "SIP Number of Stacks";
	static final public String STUN = "STUN";
	static final public String STUN_USERNAME_CREDENTIAL = "STUN Username Credential";
	static final public String STUN_PASSWORD_CREDENTIAL = "STUN Password Credential";
	static final public String STUN_PRIORITY = "STUN Priority";
	static final public String STUN_SERVER_PRIORITY = "STUN Server Priority";
	
	static final public String TEMPORARY_GRUU = "temp-gruu";
	static final public String TESTER_NAME = "Tester's Name";
	static final public String TCP = "TCP";
	static final public String TCP_PORT = "TCPPort";
	static final public String TFTP_SERVER_IP = "TFTP Server IP";
	static final public String TFTP_SERVER_PORT = "TFTP Server Port";
	static final public String TLS = "TLS";
	static final public String TLS_PORT = "TLSPort";
	static final public String TRANSPORT_PROTOCOL = "transportProtocol";
	static final public String TURN = "TURN";
	static final public String TURN_UDP_PORT ="TURN UDP Port";
	static final public String TURN_TCP_PORT = "TURN TCP Port";
	static final public String TURN_SERVER_PRIORITY = "TURN Server Priority";
	static final public String UDP = "UDP";
	static final public String UDP_PORT = "UDPPort";
	static final public String UE = "UE";
	static final public String USER_NAME = "username";
	static final public String UTIL_IP = "Utility IP";
	static final public String UTIL_PORT = "Utility Port";
	static final public String UTILITY = "utility";
	static final public String UUID = "uuid";
	//static final public String VOICE_LINE = "voiceLine";
	//static final public String VOICE_TO_LINE = "voiceToLine";
	//static final public String VOICE_FROM_LINE = "voiceFromLine";
	//static final public String VOICE_DEVICE = "voiceDevice";
	//static final public String VOICE_TO_DEVICE = "voiceToDevice";
	//static final public String VOICE_FROM_DEVICE = "voiceFromDevice";
	static final public String NUMBER = "number";
	static final public String VOICE_PORT = "voiceport";
	static final public String FROM_VOICE_PORT = "from voiceport";
	static final public String TO_VOICE_PORT = "to voiceport";
	static final public String VOICE_EXPECTED = "voiceExpected";
	static final public String WIRESHARK_DIRECTORY = "Wireshark Directory";
	static final public String WIRESHARK_INTERFACE = "Wireshark Interface";
	
	// A table of dynamically configurable platform settings
	static final public String [] dynamicSettingKeys = { FSM_PROCESS_DUPLICATE_MSGS, 
			FSM_END_IMMEDIATELY,
			SIP_DEF_TRANPORT_PROTOCOL,
			NO_RESP_TIMEOUT,
			SIP_INSPECTOR,
			SIP_INSPECTOR_TYPE };
	
	
	public static String [] getDynamicSettingsKeys() {
		return dynamicSettingKeys;
	}
	public static boolean documentConfigurableProperty(String name) {
		if (name != null &&
				name.equals(FSM_PROCESS_DUPLICATE_MSGS) ||
				name.equals(FSM_END_IMMEDIATELY) ||
				name.equals(SIP_DEF_TRANPORT_PROTOCOL) ||
				name.equals(NO_RESP_TIMEOUT) ||
				name.equals(SIP_INSPECTOR) ||
				name.equals(SIP_INSPECTOR_TYPE))
			return true;
		return false;
	}
}
