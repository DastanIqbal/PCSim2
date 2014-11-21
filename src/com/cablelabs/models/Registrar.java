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


import java.util.ListIterator;
import java.util.Properties;

import javax.sip.header.ContactHeader;
import javax.sip.message.Request;

import com.cablelabs.fsm.EventConstants;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.Generate;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.fsm.SIPMsg;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sim.PC2Protocol;
import com.cablelabs.sim.PCSim2;
import com.cablelabs.sim.Stacks;

/**
 * This is the Registrar model as well as the internal registration
 * processing class when the configuration parameters defines that
 * the system maintain registration of endpoints across test cases.
 * 
 * Most of the operations for this class are common to all models and
 * are defined in the PC2Models base class.
 * 
 * @author ghassler
 *
 */
public class Registrar extends PC2Models {

	
	private String pui = null;
	
	/**
	 * This is the local IP address that the P-CSCF<id> is using for the
	 * test. This is the value used by the SIPDistributor to determine
	 * which FSM is processing SIP registration requests.
	 */
	private String destIP = null; 

	static private final String model = "Registrar";
	

	/**
	 *  Public Constructor for the Registrar model.
	 * 
	 * @param fsm - the FSM to use for the test.
	 * 
	 */
	public Registrar(FSM fsm, String pui, String destIP) {
		super(fsm, Registrar.class.getName(), model);
		this.pui = pui;	
		fsm.setAPI(this);
		fsm.setComparisonEvaluator(examiner);
	}


