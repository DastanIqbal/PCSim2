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

import java.util.ListIterator;

import com.cablelabs.tools.RefLocator;

/**
 * This class defines all of the Utility event types allowed within a
 * PC 2.0 XML document and provides a method to validate each entry as it
 * is processed by the parser.
 * 
 * @author ghassler
 *
 */
public class UtilityConstants {

	// List of Utility Messages understood by platform
	// Voicetronix messages
	public static final String OFFHOOK = "OFFHOOK";
	public static final String DIAL_DIGITS = "DIALDIGITS";
	public static final String ONHOOK = "ONHOOK";
	public static final String HOOKFLASH = "HOOKFLASH";
	public static final String OFFHOOK_COMPLETE = "OFFHOOKCOMPLETE";
	public static final String DIAL_DIGITS_COMPLETE = "DIALDIGITSCOMPLETE";
	public static final String ONHOOK_COMPLETE = "ONHOOKCOMPLETE";
	public static final String HOOKFLASH_COMPLETE = "HOOKFLASHCOMPLETE";
	public static final String OFFHOOK_ERROR = "OFFHOOKERROR";
	public static final String DIAL_DIGITS_ERROR = "DIALDIGITSERROR";
	public static final String ONHOOK_ERROR = "ONHOOKERROR";
	public static final String HOOKFLASH_ERROR = "HOOKFLASHEROR";
	public static final String VERIFY_DIAL_TONE = "VERIFYDIALTONE";
	public static final String VERIFY_DIAL_TONE_COMPLETE = "VERIFYDIALTONECOMPLETE";
	public static final String VERIFY_DIAL_TONE_ERROR = "VERIFYDIALTONEERROR";
	public static final String VERIFY_RING = "VERIFYRING";
	public static final String VERIFY_RING_COMPLETE = "VERIFYRINGCOMPLETE";
	public static final String VERIFY_RING_ERROR = "VERIFYRINGERROR";
	public static final String VERIFY_RING_BACK = "VERIFYRINGBACK";
	public static final String VERIFY_RING_BACK_COMPLETE = "VERIFYRINGBACKCOMPLETE";
	public static final String VERIFY_RING_BACK_ERROR = "VERIFYRINGBACKERROR";
	public static final String VERIFY_REORDER = "VERIFYREORDER";
	public static final String VERIFY_REORDER_COMPLETE ="VERIFYREORDERCOMPLETE";
	public static final String VERIFY_REORDER_ERROR = "VERIFYREORDERERROR";
	public static final String VERIFY_CALL_WAITING_TONE = "VERIFYCALLWAITINGTONE";
	public static final String VERIFY_CALL_WAITING_TONE_COMPLETE = "VERIFYCALLWAITINGTONECOMPLETE";
	public static final String VERIFY_CALL_WAITING_TONE_ERROR = "VERIFYCALLWAITINGTONEERROR";
	public static final String VERIFY_VOICE_PATH = "VERIFYVOICEPATH";
	public static final String VERIFY_VOICE_PATH_COMPLETE = "VERIFYVOICEPATHCOMPLETE";
	public static final String VERIFY_VOICE_PATH_ERROR = "VERIFYVOICEPATHERROR";
	public static final String VERIFY_VOICE_PATH_TWO_WAY = "VERIFYVOICEPATH2WAY";
	public static final String VERIFY_VOICE_PATH_TWO_WAY_COMPLETE = "VERIFYVOICEPATH2WAYCOMPLETE";
	public static final String VERIFY_VOICE_PATH_TWO_WAY_ERROR = "VERIFYVOICEPATH2WAYERROR";
	public static final String VERIFY_BUSY = "VERIFYBUSY";
	public static final String VERIFY_BUSY_COMPLETE = "VERIFYBUSYCOMPLETE";
	public static final String VERIFY_BUSY_ERROR = "VERIFYBUSYERROR";
	
	// TLS Messages
	public static final String TLS_HANDSHAKE_COMP = "TLSHandShakeComplete";
	public static final String TLS_CHANGE_CONFIG = "TLSChangeSSLConfig";
	public static final String TLS_CHANGE_TRUSTED_CERTS = "TLSChangeTrustedCerts";
	public static final String TLS_VERIFY_CHAIN = "TLSVerifyChain";
	// TLS uses UTIL_ACK/UTIL_NACK as responses
	
	// SNMP Messages
	public static final String SNMP_GET = "SNMPGet";
	public static final String SNMP_SET = "SNMPSet";
	public static final String SNMP_GET_TABLE = "SNMPGetTable";
	public static final String SNMP_RESP = "SNMPResp";
	public static final String SNMP_RESP_FAILURE = "SNMPRespFailure";
	
