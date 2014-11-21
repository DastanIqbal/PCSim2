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

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Arrays;
import com.cablelabs.common.*;
import com.cablelabs.log.*;
import com.cablelabs.stun.attributes.*;

public class StunClient implements Runnable, StunListener {
	String localIP = "10.4.10.150"; //"192.168.1.2"; // "10.4.10.102"; // "10.253.129.9";
	String remoteIP = "10.4.10.150"; //"192.168.1.2"; // "10.4.10.102"; // "10.253.129.9";
	//String serverIP = "fc00:504:700:0:8502:ba23:17be:61b8";
	//String clientIP = "2001:504:712:2:0:0:0:113";
	int localPort = 3479;
	int remotePort = 3478;
	InetSocketAddress localAddr = null;
	InetSocketAddress remoteAddr = null;
	private static LogAPI logger = LogAPI.getInstance();
	
	private StunFactory factory = null;
	private static StunStack stack = null;
	private static String subCat = "Client";
//	private boolean authenticate = true;
	
	private String username = "UE0";
	private String realm = "pclab.com";
	private String password = "UE0passwd";
	private ConcurrentLinkedQueue<StunEvent> queue = new ConcurrentLinkedQueue<StunEvent>();
	public boolean isRunning = false;
	private Thread thread = null;
	private Character myChannel = 0x7210;
	private boolean addFingerPrint = true;
	
	public StunClient() throws IOException {
		LogAPI.setConsoleCreated();
		factory = new StunFactory();	
		stack = StunStack.getInstance();
		stack.setListener(this);
		stack.start();
		String threadName = "STUN Client - UDP/" + localIP + "|" + localPort;
		stack.createProcessor(localIP, localPort, threadName );
		localAddr = new InetSocketAddress (localIP, localPort);
		remoteAddr = new InetSocketAddress(remoteIP, remotePort);
	}
	
	private void dataIndication(StunEvent se, char type) {
		StunMessage event = se.getEvent();
		PeerAddress pa = (PeerAddress)event.getAttribute(StunConstants.PEER_ADDRESS_TYPE);
		Data d = (Data)event.getAttribute(StunConstants.DATA_TYPE);
		if (pa != null && d != null) {
			StunMessage req = factory.createSendIndication(pa, d, addFingerPrint);

			if (req != null) {
				int seq = LogAPI.getSequencer();
				int processorID = stack.getProcessorID(localAddr);
				stack.sendMessage(processorID, req, seq, remoteAddr);
			}
			logger.debug(PC2LogCategory.STUN, subCat,
					"Client sending :\n" + req);

		}
	}
	
	public void processChannelDataEvent(RawData rawData) {
		Object event = rawData.getData();
		if (event instanceof ByteArray) {
			byte [] data = ((ByteArray)event).getBuffer();
			Character channel = Conversion.getChar(data, 0);
			// (char)(((data[0]<<8) & 0xFF00) | (data[1]&0xFF));
			if (channel.equals(myChannel)) {
				logger.debug(PC2LogCategory.STUN, subCat,
				"Client received valid ChannelData event.");
				int seq = LogAPI.getSequencer();
				int processorID = stack.getProcessorID(localAddr);
				stack.sendRawMessage(processorID, data, data.length,
						seq, remoteAddr, subCat);
			}
		}
	}

