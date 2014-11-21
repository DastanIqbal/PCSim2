/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun;

import java.net.*;
import java.io.*;
//import java.util.*;
import com.cablelabs.common.*;
import com.cablelabs.log.*;
import com.cablelabs.stun.attributes.*;

public class StunServer implements Runnable, StunListener {

	String localIP =  "10.4.10.150"; // "10.4.1.10"; //"192.168.1.2"; // "10.4.10.102"; // "10.253.129.9";
	String remoteIP = "10.4.10.150"; //"10.4.1.10"; //"192.168.1.2"; // "10.4.10.102"; // "10.253.129.9";
	int localPort = 3478;
	int remotePort = 3479;
	//String serverIP = "fc00:504:700:0:8502:ba23:17be:61b8";
	//String clientIP = "2001:504:712:2:0:0:0:113";
	InetSocketAddress localAddr = null;
	InetSocketAddress remoteAddr = null;
	private static LogAPI logger = LogAPI.getInstance();
	
	private StunFactory factory = null;
	private static StunStack stack = null;
	private static String subCat = "Server";
	
	private String username = "UE0";
	private String realm = "pclab.com";
	//private String password = "UE0passwd";
	String password = "VOkJxbRl1RmTxUk/WvJxBt";
	public boolean isRunning = false;
	private Thread thread = null;
	
	private TurnStack turn = null;
	
	private boolean addFingerPrint = true;
	
	public StunServer() throws IOException {
		LogAPI.setConsoleCreated();
		factory = new StunFactory();	
		stack = StunStack.getInstance();
		stack.setListener(this);
		stack.start();
		stack.createProcessor(localIP, localPort, null);
		localAddr = new InetSocketAddress (localIP, localPort);
		remoteAddr = new InetSocketAddress(remoteIP, remotePort);
		
		turn = TurnStack.getInstance(localIP, 30000, 10);
		turn.start();
	}
	
