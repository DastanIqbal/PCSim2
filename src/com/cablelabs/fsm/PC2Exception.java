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

import java.io.IOException;

/**
 * An exception defintion for IO processing encountered within the 
 * platform.
 * 
 * @author ghassler
 *
 */
public class PC2Exception extends IOException {

	static final long serialVersionUID = 1;
	
	/**
	 * Constructor
	 *
	 */
	public PC2Exception () {
		super();
	}
	
	/**
	 * Constructor
	 * @param s
	 */
	public PC2Exception (String s) {
		super(s);
	}

}

