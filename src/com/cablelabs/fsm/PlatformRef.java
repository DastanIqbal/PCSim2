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
 * A container for a message reference to Platform information.
 * 
 * @author ghassler
 *
 */
public class PlatformRef extends MsgRef{

	/**
	 * A reference to a label in the Platform Settings
	 * properties
	 */
	private String neLabel;

	/**
	 * The specific parameter in the network element identified
	 * by the labeled neLable attribute within the Platform Settings
	 * properties.
	 */
	private String parameter;
	
	/**
	 * Constructor
	 * @param type
	 */
	public PlatformRef(String type) {
		super(type);
	}
	
	/**
	 * Sets the NE label of the element being referenced
	 * @param label
	 */
	public void setNELabel(String label) {
		this.neLabel = label;
	}
	
	/**
	 * Sets the specific parameter (property key) being 
	 * referenced.
	 * @param param
	 */
	public void setParameter(String param) {
		this.parameter = param;
	}
	
	/**
	 * Gets the parameter.
	 * @return
	 */
	public String getParameter() {
		return parameter;
	}
	
	/**
	 * Gets the NE label.
	 * @return
	 */
	public String getNELabel() {
		return neLabel;
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = neLabel;
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
		PlatformRef retval = (PlatformRef)super.clone();
		if (retval != null ) {
			if (this.neLabel != null) 
				retval.neLabel = new String(this.neLabel);
			if (this.parameter != null) 
				retval.parameter = new String(this.parameter);
		}	

		return retval;
	}
	
	@Override
	public String display() {
		return toString();
	}
}
