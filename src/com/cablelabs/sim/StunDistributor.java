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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import com.cablelabs.common.Conversion;
import com.cablelabs.common.Transport;
import com.cablelabs.fsm.EventConstants;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.Literal;
import com.cablelabs.fsm.Mod;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.MsgRef;
import com.cablelabs.fsm.NetworkElements;
import com.cablelabs.fsm.RTPMsg;
import com.cablelabs.fsm.Reference;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.Stream;
import com.cablelabs.fsm.StunMsg;
import com.cablelabs.fsm.StunRef;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.log.MonitorListener;
import com.cablelabs.stun.Allocation;
import com.cablelabs.stun.Binding;
import com.cablelabs.stun.ByteArray;
import com.cablelabs.stun.RTPListener;
import com.cablelabs.stun.RawData;
import com.cablelabs.stun.StunConstants;
import com.cablelabs.stun.StunEvent;
import com.cablelabs.stun.StunFactory;
import com.cablelabs.stun.StunListener;
import com.cablelabs.stun.StunMessage;
import com.cablelabs.stun.StunMessageProcessor;
import com.cablelabs.stun.StunStack;
import com.cablelabs.stun.TurnStack;
import com.cablelabs.stun.attributes.Bandwidth;
import com.cablelabs.stun.attributes.ChannelNumber;
import com.cablelabs.stun.attributes.Data;
import com.cablelabs.stun.attributes.ErrorCode;
import com.cablelabs.stun.attributes.FingerPrint;
import com.cablelabs.stun.attributes.IceControlling;
import com.cablelabs.stun.attributes.Lifetime;
import com.cablelabs.stun.attributes.MessageIntegrity;
import com.cablelabs.stun.attributes.Nonce;
import com.cablelabs.stun.attributes.PeerAddress;
import com.cablelabs.stun.attributes.Priority;
import com.cablelabs.stun.attributes.Realm;
import com.cablelabs.stun.attributes.RequestedProps;
import com.cablelabs.stun.attributes.RequestedTransport;
import com.cablelabs.stun.attributes.StunAttribute;
import com.cablelabs.stun.attributes.Username;
import com.cablelabs.stun.attributes.XorMappedAddress;
import com.cablelabs.tools.RefLocator;


/**
 * This class is the main interface to the STUN stack it 
 * handles processing of binding requests made to the RTP
 * and RTCP ports currently defined within the platform
 * engine. It also processes any binding requests that are
 * embedded in another protocol that have requested processing
 * of the stack.
 * 
 * This class is a singleton.
 * 
 * @author ghassler
 *
 */
public class StunDistributor implements Distributor, MonitorListener, StunListener, RTPListener {

	/**
	 * The factory to create STUN messages
	 */
	private StunFactory factory = null;
	/**
	 * The STUN Stack
	 */
	private static StunStack stunStack = null;

	/** 
	 * The primary RTP Port that the STUN is automatically 
	 * responding to binding requests.
	 */
	protected int udpPort = 0;

	/** 
	 * The primary RTP Port that the STUN is automatically 
	 * responding to binding requests.
	 */
	protected int rtpPort1 = 0;

	/** 
	 * The primary RTCP Port that the STUN is automatically 
	 * responding to binding requests.
	 */
	protected int rtcpPort1 = 0;
	/** 
	 * The secondary RTP Port that the STUN is automatically 
	 * responding to binding requests.
	 */
	protected int rtpPort2 = 0;

	/** 
	 * The secondary RTCP Port that the STUN is automatically 
	 * responding to binding requests.
	 */
	protected int rtcpPort2 = 0;

	/**
	 * The total number of access points that the distributor
	 * is automatically processing.
	 */
	protected int accessPoints = 0;

	/**
	 * Accessor for logging to console and log files.
	 */
	private LogAPI logger = LogAPI.getInstance();

	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Distributor";

	/**
	 * The IP address that the STUN stack is using to send/receive
	 * messages.
	 */
	private String myIP = null;
	
	/**
	 * The second IP address that the STUN stack is using to send/receive
	 * messages.
	 */
	private String myIP2 = null;

	/**
	 * This is a flag indicating that an RTP packet has been added 
	 */
	private Hashtable<String, RTPData> rtpDelivered = new Hashtable<String, RTPData>();

	private Hashtable<String, Stream> streams = new Hashtable<String, Stream>();

	private TurnStack turnStack = null;

	private String defaultRealm = null;
	
	private boolean fingerPrintFlag = false;
	
	private boolean displayCompressedForm = false;
	
	private final static int DEFAULT_PORT = 3478;
	
	private boolean registerredMonitor = false;
	
	private RefLocator refLocator = RefLocator.getInstance();

	private String turnIP = null;
	
	private int turnUDPPort = -1;
	
	/**
	 * This method handles the automatic processing of the STUN Allocate Request
	 * message when there is no STUN FSM Listener to deliver it to for user-controlled
	 * processing.
	 * 
	 * @param se
	 * @param type
	 */
	private void allocateRequest(StunEvent se, char type, String username, String realm, String password) {
		StunMessage event = se.getEvent();
		Username u = (Username)event.getAttribute(StunConstants.USERNAME_TYPE);
		MessageIntegrity mi = (MessageIntegrity)event.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
		if (u != null) {
			// Since we have already passed validation of MessageIntegrity.
			// If it is present, we have a valid user, realm and password.
			StunMessage resp = null;
			String key = getKey(se.getRawData());
			Allocation a = turnStack.getAllocation(key);
			if (a == null) {
				Allocation da = turnStack.isDeletedAllocation(key);
				if (da != null) {
					// TODO resp = factory.createAllocateErrorResponse(msg, 437, realm);
					// TODO resp = factory.createAllocateErrorResponse(msg, 438, realm);
				}
				else {
					// Since the allocation is new verify that the
					// RequestedTransport is valid.
					RequestedTransport rt = (RequestedTransport)event.getAttribute(StunConstants.REQUESTED_TRANSPORT_TYPE);
					if (rt != null) {
						if (rt.isTransportUDP()) {
							Bandwidth b = (Bandwidth)event.getAttribute(StunConstants.BANDWIDTH_TYPE);
							if (b != null && !b.inRange()) {
								// TODO resp = factory.createAllocateErrorResponse(msg, 400, realm);
							}
							RequestedProps rp = (RequestedProps)event.getAttribute(StunConstants.REQUESTED_PROPS_TYPE);
							if (rp != null) {
								if (rp.isValidProp()) {
									String user = new String(u.getValue());
									Object source = se.getRawData().getSource();
									if (source instanceof StunMessageProcessor) {
										StunMessageProcessor smp = (StunMessageProcessor)source;
										// stack.getProcessor(msg.getID());
										if (smp != null) {
											a = turnStack.createAllocation(smp, key, user, se, rp.getProp(), Transport.UDP); 
											if (a != null) {
												Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
												resp = factory.createAllocateResponse(Transport.UDP, true, Allocation.TTL,
														true, false, user, realm, password, n,
														fingerPrintFlag);
											}

										}
//										else 
//											logger.warn(PC2LogCategory.STUN, subCat,
//											"STUN Stack could locate an StunMessageProcessor to allocate TURN port");
									}
								}
								else {
									// TODO resp = factory.createAllocateErrorResponse(msg, 508, realm);
								}

							}
//							else {
//								ReservationToken rToken = 
//									(ReservationToken)event.getAttribute(StunConstants.RESERVATION_TOKEN_TYPE);
//								if (rToken != null) {
//									String user = new String(u.getValue());
//									Object source = se.getRawData().getSource();
//									if (source instanceof StunMessageProcessor) {
//										StunMessageProcessor smp = (StunMessageProcessor)source;
//
//										if (smp != null) {
//											a = turnStack.createAllocation(smp, key, user, se, rp.getProp(), Transport.UDP); 
//											if (a != null) {
//												Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
//												resp = factory.createAllocateResponse(Transport.UDP, false, Allocation.TTL,
//														false, true, user, realm, password, n,
//														fingerPrintFlag);
//											}
//										}
//									}
//								}
//							}
						}
						else {
							// TODO resp = factory.createAllocateErrorResponse(se, 422, realm);
						}
					}
					else {
						// TODO resp = factory.createAllocateErrorResponse(msg, 400, realm);
					}
				}
			}
			else {
				if (mi != null) {
					if (java.util.Arrays.equals(event.getTransactionID(), 
							a.getTransactionID())) {
						// The Allocation already exists rebuild the response
						// with the existing allocation.
						// TODO resp = factory.createAllocateResponse(msg, allocation);
					}
					else {
						// TODO resp = factory.createAllocateErrorResponse(msg, 437, realm);
					}
				}
				else {

				}
			}
			if (resp != null) {
				RawData rd = se.getRawData();
				InetSocketAddress remoteAddr = new InetSocketAddress(rd.getSrcIP(), 
						rd.getSrcPort());
				int seq = LogAPI.getSequencer();
				Object source = rd.getSource();
				if (source instanceof StunMessageProcessor)
					stunStack.sendMessage(
							((StunMessageProcessor)source).getID(),
							resp, seq, remoteAddr);
				logger.debug(PC2LogCategory.STUN, subCat,
						"STUN Distributor sending automatic TURN Response :\n" + resp);
			}

		}
	}

	/**
	 * This method handles the automatic processing of the STUN Binding Request
	 * message when there is no STUN FSM Listener to deliver it to for user-controlled
	 * processing.
	 * 
	 * @param se
	 * @param type
	 */
	private void bindingRequest(StunEvent se, char type, String username, String realm, String password) {
		StunMessage event = se.getEvent();
		Username u = (Username)event.getAttribute(StunConstants.USERNAME_TYPE);
		StunMessage resp = null;
		if (u == null) {
			if (realm == null && defaultRealm != null) {
				// resp = factory.createBindingErrorResponse(se, 401, defaultRealm);
				// For now Vikas has requested that we respond with a success in this
				// condition. 
				// TEMP
				RawData rd = se.getRawData();
				String sip = rd.getSrcIP();
				String dip = rd.getDestIP();
				int sp = rd.getSrcPort();
				int dp = rd.getDestPort();
 			    resp = factory.createBindingResponse(se);
			    rd.setSrcIP(sip);
			    rd.setDestIP(dip);
			    rd.setSrcPort(sp);
			    rd.setDestPort(dp);
			}
			else if (realm != null){
				resp = factory.createBindingErrorResponse(se, 401, realm);
			}
		}
		else {
			MessageIntegrity mi = (MessageIntegrity)event.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
			Realm r = (Realm)event.getAttribute(StunConstants.REALM_TYPE);
			Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
			if (mi == null ||
					r == null ||
					n == null) {
				resp = factory.createBindingErrorResponse(se, 400, null);
			}

			if (username.length() > 0) {
				resp = factory.createBindingResponse(se, username, realm, password, n, fingerPrintFlag);
			}
			else {
				resp = factory.createBindingErrorResponse(se, 401, realm);
			}
		}

		if (resp != null) {
			RawData rd = se.getRawData();
			InetSocketAddress remoteAddr = new InetSocketAddress(rd.getSrcIP(), rd.getSrcPort());
			int seq = LogAPI.getSequencer();
			Object source = rd.getSource();
			if (source instanceof StunMessageProcessor)
				stunStack.sendMessage(
						((StunMessageProcessor)source).getID(), 
						resp, seq, remoteAddr);
			logger.debug(PC2LogCategory.STUN, subCat,
					"STUN Distributor sending automatic STUN Response :\n" + resp);
		}

	}

