/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.sim;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

import com.cablelabs.common.Conversion;
import com.cablelabs.common.Transport;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.Mod;
import com.cablelabs.fsm.PlatformRef;
import com.cablelabs.fsm.Reference;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.fsm.UtilityConstants;
import com.cablelabs.fsm.UtilityMsg;
import com.cablelabs.fsm.VoicetronixPort;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.tools.RefLocator;
import com.cablelabs.utility.UtilityArrayAttribute;
import com.cablelabs.utility.UtilityAttribute;
import com.cablelabs.utility.UtilityEvent;
import com.cablelabs.utility.UtilityListener;
import com.cablelabs.utility.UtilityMessage;
import com.cablelabs.utility.UtilityStack;
import com.cablelabs.vpbapi.VpbAPI;

/**
 * This is the main interface to the Utility Stack. 
 * The Utility Stack is your external application 
 * interface to various support applications such
 * as Ethereal, Voicetronix, SmartBits, and SigTek.
 * 
 * It sends communication to the various utiliity
 * applications when the configuration is set or 
 * response with a successful acknowledgement when the
 * configuration is not defined.
 * 
 * @author ghassler
 *
 */
public class UtilityDistributor implements Distributor, UtilityListener {
	
	public static final String transPrefix = ""; 
	/**
	 * This is a counter for the creation of transaction
	 * IDs within a utility message. Each utility message
	 * has a unique identifer so that the response can
	 * properly be routed back to the correct listener.
	 */
	private int transID = 0;
	
	/**
	 * This is container for all pending Utility requests.
	 * The key is the unique transaction ID that the 
	 * class generates for every request made by the 
	 * application layer for transmission of the stack.
	 */
	private HashMap<String, FSMListener> listenerTable = null;
	
	/**
	 * Global settings defining whether the SNMP interface
	 * is active or inactive.
	 */
	private static boolean useSNMP = false;
	
	/**
	 * The Platform Configuration File's SNMP Version setting.
	 */
	private static String defSNMPVersion = null;
	
	/**
	 * Global settings defining whether the Voicetronix interface
	 * is active or inactive.
	 */
	private static boolean useVoicetronix = false;
	
	/**
	 * The API to the voicetronix library
	 */
	private VpbAPI vpbAPI = null;
	
	/**
	 * A table of all request waiting upon a response.
	 */
	private HashMap<String, UtilityMessage> reqTable = new HashMap<String, UtilityMessage>();

	private UtilityStack stack = null;
	
	private InetSocketAddress address = null;

	/**
	 * Local reference to the Reference Locator for modifying 
	 * a portion of a message from a msg_ref.
	 */
	private RefLocator refLocator = RefLocator.getInstance();
	
	
	private LogAPI logger = LogAPI.getInstance(); 
	
	/**
	 * The IP Address that was given to the stack to use.
	 */
	private String stacksIP = null;
	/**
	 * The Port that was given to the stack to use.
	 */
	private int stacksPort = 0;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Distributor";
	
	private void addVoicePort(UtilityMessage um, VoicetronixPort vp) {
		String msgType = um.getType();
		if (msgType.equalsIgnoreCase(UtilityConstants.ONHOOK) ||
				msgType.equalsIgnoreCase(UtilityConstants.OFFHOOK) ||
				msgType.equalsIgnoreCase(UtilityConstants.DIAL_DIGITS) ||
				msgType.equalsIgnoreCase(UtilityConstants.HOOKFLASH)) {
			
			// Make sure that the message does't already contain the
			// voiceport attribute
			ListIterator<UtilityAttribute> iter = um.getAttributes();
			UtilityAttribute vpa = null;
			UtilityAttribute ua = null;
			while (iter.hasNext() && 
					vpa == null) {
				 ua = iter.next();
				 if (ua.getName().equals(SettingConstants.VOICE_PORT))
					 vpa = ua;
			}
			if (vp != null && vpa == null) {
				ua = new UtilityAttribute(SettingConstants.VOICE_PORT, Integer.toString(vp.getPort()));
				um.addAttribute(ua);
			}
		}
	}
	