	private void allocateRequest(StunEvent se, char type) {
		StunMessage event = se.getEvent();
		Username u = (Username)event.getAttribute(StunConstants.USERNAME_TYPE);
		MessageIntegrity mi = (MessageIntegrity)event.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
		if (u != null) {
			// Since we have already passed validation of MessageIntegrity.
			// If it is present, we have a valid user, realm and password.
			StunMessage resp = null;
			String key = getKey(se.getRawData());
			Allocation a = turn.getAllocation(key);
			if (a == null) {
				Allocation da = turn.isDeletedAllocation(key);
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
								//	int id = -1;
									Object source = se.getRawData().getSource();
									if (source instanceof StunMessageProcessor) {
										StunMessageProcessor smp = (StunMessageProcessor)source;
										// stack.getProcessor(msg.getID());
										if (smp != null) {
											a = turn.createAllocation(smp, key, user, se, rp.getProp(), Transport.UDP); 
											if (a != null) {
												Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
												resp = factory.createAllocateResponse(Transport.UDP, true, Allocation.TTL,
														true, false, user, realm, password, n, addFingerPrint);
											}

										}
//										else {
//											logger.warn(PC2LogCategory.STUN, subCat,
//											"STUN Stack could locate an StunMessageProcessor to allocate TURN port");
//										}
									}
								}
								else {
									// TODO resp = factory.createAllocateErrorResponse(msg, 508, realm);
								}
									
							}
							else {
								ReservationToken rToken = 
									(ReservationToken)event.getAttribute(StunConstants.RESERVATION_TOKEN_TYPE);
								if (rToken != null) {
									String user = new String(u.getValue());
									Object source = se.getRawData().getSource();
									if (source instanceof StunMessageProcessor) {
										StunMessageProcessor smp = (StunMessageProcessor)source;

										if (smp != null) {
											a = turn.createAllocation(smp, key, user, se, null, Transport.UDP); 
											if (a != null) {
												Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
												resp = factory.createAllocateResponse(Transport.UDP, false, Allocation.TTL,
														false, true, user, realm, password, n, addFingerPrint);
											}
										}
									}
								}
							}
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
					stack.sendMessage(
							((StunMessageProcessor)source).getID(),
							resp, seq, remoteAddr);
			}
			logger.debug(PC2LogCategory.STUN, subCat,
					"Server sending automatic TURN Response :\n" + resp);
		}
	}
	
	private void bindingRequest(StunEvent se, char type) {
		StunMessage event = se.getEvent();
		Username u = (Username)event.getAttribute(StunConstants.USERNAME_TYPE);
		StunMessage resp = null;
		if (u == null) 
			resp = factory.createBindingErrorResponse(se, 401, realm);
		else {
			MessageIntegrity mi = (MessageIntegrity)event.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
			Realm r = (Realm)event.getAttribute(StunConstants.REALM_TYPE);
			Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
			String userVal = new String(u.getValue());
			if (mi == null ||
					r == null ||
					n == null) {
				resp = factory.createBindingErrorResponse(se, 400, null);
			}

			if (userVal != null && userVal.equals(username)) {
				resp = factory.createBindingResponse(se, username, realm, password, n, addFingerPrint);
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
				stack.sendMessage(
					((StunMessageProcessor)source).getID(), 
					resp, seq, remoteAddr);
		}
		logger.debug(PC2LogCategory.STUN, subCat,
				"Server sending automatic STUN Response :\n" + resp);
	}
	
	private void channelBindRequest(StunEvent se, char type) {
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
					Allocation a = turn.addChannelBind(key, pa, cn);
					if (a != null) {
						resp = factory.createChannelBindResponse(event, username, realm, password, 
								Allocation.TTL*2, addFingerPrint);
//						Binding ba = a.getBinding(pa.getAddress());
//						Binding ca = a.getBinding(cn.getChannel());
//						if (ba == ca) 
//							logger.info(PC2LogCategory.STUN, subCat, "Bindings match.");
						
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
			if (source instanceof StunMessageProcessor)
				stack.sendMessage(
						((StunMessageProcessor)source).getID(), 
						resp, seq, remoteAddr);
		}
		logger.debug(PC2LogCategory.STUN, subCat,
				"Server sending automatic STUN Response :\n" + resp);
	}
	private String getKey(RawData rawData) {
		String key = rawData.getSrcIP()
			+ "|" + rawData.getSrcPort() + "|"
			+ rawData.getDestIP() 
			+ "|" + rawData.getDestPort() + "|" 
			+ rawData.getTransport();
		return key;
	}
	

	public void processEvent(StunEvent se) {
		StunMessage event = se.getEvent();
		String typeName = event.getName();
		if (typeName != null) {
			if (validateFingerPrint(event)) {
				try {
					char type = event.getMessageType();

					// Verify the message integrity, if present
					if (validateMessageIntegrity(event)) {
						// Next determine if the response is an error 
						if (StunConstants.isErrorResponse(type)) {
							processErrorResponse(se, type);
						}
						else if (StunConstants.isResponse(type)) {
							processResponse(se, type);
						}
						else if (StunConstants.isIndication(type)) {
							processIndication(se, type);
						}
						else if (StunConstants.isRequest(type)) {
							processRequest(se, type);
						}
					}
				}
				catch (Exception e) {
					logger.warn(PC2LogCategory.STUN, subCat,
							e.getMessage(), e);
				}
			}

		}
	}
	
	public void processChannelDataEvent(RawData rawData) {
		String key = getKey(rawData);
		Allocation a = turn.getAllocation(key);
		if (a != null) {
			Object event = rawData.getData();
			if (event instanceof ByteArray) {
				byte [] chanData = ((ByteArray)event).getBuffer();
				Character channel = Conversion.getChar(chanData, 0);
				// (char)(((chanData[0]<<8) & 0xFF00) | (chanData[1]&0xFF));
				Binding b = a.getBinding(channel);
				if (b != null) {
					try {
						InetAddress destAddr = InetAddress.getByAddress(b.pa.getAddress());

						int destPort = b.pa.getPort();
						InetSocketAddress remoteAddr = new InetSocketAddress(destAddr, destPort);
						int seq = LogAPI.getSequencer();
						byte [] data = new byte [chanData.length-4];
						System.arraycopy(chanData,4,data, 0, chanData.length-4);

						a.sendRawMessage(data, 
								data.length, seq, remoteAddr);
					}
					catch (UnknownHostException uhe) {
						logger.warn(PC2LogCategory.STUN, subCat,
								"STUN Server could not convey Channel Data message because the peerAddress["
								+ Conversion.hexString(b.pa.getAddress()) + "] could not be found.");

					}
					catch (IOException ioe) {
						logger.warn(PC2LogCategory.STUN, subCat,
								"STUN Server could not convey Channel Data message because an error "
								+ "occurred during the write operation.");

					}
				}
			}
		}
	}

	private void processErrorResponse(StunEvent se, char type) {
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
	
	private void processIndication(StunEvent se, char type) {
		switch (type) {
		case StunConstants.SEND_INDICATION_MSG_TYPE :
			sendIndication(se,type);
			break;
		case StunConstants.DATA_INDICATION_MSG_TYPE :
			logger.error(PC2LogCategory.STUN, subCat, 
					"STUN Server received a DATA Indication from a client, dropping message.");
			break;
		}
	}
	
	private void processRequest(StunEvent se, char type) {
		switch (type) {
		case StunConstants.ALLOCATE_REQUEST_MSG_TYPE :
			allocateRequest(se, type);
			break;
		case StunConstants.BINDING_REQUEST_MSG_TYPE :
			bindingRequest(se, type);
			break;
		case StunConstants.CHANNEL_BIND_REQUEST_MSG_TYPE:
			channelBindRequest(se, type);
			break;
		case StunConstants.REFRESH_REQUEST_MSG_TYPE :
			refreshRequest(se, type);
			break;
		}

	}
	private void processResponse(StunEvent se, char type) {
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
	
	public void refreshRequest(StunEvent se, char type) {
		StunMessage event = se.getEvent();
		Lifetime l = (Lifetime)event.getAttribute(StunConstants.LIFETIME_TYPE);
		if (l != null) {
			StunMessage resp = null;
			Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
			int ttl = Conversion.byteArrayToInt(l.getValue());
			String key = getKey(se.getRawData());
			if (ttl > 0) {
				
				Allocation a = turn.getAllocation(key);
				if (a != null) {
					a.refresh();
					resp = factory.createRefreshResponse(username, realm, password, 
							n, Allocation.TTL, addFingerPrint);
				}
			}
			else if (ttl == 0) {
				turn.deleteAllocation(key);
				resp = factory.createRefreshResponse(username, realm, password, n, 0, addFingerPrint);
			}

			if (resp != null) {
				RawData rd = se.getRawData();
				InetSocketAddress remoteAddr = new InetSocketAddress(rd.getSrcIP(), rd.getSrcPort());
				int seq = LogAPI.getSequencer();
				Object source = rd.getSource();
				if (source instanceof StunMessageProcessor)
					stack.sendMessage(
							((StunMessageProcessor)source).getID(), resp, seq, remoteAddr);
			}
			logger.debug(PC2LogCategory.STUN, subCat,
					"Server sending automatic STUN Response :\n" + resp);
		}
	}
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ie) {
				return;
			}
		}

	}
	public void start()	throws IOException	{
		try {
				this.isRunning = true;
				thread = new Thread(this, subCat);
				thread.setDaemon(true);
				thread.start();
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.STUN, subCat,
						"Client encountered an error when starting.", ex);
			}
	}
	
	private void sendIndication(StunEvent se, char Type) {
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
					turn.sendRawMessage(key, data, data.length, seq, remote );
				}
				catch (UnknownHostException uhe) {
					logger.error(PC2LogCategory.STUN, subCat, 
							"STUN Stack could not forward Send Indication to TURN Stack because bad peer address[" 
							+ pa + "].");
				}
			}
			else {
				Allocation a = turn.getAllocation(key);
				if (a != null)
					a.addPermission(pa);
			}
		}
		
	}
	
	/**
	 * This method will return true if the message integrity field is present and it matches
	 * the calculated value or in the case when no message integrity field is present. 
	 * 
	 * @param event
	 * 
	 * @return
	 */
	private boolean validateMessageIntegrity(StunMessage event) {
		// First we need to remove the finger print attribute if it is present
		boolean result = false;
		FingerPrint fp = (FingerPrint)event.getAttribute(StunConstants.FINGERPRINT_TYPE);
		 if (fp != null) 
			 event.removeAttribute(StunConstants.FINGERPRINT_TYPE);
		 MessageIntegrity mi = (MessageIntegrity)event.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
		if (mi != null) {
//			Username u = (Username)event.getAttribute(StunConstants.USERNAME_TYPE);
//			Realm r = (Realm)event.getAttribute(StunConstants.REALM_TYPE);
//			Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
			event.removeAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
			MessageIntegrity localMI = new MessageIntegrity(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
			localMI.calculate(event, password);
			byte [] eventMI = mi.getValue(); 
			byte [] localCalc = localMI.getValue();
			if (java.util.Arrays.equals(eventMI, localCalc)) {
				result = true;
			}
			else {
					 logger.warn(PC2LogCategory.STUN, subCat,
							 "Client failing MessageIntegrity because event containted\neventMI=" 
							 + Conversion.hexString(eventMI) + "\n calcMI=" 
							 + Conversion.hexString(localCalc));
				 }
		}
		else
			result = true;
		
		// Whether it passed or failed add the message integrity back to the message
		if (mi != null) {
			event.addAttribute(mi);
		}
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
//			System.arraycopy(eventFP, 0, temp, 0, eventFP.length);
//			fp.setValue(StunConstants.STUN_EMPTY_FINGER_PRINT);
//			fp.calculate(event);
//			byte [] calcFP = fp.getValue();
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
	
	public StunEvent parse(byte [] msg) {
		ByteArray ba = new ByteArray(msg, msg.length);
		RawData rawData = new RawData(this,
				 ba, msg.length, 
				 localIP, 
				 0, 
				 localIP, 
				 0, Transport.UDP,
				 LogAPI.getSequencer());
		StunEvent se = stack.parse(rawData, msg, msg.length);
		return se;
	}
	public static void main(String[] args) {
		try {
			StunServer server = new StunServer();
			server.start();
						
			byte [] msg = {		
					(byte)0x01, (byte)0x01, (byte)0x00, (byte)0x38, (byte)0x21, (byte)0x12, (byte)0xa4, (byte)0x42, (byte)0xfd, (byte)0xa1, (byte)0xf9, (byte)0xc9, (byte)0x1e, (byte)0x3b, (byte)0xc7, (byte)0x15,
					(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x69, (byte)0x78, (byte)0x0a, (byte)0x01, (byte)0x00, (byte)0x37,
					(byte)0x00, (byte)0x20, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x48, (byte)0x6a, (byte)0x2b, (byte)0x13, (byte)0xa4, (byte)0x75, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x14,
					(byte)0xd1, (byte)0x9f, (byte)0xef, (byte)0x17, (byte)0xf1, (byte)0xd4, (byte)0x12, (byte)0xaf, (byte)0x39, (byte)0xdd, (byte)0xd9, (byte)0x80, (byte)0xbc, (byte)0xc5, (byte)0xb3, (byte)0x84,
					(byte)0x07, (byte)0x6f, (byte)0xc6, (byte)0x9d, (byte)0x80, (byte)0x28, (byte)0x00, (byte)0x04, (byte)0xb5, (byte)0xea, (byte)0x0e, (byte)0x6f };
			StunEvent se = server.parse(msg);
			if (se != null) {
				System.out.println("Parse complete.");
			}
			while (server.isRunning) {
				Thread.sleep(1000);
			}
		}
		catch (Exception ie) {
			handleException(ie);
		}
	}
	
	static public void handleException(Exception e)
	{
		System.out.println(e.toString());
		e.printStackTrace();

	}
	
}
