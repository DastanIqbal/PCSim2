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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

import com.cablelabs.common.Conversion;
import com.cablelabs.fsm.EventConstants;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.gui.PC2RegistrarStatus;
import com.cablelabs.gui.PC2UI;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.models.GlobalRegistrar;
import com.cablelabs.models.PresenceServer;

/**
 * This is a container class for all of the stacks
 * that is supported by the platform engine. Since
 * stacks remain active between test case executions,
 * the class is external to the actual FSMs.
 * 
 * This class is a singleton.
 * 
 * @author ghassler
 *
 */
public class Stacks  {

	/**
	 * The main interface between the FSMs and the 
	 * SIP stack
	 */
	protected static SIPDistributor sip = null;
	
		
	/**
	 * The main interface between the FSMs and the 
	 * STUN stack
	 */
	private static StunDistributor stun = null;
	
	/**
	 * The main interface between the FSMs and the 
	 * Utility stack
	 */
	protected static UtilityDistributor util = null;
	
	/**
	 * The single instantiation of this class.
	 */
	private static Stacks stacks = null;
	
	/**
	 * A private logger for the class.
	 */
	private static LogAPI logger = LogAPI.getInstance();
	
	/**
	 * Table of platform registrars that are currently active with
	 * in the system.
	 */
	private static Hashtable<String, GlobalRegistrar> registrarTable = null;
	
	/** 
	 * A table containing the association of an
	 * external elements username portion of the Request-URI with the FSM
	 * that will process any SIP session messages.
	 * The stacks require that an FSM must 'subscribe'
	 * to receive events from a specific device. In
	 * the case of SIP, it has been separated between
	 * to tables. One for registration processing and
	 * another for all others.
	 * 
	 * This table also holds all of the associations
	 * for the Utility stack.
	 */
	protected static Hashtable<String, FSMListener> session;
	
	/** 
	 * A table containing the association of an
	 * external elements Public User Identity with the FSM
	 * that will process any SIP registration messages.
	 * The stacks require that an FSM must 'subscribe'
	 * to receive events from a specific device. In
	 * the case of SIP, it has been separated between
	 * two tables. One for registration processing and
	 * another for all others.
	 * 
	 * 
	 */
	protected static Hashtable<String, FSMListener> registrars;
	
	/** 
	 * A table containing the association of an
	 * external elements IP address with the FSM
	 * that will process any STUN messages.
	 * The stacks require that an FSM must 'subscribe'
	 * to receive events from a specific device.
	 * 
	 */
	protected static Hashtable<String, FSMListener> stunServers;
	
	/** 
	 * A table containing the association of an
	 * external third-party Utility services with the FSM
	 * that will process any of that service's messages.
	 * The stacks require that an FSM must 'subscribe'
	 * to receive events for a specific service.
	 * 
	 */
	protected static Hashtable<String, FSMListener> utilityServices;

	private static boolean globalRegistration = false;
	
	/**
	 * This is a container for all of the allowable UE clients that
	 * can register with the system. The only allowable clients are
	 * those that are defined by a DUT configuration file(s) currently
	 * in the system and any elements within the Platform settings
	 * file that has their "simulated" setting set to false. The value
	 * part of the table is the NE Label of the client.
	 */
	private static Hashtable<String, String> registrarClients;
	
	/**
	 * This is a container for all of the currently active FSMListeners
	 * in the system.
	 */
	private static Hashtable<String, FSMListener> listeners = null;
	private static String subCat = "";
	
	private static FSMListener diameterAcctListener = null;
	
	private static PresenceServer presence = null;
	
	/**
	 * Private constructor of the singleton class.
	 *
	 */
	private Stacks() {
		session = new Hashtable<String, FSMListener>();
		registrars = new Hashtable<String, FSMListener>();
		stunServers = new Hashtable<String, FSMListener>();
		utilityServices = new Hashtable<String, FSMListener>();
		registrarClients = new Hashtable<String, String>();
		listeners = new Hashtable<String, FSMListener>();
//		logger = new LogAPI(); // Logger.getLogger(SIPDistributor.class);
	}

