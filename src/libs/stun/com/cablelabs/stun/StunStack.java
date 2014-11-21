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
import java.util.concurrent.ConcurrentLinkedQueue;
import com.cablelabs.log.*;
import com.cablelabs.stun.RawData;
import com.cablelabs.common.*;

import java.util.*;

public class StunStack implements Runnable {

	private Hashtable<Integer, StunMessageProcessor> processors = new Hashtable<Integer, StunMessageProcessor>();
	
	/**
	 * This table is indexed upon the local socket address for easy lookup.
	 */
	private Hashtable<InetSocketAddress, StunMessageProcessor> socketIndex = 
		new Hashtable<InetSocketAddress, StunMessageProcessor>();
	private StunListener listener = null;
	
	private RTPListener rtpListener = null;
	
	private static StunStack stack = null;

	private ConcurrentLinkedQueue<RawData> queue = new
		ConcurrentLinkedQueue<RawData>();
	
	private Thread thread = null;
	/**
	 * A flag that is set to false to exit the stack.
	 */
	private boolean isRunning;
	
	private static final LogAPI logger = LogAPI.getInstance();
		// Logger.getLogger(StunConstants.loggerName);
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Stack";
	
	/**
	 * This is a table of all of the active channel bindings in the
	 * server.
	 */
	private HashSet<Character>channelBindings = new HashSet<Character>();
	
	private boolean fingerPrintFlag = false;
	
	private boolean displayCompressedForm = false;
	
	private StunStack () {
	}
	
	public static StunStack getInstance() {
		if (stack == null)	{
			stack = new StunStack();
		}
		
		return stack;
	}
	
	public boolean addBinding(Character channel) {
		if (!channelBindings.contains(channel)) {
			channelBindings.add(channel);
			return true;
		}
		return false;
	}
	
	public void addEmbeddedProcessor(StunMessageProcessor smp) {
		processors.put(smp.getID(), smp);
	}
	
	public void createProcessor(String ip, int port, String threadName) {
		InetSocketAddress addr = new InetSocketAddress(ip, port);
		StunMessageProcessor smp = new StunMessageProcessor(queue, addr, false, threadName);
		try {
			processors.put(smp.getID(), smp);
			socketIndex.put(addr, smp);
			smp.start();
		}
		catch (IOException io) {
			logger.error(PC2LogCategory.STUN, subCat,
					"StunStack could not create new StunMessageProcessor for IP=" 
					+ ip + " and port=" + port, io);
		}
		catch (NullPointerException npe) {
			logger.error(PC2LogCategory.STUN, subCat,
					"StunStack could not create new StunMessageProcessor for IP=" 
					+ ip + " and port=" + port, npe);
		}
	}
	
	public boolean includeFingerPrint() {
		return this.fingerPrintFlag;
	}
	
	public int getProcessorID(InetSocketAddress addr) {
		StunMessageProcessor smp = socketIndex.get(addr);
		if (smp != null)
			return smp.getID();
		return -1;
	}
	
	public StunMessageProcessor getProcessor(int id) {
		return processors.get(id);
	}
	
	public boolean hasBinding(Character channel) {
		if (channelBindings.contains(channel))
			return true;
		return false;
	}
	
	public ConcurrentLinkedQueue<RawData> getQueue() {
		return this.queue;
	}
	
	public StunEvent parse(RawData rawData, byte [] data, int length) {
		try {
			
			StunMessage message = StunMessage.decode(data, 
					0, length);
			if (message != null) {
				StunEvent event = new StunEvent(message, rawData);
				logger.info(PC2LogCategory.STUN, subCat,
						">>>>> RX:\tLength = " 
	            		+ length 
	            		+ "\nReceived on IP|Port=" 
	            		+ rawData.getDestIP() 
	            		+ "|" + rawData.getDestPort()  
	            		+ "\nFrom IP|Port=" + rawData.getSrcIP()
	            		+ "|" + rawData.getSrcPort()
	            		+ "\nSequencer=" + rawData.getSequencer()
	            		+ "\nTransport=" + rawData.getTransport().toString()
	            		+ (stack.useCompressedForm() 
	            				? ("\n" + message.getName()) 
	            				: ("\n[" + message.toString() + "]")));
				return event;
			}
		}
		catch (StunException ia) {
			logger.warn(PC2LogCategory.STUN, subCat,
					"StunStack failed parsing of data=[" 
					+ Conversion.formattedHexString(data) 
					+ "] because [" + ia.getMessage() + "]."
					+ "\nReceived on IP|Port=" 
            		+ rawData.getDestIP() 
            		+ "|" + rawData.getDestPort()  
            		+ "\nFrom IP|Port=" + rawData.getSrcIP()
            		+ "|" + rawData.getSrcPort()
            		+ "\nTransport=" + rawData.getTransport().toString());
		}
		
		return null;
	}

	public boolean removeBinding(Character channel) {
		if (channelBindings.contains(channel)) {
			channelBindings.remove(channel);
			return true;
		}
		return false;
	}


