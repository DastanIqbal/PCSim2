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

import com.cablelabs.tools.RefLocator;

/**
 * The base class for all implmentations of the msg_ref element
 * within a PC2.0 Simulator XML document.
 * 
 * @author ghassler
 *
 */
public class MsgRef implements Reference {

	/**
	 * This contains the type of message reference. 
	 * Valid values: sip, sip, platform, voicetronix or extensions
	 */
	protected String type;
	
	/** 
	 * This is an index into the list of messages set and received
	 * by the system during the execution of this test.
	 * Default value: current
	 * Valid Values: first, last, prev, current, any or positive integer
	 */
	protected String msgInstance = MsgQueue.CURRENT;
	
	/**
	 * The index of the header within the message identified by the
	 * msgInstance attribute.
	 * Default Value: first
	 * Valid Values: first, last, current, any or positive integer
	 */
	protected String hdrInstance = MsgQueue.FIRST;
	
	/**
	 * This defines that the message reference is a arithmetic operational
	 * reference.
	 */
	protected boolean arithmeticRef = false;
	
	/**
	 * This defines that the arthimetic operation as type addition.
	 */
	protected boolean addRef = false;
	
	/**
	 * This defines that the arthimetic operation as type subtraction.
	 */
	protected boolean subRef = false;
	
	/**
	 * True when the reference is to the message queue.
	 */
	private boolean queueRef = false;
	
	/**
	 * This is the arithmetic value to either add or subtract to the reference
	 */
	private int arithmeticMod = 0;
	
	/**
	 * This is the arithmetic value to either add or subtract to the reference
	 */
	private double arithmeticModDec = 0.0;

	/** 
	 * The id of the fsm that this message reference is referring to in the test.
	 */
	private int fsmUID = 0;

	/**
	 * The value to be and-ed or or-ed with the referred to
	 * value.
	 */
	protected String mask = null;

	/**
	 * The specific operator to perform on the referred to
	 * value when returning it either and or or. Default is
	 * to perform or operation
	 */
	protected boolean and = false;
	
	/**
	 * A flag indicating whether the underlying reference is 
	 * to information about the event instead of information 
	 * within the event.
	 */
	private boolean msgEventHdr = false;
	
	/**
	 * These two attributes indicate that the value should use
	 * a substring of the result beginning at the firstChar
	 * to the lastChar
	 * 
	 * The string should contain a 0 or greater number of the
	 * string length- some positive integer referring to some
	 * value from the end of the string.
	 * 
	 * e.g if the reference is to the value "auth" including the
	 * quotation marks, you can have them stripped by use the
	 * substring attribute in the following manner:
	 * 		substring="1 length-1"
	 * 
	 * This will result in the string auth being used for the
	 * reference.
	 */
	private String firstChar = null;
	private boolean firstIsOffsetFromLength = false;
	private String lastChar = null;
	private boolean lastIsOffsetFromLength = false;
	
	/**
	 * This is a flag indicating once the reference has been resolved,
	 * get the length of the reference and use this instead of the actual
	 * string.
	 */
	private boolean lengthFlag = false;
	
	public final static String SIP_MSG_TYPE = "sip";
	
	public final static String UTILITY_MSG_TYPE = "utility";
	
	public final static String SDP_MSG_TYPE = "sdp";
	
	public final static String STUN_MSG_TYPE = "stun";
	
	public final static String EVENT_MSG_TYPE = "event";
	
	public final static String PLATFORM_MSG_TYPE = "platform";
	
	public final static String RTP_MSG_TYPE = "rtp";
	
	public final static String INTERNAL_MSG_TYPE = "internal";
	
	public final static String MULTIPART_MIXED_TYPE = "multipart/mixed";
	
	/**
	 * The index of the body within the message 
	 * 
	 * Default Value: first
	 * Valid Values: first, last, current, any or positive integer
	 */
	protected String bodyInstance = MsgQueue.FIRST;
	
	/**
	 * A flag indicating that the reference result should have its
	 * special characters escaped.
	 */
	protected boolean escape = false;
	/**
	 * Constructor.
	 * @param type
	 */
	public MsgRef(String type) {
		this.type = type;
	}
	
	/**
	 * This method marks the reference as being a reference
	 * to the information about the event instead of information
	 * with the actual event. eg. the source ip address that 
	 * originated the event.
	 *
	 * @return
	 */
	public boolean checkForMsgEventHeader(String hdr) {
		if (hdr.equals(RefLocator.SRC_ADDRESS) ||
				hdr.equals(RefLocator.DEST_ADDRESS) ||
				hdr.equals(RefLocator.TIMESTAMP) ||
				hdr.equals(RefLocator.TRANSPORT))
			msgEventHdr = true;
		return msgEventHdr;
	}
	