	private void clear() {
		
		if (util != null) {
			util.shutdown();
		}
		if (sip != null) {
			sip.shutdown();
		} 
		if (stun != null) {
			stun.shutdown();
		}
		if (presence != null) {
			presence.shutdown();
		}
		// Also clear all of the listeners
		if (registrarTable != null)
			registrarTable.clear();
		if (session != null)
			session.clear();
		if (registrars != null) 
			registrars.clear();
		if (stunServers != null)
			stunServers.clear();
		if (registrarClients != null) 
			registrarClients.clear();
		if (diameterAcctListener != null)
			diameterAcctListener = null;
		if (listeners != null)
			listeners.clear();
		logger.preserve("Servers Terminated.");
	}
	/**
	 * Provides a reference to the class. Since the class
	 * is a singleton, if it doesn't already exist, it will
	 * create an instance and then return a reference to it.
	 * 
	 * @return the single instantiation of the class.
	 */
	public synchronized static Stacks getInstance() {
		if (stacks == null) {
			stacks = new Stacks();
		}	
		return stacks;
	}

	/**
	 * Class initializer. It constructs all of the protocol stacks
	 * for the platform, therefore the platform settings must have
	 * been read and stored prior to its invocation.
	 *
	 */
	protected void init() {
		// Note stun stack must be created an initialized before creating
		// the sip stack.
		stun = new StunDistributor();
		stun.init();
		sip = new SIPDistributor();
		sip.init();
		util = new UtilityDistributor();
		util.init();
		globalRegistration = SystemSettings.getBooleanSetting("Global Registrar");
		
		if (registrarTable == null)
			registrarTable = new Hashtable<String, GlobalRegistrar>();
		
		// Lastly we need to get each of the stacks IP and
		// port information to store in the log files so that
		// sequence diagram tool can delineate between each test
		// and what was sent from the platform to external network
		// elements.
		String sipInfo = sip.getStackAddresses();
		String stunInfo = stun.getStackAddresses();
		String uInfo = util.getStackAddresses();
		logger.preserve("Servers Started.\n" 
				+ PCSim2.getPlatformSettingsFileName() +
				"\n" + sipInfo + stunInfo + uInfo);

	}
	
	/** 
	 * Adds the listener to the table of listeners for the given
	 * name.
	 * @param name
	 */
	public static void addListener(String name, FSMListener listener) {
		listeners.put(name,listener);
	}
	
	/**
	 * Allows a FSM to 'subscribe' for SIP registration events for
	 * processing.
	 * 
	 * @param protocol - sip 
	 * @param localAddress - the local network elements IP address
	 * @param zone - the interface zone for the IPv6 address
	 * @param pui - the public user identity of the device that is registering
	 * @param listener - the listener to whom the events should be delivered
	 * @throws IllegalArgumentException
	 */
	public static void addRegistrarListener(PC2Protocol protocol, String localAddress, 
			String zone, String pui, FSMListener listener) throws IllegalArgumentException {
		if (protocol == PC2Protocol.SIP) {
			String addr = localAddress;
			if (Conversion.isIPv6Address(addr))
				addr = Conversion.makeAddrURL(addr, zone);
			String key = getRegistrarKey(protocol, addr, pui);
			logger.info(PC2LogCategory.PCSim2, subCat,
					"Adding listener for key=" + key + " to registration table.");
			registrars.put(key, listener);
		}
		else {
			throw new IllegalArgumentException("The protocol type" + protocol + " is not currently supported by the simulator.");
		}
	}

	/**
	 * Allows a FSM to 'subscribe' for Utility services for processing.
	 * 
	 * @param protocol - stun
	 * @param peerAddress - the peer network elements IP address
	 * @param listener - the listener to whom the events should be delivered
	 * @throws IllegalArgumentException
	 */
//	public static void addServicesListener(String protocol,  FSMListener listener) throws IllegalArgumentException {
//		if (UtilityConstants.supportedService(protocol)) {
//			String key = protocol;
//			logger.debug(PC2LogCategory.PCSim2, subCat,
//					"Adding listener for key=" + key + " to services table.");
//			utilityServices.put(key, listener);
//		}
//		else {
//			throw new IllegalArgumentException("The service type" + protocol + " is not currently supported by the simulator.");
//		}
//	}