	// SNMP Attributes
	public static final String SNMP_ARRAY = "array";
	public static final String SNMP_OIDS = "oids";
	public static final String SNMP_DVA_REBOOT_OID = "PKTC-IETF-MTA-MIB::pktcMtaDevResetNow.0";
	public static final String SNMP_CM_REBOOT_OID = "DOCS-CABLE-DEVICE-MIB::docsDevResetNow.0";
	public static final String SNMP_REBOOT_PROMPT = "Power on (or reset) the UE.";
	static final public String SNMP_AGENT_IP = "agentip";
	static final public String SNMP_VERSION_ATTR = "version";

	
	// PACT Messages
	public static final String PROV_DEVICE_MOD = "PROVDeviceMod";
	public static final String PROV_DEVICE_MOD_ACK = "PROVDeviceModACK";
	public static final String PROV_DEVICE_MOD_FAILURE = "PROVDeviceModFailure";
	public static final String PROV_GET_DEV = "PROVDevGet";
	public static final String PROV_GET_DEV_ACK = "PROVDevGetACK";
	public static final String PROV_GET_DEV_FAILURE = "PROVDevGetFailure";
	public static final String PROV_DHCP_RESET = "PROVDHCPReset";
	public static final String PROV_DHCP_RESET_ACK = "PROVDHCPResetACK";
	public static final String PROV_DHCP_RESET_FAILURE = "PROVDHCPResetFailure";
	public static final String PROV_STOP_SERVICE = "PROVStopService";
	public static final String PROV_STOP_SERVICE_ACK = "PROVStopServiceACK";
	public static final String PROV_STOP_SERVICE_FAILURE = "PROVStopServiceFailure";
	public static final String PROV_RESUME_SERVICE = "PROVResumeService";
	public static final String PROV_RESUME_SERVICE_ACK = "PROVResumeServiceACK";
	public static final String PROV_RESUME_SERVICE_FAILURE = "PROVResumeServiceFailure";
	public static final String PROV_MANAGE_PORT = "PROVManagePort";
	public static final String PROV_MANAGE_PORT_ACK = "PROVManagePortACK";
	public static final String PROV_MANAGE_PORT_FAILURE = "PROVManagePortFailure";
	
	// PACT Attributes
	public static final String PROV_MAC_ATTR = "macAddr";
	public static final String PROV_POLICY_ATTR = "policyName";
	public static final String PROV_FILE_ATTR = "provFileName";
	public static final String PROV_DEPLOY_ATTR = "deploy";
    public static final String SRV_PROTOCOL_ATTR = "protocol";
    public static final String MANAGE_PORT_OP = "operation";
	
	// Generic Utility Responses
	public static final String UTIL_ACK = "UTIL_ACK";
	public static final String UTIL_NACK = "UTIL_NACK";
	public static final String UTIL_DATA = "UTIL_DATA";
	
	// List of supported Servers
	public static final String VOICETRONIX = "Voicetronix";

	// List of supported Service protocols
	public static final String SNMP = "SNMP";
	public static final String PACT = "PACT";
	
	public static final String ERROR_STRING = "errorString";
	/**
	 * Retrieves the service type based upon the given message-type
	 * @param type - the message-type to test
	 * @return - the service type if it matched, null otherwise
	 */
	public static String getService(String type) {
		if (isSNMPMsg(type)) {
			return SNMP;
		}
		else if (isPACTMsg(type)) {
			return PACT;
		}
		return null;
	}

