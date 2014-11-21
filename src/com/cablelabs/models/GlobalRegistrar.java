/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.models;

import java.util.HashSet;
import java.util.Properties;

import javax.sip.header.ContactHeader;
import javax.sip.message.Request;

import com.cablelabs.common.Transport;
import com.cablelabs.fsm.EventConstants;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.Generate;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.Literal;
import com.cablelabs.fsm.Mod;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.MsgRef;
import com.cablelabs.fsm.ReferencePointsFactory;
import com.cablelabs.fsm.Result;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.fsm.SIPMsg;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.fsm.UtilityConstants;
import com.cablelabs.gui.PC2RegistrarStatus;
import com.cablelabs.gui.PC2UI;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sim.GinRegistration;
import com.cablelabs.sim.PC2Protocol;
import com.cablelabs.sim.PCSim2;
import com.cablelabs.sim.SIPDistributor;
import com.cablelabs.sim.Stacks;

public class GlobalRegistrar extends PC2Models {

	/**
	 * This is a table of elements that have been Registered. It is
	 * based upon their pui.
	 * 
	 */
	private static HashSet<String> regTable = new HashSet<String>();
	private String pui = null;
	private String puiSettingName = null;
	private String origLabel = null;
	private String curLabel = null;
	private String destIP = null;
	private static FSM masterFSM = null;
	private static LogAPI logger = LogAPI.getInstance(); 
	private PC2RegistrarStatus status = PC2RegistrarStatus.INACTIVE;

	/**
	 * This is the default that all copies should be originally constructed
	 */
	private static Transport defaultTransport = Transport.UDP;
	
	private static final String model = "GlobalRegistrar";

	private RebootTable rebootTable = RebootTable.getInstance();
	
	/**
	 *  Public Constructor for the GlobalRegistrar model.
	 * 
	 * @param fsm - the FSM to use for the test.
	 * 
	 */
	public GlobalRegistrar(FSM f, String pui, String destIP) {
		super(f, "GR:" + pui + ":" + destIP, model);
//	ME	System.out.println("Master=" + masterFSM.me());
//	ME	System.out.println("FSM=" + super.fsm.me());
		this.pui = pui;
		this.origLabel = pui;
		this.curLabel = pui;
		// Determine which pui we are, pui or pui2
		Properties ue = SystemSettings.getSettings(pui);
		if (ue != null) {
			String uePUI = ue.getProperty(SettingConstants.PUI);
			String uePUI2 = ue.getProperty(SettingConstants.PUI2);
			if (uePUI != null && uePUI.equals(pui))
				puiSettingName = SettingConstants.PUI;
			else if (uePUI2 != null && uePUI2.equals(pui))
				puiSettingName = SettingConstants.PUI2;
			else {
				logger.error(PC2LogCategory.Model, subCat, 
						"GlobalRegistrar could not identify the Public User Identify setting name to use.");
			}
			if (PCSim2.isGUIActive()) {
				PC2UI ui = PCSim2.getUI();
				ui.addRegistrarElement(pui, curLabel);
			}
		}
		
		fsm.setAPI(this);
		fsm.setComparisonEvaluator(examiner);
		
		// Have the FSM update the No Response Timeout if it
		// has been changed in the test document.
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		String noResponseTimeout = platform.getProperty(SettingConstants.GLOBAL_REGISTRAR_TIMEOUT);
		if (noResponseTimeout != null) {
			try {
				int newValue = Integer.parseInt(noResponseTimeout);
				fsm.setNoResponseTimeout(newValue);
			}
			catch (NumberFormatException nfe) {
				logger.error(PC2LogCategory.Model, subCat, 
				"GlobalRegistrar could not update the NoResponseTimeout value because the " 
						+ SettingConstants.GLOBAL_REGISTRAR_TIMEOUT + " setting [" 
						+ noResponseTimeout + "] is not a number.");
			}
		}
		
		if (fsm.getNetworkElements().getPCSCFSize() == 1) {
			String p = fsm.getNetworkElements().getPCSCFs().next();
			if (p != null) {
				Properties pcscf = SystemSettings.getSettings(p);
				if (pcscf != null) {
					String pcscfIP = pcscf.getProperty(SettingConstants.IP);
					if (pcscfIP.equals(destIP)) {
						this.destIP = destIP;
					}
					else {
						pcscfIP = pcscf.getProperty(SettingConstants.IP2);
						if (pcscfIP != null && pcscfIP.equals(destIP)) 
							this.destIP = pcscfIP;
					}
					String zone = pcscf.getProperty(SettingConstants.IPv6_ZONE);
					Stacks.addRegistrarListener(PC2Protocol.SIP, destIP, zone, pui, this);
//					String destIP2 = pcscf.getProperty(SettingConstants.IP2);
//					if (!destIP.equals(destIP2)) {
//						Stacks.addRegistrarListener(PC2Protocol.SIP, destIP2, zone, pui, this);
//					}
					
				}
			}
		}
		else {
			logger.error(PC2LogCategory.Model, subCat, 
					"GlobalRegistrar expected to find only one PCSCF entry in its NE elements,"
					+ " instead it found " + fsm.getNetworkElements().getPCSCFSize() + ".");
		}
	}