	/**
	 * This method handles the automatic processing of the STUN Channel Bind Request
	 * message when there is no STUN FSM Listener to deliver it to for user-controlled
	 * processing.
	 * 
	 * @param se
	 * @param type
	 */
	private void channelBindRequest(StunEvent se, char type, String username, String realm, String password) {
		StunMessage event = se.getEvent();
		Username u = (Username)event.getAttribute(StunConstants.USERNAME_TYPE);
		StunMessage resp = null;
		if (u == null) 
			resp = factory.createBindingErrorResponse(se, 401, realm);
		else {
			PeerAddress pa = (PeerAddress)event.getAttribute(StunConstants.PEER_ADDRESS_TYPE);
			ChannelNumber cn = (ChannelNumber)event.getAttribute(StunConstants.CHANNEL_NUMBER_TYPE);
			if (pa != null && cn != null) {
				if (StunConstants.isValidChannelNumber(cn.getChannel())) {
					String key = getKey(se.getRawData());
					Allocation a = turnStack.addChannelBind(key, pa, cn);
					if (a != null) {
						resp = factory.createChannelBindResponse(event, username, realm, password, 
								Allocation.TTL*2, fingerPrintFlag);
//						Binding ba = a.getBinding(pa.getAddress());
//						Binding ca = a.getBinding(cn.getChannel());
//						if (ba == ca) 
//						logger.info(PC2LogCategory.STUN, subCat, "Bindings match.");

					}
					else {
						// TODO resp = factory.createChannelBindErrorResponse(se, 400, );
					}
				}
				else {
					// TODO resp = factory.createChannelBindErrorResponse(msg, 400, );
				}
			}
			else {
				// TODO resp = factory.createChannelBindErrorResponse(msg, 400, );
			}

		}

		if (resp != null) {
			RawData rd = se.getRawData();
			InetSocketAddress remoteAddr = new InetSocketAddress(rd.getSrcIP(), rd.getSrcPort());
			int seq = LogAPI.getSequencer();
			Object source = rd.getSource();
			if (source instanceof StunMessageProcessor) {
				stunStack.sendMessage(
						((StunMessageProcessor)source).getID(), 
						resp, seq, remoteAddr);
				logger.debug(PC2LogCategory.STUN, subCat,
						"STUN Distributor sending automatic STUN Response :\n" + resp);
			}
		}

	}
	
	/**
	 * This method is invoked between each test case to allow the distributor a chance to clear any specific test
	 * information that it is maintaining for a test for performance reasons.
	 * 
	 */
	public void clear() {
		rtpDelivered.clear();
		logger.removeMonitorListener(this);
		registerredMonitor = false;
		if (streams != null && streams.size() > 0) {
			Enumeration<Stream> elements = streams.elements();
			while (elements.hasMoreElements()) {
				Stream s = elements.nextElement();
				s.stop();
			}
			streams.clear();
		}
	}

	/**
	 * This method constructs the 5-tuple key used to reference any
	 * allocations created in the TURN stack when active.
	 * 
	 * @param rawData
	 * @return
	 */private String getKey(RawData rawData) {
		 String key = rawData.getSrcIP()
		 + "|" + rawData.getSrcPort() + "|"
		 + rawData.getDestIP() 
		 + "|" + rawData.getDestPort() + "|" 
		 + rawData.getTransport();
		 return key;
	 }

	 /**
	  * Initialization point of the STUN stack for the
	  * platform engine. It must be called before creation
	  * of the SIP stack and any other protocols that will
	  * use it for embedded STUN processing.
	  * 
	  * This method also constructs each of the automatic
	  * access points defined within the configuration and
	  * starts their respective threads running.
	  * 
	  */
	 public void init() {
		 try {
			 if (SystemSettings.getBooleanSetting("STUN Enabled") || 
					 SystemSettings.getBooleanSetting("TURN Enabled")) {
				 Properties platform = SystemSettings.getSettings("Platform");
				 factory = new StunFactory();	

				 myIP = platform.getProperty("STUN IP Address");
				 myIP2 = platform.getProperty("STUN IP Address2");
				 udpPort = Integer.parseInt(platform.getProperty("STUN UDP Port"));
				 if (myIP != null && udpPort != 0) {
					 if (Conversion.isIPv6Address(myIP)) {
						 String zone = platform.getProperty("STUN IPv6 Zone");
						 myIP = Conversion.addZone(myIP, zone);
					 }
					 if (myIP2 == null) {
						 myIP2 = myIP;
					 }
					 else if (!myIP2.equals(myIP)){
						 String zone = platform.getProperty("STUN IPv6 Zone2");
						 myIP2 = Conversion.addZone(myIP2, zone);
					 }
					 rtpPort1 = Integer.parseInt(platform.getProperty(SettingConstants.RTP_PORT1));
					 rtcpPort1 = Integer.parseInt(platform.getProperty(SettingConstants.RTCP_PORT1));
					 rtpPort2 = Integer.parseInt(platform.getProperty(SettingConstants.RTP_PORT2));
					 rtcpPort2 = Integer.parseInt(platform.getProperty(SettingConstants.RTCP_PORT2));
					 stunStack = StunStack.getInstance();
					 stunStack.setListener(this);
					 stunStack.setRTPListener(this);
					 fingerPrintFlag = SystemSettings.getBooleanSetting("STUN Include FingerPrint");
					 displayCompressedForm = SystemSettings.getBooleanSetting("STUN Message Compressed Form");
					 stunStack.setFingerPrint(fingerPrintFlag);
					 stunStack.setDisplayCompressedForm(displayCompressedForm);
					 stunStack.start();
					 String threadName = null;
					 if (udpPort > 0 && udpPort < 65536) {
						 if (SystemSettings.getBooleanSetting("TURN Enabled")) 
							 threadName = "STUN/TURN Server - UDP/" + myIP + ":" + udpPort;
						 else 
							 threadName = "STUN Server - UDP/" + myIP + ":" + udpPort;
						 
						 stunStack.createProcessor(myIP, udpPort, threadName);
						 accessPoints++;
					 }
					 if (rtpPort1 > 0 && rtpPort1 < 65536) {
						 threadName = "RTP Port1 - UDP/" + myIP + ":" + rtpPort1;
						 stunStack.createProcessor(myIP, rtpPort1, threadName);
						 accessPoints++;
					 }
					 if (rtcpPort1 > 0 && rtcpPort1 < 65536) {
						 threadName = "RTCP Port1 - UDP/" + myIP + ":" + rtcpPort1;
						 stunStack.createProcessor(myIP, rtcpPort1, threadName);
						 accessPoints++;
					 }

					 if (rtpPort2 > 0 && rtpPort2 < 65536) {
						 threadName = "RTP Port2 - UDP/" + myIP2 + ":" + rtpPort2;
						 stunStack.createProcessor(myIP2, rtpPort2, threadName);
						 accessPoints++;
					 }
					 if (rtcpPort2 > 0 && rtcpPort2 < 65536) {
						 threadName = "RTCP Port2 - UDP/" + myIP2 + ":" + rtcpPort2;
						 stunStack.createProcessor(myIP2, rtcpPort2, threadName);
						 accessPoints++;
					 }
					 
					 defaultRealm = platform.getProperty("STUN Default Binding Realm");
					 turnIP = platform.getProperty("TURN IP Address");
					 if (SystemSettings.getBooleanSetting("TURN Enabled")) {
						 if (Conversion.isIPv6Address(myIP)) {
							 String zone = platform.getProperty("TURN IPv6 Zone");
							 turnIP = Conversion.addZone(turnIP, zone);
						 }
						 int initPort = Integer.parseInt(platform.getProperty("TURN Initial Allocation Port"));
						 int numPorts = Integer.parseInt(platform.getProperty("TURN Number of Allocation Ports"));
						 if (initPort % 2 == 0 &&
								 initPort > 0  && 
								 initPort < 65535 &&
								 numPorts % 2 == 0 &&
								 numPorts > 0  &&
								 (initPort + numPorts) < 65536) {
							 turnStack = TurnStack.getInstance(turnIP, initPort, numPorts);
							 logger.debug(PC2LogCategory.STUN, subCat,
							 "STUN Distributor has created the TURN stack.");
							 String port = platform.getProperty(SettingConstants.TURN_UDP_PORT);
							 if (port != null && !port.equals("0")) {
								 turnUDPPort =Integer.parseInt(port);
								 threadName = "TURN Server - UDP/" + myIP + ":" + turnUDPPort;
								 stunStack.createProcessor(myIP, turnUDPPort, threadName);
								 accessPoints++;
							 }
							 
							 
						 }
						 else {
							 if (initPort % 2 != 0) {
								 logger.warn(PC2LogCategory.STUN, subCat,
										 "STUN Distributor could not create the TURN stack because " 
										 + "the \"TURN Initial Allocation Port\"=" + initPort 
										 + "] is not an even integer value.");
							 }
							 if (initPort < 0 || initPort > 65535) {
								 logger.warn(PC2LogCategory.STUN, subCat,
										 "STUN Distributor could not create the TURN stack because " 
										 + "the \"TURN Initial Allocation Port\"=" + initPort 
										 + "] is between 2 - 65534.");
							 }
							 if (numPorts % 2 != 0 ) {
								 logger.warn(PC2LogCategory.STUN, subCat,
										 "STUN Distributor could not create the TURN stack because " 
										 + "the \"TURN Number Allocation Ports\"=" + numPorts 
										 + "] is not an even integer value.");
							 }
							 if (numPorts <= 0 ) {
								 logger.warn(PC2LogCategory.STUN, subCat,
										 "STUN Distributor could not create the TURN stack because " 
										 + "the \"TURN Number Allocation Ports\"=" + numPorts 
										 + "] is not a value greater than 0.");
							 }
							 else if ((initPort + numPorts) > 65536) {
								 logger.warn(PC2LogCategory.STUN, subCat,
										 "STUN Distributor could not create the TURN stack because " 
										 + "the sum of \"TURN Initial Allocation Port\"=" + initPort 
										 + "] + \"TURN Number Allocation Ports\"=[" +
										 numPorts + "] exceeds the 65536 maximum port value.");
							 }

						 }
					 }

					 logger.debug(PC2LogCategory.STUN, subCat,
							 "STUN Distributor has created " + accessPoints + " accessPoints.");
				 }
				 else {
					 logger.error(PC2LogCategory.STUN, subCat, 
							 "STUN Distributor could not start the STUN/TURN processing because the "
							 + "STUN IP Address or STUN UDP Port settings in the plaform configuration "
							 + "file are not set.");
				 }
			 }
			 else {
				 logger.info(PC2LogCategory.STUN, subCat,
				 "STUN Distributor is not being started.");
			 }
		 }
		 catch (Exception e) {
			 String msg = "Failed initializing Stun Stack.";
			 logger.fatal(PC2LogCategory.STUN, subCat, msg, e);
		 }

	 }

	 
	 @Override
	public void processChannelDataEvent(RawData rawData) {
		 String key = getKey(rawData);
		 Allocation a = turnStack.getAllocation(key);
		 if (a != null) {
			 Object event = rawData.getData();
			 if (event instanceof ByteArray) {
				 byte [] chanData = ((ByteArray)event).getBuffer();
				 Character channel = Conversion.getChar(chanData, 0);
				 Binding b = a.getBinding(channel);
				 if (b != null) {
					 PeerAddress pa = b.getPeerAddress();
					 try {
						 InetAddress destAddr = InetAddress.getByAddress(pa.getAddress());
						 int destPort = pa.getPort();
						 InetSocketAddress remoteAddr = new InetSocketAddress(destAddr, destPort);
						 int seq = LogAPI.getSequencer();
						 byte [] data = new byte [chanData.length-4];
						 System.arraycopy(chanData,4,data, 0, chanData.length-4);

						 a.sendRawMessage(data, 
								 data.length, seq, remoteAddr);
					 }
					 catch (UnknownHostException uhe) {
						 logger.warn(PC2LogCategory.STUN, subCat,
								 "STUN Distributor could not convey Channel Data message because the peerAddress["
								 + Conversion.hexString(pa.getAddress()) + "] could not be found.");

					 }
					 catch (IOException ioe) {
						 logger.warn(PC2LogCategory.STUN, subCat,
								 "STUN Distributor could not convey Channel Data message because an error "
								 + "occurred during the write operation.");

					 }
				 }
			 }
		 }
	 }


