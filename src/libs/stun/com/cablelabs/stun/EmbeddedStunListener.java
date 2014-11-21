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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.ListIterator;
import com.cablelabs.log.*;


public class EmbeddedStunListener implements Runnable {

	/**
	 * The single instantiation of the class.
	 */
	private static EmbeddedStunListener listener = null;
	
	/**
	 * This thread
	 */
	private Thread runningThread = null;
	
	/**
	 * The Stun Stack to process messages from a socket
	 */
	private StunStack stack = StunStack.getInstance();
	
	/**
	 * A list of Stun Message Processors that we have requested
	 * of the STUN Stack.
	 */
	private LinkedList<StunMessageProcessor> queue = null;
		
	/**
	 * Indicates whether the listener has been started.
	 */
	private boolean isRunning = false;
	
	/**
	 * Logger
	 */
	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger(StunDistributor.class);
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Stack";
	
	/**
	 * Private constructor as we want a singleton pattern.
	 */
	private EmbeddedStunListener()	{
		queue = new LinkedList<StunMessageProcessor>();
	}
	/**
	 * Returns a reference to the singleton StunListener instance. If the listener
	 * has not yet been initialized, a new instance will be created.
	 *
	 * @return a reference to the StunListener.
	 */
	public static synchronized EmbeddedStunListener getInstance()
	{
		if (listener == null) 
			listener = new EmbeddedStunListener();
		return listener;
	}
	
	/**
	 * Start the listener in it's own thread
	 *
	 */
	void start()
	{
		this.isRunning = true;
		
		runningThread = new Thread(this, "StunListener");
		runningThread.start();
	}
	
	/**
	 * 
	 */
	public void run()
	{
		while (isRunning) {
			try {
				Thread.sleep(200);
			}

			catch(Throwable err)
			{
				logger.warn(PC2LogCategory.STUN, subCat,
						"The StunListener encountered an error while trying to sleep.");
			}
		}
	}
	
	/**
	 * Clean up the NetAccessPoint threads we created and 
	 * started and then terminate our own thread.
	 *
	 */
	void stop()
	{
		ListIterator<StunMessageProcessor> iter = queue.listIterator();
		while (iter.hasNext()) {
			StunMessageProcessor smp = (StunMessageProcessor)iter.next();
			smp.stop();
		}
		
		queue.clear();
		this.isRunning = false;
		runningThread.interrupt();
	}
	
	/**
	 * Creates a new Network Access Point for an external socket.
	 * 
	 * @param sock - the external UDP socket.
	 * @return - the StunMessageProcessor to deliver the packet to for
	 * 			processing when the packet is a STUN message.
	 */
	public synchronized StunMessageProcessor createStunAccessPoint(DatagramSocket sock) {
		try {
			StunMessageProcessor smp = new StunMessageProcessor(stack.getQueue(), sock);
			stack.addEmbeddedProcessor(smp);
			queue.addLast(smp);
			
			//smp.start();
			
			return smp;
		}
		catch (Exception e) {
			logger.warn(PC2LogCategory.STUN, subCat,
					"Stun Listener could not create embedded stun processor.", e);
		}
		return null;
	}
	
	/**
	 * Creates a new StunMessageProcessor for an external socket.
	 * 
	 * @param sock - the external TCP socket.
	 * @return - the StunMessageProcessor to deliver the packet to for
	 * 			processing when the packet is a STUN message.
	 */
	public synchronized StunMessageProcessor createStunAccessPoint(Socket sock) {
		try {
			StunMessageProcessor smp = new StunMessageProcessor(stack.getQueue(), sock);
			stack.addEmbeddedProcessor(smp);
			queue.addLast(smp);
			//smp.start();
			
			return smp;
		}
		catch (Exception e) {
			logger.warn(PC2LogCategory.STUN, subCat,
					"Stun Listener could not create embedded stun processor.", e);
		}
		return null;
	}
	
	/**
	 * This method test whether the packet is a STUN packet or some
	 * other protocol. The rules are the first byte must be 0x00 and 
	 * the magic cookie must appear in bytes 4-7.
	 * 
	 * @param packet - the packet to evaluate
	 * @return - true if it is a STUN packet, false otherwise
	 */
	public boolean isStunPacket(DatagramPacket packet) {
		return isStunPacket(packet.getData(), packet.getLength());
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

}