	/**
	 * Allows a FSM to 'subscribe' for SIP session and Utility events for
	 * processing.
	 * 
	 * @param protocol - sip or utility
	 * @param username - the username portion of the Request-URI
	 * @param listener - the listener to whom the events should be delivered
	 * @throws IllegalArgumentException
	 */
	public static void addSessionListener(PC2Protocol protocol, String username, FSMListener listener) throws IllegalArgumentException {
		if (protocol == PC2Protocol.SIP || 
				protocol == PC2Protocol.UTILITY) {
			String addr = null;
			if (protocol == PC2Protocol.SIP) {
				addr = username;
				if (Conversion.isIPv6Address(addr))
					addr = Conversion.makeAddrURL(addr, "0");
				else
					addr = null;
			}
			String key = null;
			if (addr != null) {
				key = protocol + ":" + cleanUserName(addr);
				if (session.containsKey(key)) {
					logger.info(PC2LogCategory.PCSim2, subCat, 
							"Replacing listener " + listener.getFSMName() + " for key=" + key + " in session table.");
					session.put(key, listener);
				}
				else {
					logger.info(PC2LogCategory.PCSim2, subCat, 
							"Adding listener " + listener.getFSMName() + " for key=" + key + " to session table.");
					session.put(key, listener);
				}
			}
			key = protocol + ":" + cleanUserName(username);
			if (session.containsKey(key)) {
				logger.info(PC2LogCategory.PCSim2, subCat, 
						"Replacing listener " + listener.getFSMName() + " for key=" + key + " in session table.");
					session.put(key, listener);
			}
			else {
				logger.info(PC2LogCategory.PCSim2, subCat, 
					"Adding listener " + listener.getFSMName() + " for key=" + key + " to session table.");
				session.put(key, listener);
			}
		}
		else {
			throw new IllegalArgumentException("The protocol type " + protocol + " is not currently supported by the simulator.");
		}
	}

