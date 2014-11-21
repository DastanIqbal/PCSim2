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
 * This class defines all of the reference points allowed within a
 * PC 2.0 XML document and provides a method to validate each entry as it
 * is processed by the parser.
 * 
 * @author ghassler
 *
 */
public class ReferencePointConstants {

	// UE - terminating reference points
	public static final String PROV_RSP_RX_UAC = "PROV_RSP_RX_UAC";
	public static final String EARLY_DIALOG_UAC = "EARLY_DIALOG_UAC";
	public static final String ALERTING_UAC = "ALERTING_UAC";
	public static final String DIALOG_CONFIRMED_UAC = "DIALOG_CONFIRMED_UAC";
	
	// UE- originating reference points 
	public static final String EARLY_DIALOG_UAS = "EARLY_DIALOG_UAS";
	public static final String DIALOG_CONFIRMED_UAS = "DIALOG_CONFIRMED_UAS";
	public static final String PROV_RSP_TX_UAS = "PROV_RSP_TX_UAS";
	public static final String INVITE_RECEIVED_UAS = "INVITE_RECEIVED_UAS";
	public static final String ALERTING_UAS = "ALERTING_UAS";
	
	/**
	 * A string representation of the currently supported platform internal events.
	 * 
	 */
	static public String getEvents() {
		String result = ALERTING_UAC 
		+ ", " + ALERTING_UAS 
		+ ", " + DIALOG_CONFIRMED_UAC
		+ ", " + DIALOG_CONFIRMED_UAS
		+ ", " + EARLY_DIALOG_UAC 
		+ ", " + EARLY_DIALOG_UAS
		+ ", " + INVITE_RECEIVED_UAS
		+ ", " + PROV_RSP_RX_UAC
		+ ", " + PROV_RSP_TX_UAS

		;
		return result;
		
	}
	/**
	 * A class that determines if the supplied event is a reference point
	 * or not. Reference Points are case-sensitive.
	 *  
	 * @param event - the event to validate
	 * @return - true if it is a recognized Reference Point event, false
	 * 		otherwise.
	 */
	static public boolean isReferencePointEvent(String event) {
		if (PROV_RSP_RX_UAC.equals(event) ||
				EARLY_DIALOG_UAC.equals(event) ||
				ALERTING_UAC.equals(event) ||
				DIALOG_CONFIRMED_UAC.equals(event) ||
				EARLY_DIALOG_UAS.equals(event) ||
				DIALOG_CONFIRMED_UAS.equals(event) ||
				PROV_RSP_TX_UAS.equals(event) ||
				INVITE_RECEIVED_UAS.equals(event) ||
				ALERTING_UAS.equals(event)) {
			return true;
		}
		return false;
	}
}
