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
 * A container class for the SEND Action defined within PC 2.0 Simulator 
 * XML documents.
 * 
 * @author ghassler
 *
 */
public class Send implements Action { 

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
	private String target = "DUT";
	
	/**
	 * The destination of this message. The final network intended to receive
	 * the message. By default the system will use the target attribute  as
	 * the destination unless the script assigned another value.
	 */
	private String destination = null;
	
	/**
	 * The line (or port) to send this message to
	 */
	private int port = 1;
	
	/**
	 * The NE label of element generating this message.
	 */
	private String originator = null;
	
	/**
	 * A container for any modifications that must be made to the message
	 * after the default has been constructed.
	 */
	private LinkedList<Mod> modifiers;
	
	/**
	 * The transport protocol to use to send this messaage.
	 */
	private Transport transportProtocol = null;
	
	/**
	 * The associated request that the send may have when it is a resposne.
	 */
	private MsgEvent request = null;

	private LinkedList<String> bodies = null;
	
	private boolean includeMultipartBody = false;
	/** 
	 * A flag to indicate that this message needs to automatically create
	 * an SDP body for the message.
	 */
	private boolean includeSDP = false;
	
	/** 
	 * A flag to indicate that this message needs to automatically create
	 * an Open PIDF body for the message.
	 */
	private boolean includeOpen = false;
	
	/** 
	 * A flag to indicate that this message needs to automatically create
	 * an Closed PIDF body for the message.
	 */
	private boolean includeClosed = false;
	
	/** 
	 * A flag to indicate that this message needs to automatically create
	 * an message-summary body for the message indicating a voice mail 
	 * message is waiting.
	 */
	private boolean includeMessageSummary = false;
	
	/**
	 * A flag to indicate that this message needs to automatically 
	 * use the compact (short) form for the SIP headers. By default
	 * this is false.
	 */
	private boolean compact = false;
	
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
	 * This is the event-type field of the Event header
	 * that will be included in a SIP Subscribe message.
	 * A null value by default implies the 'reg' event.
	 */
	private String subscribeType = null;
	
	private boolean cleanUpFlag = false;
	
	/**
	 * This attribute allows the scriptor to override which
	 * dialog forming request message to use when building
	 * a response or intra-dialog request message.
	 */
	private String originalReq = null;
	
	/**
	 * This attribute allows the scriptor to specify which
	 * instance of a message to use a the original request
	 * message when building a response or intra-dialog request
	 * message.
	 */
	private String originalInstance = MsgQueue.LAST;

	/**
	 * The message reference information to use for the destination's
	 * IP address for the stream. In other words the peers IP address.
	 */
	private MsgRef toIP = null;
	
	/**
	 * The message reference information to use for the destination
	 * IP port of the stream. In other words the peers IP port.
	 */
	private MsgRef toPort = null;

	/**
	 * This attribute is used for STUN message only. It indicates that
	 * the message should also include the USE-CANDIDATE attribute when
	 * construction the message.
	 */
	private boolean useCandidate = false;
	
	/**
	 * This attribute is used for STUN message only. It indicates that
	 * the message should also include the ICE-CONTROLLING and 
	 * PRIORITY attributes when construction the request message.
	 */
	private boolean iceLite = false;
	
	/**
	 * This attribute is used for STUN message only. It is not available in
	 * script, but rather set by the STUN Model when ordered to perform 
	 * connectivity checks for the script. 
	 */
	private String peerICEUsername = null;
	
	/**
	 * This attribute is used for STUN message only. It is not available in
	 * script, but rather set by the STUN Model when ordered to perform 
	 * connectivity checks for the script. 
	 */
	private String peerICEPassword = null;
	
	/** 
	 * This attribute is used for STUN messages only. It controls the value to
	 * use in the IceControlling Attribute of a message.
	 */
	private long iceControlling = -1;
	
	/**
	 * This attribute is used for STUN messages only. It controls the value to
	 * use in the IceControlling Attribute of a message.
	 *  
	 */
	private byte [] id = null;
	