	 /**
	  * Creates a string representation of the container and its
	  * contents.
	  */
	 @Override
	public String display() {
		return null;
	}
	 
	/**
	 * Get the reference type
	 * @return
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Gets the message instance
	 * @return
	 */
	public String getMsgInstance() {
		return msgInstance;
	}
	
	/**
	 * Gets the header instance
	 * @return
	 */
	public String getHdrInstance() {
		return hdrInstance;
	}
	
	/**
	 * Sets the message instance
	 * @param instance
	 */
	public void setMsgInstance(String instance) {
		this.msgInstance = instance;
	}
	
	/**
	 * Sets the header instance
	 * @param instance
	 */
	public void setHdrInstance(String instance) {
		this.hdrInstance = instance;
	}
	
	/**
	 * Sets the addRef and arithmetic flags true 
	 *
	 */
	public void setAddRef(int value) {
		this.arithmeticRef = true;
		this.addRef = true;
		this.arithmeticMod = value;
		this.arithmeticModDec = 0.0;
	}
	
	/**
	 * Sets the addRef and arithmetic flags true 
	 *
	 */
	public void setAddRef(double value) {
		this.arithmeticRef = true;
		this.addRef = true;
		this.arithmeticModDec = value;
	}
	
	
	/**
	 * Sets the subRef and arithmetic flags true 
	 *
	 */
	public void setSubRef(int value) {
		this.arithmeticRef = true;
		this.subRef = true;
		this.arithmeticMod = value;
		this.arithmeticModDec = 0.0;
		this.arithmeticMod = 0;
	}
	
	/**
	 * Sets the subRef and arithmetic flags true 
	 *
	 */
	public void setSubRef(double value) {
		this.arithmeticRef = true;
		this.subRef = true;
		this.arithmeticModDec = value;
		this.arithmeticMod = 0;
	}
	
	/**
	 * A flag defining that the reference is an
	 * arithmetic type. This means that it is 
	 * either the add_ref or substract_ref
	 * @return - true if it is an arithmetic reference, false otherwise
	 */
	public boolean isArithmeticRef() {
		return arithmeticRef;
	}
	
	/**
	 * Determine if the reference is to an add operation
	 * 
	 * @return -true if the reference is arithmetic and it is
	 * 		an add, false otherwise.
	 */public boolean isAddRef() {
		return (arithmeticRef && addRef);
	}
	
	 /**
	  * Determine if the reference is to an subtract operation
	  * 
	  * @return -true if the reference is arithmetic and it is
	  * 		a subtraction, false otherwise.
	  */
	 public boolean isSubRef() {
		return (arithmeticRef && subRef);
	}
	 
	 /**
	  * Returs the current setting for the queue flag.
	  * @return
	  */
	 public boolean isQueueRef() {
		 return queueRef;
	 }
	 
	 /**
	  * Sets the queue flag true.
	  * 
	  */
	 public void setQueueRef() {
		 this.queueRef = true;
		 
	 }

	/**
	 * This returns true if the reference is pertaining to the
	 * actual event instead of details within the event. eg the
	 * source ip address that originated the event.
	 * @return
	 */
		public boolean isReferenceOnEvent() {
			return this.msgEventHdr;
		}
	/**
	 * Gets the arithmetic modification value
	 * @return
	 */
	 public int getArithmeticMod() {
		 return this.arithmeticMod;
	 }
	 
	 /**
	  * Gets the fsm name that this information is
	  * referring to.
	  * @return - the fsm that the search should be 
	  * made for the message.
	  */
	 public int getUID() {
		 return this.fsmUID;
	 }
	 
	 /**
	  * Sets the name of the fsm that this information is
	  * referring to.
	  * 
	  * @param fsm - the name of the fsm that the message is
	  * associated with.
	  */
	 public void setUID(int uid) {
		 this.fsmUID = uid;
	 }

	 /**
	  * This allows any msg_reference to be 
	  * @param mask
	  * @param and
	  */
	 public void setBinaryRef(String mask, boolean and) {
		 this.mask = mask;
		 this.and = and;
	 }
	 
	/**
	 * Allows an easy mechanism to determine if the reference
	 * is a binary referrence or not.
	 * 
	 * @return
	 */
	 public boolean isBinaryRef() {
		 if (mask != null)
			 return true;
		 return false;
	 }
	 
	 /**
	  * Gets the mask to be anded or ored with the value.
	  * 
	  * @return
	  */
	 public String getBinaryMask() {
		 return mask;
	 }
	 
	/**
	 * Gets the binary operator to perform with the mask
	 * @return
	 */
	 public boolean isBinaryAndOp() {
		 return and;
	 }
	 
