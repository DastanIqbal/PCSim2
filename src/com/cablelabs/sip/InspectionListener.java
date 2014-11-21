/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.sip;


/**
 * This class abstracts the application layer from the stack, in order for
 * the SIP stack to notify the app layer that an error occurred in the 
 * syntax of a SIP message as part of the automatically tested steps identified
 * in Appendix B of the SIP ATP.
 * 
 * @author ghassler
 *
 */
public class InspectionListener {

	private static InspectionListener listener = null;
	
	/**
	 * Stack processing encountered a failure
	 */
	private boolean stackFailure = false;
	
	/**
	 * Private constructor of the singleton class.
	 *
	 */
	private InspectionListener() {
		super();
	}

	/**
	 * Provides a reference to the class. Since the class
	 * is a singleton, if it doesn't already exist, it will
	 * create an instance and then return a reference to it.
	 * 
	 * @return the single instantiation of the class.
	 */
	public synchronized static InspectionListener getInstance() {
		if (listener == null) {
			listener = new InspectionListener();
		}	
		return listener;
	}
	public void inspectionFailure() {
		this.stackFailure = true;
	}
	
	public boolean getStackFailure() {
		return this.stackFailure;
	}
	
	public void reset() {
		this.stackFailure = false;
	}
	
}
