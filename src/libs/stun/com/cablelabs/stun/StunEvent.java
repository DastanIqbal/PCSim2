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

public class StunEvent {

	/**
	 * The STUN event
	 */
	private StunMessage event = null;
	

	/**
	 * The Raw Data container
	 * 
	 */
	private RawData data = null;
	
	/**
	 * Constructor
	 * 
	 * @param event - the STUN event
	 * @param rawData - the RawData container that contains the 
	 * 	information used to decode the StunMessage.
	 * 
	 */
	public StunEvent(StunMessage msg, RawData rawData) {
		this.event = msg;
		this.data = rawData;
	}
	
	/**
	 * Gets the stun event
	 * @return
	 */
	public StunMessage getEvent() {
		return event;
	}
	
	/**
	 * Gets the raw data used to create the
	 * StunMessage.
	 * @return
	 */
	public RawData getRawData() {
		return this.data;
	}
	
	
}