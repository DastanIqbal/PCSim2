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

import com.cablelabs.common.*;
import com.cablelabs.log.*;

public class StunMessageProcessor implements Runnable {

	private static final LogAPI logger = LogAPI.getInstance();
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private static String subCat = "Stack";
	
	private static int processorID = 0;
	/**
	 * Max datagram size.
	 */
	private static final int MAX_DATAGRAM_SIZE = 8 * 1024;
	
	/**
	 * InetSocketAddress
	 */
	private InetSocketAddress address = null;
	/**
	 * The message queue is where incoming messages are added.
	 */
	private ConcurrentLinkedQueue<RawData> queue = null;
	
	/**
	 * The socket object that is used by this access point to the network.
	 */
	protected DatagramSocket udpSock = null;
	
	/**
	 * The TCP or TLS socket that is used by this access point to the network.
	 */
	protected Socket tcpSock = null;
	
	/**
	 * A flag that is set to false to exit the message processor.
	 */
	private boolean isRunning = false;
	
	/**
	 * A flag indicating whether the socket being used is TCP or UDP
	 */
	private boolean useTCP = false;
	
	/**
	 * A flag indicating whether the access point is owned by this class or
	 * not.
	 */
	private boolean socketOwner = false;
	
	/**
	 * Used for locking socket operations
	 */
	private Object socketLock = new Object();

	/**
	 * The current thread the processor is running under.
	 */
	private Thread thread = null;
	
	private String threadName = null;
	/**
	 * A unique identifier for this instance of processor for fast retrieval
	 * when sending a STUN message
	 */
	private int id = 0;
	
	private StunStack stack = StunStack.getInstance();
	
	protected StunMessageProcessor(ConcurrentLinkedQueue<RawData> queue, 
			InetSocketAddress address, boolean useTCP, String threadName) {
		this.queue = queue;
		this.address = address;
		this.useTCP = useTCP;
		this.socketOwner = true;
		this.id = ++processorID;
		this.threadName = threadName;
	}
	
	public StunMessageProcessor(ConcurrentLinkedQueue<RawData> queue, 
			DatagramSocket sock) {
		this.queue = queue;
		//this.address = address;
		this.useTCP = false;
		this.socketOwner = false;
		this.udpSock = sock;
		this.id = ++processorID;
	}
	
	public StunMessageProcessor(ConcurrentLinkedQueue<RawData> queue, 
			Socket sock) {
		this.queue = queue;
		//this.address = address;
		this.useTCP = true;
		this.socketOwner = false;
		this.tcpSock = sock;
		this.id = ++processorID;
	}
	
	public InetSocketAddress getAddress() {
		return this.address;
	}
	
	public boolean isTCPProcessor() {
		if (tcpSock != null && udpSock == null) {
			return true;
		}
		return false;
	}
	
	public boolean isUDPProcessor() {
		if (udpSock != null && tcpSock == null) {
			return true;
		}
		return false;
	}
	static public boolean isRTPPacket(byte [] msg) {
		if ((msg[0] & (byte)0x80) == (byte) 0x80)
			return true;
		return false;
	}
	
	/**
	 * This method test whether the packet is a STUN packet or some
	 * other protocol. The rules are the first byte must be 0x00 and 
	 * the magic cookie must appear in bytes 4-7.
	 * 
	 * @param msg - the raw bytes received on the socket
	 * @param len - the number of bytes in the message
	 * @return - true if it is a STUN packet, false otherwise
	 */
	private boolean isStunPacket(DatagramPacket packet) {
		int len = packet.getLength();
		byte [] data = packet.getData();
		int highbits = (data[0] >> 6);
		Character channel = Conversion.getChar(data, 0);
		 //(char)(((data[0]<<8) & 0xFF00) | (data[1]&0xFF));
		if (highbits == 0 && len >= StunConstants.STUN_HEADER_LENGTH) {
			byte [] mc = new byte [StunConstants.STUN_MAGIC_COOKIE_LENGTH];
			System.arraycopy(data, 4, mc, 0, mc.length);
			if (java.util.Arrays.equals(StunConstants.STUN_MAGIC_COOKIE,mc)) {
				return true;
			}
		}
		else if (highbits > 0 && stack.hasBinding(channel)){
			return true;
		}
		return false;
	}
	