	/**
	 * Adds a new client's public user identity to those allowed to register to the 
	 * platform. The key information is obtained directly from the property
	 * information.
	 * 
	 * @param neLable - The NE Label for the device to add to the table of 
	 *     eligible devices that can register with the platform.
	 * 
	*/
	public static void addSIPRegistrarClient(String neLabel, LinkedList<String> puis) {
		if (globalRegistration && 
				neLabel != null && 
				puis.size() > 0) {
			Properties ne = SystemSettings.getSettings(neLabel);
			if (ne != null ) {
				String pLabel = ne.getProperty(SettingConstants.PCSCF);
				if (pLabel != null) {
					Properties pcscf = SystemSettings.getSettings(pLabel);
					if (pcscf != null) {
						String pcscfIP = pcscf.getProperty(SettingConstants.IP);
						if (pcscfIP != null) {
							ListIterator<String> iter = puis.listIterator();
							while (iter.hasNext()){
								String pui = iter.next();
								if (Conversion.isIPv6Address(pcscfIP)) {
									String zone = pcscf.getProperty(SettingConstants.IPv6_ZONE);
									pcscfIP = Conversion.makeAddrURL(pcscfIP, zone);
								}
								String key = getRegistrarKey(PC2Protocol.SIP, pcscfIP, pui);
								if (registrarClients.containsKey(key)) {
									String curLabel = registrarClients.get(key);
									logger.warn(PC2LogCategory.PCSim2, subCat,
											" Replacing network element label associated to key=" + key 
											+ " from=" + curLabel + " to=" + neLabel 
											+ " in the table of allowable registering clients.");
								}
								else {
									logger.debug(PC2LogCategory.PCSim2, subCat, 
											"Adding key=" + key + " to the table of allowable registering clients and " +
											" is associated to network element label=" + neLabel + ".");
									registrarClients.put(key, neLabel);
									createRegistrar(key, pui, pcscfIP);
								}
							}
						}
						String pcscfIP2 = pcscf.getProperty(SettingConstants.IP2);
						if (!pcscfIP2.equals(pcscfIP)) {
							ListIterator<String> iter = puis.listIterator();
							while (iter.hasNext()){
								String pui = iter.next();
								if (Conversion.isIPv6Address(pcscfIP2)) {
									String zone = pcscf.getProperty(SettingConstants.IPv6_ZONE);
									pcscfIP2 = Conversion.makeAddrURL(pcscfIP2, zone);
								}
								String key = getRegistrarKey(PC2Protocol.SIP, pcscfIP2, pui);
								if (registrarClients.containsKey(key)) {
									String curLabel = registrarClients.get(key);
									logger.warn(PC2LogCategory.PCSim2, subCat,
											" Replacing network element label associated to key=" + key 
											+ " from=" + curLabel + " to=" + neLabel 
											+ " in the table of allowable registering clients.");
								}
								else {
									logger.debug(PC2LogCategory.PCSim2, subCat, 
											"Adding key=" + key + " to the table of allowable registering clients and " +
											" is associated to network element label=" + neLabel + ".");
									registrarClients.put(key, neLabel);
									GlobalRegistrar gr = createRegistrar(key, pui, pcscfIP2);
									// Next we need to identify the default stack for this global registrar
									Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
									try {
										int stacks = Integer.parseInt(platform.getProperty(SettingConstants.NUM_SIP_STACKS));
										boolean found = false;
										for (int i = 1; i<= stacks && !found; i++) {
											String name = platform.getProperty("SIP Stack " + i + " Name");
											Properties p = SystemSettings.getSettings(name);
											String stackIP = p.getProperty("javax.sip.IP_ADDRESS");
											if (stackIP.equals(pcscfIP2)) {
												found = true;
												gr.setSipStack(name);
											}
										}
									}
									catch (NumberFormatException nfe) {
										logger.fatal(PC2LogCategory.PCSim2, subCat, "The SIP Number of Stacks setting in the platform configuration file does not appear to be a number.");
										
									}
									
								}
							}
						}
					}
					else {
						logger.warn(PC2LogCategory.PCSim2, subCat, 
								"Couldn't add registrar listener for network label(" + neLabel 
								+ ") because system couldn't find its' PCSCF network label(" 
								+ pLabel + ").");
					}
				}
			}
		}
		// TODO - We may need to handle the condition that the global registrar
		// FSM may already exist and some special processing may need to occur
		// here. As of now, none has been identified.
	}
	
	/**
	 * Adds a new client's ip address to those allowed to register to the 
	 * platform.
	 * 
	 * @param localAddress - The address the device can register to.
	 * @param peerAddress - The address that the device will register from.
	 * @param neLable - The network element label for the device.
	 */
/*	public static void addSIPRegistrarClient(String localAddress, 
			String pui, String neLabel) {
		if (globalRegistration) {
			String key = getRegistrarKey(PC2Protocol.SIP, localAddress, pui);

			boolean exists = registrarClients.contains(key);
			if (!exists) {
				logger.debug(PC2LogCategory.PCSim2, subCat, 
						"Adding key=" + key + " to table of allowable registering clients.");
				registrarClients.put(key, neLabel);

			}
			// TODO - We may need to handle the condition that the global registrar
			// FSM may already exist and some special processing may need to occur
			// here. As of now, none has been identified.
		}
	}*/


	/**
	 * Allows a FSM to 'subscribe' for STUN events for processing.
	 * 
	 * @param protocol - stun
	 * @param peerAddress - the peer network elements IP address
	 * @param zone - the interface zone for the IPv6 address
	 * @param listener - the listener to whom the events should be delivered
	 * @throws IllegalArgumentException
	 */
	public static void addStunListener(PC2Protocol protocol, String peerAddress, 
			String zone, FSMListener listener) throws IllegalArgumentException {
		if (protocol == PC2Protocol.STUN ||
				protocol == PC2Protocol.RTP) {
			String addr = peerAddress;
			if (Conversion.isIPv6Address(addr))
				addr = Conversion.makeAddrURL(addr, zone);
			String key = protocol + ":" + addr;
			logger.info(PC2LogCategory.PCSim2, subCat,
					"Adding listener for key=" + key + " to " + protocol + " table.");
			stunServers.put(key, listener);
		}
		else {
			throw new IllegalArgumentException("The protocol type" + protocol + " is not currently supported by the simulator.");
		}
	}
	
