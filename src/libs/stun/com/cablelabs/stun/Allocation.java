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
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.cablelabs.common.*;
import com.cablelabs.log.*;
import com.cablelabs.stun.attributes.*;


public class Allocation implements Runnable {

	protected String key = null;
	protected String clientIP = null;
	protected String serverIP = null; 
	protected int clientPort = -1;
	protected int serverPort = -1;
	protected InetSocketAddress clientAddr = null;
	protected InetSocketAddress serverAddr = null;
	protected Transport transport = Transport.UDP;
	
	protected String myIP = null;
	protected int myPort = -1;
	protected InetSocketAddress myAddr = null;
	
	protected StunMessageProcessor relayTransport = null;
	protected String username = null;
	protected byte [] transactionID = null;
	protected Byte propType = null;
	protected long timeToLive = 0; // in seconds.
	public static final int TTL = 600;
	public static final int RELEASING = 120;
	public static final int maxBW = 64;
	
	private boolean timerExpired = false;
	//private TurnStack stack = null;
	protected Allocation mate = null;
	private static final LogAPI logger = LogAPI.getInstance();
	private StunFactory factory = new StunFactory();
	private AllocationTask timerTask = null;
	private Timer timer = null;
	private TurnStack turnStack = null;
	private ConcurrentLinkedQueue<Integer> queue = null;
	/**
	 * This table holds the IP address of the peer to the channel number mappings for this allocation
	 * 
	 */
	private Hashtable<InetAddress, Binding>channelBindings = new Hashtable<InetAddress, Binding>();
	private Hashtable<Character, Binding>channelIndex = new Hashtable<Character, Binding>();
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private static String subCat = "Stack";
	
	/**
	 * Max datagram size.
	 */
	private static final int MAX_DATAGRAM_SIZE = 8 * 1024;
	
	/**
	 * The socket object that is used by this access point to the network.
	 */
	protected DatagramSocket udpSock = null;
	
	/**
	 * A flag that is set to false to exit the message processor.
	 */
	private boolean isRunning = false;
	
	/**
	 * Used for locking socket operations
	 */
	private Object socketLock = new Object();

	/**
	 * The current thread the processor is running under.
	 */
	private Thread thread = null;
	
	private HashSet<String> permissions = new HashSet<String>();
	
	public Allocation(TurnStack stack, 
			StunMessageProcessor smp,
			ConcurrentLinkedQueue<Integer> queue, 
			String localIP, int localPort, String key, 
			String user, StunEvent event, 
			Byte propType, Transport t) {
		this.turnStack = stack;
		this.relayTransport = smp;
		this.queue = queue;
		this.myIP = localIP;
		this.myPort = localPort;
		this.myAddr = new InetSocketAddress(myIP, myPort);
		this.key = key;
		this.username = user;
		byte [] transID = event.getEvent().getTransactionID();
		this.transactionID = new byte [transID.length];
		System.arraycopy(transID, 0, transactionID, 0, transID.length);
		this.clientIP = event.getRawData().getSrcIP();
		this.clientPort = event.getRawData().getSrcPort();
		this.serverIP = event.getRawData().getDestIP();
		this.serverPort = event.getRawData().getDestPort();
		clientAddr = new InetSocketAddress (clientIP, clientPort);
		serverAddr = new InetSocketAddress(serverIP, serverPort);
		if (propType != null)
			this.propType = propType;
		this.transport = t;
	}
	
	public void expired() {
		synchronized (queue) {
				queue.add(myPort);
				queue.notify();
		}
		if (!timerExpired) {
			timerExpired = true;
			if (timerTask != null) 
				timerTask = null;
				
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
			timer = new Timer("Allocation ExpireTimer" + myAddr, true);
			timerTask= new AllocationTask(this);
			timer.schedule(timerTask, (RELEASING * 1000));
			logger.debug(PC2LogCategory.STUN, subCat,
					"Start expire timer(" + timer + ") for " + 
					(RELEASING * 1000) + " msecs.");
			
		}
		else {
			if (timerTask != null) {
				timerTask = null;
			}
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
		}
	}
	