	private void changeRegistrarStatus() {
		if (PCSim2.isGUIActive()) {
			PC2UI ui = PCSim2.getUI();
			ui.changeRegistrarStatus(status, pui, curLabel);
		}
	}
	
	public void changeRegistrarLabel(String newLabel) {
		if (PCSim2.isGUIActive()) {
			// String newLabel = SystemSettings.getLabelByValue(pui, puiSettingName);
			PC2UI ui = PCSim2.getUI();
			if (newLabel != null && !curLabel.equals(newLabel)){
				ui.changeRegistrarLabel(pui, curLabel, newLabel);
				curLabel = newLabel;
			}
			
		}
	}
	/**
	 * Looks in the registration table to see if the IP address 
	 * exist or not
	 * 
	 * @param ip - source address of the network element
	 * @return - true if the current State's name is "Registered"
	 * 			false otherwise
	 */
	@Override
	public boolean isRegistered() {
		boolean exists = regTable.contains(pui);
		if (exists)
			logger.info(PC2LogCategory.Model, subCat,
					pui + " is registered to GlobalRegistrar " + name + ".");
		else
			logger.info(PC2LogCategory.Model, subCat,
					pui + " is not registered to GlobalRegistrar " + name + ".");
			
			
		return exists;
	}
	
	/**
	 * Allows the registrar to subscribe for registration messages from the 
	 * Stacks class on behalf of the DUT's ip.
	 */
	@Override
	public void init() {
		super.init();
		//Notify stacks that all registration processing for
		// SIP will be delivered to this FSM
		if (destIP == null && fsm.getNetworkElements().getPCSCFSize() == 1) {
			String p = fsm.getNetworkElements().getPCSCFs().next();
			if (p != null) {
				Properties pcscf = SystemSettings.getSettings(p);
				if (pcscf != null) {
					destIP = pcscf.getProperty(SettingConstants.IP);
					String zone = pcscf.getProperty(SettingConstants.IPv6_ZONE);
					Stacks.addRegistrarListener(PC2Protocol.SIP, destIP, zone, pui, this);
				}
			}
		}
		
		
		try {
			fsm.init(queue, this);
			this.start();
			reboot();
		}
		catch (IllegalStateException ise) {
			PCSim2.setTestPassed(false);
			String err = "GlobalRegistrar model failed during state machine initialization." + 
			" Test terminated. Declaring test case failure.";
			logger.fatal(PC2LogCategory.Model, subCat, err);
			shuttingDown = true;
			PCSim2.setTestComplete();
		}

	}

	public static FSM getFSMCopy() {
		if (masterFSM != null) {
			try {
// ME			System.out.println("Master precopy - " + masterFSM.me()+ " \n End Pre Master\n");
				FSM newFSM = (FSM)masterFSM.clone();
               
// ME				System.out.println("Master postcopy - " + masterFSM.me()+ " \n End Post Master\n");
// ME				System.out.println("Copy - " + newFSM.me() + " \n End COPY\n");
				return newFSM;
			}
			catch (CloneNotSupportedException cns) {
				String errMsg = "GlobalRegistrar could not replicate the FSM for a new device to register.";
				logger.error(PC2LogCategory.Model, "", errMsg, cns);
			}
		}
		return null;
	}
	
	private void checkForGinRegistration(Send s) {
		if (s != null && s.getMsgType().toUpperCase().equals("200-REGISTER")) {
			SIPDistributor dist = Stacks.getSipDistributor();
			boolean active = ((GinRegistration)dist).getGinRegistrationInfo(s, destIP);
			if (active) {
				logger.info(PC2LogCategory.Model, "", 
						"The GIN Registration processing is active for network element " 
						+ curLabel + " at IP address [" + destIP + "].");
			}
		}
	}
	
	public String getPUI() {
		return pui;
	}
	
	public String getPUISettingName() {
		return puiSettingName;
	}
	