	/**
	 * Constructor
	 * @param protocol
	 * @param msgType
	 */
	public Send (String protocol, String msgType) {
		super();
		this.protocol = protocol;
		this.msgtype = msgType;
	}
	
	/**
	 * Constructor
	 * @param protocol
	 * @param msgType
	 */
	public Send (String protocol, String msgType, boolean flag) {
		super();
		this.protocol = protocol;
		this.msgtype = msgType;
		this.cleanUpFlag = flag;
	}
	
	public void addBody(String body) {
		if (bodies == null)
			bodies = new LinkedList<String>();
		// multipart must be the first body or it is not allowed
		if (!includeMultipartBody && 
				body.equalsIgnoreCase("multipart") && 
				!includeSDP &&
				!includeOpen &&
				!includeClosed &&
				!includeMessageSummary) {
			includeMultipartBody = true;
		}
		else if (!includeMultipartBody) {
			if (body.equalsIgnoreCase("SDP"))
				this.includeSDP = true;
			else if (body.equalsIgnoreCase("open"))
				this.includeOpen = true;
			else if (body.equalsIgnoreCase("closed"))
				this.includeClosed = true;
			else if (body.equalsIgnoreCase("message-summary"))
				this.includeMessageSummary = true;
		}
		bodies.add(body);
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
	/**
	 * Gets the protocol
	 * @return
	 */
	public String getProtocol() {
		return protocol;
	}
	
	/**
	 * Gets the message type
	 * @return
	 */
	public String getMsgType() {
		return msgtype;
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
	 * Calls the FSM API to perform the actual construction and 
	 * transmission of the message defined by this container.
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.send(this);
	}
	
	public Mod getModifier(String hdr) {
		if (modifiers != null) {
			ListIterator<Mod> iter = modifiers.listIterator();
			while (iter.hasNext()) {
				Mod m = iter.next();
				if (m.getHeader().equals(hdr)) {
					return m;
				}
			}
		}
		return null;
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
	 * Gets the transport protocol to use for a specific
	 * message.
	 * 
	 * @return
	 */
	public Transport getTransportProtocol() {
		return this.transportProtocol;
	}
	
	/**
	 * Sets the transport protocol to use for a specific
	 * message.
	 * 
	 */
	public void setTransportProtocol(Transport transport) {
		this.transportProtocol = transport;
	}
	
	/**
	 * Gets the associated request
	 * @return
	 */
	public MsgEvent getRequest() {
		return request;
	}

	/**
	 * Sets the associated request
	 * @param request
	 */
	 public void setRequest(MsgEvent request) {
		this.request = request;
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
	  * Sets the include SDP flag for construction of the message.
	  * @param flag
	  */
//	 public void setIncludeSDP(boolean flag) {
//		 this.includeSDP = flag;
//	 }
	 
	 /**
	  * Gets the include SDP flag.
	  */
	 public boolean getIncludeSDP() {
		 return this.includeSDP;
	 }
	 
	 /**
	  * Sets the type of event to use in a SIP Subscribe message
	  * 
	  */
	 public void setSubscribeType(String type) {
		 this.subscribeType = type;
	 }
	 
	 /**
	  * Gets the type of event to use in a SIP Subscribe message
	  */
	 public String getSubscribeType() {
		 return this.subscribeType;
	 }
	 
	 /**
	  * Sets the include Open PIDF flag for construction of the message.
	  * @param flag
	  */
//	 public void setIncludeOpen(boolean flag) {
//		 this.includeOpen = flag;
//	 }
	 
	 /**
	  * Gets the include Open PIDF flag.
	  */
	 public boolean getIncludeOpen() {
		 return this.includeOpen;
	 }
	 
	 /**
	  * Sets the include Closed PIDF flag for construction of the message.
	  * @param flag
	  */
//	 public void setIncludeClosed(boolean flag) {
//		 this.includeClosed = flag;
//	 }
	 
	 /**
	  * Gets the include Closed PIDF flag.
	  */
	 public boolean getIncludeClosed() {
		 return this.includeClosed;
	 }
	 
	 /**
	  * Sets the include Message-Summary flag for construction of the message.
	  * @param flag
	  */
//	 public void setIncludeMessageSummary(boolean flag) {
//		 this.includeMessageSummary = flag;
//	 }
	 
	 /**
	  * Gets the include Message-Summary flag.
	  */
	 public boolean getIncludeMessageSummary() {
		 return this.includeMessageSummary;
	 }
	 
	 public boolean getIncludeMultipartBody() {
		 return this.includeMultipartBody;
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
	 
	 public void setCleaupUpFlag(boolean flag) {
		 this.cleanUpFlag = flag;
	 }
	 
	 public boolean isCleanUpEvent() {
		 return this.cleanUpFlag;
	 }
	 
	 public LinkedList<String> getBodies() {
		 return this.bodies;
	 }
	 
	 public String getOriginalRequest() {
		 return this.originalReq;
	 }
	 
	 public void setOriginalRequest(String req) {
		this.originalReq = req;
	 }
	 
	 public String getOriginalInstance() {
		 return this.originalInstance;
	 }
	 
	 public void setOriginalInstance(String instance) {
		this.originalInstance = instance;
	 }
	 
	 public void setToIP(MsgRef to) {
		 this.toIP = to;
	 }
	 
	 public MsgRef getToIP() {
		 return this.toIP;
	 }
	 
	 public void setToPort(MsgRef to) {
		 this.toPort = to;
	 }
	 
	 public MsgRef getToPort() {
		 return this.toPort;
	 }
	 
	 public void setUseCandidate(boolean flag) {
		 this.useCandidate = flag;
	 }
	 
	 public boolean useCandidate() {
		 return this.useCandidate;
	 }
	 
	 public boolean useIceLite() {
		 return this.iceLite;
	 }
	 
	 public void setIceLite(boolean flag) {
		 this.iceLite = flag;
	 }
	 
	 public String getPeerICEUsername() {
		 return this.peerICEUsername;
	 }
	 
	 public void setPeerICEUsername(String username) {
		 this.peerICEUsername = username;
	 }
	 
	 public String getPeerICEPassword() {
		 return this.peerICEPassword;
	 }
	 
	 public void setPeerICEPassword(String password) {
		 this.peerICEPassword = password;
	 }
	 
	 public long getIceControlling() {
		 return this.iceControlling;
	 }
	 
	 public void setIceControlling(long value) {
		 this.iceControlling = value;
	 }
	 
	 public byte [] getTransactionId() {
		 return this.id;
	 }
	 
	 public void setTransactionId(byte [] id) {
		 this.id = id;
	 }
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = "\tsend protocol=\"" + protocol + "\"" + " msgtype=\"" + msgtype 
		+ "\"" + " using transportProtocol=\"" + transportProtocol + "\"";
		result += " target=\"" + target + "\"";
		if (destination != null) 
			result += " destination=\"" + destination + "\"";
		if (port != 1)
			result += " port=\"" + port + "\"";
		if (originator != null)
			result += " originator=\"" + originator + "\"";
		if (stack != null)
			result += " stack=\"" + stack + "\"";
		if (compact)
			result += " compact=\"" + compact + "\"";
		if (bodies != null) {
			ListIterator<String> iter = bodies.listIterator();
			String b = null;
			while (iter.hasNext()) {
				if (b == null)
					b = iter.next();
				else 
					b = " " + iter.next();
			}
			result += " bodies=\"" + b + "\"";
		}
		if (originalReq != null) {
			result += " origReq=\"" + originalReq + "\"";
			if (originalInstance != MsgQueue.FIRST) {
				result += " orig_instance=\"" + originalInstance + "\"";
			}
		}
		if (includeMultipartBody)
			result += " include multipart ";
		else {
			if (includeSDP)
				result += " include SDP ";
			if (includeOpen)
				result += " include PIDF Open ";
			if (includeClosed)
				result += " include PIDF Closed ";
			if (includeMessageSummary)
				result += " include Message Summary ";
		}
		if (subscribeType != null)
			result += " subscribeType=" + subscribeType;
		
		result += " request=\"" + request + "\"";
		
		if (toIP != null) {
			result += "\ntoIP=\"" + toIP + "\"";
		}
		if (toPort != null) {
			result += "\ntoPort=\"" + toPort + "\"";
		}
		if (useCandidate != false) {
			result += "\ncandidate=\"" + useCandidate + "\"";
		}
		if (iceLite != false) {
			result += "\nice=\"" + iceLite + "\"";
		}
		if (id != null) {
			result += "\nid=\"" + id + "\"";
		}
		if (iceControlling != -1) {
			result += "\ncontrolling=\"" + iceControlling + "\"";
		}
		// Do not include the following as they are set internally only
		// peerICEUsername
		// peerICEPassword
		
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
		Send retval = (Send)super.clone();
		if (retval != null ) {
			
			if (this.protocol != null) 
				retval.protocol = new String(this.protocol);
			if (this.msgtype != null) 
				retval.msgtype = new String(this.msgtype);
			if (this.target != null) 
				retval.target = new String(this.target);
			if (this.originator != null) 
				retval.originator = new String(this.originator);
			if (this.destination != null) 
				retval.destination = new String(this.destination);
			if (this.transportProtocol != null) 
				retval.transportProtocol = this.transportProtocol;
			if (this.originalReq != null) 
				retval.originalReq = new String(this.originalReq);
			if (this.originalInstance!= null) 
				retval.originalInstance = new String(this.originalInstance);
			if (this.stack != null) 
				retval.stack = new String(this.stack);
			retval.port = this.port;
			retval.includeMultipartBody = this.includeMultipartBody;
			retval.includeSDP = this.includeSDP;
			retval.includeOpen = this.includeOpen;
			retval.includeClosed = this.includeClosed;
			retval.includeMessageSummary = this.includeMessageSummary;
			retval.compact = this.compact;
			if (this.subscribeType != null)
				retval.subscribeType = new String(this.subscribeType);
			
			if (this.modifiers != null) {
				retval.modifiers = new LinkedList<Mod>();
				ListIterator<Mod> iter = this.modifiers.listIterator();
				while(iter.hasNext()) {
					Mod m = iter.next();
					Mod newMod = (Mod)m.clone();
					retval.modifiers.add(newMod);
				}
			}
			
			if (this.bodies != null) {
				retval.bodies = new LinkedList<String>();
				ListIterator<String> iter = this.bodies.listIterator();
				while (iter.hasNext()) {
					String b = new String(iter.next());
					retval.bodies.add(b);
				}
			}
			
			if (toIP != null) {
				retval.toIP = (MsgRef)this.toIP.clone();
			}
			if (toPort != null) {
				retval.toPort = (MsgRef)this.toPort.clone();
			}
			if (useCandidate)
				retval.useCandidate = this.useCandidate;
			if (iceLite)
				retval.iceLite = this.iceLite;
			if (id != null) {
				retval.id = new byte [this.id.length];
				System.arraycopy(this.id,0, retval.id, 0, this.id.length);
			}
			if (iceControlling != -1) {
				retval.iceControlling = this.iceControlling;
			}
            // Do not clone any of the internal settings as this will happen at runtime
			// peerICEUsername
			// peerICEPassword
			
			// This should be null since no associated message request
			// should be have been received by the masterFSM
			if (this.request != null)
				throw new CloneNotSupportedException("The request attribute is not null and is not cloneable.");
		}	
//		System.out.println("this " + this.me());
//		System.out.println("retval " + retval.me());
		return retval;
	}
	
//	public String me() {
//		return "S " + super.toString();
//	}
}