	/**
	 * This is a common operation to remove a leading '+'
	 * and any '-' that might appear in a username 
	 * @param username - original username value  
	 * @return - the scrubbed username
	 */
	private static  String cleanUserName(String username) {
		// Because the username portion of a Request-URI may be
		// either a sip, sips or tel, we will remove any leading
		// + and any - from the passed in username
		if (username != null && username.length() >= 1) {
			String name = username;
			if (name.charAt(0) == '+') 
			name = name.substring(1);
			name = name.replace("-", "");
			return name;
		}
		return null;
	}
	
	private static GlobalRegistrar createRegistrar(String key, String pui, String localAddress) {
		GlobalRegistrar gr = null;
		FSM regFSM = GlobalRegistrar.getFSMCopy();
		if (regFSM != null) {
			gr = new GlobalRegistrar(regFSM, 
					pui, localAddress);
			
			if (gr != null) {
				registrarTable.put(key, gr);
				registrars.put(key, gr);
				gr.init();
			
				logger.debug(PC2LogCategory.PCSim2, subCat, 
						"Creating new platform registrar for network element(" + pui + ") for IP(" +
						localAddress + ").");
			}
		}
		return gr;
	}
	
	/**
	 * This method is used to look through the registrar table to determine which FSM
	 * need to receive the FSM reboot event.
	 */
	public static void generateAutoRebootEvent(String pui) {
		if (registrarTable != null) {
			Enumeration<String> e = registrarTable.keys();
			while (e.hasMoreElements()) {
				String key = e.nextElement();
				if (key.endsWith(":" + pui)) {
					GlobalRegistrar gr = registrarTable.get(key);
					if (gr != null) {
						InternalMsg msg = new InternalMsg(gr.getFsmUID(), System.currentTimeMillis(), 
								LogAPI.getSequencer(), EventConstants.AUTO_REBOOT_EVENT);
						gr.processEvent(msg);
					}
				}
			}
		}
	}
	/**
	 * Gets the listener that has registered to receive all of the 
	 * diameter accounting messages.
	 * @return
	 */
	public static FSMListener getDiameterAccountingListener() {
		return diameterAcctListener;
	}
	
	public static FSMListener getFSMListenerByName(String name) {
		Enumeration<FSMListener> elements = listeners.elements();
		while (elements.hasMoreElements()) {
			FSMListener l = elements.nextElement();
			if (l.getFSMName().equals(name)) {
				return l;
			}
		}
		
		return null;
	}

	/**
	 * Obtains the presence server for the system
	 */
	public static PresenceServer getPresenceServer() {
		return presence;
	}
	
