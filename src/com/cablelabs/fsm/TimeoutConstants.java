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
 * This class defines all of the Timeout events types allowed within a
 * PC 2.0 XML document and provides a method to validate each entry as it
 * is processed by the parser.
 * 
 * @author ghassler
 *
 */
public class TimeoutConstants {

	public static final String SLEEP_TIMEOUT = "SleepTimeout";
	public static final String NO_RESPONSE_TIMEOUT = "NoResponseTimeout";
	public static final String TIMER_EXPIRED = "TimerExpired";
	
	/**
	 * A string representation of the currently supported platform internal events.
	 * 
	 */
	static public String getEvents() {
		String result = SLEEP_TIMEOUT 
		+ ", " + NO_RESPONSE_TIMEOUT 
		+ ", " + TIMER_EXPIRED
			
		;
		return result;
		
	}
	/**
	 * Test whether the given event is a timeout event
	 * 
	 * @param event - the event to test
	 * @return - true if the event is a timeout event, false otherwise.
	 */
	static public boolean isTimeoutEvent(String event) {
		if (SLEEP_TIMEOUT.equals(event) ||
				NO_RESPONSE_TIMEOUT.equalsIgnoreCase(event) ||
				TIMER_EXPIRED.equalsIgnoreCase(event)) {
			return true;
		}
		return false;
	}
}
