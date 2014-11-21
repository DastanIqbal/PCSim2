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
 * pertaining to information in the SIP protocol.
 * 
 * @author ghassler
 *
 */
public class SIPRef extends MsgRef {

	/**
	 * The method type of the SIP message
	 */
	protected String method;
	
	/**
	 * The name of the header being referred to
	 */
	protected String hdr;
	
	/**
	 * The name of the parameter being referred to
	 */
	protected String parameter;
	
	/**
	 * Flag specifying the class refers to a response message.
	 */
	protected boolean response = false;
	
	/**
	 * The status code of a referred response message
	 */
	protected String statusCode;
	
	
	/**
	 * Constructor
	 * @param type
	 */
	public SIPRef(String type) {
		super(type);
	}

	/**
	 * Gets the method name
	 * @return
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Gets the header
	 * @return
	 */
	public String getHeader() {
		return hdr;
	}

	/**
	 * Gets the parameter
	 * @return
	 */
	public String getParameter() {
		return parameter;
	}

	/**
	 * Sets the method
	 * @param method
	 */
	public void setMethod (String method) {
		this.method = method;
		if (method.equals("Response"))
			response = true;
		else if (statusCode != null)
			response = true;
	}

	/**
	 * Sets the header
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
	 * Retrieves the response flag
	 * @return
	 */
	public boolean isSIPResponseRef() {
		return response;
	}

	/**
	 * Gets the status code
	 * @return
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
		SIPRef retval = (SIPRef)super.clone();
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
		if (response && statusCode != null) {
			if (super.addRef || super.subRef)
				result += "(" + statusCode + "-" + method;
			else
				result += statusCode + "-" + method;
		}
		else {
			if (super.addRef || super.subRef)
				result += "(" + method;
			else
				result += method;
		}
		if (msgInstance != null)
			result += "[" + msgInstance + "]";
		if (hdr != null)
			result += "." + hdr + "[" + hdrInstance + "]";
		if (parameter != null) 
			result += "." + parameter;
		

		if (super.addRef) {
			result += " + " + super.getArithmeticMod() + ")";
		}
		else if (super.subRef) {
			result += " - " + super.getArithmeticMod() + ")";
		}
		return result;
	}
}