	 @Override
	public void processEvent(StunEvent se) {
		 StunMessage event = se.getEvent();
		 String typeName = event.getName();
		 logger.debug(PC2LogCategory.STUN, subCat,
				 "STUN Distributor rcvd a " + typeName + " message\n[" 
				 + event + "]");
		 if (typeName != null) {
			 if (validateFingerPrint(event)) {
				 try {
					 char type = event.getMessageType();
					 String [] miResult = null;
					 // Verify the message integrity, if present
					 if ((miResult = validateMessageIntegrity(event)) != null) {
						 // Now that we know everything appears to be valid
						 // Determine if the message should be automatically
						 // processed, delivered to an FSM, or simply ignored
						 // The lookup for the listener is the protocol and the peer's address.
						 RawData rd = se.getRawData();
						 FSMListener listener = Stacks.getStunListener("STUN", rd.getSrcIP());
						 if (listener != null) {
							 Object source = rd.getSource();
							 int id = 0;
							 if (source != null && source instanceof StunMessageProcessor) {
								 id = ((StunMessageProcessor)source).getID();
							 }
							 StunMsg msg = new StunMsg(listener.getFsmUID(), rd.getTimeStamp(), 
									 rd.getSequencer(), rd.getTransport(), se, id,  false, new Boolean(false));
							 listener.processEvent(msg);
						 }
						 // See if the stun or turn is active
						 else if (stunStack != null || turnStack != null) {
							 // Next determine if the response is an error 
							 if (StunConstants.isErrorResponse(type)) {
								 processErrorResponse(se, type, miResult[0], miResult[1], miResult[2]);
							 }
							 else if (StunConstants.isResponse(type)) {
								 processResponse(se, type, miResult[0], miResult[1], miResult[2]);
							 }
							 else if (StunConstants.isIndication(type)) {
								 processIndication(se, type, miResult[0], miResult[1], miResult[2]);
							 }
							 else if (StunConstants.isRequest(type)) {
								 processRequest(se, type, miResult[0], miResult[1], miResult[2]);
							 }
						 }
						 else {
							 logger.info(PC2LogCategory.STUN, subCat, 
							 "STUN Distributor dropping packet because STUN/TURN are disabled.");
						 }
					 }
					 else {
						 logger.debug(PC2LogCategory.STUN, subCat,
									"STUN Distributor is failing MessageIntegrity validation for event=" 
									+ event.getName() + ". Dropping event.");
					 }
						
				 }
				 catch (Exception e) {
					 logger.warn(PC2LogCategory.STUN, subCat,
							 e.getMessage(), e);
				 }
			 }
			 else {
				 logger.debug(PC2LogCategory.STUN, subCat,
							"STUN Distributor is failing FingerPrint validation for event=" 
							+ event.getName() + ". Dropping event.");
			 }
		 }
	 }

	 private void processErrorResponse(StunEvent se, char type, String username,
			 String realm, String password) {
		 switch (type) {
		 case StunConstants.ALLOCATE_ERROR_RESPONSE_MSG_TYPE :
			 break;
		 case StunConstants.BINDING_ERROR_RESPONSE_MSG_TYPE :
			 break;
		 case StunConstants.CHANNEL_BIND_ERROR_RESPONSE_MSG_TYPE:
			 break;
		 case StunConstants.REFRESH_ERROR_RESPONSE_MSG_TYPE :
			 break;
		 }
	 }

	 private void processIndication(StunEvent se, char type, String username,
			 String realm, String password) {
		 switch (type) {
		 case StunConstants.SEND_INDICATION_MSG_TYPE :
			 sendIndication(se,type, username, realm, password);
			 break;
		 case StunConstants.DATA_INDICATION_MSG_TYPE :
			 logger.error(PC2LogCategory.STUN, subCat, 
			 "STUN Server received a DATA Indication from a client, dropping message.");
			 break;
		 }
	 }

	 private void processRequest(StunEvent se, char type, String username,
			 String realm, String password) {
		 switch (type) {
		 case StunConstants.ALLOCATE_REQUEST_MSG_TYPE :
			 allocateRequest(se, type, username, realm, password);
			 break;
		 case StunConstants.BINDING_REQUEST_MSG_TYPE :
			 bindingRequest(se, type, username, realm, password);
			 break;
		 case StunConstants.CHANNEL_BIND_REQUEST_MSG_TYPE:
			 channelBindRequest(se, type, username, realm, password);
			 break;
		 case StunConstants.REFRESH_REQUEST_MSG_TYPE :
			 refreshRequest(se, type, username, realm, password);
			 break;
		 }

	 }
	 private void processResponse(StunEvent se, char type, String username,
			 String realm, String password) {
		 switch (type) {
		 case StunConstants.ALLOCATE_RESPONSE_MSG_TYPE :
			 break;
		 case StunConstants.BINDING_RESPONSE_MSG_TYPE :
			 break;
		 case StunConstants.CHANNEL_BIND_RESPONSE_MSG_TYPE:
			 break;
		 case StunConstants.REFRESH_RESPONSE_MSG_TYPE :
			 break;
		 }
	 }


	 /**
	  * The processor for RTP requests that may need to be processed
	  * by an FSM.  
	  *  
	  */
	 @Override
	public void processEvent(RawData rawData) {
		 Object ba = rawData.getData();
		 int recvPort = rawData.getDestPort();
		 // We only need to continue to process the information
		 // if it is received on one of the RTP ports.
		 if (ba instanceof ByteArray) {
			 byte [] data = ((ByteArray)ba).getBuffer();
			 RTPMsg msg = new RTPMsg (0, 
					 System.currentTimeMillis(), 
					 LogAPI.getSequencer(),
					 rawData.getTransport(),
					 rawData.getSrcIP(), 
					 rawData.getSrcPort(),
					 rawData.getDestIP(), 
					 rawData.getDestPort(), 
					 "RTP", new Boolean(false), data);
//			 logger.debug(PC2LogCategory.SIP, "RTP",
//					 "RTP Distributor rcvd a " + msg.getPayloadType() + " RTP packet.\n[" 
//					 + msg + "]");
			 try {
				 String destIP = msg.getDestIP();
				 FSMListener listener = Stacks.getStunListener("RTP", destIP);
				 // RTP is unusual in the sense that no packets are actually delivered
				 // to the FSM, we simply place the packet into the MsgQueue for the
				 // specific FSM to use in testing whenever it wants. Also we only 
				 // add one packet, the first one that is received because we don't
				 // want the FSMs to have to deal with arbitrary RTP packets that may
				 // get received to a state with no transition for them.

				 // First see if we have already delivered a RTP packet to the queue
				 // for an FSM based upon the port we received the message
				 //int port = msg.getDestPort();
				 String deliveryKey = destIP + "|" + msg.getDestPort() 
				 + "-" + msg.getSrcIP() + "|" + msg.getSrcPort();
				 if (listener != null  && 
						 (recvPort == rtpPort1 || recvPort == rtpPort2)) {
					 RTPData rd = rtpDelivered.get(deliveryKey);
					 if (rd == null) {
						 logger.info(PC2LogCategory.LOG_MSG, "RTP",
								 ">>>>> RX:\tLength = " + msg.getRTP().length
								 + "\nReceived on IP|Port=" + msg.getDestIP() 
								 + "|" + msg.getDestPort()  
								 + "\nFrom IP|Port=" + msg.getSrcIP() 
								 + "|" + msg.getSrcPort()
								 + "\nSequencer=" + msg.getSequencer()
								 + "\nTransport=" + Transport.UDP
								 + "\n RTP "
								 + "[" + Conversion.hexString(msg.getRTP())+ "]");
						 msg.setUID(listener.getFsmUID());
						 MsgQueue q = MsgQueue.getInstance();
						 q.add(msg);
						 rd = new RTPData(listener, msg.getTimeStamp(), 
								 msg.getSrcIP(), msg.getSrcPort());
						 rtpDelivered.put(deliveryKey, rd);
						 logger.debug(PC2LogCategory.SIP, "RTP",
								 "RTP Distributor adding RTP Data for key" + deliveryKey 
								 + ". Table contains " + rtpDelivered.size() + " stream(s).");
						 if (!registerredMonitor) {
							 logger.addMonitorListener(this);
							 registerredMonitor = true;
						 }
						 logger.info(PC2LogCategory.SIP, "RTP",
								 "RTP Distributor added RTP packet to IP Address" 
								 + destIP + " for FSMListener(" + listener.getFSMName() + ").");
						 listener.processEvent(msg);
					 }
					 else {
						 rd.updateTime(msg.getTimeStamp());
					 }
				 }
				 else {
					 logger.debug(PC2LogCategory.SIP, "RTP",
							 "RTP Distributor dropping RTP packet to IP Address" 
							 + destIP + " because there is no listener for it.");

				 }
			 }
			 catch (Exception e) {
				 logger.warn(PC2LogCategory.SIP, "RTP",
						 e.getMessage(), e);
			 }
		 }
	 }

