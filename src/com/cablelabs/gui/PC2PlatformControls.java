/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.gui;

import java.io.File;
import java.util.LinkedList;

public interface PC2PlatformControls {

	/**
	 * This interface allows the UI to give the platform the
	 * platform settings file and to begin the global
	 * registrar if enabled.
	 * 
	 * @param fileName - name of the configuration file
	 * @return - true
	 */
	public boolean setPlatformSettings(String fileName);
	
	/**
	 * Informs the platform to begin conducting the test
	 * scripts with the list of DUT configuration files
	 * and which set is to be the primary set.
	 * 
	 * @param dutFiles - a list of DUT configuration files
	 * @param testCaseFiles - a list of test script files
	 * @param dutPrimary - true if the DUT files are the primary set.
	 * @return - true
	 */
	public boolean startTests(LinkedList<File> dutFiles,
			LinkedList<File>testCaseFiles,
			boolean dutPrimary);
	
	/**
	 * This method notifies the platform that the user has
	 * terminated this set of tests.
	 * 
	 * @return - true
	 */
	public boolean stopTests();
	
	/**
	 * Notifies the platform to shutdown.
	 *
	 */
	public void shutdown();
	
	/**
	 * This method causes the platform to read the configuration
	 * file and add an entry to the platform settings based upon
	 * the IP property in the file for the global registrar.
	 *
	 *@return - true if added, false otherwise
	 */
	public boolean addDUTConfig(File f);
	
	/**
	 * This method removes the configuration information 
	 * from the global registrar.
	 *
	 */
	public void removeDUTConfig(File f);
	
	/**
	 * This method notifies the platform to close the current batch file
	 * and clear all resources in the stacks.
	 */
	public void closeBatch();
	
	/**
	 * This method delivers an user event into a FSM for processing
	 */
	public void injectUserEvent(String fsm, String event);
}
