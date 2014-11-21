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
 * Container class for the msg_ref element of a PC 2.0 Simulator
 * XML document with the type attribute set to "extension".
 * 
 * @author ghassler
 *
 */
public class ExtensionRef extends MsgRef {

	/**
	 * The extension that this class refers to.
	 */
	private String ext;
	
	/**
	 * Constructor.
	 * @param type
	 */
	public ExtensionRef(String type) {
		super(type);
	}

	/**
	 * Gets the extension.
	 * 
	 */
	public String getExt() {
		return ext;
	}

	/**
	 * Sets the extension.
	 * 
	 */
	public void setExt(String ext) {
		this.ext = ext;
	}
	
	/**
	 * A string representation of the extension.
	 */
	@Override
	public String toString() {
		return ext;
	}
	
	 /** This implements a deep copy of the class for replicating 
	  * FSM information.
	  * 
	  * @throws CloneNotSupportedException if clone method is not supported
	  * @return Object
	  */ 
	 @Override
	public Object clone() throws CloneNotSupportedException {
		 ExtensionRef retval = (ExtensionRef)super.clone();
		 if (retval != null) {
			 if (this.ext != null) 
				 retval.ext = new String(this.ext);
		 }	

		 return retval;
	 }
	 
	@Override
	public String display() {
		return ext + " extension setting";
	}
}