	 public void refreshRequest(StunEvent se, char type, String username,
			 String realm, String password) {
		 StunMessage event = se.getEvent();
		 Lifetime l = (Lifetime)event.getAttribute(StunConstants.LIFETIME_TYPE);
		 if (l != null) {
			 StunMessage resp = null;
			 Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
			 int ttl = Conversion.byteArrayToInt(l.getValue());
			 String key = getKey(se.getRawData());
			 if (ttl > 0) {

				 Allocation a = turnStack.getAllocation(key);
				 if (a != null) {
					 a.refresh();
					 resp = factory.createRefreshResponse(username, realm, password, n, Allocation.TTL,
							 fingerPrintFlag);
				 }
			 }
			 else if (ttl == 0) {
				 turnStack.deleteAllocation(key);
				 resp = factory.createRefreshResponse(username, realm, password, n, 0, fingerPrintFlag);
			 }

			 if (resp != null) {
				 RawData rd = se.getRawData();
				 InetSocketAddress remoteAddr = new InetSocketAddress(rd.getSrcIP(), rd.getSrcPort());
				 int seq = LogAPI.getSequencer();
				 Object source = rd.getSource();
				 if (source instanceof StunMessageProcessor)
					 stunStack.sendMessage(
							 ((StunMessageProcessor)source).getID(), resp, seq, remoteAddr);
				 logger.debug(PC2LogCategory.STUN, subCat,
						 "Server sending automatic STUN Response :\n" + resp);
			 }

		 }
	 }

	 /**
	  * This is the STUN stacks implementation for sending message generated by
	  * an FSM.
	  * 
	  * @param fsm - the FSM generating the STUN message
	  * @param s - the STUN message to be create 
	  * @param nes - the network elements currently being simulated
	  * @return - the message created by the method or null
	  */
	 public StunMsg send(FSMListener fsm, Send s, NetworkElements nes) {
		 if (factory == null) {
			 logger.error(PC2LogCategory.STUN, subCat, "StunDistributor can't send a message because "
					 + "the STUN protocol is not enabled.");
			 return null;
		 }
		 
		 if (s.getProtocol().equalsIgnoreCase(MsgRef.STUN_MSG_TYPE)) {
			 if (s.getRequest() instanceof StunMsg) {
				 StunMsg req = (StunMsg)s.getRequest();
				 StunEvent se = req.getEvent();
				 String msgType = s.getMsgType();
				 if (msgType.equals(StunConstants.BINDING_REQUEST))  {
					 try {
						 String targetLabel = s.getTarget();
						 Properties target = SystemSettings.getSettings(targetLabel);
						 if (target != null) {
							 String username = target.getProperty(SettingConstants.USER_NAME);
							 String realm = target.getProperty(SettingConstants.DOMAIN);
							 String password = target.getProperty(SettingConstants.PASSWORD);
							 Nonce n = (Nonce)se.getEvent().getAttribute(StunConstants.NONCE_TYPE);
							 StunMessage resp = factory.createBindingResponse(se, username, realm,
									 password, n, fingerPrintFlag);
							 //StunMessage resp = factory.createBindingErrorResponse(se, 401, realm)

							 if (s.hasModifiers()) {
								 modifyStun(fsm.getFsmUID(), s, resp, fsm.getCurrentMsgIndex());
							 }
							 InetSocketAddress remoteAddr = new InetSocketAddress(req.getSrcIP(), 
									 req.getSrcPort());
							 int seq = LogAPI.getSequencer();
							 stunStack.sendMessage(req.getID(), resp, seq, remoteAddr);
							 logger.debug(PC2LogCategory.STUN, subCat,
									 "StunDistributor sent Stun Response :\n" + resp);
							 String destIP = req.getSrcIP();
							 int destPort = req.getSrcPort();
							 String srcIP = req.getDestIP();
							 int srcPort = req.getDestPort();
							 Transport transport = req.getTransport();
							 StunEvent newSE = new StunEvent(resp, null);

							 StunMsg stunResp = new StunMsg(fsm.getFsmUID(), System.currentTimeMillis(),
									 seq, transport, srcIP, srcPort, destIP, destPort, 
									 req.getID(), newSE, req.useIPV6(), new Boolean(true));
							 return stunResp;
						 }
					 }
					 catch (Exception e) {
						 logger.warn(PC2LogCategory.STUN, subCat,
								 "Failed constructing Binding Response for FSM with uid=" + 
								 fsm.getFsmUID() + ".", e);
					 }
				 }

				 else if (StunConstants.isErrorType(msgType)) {
					 try {
						 int statusCode = Integer.parseInt(s.getMsgType().substring(0,3));
						 StunMessage resp = null;
						 String targetLabel = s.getTarget();
						 Properties target = SystemSettings.getSettings(targetLabel);
						 if (target != null) {
							 String realm = target.getProperty(SettingConstants.DOMAIN);
							 if (statusCode == 401) {
								 resp = factory.createBindingErrorResponse(se, statusCode, realm);

							 }
							 else {
								 String username = target.getProperty(SettingConstants.USER_NAME);
								 String password = target.getProperty(SettingConstants.PASSWORD);
								 resp = factory.createBindingErrorResponse(se, statusCode, 
										 username, realm, password, fingerPrintFlag);
							 }

							 if (resp != null) {

								 if (s.hasModifiers()) {
									 modifyStun(fsm.getFsmUID(), s, resp, fsm.getCurrentMsgIndex());
								 }
								 InetSocketAddress remoteAddr = new InetSocketAddress(req.getSrcIP(), 
										 req.getSrcPort());
								 int seq = LogAPI.getSequencer();
								 stunStack.sendMessage(req.getID(), resp, seq, remoteAddr);
								 logger.debug(PC2LogCategory.STUN, subCat,
										 "StunDistributor sent Stun Error Response :\n" + resp);
								 String destIP = req.getSrcIP();
								 int destPort = req.getSrcPort();
								 String srcIP = req.getDestIP();
								 int srcPort = req.getDestPort();
								 Transport transport = req.getTransport();
								 StunEvent newSE = new StunEvent(resp, null);
								 StunMsg stunResp = new StunMsg(fsm.getFsmUID(), System.currentTimeMillis(), seq,
										 transport, srcIP, srcPort, destIP, destPort, req.getID(), 
										 newSE, req.useIPV6(), new Boolean(true));
								 return stunResp;
							 }
						 }
					 }
					 catch (Exception e) {
						 logger.warn(PC2LogCategory.STUN, subCat,
								 "Failed constructing Binding Error for FSM with uid=" + 
								 fsm.getFsmUID() + ".", e);
					 }
				 }
				 else if (StunConstants.isStunResponse(msgType)) {
					 if (msgType.equals(StunConstants.BINDING_RESPONSE)) {
						 StunMessage resp = null;
						 StunMessage sm = se.getEvent();
						 Username u = (Username)sm.getAttribute(StunConstants.USERNAME_TYPE);
						 String username = null;
						 String password = null;
						 String realm = null;
						 if (u != null) {
							 username = new String(u.getValue(), 0, u.getValue().length);
							 if (username != null) {
								 Properties ne = SystemSettings.getPropertiesByValue(SettingConstants.USER_NAME, username);
								 if (ne != null) {
									 password = ne.getProperty(SettingConstants.PASSWORD);
									 realm = ne.getProperty(SettingConstants.DOMAIN);
								 }
							 }
						 }
						 if (u == null) {
							 resp = factory.createBindingResponse(se);
						 }	
						 else {
							 Nonce n = (Nonce)sm.getAttribute(StunConstants.NONCE_TYPE);
							 resp = factory.createBindingResponse(se, username, realm, password, n, fingerPrintFlag);
						 }

						 if (resp != null) {
							 if (s.hasModifiers()) {
								 modifyStun(fsm.getFsmUID(), s, resp, fsm.getCurrentMsgIndex());
							 }
							 InetSocketAddress remoteAddr = new InetSocketAddress(req.getSrcIP(), 
									 req.getSrcPort());
							 int seq = LogAPI.getSequencer();
							 stunStack.sendMessage(req.getID(), resp, seq, remoteAddr);
							 logger.debug(PC2LogCategory.STUN, subCat,
									 "StunDistributor sent Stun " + msgType + ":\n" + resp);
							 String destIP = req.getSrcIP();
							 int destPort = req.getSrcPort();
							 String srcIP = req.getDestIP();
							 int srcPort = req.getDestPort();
							 Transport transport = req.getTransport();
							 StunEvent newSE = new StunEvent(resp, null);
							 StunMsg stunResp = new StunMsg(fsm.getFsmUID(), System.currentTimeMillis(), seq,
									 transport, srcIP, srcPort, destIP, destPort, req.getID(), 
									 newSE, req.useIPV6(), new Boolean(true));
							 return stunResp;
						 }
					 }
				 }
			 }
			 else if (StunConstants.isStunRequest(s.getMsgType())) {
				 String msgType = s.getMsgType();
				 StunMessage req = null;
				 if (msgType.equals(StunConstants.BINDING_REQUEST)) {
					 if (s.getOriginator() != null) {
						 String label = s.getOriginator();
						 Properties src = SystemSettings.getSettings(label);
						 if (src != null) {
							// Get the short term credentials
							 // This is a connectivity check, so the format is peer username ":" local username
							 String username = s.getPeerICEUsername() + ":" + src.getProperty(SettingConstants.STUN_USERNAME_CREDENTIAL);
							 // The password should be the peer's password
							 String password = s.getPeerICEPassword(); 
							 String priority = src.getProperty(SettingConstants.STUN_PRIORITY);
							 if (username != null &&
									 password != null && 
									 priority != null) {
								 try {
									 int pr = Integer.parseInt(priority);
									 req = factory.createBindingRequest(username, password, fingerPrintFlag, 
											 pr, s.useIceLite(), s.useCandidate(), s.getIceControlling(), s.getTransactionId() );
									 
								 }
								 catch (NumberFormatException nfe) {
									 logger.warn(PC2LogCategory.STUN, subCat,
												"STUNDistributor couldn't create BindingRequest because the network element label(" 
											 + s.getOriginator() + ") doesn't have an interger value for the " 
											 + SettingConstants.STUN_PRIORITY + " setting.");
								 }
							 }
							 else {
								 if (username == null) {
									 logger.warn(PC2LogCategory.STUN, subCat,
												"STUNDistributor couldn't create BindingRequest because the network element label(" 
											 + s.getOriginator() + ") doesn't have the " + SettingConstants.STUN_USERNAME_CREDENTIAL + " setting.");
								 }
								 if (password == null) {
									 logger.warn(PC2LogCategory.STUN, subCat,
												"STUNDistributor couldn't create BindingRequest because the network element label(" 
											 + s.getOriginator() + ") doesn't have the " + SettingConstants.STUN_PASSWORD_CREDENTIAL + " setting.");
								 }
								 if (priority == null) {
									 logger.warn(PC2LogCategory.STUN, subCat,
												"STUNDistributor couldn't create BindingRequest because the network element label(" 
											 + s.getOriginator() + ") doesn't have the " + SettingConstants.STUN_PRIORITY + " setting.");
								 }
							 }
						 }
						 else {
							 logger.warn(PC2LogCategory.STUN, subCat,
										"STUNDistributor couldn't create BindingRequest because the system couldn't find originator(" 
									 + s.getOriginator() + ").");
						 }
					 }
					 else {
						 req = factory.createBindingRequest();
					 }
				 }

				 if (req != null) {
					 if (s.hasModifiers()) {
						 modifyStun(fsm.getFsmUID(), s, req, fsm.getCurrentMsgIndex());
					 }
					 int seq = LogAPI.getSequencer();
					 InetSocketAddress localAddr = sourceAddr(s);
					 int processorID = stunStack.getProcessorID(localAddr);
					 String target = s.getTarget();
					 if (target != null) {
						 Properties p = SystemSettings.getSettings(target);
						 if (p != null) {
							 String peerIP = null;
							 if (s.getToIP() == null)
								 peerIP = p.getProperty(SettingConstants.IP);
							 else {
								 MsgEvent event = MsgQueue.getInstance().get(fsm.getCurrentMsgIndex());
								 peerIP = refLocator.getReferenceInfo(fsm.getFsmUID(), s.getToIP(), event);
							 }
							 int peerPort = -1;
							 if (s.getToPort() == null)
								 peerPort = DEFAULT_PORT;
							 else {
								 MsgEvent event = MsgQueue.getInstance().get(fsm.getCurrentMsgIndex());
								 String port = refLocator.getReferenceInfo(fsm.getFsmUID(), s.getToPort(), event);
								 if (port != null) {
									 try {
										 peerPort = Integer.parseInt(port);
									 }
									 catch (NumberFormatException nfe) {
										 logger.warn(PC2LogCategory.STUN, subCat,
												 "StunDistributor failed to find the port referenced by " 
												 + s.getToPort() + ", using " + DEFAULT_PORT);
										 peerPort = DEFAULT_PORT;
									 }
								 }
							 } 
							 if (peerIP != null) {
								 InetSocketAddress remoteAddr = new InetSocketAddress(peerIP, peerPort);
								 stunStack.sendMessage(processorID, req, seq, remoteAddr);
								 StunEvent newSE = new StunEvent(req, null);
								 StunMsg stunReq = new StunMsg(fsm.getFsmUID(), System.currentTimeMillis(),
										 seq, Transport.UDP, myIP, udpPort, peerIP, peerPort, 
										 processorID, newSE, false, new Boolean(true));
								 return stunReq;
							 }
							 else {
								 logger.error(PC2LogCategory.STUN, subCat,
										 "STUNDistributor couldn't find IP Address("
										 + peerIP +") or port(" + peerPort + ") in order to send " 
										 + s.getMsgType() + " STUN message.");
							 }
						 }
						 else {
							 logger.error(PC2LogCategory.STUN, subCat,
									 "STUNDistributor couldn't find the property information for target("
									 + target +"), message(" + s.getMsgType() + ") not sent.");

						 }
					 }
					 else {
						 logger.error(PC2LogCategory.STUN, subCat,
								 "STUNDistributor couldn't send message(" + s.getMsgType() 
								 + ") because there is no target for the send.");

					 }
				 }
			 }
		 }
		 else if (s.getProtocol().equals(MsgRef.RTP_MSG_TYPE)) {
			 if (s.getMsgType().equalsIgnoreCase("PCMU")) {
				 byte [] pcmu = { (byte)0x80, 0x00, 0x02, 0x07, 0x00, 0x01,
						 0x76, 0x11, 0x5f, 0x31, (byte)0xec, (byte)0x9f, 0x7d, (byte)0xfd,
						 0x78, 0x6e, 0x6b, 0x6f, 0x6d, 0x79, (byte)0xfd, (byte)0xfa,
						 (byte)0xea, (byte)0xe0, (byte)0xed, 0x72, 0x7f, 0x7b, 0x7c, (byte)0xfc,
						 0x73, 0x69, 0x66, 0x74, 0x7b, (byte)0xfa, (byte)0xed, (byte)0xfb,
						 (byte)0xf9, (byte)0xeb, (byte)0xef, 0x7e, 0x75, 0x7a, 0x7b, 0x68,
						 0x72, (byte)0xed, 0x75, 0x73, 0x7b, 0x69, 0x7c, (byte)0xe6,
						 (byte)0xf5,(byte) 0xfb, (byte)0xec, (byte)0xf2, (byte)0xf9, (byte)0xe9, (byte)0xed, (byte)0xfb, 
						 (byte)0xef,(byte) 0xfe, 0x7e, (byte)0xe7, (byte)0xeb, 0x6d, 0x77, 0x7d, 
						 0x70, 0x6f, 0x6c, 0x68, 0x65, 0x72, (byte)0xf4, (byte)0xee,
						 (byte)0xfc, (byte)0xf7, (byte)0xec, (byte)0xf9, 0x68, 0x71, (byte)0xf9, 0x71,
						 0x77, 0x79, 0x6a, 0x75, (byte)0xf5, 0x7c 
				 };
				 byte [] msg = pcmu;
				 String target = s.getTarget();
				 if (target != null) {
					 Properties dest = SystemSettings.getSettings(target);
					 if (dest != null) {
						 String ip = dest.getProperty(SettingConstants.IP);
						 if (ip != null) {
							 String portStr = dest.getProperty(SettingConstants.SDP_PORT);
							 if (portStr != null) {
								 try {
									 int port = Integer.parseInt(portStr);
									 int processorID = 1;
									 if (s.getPort() == 2) {
										 processorID = 3;
										 portStr = dest.getProperty(SettingConstants.SDP_PORT2);
										 if (portStr != null)
											 port = Integer.parseInt(portStr);
										 else {
											 logger.error(PC2LogCategory.SIP, "RTP",
													 "Couldn't send RTP packet because there is no setting for SDPPort2 for NE label(" 
													 + target + " is not set.");
											 return null;
										 }
									 }
									 InetSocketAddress remoteAddr = new InetSocketAddress(ip, port); 

									 int seq = LogAPI.getSequencer();
									 stunStack.sendRawMessage(processorID, msg, msg.length, seq, remoteAddr, "RTP");

								 }
								 catch (NumberFormatException e) {
									 logger.error(PC2LogCategory.SIP, "RTP",
											 "Couldn't send RTP packet because " + portStr + " setting for NE label(" 
											 + target + " is not an integer.");
								 }
							 }
							 else
								 logger.error(PC2LogCategory.SIP, "RTP",
										 "Couldn't send RTP packet because there is no setting for SDPPort for NE label(" 
										 + target + " is not set.");
						 }
						 else
							 logger.error(PC2LogCategory.SIP, "RTP",
									 "Couldn't send RTP packet because there is no setting for IP for NE label(" 
									 + target + " is not set.");
					 }
					 else {
						 logger.warn(PC2LogCategory.SIP, "RTP",
								 "Couldn't send RTP packet to target because couldn't find the NE label(" 
								 + target + ")");
					 }

				 }
				 else {
					 logger.warn(PC2LogCategory.SIP, "RTP", 
					 "Couldn't send RTP packet to anyone, because target is null.");
				 }
			 }

		 }
		 return null;
	 }