	public static String getVoicetronixPrompt(Send s, int fsmUID, VoicetronixPort []  vp, boolean yesExpected) {
		String type = s.getMsgType();
		String no = "";
		String not = "";
		if (!yesExpected) {
			no = "no ";
			not = "not ";
		}
		

		String label1 = vp[0].getNELabel();
		if (label1.equals(SettingConstants.DUT ) ||
				label1.equals("UE0")) {
			label1 = "the " + SettingConstants.DUT;
		}
		String line1 = "line " + vp[0].getLine();
	
	
		if (type.equalsIgnoreCase(ONHOOK)) {
		     return "Place " + line1 + " of " + label1 + " on hook.";
		}
		else if (type.equalsIgnoreCase(OFFHOOK)) {
			return "Take " + line1 + " of " + label1 + " off hook.";
		}
		else if (type.equalsIgnoreCase(DIAL_DIGITS)) {
			String prompt = "Enter the following on " + line1 + "\nof " + label1;
			ListIterator<Mod> iter = s.getModifiers().listIterator();
			boolean found = false;
			while (iter.hasNext() && !found) {
				Mod m = iter.next();
				if ((m.getModType().equals("add") ||
						m.getModType().equals("replace")) &&
						m.getHeader().equals(SettingConstants.NUMBER) &&
						m.getRef() != null) {
					found = true;
					Reference ref = m.getRef();
					int uid = fsmUID;
					if (ref instanceof MsgRef)
						uid = ((MsgRef)ref).getUID();

					RefLocator rl = RefLocator.getInstance();
					String number = rl.getReferenceInfo(uid, ref, null);
					if (number != null)
						prompt += "\n" + number;
				}
			}
			return prompt;
		}
		else if (type.equalsIgnoreCase(HOOKFLASH)) {
			return "Execute a hook flash on " + line1 + " of " + label1;
		}
			
		else if (type.equalsIgnoreCase(VERIFY_DIAL_TONE)) {
			return "Verify that there is " + no + "dial tone on " + line1 + "\nof " + label1 + ".";
		}
		else if (type.equalsIgnoreCase(VERIFY_RING)) {
			return "Verify that " + line1 + " of " + label1 + "\nis " + not + "ringing.";
		}
		else if (type.equalsIgnoreCase(VERIFY_RING_BACK)) {
			return "Verify that there is " + no + "ring back on " + line1 + "\nof " + label1 + ".";
		}
		else if (type.equalsIgnoreCase(VERIFY_REORDER)) {
			return "Verify that the reorder tone is " + not + "\nbeing played on " + line1 + "\nof " + label1 + ".";
		}
		else if (type.equalsIgnoreCase(VERIFY_CALL_WAITING_TONE)) {
			return "Verify that the call waiting tone is " + not + "\nbeing played on " + line1 + "\nof " + label1 + ".";
		}
		else if (type.equalsIgnoreCase(VERIFY_VOICE_PATH)) {
			return "Verify that the voice path has " + not + "\nbeen establish on " + line1 + "\nof " + label1 + ".";
		}
		else if (type.equalsIgnoreCase(VERIFY_BUSY)) {
			return "Verify that the busy tone is " + not + "\nbeing played on " + line1 + "\nof " + label1 + ".";
		}
		else if (type.equalsIgnoreCase(VERIFY_VOICE_PATH_TWO_WAY)) {
			String label2 = null;
			String line2 = null;
			if (vp.length == 2) {
				label2 = vp[1].getNELabel();
				line2 = "line " + vp[1].getLine();
				if (label2.equals(SettingConstants.DUT)) {
					label1 = "the " + label1;
				}
			}
			return "Verify that a two-way voice path does " + not + "\nexist between " 
				+ line1 + "\nof " + label1 + " and " + line2 + "\nof " + label2 + ".";

		}
		return null;
	}
	
	/**
	 * This method test's whether the message-type is a known PACT message
	 * @param type - the Utility message's message-type field
	 * @return - true if the message is a known PACT message
	 */
	public static boolean isPACTMsg(String type) {
		return (isPACTRequest(type) || isPACTResponse(type));
	}
	
	/**
	 * Tests whether the message-type is a PACT Request message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a PACT Request message,
	 * 	false otherwise
	 */
	public static boolean isPACTRequest(String type) {
		if (PROV_DEVICE_MOD.equalsIgnoreCase(type) ||
				PROV_GET_DEV.equalsIgnoreCase(type)  ||
				PROV_DHCP_RESET.equalsIgnoreCase(type) ||
				PROV_STOP_SERVICE.equalsIgnoreCase(type) ||
				PROV_RESUME_SERVICE.equalsIgnoreCase(type) ||
				PROV_MANAGE_PORT.equalsIgnoreCase(type)) {
				return true;
		}
		return false;
	}
	/**
	 * Tests whether the message-type is a PACT Response message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a PACT Response message,
	 * 	false otherwise
	 */
	public static boolean isPACTResponse(String type) {
		if (PROV_DEVICE_MOD_ACK.equalsIgnoreCase(type) ||
				PROV_DEVICE_MOD_FAILURE.equalsIgnoreCase(type) ||
				PROV_GET_DEV_ACK.equalsIgnoreCase(type) ||
				PROV_GET_DEV_FAILURE.equalsIgnoreCase(type)  ||
				PROV_DHCP_RESET_ACK.equalsIgnoreCase(type) ||
				PROV_DHCP_RESET_FAILURE.equalsIgnoreCase(type) ||
				PROV_STOP_SERVICE_ACK.equalsIgnoreCase(type) ||
				PROV_STOP_SERVICE_FAILURE.equalsIgnoreCase(type) ||
				PROV_RESUME_SERVICE_ACK.equalsIgnoreCase(type) ||
				PROV_RESUME_SERVICE_FAILURE.equalsIgnoreCase(type) ||
				PROV_MANAGE_PORT_ACK.equalsIgnoreCase(type) ||
				PROV_MANAGE_PORT_FAILURE.equalsIgnoreCase(type)) {
			return true;
		}
		return false;
	}
	
