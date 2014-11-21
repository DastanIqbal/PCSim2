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

import com.cablelabs.common.Transport;
/**
 * This class defines the Proxy action which is to pass a message through
 * the FSM to the peer network element
 * 
 * @author Garey Hassler
 *
 */
public class Proxy implements Action {

	/**
	 * Reference to the message queue
	 */
	private static MsgQueue queue = MsgQueue.getInstance();
	/**
	 * The SIP event being processed by the FSM.
	 */
	private SIPMsg msg = null;
	
	/**
	 * The target (NE label) of this message. 
	 */
	private String target = null;
	
	/**
	 * The line (or port) to send this message to
	 */
	private int port = 1;

	/**
	 * The originator (NE label) of this message. By default it is the 
	 * null.
	 */
	private String originator = null;
	
	/**
	 * The transport protocol to use to send the message. eg. UDP, TCP or TLS.
	 * 
	 */
	private Transport transport = null;
	
	/**
	 * The instance of message to be proxied
	 */
	private String msgInstance = null;
	
	/**
	 * The protocol that the message to be proxied is from.
	 */
	private String protocol = null;
	
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
	 * A flag to indicate that this message needs to automatically 
	 * use the compact (short) form for the SIP headers. By default
	 * this is false.
	 */
	private boolean compact = false;
	
	/**
	 * A container for any modifications that must be made to the message
	 * after the default has been constructed.
	 */
	private LinkedList<Mod> modifiers;
	
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		MsgEvent event = null;
		if (msgInstance != null && protocol != null)
			event = queue.findByProcotol(api.getFsmUID(), protocol, 
					msgInstance, msgQueueIndex);
		else 
			event = queue.get(msgQueueIndex);
		if (event != null && event instanceof SIPMsg) {
			this.msg = (SIPMsg)event;
			api.proxy(this);
		}
		else if (event == null) {
			throw new PC2Exception("Proxy couldn't find message event[" + msgQueueIndex + "] to proxy.");
		}
		else {
			throw new PC2Exception("Proxy couldn't proxy event [" + event.getEventName() + "].");
		}
	}

	/**
	 * Sets the compact form flag.
	 * @param flag
	 */
	 public void setCompact(boolean flag) {
		 this.compact = flag;
	 }
	 
	 /**
	  * Gets the compact form flag.
	  */
	 public boolean isCompact() {
		 return this.compact;
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
	 * Gets the associated request
	 * @return
	 */public SIPMsg getSIPMsg() {
		return msg;
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
	
	@Override
	public String toString() {
		String result = null;
		if (msg != null) {
			result = "proxy - " + msg.toString();
			result += "\n";
			if (modifiers != null && modifiers.size() > 0) {
				for (int i = 0; i < modifiers.size(); i++) {
					Mod m = modifiers.get(i);
					result += m.toString();
				}
			}
		}
		else {
			result = "proxy empty";
		}
		if (target != null)
			result += " target=" + target;
		if (originator != null)
			result += " originator=" + originator;
		if (transport != null)
			result += " transportProtocol=" + transport;
		if (stack != null)
			result += " stack=" + stack;
		if (msgInstance != null)
			result += " msgInstance=" + msgInstance;
		if (protocol != null)
			result += " protocol=" + protocol;
		if (compact)
			result += " compact=\"" + compact + "\"";
		
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
		Proxy retval = (Proxy)super.clone();
		if (retval != null ) {
			// Should not need to copy the msg attribute since
			// nothing should be in the field
			if (this.msg != null) 
				throw new CloneNotSupportedException("The msg attribute is not null and is not cloneable.");
			if (this.target != null)
				retval.target = new String(this.target);
			retval.port = this.port;
			retval.compact = this.compact;
			if (this.modifiers != null) {
				retval.modifiers = new LinkedList<Mod>();
				ListIterator<Mod> iter = this.modifiers.listIterator();
				while (iter.hasNext()) {
					Mod m = iter.next();
					Mod newMod = (Mod)m.clone();
					retval.modifiers.add(newMod);
				}
			}
			retval.transport = this.transport;
		}	

		return retval;
	}

	public String getMsgInstance() {
		return msgInstance;
	}
	
	public void setMsgInstance(String instance) {
		this.msgInstance = instance;
	}

	public String getOriginator() {
		return originator;
	}

	public void setOriginator(String originator) {
		this.originator = originator;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public Transport getTransport() {
		return transport;
	}



	public void setTransport(Transport transport) {
		this.transport = transport;
	}
	
//	 This method was added as a validator for cloning
//	public String me() {
//		return "P " + super.toString();
//	}
}