	 private void sendIndication(StunEvent se, char Type, String username,
			 String realm, String password) {
		 StunMessage event = se.getEvent();
		 String key = getKey(se.getRawData());
		 Data d = (Data)event.getAttribute(StunConstants.DATA_TYPE);
		 PeerAddress pa = (PeerAddress)event.getAttribute(StunConstants.PEER_ADDRESS_TYPE);
		 if (pa != null) {
			 if (d != null) {
				 // Forward the data through the relay transport.
				 byte [] data = d.getValue();
				 try {
					 InetAddress peerAddr = InetAddress.getByAddress(pa.getAddress());

					 InetSocketAddress remote = new InetSocketAddress(peerAddr, pa.getPort());
					 int seq = LogAPI.getSequencer();
					 turnStack.sendRawMessage(key, data, data.length, seq, remote );
				 }
				 catch (UnknownHostException uhe) {
					 logger.error(PC2LogCategory.STUN, subCat, 
							 "STUN Stack could not forward Send Indication to TURN Stack because bad peer address[" 
							 + pa + "].");
				 }
			 }
			 else {
				 Allocation a = turnStack.getAllocation(key);
				 if (a != null)
					 a.addPermission(pa);
			 }
		 }

	 }
	 
	 private InetSocketAddress sourceAddr(Send s) {
		 InetSocketAddress localAddr = null;
		 String originator = s.getOriginator();
		  if (originator != null) {
			 // See if the destination is something other
			 // than a network element label. If it is
			 // STUN or TURN it means to send them
			 // using the configuration information for the 
			 // peer.
			 String ip = null;
			 String port = null;
			 int localPort = -1;
			 if (originator.equals(SettingConstants.STUN)) {
				 ip = myIP;
				 localPort = udpPort;
			 }
			 else if (originator.equals(SettingConstants.TURN) &&
					 turnStack != null) {
				 ip = turnIP;
				 localPort = turnUDPPort;
			 }
			 else { 
				 Properties src = SystemSettings.getSettings(originator);
				 if (src != null) { 
					 if (src != null) {
						 ip = src.getProperty(SettingConstants.IP);
						 port = src.getProperty(SettingConstants.SDP_PORT);
						 if (port != null) {
							 try { 
								 localPort = Integer.parseInt(port);
							 }
							 catch (NumberFormatException nfe) {

							 }
						 }
					 }

					 if (ip != null && localPort != -1) {
						 localAddr = new InetSocketAddress(ip, localPort);
					 }
					 else {	 
						 localAddr = new InetSocketAddress(myIP, 
								 udpPort);
					 }
				 }
				
			 }
		 }
		 else {
			 localAddr = new InetSocketAddress(myIP, 
						 udpPort);
		 }
		 return localAddr;
		 
	 }