	public void processEvent(StunEvent se) {
		StunMessage event = se.getEvent();
		String typeName = event.getName();
		logger.debug(PC2LogCategory.STUN, subCat,
				"Client rcvd a " + typeName + " message\n[" 
				+ event + "]");
		// As a sanity check make sure there is a message type (method)
		if (typeName != null) {
			try {
				//StunMessage event = msg.getEvent();
				if (validateFingerPrint(event)) {
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
			}
			catch (Exception e) {
				logger.warn(PC2LogCategory.STUN, subCat,
						e.getMessage(), e);
			}
		}
	}
	private void processErrorResponse(StunEvent se, char type) {
		StunMessage event = se.getEvent();
		ErrorCode ec = (ErrorCode)event.getAttribute(StunConstants.ERROR_CODE_TYPE);
		int statusCode = ec.getStatusCode();
		if (statusCode == 401) {
			StunMessage req = factory.createBindingRequest(username, realm, 
					password, null, addFingerPrint);
			if (req != null) {
				int seq = LogAPI.getSequencer();
				int processorID = stack.getProcessorID(localAddr);
				stack.sendMessage(processorID, req, seq, remoteAddr);
			}
			logger.debug(PC2LogCategory.STUN, subCat,
					"Client sending :\n" + req);


		}
	}
	
	private void processIndication(StunEvent se, char type) {
		switch (type) {
		case StunConstants.SEND_INDICATION_MSG_TYPE :
			break;
		case StunConstants.DATA_INDICATION_MSG_TYPE :
			dataIndication(se, type);
			break;
		}
	}
	
	private void processRequest(StunEvent se, char type) {
		
	}
	
	private void processResponse(StunEvent se, char type) {
		StunMessage event = se.getEvent();
		if (type == StunConstants.BINDING_RESPONSE_MSG_TYPE) {
			XorMappedAddress xma = (XorMappedAddress)event.getAttribute(StunConstants.XOR_MAPPED_ADDRESS_TYPE);
			if (xma != null) {
				try {
					String addr = InetAddress.getByAddress(xma.getAddress()).toString();
					if (addr.charAt(0) == '/')
						addr = addr.replace("/", "");
					if (addr.equals(localIP)) {
						logger.debug(PC2LogCategory.STUN, subCat,
								"Client validated XorMappedAddress' IP Address.");
					}
					else 
						logger.debug(PC2LogCategory.STUN, subCat,
						"The XorMappedAddress' IP Address received by the Client is invalid.");
				}
				catch (UnknownHostException uhe)  {
					logger.debug(PC2LogCategory.STUN, subCat,
					"The XorMappedAddress' IP Address received by the Client is invalid.");
				}
				int port = xma.getPort();
				if (port == localPort) {
					logger.debug(PC2LogCategory.STUN, subCat,
							"Client validated XorMappedAddress' Port field.");
				}
				else 
					logger.debug(PC2LogCategory.STUN, subCat,
					"The XorMappedAddress' Port field received by the Client is invalid.");

				StunMessage req = factory.createAllocateRequest(Transport.UDP, false, Allocation.TTL,
						true, false, username, realm, password, null, addFingerPrint);
				if (req != null) {
					int seq = LogAPI.getSequencer();
					int processorID = stack.getProcessorID(localAddr);
					stack.sendMessage(processorID, req, seq, remoteAddr);
				}
				logger.debug(PC2LogCategory.STUN, subCat,
						"Client sending :\n" + req);
			}
		}
		else if (type == StunConstants.ALLOCATE_RESPONSE_MSG_TYPE) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ie) {
				
			}
			StunMessage req = factory.createRefreshRequest(username, realm, 
					password, null, Allocation.TTL, addFingerPrint);
			if (req != null) {
				int seq = LogAPI.getSequencer();
				int processorID = stack.getProcessorID(localAddr);
				stack.sendMessage(processorID, req, seq, remoteAddr);
			}
			logger.debug(PC2LogCategory.STUN, subCat,
					"Client sending :\n" + req);
		}
		else if (type == StunConstants.REFRESH_RESPONSE_MSG_TYPE) {
			Nonce n = (Nonce)event.getAttribute(StunConstants.NONCE_TYPE);
			if (n != null) {
				String peerAddress = remoteIP;
				int peerPort = 20000;
				StunMessage req = factory.createChannelBindRequest(username, realm, password, 
						n, myChannel, peerAddress, peerPort, addFingerPrint);
				if (req != null) {
					int seq = LogAPI.getSequencer();
					int processorID = stack.getProcessorID(localAddr);
					stack.sendMessage(processorID, req, seq, remoteAddr);
					logger.debug(PC2LogCategory.STUN, subCat,
							"Client sending :\n" + req);
				}
				
			}
			
		}
		else if (type == StunConstants.CHANNEL_BIND_RESPONSE_MSG_TYPE) {
			stack.addBinding(myChannel);
			logger.info(PC2LogCategory.STUN, subCat, "Client successfully bound channel.");
		}
	}
	
	public void run() {
		StunMessage req = factory.createBindingRequest();
		req = create5769BindingRequest(req);
		//StunMessage req = factory.createBindingRequest(username, realm, 
		//		password, null, addFingerPrint);
		

		if (req != null) {
			int seq = LogAPI.getSequencer();
			int processorID = stack.getProcessorID(localAddr);
			stack.sendMessage(processorID, req, seq, remoteAddr);

			try {
				while (queue.size() == 0) {
					Thread.sleep(1000);
				}

				StunEvent se = queue.remove();
				if (se.getEvent().getName().equals(StunConstants.BINDING_RESPONSE))
					logger.debug(PC2LogCategory.STUN, subCat, "OKAY");
				else
					logger.debug(PC2LogCategory.STUN, subCat, "FAILED");
				
				Nonce n = (Nonce)se.getEvent().getAttribute(StunConstants.NONCE_TYPE);
				req = factory.createBindingRequest(username, realm, password, n, 
						addFingerPrint);
				
			}
			catch (InterruptedException ie) {
				return;
			}
		}
	}
	
	public StunMessage create5769BindingRequest(StunMessage msg) {
		logger.info(PC2LogCategory.STUN, subCat, "Creating BindingRequest message defined by RFC5769");

		String softwareName = "STUN test client";
		String userName = "evtj:h6vY";
		String password = "VOkJxbRl1RmTxUk/WvJxBt";
		byte [] test = { // HEADER and Length
				(byte)0x00, (byte)0x01, (byte)0x00, (byte)0x50,     
				// TRANSACTION ID
				(byte)0x21, (byte)0x12, (byte)0xa4, (byte)0x42,   
				(byte)0xb7, (byte)0xe7, (byte)0xa7, (byte)0x01,  
				(byte)0xbc, (byte)0x34, (byte)0xd6, (byte)0x86, 
				(byte)0xfa, (byte)0x87, (byte)0xdf, (byte)0xae,
				// SOFTWARE
				(byte)0x80, (byte)0x22, (byte)0x00, (byte)0x10, 
				(byte)0x53, (byte)0x54, (byte)0x55, (byte)0x4e, 
				(byte)0x20, (byte)0x74, (byte)0x65, (byte)0x73,  
				(byte)0x74, (byte)0x20, (byte)0x63, (byte)0x6c,  
				(byte)0x69, (byte)0x65, (byte)0x6e, (byte)0x74,  
				// PRIORITY
				(byte)0x00, (byte)0x24, (byte)0x00, (byte)0x04,    
				(byte)0x6e, (byte)0x00, (byte)0x01, (byte)0xff,     
				(byte)0x80, (byte)0x29, (byte)0x00, (byte)0x08,    
				(byte)0x93, (byte)0x2f, (byte)0xf9, (byte)0xb1,  
				(byte)0x51, (byte)0x26, (byte)0x3b, (byte)0x36,  
				// USERNAME (with 3 bytes of padding)
				(byte)0x00, (byte)0x06, (byte)0x00, (byte)0x09,  
				(byte)0x65, (byte)0x76, (byte)0x74, (byte)0x6a, 
				(byte)0x3a, (byte)0x68, (byte)0x36, (byte)0x76,  
				(byte)0x59, (byte)0x20, (byte)0x20, (byte)0x20};  
		// MESSAGER_INTEGRITY
		//				(byte)0x00, (byte)0x08, (byte)0x00, (byte)0x14,  
		//				(byte)0x9a, (byte)0xea, (byte)0xa7, (byte)0x0c,
		//				(byte)0xbf, (byte)0xd8, (byte)0xcb, (byte)0x56, 				 
		//				(byte)0x78, (byte)0x1e, (byte)0xf2, (byte)0xb5, 
		//				(byte)0xb2, (byte)0xd3, (byte)0xf2, (byte)0x49,  
		//				(byte)0xc1, (byte)0xb5, (byte)0x71, (byte)0xa2};  
		//				// FINGERPRINT
		//				(byte)0x80, (byte)0x28, (byte)0x00, (byte)0x04, 
		//				(byte)0xe5, (byte)0x7a, (byte)0x3b, (byte)0xcf}; 

		byte [] testMI = {(byte)0x9a, (byte)0xea, (byte)0xa7, (byte)0x0c,
				(byte)0xbf, (byte)0xd8, (byte)0xcb, (byte)0x56, 				 
				(byte)0x78, (byte)0x1e, (byte)0xf2, (byte)0xb5, 
				(byte)0xb2, (byte)0xd3, (byte)0xf2, (byte)0x49,  
				(byte)0xc1, (byte)0xb5, (byte)0x71, (byte)0xa2};

		byte [] test2 = { // HEADER and Length
				(byte)0x00, (byte)0x01, (byte)0x00, (byte)0x50,     
				// TRANSACTION ID
				(byte)0x21, (byte)0x12, (byte)0xa4, (byte)0x42,   
				(byte)0xb7, (byte)0xe7, (byte)0xa7, (byte)0x01,  
				(byte)0xbc, (byte)0x34, (byte)0xd6, (byte)0x86, 
				(byte)0xfa, (byte)0x87, (byte)0xdf, (byte)0xae,
				// SOFTWARE
				(byte)0x80, (byte)0x22, (byte)0x00, (byte)0x10, 
				(byte)0x53, (byte)0x54, (byte)0x55, (byte)0x4e, 
				(byte)0x20, (byte)0x74, (byte)0x65, (byte)0x73,  
				(byte)0x74, (byte)0x20, (byte)0x63, (byte)0x6c,  
				(byte)0x69, (byte)0x65, (byte)0x6e, (byte)0x74,  
				// PRIORITY
				(byte)0x00, (byte)0x24, (byte)0x00, (byte)0x04,    
				(byte)0x6e, (byte)0x00, (byte)0x01, (byte)0xff,     
				(byte)0x80, (byte)0x29, (byte)0x00, (byte)0x08,    
				(byte)0x93, (byte)0x2f, (byte)0xf9, (byte)0xb1,  
				(byte)0x51, (byte)0x26, (byte)0x3b, (byte)0x36,  
				// USERNAME (with 3 bytes of padding)
				(byte)0x00, (byte)0x06, (byte)0x00, (byte)0x09,  
				(byte)0x65, (byte)0x76, (byte)0x74, (byte)0x6a, 
				(byte)0x3a, (byte)0x68, (byte)0x36, (byte)0x76,  
				(byte)0x59, (byte)0x20, (byte)0x20, (byte)0x20,  
				// MESSAGER_INTEGRITY
				(byte)0x00, (byte)0x08, (byte)0x00, (byte)0x14,  
				(byte)0x9a, (byte)0xea, (byte)0xa7, (byte)0x0c,
				(byte)0xbf, (byte)0xd8, (byte)0xcb, (byte)0x56, 				 
				(byte)0x78, (byte)0x1e, (byte)0xf2, (byte)0xb5, 
				(byte)0xb2, (byte)0xd3, (byte)0xf2, (byte)0x49,  
				(byte)0xc1, (byte)0xb5, (byte)0x71, (byte)0xa2};  		

		byte [] testBody = { // HEADER and Length
				(byte)0x00, (byte)0x01, (byte)0x00, (byte)0x38,     
				// TRANSACTION ID
				(byte)0x21, (byte)0x12, (byte)0xa4, (byte)0x42,   
				(byte)0xb7, (byte)0xe7, (byte)0xa7, (byte)0x01,  
				(byte)0xbc, (byte)0x34, (byte)0xd6, (byte)0x86, 
				(byte)0xfa, (byte)0x87, (byte)0xdf, (byte)0xae,
				// SOFTWARE
				(byte)0x80, (byte)0x22, (byte)0x00, (byte)0x10, 
				(byte)0x53, (byte)0x54, (byte)0x55, (byte)0x4e, 
				(byte)0x20, (byte)0x74, (byte)0x65, (byte)0x73,  
				(byte)0x74, (byte)0x20, (byte)0x63, (byte)0x6c,  
				(byte)0x69, (byte)0x65, (byte)0x6e, (byte)0x74,  
				// PRIORITY
				(byte)0x00, (byte)0x24, (byte)0x00, (byte)0x04,    
				(byte)0x6e, (byte)0x00, (byte)0x01, (byte)0xff,     
				(byte)0x80, (byte)0x29, (byte)0x00, (byte)0x08,    
				(byte)0x93, (byte)0x2f, (byte)0xf9, (byte)0xb1,  
				(byte)0x51, (byte)0x26, (byte)0x3b, (byte)0x36,  
				// USERNAME (with 3 bytes of padding)
				(byte)0x00, (byte)0x06, (byte)0x00, (byte)0x09,  
				(byte)0x65, (byte)0x76, (byte)0x74, (byte)0x6a, 
				(byte)0x3a, (byte)0x68, (byte)0x36, (byte)0x76,  
				(byte)0x59, (byte)0x20, (byte)0x20, (byte)0x20}; 

		byte[] msg1 = {
				(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x58, //    Request type and message length

				(byte) 0x21, (byte) 0x12, (byte) 0xa4, (byte) 0x42, //    Magic cookie

				(byte) 0xb7, (byte) 0xe7, (byte) 0xa7, (byte) 0x01, // }
				(byte) 0xbc, (byte) 0x34, (byte) 0xd6, (byte) 0x86, // }  Transaction ID
				(byte) 0xfa, (byte) 0x87, (byte) 0xdf, (byte) 0xae, // }

				(byte) 0x80, (byte) 0x22, (byte) 0x00, (byte) 0x10, //    SOFTWARE attribute header

				(byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x4e, // }
				(byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x73, // }  User-agent...
				(byte) 0x74, (byte) 0x20, (byte) 0x63, (byte) 0x6c, // }  ...name
				(byte) 0x69, (byte) 0x65, (byte) 0x6e, (byte) 0x74, // }

				(byte) 0x00, (byte) 0x24, (byte) 0x00, (byte) 0x04, //    PRIORITY attribute header
				(byte) 0x6e, (byte) 0x00, (byte) 0x01, (byte) 0xff, //    ICE priority value
				(byte) 0x80, (byte) 0x29, (byte) 0x00, (byte) 0x08, //    ICE-CONTROLLED attribute header

				(byte) 0x93, (byte) 0x2f, (byte) 0xf9, (byte) 0xb1, // }  Pseudo-random tie breaker...
				(byte) 0x51, (byte) 0x26, (byte) 0x3b, (byte) 0x36, // }   ...for ICE control

				(byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x09, //    USERNAME attribute header
				(byte) 0x65, (byte) 0x76, (byte) 0x74, (byte) 0x6a, // }
				(byte) 0x3a, (byte) 0x68, (byte) 0x36, (byte) 0x76, // }  Username (9 bytes) and padding (3 bytes)
				(byte) 0x59, (byte) 0x20, (byte) 0x20, (byte) 0x20, // }

				(byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x14, //    MESSAGE-INTEGRITY attribute header
				(byte) 0x9a, (byte) 0xea, (byte) 0xa7, (byte) 0x0c, // }
				(byte) 0xbf, (byte) 0xd8, (byte) 0xcb, (byte) 0x56, // }
				(byte) 0x78, (byte) 0x1e, (byte) 0xf2, (byte) 0xb5, // }  HMAC-SHA1 fingerprint
				(byte) 0xb2, (byte) 0xd3, (byte) 0xf2, (byte) 0x49, // }
				(byte) 0xc1, (byte) 0xb5, (byte) 0x71, (byte) 0xa2, // }
				(byte) 0x80, (byte) 0x28, (byte) 0x00, (byte) 0x04, //    FINGERPRINT attribute header
				(byte) 0xe5, (byte) 0x7a, (byte) 0x3b, (byte) 0xcf
		};
		
		byte [] testCRC = { (byte) 0xe5, (byte) 0x7a, (byte) 0x3b, (byte) 0xcf };

		byte[] id = {0x21, 0x12, (byte)0xa4, 0x42, (byte)0xb7, (byte)0xe7, (byte)0xa7, 0x01, (byte)0xbc,
				(byte)0x34, (byte)0xd6, (byte)0x86, (byte)0xfa, (byte)0x87, (byte)0xdf, (byte)0xae }; 
		msg.setTransactionID(id);
		Software s = new Software(softwareName.getBytes());
		msg.addAttribute(s);

		byte [] priority = { 0x6e, 0x00, 0x01, (byte)0xff};
		Priority p = new Priority(priority);
		msg.addAttribute(p);

		byte [] iceControlled = {(byte)0x93, 0x2f, (byte)0xf9, (byte)0xb1, 0x51, 0x26, 0x3b, 0x36 };  
		IceControlled ic = new IceControlled(iceControlled);
		msg.addAttribute(ic);

		Username u = new Username(userName.getBytes());
		msg.addAttribute(u);

		byte [] body = msg.encode();

		System.out.println("Bodies match = " + Arrays.equals(testBody, body));
		//System.out.println("Body hex = " + Conversion.formattedHexString(body));

		byte [] digest = msg.encodeForDigest();
		System.out.println("Digest bytes match = " + Arrays.equals(test, digest));
		//		System.out.println("Digest hex = " + Conversion.formattedHexString(digest));
		MessageIntegrity mi = new MessageIntegrity(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
		mi.calculate(msg, password);
		msg.addAttribute(mi);
		//mi.calculate(msg, password);

		UseCandidate uc = new UseCandidate();
		msg.addAttribute(uc);
		byte [] hmac = mi.getValue();

		System.out.println("Hmac matches = " + Arrays.equals(testMI, hmac));


		byte [] encoding = msg.encode();

		System.out.println("Message-Integrity matches = " + Arrays.equals(test2, encoding));

		byte [] mi2 = msg.encodeForFingerPrint();
		
		System.out.println("FingerPrint ingest matches = " + Arrays.equals(test2, mi2));
		//		if (includeFingerPrint) {
		FingerPrint fp = new FingerPrint(StunConstants.STUN_EMPTY_FINGER_PRINT);
			//				req.addAttribute(fp);
			fp.calculate(msg);
			msg.addAttribute(fp);
			//				
			//			}
            
			byte [] crc = fp.getValue();
			System.out.println("FingerPrint matches = " + Arrays.equals(testCRC,crc));
			logger.info(PC2LogCategory.STUN, subCat, msg.toString());
			byte [] encoded = msg.encode();
			//		StringBuffer hex = Conversion.formattedHexString(encoding);
			boolean match = Arrays.equals(msg1, encoded);
            System.out.println("Encoding is " + (match ? "valid" : "invalid"));
			logger.info(PC2LogCategory.STUN, subCat, "Encoding is " + (match ? "valid" : "invalid"));
			return msg;
	}
	
//	public static String hmacSha1(byte [] value, String  key) { 
//        try { 
//            // Get an hmac_sha1 key from the raw key bytes 
//            byte[] keyBytes = key.getBytes();            
//            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1"); 
// 
//            // Get an hmac_sha1 Mac instance and initialize with the signing key 
//            Mac mac = Mac.getInstance("HmacSHA1"); 
//            mac.init(signingKey); 
//           
//           // Compute the hmac on input data bytes 
//            byte[] rawHmac = mac.doFinal(value); 
// 
//            // Convert raw bytes to Hex 
//            byte[] hexBytes = new Hex().encode(rawHmac); 
//            System.out.println("hmac=\n" + Conversion.formattedHexString(hexBytes));
//            //  Covert array of Hex bytes to a String 
//            return new String(hexBytes, "UTF-8"); 
//        } catch (Exception e) { 
//            throw new RuntimeException(e); 
//        } 
//    } 

	
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

	private boolean validateFingerPrint(StunMessage event) {
		FingerPrint fp = (FingerPrint)event.getAttribute(StunConstants.FINGERPRINT_TYPE);
		if (fp != null) {
			byte [] eventFP = fp.getValue(); 
			byte [] temp = new byte[eventFP.length];
			System.arraycopy(eventFP, 0, temp, 0, eventFP.length);
			fp.setValue(StunConstants.STUN_EMPTY_FINGER_PRINT);
			fp.calculate(event);
			byte [] calcFP = fp.getValue();
			if (java.util.Arrays.equals(temp, calcFP))
				return true;
			else {
				logger.warn(PC2LogCategory.STUN, subCat,
						"Client failing FingerPrint because event containted\neventFP=" 
						+ Conversion.hexString(eventFP) + "\n calcFP=" 
						+ Conversion.hexString(calcFP));
			}
		}
		else 
			return true;

		logger.debug(PC2LogCategory.STUN, subCat,
				"STUN Client is failing FingerPrint validation for event=" 
				+ event.getName());
		return false;
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
		boolean result = false;
		// First we need to remove the finger print attribute if it is present
		FingerPrint fp = (FingerPrint)event.getAttribute(StunConstants.FINGERPRINT_TYPE);
		if (fp != null) 
			event.removeAttribute(StunConstants.FINGERPRINT_TYPE);
		MessageIntegrity mi = (MessageIntegrity)event.getAttribute(StunConstants.MESSAGE_INTEGRITY_TYPE);
		if (mi != null) {
			byte [] eventMI = mi.getValue(); 
			byte [] temp = new byte[eventMI.length];
			System.arraycopy(eventMI, 0, temp, 0, eventMI.length);
			mi.setValue(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
			mi.calculate(event, username, realm, password);
			byte [] calcMI = mi.getValue();
			if (java.util.Arrays.equals(temp, calcMI))
				result = true;
			else {
				logger.warn(PC2LogCategory.STUN, subCat,
						"Client failing MessageIntegrity because event containted\neventMI=" 
						+ Conversion.hexString(eventMI) + "\n calcMI=" 
						+ Conversion.hexString(calcMI));
			}

		}
		else
			result = true;
		
		// Whether it passed or failed add the finger print back to the message
		if (fp != null)
			event.addAttribute(fp);
		
		return result;
	}
	
	public static void main(String[] args) {
		try {
			StunClient client = new StunClient();
			client.start();
			
			while (client.isRunning) {
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