	/**
	 * This method test's whether the message-type is a known SNMP message
	 * @param type - the Utility message's message-type field
	 * @return - true if the message is a known SNMP message
	 */
	public static boolean isSNMPMsg(String type) {
		return (isSNMPRequest(type) || isSNMPResponse(type));
	}
	
	/**
	 * Tests whether the message-type is a SNMP Request message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a SNMP Request message,
	 * 	false otherwise
	 */
	public static boolean isSNMPRequest(String type) {
		if (SNMP_GET.equalsIgnoreCase(type) ||
				SNMP_SET.equalsIgnoreCase(type)||
				SNMP_GET_TABLE.equalsIgnoreCase(type) ) {
				return true;
		}
		return false;
	}
	/**
	 * Tests whether the message-type is a Smart Bits Response message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a Smart Bits Response message,
	 * 	false otherwise
	 */
	public static boolean isSNMPResponse(String type) {
		if (SNMP_RESP.equalsIgnoreCase(type) ||
				SNMP_RESP_FAILURE.equalsIgnoreCase(type)) {
			return true;
		}
		return false;
	}
	/**
	 * This method test's whether the message-type is a known Smart Bits message
	 * @param type - the Utility message's message-type field
	 * @return - true if the message is a known Smart Bits message
	 */
	public static boolean isTLSMsg(String type) {
		return (isTLSRequest(type) || isTLSResponse(type));
	}
	
	/**
	 * Tests whether the message-type is a Smart Bits Request message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a Smart Bits Request message,
	 * 	false otherwise
	 */
	public static boolean isTLSRequest(String type) {
		if (TLS_HANDSHAKE_COMP.equalsIgnoreCase(type) ||
				TLS_CHANGE_CONFIG.equalsIgnoreCase(type)||
				TLS_CHANGE_TRUSTED_CERTS.equalsIgnoreCase(type)||
				TLS_VERIFY_CHAIN.equalsIgnoreCase(type)) {
			return true;
		}
		return false;
	}
	/**
	 * Tests whether the message-type is a Smart Bits Response message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a Smart Bits Response message,
	 * 	false otherwise
	 */
	public static boolean isTLSResponse(String type) {
		if (UTIL_ACK.equalsIgnoreCase(type) ||
				UTIL_NACK.equalsIgnoreCase(type)) {
			return true;
		}
		return false;
	}
	/**
	 * Tests whether the Utility message is understood by the platform.
	 * 
	 * @param type - the message-type of the message
	 * @return - true if it is an understood message-type, false otherwise
	 */
	public static boolean isUtilityMsg(String msg) {
		return (isVoicetronixMsg(msg) || 
					isSNMPMsg(msg) || 
					isPACTMsg(msg));
	}
	
	/**
	 * This method test's whether the message-type is a known Utility 
	 * request message.
	 * 
	 * @param type - the Utility message's message-type field
	 * @return - true if the message is a known Utility request message 
	 * 	regardless of protocol or services
	 */
	public static boolean isUtilityRequest(String type) {
		return (isVoicetronixRequest(type));
	}
	
	/**
	 * This method test's whether the message-type is a known Voicetronix message
	 * @param type - the Utility message's message-type field
	 * @return - true if the message is a known Voicetronix message
	 */
	public static boolean isVoicetronixMsg(String type) {
		return (isVoicetronixRequest(type) || isVoicetronixResponse(type));
	}
	