	 /**
	  * This method will return an array of Strings if the message integrity field is present and it matches
	  * the calculated value or in the case when no message integrity field is present. 
	  * 
	  * @param event
	  * 
	  * @return
	  */
	 private String [] validateMessageIntegrity(StunMessage event) {
		 // First we need to remove the finger print attribute if it is present
		 String [] result = null;
		 FingerPrint fp = (FingerPrint)event.getAttribute(StunConstants.FINGERPRINT_TYPE);
		 if (fp != null) 
			 event.removeAttribute(StunConstants.FINGERPRINT_TYPE);
		 MessageIntegrity mi = (MessageIntegrity)event.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
		 if (mi != null) {
			 event.removeAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
			 Username u = (Username)event.getAttribute(StunConstants.USERNAME_TYPE);
			 if (u != null) {
				 String username = new String(u.getValue(), 0, u.getValue().length);
				 if (username != null) {
					 Properties ne = SystemSettings.getPropertiesByValue(SettingConstants.USER_NAME, username);
					 if (ne != null) {
						 String password = ne.getProperty(SettingConstants.PASSWORD);
						 String realm = ne.getProperty(SettingConstants.DOMAIN);
						 Realm r = (Realm)event.getAttribute(StunConstants.REALM_TYPE);
						 if (password != null && r != null) {
							 byte [] eventMI = mi.getValue(); 
							 MessageIntegrity localMI = new MessageIntegrity(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
							 localMI.calculate(event,  username, realm, password);
							 byte [] calcMI = mi.getValue();
							 if (java.util.Arrays.equals(eventMI, calcMI)) {
								 result = new String[3];
								 result[0] = username;
								 result[1] = realm;
								 result[2] = password;
								 return result;
							 }
							 else {
								 logger.warn(PC2LogCategory.STUN, subCat,
										 "STUN Distributor failing MessageIntegrity because event containted\neventMI=" 
										 + Conversion.hexString(eventMI) + "\n calcMI=" 
										 + Conversion.hexString(calcMI));
							 }
						 } 
						 else if (password != null) {
							 byte [] eventMI = mi.getValue(); 
							 MessageIntegrity localMI = new MessageIntegrity(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
							 localMI.calculate(event, password);
							 byte [] calcMI = mi.getValue();
							 if (java.util.Arrays.equals(eventMI, calcMI)) {
								 result = new String[3];
								 result[0] = username;
								 result[1] = realm;
								 result[2] = password;
								 return result;
							 }
							 else {
								 logger.warn(PC2LogCategory.STUN, subCat,
										 "STUN Distributor failing MessageIntegrity because event containted\neventMI=" 
										 + Conversion.hexString(eventMI) + "\n calcMI=" 
										 + Conversion.hexString(calcMI));
							 }
						 }

					 }
				 }
			 }
		 }
		 else
			 result = new String[3];

		 // Whether it passed or failed add the finger print back to the message
		 if (fp != null)
			 event.addAttribute(fp);

		 return result;
	 }

	 private boolean validateFingerPrint(StunMessage event) {
			FingerPrint fp = (FingerPrint)event.getAttribute(StunConstants.FINGERPRINT_TYPE);
			
			if (fp != null) {
				event.removeAttribute(StunConstants.FINGERPRINT_TYPE);
		        FingerPrint fpTest = new FingerPrint(StunConstants.STUN_EMPTY_FINGER_PRINT);
		        fpTest.calculate(event);
		        event.addAttribute(fp);
				byte [] msgFP = fp.getValue(); 
				byte [] calcFP = fpTest.getValue();
				if (java.util.Arrays.equals(msgFP, calcFP))
					return true;
				else {
					 logger.warn(PC2LogCategory.STUN, subCat,
							 "Client failing FingerPrint because event containted\neventFP=" 
							 + Conversion.hexString(msgFP) + "\n calcFP=" 
							 + Conversion.hexString(calcFP));
				 }
			}
			else 
				return true;
			return false;
	 }

	 /**
	  * This method provides a common view of the listening
	  * sockets opened in the stack for processing during
	  * this series of tests.
	  */
	 @Override
	public String getStackAddresses() {
		 String result = "";
		 if (myIP != null) {
			 result = "STUN " + Transport.UDP.toString() + " " + myIP + "|" + udpPort + "\n" +
			 "STUN " + Transport.UDP.toString() + " " + myIP + "|" + rtpPort1 + "\n" +
			 "STUN " + Transport.UDP.toString() + " " + myIP + "|" + rtcpPort1 + "\n" +
			 "STUN " + Transport.UDP.toString() + " " + myIP2 + "|" + rtpPort2 + "\n" +
			 "STUN " + Transport.UDP.toString() + " " + myIP2 + "|" + rtcpPort2 + "\n";
			 if (turnIP != null && turnUDPPort > 0)
				 result += "TURN " + Transport.UDP.toString() + " " + turnIP  + "|" + turnUDPPort + "\n";
		 }
		 return result;
	 }

	 /**
	  * This method is invoked once the basic message has
	  * been constructed to make modifications to it.
	  * 
	  * @param fsmUID
	  * @param s
	  * @param sm
	  * @param curMsgIndex - the index of the current event being processed
	  */
	 private void modifyStun(int fsmUID, Send s, StunMessage sm, int curMsgIndex) {
		 LinkedList<Mod> mods = s.getModifiers();
		 Iterator<Mod> iter = mods.iterator();
		 boolean recalcFingerPrint = true;
		 boolean recalcMessageIntegrity = true;
		 if (mods.size() > 0) {
			 while (iter.hasNext()) {
				 Mod element = iter.next();
				 String hdr = element.getHeader();
				 String param = element.getParam();
				 String modType = element.getModType();
				 if (hdr != null) {
					 // Header modifications
					 if (hdr.equals("Header")) {
						 if (modType.equals("replace") && param != null) {
							 if (param.equals("transaction_id")) {
								 if (element.getRef() instanceof Literal) {
									 byte [] arg = ((Literal)element.getRef()).getExpr().getBytes();
									 byte [] transId = new byte [StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH];
									 if (arg.length > StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH) {
										 System.arraycopy(arg,0,transId,0,StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH);
									 }
									 if (arg.length <= StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH) {
										 System.arraycopy(arg,0,transId,0,arg.length);
									 }try {
										 logger.debug(PC2LogCategory.STUN, subCat,
										 "Modifying transaction id using literal.");
										 sm.setTransactionID(transId);
									 }
									 catch (Exception e) {
										 logger.warn(PC2LogCategory.STUN, subCat,
												 "StunDistributor was unable to replace the transactionID.", e);
									 }
								 }
								 else if (element.getRef() instanceof StunRef) {
									 StunRef sr = (StunRef)element.getRef();
									 int uid = sr.getUID();
									 String key = sr.getMethod();
									 String instance = sr.getMsgInstance();
									 StunMsg msg =  (StunMsg)PCSim2.getMsgQueue().find(uid, key, 
											 instance, curMsgIndex);
									 if (msg != null) {

										 try {
											 logger.debug(PC2LogCategory.STUN, subCat,
													 "Modifying transaction id using previous message.");
											 sm.setTransactionID(msg.getMessage().getTransactionID());
										 }
										 catch (Exception e) {
											 logger.warn(PC2LogCategory.STUN, subCat,
													 "StunDistributor was unable to replace the transactionID.", e);
										 }
									 }
								 }
							 }
						 }
						 else if (modType.equals("delete")) {
							 // Deletion is supported for the entire Attribute only 
							 if (param == null) {
								 sm.setIgnoreHeader();
								 logger.debug(PC2LogCategory.STUN, subCat,
								 "Removed Stun message header from message ");
							 }
						 }
					 }
					 // XorMappedAddress modifications
					 else if (hdr.equals("XorMappedAddress")) {
						 XorMappedAddress xma = (XorMappedAddress)sm.getAttribute(StunConstants.XOR_MAPPED_ADDRESS_TYPE);
						 if (xma != null) {
							 if (modType.equals("replace") && param != null) {
								 if (param.equals("IP")) {
//									 byte [] newValue = null;
									 if (element.getRef() instanceof Literal) {
										 String expr = ((Literal)element.getRef()).getExpr();
										 xma.setAddress(expr);
										 logger.debug(PC2LogCategory.STUN, subCat,
												 "Modifying XorMappedAddress IP using literal to value=[" 
												 + Conversion.hexString(xma.getAddress()) + "].");
									 }
									 else if (element.getRef() instanceof StunRef) {
										 StunRef sr = (StunRef)element.getRef();
										 int uid = sr.getUID();
										 String key = sr.getMethod();
										 String instance = sr.getMsgInstance();
										 StunMsg msg =  (StunMsg)PCSim2.getMsgQueue().find(uid, key, 
												 instance, curMsgIndex);
										 if (msg != null) {
											 XorMappedAddress origXMA = (XorMappedAddress)msg.getMessage().getAttribute(StunConstants.XOR_MAPPED_ADDRESS_TYPE);
											 xma.setAddress(origXMA.getAddress());
											 logger.debug(PC2LogCategory.STUN, subCat,
													 "Modifying XorMapped IP using previous message to value=[" 
													 + Conversion.hexString(xma.getAddress()) + "].");
										 }
									 }
//									 if (newValue != null) {
//									 attr.setValue(newValue);
//									 logger.debug(PC2LogCategory.STUN, subCat,
//									 "XorMappedAddressAttribute now contains " + attr);
//									 }
								 }
								 else if (param.equals("port")) {
//									 byte [] newValue = null;
									 if (element.getRef() instanceof Literal) {
										 Integer newPort = Integer.parseInt(((Literal)element.getRef()).getExpr());
										 xma.setPort(newPort);
										 logger.debug(PC2LogCategory.STUN, subCat,
												 "Modifying XorMappedAddress port using literal to value=[" 
												 + xma.getPort() + "].");
									 }
									 else if (element.getRef() instanceof StunRef) {
										 StunRef sr = (StunRef)element.getRef();
										 int uid = sr.getUID();
										 String key = sr.getMethod();
										 String instance = sr.getMsgInstance();
										 StunMsg msg =  (StunMsg)PCSim2.getMsgQueue().find(uid, key, 
												 instance, curMsgIndex);
										 if (msg != null) {
											 XorMappedAddress origXMA = (XorMappedAddress)msg.getMessage().getAttribute(StunConstants.XOR_MAPPED_ADDRESS_TYPE);
											 xma.setPort(origXMA.getPort());
											 logger.debug(PC2LogCategory.STUN, subCat,
													 "Modifying XorMappedAddress port using previous message to value=[" 
													 + xma.getPort() + "].");
										 }	
									 }
//									 if (newValue != null) {
//									 attr.setValue(newValue);
//									 logger.debug(PC2LogCategory.STUN, subCat,
//									 "XorMappedAddressAttribute now contains " + attr);
//									 }
//									 else
//									 logger.warn(PC2LogCategory.STUN, subCat,
//									 "StunDistributor was unable to replace the XorMapped's port.");
								 }
							 }
							 else if (modType.equals("delete")) {
								 // Deletion is supported for the entire Attribute only 
								 if (param == null) {
									 sm.removeAttribute(StunConstants.XOR_MAPPED_ADDRESS_TYPE);
									 logger.debug(PC2LogCategory.STUN, subCat,
									 "Removed XorMappedAddressAttribute from message ");
								 }
							 }
						 }
//						 else if (xma == null && 
//								 modType.equals("add") || 
//								 modType.equals("replace")) {
//							 if (param != null && param.equals(SettingConstants.IP)) {
//									 MsgEvent event = MsgQueue.getInstance().get(curMsgIndex);
//									 String ip = refLocator.getReferenceInfo(fsmUID, element.getRef(), event);
//									 if (ip != null) {
//										 xma =  new XorMappedAddress(StunConstants.XOR_MAPPED_ADDRESS_TYPE,
//												StunConstants.FAMILY_IPv4, rd.getSrcPort(), 
//												rd.getSrcIP(), resp.getTransactionID());)
//									 }
//							 String port = refLocator.getReferenceInfo(fsmUID, element.getRef(), event);
//							 int localPort = -1;
//							 if (port != null) {
//								 try {
//									 localPort = Integer.parseInt(port);
//								 }
//								 catch (NumberFormatException nfe) {
//									 logger.warn(PC2LogCategory.STUN, subCat,
//											 "StunDistributor failed to find the port referenced by " 
//											 + s.getToPort() + ", using " + DEFAULT_PORT);
//								 }
//							 }
//							 xma =  new XorMappedAddress(StunConstants.XOR_MAPPED_ADDRESS_TYPE,
//										StunConstants.FAMILY_IPv4, rd.getSrcPort(), 
//										rd.getSrcIP(), resp.getTransactionID());)
//						 }
						 else {
							 logger.error(PC2LogCategory.STUN, subCat,
									 "StunDistributor could not modify " + hdr + " attribute because it is not in the message.");
						 }
					 }
					 // ErrorCode modifications
					 else if (hdr.equals("ErrorCode")) {
						 ErrorCode attr = (ErrorCode)sm.getAttribute(StunConstants.ERROR_CODE_TYPE);
						 if (attr != null) {
							 if (modType.equals("replace") && param != null) {
								 boolean cls = false;
								 boolean number = false;
								 boolean phrase = false;
								 byte [] newValue = null;
								 String field = "";
								 int position = 0;
								 if (param.equals("class")) {
									 cls = true;
									 field = "class";
									 position = 2;
								 }
								 else if (param.equals("number")) {
									 number = true;
									 field = "number";
									 position = 3;
								 }
								 else if (param.equals("phrase")) {
									 phrase = true;
									 position = 4;
								 }

								 if (cls || number) {
									 newValue = new byte [1];
									 if (element.getRef() instanceof Literal) {
										 newValue[0] = ((Integer)Integer.parseInt(((Literal)element.getRef()).getExpr())).byteValue();
										 logger.debug(PC2LogCategory.STUN, subCat,
												 "Modifying ErrorCode's " + field + " parameter using literal.");
									 }
									 else if (element.getRef() instanceof StunRef) {
										 StunRef sr = (StunRef)element.getRef();
										 int uid = sr.getUID();
										 String key = sr.getMethod();
										 String instance = sr.getMsgInstance();
										 StunMsg msg =  (StunMsg)PCSim2.getMsgQueue().find(uid, key, 
												 instance, curMsgIndex);
										 if (msg != null) {
											 StunAttribute origAttr = msg.getMessage().getAttribute(StunConstants.ERROR_CODE_TYPE);
											 newValue[0] = origAttr.getValue()[2];
											 logger.debug(PC2LogCategory.STUN, subCat,
													 "Modifying ErrorCode's " + field + " parameter using previous message.");
										 }
									 }
									 if (newValue != null) {
										 System.arraycopy(newValue,0,attr.getValue(),position,newValue.length);
										 logger.debug(PC2LogCategory.STUN, subCat,
												 "ErrorCodeAttribute now contains " + attr);
									 }
								 }
								 else if (phrase) {
									 if (element.getRef() instanceof Literal) {
										 newValue = ((Literal)element.getRef()).getExpr().getBytes();
										 logger.debug(PC2LogCategory.STUN, subCat,
										 "Modifying ErrorCode's reason phrase parameter using literal.");
									 }
									 else if (element.getRef() instanceof StunRef) {
										 StunRef sr = (StunRef)element.getRef();
										 int uid = sr.getUID();
										 String key = sr.getMethod();
										 String instance = sr.getMsgInstance();
										 StunMsg msg =  (StunMsg)PCSim2.getMsgQueue().find(uid, key, 
												 instance, curMsgIndex);
										 if (msg != null) {
											 StunAttribute origAttr = msg.getMessage().getAttribute(StunConstants.ERROR_CODE_TYPE);
											 System.arraycopy(origAttr.getValue(),4, newValue,0,(origAttr.getValue().length-4));
										 }
									 }
									 if (newValue != null) {
										 byte [] compValue = new byte [4 + newValue.length];
										 System.arraycopy(attr.getValue(),0,compValue,0,position);
										 System.arraycopy(newValue,0,compValue,position,newValue.length);
										 attr.setValue(compValue);
										 logger.debug(PC2LogCategory.STUN, subCat,
												 "ErrorCodeAttribute now contains " + attr);
									 }
								 }

							 }
							 else if (modType.equals("delete")) {
								 // Deletion is supported for the entire Attribute only 
								 if (param == null) {
									 sm.removeAttribute(StunConstants.ERROR_CODE_TYPE);
									 logger.debug(PC2LogCategory.STUN, subCat,
									 "Removed ErrorCodeAttribute from message ");
								 }
							 }
						 }
						 else if (modType.equals("add")) {
							 if (element.getRef() instanceof Literal) {
								 int errorCode = Integer.parseInt(((Literal)element.getRef()).toString());
								 ErrorCode ec = new ErrorCode(null);
								 if (ec != null) {
									 ec.setValue(errorCode);
									 sm.addAttribute(ec);
									 logger.debug(PC2LogCategory.STUN, subCat,
											 "Adding ErrorCodeAttribute to message." + attr);
								 }
							 }
						 }
						 else {
							 logger.error(PC2LogCategory.STUN, subCat,
									 "StunDistributor could not modify " + hdr + " attribute because it is not in the message.");
						 }
					 }
					 // XorMappedAddress modifications
					 else if (hdr.equals("Priority")) {
						 MsgEvent event = MsgQueue.getInstance().get(curMsgIndex);
						 String priority = refLocator.getReferenceInfo(fsmUID, element.getRef(), event);
						 if (priority != null) {
							 Priority p = (Priority)sm.getAttribute(StunConstants.PRIORITY);
							 if (modType.equals("replace")) {
								 if (p != null)
									 p.setValue(priority.getBytes());
								 else {
									 // Add the attribute if it doesn't exist
									 p = new Priority(priority.getBytes());
									 sm.addAttribute(p);
								 }
							 }
							 else if (modType.equals("add")) {
								 // Add the attribute if it doesn't exist
								 p = new Priority(priority.getBytes());
								 sm.addAttribute(p);
							 }
							 else if (modType.equals("delete")) {
								sm.removeAttribute(StunConstants.PRIORITY_TYPE);
								
							 }
						 }
						 else {
							 logger.error(PC2LogCategory.STUN, subCat,
									 "StunDistributor could not modify " + hdr 
									 + " attribute because the value to use for the attribute could not be found.");
						 }
					 }
					 else if (hdr.equals("IceControlling")) {
						 MsgEvent event = MsgQueue.getInstance().get(curMsgIndex);
						 String iceControlling = refLocator.getReferenceInfo(fsmUID, element.getRef(), event);
						 if (iceControlling != null) {
							 IceControlling ic = (IceControlling)sm.getAttribute(StunConstants.ICE_CONTROLLING);
							 if (modType.equals("replace")) {
								 if (ic != null)
									 ic.setValue(iceControlling.getBytes());
								 else {
									 // Add the attribute if it doesn't exist
									 ic = new IceControlling(iceControlling.getBytes());
									 sm.addAttribute(ic);
								 }
							 }
							 else if (modType.equals("add")) {
								 // Add the attribute if it doesn't exist
								 ic = new IceControlling(iceControlling.getBytes());
								 sm.addAttribute(ic);
							 }
							 else if (modType.equals("delete")) {
								sm.removeAttribute(StunConstants.ICE_CONTROLLING_TYPE);
								
							 }
						 }
						 else {
							 logger.error(PC2LogCategory.STUN, subCat,
									 "StunDistributor could not modify " + hdr 
									 + " attribute because the value to use for the attribute could not be found.");
						 }
					 }
					 else if (hdr.equals("FingerPrint")) {
						 FingerPrint fp = (FingerPrint)sm.getAttribute(StunConstants.FINGERPRINT_TYPE);
						 if (fp != null) {
							 if (modType.equals("replace") && param != null) {
								 if (element.getRef() instanceof Literal) {
									 byte [] arg = ((Literal)element.getRef()).getExpr().getBytes();
									 sm.removeAttribute(StunConstants.FINGERPRINT_TYPE);
									 fp = new FingerPrint(arg);
									 sm.addAttribute(fp);
									 logger.debug(PC2LogCategory.STUN, subCat,
											 "Modifying FingerPrint's to value=["
											 + Conversion.hexString(arg) + "].");

									 recalcFingerPrint = false;
								 }
								 else if (element.getRef() instanceof StunRef) {
									 StunRef sr = (StunRef)element.getRef();
									 int uid = sr.getUID();
									 String key = sr.getMethod();
									 String instance = sr.getMsgInstance();
									 StunMsg msg =  (StunMsg)PCSim2.getMsgQueue().find(uid, key, 
											 instance, curMsgIndex);
									 if (msg != null) {
										 FingerPrint origAttr = (FingerPrint)msg.getMessage().getAttribute(StunConstants.FINGERPRINT_TYPE);
										 sm.removeAttribute(StunConstants.FINGERPRINT_TYPE);
										 sm.addAttribute(origAttr);
										 logger.debug(PC2LogCategory.STUN, subCat,
												 "Modifying FingerPrint's hash parameter using previous message value=[" 
												 + Conversion.hexString(origAttr.getValue()) + "].");
										 recalcFingerPrint = false;
									 }
								 }
							 }
							 else if (modType.equals("delete"))  {
								 // Deletion is supported for the entire Attribute only 
								 if (param == null) {
									 sm.removeAttribute(StunConstants.FINGERPRINT_TYPE);
									 logger.debug(PC2LogCategory.STUN, subCat,
									 "Removing FingerPrintAttribute from message.");
									 recalcFingerPrint = false;
								 }
							 }
						 }
						 else if (modType.equals("add")) {
							 logger.debug(PC2LogCategory.STUN, subCat,
							 "Adding FingerPrintAttribute to message.");
							 recalcFingerPrint = true;
						 }
						 else {
							 logger.error(PC2LogCategory.STUN, subCat,
									 "StunDistributor could not modify " + hdr + " attribute because it is not in the message.");
						 }
					 }
					 else if (hdr.equals("MessageIntegrity")) {
						 MessageIntegrity mi = (MessageIntegrity)sm.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
						 String secretKey = SystemSettings.getSettings("DUT").getProperty(SettingConstants.PASSWORD);
						 if (mi != null && secretKey != null) {
							 if (modType.equals("replace") && param != null) {
								 if (element.getRef() instanceof Literal) {
									 byte [] arg = ((Literal)element.getRef()).getExpr().getBytes();
									 sm.removeAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
									 mi = new MessageIntegrity(arg);
									 sm.addAttribute(mi);
									 logger.debug(PC2LogCategory.STUN, subCat,
											 "Modifying MessageIntegrity's hash parameter using literal value=["
											 + Conversion.hexString(arg) + "].");
									 recalcFingerPrint = true;

								 }
								 else if (element.getRef() instanceof StunRef) {
									 StunRef sr = (StunRef)element.getRef();
									 int uid = sr.getUID();
									 String key = sr.getMethod();
									 String instance = sr.getMsgInstance();
									 StunMsg msg =  (StunMsg)PCSim2.getMsgQueue().find(uid, key, 
											 instance, curMsgIndex);
									 if (msg != null) {
										 mi = (MessageIntegrity)msg.getMessage().getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
										 sm.addAttribute(mi);
										 logger.debug(PC2LogCategory.STUN, subCat,
												 "Modifying MessageIntegrity's hash parameter using previous message value=["
												 + Conversion.hexString(mi.getValue()) + "].");
										 recalcFingerPrint = true;

									 }
								 }
							 }
							 else if (modType.equals("delete"))  {
								 // Deletion is supported for the entire Attribute only 
								 if (param == null) {
									 sm.removeAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
									 recalcFingerPrint = true;
								 }
							 }

						 }
						 else if (modType.equals("add")) {
							 try {
								 Username u = (Username)sm.getAttribute(StunConstants.USERNAME_TYPE);
								 Realm r = (Realm)sm.getAttribute(StunConstants.REALM_TYPE);
								 if (u != null && r != null) {
									 Properties p = SystemSettings.getPropertiesByValue(SettingConstants.USER_NAME, u.getUsername());
									 if (p != null) {
										 String password = p.getProperty(SettingConstants.PASSWORD);
										 if (password != null) {
											 MessageIntegrity newMI = new MessageIntegrity(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
											 sm.addAttribute(newMI);
											 newMI.calculate(sm, u.getUsername(), r.getRealm(), password);
											 recalcFingerPrint = true;
										 }



									 }
								 }
							 }
							 catch (Exception e) {
								 logger.warn(PC2LogCategory.STUN, subCat,
										 "StunDistributor was unable to add the MessageIntegriy's hash parameter.", e);
							 }
						 }
						 else {
							 logger.error(PC2LogCategory.STUN, subCat,
									 "StunDistributor could not modify " + hdr + " attribute because it is not in the message.");
						 }
					 }
					 else if (hdr.charAt(0) == '0' && hdr.charAt(1) == 'x' && hdr.length() == 6) {
						 byte [] type = convertToHex(hdr.substring(2,6));
						 if (type != null && type.length == 2) {
							 logger.debug(PC2LogCategory.STUN, subCat,
									 "Creating Attribute of type" + Conversion.hexString(type));
							 Reference ref = element.getRef();
							 if (modType.equals("add") && ref != null &&
									 ref instanceof Literal) {
								 String value = ((Literal)ref).getExpr();
								 if (value != null && value.charAt(0) == '0' &&
										 value.charAt(1) == 'x') {
									 byte [] attrValue = convertToHex(value.substring(2,value.length()));
									 if (attrValue != null) {
										 StunAttribute attr = new StunAttribute(type, attrValue);
										 sm.addAttribute(attr);
									 }
								 }
							 }
							 else if (modType.equals("delete")) {
								 Character c = Conversion.getChar(type, 0);
								 sm.removeAttribute(c);
								 logger.debug(PC2LogCategory.STUN, subCat,
										 "Deleting Attribute of type[" + Conversion.hexString(type) 
										 + "].");
							 }
						 }

					 }
				 }
			 }
			 MessageIntegrity mi = (MessageIntegrity)sm.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
			 FingerPrint fp = (FingerPrint)sm.getAttribute(StunConstants.FINGERPRINT_TYPE);
			 if (recalcMessageIntegrity && mi != null) {
				 Properties orig = SystemSettings.getSettings(s.getOriginator());
				 if (orig != null) {
					 String password = orig.getProperty(SettingConstants.STUN_PASSWORD_CREDENTIAL);
					 if (password != null) {
						 if (fp != null)
							 sm.removeAttribute(StunConstants.FINGERPRINT_TYPE);
						 sm.removeAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
						 mi = new MessageIntegrity(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
						 if (mi != null) {
							 sm.addAttribute(mi);
							 mi.calculate(sm, password);
						 }

					 }
					 else {
						 logger.error(PC2LogCategory.STUN, subCat,
								 "StunDistributor could not find the " + SettingConstants.STUN_PASSWORD_CREDENTIAL 
								 + " setting for network element label " + s.getOriginator() + "."); 
					 }
				 }
				 else {
					 logger.error(PC2LogCategory.STUN, subCat,
							 "StunDistributor could not recalculate the MESSAGE-INTEGRITY attribute "
							 + "because it couldn't identify the originator of the message."); 
				 }
			 }
			 if (recalcFingerPrint && fp != null) {
				 try {
					 sm.removeAttribute(StunConstants.FINGERPRINT_TYPE);
					 fp = new FingerPrint(StunConstants.STUN_EMPTY_FINGER_PRINT);
					 if (fp != null) {
						 sm.addAttribute(fp);
						 fp.calculate(sm);
					 }
				 }
				 catch (Exception e) {
					 logger.warn(PC2LogCategory.STUN, subCat,
							 "StunDistributor could not properly recalculate fingerprint.", e);
				 }
			 }
		 }
	 }

	 /**
	  * This method converts a string into a hex value by constructing a single
	  * hex value from two consecutive characters in the string.
	  * 
	  * @param str - the string to convert
	  * @return - the byte array for the string value or null.
	  */
	 private byte [] convertToHex(String str) {
		 byte [] result = null;
		 if ((str.length() % 2) == 0) {
			 result = new byte[str.length()/2];
			 for (int i = 0; i < result.length; i++) {
				 Byte u = charAsByte(str.charAt(i*2));
				 Byte l = charAsByte(str.charAt(i*2+1));
				 if (u != null && l != null) {
					 result[i] = (byte)((u<<4) | (l&0xF));
				 }
				 else {
					 result = null;
					 break;
				 }
			 }
		 }
		 return result;
	 }

	 private Byte charAsByte(char c) {
		 switch (c) {
		 case '1' :
			 return new Byte((byte)0x1);
		 case '2' :
			 return new Byte((byte)0x2);
		 case '3' :
			 return new Byte((byte)0x3);
		 case '4' :
			 return new Byte((byte)0x4);
		 case '5' :
			 return new Byte((byte)0x5);
		 case '6' :
			 return new Byte((byte)0x6);
		 case '7' :
			 return new Byte((byte)0x7);
		 case '8' :
			 return new Byte((byte)0x8);
		 case '9' :
			 return new Byte((byte)0x9);
		 case '0' :
			 return new Byte((byte)0x0);
		 case 'a' :
		 case 'A' :
			 return new Byte((byte)0xa);
		 case 'b' :
		 case 'B' :
			 return new Byte((byte)0xb);
		 case 'c' :
		 case 'C' :
			 return new Byte((byte)0xc);
		 case 'd' :
		 case 'D' :
			 return new Byte((byte)0xd);
		 case 'e' :
		 case 'E' :
			 return new Byte((byte)0xe);
		 case 'f' :
		 case 'F' :
			 return new Byte((byte)0xf);
		 default :
			 return null;

		 }
	 }

//	 private byte[] intToByteArray(int value) {
//	 byte[] byteArray = new byte[2];
//	 byteArray[0] = (byte)((value & 0x0000FF00)>>>8);
//	 byteArray[1] = (byte)((value & 0x000000FF));
//	 return (byteArray);
//	 }

	 public Thread stream(int fsmUID, Stream s, NetworkElements nes) {
		 if (stunStack != null) {
			 if (s.isStop()) {
				 Stream stream = streams.get(s.getName());
				 if (stream != null) {
					 stream.stop();
					 //stream.getThread().interrupt();
				 }
			 }
			 else {
				 try {
					 s.resolve(fsmUID);
					 if (s.getSrcIP().equals(myIP) || s.getSrcIP().equals(myIP2)) {
						 int port = s.getSrcPort();
						 if (rtpPort1 == port ||
								 rtpPort2 == port) {
							 InetSocketAddress localAddr = 
								 new InetSocketAddress(s.getSrcIP(), port);
							 int id = stunStack.getProcessorID(localAddr);
							 if (id != -1) {
								 s.setStack(stunStack);
								 s.setProcessorID(id);
								 streams.put(s.getName(), s);
								 s.start();
								 logger.info(PC2LogCategory.STUN, subCat,
										 "The platform has started to send the RTP information from file[" 
										 + s.getFile().getName() + "]  to IP=" + s.getDestIP() + "|" + s.getDestPort() 
										 + " from IP="
										 + s.getSrcIP() + "|" + port);
								 return s.getThread();
							 }
							 else {
								 logger.error(PC2LogCategory.STUN, subCat,
										 "The platform could not locate the processor for IP address=" 
										 + s.getSrcIP() + " and port=" + port  
										 + " Failed to start streaming file.");
							 }
						 }
						 else {
							 logger.error(PC2LogCategory.STUN, subCat,
									 "The stream action's from IP port doesn't match the Platform setting for 'STUN RTPPort1' or 'STUN RTPPort2" 
									 + " Failed to start streaming file.");
						 }
					 }
					 else {
						 logger.error(PC2LogCategory.STUN, subCat,
								 "The stream action's from IP address doesn't match the Platform setting for 'STUN IP Address'" 
								 + " Failed to start streaming file.");
					 }
				 }
				 catch (Exception pe) {
					 logger.warn(PC2LogCategory.STUN, subCat,
							 "StunDistributor failed to start streaming file." + 
							 fsmUID + ".\n" + pe.getMessage() + "\n" + pe.getStackTrace());
				 }
			 }
		 }
		 else {
			 logger.error(PC2LogCategory.STUN, subCat,
					 "The couldn't perform start_stream or stop_stream because the stack is null." 
					 + " Failed stream action.");
		 }
		 return null;
	 }

	 /**
	  * Notifier for the STUN stack to be shutdown.
	  */
	 public void shutdown() {
		 if (turnStack != null) 
			 turnStack.stop();
		 if (stunStack != null)
			 stunStack.stop();
	 }
	 
	 @Override
	public void timerTick() {
		 if (rtpDelivered != null) {
			 try { 
				 Enumeration<String> keys = rtpDelivered.keys();
				 int count = rtpDelivered.size();
				 long curTime = System.currentTimeMillis();
				 logger.debug(PC2LogCategory.SIP, "RTP",
						 "RTP Distributor reviewing " + count + " RTP stream(s) for continued packets.");
				 while (keys.hasMoreElements()) {
					 String key = keys.nextElement();
					 RTPData rd = rtpDelivered.get(key);
					 if (!rd.isComplete()) { 
						 // We need to allow for 1 second of silence suppression
						 // for G.711. Change the comparison from 500 to 1500
						 // PCPCIIATP-51
						 if (rd.getLastTime() < (curTime - 1500)) {
							 float totalTime = (rd.getLastTime() - rd.getInitialTime())/(float)1000.0;
							 logger.info(PC2LogCategory.LOG_MSG, "RTP",
									 " USER: \n" +
									 "RTP Packets have stopped from IP address(" 
									 + rd.getSrcIP() + "|" + rd.getSrcPort() 
									 + ").\nTotal time=" 
									 + totalTime + " seconds." // last=" + rd.getLastTime() 
									 //+ " init=" + rd.getInitialTime()
									 );
							FSMListener listener = rd.getFSM();
							InternalMsg msg = new InternalMsg(listener.getFsmUID(), 
									System.currentTimeMillis(), 
									LogAPI.getSequencer(), EventConstants.MEDIA_COMPLETE);
							listener.processEvent(msg);
							rd.setComplete();
							rtpDelivered.remove(rd);
							logger.debug(PC2LogCategory.SIP, "RTP",
									 "RTP Distributor removing RTP Data for key" + key + ". Table contains " + rtpDelivered.size() + " stream(s).");
						 }
					 }
				 }
			 }
			 catch (Exception e) {
				 logger.warn(PC2LogCategory.SIP, "RTP", "StunDistributor RTP monitor processing failed.");
			 }
		 } 
	 }
}
