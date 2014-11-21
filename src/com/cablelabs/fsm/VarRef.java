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

import com.cablelabs.tools.SDPLocator;
import com.cablelabs.tools.SIPLocator;


public class VarRef extends MsgRef {

	/**
	 * This is the global variable name that the
	 * script assigned to some string or array
	 * for future referencing
	 * 
	 */
//	private String varName = null;
	
	private Integer [] indexes = null;
	/**
	 * The actual data that this variable is holding for
	 * future referencing
	 */
	private Variable variable = null;
	
	/**
	 * Allows the user to define the hdr value to obtain when the system
	 * stores an event in the variable
	 */
	private String hdr = null;
	
	/**
	 * Allows the user to define the param value to obtain when the system
	 * stores an event in the variable
	 */
	private String param = null;
	
	/**
	 * Allows the user to define the protocol value to obtain when the system
	 * stores an event in the variable
	 */
	private String protocol = null;
	
	/**
	 * A handle to the SIP Locator for retrievial of the data we need
	 */
	static private SIPLocator sipLocator = SIPLocator.getInstance();
	
	/**
	 * A handle to the SDP Locator for retrievial of the data we need
	 */
	static private SDPLocator sdpLocator = SDPLocator.getInstance();
	
	public VarRef(String name) {
		super(name);
	}

	public VarRef(String name, Integer [] ndxs) {
		super(name);
		this.indexes = ndxs;
	}
	
	public String getName() {
		return super.type;
	}

	public Variable getVariable() {
		return variable;
	}
	
	public void setVariable(Variable var) {
		this.variable = var;
	}

	public Integer [] getIndexes() {
		return indexes;
	}
	
	@Override
	public String toString() {
		String result = "Global variable " + type;
		if (needsResolving()) {
			result += " protocol=" + protocol + " hdr=" + hdr;
			if (param != null) {
				result += " param=" + param;
			}
			if (hdrInstance != null) {
				result += " hdr_instance=" + hdrInstance;
			}
		}
		if (indexes != null) {
		
			result += " [ ";
		    for (int i = 0; i < indexes.length; i++) {
		    	result += " " + indexes[i];
		    }
		    result += " ] ";
		}
		return result;
	}
	
	/**
	 * Creates a copy of the class
	 */
	@Override
	public Object clone()  throws CloneNotSupportedException {
		VarRef retval = (VarRef)super.clone();
		if (retval != null) {
//			if (this.varName != null) 
//				retval.varName = new String(this.varName);
			// The variable value should never be copied
			// set to null
			if (needsResolving()) {
				retval.protocol = new String(this.protocol);
				retval.hdr = new String(this.hdr);
				if (param != null)
					retval.param = new String(this.param);
				if (hdrInstance != null) 
					retval.hdrInstance = new String(this.hdrInstance);
			}
			if (indexes != null)
				retval.indexes = this.indexes.clone();
			retval.variable = null;
		}	
		
		return retval;
		
	}
	
	@Override
	public String display() {
		String result = "variable " + type;
		if (needsResolving()) {
			result += " protocol=" + protocol + " hdr=" + hdr;
			if (param != null) {
				result += " param=" + param;
			}
			if (hdrInstance != null) {
				result += " hdr_instance=" + hdrInstance;
			}
		}
		if (indexes != null) {
			result += " [ ";
		    for (int i = 0; i < indexes.length; i++) {
		    	result += " " + indexes[i];
		    }
		    result += " ] ";
		}
		if (mask != null) {
				if (and)
					result += " & " + mask;
				else
					result += " | " + mask;
			}
		if (variable != null)
			result += " " + variable;
		
		return result;
	}
	
	public void setHdr(String h) {
		this.hdr = h;
	}
	
	public String getHdr() {
		return this.hdr;
	}
	
	public void setParam(String p) {
		this.param = p;
	}
	
	public String getParam() {
		return this.param;
	}
	
	public void setProtocol(String p) {
		this.protocol = p;
	}
	
	public String getProtocol() {
		return this.protocol;
	}
	
	public boolean needsResolving() {
		if (super.type != null && hdr != null)
			return true;
		return false;
	}
	
	public String resolve(Variable var) {
		String value = var.getElement();
		if (needsResolving()) {
			if (protocol.equals(MsgRef.SIP_MSG_TYPE)) {
				if (param != null) 
					value = sipLocator.getSIPParameter(hdr, 
							param, super.hdrInstance, value);
				else 
					value = sipLocator.getSIPHeader(hdr,
							super.hdrInstance, value);
			}
			else if (protocol.equals(MsgRef.SDP_MSG_TYPE)) {
				String sdp = value;
				if (var.isSIPMsg()) {
					int body = value.indexOf("\r\n\r\n");
					if (body != -1)
						sdp = value.substring(body+4);
				}
				else if (var.isSDPBody()) {
					// Do nothing as this is what we want and value 
					// should contain it.
				}
				
				if (param != null) 
					value = sdpLocator.getSDPParameter(hdr, 
							param, super.hdrInstance, 
							MsgQueue.FIRST, null, sdp);
				else 
					value = sdpLocator.getSDPHeader(hdr,
							super.hdrInstance, MsgQueue.FIRST, null, sdp);
			}
		}
		return value;
	}
}