	/**
	 * Tests whether the message-type is a Voicetronix Request message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a Voicetronix Request message,
	 * 	false otherwise
	 */
	public static boolean isVoicetronixPrompt(String type) {
		if (OFFHOOK.equalsIgnoreCase(type) ||
				DIAL_DIGITS.equalsIgnoreCase(type) ||
				ONHOOK.equalsIgnoreCase(type) ||
				HOOKFLASH.equalsIgnoreCase(type)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Tests whether the message-type is a Voicetronix Request message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a Voicetronix Request message,
	 * 	false otherwise
	 */
	public static boolean isVoicetronixRequest(String type) {
		if (isVoicetronixPrompt(type) || isVoicetronixVerify(type)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Tests whether the message-type is a Voicetronix Response message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a Voicetronix Response message,
	 * 	false otherwise
	 */
	public static boolean isVoicetronixResponse(String type) {
		if (OFFHOOK_COMPLETE.equalsIgnoreCase(type) ||
				DIAL_DIGITS_COMPLETE.equalsIgnoreCase(type) ||
				ONHOOK_COMPLETE.equalsIgnoreCase(type) ||
				HOOKFLASH_COMPLETE.equalsIgnoreCase(type) ||
				OFFHOOK_ERROR.equalsIgnoreCase(type) ||
				DIAL_DIGITS_ERROR.equalsIgnoreCase(type) ||
				ONHOOK_ERROR.equalsIgnoreCase(type) ||
				HOOKFLASH_ERROR.equalsIgnoreCase(type) ||
				VERIFY_DIAL_TONE_COMPLETE.equalsIgnoreCase(type) ||
				VERIFY_RING_COMPLETE.equalsIgnoreCase(type) ||
				VERIFY_RING_BACK_COMPLETE.equalsIgnoreCase(type) ||
				VERIFY_REORDER_COMPLETE.equalsIgnoreCase(type) ||
				VERIFY_CALL_WAITING_TONE_COMPLETE.equalsIgnoreCase(type) ||
				VERIFY_VOICE_PATH_COMPLETE.equalsIgnoreCase(type) ||
				VERIFY_VOICE_PATH_TWO_WAY_COMPLETE.equalsIgnoreCase(type) ||
				VERIFY_DIAL_TONE_ERROR.equalsIgnoreCase(type) ||
				VERIFY_RING_ERROR.equalsIgnoreCase(type) ||
				VERIFY_RING_BACK_ERROR.equalsIgnoreCase(type) ||
				VERIFY_REORDER_ERROR.equalsIgnoreCase(type) ||
				VERIFY_CALL_WAITING_TONE_ERROR.equalsIgnoreCase(type) ||
				VERIFY_VOICE_PATH_ERROR.equalsIgnoreCase(type) ||
				VERIFY_VOICE_PATH_TWO_WAY_ERROR.equalsIgnoreCase(type) ||
				VERIFY_BUSY_ERROR.equalsIgnoreCase(type) ||
				VERIFY_BUSY_COMPLETE.equalsIgnoreCase(type)){
			return true;
		}
		return false;
	}
	
	/**
	 * Tests whether the Voicetronix Request message is a verification message
	 * 
	 * @param type - the message-type of the message
	 * @return - true if the message-type is a Voicetronix Request message,
	 * 	false otherwise
	 */
	public static boolean isVoicetronixVerify(String type) {
		if (VERIFY_DIAL_TONE.equalsIgnoreCase(type) ||
				VERIFY_RING.equalsIgnoreCase(type) ||
				VERIFY_RING_BACK.equalsIgnoreCase(type) ||
				VERIFY_REORDER.equalsIgnoreCase(type) ||
				VERIFY_CALL_WAITING_TONE.equalsIgnoreCase(type) ||
				VERIFY_VOICE_PATH.equalsIgnoreCase(type) ||
				VERIFY_VOICE_PATH_TWO_WAY.equalsIgnoreCase(type) ||
				VERIFY_BUSY.equalsIgnoreCase(type)) {
			return true;
		}
		return false;
	}
	

	/**
	 * Tests whether the plaform knows how to communicate with the specified
	 * server.
	 * 
	 * @param server - the server to test
	 * @return - true if the platform supports the communication to the server,
	 * 	false otherwise
	 */
	public static boolean supportedServer(String server) {
		if (VOICETRONIX.equals(server)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Tests whether the requested service is supported by the platform.
	 * 
	 * @param service - the service to test
	 * @return true if the platform supports the service, false otherwise
	 */
	public static boolean supportedService(String service) {
		if (service.equals("PACT")) {
			return true;
		}
		return false;
	}
	

	
	/**
	 * Tests whether the target is a supported service or server known by
	 * the platform.
	 * 
	 * @param target - the target to test
	 * @return - true if it is a recognized service or server, false otherwise
	 * 
	 */
	public static boolean validUtilityTarget(String target) {
		return (supportedServer(target));
	}
	

}
