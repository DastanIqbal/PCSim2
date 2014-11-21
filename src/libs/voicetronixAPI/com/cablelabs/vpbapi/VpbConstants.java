/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.vpbapi;

public class VpbConstants {

	public static final String OFFHOOK = "OFFHOOK";
	public static final String DIAL_DIGITS = "DIALDIGITS";
	public static final String ONHOOK = "ONHOOK";
	public static final String HOOKFLASH = "HOOKFLASH";
	public static final String OFFHOOK_COMPLETE = "OFFHOOKCOMPLETE";
	public static final String DIAL_DIGITS_COMPLETE = "DIALDIGITSCOMPLETE";
	public static final String ONHOOK_COMPLETE = "ONHOOKCOMPLETE";
	public static final String HOOKFLASH_COMPLETE = "HOOKFLASHCOMPLETE";
	public static final String OFFHOOK_ERROR = "OFFHOOKERROR";
	public static final String DIAL_DIGITS_ERROR = "DIALDIGITSERROR";
	public static final String ONHOOK_ERROR = "ONHOOKERROR";
	public static final String HOOKFLASH_ERROR = "HOOKFLASHEROR";
	public static final String VERIFY_BUSY = "VERIFYBUSY";
	public static final String VERIFY_BUSY_COMPLETE = "VERIFYBUSYCOMPLETE";
	public static final String VERIFY_BUSY_ERROR = "VERIFYBUSYERROR";
	public static final String VERIFY_DIAL_TONE = "VERIFYDIALTONE";
	public static final String VERIFY_DIAL_TONE_COMPLETE = "VERIFYDIALTONECOMPLETE";
	public static final String VERIFY_DIAL_TONE_ERROR = "VERIFYDIALTONEERROR";
	public static final String VERIFY_RING = "VERIFYRING";
	public static final String VERIFY_RING_COMPLETE = "VERIFYRINGCOMPLETE";
	public static final String VERIFY_RING_ERROR = "VERIFYRINGERROR";
	public static final String VERIFY_RING_BACK = "VERIFYRINGBACK";
	public static final String VERIFY_RING_BACK_COMPLETE = "VERIFYRINGBACKCOMPLETE";
	public static final String VERIFY_RING_BACK_ERROR = "VERIFYRINGBACKERROR";
	public static final String VERIFY_REORDER = "VERIFYREORDER";
	public static final String VERIFY_REORDER_COMPLETE ="VERIFYREORDERCOMPLETE";
	public static final String VERIFY_REORDER_ERROR = "VERIFYREORDERERROR";
	public static final String VERIFY_CALL_WAITING_TONE = "VERIFYCALLWAITINGTONE";
	public static final String VERIFY_CALL_WAITING_TONE_COMPLETE = "VERIFYCALLWAITINGTONECOMPLETE";
	public static final String VERIFY_CALL_WAITING_TONE_ERROR = "VERIFYCALLWAITINGTONEERROR";
	public static final String VERIFY_VOICE_PATH = "VERIFYVOICEPATH";
	public static final String VERIFY_VOICE_PATH_COMPLETE = "VERIFYVOICEPATHCOMPLETE";
	public static final String VERIFY_VOICE_PATH_ERROR = "VERIFYVOICEPATHERROR";
	public static final String VERIFY_VOICE_PATH_TWO_WAY = "VERIFYVOICEPATH2WAY";
	public static final String VERIFY_VOICE_PATH_TWO_WAY_COMPLETE = "VERIFYVOICEPATH2WAYCOMPLETE";
	public static final String VERIFY_VOICE_PATH_TWO_WAY_ERROR = "VERIFYVOICEPATH2WAYERROR";
	
	public static final int DIALTONE = 1;
	public static final int BUSY = 7;
	public static final int NO_ANSWER = 8;
	public static final int NO_RESPONSE = 9;
	public static final int CONNECT = 10;
	public static final int INTERCEPT = 11;
	public static final int FAX = 12;
	
	public static VpbEventType getEventType(String type) {
		if (type.equals(VpbEventType.BUSY.toString()))
			return VpbEventType.BUSY;
		else if (type.equals(VpbEventType.CALL_WAITING_TONE.toString()))
			return VpbEventType.CALL_WAITING_TONE;
		else if (type.equals(VpbEventType.DIAL.toString()))
			return VpbEventType.DIAL;
		else if (type.equals(VpbEventType.DIALTONE.toString()))
			return VpbEventType.DIALTONE;
		else if (type.equals(VpbEventType.REORDER.toString()))
			return VpbEventType.REORDER;
		else if (type.equals(VpbEventType.RINGBACK.toString()))
			return VpbEventType.RINGBACK;
		else if (type.equals(VpbEventType.RINGING.toString()))
			return VpbEventType.RINGING;
		else if (type.equals(VpbEventType.VOICE_DETECTED.toString()))
			return VpbEventType.VOICE_DETECTED;

		return VpbEventType.UNKNOWN;
		
	}
	
	public static String getErrorEventType(VpbEventType type) {
		switch (type) {
			case BUSY:
				return VERIFY_BUSY_ERROR;
			case CALL_WAITING_TONE:
				return VERIFY_CALL_WAITING_TONE_ERROR;
			case DIAL:
				return DIAL_DIGITS_ERROR;
			case DIALTONE:
				return VERIFY_DIAL_TONE_ERROR;
			case REORDER:
				return VERIFY_REORDER_ERROR;
			case RINGBACK:
				return VERIFY_RING_BACK_ERROR;
			case RINGING:
				return VERIFY_RING_ERROR;
			case VOICE_DETECTED:
				return VERIFY_VOICE_PATH_ERROR;
				
		}
		return null;
	}
	
	public static String getEventType(VpbEventType type) {
		switch (type) {
			case BUSY:
				return VERIFY_BUSY_COMPLETE;
			case CALL_WAITING_TONE:
				return VERIFY_CALL_WAITING_TONE_COMPLETE;
			case DIAL:
				return DIAL_DIGITS_COMPLETE;
			case DIALTONE:
				return VERIFY_DIAL_TONE_COMPLETE;
			case REORDER:
				return VERIFY_REORDER_COMPLETE;
			case RINGBACK:
				return VERIFY_RING_BACK_COMPLETE;
			case RINGING:
				return VERIFY_RING_COMPLETE;
			case VOICE_DETECTED:
				return VERIFY_VOICE_PATH_COMPLETE;
				
		}
		return null;
	}
}
