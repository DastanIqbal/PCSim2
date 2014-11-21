/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun;

import java.io.IOException;

public class StunException extends IOException {

	static final long serialVersionUID = 1;
	
	/**
	 * Constructor
	 *
	 */
	public StunException () {
		super();
	}
	
	/**
	 * Constructor
	 * @param s
	 */
	public StunException (String s) {
		super(s);
	}
}