	/**
	 * This method test whether the packet is a STUN packet or some
	 * other protocol. The rules are the first byte must be 0x00 and 
	 * the magic cookie must appear in bytes 4-7.
	 * 
	 * @param msg - the raw bytes received on the socket
	 * @param len - the number of bytes in the message
	 * @return - true if it is a STUN packet, false otherwise
	 */
	static public boolean isStunPacket(byte [] data, int len) {
		int highbits = (data[0] >> 6);
		if (highbits == 0) {
			byte [] mc = new byte [StunConstants.STUN_MAGIC_COOKIE_LENGTH];
			System.arraycopy(data, 4, mc, 0, mc.length);
			if (java.util.Arrays.equals(StunConstants.STUN_MAGIC_COOKIE,mc)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Start the network listening thread.
	 *
	 * @throws IOException if we fail to setup the socket.
	 */
	public void start()	throws IOException	{
		if (socketOwner) {
			if (useTCP) {
				logger.warn(PC2LogCategory.STUN, subCat,
						"Local TCP sockets are not currently supported for STUN protocol.");
			}
			else {
				synchronized(socketLock){
					if (udpSock == null) {
						this.udpSock = new DatagramSocket(address);
						logger.info(PC2LogCategory.STUN, subCat,
								"StunMessageProcessor bound to socket " + address.toString());
					}			
					udpSock.setReceiveBufferSize(MAX_DATAGRAM_SIZE);
				}
			}
		}
		this.isRunning = true;
		if (threadName != null) {
			thread = new Thread(this, threadName);
		}
		else {
			if (udpSock != null) {
				thread = new Thread(this, "STUN - UDP" + udpSock.getLocalAddress().toString() + ":" + udpSock.getLocalPort());
			}
			else if (tcpSock != null)
				thread = new Thread(this, "STUN - TCP" + tcpSock.getLocalAddress().toString() + ":" + tcpSock.getLocalPort());
			else 
				thread = new Thread(this, "STUN - " + id);
		}
		thread.setDaemon(true);
		thread.start();	
		
	}
	
	
	/**
	 * The listening thread's run method.
	 */
	public void run() {
		while (this.isRunning) {
			
			if (socketOwner) {
				if (!useTCP) {
					try	{
						int bufsize = udpSock.getReceiveBufferSize();
						byte message[] = new byte[bufsize];
						DatagramPacket packet = new DatagramPacket(message, bufsize);
						udpSock.receive(packet);
						if (isStunPacket(packet)) {
//							RawMessage rawMessage = new RawMessage( message,
//									packet.getLength(), packet.getAddress(), packet.getPort(),
//									address, id, Transport.UDP);
							ByteArray ba = new ByteArray(message, packet.getLength());
				    		RawData rawData = new RawData(this,
				    				 ba, packet.getLength(), 
				    				 packet.getAddress().getHostAddress(), 
				    				 packet.getPort(), 
				    				 address.getAddress().getHostAddress(), 
				    				 address.getPort(), Transport.UDP,
				    				 LogAPI.getSequencer());
//							int seq = LogAPI.getSequencer();
//							logger.info(PC2LogCategory.STUN, subCat,
//									">>>>> RX:\tLength = " + packet.getLength()
//				            		+ "\nReceived on IP|Port=" + address.getAddress().getHostAddress() 
//				            		+ "|" + address.getPort()  
//				            		+ "\nFrom IP|Port=" + packet.getAddress().getHostAddress() 
//				            		+ "|" + packet.getPort()
//				            		+ "\nSequencer=" + seq
//				            		+ "\n[" + Conversion.hexString(packet.getData()) + "]");
//							logger.trace(PC2LogCategory.STUN, subCat,
//									"StunMessageProcessor read " + packet.getLength() + " bytes.");
							synchronized (queue) {
								queue.add(rawData);
								queue.notify();
							}
						}
						else if (isRTPPacket(packet.getData())) {
//							RawMessage rawMessage = new RawMessage( message,
//									packet.getLength(), packet.getAddress(), packet.getPort(),
//									address, id, Transport.UDP);
							ByteArray ba = new ByteArray(message, packet.getLength());
				    		int seq = LogAPI.getSequencer();
							RawData rawData = new RawData(this,
				    				 ba, packet.getLength(), 
				    				 packet.getAddress().getHostAddress(), 
				    				 packet.getPort(), 
				    				 address.getAddress().getHostAddress(), 
				    				 address.getPort(), Transport.UDP,
				    				 seq);

							if (logger.isTraceEnabled(PC2LogCategory.SIP, "RTP")) {
								logger.trace(PC2LogCategory.SIP, "RTP",
									">>>>> RX:\tLength = " + packet.getLength()
				            		+ "\nReceived on IP|Port=" + address.getAddress().getHostAddress() 
				            		+ "|" + address.getPort()  
				            		+ "\nFrom IP|Port=" + packet.getAddress().getHostAddress() 
				            		+ "|" + packet.getPort()
				            		+ "\nSequencer=" + seq
				            		+ "\nTransport=" + Transport.UDP
				            		+ "\n[" + Conversion.formattedHexString(packet.getData())+ "]");
							}
							else if (logger.isDebugEnabled(PC2LogCategory.SIP, "RTP")){
								logger.debug(PC2LogCategory.SIP, "RTP",
										">>>>> RX:\tLength = " + packet.getLength()
					            		+ "\nReceived on IP|Port=" + address.getAddress().getHostAddress() 
					            		+ "|" + address.getPort()  
					            		+ "\nFrom IP|Port=" + packet.getAddress().getHostAddress() 
					            		+ "|" + packet.getPort()
					            		+ "\nSequencer=" + seq
					            		+ "\nTransport=" + Transport.UDP
					            		+ "\n");
							}
//							logger.trace(PC2LogCategory.SIP, "RTP",
//									"RTPMessageProcessor read " + packet.getLength() + " bytes.");
							synchronized (queue) {
								queue.add(rawData);
								queue.notify();
							}
						}
						else
							logger.debug(PC2LogCategory.STUN, subCat,
									"Dropping non-STUN packet of length=" + packet.getLength() + " on port=" + udpSock.getPort());
						
					}
					
					catch (SocketException ex) {
						if (isRunning) {
							// Something wrong has happened
							logger.warn(PC2LogCategory.STUN, subCat,
									"The StunMessageProcessor has gone useless:", ex);
							stop();
						}
						else				{
							//The exception was most probably caused by calling this.stop()
							// ....
						}
					}
					catch (IOException ex) {
						logger.warn(PC2LogCategory.STUN, subCat,
								"The StunMessageProcessor has gone useless:", ex);
					}
					catch (Throwable ex) {
						logger.warn(PC2LogCategory.STUN, subCat,
								"The StunMessageProcessor has gone useless:", ex);
						
						stop();
					}
				}
			}
		}
	}
	
	public synchronized void stop() {
		this.isRunning = false;
		if (socketOwner) {
			if (udpSock != null)  {
				synchronized (socketLock) {
					udpSock.close();
				}
				logger.info(PC2LogCategory.STUN, subCat,
						"StunMessageProcessor closed UDP socket on " + address.toString());
				udpSock = null;
				
			}
			if (tcpSock != null) {
				synchronized (socketLock) {
					try {
						tcpSock.close();
					}
					catch (Exception e) {
						logger.warn(PC2LogCategory.STUN, subCat,
								"StunMessageProcess encountered error while trying to close TCP socket.");
					}
				}
				logger.info(PC2LogCategory.STUN, subCat,
						"StunMessageProcessor closed UDP socket on " + address.toString());
				tcpSock = null;
			}
			
		}
		
	}
	
    public boolean addEmbeddedPacket(byte[] messageBytes,
            int messageLength, InetAddress remoteAddress,
            int remotePort, String localIP, int localPort) {
   	 try {
//    		 InetSocketAddress local = new InetSocketAddress(localIP, localPort);
    		 
//    		 RawMessage rawMessage = new RawMessage( messageBytes,
//   				 messageLength, remoteAddress, remotePort, local, id, Transport.UDP);
    		 ByteArray ba = new ByteArray(messageBytes, messageLength);
    		 RawData rawData = new RawData(this,
    				 ba, messageLength, 
    				 remoteAddress.getHostAddress(), 
    				 remotePort, localIP, localPort, Transport.UDP,
    				 LogAPI.getSequencer());
    		 
    		 synchronized (queue) {
    			 queue.add(rawData);
    			 queue.notify();
    		 }
   		 return true;
   	 }
   	 catch (Exception e) {
   		 logger.warn(PC2LogCategory.STUN, subCat,
   				 "Encountered error while trying to deliver stun packet.", e);
   	 }
   	 return false;
    }

	void sendMessage(StunMessage message, int seq,
			InetSocketAddress peerAddress)	
		throws IOException	{
		//logger.trace("Sending Stun message(" + message.toString() +") to " 
		//		+ address.toString() + " using UDP.\n");
		byte [] data = message.encode();
		if (!useTCP) {
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, peerAddress);
			synchronized(socketLock){
				udpSock.send(datagramPacket);
				// PC 2.0 add logging statement for all SIP messages received on the s
				// socket
	            logger.info(PC2LogCategory.STUN, subCat,
	            		"<<<<< TX:\tLength = " + data.length
	            		+ "\nSent from IP|Port=" 
	            		+ udpSock.getLocalAddress().getHostAddress() 
	            		+ "|" + udpSock.getLocalPort() 
	            		+ "\nTo IP|Port=" 
	            		+ peerAddress// + "|" + peerPort
	            		+ "\nSequencer=" + seq
	            		+ "\nTransport=" + Transport.UDP
	            		+ (stack.useCompressedForm() 
	            				? ("\n" + message.getName()) 
	            						: ("\n[" + message + "]")));
			}
		}
		else if (tcpSock != null && tcpSock.isConnected()) {
        	logger.trace(PC2LogCategory.STUN, subCat,
        			"Sending Stun message(" + message.toString() + ") to " 
        			+ address.toString() + " using TCP.\n");
        	synchronized(tcpSock) {
        		tcpSock.getOutputStream().write(data, 0, data.length);
        		// PC 2.0 add logging statement for all SIP messages received on the s
				// socket
	            logger.info(PC2LogCategory.STUN, subCat,
	            		"<<<<< TX:\tLength = " + data.length
	            		+ "\nSent from IP|Port=" 
	            		+ tcpSock.getLocalAddress().getHostAddress() 
	            		+ "|" + tcpSock.getLocalPort()  
	            		+ "\nTo IP|Port=" 
	            		+ peerAddress// + "|" + peerPort
	            		+ "\nSequencer=" + seq
	            		+ "\nTransport=" + Transport.TCP
	            		+ "\n[" + message + "]");
        	}
		}
	}
	
	void sendRawMessage(byte [] data, int length, int seq, InetSocketAddress peerAddress, String sub)	
		throws IOException	{
		
		if (!useTCP) {
			DatagramPacket datagramPacket = new DatagramPacket(data, length, peerAddress);
			synchronized(socketLock){
				udpSock.send(datagramPacket);
				// PC 2.0 add logging statement for all SIP messages received on the s
				// socket
				if (logger.isTraceEnabled(PC2LogCategory.SIP, sub)) {
						logger.trace(PC2LogCategory.SIP, sub,
						"<<<<< TX:\tLength = " + length
						+ "\nSent from IP|Port=" 
						+ udpSock.getLocalAddress().getHostAddress() 
						+ "|" + udpSock.getLocalPort() 
						+ "\nTo IP|Port=" 
						+ peerAddress// + "|" + peerPort
						+ "\nSequencer=" + seq
						+ "\nTransport=" + Transport.UDP
						+ "\n[" + Conversion.formattedHexString(data) + "]");
				}
				else {
					logger.debug(PC2LogCategory.SIP, sub,
							"<<<<< TX:\tLength = " + length
							+ "\nSent from IP|Port=" 
							+ udpSock.getLocalAddress().getHostAddress() 
							+ "|" + udpSock.getLocalPort() 
							+ "\nTo IP|Port=" 
							+ peerAddress// + "|" + peerPort
							+ "\nSequencer=" + seq
							+ "\nTransport=" + Transport.UDP
							+ "\n");
				}
			}
		}
		else if (tcpSock != null && tcpSock.isConnected()) {
			logger.trace(PC2LogCategory.SIP, sub,
					"Sending RTP message(" + Conversion.formattedHexString(data) + ") to " 
					+ address.toString() + " using TCP.\n");
			synchronized(tcpSock) {
				tcpSock.getOutputStream().write(data, 0, length);
				// PC 2.0 add logging statement for all SIP messages received on the s
				// socket
				if (logger.isTraceEnabled(PC2LogCategory.SIP, sub)) {
					logger.trace(PC2LogCategory.SIP, sub,
						"<<<<< TX:\tLength = " + length
						+ "\nSent from IP|Port=" 
						+ tcpSock.getLocalAddress().getHostAddress() 
						+ "|" + tcpSock.getLocalPort()  
						+ "\nTo IP|Port=" 
						+ peerAddress// + "|" + peerPort
						+ "\nSequencer=" + seq
						+ "\nTransport=" + Transport.TCP
						+ "\n[" + Conversion.formattedHexString(data) + "]");
				}
				else {
					logger.debug(PC2LogCategory.SIP, sub,
							"<<<<< TX:\tLength = " + length
							+ "\nSent from IP|Port=" 
							+ tcpSock.getLocalAddress().getHostAddress() 
							+ "|" + tcpSock.getLocalPort()  
							+ "\nTo IP|Port=" 
							+ peerAddress// + "|" + peerPort
							+ "\nSequencer=" + seq
							+ "\nTransport=" + Transport.TCP
							+ "\n");
				}
			}
		}
	}
	
	/**
	 * Returns a String representation of the object.
	 * @return a String representation of the object.
	 */
	public String toString() {
		return "StunMessageProcessor "
		+ (isRunning ? "isRunning":"not Running")
		+ "on address" + address.toString();
	}
	
	public int getID() {
		return this.id;
	}
}
