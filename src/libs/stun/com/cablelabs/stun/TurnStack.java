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
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.cablelabs.common.Transport;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.stun.attributes.ChannelNumber;
import com.cablelabs.stun.attributes.PeerAddress;

public class TurnStack implements Runnable {

	private Hashtable<String, Allocation> allocations = new Hashtable<String, Allocation>();
	private Hashtable<Integer, Allocation> allocationPortIndex = new Hashtable<Integer, Allocation>();
	private Hashtable<Integer, Allocation> deletingAllocations = new Hashtable<Integer, Allocation>();
	
	/**
	 * This table is indexed upon the local socket address for easy lookup.
	 */
//	private Hashtable<InetSocketAddress, StunMessageProcessor> socketIndex = 
//		new Hashtable<InetSocketAddress, StunMessageProcessor>();
//	private AllocationListener listener = null;
	
//	private ConcurrentLinkedQueue<RawMessage> queue = new
//		ConcurrentLinkedQueue<RawMessage>();
	private ConcurrentLinkedQueue<Integer> expiredQueue = new
		ConcurrentLinkedQueue<Integer>();
	
	private Thread thread = null;
	/**
	 * A flag that is set to false to exit the stack.
	 */
	private boolean isRunning;
	
	private static final LogAPI logger = LogAPI.getInstance();
	
	private static TurnStack stack = null;
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Stack";
	
	private int maxNumPorts = 0;
	private int initPort = 0;
	private String localIP = null;
	private boolean [] ports = null;
	
	private static StunStack stunStack = null;
	
	private TurnStack (String ipAddr, int initialPort, int totalNumPorts) {
		if (stunStack == null) {
			this.localIP = ipAddr;
			this.initPort = initialPort;
			this.maxNumPorts = totalNumPorts;
			this.ports = new boolean [maxNumPorts];
			stunStack = StunStack.getInstance();
		}
	}
	
	public static TurnStack getInstance(String ipAddr, int initialPort, int totalNumPorts) 
		throws IllegalArgumentException{
		if (stack == null)	{
			stack = new TurnStack(ipAddr, initialPort, totalNumPorts);
		}
		
		return stack;
	}
	
	public static TurnStack getInstance() {
		return stack;
		
	}
	
	private int assignPort() {
		for (int i = 0; i < maxNumPorts; i+=2) {
			if (!ports[i]) {
				ports[i] = true;
				return (initPort + i);
			}
		}
		return -1;
	}
//	public void setListener(AllocationListener listener) {
//		this.listener = listener;
//	}
	
	public Allocation addChannelBind(String key, PeerAddress pa, ChannelNumber cn) {
		Allocation a = allocations.get(key);
		if (a != null) {
			if (a.addBinding(pa, cn)) {
				Character channel = cn.getChannel();
				//channelBindings.put(channel, a);
				stunStack.addBinding(channel);
				return a;
			}
			
		}
		return null;
	}

	public Allocation createAllocation(StunMessageProcessor smp, 
			String key, String user, StunEvent se, 
			Byte propType, Transport t) {
		int port = assignPort();
		if (port != -1) {
			try {
				Allocation a = new Allocation(this, smp, expiredQueue, localIP, port,
						key, user, se, propType, t);
				int count = 0;
				if (a != null) {
					count++;
					allocationPortIndex.put(port, a);
					allocations.put(key, a);
					Allocation mate = a.getMate();
					if (mate != null) {
						allocationPortIndex.put(port+1, mate);
						count++;
					}
					a.start();
					logger.info(PC2LogCategory.STUN, subCat, 
							"TURN Stack successfully allocated " + count + " new " 
							+ (count > 1 ? "ports." : "port." ));
					// Let the parent spawn the child so they can share a single
					// TimerTask
//					if (propType == RequestedProps.PAIR_OF_PORTS) {
//					int port2 = port - initPort + 1;
//					if (!ports.get(port2)) {
//					ports.set(port2, true);
//					Allocation a2 = new Allocation(queue, localIP, port2,
//					key, user, msg, propType, t);
//					if (a2 != null) {
//					a.setMate(a2);
//					a2.start();
//					}
//					}
//					}
					return a;
				}
			}
			catch (IOException io) {
				logger.error(PC2LogCategory.STUN, subCat,
						"TURN Stack could not create a new Allocation for IP=" 
						+ localIP + " and port=" + port, io);
			}
			catch (NullPointerException npe) {
				logger.error(PC2LogCategory.STUN, subCat,
						"TURN Stack could not create a new Allocation for IP=" 
						+ localIP + " and port=" + port, npe);
			}
		}
		return null;
	}
	public void deleteAllocation(String key) {
		// First see if we need to move the allocation to the deleted list
		Allocation a = allocations.remove(key);
		int count = 0;
		if (a != null) {
			count++;
			allocationPortIndex.remove(a.getLocalPort());
			// deletingAllocations.put(index, a);
			Allocation mate = a.getMate();
			int matePortKey = -1;
			if (mate != null) {
				allocationPortIndex.remove(mate.getLocalPort());
				// deletingAllocations.put(index,mate);
				matePortKey = mate.getLocalPort()-initPort;
				count++;
			}
			// Lastly clean up the port information
			int portKey = a.getLocalPort() - initPort;
			ports[portKey] = false;
			
			if (matePortKey != -1) {
				ports[matePortKey] = false;
			}
			logger.info(PC2LogCategory.STUN, subCat, 
					"TURN Stack successfully released " + count + " new " 
					+ (count > 1 ? "ports." : "port." ));
		}

	}
	
