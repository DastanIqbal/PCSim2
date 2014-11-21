/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.log;

/**
 * This class creates an interface for external entities to perform
 * operations on the 500 ms firing of the LogMonitor thread. This allow
 * systems to reuse this thread to perform an operation on a consistent
 * basis without having to create their own threads each time. The
 * object simply implements this interface and adds the object to the
 * PC2LogMontitor's listeners and it will invoke the method every timer
 * interval (500ms).
 *
 * @author ghassler
 *
 */
public interface MonitorListener {

	public void timerTick();
}