	public boolean addBinding(PeerAddress pa, ChannelNumber cn) {
		if (pa != null && cn != null) {
			byte [] address = pa.getAddress();
			Character newChannel = cn.getChannel();
			Binding curBinding = channelBindings.get(address);
			if (curBinding != null) {
				Character curChannel = curBinding.cn.getChannel();
				if (newChannel == curChannel) {
					logger.debug(PC2LogCategory.STUN, subCat,
						"TURN Allocation refreshing binding channel[" + Conversion.hexString(newChannel)
						+ "] to address[" + Conversion.address2String(address)
						+ "].");
					return true;
				}
				else {
					logger.warn(PC2LogCategory.STUN, subCat,
							"TURN Allocation failed to refresh binding channel[" + Conversion.hexString(newChannel)
							+ "] to address[" + Conversion.address2String(address)
							+ "] because the address is bound to another channel[" 
							+ Conversion.hexString(curChannel) + "].");
				}
			}
			else {
				logger.debug(PC2LogCategory.STUN, subCat,
						"TURN Allocation is binding channel[" + Conversion.hexString(newChannel)
						+ "] to address[" + Conversion.address2String(address)
						+ "].");
				try {
					Binding b = new Binding(pa, cn);
					InetAddress addr = InetAddress.getByAddress(address);
					channelBindings.put(addr, b);
					channelIndex.put(newChannel, b);
					return true;
				}
				catch (UnknownHostException uhe) {
					logger.warn(PC2LogCategory.STUN, subCat, 
							"TURN Allocation encountered an error when trying to create Channel Binding.", uhe);
				}
			}
		}
		return false;
	}
	
	public void addPermission(PeerAddress pa) {
		try {
			InetAddress ia = InetAddress.getByAddress(pa.getAddress());
			String ip = ia.toString();
			synchronized (permissions) {
				permissions.add(ip);
			}
		}
		catch (UnknownHostException uhe) {
			logger.warn(PC2LogCategory.STUN, subCat,
					"TURN Allocation could not add permission for IP=[" 
					+ new String(pa.getAddress()) + "].");
		}
		
	}
	
	public Binding getBinding(byte [] address) {
		try {
			InetAddress ia = InetAddress.getByAddress(address);
			return channelBindings.get(ia);

		}
		catch (UnknownHostException uhe) {
			logger.warn(PC2LogCategory.STUN, subCat, 
					"TURN Allocation encountered an error when trying to create Channel Binding.", uhe);
		}

		return null;
	}
	
	public Binding getBinding(Character channel) {
		return channelIndex.get(channel);
	}

	public String getKey() {
		return this.key;
	}
	
	public int getLocalPort() {
		return myPort;
	}
	

	public long getTimeToLive() {
		return this.timeToLive;
	}
	
	public byte [] getTransactionID() {
		return this.transactionID;
	}
	
	public Allocation getMate() {
		return this.mate;
	}
	