	public Allocation getAllocation(String key) {
		return allocations.get(key);
	}
	
	public boolean includeFingerPrint() {
		if (stunStack != null) 
			return stunStack.includeFingerPrint();
		return false;
	}
	
	public Allocation isDeletedAllocation(String key) {
		Enumeration<Allocation> allocs = deletingAllocations.elements();
		while (allocs.hasMoreElements()) {
			Allocation a = allocs.nextElement();
			if (a.getKey().equals(key))
				return a;
		}
		return null;
	}

	public Allocation removeChannelBind(String key, PeerAddress pa, ChannelNumber cn) {
		Allocation a = allocations.get(key);
		if (a != null) {
			if (a.removeBinding(pa, cn)) {
				Character channel = cn.getChannel();
				stunStack.removeBinding(channel);
				return a;
			}
			
		}
		return null;
	}

	public void run() {
		while (this.isRunning) {
			try	{
				int index = -1;
				synchronized (expiredQueue) {
					while (expiredQueue.isEmpty()) {
						// Check to see if we need to exit.
						if (!isRunning)
							return;
						
						try {
							expiredQueue.wait();
						} 
						catch (InterruptedException ex) {
							if (!isRunning)
								return;
						}
					}
					index = (Integer)expiredQueue.remove();
				}
				
				// First see if we need to move the allocation to the deleted list
				Allocation a = allocationPortIndex.remove(index);
				int count = 0;
				if (a != null) {
					count++;
					allocations.remove(a.getKey());
					deletingAllocations.put(index, a);
					Allocation mate = a.getMate();
					if (mate != null) {
						allocationPortIndex.remove(index+1);
						deletingAllocations.put(index,mate);
						count++;
					}
					logger.info(PC2LogCategory.STUN, subCat, 
							"TURN Stack successfully releasing " + count + " new " 
							+ (count > 1 ? "ports." : "port." ));
				}
				else {
					a = deletingAllocations.remove(index);
					if (a != null) {
						Allocation mate = a.getMate();
						int matePortKey = -1;
						if (mate != null) {
							deletingAllocations.remove(index+1);
							matePortKey = index+1-initPort;
						}
						// Lastly clean up the port information
						int portKey = index - initPort;
						ports[portKey] = false;
						if (matePortKey != -1) {
							ports[matePortKey] = false;
						}
						a.stop();
						logger.info(PC2LogCategory.STUN, subCat, 
								"TURN Stack successfully released " + count + " new " 
								+ (count > 1 ? "ports." : "port." ));
					}
					
				}
			}
			catch (Exception ex) {
				logger.error(PC2LogCategory.STUN, subCat,
						"TURN Stack encountered an error.", ex);
			}
		}
	}
	
	public void sendRawMessage(String key, byte [] msg, int length, 
			int seq, InetSocketAddress remoteAddress) {
		Allocation a = allocations.get(key);
		if (a != null) {
			try {
				a.sendRawMessage(msg, length, seq, remoteAddress);
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.STUN, subCat,
						"TURN Stack encountered an error when trying to send data over relay transport.", ex);
			}
		}
		else {
			logger.warn(PC2LogCategory.STUN, subCat,
					"TURN Stack could not find the Allocation for key=" 
				+ key + ", message dropped."); 
		}
	}

	public void start()	throws IOException	{
		try {
				this.isRunning = true;
				thread = new Thread(this, "TURN Stack");
				thread.setDaemon(true);
				thread.start();
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.STUN, subCat,
						"TURN Stack encountered an error when starting.", ex);
			}
	}

	public void stop() {
		if (allocations != null) {
			Enumeration<Allocation> iter = allocations.elements();
			while (iter.hasMoreElements()) {
				Allocation a = (Allocation)iter.nextElement();
				Allocation mate = a.getMate();
				if (mate != null)
					mate.stop();
				a.stop();
			}
		}
		this.isRunning = false;
		if (thread != null)
			thread.interrupt();
	}

	public boolean useCompressedForm() {
		return stunStack.useCompressedForm();
	}
}
