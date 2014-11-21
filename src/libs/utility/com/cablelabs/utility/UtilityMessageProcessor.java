/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.utility;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetSocketAddress;

import com.cablelabs.common.Transport;
import com.cablelabs.log.*;

import java.util.concurrent.ConcurrentLinkedQueue;

public class UtilityMessageProcessor implements Runnable {
	
	private static final LogAPI logger = LogAPI.getInstance();
		//Logger.getLogger(UtilityStack.class);
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Stack";
	
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
	private ConcurrentLinkedQueue<UtilityRawMessage> queue = null;
	
	/**
	 * The socket object that used by this access point to access the network.
	 */
	protected DatagramSocket sock;
	
	/**
	 * A flag that is set to false to exit the message processor.
	 */
	private boolean isRunning;
	
	/**
	 * Used for locking socket operations
	 */
	private Object socketLock = new Object();
	
	
	public UtilityMessageProcessor(ConcurrentLinkedQueue<UtilityRawMessage> queue, 
			InetSocketAddress address) {
		this.queue = queue;
		this.address = address;
	}
	
	
	/**
	 * Start the network listening thread.
	 *
	 * @throws IOException if we fail to setup the socket.
	 */
	public void start()
	throws IOException
	{
		synchronized(socketLock){
			
			//do not create the socket earlier as someone might want to set an
			// existing one 
			if (sock == null)
			{
				this.sock = new DatagramSocket(address);
				logger.info(PC2LogCategory.UTILITY, subCat,
						"UtilityProcessor bound to socket " + address.toString());
			}
			
			sock.setReceiveBufferSize(MAX_DATAGRAM_SIZE);
			this.isRunning = true;
			Thread thread = new Thread(this, "UTILITY - UDP" + sock.getLocalAddress() + ":" + sock.getLocalPort());
			thread.setDaemon(true);
			thread.start();
		}
	}
	
	
	/**
	 * The listening thread's run method.
	 */
	public void run() {
		while (this.isRunning) {
			try	{
				int bufsize = sock.getReceiveBufferSize();
				byte message[] = new byte[bufsize];
				DatagramPacket packet = new DatagramPacket(message, bufsize);
				// System.out.println("received " + new String(message));
				sock.receive(packet);
				
				UtilityRawMessage rawMessage = new UtilityRawMessage( message,
						packet.getLength(), packet.getAddress(), packet.getPort(),
						address);
				
				synchronized (queue) {
					queue.add(rawMessage);
					queue.notify();
				}
				
			}
			
			catch (SocketException ex)	            {
				if (isRunning)				{
//					Something wrong has happened
					logger.warn(PC2LogCategory.UTILITY, subCat,
							"The UtilityMessageProcessor has gone useless:", ex);
					
					stop();
				}
				else				{
					//The exception was most probably caused by calling this.stop()
					// ....
				}
			}
			catch (IOException ex) {
				logger.warn(PC2LogCategory.UTILITY, subCat,
						"The UtilityMessageProcessor has gone useless:", ex);
			}
			catch (Throwable ex) {
				logger.warn(PC2LogCategory.UTILITY, subCat,
						"The UtilityMessageProcessor has gone useless:", ex);
				
				stop();
			}
		}
	}
	
	public synchronized void stop() {
		this.isRunning = false;
		if (sock != null)  {
			synchronized (socketLock) {
				sock.close();
				
				logger.info(PC2LogCategory.UTILITY, subCat,
						"UtilityMessageProcessor closed socket on " + address.toString());
				sock = null;
				
			}
		}
	}
	

	void sendMessage(UtilityMessage message, int seq, InetSocketAddress peerAddress)	
		throws IOException	{
		//logger.trace("Sending utility message(" + message.toString() +") to " + address.toString() + "\n");
		String data = message.encode();
		DatagramPacket datagramPacket = new DatagramPacket(
				data.getBytes(), data.length(), peerAddress);
		synchronized(socketLock){
			if (sock != null ) {
				sock.send(datagramPacket);
			// PC 2.0 add logging statement for all SIP messages received on the s
			// socket
            logger.info(PC2LogCategory.UTILITY, subCat,
            		"<<<<< TX:\tLength = " + data.length() 
            		+ "\nSent from IP|Port=" 
            		+ address.getAddress().getHostAddress() 
            		+ "|" + address.getPort()  
            		+ "\nTo IP|Port=" 
            		+ peerAddress// + ":" + peerPort
            		+ "\nSequencer=" + seq
            		+ "\nTransport=" + Transport.UDP
            		+ "\n[" + new String(data.getBytes(), 0, data.length()) + "]");
			}
			else {
				logger.error(PC2LogCategory.UTILITY, subCat, "The utility stack could not send " 
						+ "\n[" + new String(data.getBytes(), 0, data.length()) + "] because the socket is null, address=" + address); 
			}
		}
	}
	
	/**
	 * Returns a String representation of the object.
	 * @return a String representation of the object.
	 */
	public String toString() {
		return "UtilityMessageProcessor "
		+ (isRunning ? "isRunning":"not Running")
		+ "on address" + address.toString();
	}
	
	
}
