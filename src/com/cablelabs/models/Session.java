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

import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;

import com.cablelabs.common.Conversion;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sim.PC2Protocol;
import com.cablelabs.sim.PCSim2;
import com.cablelabs.sim.Stacks;

/**
 * This is the Session model for the Test Script document.
 * It spawns the FSM off into its' own thread for processing.
 * 
 * @author ghassler
 *
 */
public class Session extends PC2Models {

	/**
	 * This table consists of the dialog instance defined by each message
	 * to the a unique call ID assigned by the stacks.
	 */
	protected Hashtable<Integer, Integer> dialogTable = null;
	
	static private final String model = "Session";
	/**
	 * Constructor
	 * @param fsm - the FSM to be used for the test.
	 */
	public Session(FSM fsm) {
		super(fsm, fsm.getName(), model);
		fsm.setAPI(this);
		fsm.setComparisonEvaluator(examiner);
		dialogTable = new Hashtable<Integer, Integer>();
	}
	
	/**
	 * Allows the session to subscribe for session messages from the 
	 * Stacks class on behalf of the DUT IP address.
	 */
	@Override
	public void init() {
		super.init();
		fsm.setDefaultNoResponseTimeout(SystemSettings.getNoResponseTimeout());
		if (fsm.getNetworkElements().getTargetsSize() > 0) {
			ListIterator<String> iter = fsm.getNetworkElements().getTargets();
			boolean hasUE = false;
			while (iter.hasNext() && !hasUE) {
				String target = iter.next();
				if (target.startsWith("UE"))
					hasUE = true;
			}
			iter = fsm.getNetworkElements().getTargets();	
			while (iter.hasNext()) {
				String target = iter.next();
				// First see if the target is a defined network element
				if (target.startsWith("UE")) {
					Properties p = SystemSettings.getSettings(target);
					if (p != null) {

						String username = p.getProperty(SettingConstants.USER_NAME);
						String numLines = p.getProperty(SettingConstants.PHONE_LINES);
						String uuid = p.getProperty(SettingConstants.UUID);
						if (username != null && !username.equals(""))
							Stacks.addSessionListener(PC2Protocol.SIP, username, this );
						else 
							logger.warn(PC2LogCategory.Model, subCat,
							"Session model unable to register for events based upon username.");
						if (numLines != null) {
							try {
								int lines = Integer.parseInt(numLines);
								for (int i = 1; i <= lines; i++) {
									String phoneNum = p.getProperty(SettingConstants.PHONE_NUMBER + Integer.toString(i));
									if (phoneNum != null && !phoneNum.equals(""))
										Stacks.addSessionListener(PC2Protocol.SIP, phoneNum, this );
									else 
										logger.warn(PC2LogCategory.Model, subCat,
										"Session model unable to register for events based upon phoneNum.");
								}
							}
							catch (NumberFormatException nfe) {
								logger.warn(PC2LogCategory.Model, subCat,
								"Session model unable to register for events based upon phone number settings.");
							}
						
						}
						if (uuid != null && !uuid.equals("")) {
							String urn = "urn%4euuid%00" + uuid.replace("-", "");
							Stacks.addSessionListener(PC2Protocol.SIP, urn.toUpperCase(), this);
						}
						else 
							logger.warn(PC2LogCategory.Model, subCat,
							"Session model unable to register for events based upon UUID.");
					}
					// Since the target is not a network element, take the literal string
					else if (target.equals("RTP")) {
						ListIterator<String> iter2 = fsm.getNetworkElements().getUEs();
						String label = iter2.next();
						if (label != null) {
							Properties dut = SystemSettings.getSettings(label);
							if (dut != null) {
								String dutIP = dut.getProperty(SettingConstants.IP);
								String dutIP2 = dut.getProperty(SettingConstants.IP2);
								String zone = dut.getProperty(SettingConstants.IPv6_ZONE);
								Stacks.addStunListener(PC2Protocol.RTP, dutIP, zone, this);
								Stacks.addStunListener(PC2Protocol.RTP, dutIP2, zone, this);
							}
						}
					}
					else {
						Stacks.addSessionListener(PC2Protocol.SIP, target, this );
//						logger.warn(PC2LogCategory.Model, subCat,
//						"Session model could not subscribe to the FSM Listener for target=(" 
//						+ neLabel + ").");

					}
				}
//				 Now see if this Session FSM is the processor for any 
				// Diameter Accounting messages
				else if (target.startsWith("CDF")) {
					Stacks.setDiameterAccountingListener(this);
				}
				else if (target.startsWith("PCSCF") ||
							target.startsWith("SCSCF") || 
							target.startsWith("AS"))  {
					if (!hasUE) {
						Properties p = SystemSettings.getSettings(target);
						if (p != null) {
							String pui = p.getProperty(SettingConstants.PUI);
							if (pui != null)
								Stacks.addSessionListener(PC2Protocol.SIP, pui, this );
							String fqdn = p.getProperty(SettingConstants.FQDN);
							if (fqdn != null)
								Stacks.addSessionListener(PC2Protocol.SIP, fqdn, this );
						}
					}
				}
				else if (target.equals(SettingConstants.RTP)) {
					// Get the IP address that we will be receiving the media upon
					ListIterator<String> iter2 = fsm.getNetworkElements().getUEs();
					String label = iter2.next();
					if (label != null) {
						Properties p = SystemSettings.getSettings(label);
						if (p != null) {
							String dutIP = p.getProperty(SettingConstants.IP);
							String dutIP2 = p.getProperty(SettingConstants.IP2);
							String zone = p.getProperty(SettingConstants.IPv6_ZONE);
							Stacks.addStunListener(PC2Protocol.RTP, dutIP, zone, this);
							Stacks.addStunListener(PC2Protocol.RTP, dutIP2, zone, this);
						}
					}
				}
				else if (target.equals(SettingConstants.STUN)) {
					ListIterator<String> iter2 = fsm.getNetworkElements().getUEs();
					String label = iter2.next();
					if (label != null) {
						Properties dut = SystemSettings.getSettings(label);
						if (dut != null) {
							String dutIP = dut.getProperty(SettingConstants.IP);
							String dutIP2 = dut.getProperty(SettingConstants.IP2);
							String zone = dut.getProperty(SettingConstants.IPv6_ZONE);
							Stacks.addStunListener(PC2Protocol.STUN, dutIP, zone, this);
							Stacks.addStunListener(PC2Protocol.STUN, dutIP2, zone, this);
						}
					}
				}
				else {
					Stacks.addSessionListener(PC2Protocol.SIP, target, this );
				}
			}
		}
	}
	
