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

import com.cablelabs.tools.SIPLocator;

public class SIPBodyRef extends SIPRef {

	private boolean xmlValue = false;
	
	private boolean text = false;
	
	public static final int PARENT = SIPLocator.PARENT;
	public static final int GRANDPARENT = SIPLocator.GRANDPARENT;
	private int ancestor = -1;


	/**
	 * Constructor
	 * @param type
	 */
	public SIPBodyRef(String type, boolean xml, boolean text) {
		super(type);
		this.xmlValue = xml;
		this.text = text;
	}

	 /**
	  * Gets the text body indication flag.
	  * @return
	  */
	 public boolean isTextBody() {
		 return text;
	 }
	 
	public boolean isXMLValue() {
		return this.xmlValue;
	}
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = super.toString();
		if (xmlValue)
			result += " xmlValue=" + xmlValue;
		if (text)
			result += " text=" + text;
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
		SIPBodyRef retval = (SIPBodyRef)super.clone();
			retval.xmlValue = this.xmlValue;
			retval.text = this.text;
		return retval;
	}
	
	@Override
	public String display() {
		String result = "";
		if (response) {
			if (super.addRef || super.subRef)
				result = "(" + statusCode + "-" + method + "[" + msgInstance + "]"; 
			else
				result += statusCode + "-" + method + "[" + msgInstance + "]";
		}
		else  {
			if (super.addRef || super.subRef)
				result = "(" + method + "[" + msgInstance + "]"; 
			else
				result += method + "[" + msgInstance + "]";
		}
		if (hdr != null)
			result += "." + hdr + "[" + hdrInstance + "]";
		if (parameter != null) 
			result += "." + parameter;
		
		if (text)
			result += " text body";
		else if (xmlValue)
			result += " xml body";
		

		if (super.addRef) {
			result += " + " + super.getArithmeticMod() + ")";
		}
		else if (super.subRef) {
			result += " - " + super.getArithmeticMod() + ")";
		}
		return result;
	}
	
	
	public int getAncestor() {
		return ancestor;
	}

	public void setAncestor(int ancestor) {
		this.ancestor = ancestor;
	}
	
	public void setParent() {
		this.ancestor = PARENT;
	}
	
	public void setGrandparent() {
		this.ancestor = GRANDPARENT;
	}
	
	public boolean hasAncestor() {
		if (ancestor >= PARENT && ancestor <= GRANDPARENT)
			return true;
		return false;
	}
}
