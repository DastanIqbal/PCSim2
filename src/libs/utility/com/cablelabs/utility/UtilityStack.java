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
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.cablelabs.log.*;
import com.cablelabs.common.*;

public class UtilityStack implements Runnable {

	private UtilityMessageProcessor processor = null;
	
	private UtilityListener listener = null;
	
	private InetSocketAddress address = null;

	private ConcurrentLinkedQueue<UtilityRawMessage> queue = new
		ConcurrentLinkedQueue<UtilityRawMessage>();
	
	private Thread thread = null;
	/**
	 * A flag that is set to false to exit the stack.
	 */
	private boolean isRunning;
	
	private static LogAPI logger = LogAPI.getInstance();
		// Logger.getLogger(UtilityStack.class);
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Stack";
	
	/** 
	 * A array of error messages that the parsing operation may produce to help
	 * identify the location of the parsing failure for the Utility protocol.
	 */
	private final String [] parsingErr = {
			"Unknown.", // index 0
			"Parsing Utility header's version field.", //  index 1
			"Parsing Utility header's message type field.", //  index 2
			"Parsing Utility header's transaction id field.",//  index 3
			"Parsing Utility header's body length field.",//  index 4
			"Converting body length field to an integer.",//  index 5
			"Comparing actual body length to the body length field.", //  index 6
			"Parsing the tag field of the body.",  //  index 7
			"Parsing the length field of the body.",  //  index 8
			"Parsing the value field of the body."   //  index 9
	};
	
	public UtilityStack(InetSocketAddress address, UtilityListener listener) {
		this.listener = listener;
		this.address = address;
	}
	