	/**
	 * Initializes the current FSM, and begins executing each of the messages that
	 * gets delivered to the FSM.
	 */
	@Override
	public void run() {
		// First set the state into the initial state
		logger.info(PC2LogCategory.Model, subCat,
				"Beginning Session(" + fsm.getName() + ") thread.");
		try {
			fsm.init(queue, this);
		}
		catch (IllegalStateException ise) {
			PCSim2.setTestPassed(false);
			String err = "Session(" + fsm.getName() + ") model failed during state machine initialization." + 
			    " Test terminated. Declaring test case failure.";
			logger.fatal(PC2LogCategory.Model, subCat, err);
			shuttingDown = true;
			PCSim2.setTestComplete();
		}
		
		boolean globalReg = SystemSettings.getBooleanSetting("Global Registrar");
		if (globalReg) {
			// We need to check for all of the UE labels that we are operating on behalf
			// of during the test. Get all of the UEs that are real in the test first
			ListIterator<String> iter = fsm.getNetworkElements().getTargets();
			while (iter.hasNext()) {
				String ne = iter.next();
				if (ne.startsWith("UE")) {
					Properties p = SystemSettings.getSettings(ne);
					if (p != null) {
						String simulated = p.getProperty(SettingConstants.SIMULATED);
						String pLabel = p.getProperty(SettingConstants.PCSCF);
						if (pLabel != null && !SystemSettings.resolveBooleanSetting(simulated)) {
							// Now that we know the device is NOT simulated and it
							// has an assigned PCSCF label, get the pui of the UE network 
							// element and the primary IP address of its' PCSCF
							Properties pcscf = SystemSettings.getSettings(pLabel);
							String pcscfIP = pcscf.getProperty(SettingConstants.IP);
							String pui = p.getProperty(SettingConstants.PUI);
							if (pui != null && pcscfIP != null) {
								if (Conversion.isIPv6Address(pcscfIP)) {
									String zone = pcscf.getProperty(SettingConstants.IPv6_ZONE);
									pcscfIP = Conversion.makeAddrURL(pcscfIP, zone);
								}
								FSMListener listener = Stacks.getRegistrarListener(PC2Protocol.SIP, 
										pcscfIP, pui);
								logger.debug(PC2LogCategory.Model, subCat, "Checking for SIP:" 
										+ pcscfIP + ":" + pui + " is registered");
								if (listener != null) {
									boolean registered = listener.isRegistered();
									if (registered) {
										// deliver a Registered message to the state machine
										InternalMsg msg = new InternalMsg(fsm.getUID(), 
												System.currentTimeMillis(), 
												LogAPI.getSequencer(), "Registered");
										logger.info(PC2LogCategory.Model, subCat, "Generating Registered event for SIP:" 
												+ pcscfIP + ":" + pui + ".");
										((FSMListener)this).processEvent(msg);
									}
								}
							}
						}
					}
				}
			}
			// With the new mechanism for Registrar delivery based upon the IP
			// address of the P-CSCF, we need to get the PCSCF NELabel and the DUT
			// and pass this into the request for the registrar.
			Properties dut = SystemSettings.getSettings("DUT");
			String device = dut.getProperty(SettingConstants.DEVICE_TYPE);
			if (device.equals("UE")) {
				String pcscfs = dut.getProperty(SettingConstants.PCSCF);
				StringTokenizer tokens = new StringTokenizer(pcscfs);
				while (tokens.hasMoreTokens()) {
					String pcscf = (String)tokens.nextElement();
					Properties p = SystemSettings.getSettings(pcscf);
					if (p != null && dut != null) {
						String localAddress = p.getProperty(SettingConstants.IP);
						String pui = dut.getProperty(SettingConstants.PUI);
						if (Conversion.isIPv6Address(localAddress)) {
							String zone = p.getProperty(SettingConstants.IPv6_ZONE);
							localAddress = Conversion.makeAddrURL(localAddress, zone);
						}
						FSMListener listener = Stacks.getRegistrarListener(PC2Protocol.SIP, 
								localAddress, pui);
						logger.debug(PC2LogCategory.Model, subCat, "Checking for SIP:" 
								+ localAddress + ":" + pui + " is registered");

						if (listener != null) {
							boolean registered = listener.isRegistered();
							if (registered) {
								// deliver a Registered message to the state machine
								InternalMsg msg = new InternalMsg(fsm.getUID(), 
										System.currentTimeMillis(), LogAPI.getSequencer(), "Registered");
								logger.info(PC2LogCategory.Model, subCat, "Generating Registered event for SIP:" 
										+ localAddress + ":" + pui + ".");
								((FSMListener)this).processEvent(msg);
							}
						}
						// Now do the same thing for the pui2
						String pui2 = dut.getProperty(SettingConstants.PUI2);
						if (Conversion.isIPv6Address(localAddress)) {
							String zone = p.getProperty(SettingConstants.IPv6_ZONE);
							localAddress = Conversion.makeAddrURL(localAddress, zone);
						}
						listener = Stacks.getRegistrarListener(PC2Protocol.SIP, 
								localAddress, pui2);
						logger.debug(PC2LogCategory.Model, subCat, "Checking for SIP:" 
								+ localAddress + ":" + pui2 + " is registered");
						if (listener != null) {
							boolean registered = listener.isRegistered();
							if (registered) {
								// deliver a Registered message to the state machine
								InternalMsg msg = new InternalMsg(fsm.getUID(), 
										System.currentTimeMillis(), LogAPI.getSequencer(), "Registered");
								logger.info(PC2LogCategory.Model, subCat, "Generating Registered event for SIP:" 
										+ localAddress + ":" + pui + ".");
								((FSMListener)this).processEvent(msg);
							}
						}
					}
					if (p == null)
						logger.warn(PC2LogCategory.Model, subCat, "Session (" 
								+ fsm.getName() + ") could not checking for registered, because  the DUT's PCSCF property was not found.");
					if (dut == null)
						logger.warn(PC2LogCategory.Model, subCat, "Session (" 
								+ fsm.getName() + ") could not checking for registered, because the DUT properties was not found.");
				}
			}
		}
		else 
			logger.debug(PC2LogCategory.Model, subCat, "Session (" 
					+ fsm.getName() + ") did not check for registered");
		
		
		super.run();
	}
}
