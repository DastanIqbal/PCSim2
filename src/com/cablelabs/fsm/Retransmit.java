/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

import java.util.LinkedList;
import java.util.ListIterator;


/** 
 * This class holds the information for the retransmit element of the
 * XML Document. It specifies the retransmit of the previous message 
 * for the given protocol
 * @author Garey Hassler
 *
 */
public class Retransmit implements Action {

	/**
	 * The protocol of the message to send
	 */
	private String protocol;

	/**
	 * The type of message to send
	 */
	private String msgtype;
	
	/**
	 * The target (NE label) of this message. By default the target is the
	 * DUT
	 */
//	 Not currently used
	private String target = null;
	
	/**
	 * The line (or port) to send this message to
	 */
//	 Not currently used
	private int port = 1;
	
	/**
	 * The NE label of element generating this message.
	 */
//	 Not currently used
	private String originator = null;
	
	/**
	 * A container for any modifications that must be made to the message
	 * after the default has been constructed.
	 */
//	 Not currently used
	private LinkedList<Mod> modifiers;
	
	/**
	 * The transport protocol to use to send this messaage.
	 */
//	 Not currently used
	private String transportProtocol = null;
	
	/**
	  * The name of the Stack to use when sending
	  * a message from a distributor that contains 
	  * multiple IPs. Each stack is associated with one
	  * and only one IP address for a given protocol.
	  * If the value is not set, the distributor will
	  * use the default stack name. Defining the stack
	  * name in an individual message, takes precedence
	  * over that defined by the FSM and the default
	  * for the system.
	  */
	private String stack = null;
	/**
	 * The destination of this message. The final network intended to receive
	 * the message. By default the system will use the target attribute  as
	 * the destination unless the script assigned another value.
	 */
	private String destination = null;
	
	public Retransmit(String protocol, String type) {
		this.protocol = protocol;
		this.msgtype = type;
	}
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		// TODO Auto-generated method stub
		api.retransmit(this, msgQueueIndex);
	}

	/**
	 * Forces derived classes to implement this method for logging.
	 */
	@Override
	public String toString() {
		String result = "\tretransmit protocol=\"" + protocol + "\""  
		+ " msgtype=\"" + msgtype 
		+ "\""; 
		if (transportProtocol != null)
			result += " using transportProtocol=\"" + transportProtocol + "\"";
		if (target != null)
			result += " target=\"" + target + "\"";
		if (port != 1)
			result += " port=\"" + port + "\"";
		if (originator != null)
			result += " originator=\"" + originator + "\"";
		if (stack != null)
			result += " stack=\"" + stack + "\"";
			
		result += "\n";
		if (modifiers != null && modifiers.size() > 0) {
			for (int i = 0; i < modifiers.size(); i++) {
				Mod m = modifiers.get(i);
				result += m.toString();
			}
		}
		return result;
		
	}

	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		Retransmit retval = (Retransmit)super.clone();
		if (retval != null ) {
			
			if (this.protocol != null) 
				retval.protocol = new String(this.protocol);
			if (this.msgtype != null) 
				retval.msgtype = new String(this.msgtype);
			if (this.target != null) 
				retval.target = new String(this.target);
			if (this.originator != null) 
				retval.originator = new String(this.originator);
			if (this.transportProtocol != null) 
				retval.transportProtocol = new String(this.transportProtocol);
			if (this.stack != null) 
				retval.stack = new String(this.stack);
			retval.port = this.port;
			if (this.modifiers != null) {
				retval.modifiers = new LinkedList<Mod>();
				ListIterator<Mod> iter = this.modifiers.listIterator();
				while(iter.hasNext()) {
					Mod m = iter.next();
					Mod newMod = (Mod)m.clone();
					retval.modifiers.add(newMod);
				}
			}
		}	

		return retval;
	}
	/**
	 * Gets the value for the destination of the message.
	 * @return
	 */
	public String getDestination() {
		return this.destination;
	}
	
	/**
	 * Sets the destination for the message.
	 * @param dest
	 */
	public void setDestination(String dest) {
		this.destination = dest;
	}
	
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	/**
	 * Gets the message type
	 * @return
	 */
	public String getMsgType() {
		return msgtype;
	}
	
	/**
	 * Gets the list of modifiers
	 * @return
	 */
	public LinkedList<Mod> getModifiers() {
		return modifiers;
	}
	
	/**
	 * Adds a modifier to the container
	 * @param mod
	 */
	public void addModifier(Mod mod) {
		if (this.modifiers == null)
			this.modifiers = new LinkedList<Mod>();
		this.modifiers.add(mod);
	}
	
	/**
	 * Test for any modifiers on the message.
	 * @return
	 */
	public boolean hasModifiers() {
		if (modifiers != null && modifiers.size() > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the originator
	 * @return
	 */
	public String getOriginator() {
	
		return originator;
	}
	
	/**
	 * Sets the originator
	 * @param originator
	 */
	public void setOriginator(String originator) {
		this.originator = originator;
	}
	
	/**
	 * Gets the port (or line on the UE to send to)
	 * @return
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Sets the port (or line) on the UE to send to
	 * @return
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	 /**
	  * Sets the name of the Stack to use when sending
	  * a message from a distributor that contains 
	  * multiple IPs. Each stack is associated with one
	  * and only one IP address for a given protocol.
	  * If the value is not set, the distributor will
	  * use the default stack name.
	  */
	 public void setStack(String stack) {
		 this.stack = stack;
	 }
	 
	 /**
	  * Gets the name of the Stack to use when sending
	  * a message. This is used to override the default
	  * stack name.
	  */
	 public String getStack() {
		 return stack;
	 }
	 
	/**
	 * Gets the target (NE label) to send the message to
	 * @return
	 */
	public String getTarget() {
		return target;
	}
	
	/**
	 * Sets the target (NE label) to send the message to
	 * @param target
	 */
	public void setTarget(String target) {
		this.target = target;
	}
	
	/**
	 * Gets the transport protocol to use for a specific
	 * message.
	 * 
	 * @return
	 */
	public String getTransportProtocol() {
		return this.transportProtocol;
	}
	
	/**
	 * Sets the transport protocol to use for a specific
	 * message.
	 * 
	 */
	public void setTransportProtocol(String transport) {
		this.transportProtocol = transport;
	}
}
