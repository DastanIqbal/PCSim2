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
 * The container for the expr elements within the PC 2.0
 * Simulator's XML document.
 * 
 * @author ghassler
 *
 */
public class Literal implements Reference {

	/**
	 * The literal string between the beginning and ending
	 * <expr> tags.
	 */
	private String expr;
	
	/**
	 * Constructor.
	 */
	public Literal() {
	}
	
	/**
	 * Constructor.
	 */
	public Literal(String expr) {
	
		this.expr = expr;
	}
	
	/**
	 * Gets the literal expression.
	 * @return
	 */
	public String getExpr() {
		return expr;
	}
	
	/**
	 * Sets the expression.
	 * @param e
	 */
	public void setExpr(String e) {
		this.expr = e;
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString () {
		return expr;
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		Literal retval = (Literal)super.clone();
		if (retval != null ) {
			if (this.expr != null) 
				retval.expr = new String(this.expr);
		}	

		return retval;
	}
	
	@Override
	public String display() {
		return this.expr;
	}
}