	public void lostRegistration() {
	    	regTable.remove(pui);
	    	
	    	status = PC2RegistrarStatus.UNREGISTERED;
			changeRegistrarStatus();
			logger.info(PC2LogCategory.Model, subCat,
					"NetworkElement with username=" + pui + " lost registration.");
			
	    	// Next we need to remove the information to the contactTable
			Properties p = SystemSettings.getPropertiesByValue(SettingConstants.PUI, pui);
			if (p != null) {
				String phoneNum = p.getProperty(SettingConstants.PHONE_NUMBER + "1");
				removeContact(phoneNum);
			}
			else if (p == null) {
				// Try pui2
				p = SystemSettings.getPropertiesByValue(SettingConstants.PUI2, pui);
				if (p != null) {
					String phoneNum = p.getProperty(SettingConstants.PHONE_NUMBER + "2");
					removeContact(phoneNum);
				}
			}
	    }
	    
	/**
	 * Implementation for delivering an event from one FSM to another.
	 * 
	 * @return -true if the message was delivered successfully to the
	 * 			target FSM, false otherwise. 
	 */
	@Override
	public boolean processEvent(Generate g) {
		if (g.getTarget() == null || g.getTarget().equals(fsm.getName())) {
		
			// We only want to tell the first script waiting for the device
			// to register, because all subsequent scripts will learn of it as soon
			// as they start.
			 if (g.getEvent().equalsIgnoreCase(EventConstants.REGISTERED) && !isRegistered()) {

				setRegistered();
				
				if (curLabel.equals("UE0"))
					informSessions(g.getEvent());

				// Lastly forward the event to ourselves on the chance the 
				// FSM wants to act upon it.
				InternalMsg msg = new InternalMsg(getFsmUID(), 
						System.currentTimeMillis(), LogAPI.getSequencer(), g.getEvent());
				processEvent(msg);
				return true;
			}
			else if (g.getEvent().equalsIgnoreCase(EventConstants.AUTO_REBOOT_EVENT)) {
					lostRegistration();
			}
			else if (g.getEvent().equalsIgnoreCase(EventConstants.REGISTRATION_LOST)) {
				lostRegistration();
				
				FSMListener t = Stacks.getSessionListener(PC2Protocol.SIP, destIP);
				if (t != null) {
					InternalMsg msg = new InternalMsg(t.getFsmUID(), System.currentTimeMillis(), 
							LogAPI.getSequencer(), g.getEvent());
					return t.processEvent(msg);
				}
			}
			else if (g.getEvent().equalsIgnoreCase(SIPConstants.REGISTER) && status == PC2RegistrarStatus.INACTIVE) {
				status = PC2RegistrarStatus.ATTEMPTING;
				changeRegistrarStatus();
			}
		}
		return super.processEvent(g);


	}
	/**
	 * Implementation for the Pass and Fail actions within a FSM.
	 * 
	 * @return - true if the global result value was updated, false otherwise
	 */
	@Override
	public boolean pass(Result r) {
		if (r.getPass())
			logger.info(PC2LogCategory.Model, subCat, "PASS");
		else 
			logger.info(PC2LogCategory.Model, subCat, "FAIL");
		return true;
	}
	
	private void reboot() {
//		// Next lets reboot the device automatically if possible or prompt the user to 
//		// perform the operation as long as the auto provision is not set.
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		if (platform != null) {
			String value = platform.getProperty(SettingConstants.AUTO_PROVISION);
			boolean autoProv = SystemSettings.resolveBooleanSetting(value);
			if (!autoProv) {
				Properties p = SystemSettings.getSettings(curLabel);
				if (p != null) {
					String myIP = p.getProperty(SettingConstants.IP);
					if (myIP != null) {
						if (!rebootTable.contains(myIP)) {
							// Since we haven't sent a request to the user or PACT to
							// reboot the device, create a Send event for the FSM and 
							// issue the request.
							rebootTable.add(myIP, curLabel);
							Send s = new Send(SettingConstants.UTILITY, UtilityConstants.SNMP_SET);
							s.setTarget(null);
							Mod m = new Mod(SettingConstants.REPLACE_MOD_TYPE);
							m.setHeader(UtilityConstants.SNMP_AGENT_IP);
							Literal l = new Literal(myIP);
							m.setRef(l);
							s.addModifier(m);
							
							m = new Mod(SettingConstants.ADD_MOD_TYPE);
							m.setHeader(UtilityConstants.SNMP_ARRAY);
							l = new Literal(SettingConstants.SNMP_REBOOT_ARRAY);
							m.setRef(l);
							s.addModifier(m);
							
							
							sendSNMP(s, this, curLabel);
						}
					}
				}
			}
		}
	}