	/**
	 * Sets the presence server for the system
	 */
	public static void setPresenceServer(PresenceServer server) {
		presence = server;
	}
	/**
	 * The registrar key comprises the concatenation of the following fields
	 * into a single string.
	 * 		Protocol:Received Address:Public User Identity
	 * 
	 * @param protocol - is the signaling protocol being used
	 * @param localAddress - is the IP Address that the message was received upon by
	 * 			the platform.
	 * @param pui - is the public address of the device.
	 * 			- in SIP case this is the name-addr in the From field.
	 * 
	 * @return key - the concatenated string
	 */
	public static String getRegistrarKey(PC2Protocol protocol, String localAddress, String pui) {
		String key = protocol + ":" + localAddress + ":" + pui ;
		return key;
	}
	/**
	 * Gets the listener that will process a SIP registration event for the given 
	 * protocol and public user identity. 
	 *
	 * When global registration is true in the platformsettings, this method will 
	 * create a new registration processor for any registration message received 
	 * from a system known UE network element label.
	 * 
	 * @param protocol - the protocol of the event
	 * @param localAddress - the IP Address that he message was received upon by the
	 * 	platform
	 * @param pui - the public user identity of the device
	 * @param puiSettingName - the SettingConstants name associated to the pui. This
	 * 		should either be the value 'pui' or 'pui2'
	 * 				
	 * @return - the listener to deliver the event to or null
	 */
	public static FSMListener getRegistrarListener(PC2Protocol protocol, 
			String localAddress, String pui) {
		String key = getRegistrarKey(protocol, localAddress, pui); 
		synchronized (registrars) {
			FSMListener result = registrars.get(key);
			
			// Since we couldn't find a specific FSM for the register message,
			// look and see if the platform is performing registration processing
			// and the IP address is a valid UE.
			if (result == null) {
				if (globalRegistration) {
					//String label = SystemSettings.isRegistrarClient(peerAddress);
					String label = registrarClients.get(key);
					// NOTE an empty label string simply means that we don't know the
					// current label of the ip address, but we know we should accept
					// registration from the client
					if (label != null) {	
						// Create a new FSM and Registrar to handle the elements
						GlobalRegistrar gr = createRegistrar(key, pui, localAddress);
						result = gr;
					}
					
				}
				else {
					logger.error(PC2LogCategory.PCSim2, subCat, "Failed to find " + key + " in registrar listener table.");
				}
			}
			
			return result;
		}
	}
	


	

	/**
	 * Gets all of the session model listeners currently "subscribed" in the
	 * system.
	 * 
	 * @return
	 */
	public static Enumeration<FSMListener> getSessionListeners() {
		return session.elements();
	}
	/**
	 * Gets the listener that will process a session event for the given 
	 * protocol and IP address.
	 * 
	 * @param protocol - the protocol of the event
	 * @param username - the username portion of the Request-URI.
	 * @return - the listener to deliver the event to or null
	 */
	public static FSMListener getSessionListener(PC2Protocol protocol, String username) {
		String key = protocol + ":" + cleanUserName(username);
		FSMListener result = session.get(key);
		if (result == null)
			logger.debug(PC2LogCategory.PCSim2, subCat, "Failed to find " + key + " in session listener table.");
		return result;
	}

	/**
	 * Get the SIP Distributor.
	 * @return
	 */
	public static SIPDistributor getSipDistributor() {
		return sip;
	}
	
	/**
	 * Get the STUN Distributor.
	 * @return
	 */
	public static StunDistributor getStunDistributor() {
		return stun;
	}

	/**
	 * Gets the listener that will process a STUN event for the given 
	 * protocol and IP address.
	 * 
	 * @param protocol - the protocol of the event
	 * @param peerAddress - the IP address of the network element that sent
	 * 				the stack this message.
	 * @return - the listener to deliver the event to or null
	 */
	public static FSMListener getStunListener(String protocol, String peerAddress) {
		String key = protocol + ":" + peerAddress;
		FSMListener result = stunServers.get(key);
		return result;
	}
	
	/**
	 * Get the Utility Distributor.
	 * @return
	 */
	public static UtilityDistributor getUtilDistributor() {
		return util;
	}

	/**
	 * Gets the listener that will process a session event for the given 
	 * protocol and IP address.
	 * 
	 * @param protocol - the protocol of the event
	 * @param peerAddress - the IP address of the network element that sent
	 * 				the stack this message.
	 * @return - the listener to deliver the event to or null
	 */
	public static FSMListener getUtilityListener(String protocol) {
		String key = protocol;
		FSMListener result = utilityServices.get(key);
		return result;
	}

	/**
	 * Allows Session FSMs to determine if an endpoint has 
	 * already registered with the system prior to it's 
	 * creation.
	 * 
	 * @param username - the username portion of the Request-URI
	 * @return - true if the device is registered, false otherwise.
	 */
	public static boolean isRegistered(String username) {
		FSMListener l = registrars.get(PC2Protocol.SIP + username);
		if (l != null) {
			return l.isRegistered();
		}
		return false;
	}
	
