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
 * pertaining to information in the STUN protocol.
 * 
 * @author ghassler
 *
 */
public class StunRef extends MsgRef {

	/** 
	 * This is the STUN Message Type 
	 */
	private String method;
	/**
	 * This is STUN attribute name
	 * within a message.
	 * 
	 */
	private String hdr;

	/**
	 * A field within a specific attribute
	 */
	private String parameter;

	/**
	 * A flag to indicate this is a error response message
	 */
	private boolean response = false;
	/**
	 * The status code of the error response
	 */
	private String statusCode;

	/**
	 * Constructor
	 * @param type
	 */
	public StunRef(String type) {
		super(type);
	}

	/**
	 * Gets the message type
	 * @return
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Gets the Stun attribute
	 * @return
	 */
	public String getHeader() {
		return hdr;
	}

	/**
	 * Gets the parameter in the attribute
	 * @return
	 */
	public String getParameter() {
		return parameter;
	}

	/**
	 * Sets the method (message-type)
	 * @param method
	 */
	public void setMethod (String method) {
		this.method = method;
	}

	/**
	 * Sets the header (attribute)
	 * @param hdr
	 */
	public void setHeader(String hdr) {
		this.hdr = hdr;
		checkForMsgEventHeader(hdr);
	}

	/**
	 * Sets the parameter
	 * @param param
	 */
	public void setParameter(String param) {
		this.parameter = param;
	}

	/**
	 * Retrieves the response flag.
	 * @return
	 */
	public boolean isResponseRef() {
		return response;
	}

	/**
	 * Gets the status code
	 * 
	 */
	public String getStatusCode() {
		if (response) {
			return statusCode;
		}
		return null;
	}

	/**
	 * Sets the status code
	 * @param code
	 */
	public void setStatusCode(String code) {
		this.statusCode = code;
		this.response = true;
	}

	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		if (response) 
			result += statusCode + "-" + method + "[" + super.msgInstance + "]";
		else 
			result += method + "[" + super.msgInstance + "]";
		if (hdr != null)
			result += "." + hdr + "[" + super.hdrInstance + "]";
		if (parameter != null) 
			result += "." + parameter;
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
		StunRef retval = (StunRef)super.clone();
		if (retval != null ) {
			if (this.method != null) 
				retval.method = new String(this.method);
			if (this.hdr != null) 
				retval.hdr = new String(this.hdr);
			if (this.parameter != null) 
				retval.parameter = new String(this.parameter);
			if (this.statusCode != null) 
				retval.statusCode = new String(this.statusCode);
			retval.response = this.response;
		}	

		return retval;
	}

	@Override
	public String display() {
		String result = "";
		if (response) 
			result += statusCode + "-" + method;
		else 
			result += method;
		if (msgInstance != null)
			result += "[" + msgInstance + "]";
		if (hdr != null)
			result += "." + hdr + "[" + hdrInstance + "]";
		if (parameter != null) 
			result += "." + parameter;
		return result;
	}
}