	 public String updateArithmeticRef(String value) {
		 if (value != null) {
			 if (value.contains(".") || arithmeticModDec != 0.0) {
				 try{
					 Double val = null;
					 val = Double.parseDouble(value);
					 if (addRef)
						 val += arithmeticModDec;
					 else if (subRef) 
						 val -= arithmeticModDec;
					 return Double.toString(val);
				 }
				 catch (Exception e) {
					 // Log message 
					 return value;
				 }
			 }
			 else {
				 try {
					 Long val = null;
					 val = Long.parseLong(value);
					 if (addRef)
						 val += arithmeticMod;
					 else if (subRef)
						 val -= arithmeticMod;
					 return Long.toString(val);
				 }
				 catch (Exception e) {
					 // Log message 
					 return value;
				 }
			 }
		 }
		 else 
			 return value;
	 }
	 
	 public String getBodyInstance() {
		 return this.bodyInstance;
	 }
	 
	 public void setBodyInstance(String instance) {
		 this.bodyInstance = instance;
	 }
	 
	 /**
	  * Creates a string representation of the container and its
	  * contents.
	  */
	 @Override
	public String toString() {
		String result = "type=\"" + type + "\" msg_instance=\"" + msgInstance + 
		"\" hdrInstance=\"" + hdrInstance + "\"";
		if (!bodyInstance.equals(MsgQueue.FIRST)) {
			result += " bodyInstance=\"" + bodyInstance + "\"";
		}
		if (queueRef)
			result += " queueRef=" + queueRef;
		if (arithmeticRef) {
			if (addRef)
				result += " arithmetic=" + arithmeticRef + " addRef=" + addRef;
			else if (subRef)
				result += " arithmetic=" + arithmeticRef + " subRef=" + subRef;
		}
		if (fsmUID > 0) 
			result += " fsmUID=" + fsmUID;
		
		if (firstChar != null && lastChar != null)
			result += " substring=" + firstChar + " " + lastChar;
		else if (firstChar != null)
			result += " substring=" + firstChar;
		 
		if (mask != null) {
			if (and)
				result += "  op=\"&\" mask= " + mask;
			else
				result += " op=\"|\" mask=" + mask;
		}
		if (msgEventHdr) 
			result += "  msgEventHdr=" + msgEventHdr;
		
		if (escape)
			result += " escape=" + escape;
		
		result += " ";
		return result;
	}
	 
	 /** This implements a deep copy of the class for replicating 
	  * FSM information.
	  */ 
	 @Override
	public Object clone() throws CloneNotSupportedException {
		 MsgRef retval = (MsgRef)super.clone();
		 if (retval != null) {
			 if (this.type != null) 
				 retval.type = new String(this.type);
			 if (this.msgInstance != null)
				 retval.msgInstance = new String(this.msgInstance);
			 if (this.hdrInstance != null)
				 retval.hdrInstance = new String(this.hdrInstance);
			 if (this.bodyInstance != null)
				 retval.bodyInstance = new String(this.bodyInstance);
			 retval.arithmeticRef = this.arithmeticRef;
			 retval.addRef = this.addRef;
			 retval.subRef = this.subRef;
			 retval.queueRef = this.queueRef;
			 retval.arithmeticMod = this.arithmeticMod;
			 retval.fsmUID = this.fsmUID;
			 if (this.mask != null)
				 retval.mask = new String(this.mask);
			 retval.and = this.and;
			 retval.msgEventHdr =this.msgEventHdr;
			 if (firstChar != null)
				 retval.firstChar = new String(this.firstChar);
			 if (lastChar != null)
				 retval.lastChar = new String(this.firstChar);
			 retval.escape = this.escape;
		 }	

		 return retval;
	 }

	public String getFirstChar() {
		return firstChar;
	}

	public void setFirstChar(String firstChar) {
		this.firstChar = firstChar;
	}

	public String getLastChar() {
		return lastChar;
	}

	public void setLastChar(String lastChar) {
		this.lastChar = lastChar;
	}

	public boolean isFirstAnOffsetFromLength() {
		return firstIsOffsetFromLength;
	}

	public void setFirstIsOffsetFromLength(boolean firstIsOffsetFromLength) {
		this.firstIsOffsetFromLength = firstIsOffsetFromLength;
	}

	public boolean isLastAnOffsetFromLength() {
		return lastIsOffsetFromLength;
	}

	public void setLastIsOffsetFromLength(boolean lastIsOffsetFromLength) {
		this.lastIsOffsetFromLength = lastIsOffsetFromLength;
	}

	public boolean useLength() {
		return lengthFlag;
	}

	public void setLengthFlag(boolean lengthFlag) {
		this.lengthFlag = lengthFlag;
	}
	
	public void setEscape(boolean flag) {
		this.escape = flag;
	}
	
	public boolean getEscape() {
		return this.escape;
	}
 }