	public static void logStackSocketInformation() {
	
		String sipInfo = "";
		if (sip != null)
			sipInfo = sip.getStackAddresses();
		String stunInfo = "";
		if (stun != null)
			stunInfo = stun.getStackAddresses();
		String uInfo = "";
		if (util != null)
			uInfo = util.getStackAddresses();
		logger.preserve("Active servers.\n" 
				+ PCSim2.getPlatformSettingsFileName() +
				"\n" + sipInfo + stunInfo + uInfo);
	}
	/**
	 * Removes a client's ip address from those allowed to register to the 
	 * platform. The key information is obtained directly from the property
	 * information.
	 * 
	 * @param neLable - The NE Label for the device to remove from the table of 
	 *     elegible devices that can register with the platform.
	 * 
	*/
	public static void removeSIPRegistrarClient(String neLabel, LinkedList<String> puis) {
		if (globalRegistration && 
				neLabel != null && 
				puis.size() > 0) {
			Properties ne = SystemSettings.getSettings(neLabel);
			if (ne != null ) {
				String pLabel = ne.getProperty(SettingConstants.PCSCF);
				if (pLabel != null) {
					Properties pcscf = SystemSettings.getSettings(pLabel);
					if (pcscf != null) {
						String localAddress = pcscf.getProperty(SettingConstants.IP);
						if (localAddress != null) {
							ListIterator<String> iter = puis.listIterator();
							while (iter.hasNext()){
								String pui = iter.next();
								String key = getRegistrarKey(PC2Protocol.SIP, localAddress, pui);
								
								logger.debug(PC2LogCategory.PCSim2, subCat, 
									"Removing key=" + key + " from the table of allowable registering clients.");
								if (PCSim2.isGUIActive()) {
									PC2UI ui = PCSim2.getUI();
									ui.changeRegistrarStatus(PC2RegistrarStatus.DENIED, pui, neLabel);
								}
								if (registrarTable.contains(key))
									registrarTable.get(key).shutdown();
								registrarClients.remove(key);
							}
//							if (!pui2.equals(pui)) {
//								String key2 = getRegistrarKey(PC2Protocol.SIP, localAddress, pui2);
//								logger.debug(PC2LogCategory.PCSim2, subCat, 
//										"Removing key=" + key2 + " from the table of allowable registering clients.");
//								if (PCSim2.isGUIActive()) {
//									PC2UI ui = PCSim2.getUI();
//									ui.updateRegistrarElement(PC2RegistrarStatus.DENIED,pui2, neLabel);
//								}
//								if (registrarTable.contains(key2))
//									registrarTable.get(key2).shutdown();
//								registrarClients.remove(key2);
//							}
						}
					}
				}
			}
		}
	}
	
	public static void removeSessionListeners(FSMListener listener) {
		Enumeration<String> keys = session.keys();
		LinkedList<String> keysToRemove = new LinkedList<String>();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			FSMListener element = session.get(key);
			if (element.equals(listener)) {
				
				keysToRemove.add(key);
			}
		}
		
		// Now that we have identified all of them, remove
		// them from the table.
		ListIterator<String> iter = keysToRemove.listIterator();
		while (iter.hasNext()) {
			String key = iter.next();
			session.remove(key);
			logger.debug(PC2LogCategory.PCSim2, subCat, 
					"Removing listener " + listener.getFSMName() 
					+ " for key=" + key + " in session table.");
		}
		
		listeners.remove(listener.getFSMName());
	}
	/**
	 * Removes a client's ip address from those allowed to register to the 
	 * platform. 
	 * 
	 * @param localAddress - The address the device can register to.
	 * @param pui - The public user identity that the device will register under.
	 * 
	*/