	public void removePermission(PeerAddress pa) {
		try {
			InetAddress ia = InetAddress.getByAddress(pa.getAddress());
			String ip = ia.toString();
			synchronized (permissions) {
				permissions.remove(ip);
			}
		}
		catch (UnknownHostException uhe) {
			logger.warn(PC2LogCategory.STUN, subCat,
					"TURN Allocation could not remove permission for IP=[" 
					+ new String(pa.getAddress()) + "].");
		}
		
	}
	public void refresh() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
			
		}
		timer = new Timer("Allocation Timer" + myAddr, true);
		timerTask= new AllocationTask(this);
		timer.schedule(timerTask, (TTL * 1000));
		logger.debug(PC2LogCategory.STUN, subCat,
				"Refreshing allocation timer(" + timer + ") for " + 
				(TTL * 1000) + " msecs.");
		
	}
	
	public boolean removeBinding(PeerAddress pa, ChannelNumber cn) {
		if (pa != null && cn != null) {
			try {
				InetAddress ia = InetAddress.getByAddress(pa.getAddress());
				Binding b = channelBindings.remove(ia);
				if (b != null) {
					Character channel = b.cn.getChannel();
					channelIndex.remove(channel);
					logger.debug(PC2LogCategory.STUN, subCat,
							"Removing channel binding for address[" 
							+ Conversion.address2String(pa.getAddress()) 
							+ "], mapped channel[" + 
							Conversion.hexString(channel) 
							+ "], and requested channel[" 
							+ Conversion.hexString(cn.getChannel()) + "].");
					return true;
				}
			}
			catch (UnknownHostException uhe) {
				logger.warn(PC2LogCategory.STUN, subCat, 
						"TURN Allocation encountered an error when trying to create Channel Binding.", uhe);
			}
		}
		return false;
	}
	
	/**
	 * The listening thread's run method.
	 */
	public void run() {
		while (this.isRunning) {
			try	{
				int bufsize = udpSock.getReceiveBufferSize();
				byte message[] = new byte[bufsize];
				DatagramPacket packet = new DatagramPacket(message, bufsize);
				udpSock.receive(packet);
				
//				RawMessage rawMessage = new RawMessage( message,
//						packet.getLength(), packet.getAddress(), packet.getPort(),
//						address, 0, Transport.UDP);
				int seq = LogAPI.getSequencer();
				logger.debug(PC2LogCategory.STUN, subCat,
						">>>>> RX:\tLength = " + packet.getLength()
						+ "\nReceived on IP|Port=" + myAddr.getAddress().getHostAddress() 
						+ "|" + myAddr.getPort()  
						+ "\nFrom IP|Port=" + packet.getAddress().getHostAddress() 
						+ "|" + packet.getPort()
						+ "\nSequencer=" + seq
						+ "\nTransport=" + transport.toString()
						+ "\n[" + Conversion.hexString(packet.getData()) + "]");
				if (relayTransport != null) {
					// Next we need to see if we should send the information in a DataIndication
					// or a ChannelData.
					byte [] peerAddr = packet.getAddress().getAddress();
					Binding b = getBinding(peerAddr);
					if (b != null) {
						// Use ChannelData to forward the data
						byte [] channel = Conversion.charToByteArray(b.cn.getChannel());
						byte [] length = StunConstants.lengthToByteArray(packet.getLength());
						byte [] channelData = new byte [packet.getLength() + 4];
						System.arraycopy(channel, 0, channelData, 0, 2);
						System.arraycopy(length, 0, channelData, 2, 2);
						System.arraycopy(message, 0, channelData, 4, packet.getLength());
						relayTransport.sendRawMessage(channelData, channelData.length, seq, 
								clientAddr,	"");
					}
					// Use DataIndication to forward the data
					else {
						byte [] info = new byte [packet.getLength()];
						System.arraycopy(packet.getData(), 0, info, 0, packet.getLength());
						StunMessage data = factory.createDataIndication(packet.getAddress(),
								packet.getPort(), info, transactionID, 
								turnStack.includeFingerPrint());
						if (data != null) {
							relayTransport.sendMessage(data, seq, clientAddr);
						}
					}
				}
				else {
					// Drop the message
				}
			}	
			catch (SocketException ex) {
				if (isRunning) {
					// Something wrong has happened
					logger.warn(PC2LogCategory.STUN, subCat,
							"The StunMessageProcessor has gone useless:", ex);
					stop();
				}
				else				{
					//The exception was most probably caused by calling stop()
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
	
	public void sendRawMessage(byte [] data, int length, int seq, 
			InetSocketAddress peerAddress) throws IOException	{
	
		DatagramPacket datagramPacket = new DatagramPacket(data, length, peerAddress);
		synchronized(socketLock){
			udpSock.send(datagramPacket);
			if (logger.isDebugEnabled(PC2LogCategory.STUN, subCat)) {
					logger.debug(PC2LogCategory.SIP, subCat,
					"<<<<< TX:\tLength = " + length
					+ "\nSent from IP|Port=" 
					+ udpSock.getLocalAddress().getHostAddress() 
					+ "|" + udpSock.getLocalPort() 
					+ "\nTo IP|Port=" 
					+ peerAddress// + "|" + peerPort
					+ "\nSequencer=" + seq
					+ "\nTransport=" + Transport.UDP
					+ "\n[" + Conversion.hexString(data) + "]");
			}
		}
	}
	
	/**
	 * Start the network listening thread.
	 *
	 * @throws IOException if we fail to setup the socket.
	 */
	public void start()	throws IOException	{
		synchronized(socketLock){
			if (udpSock == null) {
				this.udpSock = new DatagramSocket(myAddr);
				logger.info(PC2LogCategory.STUN, subCat,
					"TURN Allocation bound to socket " + myAddr.toString());
				}			
				udpSock.setReceiveBufferSize(MAX_DATAGRAM_SIZE);
			}
		
		
		if (udpSock != null) {
			if (timerTask != null)
				timerTask = null;
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
			timer = new Timer("Allocation Timer" + myAddr, true);
			timerTask= new AllocationTask(this);
			timer.schedule(timerTask, (TTL * 1000));
			logger.debug(PC2LogCategory.STUN, subCat,
					"Starting allocation timer(" + timer + ") for " + 
					(TTL * 1000) + " msecs.");
			thread = new Thread(this, "TURN Allocation - UDP" + udpSock.getLocalAddress().toString() + ":" + udpSock.getLocalPort());
			thread.setDaemon(true);
			thread.start();	
			this.isRunning = true;
		}
		
		
	}
	
	public synchronized void stop() {
		// First see if we have a mate and stop it
		if (mate != null)
			mate.stop();
		this.isRunning = false;
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (udpSock != null)  {
			synchronized (socketLock) {
				udpSock.close();
			}
			logger.info(PC2LogCategory.STUN, subCat,
					"TURN Allocation closed UDP socket on " + myAddr.toString());
			udpSock = null;
		}
	}
	

	public void setRelayTransport(StunMessageProcessor processor) {
		this.relayTransport = processor;
	}
	public void setTimeToLive(int sec) {
		this.timeToLive = System.currentTimeMillis() + (sec * 1000);
	}
	

	/**
	 * Returns a String representation of the object.
	 * @return a String representation of the object.
	 */
	public String toString() {
		return "TURN Allocation "
		+ (isRunning ? "isRunning":"not Running")
		+ "on address" + myAddr.toString();
	}
}