	@Override
	public void run() {
		// First set the state into the initial state
		logger.info(PC2LogCategory.Model, subCat,
				"Beginning " + fsm.getName() + " thread.");
		
		super.run();
		
		lostRegistration();
	}
	/**
	 * Implementation for the Send action within a FSM.
	 * 
	 * @return - true when the message was sent, false otherwise
	 */
	@Override
	public boolean send(Send s) {
		if (s.getProtocol().equals(MsgRef.SIP_MSG_TYPE)) {
			locateRequest(s);
			checkForGinRegistration(s);
			// For the global registrar to obtain the configuration 
			// parameters to use in its' message, the class must
			// replace the DUT target with the network label
			// based upon the device type for lookup.
			if (s.getTarget().equals("DUT")) {
				s.setTarget(pui); // s.setTarget("IP:" + sourceIP);
				if (s.getTransportProtocol() == null)
					s.setTransportProtocol(defaultTransport);
//				Properties dut = SystemSettings.getSettings(s.getTarget());
//				if (dut != null) {
//					String device = dut.getProperty(SettingConstants.DEVICE_TYPE);
//					if (device != null) {
//						s.setTarget(device+"0");
//					}
//				}
			}
			SIPDistributor dist = Stacks.getSipDistributor();
			if (dist != null) {
				// Next look for this dialog in the table
				SIPMsg msgSent =  dist.send(this, s, 
						fsm.getNetworkElements(), fsm.getLastUtilityMsg(), fsm.getSipStack());
				if (msgSent == null)
					return false;

				ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
				if (rpf != null) {
					rpf.sent(msgSent);
					// LOG USING LOGMSG category so that the user can't disable which
					// would break the trace tool.
					logger.info(PC2LogCategory.LOG_MSG, subCat, 
							"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
							+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");
					q.add(msgSent);
				}
				return true;

			}
		}
		else if (s.getProtocol().equals(MsgRef.UTILITY_MSG_TYPE)) {

			return sendUtility(s, this);

		}

		return false;
	}

	public static void setMasterFSM(FSM Mfsm, Transport defTransport) {
		masterFSM = Mfsm;
		if (Mfsm == null) {
			regTable.clear();
		}	
		else {
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			if (platform != null) {
				try {
					int noResponseTimeout = Integer.parseInt(
							platform.getProperty(SettingConstants.GLOBAL_REGISTRAR 
									+ " " + SettingConstants.NO_RESP_TIMEOUT));
					masterFSM.setDefaultNoResponseTimeout(noResponseTimeout);
					logger.info(PC2LogCategory.Model, "",
							"GlobalRegistrar's No Response Timeout set to " 
							+ noResponseTimeout + ".");
				}
				catch (NumberFormatException nfe) {
					logger.warn(PC2LogCategory.Model, "",
							"GlobalRegistrar could not set the No Response Timeout from " 
							+ "the settings, because value isn't a number. System using 60000 " 
							+ "msec. by default");
					masterFSM.setDefaultNoResponseTimeout(60000);
				}
			}
			//masterFSM.setBackgroundServer();
		}
		GlobalRegistrar.defaultTransport = defTransport;
	}
	
	public void reset() {
		PC2UI ui = PCSim2.getUI();
		if (origLabel != null && !curLabel.equals(origLabel)){
			ui.changeRegistrarLabel(pui, curLabel, origLabel);
			curLabel = origLabel;
		}
	}
	
	public void setRegistered() {
		regTable.add(pui);
		
		status = PC2RegistrarStatus.REGISTERED;
		changeRegistrarStatus();
		logger.info(PC2LogCategory.Model, subCat,
				"NetworkElement " + pui + " registered on IP=" + destIP +".");
		
		// Next we need to add the information to the contactTable
		Properties p = SystemSettings.getPropertiesByValue(SettingConstants.PUI, pui);
		String line = "1";
		if (p == null) {
			// Try pui2
			p = SystemSettings.getPropertiesByValue(SettingConstants.PUI2, pui);
			line = "2";
		}
		if (p != null) {
			String phoneNum = p.getProperty(SettingConstants.PHONE_NUMBER + line);
			MsgEvent msg = q.find(fsm.getUID(), SIPConstants.REGISTER, 
					MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
			if (msg != null && msg instanceof SIPMsg) {
				SIPMsg sm = (SIPMsg)msg;
				Request req = sm.getRequest();
				ContactHeader ch = (ContactHeader)req.getHeader(ContactHeader.NAME);
				String contact = ch.getAddress().toString();
				if (contact != null) {
					contact = super.stripContact(contact);
					logger.info(PC2LogCategory.Model, "", 
							"Storing contact information for phoneNum=" 
							+ phoneNum + ", contact=" + contact);
					super.addContact(phoneNum, contact);
				}
			}
		}
	}
	
	public void setSipStack(String name) {
		if (fsm != null) {
		    fsm.setSipStack(name);
		}
	}
 
}