/*	public static void removeSIPRegistrarClient(String localAddress, String pui, String peerAddress){
		if (globalRegistration) {
			String key = getRegistrarKey(PC2Protocol.SIP, localAddress, pui);

			String neLabel = registrarClients.get(key);
			if (neLabel != null) {
				logger.debug(PC2LogCategory.PCSim2, subCat, 
						"Removing key=" + key + " to table of allowable registering clients.");
				if (PCSim2.isGUIActive()) {
					PC2UI ui = PCSim2.getUI();
					ui.updateRegistrarElement(PC2RegistrarStatus.DENIED,peerAddress, neLabel);
				}
				GlobalRegistrar gr = registrarTable.get(key);
				if (gr != null) {
					gr.shutdown();
				}
				registrarClients.remove(key);
			}
		}
	}*/
	
	/**
	 * This method is invoked between each test case to allow and distributors a chance to clear specific test
	 * information that they maintain for performance reasons.
	 * 
	 */
	public static void reset() {
		utilityServices.clear();
		if (sip != null) 
			sip.clear();
		if (stun != null)
			stun.clear();
		diameterAcctListener = null;
		resetRegistrarDisplay();		
		// As a precaution remove all of the session listeners between each test
		session.clear();
	}
	
	/**
	 * Terminates all of the currently active stacks in preparation
	 * to be restarted by the system with new configuration settings.
	 *
	 */
	public void restart() {
		clear();
	}

	public static void setDiameterAccountingListener(FSMListener listener) {
		if (diameterAcctListener == null) {
			diameterAcctListener = listener;
			logger.debug(PC2LogCategory.PCSim2, subCat, 
					"Setting listener for Diameter accounting message to " 
					+ listener.getFSMName());
		}
		else {
			logger.error(PC2LogCategory.Diameter, subCat, 
					"An FSM is attempting to reassign the Diameter Accounting Listener.");
		}
	}

	/**
	 * This method notifies all of the fsms to shutdown.
	 *  
	 *
	 */
	public void shutdown() {
		shutdownGlobalRegistrars();
		clear();
		stacks = null;
	}
	
	/**
	 * This method notifies all of the platform registrars to
	 * shutdown.
	 *
	 */
	public void shutdownGlobalRegistrars() {
		Enumeration <GlobalRegistrar>e = registrarTable.elements();
		while (e.hasMoreElements()) {
			GlobalRegistrar reg = e.nextElement();
			reg.shutdown();
			
			reg.interrupt();
		}
	}
	
	private static void resetRegistrarDisplay() {
		Enumeration<GlobalRegistrar> e = registrarTable.elements();
		while (e.hasMoreElements()) {
			GlobalRegistrar gr = e.nextElement();
			gr.reset();
		}
//		Enumeration keys = registrars.keys();
//		while(keys.hasMoreElements()) {
//			String key = (String)keys.nextElement();
//			String label = SystemSettings.getLabelByIP(key);
//			PC2UI ui = PC2Sim.getUI();
//			ui.updateRegistrarElement(PC2RegistrarStatus.ACTIVE, key, label);
//		}
	}
	/**
	 * This method notifies all of the platform registrars to
	 * shutdown.
	 *
	 */
	public void shutdownPresenceServer() {
		if (presence != null) {
			presence.shutdown();
			presence.interrupt();
		}
	}
	public void updateRegistrarDisplay(String [] changes) {
		// First see if this is a change for a UE or not
		boolean update = false;
		for (int i=0; i < changes.length && !update; i++) {
			if (changes[i] != null && changes[i].equals("UE0"))
				update = true;
		}
		if (update) {
			// Since we need to update for each
			// value that is a pui, locate the GlobalRegistrar
			// and change the label to UE0
			for (int i=0; i < changes.length; i++) {
				if (changes[i].contains("@")) {
					// Since we are changing the registrar label for
					// all local IP addresses, we need to match the
					// last characters of the key to find all of the
					// GlobalRegistrars to change.
					Enumeration<String> keys = registrarTable.keys();
					boolean found = false;
					while (keys.hasMoreElements() && !found) {
						String key = keys.nextElement();
						if (key.endsWith(changes[i])) {
							GlobalRegistrar gr = registrarTable.get(key);
							gr.changeRegistrarLabel("UE0");
							found = true;
						}
					}
				}
			}
		}
	}
}