	public UtilityMsg autoRespond(Send s, FSMListener listener, Boolean expectedResponse) {
		UtilityMsg req = createAutoRequest(s, listener);
		if (UtilityConstants.OFFHOOK.equalsIgnoreCase(s.getMsgType())) {
			UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
					LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.OFFHOOK_COMPLETE, null);
			listener.processEvent(msg);
			return req;
		}
		else if (UtilityConstants.ONHOOK.equalsIgnoreCase(s.getMsgType())) {

			UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
					LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.ONHOOK_COMPLETE, null);
			listener.processEvent(msg);
			return req;
		}
		else if (UtilityConstants.DIAL_DIGITS.equalsIgnoreCase(s.getMsgType())) {

			UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
					LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.DIAL_DIGITS_COMPLETE, null);
			listener.processEvent(msg);
			return req;
		}
		else if (UtilityConstants.HOOKFLASH.equalsIgnoreCase(s.getMsgType())) {

			UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
					LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.HOOKFLASH_COMPLETE, null);
			listener.processEvent(msg);
			return req;
		}
		else if (UtilityConstants.VERIFY_DIAL_TONE.equalsIgnoreCase(s.getMsgType())) {
			if (expectedResponse != null && expectedResponse) {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_DIAL_TONE_COMPLETE, null);
				listener.processEvent(msg);
				return req;
			}
			else {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_DIAL_TONE_ERROR, null);
				listener.processEvent(msg);
				return req;
			}
		}
		else if (UtilityConstants.VERIFY_RING.equalsIgnoreCase(s.getMsgType())) {
			if (expectedResponse != null && expectedResponse) {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_RING_COMPLETE, null);
				listener.processEvent(msg);
				return req;
			}
			else {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_RING_ERROR, null);
				listener.processEvent(msg);
				return req;
			}
		}
		else if (UtilityConstants.VERIFY_RING_BACK.equalsIgnoreCase(s.getMsgType())) {
			if (expectedResponse != null && expectedResponse) {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_RING_BACK_COMPLETE, null);
				listener.processEvent(msg);
				return req;
			}
			else {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_RING_BACK_ERROR, null);
				listener.processEvent(msg);
				return req;
			}
		}
		else if (UtilityConstants.VERIFY_REORDER.equalsIgnoreCase(s.getMsgType())) {
			if (expectedResponse != null && expectedResponse) {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_REORDER_COMPLETE, null);
				listener.processEvent(msg);
				return req;
			}
			else {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_REORDER_ERROR, null);
				listener.processEvent(msg);
				return req;
			}
		}
		else if (UtilityConstants.VERIFY_CALL_WAITING_TONE.equalsIgnoreCase(s.getMsgType())) {
			if (expectedResponse != null && expectedResponse) {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_CALL_WAITING_TONE_COMPLETE, null);
				listener.processEvent(msg);
				return req;
			}
			else {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_CALL_WAITING_TONE_ERROR, null);
				listener.processEvent(msg);
				return req;
			}
		}
		else if (UtilityConstants.VERIFY_VOICE_PATH.equalsIgnoreCase(s.getMsgType())) {
			if (expectedResponse != null && expectedResponse) {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_VOICE_PATH_COMPLETE, null);
				listener.processEvent(msg);
				return req;
			}
			else {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_VOICE_PATH_ERROR, null);
				listener.processEvent(msg);
				return req;
			}
		}
		else if (UtilityConstants.VERIFY_VOICE_PATH_TWO_WAY.equalsIgnoreCase(s.getMsgType())) {
			if (expectedResponse != null && expectedResponse) {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_VOICE_PATH_TWO_WAY_COMPLETE, null);
				listener.processEvent(msg);
				return req;
			}
			else {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_VOICE_PATH_TWO_WAY_ERROR, null);
				listener.processEvent(msg);
				return req;
			}
		}
		else if (UtilityConstants.VERIFY_BUSY.equalsIgnoreCase(s.getMsgType())) {
			if (expectedResponse != null && expectedResponse) {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_BUSY_COMPLETE, null);
				listener.processEvent(msg);
				return req;
			}
			else {
				UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.VERIFY_BUSY_ERROR, null);
				listener.processEvent(msg);
				return req;
			}
		}
		else if (UtilityConstants.SNMP_SET.equalsIgnoreCase(s.getMsgType())) {
			UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(),
					LogAPI.getSequencer(), null, null, 0, null, 0, UtilityConstants.SNMP_RESP, null);
			listener.processEvent(msg);
			return req;
		}
		
		return null;
	}
	
	/** 
	 * This method is used to notify the Utility stack for clearing 
	 * any internal data between tests.
	 *
	 */
	public void clear() {
		vpbAPI.reset();
	}

	private UtilityMsg createAutoRequest(Send s, FSMListener listener) {
		transID++;
		String tID = transPrefix+((Integer)transID).toString();
		UtilityMessage um = createUtilityMessage(s.getMsgType().toUpperCase(), tID, 
				listener.getFsmUID(), null, s.getModifiers());
		if (um != null) {
			int seq = LogAPI.getSequencer();
			UtilityEvent ue = new UtilityEvent(um, seq,
					null, 
					0, null, 
					0);
			UtilityMsg utilMsg = new UtilityMsg(listener.getFsmUID(), 
					System.currentTimeMillis(), seq, null, ue, null);
			VoicetronixPort port = null;
			Mod m = s.getModifier(SettingConstants.VOICE_PORT);
			if (m != null) {
				port = SystemSettings.getVoicePort((PlatformRef)m.getRef());
			}
			else {
				port = SystemSettings.getDefaultVoicePort(); 
			}
			addVoicePort(um, port);
			return utilMsg;
		}
		return null;
	}
	
	private UtilityMessage createPACTUtilityMessage(String type, String tid, int fsmUID, 
			LinkedList<Mod> mods) {
		if (type.equals(UtilityConstants.PROV_DHCP_RESET)) {
			UtilityMessage um = new UtilityMessage(type, tid);
			um = modUtilityMsg(um, fsmUID, mods);
			return um;
		}
		else if (type.equals(UtilityConstants.PROV_STOP_SERVICE)) {
			UtilityMessage um = new UtilityMessage(type, tid);
			um = modUtilityMsg(um, fsmUID, mods);
			return um;
		}
		else if (type.equals(UtilityConstants.PROV_RESUME_SERVICE)) {
			UtilityMessage um = new UtilityMessage(type, tid);
			um = modUtilityMsg(um, fsmUID, mods);
			return um;
		}
		else if (type.equals(UtilityConstants.PROV_MANAGE_PORT)) {
			UtilityMessage um = new UtilityMessage(type, tid);
			um = modUtilityMsg(um, fsmUID, mods);
			return um;
		}
		else {
			UtilityMessage um = new UtilityMessage(type, tid);
			// We need to get the current DUT information to know what is expected.
			Properties dut = SystemSettings.getSettings(SettingConstants.DUT);
			if (dut != null) {
				String macAddr = dut.getProperty(SettingConstants.MAC_ADDRESS);
				if (macAddr != null) {
					UtilityAttribute ua = new UtilityAttribute(UtilityConstants.PROV_MAC_ATTR, macAddr.toUpperCase());
					um.addAttribute(ua);
					if (type.equalsIgnoreCase(UtilityConstants.PROV_DEVICE_MOD)) {
						ua = new UtilityAttribute(UtilityConstants.PROV_DEPLOY_ATTR, "true");
						um.addAttribute(ua);
						ua = new UtilityAttribute(UtilityConstants.PROV_FILE_ATTR, macAddr + ".bin");
						um.addAttribute(ua);
						// Note: The policy and provisioning file name attributes necessary for the message 
						// were added via a mod operation when the Send was originally constructed by the
						// listener.
					}
				}
				um = modUtilityMsg(um, fsmUID, mods);
				return um;
			}
		}
		return null;
	}
	
	private UtilityMessage createSNMPUtilityMessage(String type, String tid, int fsmUID, 
			LinkedList<Mod> mods) {
		// First see if this is a reboot and if the auto provisioning just rebooted the device
		ListIterator<Mod> iter = mods.listIterator();
		boolean reboot = false;
		while (iter.hasNext() && !reboot) {
			Mod m = iter.next();
			if (m.getHeader().equals(UtilityConstants.SNMP_ARRAY)) {
				Reference ref = m.getRef();
				String value = refLocator.getReferenceInfo(fsmUID, ref, null);
				if (value != null && 
						(value.contains(UtilityConstants.SNMP_DVA_REBOOT_OID) || 
								value.contains(UtilityConstants.SNMP_CM_REBOOT_OID))) {
					logger.info(PC2LogCategory.UTILITY, subCat, "The utility message creator detected the SNMP message as a reboot request.");
					reboot = true;
				}
			}
		}
		
		if (reboot && PCSim2.autoProvisionedDevice()) {
			logger.info(PC2LogCategory.UTILITY, subCat, "Not sending reboot because the auto provisioning has issued a reboot.");
			return null;
		}
		
		UtilityMessage um = new UtilityMessage(type, tid);
		UtilityAttribute ua = new UtilityAttribute(UtilityConstants.SNMP_VERSION_ATTR, defSNMPVersion);
		um.addAttribute(ua);
		//ua = new UtilityAttribute(SettingConstants.SNMP_COMMUNITY, defSNMPCommunity);
		Properties dut = SystemSettings.getSettings(SettingConstants.DUT);
		if (dut != null) {
			String dutIP = dut.getProperty(SettingConstants.IP);
			if (dutIP != null) {
				ua = new UtilityAttribute(UtilityConstants.SNMP_AGENT_IP, dutIP );
				um.addAttribute(ua);
			}
			
		}

		um = modUtilityMsg(um, fsmUID, mods);
		return um;
	}
	
	private UtilityMessage createUtilityMessage(String type, String tid, int fsmUID, 
			InetSocketAddress peerAddress, 
			LinkedList<Mod> mods) {
		UtilityMessage um = new UtilityMessage(type, tid);
		um = modUtilityMsg(um, fsmUID, mods);
		return um;
	}
	
	private int getChannel(Send s, int uid, String attr) {
		int channel = 0;
		LinkedList<Mod> mods = s.getModifiers();
		boolean found = false;
		if (mods != null) {
			ListIterator<Mod> iter = mods.listIterator();
			while (iter.hasNext() && !found) {
				Mod m = iter.next();
				if (m.getHeader().equals(attr)) {
					String port = refLocator.getReferenceInfo(uid, m.getRef(), null);
					try {
						channel = Integer.parseInt(port);
						found = true;
					}
					catch (NumberFormatException nfe) {
						logger.error(PC2LogCategory.UTILITY, subCat, 
								"Unable to resolve the voiceport value.");
					}
				}
			}
		}
		return channel;
	}
	
	private String getDigits(Send s, int uid) {
		String digits = null;
		LinkedList<Mod> mods = s.getModifiers();
		boolean found = false;
		if (mods != null) {
			ListIterator<Mod> iter = mods.listIterator();
			while (iter.hasNext() && !found) {
				Mod m = iter.next();
				if (m.getHeader().equals(SettingConstants.NUMBER)) {
					digits = refLocator.getReferenceInfo(uid, m.getRef(), null);
					
				}
			}
		}
		return digits;
	}
	
	public boolean getExpectedAnswer(Send s, int uid) {
		boolean yesExpected = true;
		if (s.hasModifiers()) {
			boolean done = false;
			ListIterator<Mod> iter = s.getModifiers().listIterator();
			while (iter.hasNext() && !done) {
				Mod m = iter.next();
				if (m.getHeader().equals(SettingConstants.VOICE_EXPECTED)) {
					done = true;
					String temp = refLocator.getReferenceInfo(uid, m.getRef(), null);
					if (temp.equals("no"))
						yesExpected = false;
				}

			}
		}
		
		return yesExpected;
	}
	/**
	 * This method provides a common view of the listening
	 * sockets opened in the stack for processing during
	 * this series of tests.
	 */
	@Override
	public String getStackAddresses() {
		String result = "";
		if (stack != null) {
			result = "Utility " + Transport.UDP.toString() + " " + stacksIP + "|" + stacksPort + "\n";
		}
		return result;
	}
	
	
	
	/**
	 * Initializes the class as well as creates and connections
	 * necessary between the various thrid-party application 
	 * utilities and stack. this method must be called prior to
	 * receiving a request to send a message or the class will
	 * throw an exception.
	 */
	public void init() {
		if (SystemSettings.getBooleanSetting("Utility Stack Enabled")) {
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			listenerTable = new HashMap<String, FSMListener>();
			stacksIP = platform.getProperty(SettingConstants.UTIL_IP);
			if (Conversion.isIPv6Address(stacksIP)) {
				 String zone = platform.getProperty("Utility IPv6 Zone");
				 stacksIP = Conversion.addZone(stacksIP, zone);
			 }
			stacksPort = Integer.parseInt(platform.getProperty(SettingConstants.UTIL_PORT));
			address = new InetSocketAddress(stacksIP,stacksPort);
			String port = platform.getProperty(SettingConstants.SNMP_PORT);
			int snmpPort = 0;
			if (port != null)
				snmpPort = Integer.parseInt(port);
			
			if (snmpPort > 0)
				useSNMP = true;
			defSNMPVersion = platform.getProperty(SettingConstants.SNMP_VERSION);

			if (!defSNMPVersion.equals(SettingConstants.SNMP_V1) &&
					!defSNMPVersion.equals(SettingConstants.SNMP_V2) &&
					!defSNMPVersion.equals(SettingConstants.SNMP_V3)) {
				logger.warn(PC2LogCategory.UTILITY, subCat, 
						"The " + SettingConstants.SNMP_VERSION 
						+ " contains an invalid value. UtilityStack is defaulting to 'v2c'.");
				defSNMPVersion = SettingConstants.SNMP_V2;
			}

			if (address != null) {
				stack = new UtilityStack(address, this);
				try {
					stack.start();
					logger.debug(PC2LogCategory.UTILITY, subCat,
							"UtilityStack started.");
					useVoicetronix = SystemSettings.getBooleanSetting("Voicetronix Enabled");
					if (useVoicetronix) {
						try {
							vpbAPI = new VpbAPI(this);

							if (vpbAPI != null) {
								vpbAPI.init();
								vpbAPI.start();
							}
						}
						catch (Exception ex) {
							logger.warn(PC2LogCategory.UTILITY, subCat,
								"Unable to start the VpbAPI library. Disabling voicetronix operations.");
							useVoicetronix = false;
						}
					}
//					usePACT = SystemSettings.getBooleanSetting("PACT Enabled");
//					useEthereal = SystemSettings.getBooleanSetting("Ethereal Enabled");
//					useSmartBits = SystemSettings.getBooleanSetting("SmartBits Enabled");
//					useSigTek = SystemSettings.getBooleanSetting("SigTek Enabled");;
				}
				catch (IOException io) {
					logger.warn(PC2LogCategory.UTILITY, subCat,
							"Unable to start UtilityStack.");
				}
			}
			else {
				logger.debug(PC2LogCategory.UTILITY, subCat,
						"UtilityStack is not being started.");
			}
			
		}
	}
	
	private UtilityMsg invokeVpbAPI(Send s, FSMListener listener) {
		transID++;
		String msgType = s.getMsgType();
		int channel = getChannel(s, listener.getFsmUID(), SettingConstants.VOICE_PORT);
		boolean yesExpected = getExpectedAnswer(s, listener.getFsmUID());
		// Before we can call the API to perform the operation, we need  to create the UtilityMsg
		// and store it in the requests table.
		String tID =((Integer)transID).toString();
		UtilityMessage um = createUtilityMessage(s.getMsgType().toUpperCase(), tID, listener.getFsmUID(), null, s.getModifiers());
		UtilityMsg utilMsg = null;
		if (um != null) {
			listenerTable.put(tID, listener);
			reqTable.put(tID, um);
			int seq = LogAPI.getSequencer();
			UtilityEvent ue = new UtilityEvent(um, seq,
					null, -1, null, -1);
			utilMsg = new UtilityMsg(listener.getFsmUID(), 
					System.currentTimeMillis(), seq, Transport.UDP, ue, null);
			VoicetronixPort port = null;
			Mod m = s.getModifier(SettingConstants.VOICE_PORT);
			if (m != null) {
				port = SystemSettings.getVoicePort((PlatformRef)m.getRef());
			}
			else {
				port = SystemSettings.getDefaultVoicePort(); 
			}
			addVoicePort(um, port);
			logger.info(PC2LogCategory.UTILITY, subCat, 
					"<<<<< TX:\tLength = " + um.getMsgLength()
					+ "\nSequencer " + seq
					+ "\n[" + s.getMsgType() + "]");
		}	
		if (msgType.equalsIgnoreCase(UtilityConstants.OFFHOOK)) {
			vpbAPI.offhook(channel, transID);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.ONHOOK)) {
			vpbAPI.onhook(channel, transID);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.DIAL_DIGITS)) {
			String digits = getDigits(s, listener.getFsmUID());
			vpbAPI.dial(channel, digits, transID);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.VERIFY_DIAL_TONE)) {
			vpbAPI.dialtone(channel, transID, yesExpected);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.HOOKFLASH)) {
			vpbAPI.hookflash(channel, transID);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.VERIFY_CALL_WAITING_TONE)) {
			vpbAPI.callwaiting(channel, transID, yesExpected);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.VERIFY_REORDER)) {
			vpbAPI.reorder(channel, transID, yesExpected);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.VERIFY_BUSY)) {
			vpbAPI.busy(channel, transID, yesExpected);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.VERIFY_RING)) {
			vpbAPI.ring(channel, transID, yesExpected);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.VERIFY_RING_BACK)) {
			vpbAPI.ringback(channel, transID, yesExpected);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.VERIFY_VOICE_PATH)) {
			vpbAPI.voicepath(channel, transID, yesExpected);
		}
		else if (msgType.equalsIgnoreCase(UtilityConstants.VERIFY_VOICE_PATH_TWO_WAY)) {
			int to = getChannel(s, listener.getFsmUID(), SettingConstants.TO_VOICE_PORT);
			int from = getChannel(s, listener.getFsmUID(), SettingConstants.FROM_VOICE_PORT);
			vpbAPI.voicepath2way(from, to, transID, yesExpected);
		}
		
		return utilMsg;
	}
	
	private UtilityMessage modUtilityMsg(UtilityMessage um, int fsmUID, LinkedList<Mod> mods) {
		if (um != null && mods != null && mods.size() > 0) {
			ListIterator<Mod> iter = mods.listIterator();
			while (iter.hasNext()) {
				Mod m = iter.next();
				if (m != null) {
					if (m.getModType().equals("add")) {
						Reference ref = m.getRef();
						String value = refLocator.getReferenceInfo(fsmUID, ref, null);
						if (value != null) {
							UtilityAttribute ua = null;
							if (m.getHeader().equals("array")) 
								ua = new UtilityArrayAttribute(m.getHeader(), value);
							else
								ua = new UtilityAttribute(m.getHeader(), value);
							if (ua != null) {
								logger.debug(PC2LogCategory.UTILITY, subCat,
										"Adding attribute " + m.getHeader() 
										+ " with value=[" + value + "] to " + um.getType() 
										+ " utility message.");
								um.addAttribute(ua);
							}
						}
					}
					else if (m.getModType().equals("delete")) {
						ListIterator<UtilityAttribute> attrs = um.getAttributes();
						while (attrs.hasNext()) {
							UtilityAttribute ua = attrs.next();
							if (ua != null && ua.equals(m.getHeader())) {
								if (um.removeAttribute(ua)) {
									logger.debug(PC2LogCategory.UTILITY, subCat,
											"Deleting attribute " + ua.getName() 
											+ " from utility message.");
								}
							}
						}
					}
					else if (m.getModType().equals("replace")) {
						if (m.getHeader().equals("transactionId")) {
							Reference ref = m.getRef();
							String value = refLocator.getReferenceInfo(fsmUID, ref, null);
							if (value != null) {
								logger.debug(PC2LogCategory.UTILITY, subCat,
										"Replacing transactionId from value" + um.getTransactionID() 
										+ " to value=[" + value + "]  "  
										+ " in " + um.getType() + " utility message.");
								um.setTransactionID(value);
							}
						}
						else {
							ListIterator<UtilityAttribute> attrs = um.getAttributes();
							boolean found = false;
							while (attrs.hasNext() && !found) {
								UtilityAttribute ua = attrs.next();
								if (ua != null && ua.getName().equals(m.getHeader())) {
									Reference ref = m.getRef();
									String value = refLocator.getReferenceInfo(fsmUID, ref, null);
									if (value != null) {
										logger.debug(PC2LogCategory.UTILITY, subCat,
												"Replacing attribute " + m.getHeader() 
												+ "'s value=[" + ua.getValue() + "] with " + value 
												+ " in " + um.getType() + " utility message.");
										ua.setValue(value);
										found = true;
									}
								}
							}

							if (!found) {
								// Basicly we are going to add the element if it didn't already
								// exist
								Reference ref = m.getRef();
								String value = refLocator.getReferenceInfo(fsmUID, ref, null);
								if (value != null) {
									UtilityAttribute ua = null;
									if (value.equals("array")) 
										ua = new UtilityArrayAttribute(m.getHeader(), value);
									else
										ua = new UtilityAttribute(m.getHeader(), value);
									if (ua != null) {
										logger.debug(PC2LogCategory.UTILITY, subCat,
												"Adding attribute " + m.getHeader() 
												+ " with value=[" + value + "] to " + um.getType() 
												+ " utility message.");
										um.addAttribute(ua);
									}
								}
							}
						}
					}
				}
			}
		}
		return um;
	}
	


	@Override
	public void processEvent(UtilityEvent event) {
		String msgType = event.getMessage().getType();
		String tID = event.getMessage().getTransactionID();
		logger.debug(PC2LogCategory.UTILITY, subCat,
				"UtilityDistributor rcvd a " + msgType + " message\n["
				+ event + "]");
		FSMListener listener = listenerTable.get(tID);
		if (listener != null) {
			// This should be a response to our request
			UtilityMsg msg = null;
			msg = new UtilityMsg(listener.getFsmUID(), System.currentTimeMillis(), 
						LogAPI.getSequencer(), Transport.UDP, event, null);
			logger.info(PC2LogCategory.UTILITY, subCat, 
					">>>>> RX:\tLength = " + event.getMessage().getMsgLength()
					+ "\nSequencer " + msg.getSequencer()
					+ "\n[" + event.getMessage().getType() + "]");
			if (msg != null)
				listener.processEvent(msg);
//			else {
//				logger.warn(PC2LogCategory.UTILITY, subCat,
//						"UtilityDistributor received a message without delivering to application layer.");
//			}

		}
		else {
			// This means it is a brand new request. We need to get the service and the listener
			// 'subscribed' for the service.
			String service = UtilityConstants.getService(msgType);
			if (service != null) {
				listener = Stacks.getUtilityListener(service);
				if (listener != null) {
					UtilityMsg msg = new UtilityMsg(listener.getFsmUID(), 
						System.currentTimeMillis(), 
						LogAPI.getSequencer(), Transport.UDP, event, null);
					reqTable.put(tID, event.getMessage());
					listener.processEvent(msg);
				}
				else {
					logger.warn(PC2LogCategory.UTILITY, subCat,
							"No listener is currently processing " + service + " type of messages. " 
							+ "Message with message-type="+ msgType + " and transactionID=" 
							+ tID + " was dropped!" );
				}
			}
			else {
				logger.warn(PC2LogCategory.UTILITY, subCat,
						"UtilityDistributor could not find a listener for the " 
						+ msgType + " message-type. Message with transactionID=" 
						+ tID + " was dropped!" );
				
			}
		}
	}

	/**
	 * Determines if the user should be requested to manually complete 
	 * a Voicetronix operation such as go on hook, off hook, dial digits,
	 * or hook flash.
	 *  
	 * @param msgType - The type of messages being sent.
	 * 
	 * @return - true if the user should be prompted.
	 */
	static public boolean promptForVoicetronix(String msgType) {
		if (!useVoicetronix && UtilityConstants.isVoicetronixPrompt(msgType)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Sends the message to the third-party application utility 
	 * when the interface to the application is active or responds
	 * successfully to the application layer when inactive.
	 * 
	 * @param s - the message to be sent
	 * @param listener - the FSM to which the response is to be delivered 
	 */
	public UtilityMsg send(Send s, FSMListener listener) {
		int fsmUID = listener.getFsmUID();
		String target = s.getTarget();
		String msgType = s.getMsgType();
		boolean voicetronix = false;
//		boolean sigtek = false;
//		boolean smartbits = false;
//		boolean http = false;
//		boolean tls = false;
		boolean snmp = false;
		boolean pact = false;
//		boolean other = false;
		if (target == null) {
			// Use default server based upon message type
			if (UtilityConstants.isVoicetronixMsg(msgType)) {
				target = UtilityConstants.VOICETRONIX;
				voicetronix = true;
			}
//			else if (UtilityConstants.isHTTPMsg(msgType)) {
//				target = UtilityConstants.HTTP;
//				http = true;
//			}
			else if (UtilityConstants.isSNMPMsg(msgType)) {
				target = UtilityConstants.SNMP;
				snmp = true;
			}
			else if (UtilityConstants.isPACTMsg(msgType)) {
				target = UtilityConstants.PACT;
				pact = true;
			}

//			else if (UtilityConstants.isTLSMsg(msgType)) {
//				target = UtilityConstants.TLS;
//				tls = true;
//			}
//			else if (UtilityConstants.isSigTekMsg(msgType)) {
//				target = UtilityConstants.SIGTEK;
//				sigtek = true;
//			}
//			else if (UtilityConstants.isSmartBitsMsg(msgType)) {
//				target = UtilityConstants.SMARTBITS;
//				smartbits = true;
//			}
		}
		
		if (voicetronix && vpbAPI != null) {
			if (vpbAPI != null) {
				UtilityMsg utilMsg = invokeVpbAPI(s, listener);
				return utilMsg;
			}
			else {
				logger.error(PC2LogCategory.UTILITY, subCat,
						"UtilityDistributor could not send the " + s.getMsgType() + " voicetronix message because the API is null.");
				return null;
			}
		}
		else {
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			String targetIP = null;
			String targetPort = null;
			if (target != null) {
				targetIP = platform.getProperty(target+" Server IP");
				targetPort = platform.getProperty(target+" Server Port");
			}

			if (targetIP != null && targetPort != null) {
				try {
					int port = Integer.parseInt(targetPort);
					InetSocketAddress peerAddress = new InetSocketAddress(targetIP, port);
					if (snmp || pact) {
						if (stack != null) {
							transID++;
							String tID = transPrefix+((Integer)transID).toString();
							UtilityMessage um = null;
							if (snmp && port > 0) {
							    um = createSNMPUtilityMessage(s.getMsgType(), tID, fsmUID, s.getModifiers());
							}
							else if (pact) {
								um = createPACTUtilityMessage(s.getMsgType(), tID, fsmUID, s.getModifiers());
							}
							
							if (um != null) {
								listenerTable.put(tID, listener);
								reqTable.put(tID, um);
								int seq = LogAPI.getSequencer();
								UtilityEvent ue = new UtilityEvent(um, seq,
										address.getAddress().getHostAddress(), 
										address.getPort(), peerAddress.getAddress().getHostAddress(), 
										peerAddress.getPort());
								UtilityMsg utilMsg = new UtilityMsg(fsmUID, 
										System.currentTimeMillis(), 
										seq, Transport.UDP, ue, null);
								stack.sendMessage(um, seq, peerAddress);
								return utilMsg;
							}
						}
						else {
							logger.error(PC2LogCategory.UTILITY, subCat,
									"UtilityDistributor could not send " + s.getMsgType().toUpperCase()
									+ " message-type because the Utility stack is not enabled. Message was dropped!" );
						}
					}
//					else if ( tls || part || other || xcap || http || bsf) {
//						if (stack != null) {
//							transID++;
//							String tID = transPrefix+((Integer)transID).toString();
//							UtilityMessage um = createUtilityMessage(s.getMsgType(), tID, fsmUID, peerAddress, s.getModifiers());
//							if (um != null) {
//								listenerTable.put(tID, listener);
//								reqTable.put(tID, um);
//								int seq = LogAPI.getSequencer();
//								UtilityEvent ue = new UtilityEvent(um, seq,
//										address.getAddress().getHostAddress(), 
//										address.getPort(), peerAddress.getAddress().getHostAddress(), 
//										peerAddress.getPort());
//								UtilityMsg utilMsg = new UtilityMsg(fsmUID, 
//										(long)System.currentTimeMillis(), 
//										seq, Transport.UDP, ue);
//								//msgRequestTable.put(tID, listener);
//								stack.sendMessage(um, seq, peerAddress);
//								return utilMsg;
//							}
//						}
//						else {
//							logger.error(PC2LogCategory.UTILITY, subCat,
//									"UtilityDistributor could not send " + s.getMsgType().toUpperCase()
//									+ " message-type because the Utility stack is not enabled. Message was dropped!" );
//						}
//					}

				}
				catch (NumberFormatException nfe) {
					logger.warn(PC2LogCategory.UTILITY, subCat,
							"UtilityDistributor couldn't convert the setting [" 
							+ target+" Server Port] to an integer." );
				}
			}
			else {
				logger.error(PC2LogCategory.UTILITY, subCat,
						"UtilityDistributor could not locate the "
						+ target + " Server IP or " + target 
						+ " Server Port setting to send the Utility message.");
			}
		}
		return null;

	}

	public void shutdown() {
		if (vpbAPI != null) 
			vpbAPI.stop();
		if (stack != null)
			stack.stop();
	}
	
	public static boolean snmpEnabled() {
		return useSNMP;
	}
	
	/**
	 * Determines if the user should be requested to manually complete 
	 * a Voicetronix operation such as go on hook, off hook, dial digits,
	 * or hook flash.
	 *  
	 * @param msgType - The type of messages being sent.
	 * 
	 * @return - true if the user should be prompted.
	 */
	static public boolean verifyForVoicetronix(String msgType) {
		if (!useVoicetronix && UtilityConstants.isVoicetronixVerify(msgType)) {
			return true;
		}
		return false;
	}
	
	static public boolean voicetronixEnabled() {
		return useVoicetronix;
	}
}
