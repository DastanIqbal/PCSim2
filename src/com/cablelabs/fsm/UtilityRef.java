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



/**
 * A container for the message reference information necessary
 * pertaining to information in the Utility protocol.
 * 
 * @author ghassler
 *
 */
public class UtilityRef extends MsgRef {

	/**
	 * The utility message's message type.
	 * 
	 */
	private String msgType = null;
		
	/**
	 * The name of the header being referred to
	 */
	private String hdr = null;
	
	/**
	 * The name of the parameter being referred to
	 */
	private String parameter = null;

	
	/**
	 * This reference is for an array attribute
	 */
	private ArrayRef arrayReference = null;
	
	/**
	 * Constructor
	 * @param type
	 */
	public UtilityRef(String type) {
		super(type);
	}

	/**
	 * Gets the message type
	 * @return
	 */
	public String getMsgType() {
		return msgType;
	}

	/**
	 * Sets the message type
	 * @param msgType
	 */
	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}
	
	/**
	 * Gets the message header (tag)
	 * @return
	 */
	public String getHeader() {
		return hdr;
	}
	
	/**
	 * Sets the message header (tag)
	 * @param header
	 */
	public void setHeader(String header) {
		this.hdr = header;
		checkForMsgEventHeader(hdr);
	}
	
	/**
	 * Gets the message parameter (sub-tag)
	 * @return
	 */
	public String getParameter() {
		return parameter;
	}
	
	/**
	 * Sets the message parameter (sub-tag)
	 * @param param - currently only port is a valid parameter
	 *    value used in conjuction with valid SourceAddress or 
	 *    DestinationAddress header (tag)
	 */
	public void setParameter(String param) {
		this.parameter = param;
	}
	
	/**
	 * Returns the array reference information for the 
	 * msg reference
	 *
	 */
	public ArrayRef getArrayReference() {
		return arrayReference;
	}
	
	/**
	 * Sets the array reference information for the 
	 * msg reference.
	 * @param ref
	 */
	public void setArrayReference(ArrayRef ref) {
		this.arrayReference = ref;
	}
	
	/**
	 * A common test routine to determine if the referred to
	 * information is an array of data or not.
	 * @return - true if the arrayReference attribute is set, 
	 * 		false otherwise.
	 */
	public boolean hasArrayReference() {
		if (arrayReference != null)
			return true;
		return false;
	}
	
	/**
	 * Sets the fsm UID that the reference is associated to
	 * 
	 */
	@Override
	public void setUID(int uid) {
		super.setUID(uid);
	}
	
	/**
	 * Gets the fsm UID that the reference is associated to
	 * 
	 */
	@Override
	public int getUID() {
		return super.getUID();
	}
	
	/**
	 * Provides a printable representation of the message
	 * reference.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		 result += msgType;
		  if (hdr != null)
		  		result += "." + hdr + "[" + super.hdrInstance + "]";
		  if (parameter != null) 
			  result += "." + parameter;
		  if (arrayReference != null)
			  result += arrayReference;
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
		UtilityRef retval = (UtilityRef)super.clone();
		if (retval != null ) {
			if (this.msgType != null) 
				retval.msgType = new String(this.msgType);
			if (this.hdr != null) 
				retval.hdr = new String(this.hdr);
			if (this.parameter != null) 
				retval.parameter = new String(this.parameter);
			if (this.arrayReference != null)
				retval.arrayReference = (ArrayRef)this.arrayReference.clone();

		}	

		return retval;
	}
	
	@Override
	public String display() {
		String result = msgType;
		  if (msgInstance != null)
			  result += "[" + msgInstance + "]";
		  if (hdr != null)
		  		result += "." + hdr + "[" + super.hdrInstance + "]";
		  if (parameter != null) 
			  result += "." + parameter;
		  if (arrayReference != null)
			  result += arrayReference;
		  return result;
	}
}
