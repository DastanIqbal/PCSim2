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
 * A container class for the information described by the 
 * mod element within a PC 2.0 Simulator XML document.
 * 
 * @author ghassler
 *
 */
public class Mod implements Cloneable {

	/**
	 * The type of modification the class represents:
	 * add, replace or delete.
	 */
	private String modType;
	
	/** 
	 * The header that will be modified.
	 */
	private String hdr;
	
	/**
	 * The instance of the header within the message that
	 * will be modified.
	 */
	private String hdrInstance = MsgQueue.FIRST;
	
	/**
	 * The parameter within the header that will be be modified.
	 */
	private String param;
	
	/**
	 * The reference to use as input when performing the change.
	 */
	private Reference ref;

	/**
	 * A flag specifying that an add should happen prior before
	 * a refence element or after
	 */
	private boolean before = false;
	
	/**
	 * A flag specifying that an add should appear as a separate line
	 * instead of being comma separated
	 */
	private boolean separate = false;
	
	/**
	 * This is the media-subtype of the body to have the modification
	 * performed upon it.
	 */
	private String body = null;
	
	/**
	 * This is whether the value portion of an XML tag will be 
	 * modified.
	 */
	private boolean xmlValue = false;
	
	/**
	 * The index of the body within the message 
	 * 
	 * Default Value: first
	 * Valid Values: first, last, current, any or positive integer
	 */
	protected String bodyInstance = MsgQueue.FIRST;
	
	/**
	 * Constructor.
	 * @param type
	 */
	public Mod(String type) {
		this.modType = type;
	}

	/**
	 * Gets the header field.
	 * @return
	 */
	public String getHeader() {
		return hdr;
	}
	
	/**
	 * Sets the header field.
	 * @param hdr
	 */
	public void setHeader(String hdr) {
		this.hdr = hdr;
	}
	
	/**
	 * Gets the header instance
	 * @return
	 */
	public String getHeaderInstance() {
		return hdrInstance;
	}
	
	/**
	 * Sets the header instance
	 * @param hdrInstance
	 */
	public void setHeaderInstance(String hdrInstance) {
		this.hdrInstance = hdrInstance;
	}
	
	/**
	 * Gets the modification type
	 * @return
	 */
	public String getModType() {
		return modType;
	}
	
	/**
	 * Sets the modification type
	 * @param modType
	 */
	public void setModType(String modType) {
		this.modType = modType;
	}
	
	/**
	 * Gets the parameter
	 * @return
	 */
	public String getParam() {
		return param;
	}
	
	/**
	 * Sets the parameter
	 * @param param
	 */
	public void setParam(String param) {
		this.param = param;
	}
	
	/**
	 * Sets the reference field.
	 * @param ref
	 */
	public void setRef(Reference ref) {
		this.ref = ref;
	}
	
 	/**
 	 * Gets the reference field.
 	 * @return
 	 */
	public Reference getRef() {
 		return ref;
 	}
 	
	public boolean getBefore() {
		return before;
	}
	
	public void setBefore() {
		this.before = true;
	}
	
	public boolean getSeparate() {
		return this.separate;
	}
	
	public void setSeparate() {
		this.separate = true;
	}
	
	public String getBody() {
		return this.body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public boolean isXMLValue() {
		return this.xmlValue;
	}
	
	public void setXMLValue(boolean flag) {
		if (this.body != null) 
			this.xmlValue = flag;
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
		String result = "\t\tmod modType=\"" + modType + "\" hdr=\"" + hdr + "\"";
 		if (!(hdrInstance.equals(MsgQueue.FIRST))) {
 			result += " hdr_instance=\"" + hdrInstance + "\"";
 		}
 		if (!bodyInstance.equals(MsgQueue.FIRST)) {
			result += " bodyInstance=\"" + bodyInstance + "\"";
		}
 		if (param != null) {
 			result += " param=\"" + param + "\"";
 		}
 		if (ref != null) {
 			result += " ref=\"" + ref.toString() + "\"";
 		}
 		if (before != false) {
 			result += " before=true";
 		}
 		if (separate != false) {
 			result += " separate=true";
 		}
 		
 		if (body != null) {
 			result += " body=" + body + " value=" + xmlValue;
 		}
 		
 		result += "\n";
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
		Mod retval = (Mod)super.clone();
		if (retval != null ) {
			if (this.modType != null) 
				retval.modType = new String(this.modType);
			if (this.hdr != null)
				retval.hdr = new String(this.hdr);
			if (this.hdrInstance != null)
				retval.hdrInstance = new String(this.hdrInstance);
			if (this.param != null) 
				retval.param = new String(this.param);
			if (this.ref != null)
				retval.ref = (Reference)this.ref.clone();
			retval.before = this.before;
			retval.separate = this.separate;
			if (this.body != null)
				retval.body = new String(this.body);
			retval.xmlValue = this.xmlValue;
		}	

		return retval;
	}
}
