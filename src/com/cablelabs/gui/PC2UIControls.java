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

public interface PC2UIControls {

	/** 
	 * Allows the application to add a new element to the 
	 * registered endpoints' in the user interface.
	 * @param ip - The IP Address of the device attempting to
	 * 		register
	 * @param neLabel - The NE label that the device has been
	 * 		identified as in the configuration parameters.
	 */
	public void addRegistrarElement(String ip, String neLabel);
	
	/**
	 * Allows the application to update the registered endpoints'
	 * network element label being used to communicate to the 
	 * device.
	 * 
	 * @param ip - The Public User Identity of the device (key of the table)
	 * @param curLabel - The current NE label for the device
	 * @param newLabel - The new NE label for the device
	 */
	public void changeRegistrarLabel(String pui, String curLabel, String newLabel);
	
	/**
	 * Allows the application or global registrar to change the 
	 * current state(status) of the devices registration status.
	 * 
	 * @param status - The current registration status of the device
	 * @param pui - The Public User Identitiy of the device (key of the table)
	 * @param curLabel - The current NE label for the device
	 */
	public void changeRegistrarStatus(PC2RegistrarStatus status,
			String pui, String curLabel);
	/**
	 * Allows the application to update the registered endpoints'
	 * information as the platform processes new requests and 
	 * conducts multiple tests.
	 * 
	 * @param status - The current registration status of the device
	 * @param ip - The IP Address of the device (key of the table)
	 * @param curLabel - The current NE label for the device
	 * @param newLabel - The new NE label for the device
	 */
	//public void updateRegistrarElement(PC2RegistrarStatus status,
	//		String ip, String curLabel, String newLabel);
	
	/**
	 * Allows the application to empty the registered endpoints'
	 * information as the platform closes a batch file.
	 * 
	 */
	public void clearRegistrarElements();
	
	/**
	 * This method allows for the addition/update of a dut/test case
	 * pair's execution by the platform.
	 *
	 * @param r - The current state of the test
	 * @param runNum - The run number of this test case.
	 * @param tcFile - The test script file name to add/modify.
	 * @param dutFile - The DUT configuration file name to add/modify.
	 * 
	 *  NOTE the run number, dutFile and tcFile together form the key to the table.
	 */
	public void setTestResults(PC2Result r, 
			String runNum, String tcFile, String dutFile);
	
	/**
	 * This method allows the user interface to highlight which dut and
	 * test script files are being executed.
	 * 
	 * @param runNum - The run number of this test case.
	 * @param tsName - The path and file name of the Test Script 
	 * @param dutName - The path and file name of the DUT Configuration
	 */
	public void startingTest(String runNum, String tsName, String dutName);
	
	/**
	 * This method informs the control pane that the testing of a
	 * test pair is complete and to deselect them in the UI.
	 */
	public void testComplete();
	
	/**
	 * This method notifies the user interface when all test pairs
	 * have been attempted and are complete. 
	 *
	 */public void testsComplete();
	
	/**
	 * This method implements the dialog to prompt the tester to perform
	 * some manual task during a test.   
	 * 
	 * @param msg - The string to be displayed.
	 * @param verify - The string is a verification request
	 * @param yesExpected - The user is expected to respond with a yes
	 * 
	 * @return - true if the user responded as expected, false otherwise
	 */
	public boolean notifyUser(String msg, boolean verify, boolean yesExpected);
}
