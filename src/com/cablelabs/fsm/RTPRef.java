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


public class RTPRef extends MsgRef {

	/** 
	 * This is RTP. 
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
	 * Constructor
	 * @param type
	 */
	public RTPRef(String type) {
		super(type);
		this.method = "RTP";
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
	   * Creates a string representation of the container and its
	   * contents.
	   */
	  @Override
	public String toString() {
		  String result = super.toString();
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
			RTPRef retval = (RTPRef)super.clone();
			if (retval != null ) {
				if (this.method != null) 
					retval.method = new String(this.method);
				if (this.hdr != null) 
					retval.hdr = new String(this.hdr);
				if (this.parameter != null) 
					retval.parameter = new String(this.parameter);
			}	

			return retval;
		}
		
		@Override
		public String display() {
			String result = null;
			if (super.addRef || super.subRef)
				result = "(" + method; 
			else 
				result = method;
			
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