	public void run() {
		while (this.isRunning) {
			try	{
				RawData rawData = null;
				synchronized (queue) {
					while (queue.isEmpty()) {
						// Check to see if we need to exit.
						if (!isRunning)
							return;
						
						try {
							queue.wait();
						} 
						catch (InterruptedException ex) {
							if (!isRunning)
								return;
						}
					}
					rawData = (RawData)queue.remove();
				}
				
				Object data = rawData.getData();
				if (data instanceof ByteArray) {
					ByteArray ba = (ByteArray)data;
					if (StunMessageProcessor.isStunPacket(ba.getBuffer(), 
							ba.length())) {
						StunEvent event = parse(rawData, ba.getBuffer(), ba.length());
						if (event != null && listener != null) {
							listener.processEvent(event); 
						}
					}
					else {
						// Get the first two bytes and see if they
						// result in a ChannlNumber that is currently bound.
						Character channel = Conversion.getChar(ba.getBuffer(), 0);
						// (char)(((raw[0]<<8) & 0xFF00) | (raw[1]&0xFF));
						if (channelBindings.contains(channel)) {
							logger.info(PC2LogCategory.STUN, subCat,
									">>>>> RX:\tLength = " 
									+ rawData.length()
									+ "\nReceived on IP|Port=" 
									+ rawData.getDestIP() 
									+ "|" + rawData.getDestPort()  
									+ "\nFrom IP|Port=" + rawData.getSrcIP()
									+ "|" + rawData.getSrcPort()
									+ "\nSequencer=" + rawData.getSequencer()
									+ "\nTransport=" + rawData.getTransport().toString()
									+ "\n[ ChannelData[" 
									+ Conversion.hexString(ba.getBuffer()) + "]");
							listener.processChannelDataEvent(rawData);
						}
						else if (StunMessageProcessor.isRTPPacket(ba.getBuffer())) {
							if (rawData != null && rtpListener != null)
								rtpListener.processEvent(rawData);
						}
					}
				}
			}
			catch (Exception ex) {
				logger.error(PC2LogCategory.STUN, subCat,
						"StunStack encountered an error.", ex);
			}
		}
	}
	
	           

	
	public void start()	throws IOException	{
		try {
				this.isRunning = true;
				thread = new Thread(this, "StunStack");
				thread.setDaemon(true);
				thread.start();
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.STUN, subCat,
						"StunStack encountered an error when starting the StunMessageProcessor.", ex);
			}
	}
	
	public void sendMessage(int processorID, StunMessage message, int seq,
			InetSocketAddress remoteAddress) {
		StunMessageProcessor processor = processors.get(processorID);
		if (processor != null) {
			try {
				processor.sendMessage(message, seq, remoteAddress);
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.STUN, subCat,
						"StunStack encountered an error when trying to send Stun message", ex);
			}
		}
		else 
			logger.warn(PC2LogCategory.STUN, subCat,
					"StunStack could not find the processor for id=" 
				+ processorID + ", message dropped."); 
	}
	
	public void sendRawMessage(int processorID, byte [] msg, int length, 
			int seq, InetSocketAddress remoteAddress, String sub) {
		StunMessageProcessor processor = processors.get(processorID);
		if (processor != null) {
			try {
				processor.sendRawMessage(msg, length, seq, remoteAddress, sub);
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.SIP, "RTP",
						"STUN Stack encountered an error when trying to send Stun message", ex);
			}
		}
		else {
			logger.warn(PC2LogCategory.SIP, "RTP",
					"STUN Stack could not find the processor for id=" 
				+ processorID + ", message dropped."); 
		}
	}

	public void setDisplayCompressedForm(boolean flag) {
		this.displayCompressedForm = flag;
	}
	
	public void setFingerPrint(boolean flag) {
		this.fingerPrintFlag = flag;
		logger.debug(PC2LogCategory.STUN, subCat, 
				"STUN Stack changing the include FingerPrint setting to " 
				+ this.fingerPrintFlag);
	}

	public void setListener(StunListener listener) {
		this.listener = listener;
	}
	
	public void setRTPListener(RTPListener listener) {
		this.rtpListener = listener;
	}
	
	public void stop() {
		if (processors != null) {
			Enumeration<StunMessageProcessor> iter = processors.elements();
			while (iter.hasMoreElements()) {
				StunMessageProcessor smp = (StunMessageProcessor)iter.nextElement();
				smp.stop();
			}
		}
		this.isRunning = false;
		thread.interrupt();
	}

	public void stop(String ip, int port) {
		if (processors != null) {
			InetSocketAddress addr = new InetSocketAddress(ip, port);
			Enumeration<StunMessageProcessor> iter = processors.elements();
			while (iter.hasMoreElements()) {
				StunMessageProcessor smp = (StunMessageProcessor)iter.nextElement();
				if (smp.getAddress().equals(addr))
					smp.stop();
			}
			
		}
	}
	
	public boolean useCompressedForm() {
		return this.displayCompressedForm;
	}
}