	/**
	 * Allows the registrar to subscribe for session messages from the 
	 * Stacks class on behalf of the DUT IP address.
	 */
	@Override
	public void init() {
		//Notify stacks that all registration processing for
		// SIP will be delivered to this FSM
		super.init();
		if (pui != null && fsm.getNetworkElements().getPCSCFSize() > 0) {
			ListIterator<String> iter = fsm.getNetworkElements().getPCSCFs();
			while(iter.hasNext()) {
				String label = iter.next();
				Properties p = SystemSettings.getSettings(label);
				if (p != null) {
					if (destIP == null)
						this.destIP = p.getProperty(SettingConstants.IP);
					String zone = p.getProperty(SettingConstants.IPv6_ZONE);
					Stacks.addRegistrarListener(PC2Protocol.SIP, destIP, zone, pui, this);
					String destIP2 = p.getProperty(SettingConstants.IP2);
					if (destIP != null && !destIP.equals(destIP2)) {
						Stacks.addRegistrarListener(PC2Protocol.SIP, destIP2, zone, pui, this);
					}
				}
			}
			
		}
		if (fsm.getNetworkElements().getTargetsSize() > 0) {
			ListIterator<String> iter = fsm.getNetworkElements().getTargets();
			while (iter.hasNext()) {
				String target = iter.next();
				// First see if the target is a defined network element
				Properties p = SystemSettings.getSettings(target.toUpperCase());
				if (p != null &&
						target.startsWith("UE")) {
					if (destIP == null)
						this.destIP = p.getProperty(SettingConstants.IP);
					String peerPUI = p.getProperty(SettingConstants.PUI);
					if (peerPUI != null) {
						String zone = p.getProperty(SettingConstants.IPv6_ZONE);
						Stacks.addRegistrarListener(PC2Protocol.SIP, destIP, zone, peerPUI, this);
					}
				}
				
				// Next register listeners for any CSCF objects
				else if (p != null) {
					if (destIP == null){
						this.destIP = p.getProperty(SettingConstants.IP);
					String zone = p.getProperty(SettingConstants.IPv6_ZONE);
					Stacks.addRegistrarListener(PC2Protocol.SIP, destIP, zone, pui, this);
					}
				}
				else if (target.equals(SettingConstants.STUN)) {
					Properties dut = SystemSettings.getPropertiesByValue(SettingConstants.PUI, pui);
					if (dut != null) {
						String dutIP = dut.getProperty(SettingConstants.IP);
						String dutIP2 = dut.getProperty(SettingConstants.IP2);
						String zone = dut.getProperty(SettingConstants.IPv6_ZONE);
						Stacks.addStunListener(PC2Protocol.STUN, dutIP, zone, this);
						Stacks.addStunListener(PC2Protocol.STUN, dutIP2, zone, this);
					}
				}
				else if (target.equals(SettingConstants.RTP)) {
					Properties dut = SystemSettings.getPropertiesByValue(SettingConstants.PUI, pui);
					if (dut != null) {
						String dutIP = dut.getProperty(SettingConstants.IP);
						String dutIP2 = dut.getProperty(SettingConstants.IP2);
						String zone = dut.getProperty(SettingConstants.IPv6_ZONE);
						Stacks.addStunListener(PC2Protocol.RTP, dutIP, zone, this);
						Stacks.addStunListener(PC2Protocol.RTP, dutIP2, zone, this);
					}
				}
				else if (target.equals(SettingConstants.PUI2.toUpperCase())) {
					String pui2 = null;
					Properties dut = SystemSettings.getPropertiesByValue(SettingConstants.PUI, pui);
					if (dut != null)
						pui2 = dut.getProperty(SettingConstants.PUI2);
					if (pui2 != null && !pui.equals(pui2)) {
						if (dut != null) {
							if (destIP == null)
								this.destIP = dut.getProperty(SettingConstants.IP);
							String zone = dut.getProperty(SettingConstants.IPv6_ZONE);
							Stacks.addRegistrarListener(PC2Protocol.SIP, destIP, zone, pui2, this);
						}
					}
				}
				else {
					String zone = "0";
					Stacks.addRegistrarListener(PC2Protocol.SIP, destIP, zone, target, this);
					// Also add the target value with the @domain as a precaution
					if (!target.contains("@")) {
						int amp = pui.indexOf("@");
						if (amp != -1 ) {
							String target2 = target + pui.substring(amp);
							Stacks.addRegistrarListener(PC2Protocol.SIP, destIP, zone, target2, this);
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
		try {
			fsm.init(queue, this);
		}
		catch (IllegalStateException ise) {
			PCSim2.setTestPassed(false);
			String err = "Session model failed during state machine initialization." + 
			" Test terminated. Declaring test case failure.";
			logger.fatal(PC2LogCategory.Model, subCat, err);
			shuttingDown = true;
			PCSim2.setTestComplete();
		}
		
		super.run();
		
	}
	
	/**
	 * Implementation for delivering an event from one FSM to another.
	 * 
	 * @return -true if the message was delivered successfully to the
	 * 			target FSM, false otherwise. 
	 */
	@Override
	public boolean processEvent(Generate g) {
		// First see if the event is Registered event or not
		if (g.getEvent().equals(EventConstants.REGISTERED)) {
			// Look in the session subscription table to see if 
			// there is a session FSM waiting for notification
			// of registration.
			FSMListener t = Stacks.getSessionListener(PC2Protocol.SIP, g.getTarget());
			if (t != null) {
				InternalMsg msg = new InternalMsg(t.getFsmUID(), System.currentTimeMillis(), 
						LogAPI.getSequencer(), g.getEvent());
				logger.info(PC2LogCategory.Model, subCat,
						"Registrar 	Creating event -" + msg.getEventName());
				return t.processEvent(msg);
			}
			else {
				t = Stacks.getFSMListenerByName(g.getTarget());
				if (t != null) {
					InternalMsg msg = new InternalMsg(t.getFsmUID(), System.currentTimeMillis(),
							LogAPI.getSequencer(), g.getEvent());
					logger.info(PC2LogCategory.Model, subCat,
							"Registrar 	Creating event -" + msg.getEventName());
					return t.processEvent(msg);
				}
			}
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
						addContact(phoneNum, contact);
					}
				}
			}
		}
		else if (g.getEvent().equals(EventConstants.REGISTRATION_LOST)) {
			// We need to remove the information to the contactTable
			Properties p = SystemSettings.getPropertiesByValue(SettingConstants.PUI, pui);
			String line = "1";
			
			if (p == null) {
				// Try pui2
				p = SystemSettings.getPropertiesByValue(SettingConstants.PUI2, pui);
				line = "2";
			}
			if (p != null) {
				String phoneNum = p.getProperty(SettingConstants.PHONE_NUMBER + line);
				removeContact(phoneNum);
			}
		}
		else {
			FSMListener t = Stacks.getFSMListenerByName(g.getTarget());
			if (t != null) {
				InternalMsg msg = new InternalMsg(t.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), g.getEvent());
				return t.processEvent(msg);
			}
			else if (g.getTarget() == null){
				InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), g.getEvent());
				return ((FSMListener)this).processEvent(msg);
			}
		}
			
		return false;
	}

	/**
	 * Looks up the FSM for the given ip address and returns
	 * true if the state's name is Registered.
	 * 
	 * @param ip - source address of the network element
	 * @return - true if the current State's name is "Registered"
	 * 			false otherwise
	 */
	public boolean isRegistered(String ip) {
		if (fsm != null) {
			return fsm.isCurrentStateRegistered();
		}
		return false;
	}
}