	public void start()	throws IOException	{
		if (address != null) {
			processor = new UtilityMessageProcessor(queue, address);
			try {
				processor.start();
				this.isRunning = true;
				thread = new Thread(this, "UtilityStack");
				thread.setDaemon(true);
				thread.start();
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.UTILITY, subCat,
						"UtilityStack encountered error when starting the UtilityMessageProcessor.", ex);
				processor.stop();
			}
		}
	}

	public void run() {
		while (this.isRunning) {
			try	{
				UtilityRawMessage rawMsg = null;
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
					rawMsg = (UtilityRawMessage)queue.remove();
				}
				
				UtilityEvent event = parse(rawMsg);
				if (event != null && listener != null) {
					// PC 2.0 add logging statement for all SIP messages received on the s
					// socket
		            logger.info(PC2LogCategory.UTILITY, subCat,
		            		">>>>> RX:\tLength = " 
		            		+ rawMsg.getMessageLength() 
		            		+ "\nReceived on IP|Port=" 
		            		+ rawMsg.getLocalAddress().getAddress().getHostAddress() 
		            		+ "|" + rawMsg.getLocalAddress().getPort()  
		            		+ "\nFrom IP|Port=" + rawMsg.getRemoteAddress().getAddress().getHostAddress()
		            		+ "|" + rawMsg.getRemoteAddress().getPort()
		            		+ "\nSequencer=" + event.getSequencer()
		            		+ "\nTransport=" + Transport.UDP
		            		+ "\n[" + event.getMessage().toString() + "]");
		            listener.processEvent(event); 
				}
				else if (event == null) {
				  logger.warn(PC2LogCategory.UTILITY, subCat,
						  "Failed parsing Utility Message" +
				  			">>>>> RX:\tLength = " 
		            		+ rawMsg.getMessageLength() 
		            		+ "\nReceived on IP|Port=" 
		            		+ rawMsg.getLocalAddress().getAddress().getHostAddress() 
		            		+ "|" + rawMsg.getLocalAddress().getPort()  
		            		+ "\nFrom IP|Port=" + rawMsg.getRemoteAddress().getAddress().getHostAddress()
		            		+ "|" + rawMsg.getRemoteAddress().getPort()
		            		+ "\nTransport=" + Transport.UDP
		            		+ "\n[" 
		            		+ new String(rawMsg.getMessage(), 0, rawMsg.getMessageLength()) + "]");
				}
				else {
					logger.warn(PC2LogCategory.UTILITY, subCat,
							"Failed to deliver packet because there is not listener" 
							+">>>>> RX:\tLength = " 
		            		+ rawMsg.getMessageLength() 
		            		+ "\nReceived on IP|Port=" 
		            		+ rawMsg.getLocalAddress().getAddress().getHostAddress() 
		            		+ "|" + rawMsg.getLocalAddress().getPort()  
		            		+ "\nFrom IP|Port=" 
		            		+ rawMsg.getRemoteAddress().getAddress().getHostAddress()
		            		+ "|" + rawMsg.getRemoteAddress().getPort()	
		            		+ "\nTransport=" + Transport.UDP
		            		+ "\n[" 
		            		+ new String(rawMsg.getMessage(), 
		            				0, rawMsg.getMessageLength()) + "]");
				}
			}
			catch (Exception ex) {
				logger.error(PC2LogCategory.UTILITY, subCat,
						"UtilityStack encountered an error.", ex);
			}
		}
	}
	
	           
	private UtilityEvent parse(UtilityRawMessage rawMsg) {
		if (rawMsg != null) {
			UtilityEvent event = new UtilityEvent();
			byte [] msg = rawMsg.getMessage();
			int msgLen = rawMsg.getMessageLength();
			if (msg.length == msgLen) {
				if (parseUtilityHeader(msg, event)) {
					event.setSequencer(LogAPI.getSequencer());
					event.setDestIP(rawMsg.getLocalAddress().getAddress().getHostAddress());
					event.setDestPort(rawMsg.getLocalAddress().getPort());
					event.setSrcIP(rawMsg.getRemoteAddress().getAddress().getHostAddress());
					event.setSrcPort(rawMsg.getRemoteAddress().getPort());
					return event;
				}
				else {
					logger.warn(PC2LogCategory.UTILITY, subCat,
							"Parsing failed for utility message.");
				}
			}
			else {
				logger.warn(PC2LogCategory.UTILITY, subCat,
						"Utility message length was not correct, dropping packet.");
			}
		}
		return null;
	}
	
	private boolean parseUtilityHeader(byte [] msg, UtilityEvent event) {
		int errIndex = 0;
		try {
			String buffer = new String(msg);
			String pc = buffer.substring(0,2);
			// This is a string to provide a more meaningful error response
			// if something fails or throws an exception while parsing
			errIndex = 1;
			if (pc.equals("PC")) {
				int start = buffer.indexOf(" ") + 1;
				int end = buffer.indexOf(" ", start);
				String version = buffer.substring(start,end);
				errIndex = 2;
				start = end+1;
				end = buffer.indexOf(" ", start);
				String type = buffer.substring(start, end);
				errIndex = 3;
				start = end+1;
				end = buffer.indexOf(" ", start);
				String transID = buffer.substring(start,end);
				errIndex = 4;
				start = end+1;
				end = buffer.indexOf(" ", start);
				String msgLen = null;
				String body = null;
				if (end == -1 && start+1 == buffer.length()) {
					// This means there is no body, but the message
					// is formatted properly according to the protocol.
					msgLen = buffer.substring(start);
					UtilityMessage um = new UtilityMessage(type, transID, version);
					event.setMessage(um);
			    	return true;
				}
				else {
				    msgLen = buffer.substring(start, end);
				    body = buffer.substring(end+1, buffer.length());
				    errIndex = 5;
				    int ml = Integer.parseInt(msgLen);
				    errIndex = 6;
				    if (ml == body.length()) {
				    	// Now the remainder of the message should be in 
				    	// TLV format with spaces separating individual fields
				    	UtilityMessage um = new UtilityMessage(type, transID, version);
				    	int offset = 0;
				    	while(offset < body.length()) {
				    		int tagIndex = body.indexOf(" ", offset);
				    		int lenIndex = body.indexOf(" ", tagIndex+1);
				    		errIndex = 7;
				    		String tag = body.substring(offset, tagIndex);
				    		errIndex = 8;
				    		int length = Integer.parseInt(body.substring(tagIndex+1,lenIndex));
				    		errIndex = 9;
				    		String value = body.substring(lenIndex+1, ((lenIndex+1)+length));
				    		if (length == value.length()) {
				    			UtilityAttribute ua = null;
				    			if (tag.equals("array")) {
				    				ua = new UtilityArrayAttribute(tag, value);
				    			}
				    			else {
				    				ua = new UtilityAttribute(tag, value);
				    			}
				    			if (ua != null)
				    				um.addAttribute(ua);
//				    			else
//				    				logger.warn(PC2LogCategory.UTILITY, subCat,
//				    						"UtilityStack detected improperly formatted attribute." +
//				    						" tag=" + tag + " length=" + length + " value=" + value + 
//				    						" valueLen=" + value.length() + ". Ignore attribute.");
				    			offset = ((lenIndex+1)+length + 1);
				    		}
				    		else {
				    			logger.warn(PC2LogCategory.UTILITY, subCat,
				    					"UtilityStack detected improperly formatted attribute." +
				    					" tag=" + tag + " length=" + length + " value=" + value + 
				    					" valueLen=" + value.length() + ". Ignore attribute.");
				    			break;
				    		}
				    		
				    	}
				    	event.setMessage(um);
				    	return true;
				    	
				    }
				    else {
				    	logger.warn(PC2LogCategory.UTILITY, subCat,
				    			"UtilityStack received message(" + buffer +
				    	") with invalid length field in header. Dropping message.");
				    }
				}
			}
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.UTILITY, subCat,
					"UtilityStack failed during parsing of message(" + 
					msg.toString() + "). Error occurred while " + parsingErr[errIndex], ex);
		}
		return false;
	}

	public void sendMessage(UtilityMessage message, int seq, 
			InetSocketAddress remoteAddress) {
		try {
			processor.sendMessage(message, seq, remoteAddress);
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.UTILITY, subCat,
					"UtilityStack encountered an error when trying to send Utility message", ex);
		}
	}

	public void stop() {
		try {
			if (processor != null) {
				
				processor.stop();
			}
			this.isRunning = false;
			if (thread != null)
				thread.interrupt();
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.UTILITY, subCat,
					"UtilityStack encountered an error when trying to stop the proccessor and thread", ex);
		
		}
	}
}
