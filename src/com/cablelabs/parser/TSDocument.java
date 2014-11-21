/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.parser;

import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.SettingConstants;


/**
 * A container class for the results of the TSParser.
 * It contains some information that will be used to
 * create the log file and T.I.M results file.
 * 
 * @author ghassler
 *
 */

public class TSDocument {

	/**
	 * The name of the Test Case.
	 */
	private String name;
	
	/**
	 * The fileName this TestCase was parsed from.
	 */
	private String fileName;

	/**
	 * A description of the Test Case.
	 */
	private String descrip;

	/**
	 * The Test Case number.
	 */
	private String number;

	/**
	 * A setting to enable/disable the inspector's influence 
	 * upon the results upon the test.
	 */
	private boolean inspector = false;
	
	/**
	 * A list of the FSMs defined in the Test Case.
	 */
	private LinkedList<FSM> fsms;
	
	/**
	 * The name of the current log file being used for this
	 * test.
	 */
	private String logFileName = null;
	
	/**
	 * The time the document started to be parsed.
	 */
	public Date start = new Date();
	
	/**
	 * This is a container to hold any dynamic platform configuration
	 * changes that need to be made while executing the test defined 
	 * by this document.
	 */
	public Properties platformChanges = null;
	
	/**
	 * The version of the XML Document being used to conduct the test
	 */
	private String version = null;
	
	public final static String REVISION = "$Revision: ";
	
	/**
	 * Get the description of the document.
	 */
	public String getDescrip() {
		return descrip;
	}
	
	
	/**
	 * Sets the description of the document.
	 * @param descrip - the descrip attribute of the 
	 * 				<pc2xml> element within the document.
	 */
	public void setDescrip(String descrip) {
		this.descrip = descrip;
	}
	
    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
	 * Get the name of the document.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the document.
	 * @param name - the name attribute of the 
	 * 				<pc2xml> element within the document.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Get the test case number of the document.
	 */
	public String getNumber() {
		return number;
	}
	
	/**
	 * Sets the test case number of the document.
	 * 
	 * @param number - the number attribute of the 
	 * 				<pc2xml> element within the document.
	 */
	public void setNumber(String number) {
		this.number = number;
	}

	/**
	 * Gets a list of the FSMs within the document.
	 */
	public LinkedList<FSM> getFsms() {
		return fsms;
	}

	public FSM getFsm(String fsm) {
		ListIterator<FSM> iter = fsms.listIterator();
		while (iter.hasNext()) {
			FSM f = iter.next();
			if (f.getName().equals(fsm))
				return f;
		}
		return null;
	}
	/**
	 * Sets the FSMs within the document.
	 * @param fsms - a linked list of all the FSMs constructed
	 * 			during the parsing of the document.
	 */
	public void setFsms(LinkedList<FSM> fsms) {
		this.fsms = fsms;
	}
	
	/**
	 * Sets the document override for the inspector influencing 
	 * the test results.
	 *
	 */
	public void enableInspector() {
		this.inspector = true;
	}
	
	/**
	 * Gets the document override for the inspector influencing 
	 * the test results.
	 *
	 */
	public boolean getInspector() {
		return this.inspector;
	}
	
	/**
	 * Gets the name of the current log file being used for this
	 * test case.
	 * 
	 */
	public String getLogFileName() {
		return logFileName;
	}

	
	/**
	 * Sets the name of the current log file being used for this
	 * test case.
	 * 
	 */
	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}

	/**
	 * Gets the start time of the test case.
	 * 
	 */
	public Date getStart() {
		return this.start;
	}
	
	/**
	 * Gets the version of the test script.
	 * 
	 */
	public String getVersion() {
		return this.version;
	}
	
	/**
	 * Sets the version of the test script.
	 * 
	 */
	protected void setVersion(String v) {
		if (v.startsWith(REVISION)) {
			int offset = v.indexOf("$", REVISION.length());
			if (offset != -1)
				this.version = v.substring(REVISION.length(), offset-1);
			else 
				this.version = v.substring(REVISION.length());
		}
		else 
			this.version = v;
	}
	/**
	 * A string representation of the contains of this
	 * container class.
	 * @return String - for logging or displaying.
	 */
	@Override
	public String toString() {
		String result = "Document version=\"" + version 
				+ "\" name=\"" + name + "\"\n\tdescrip=\"" 
				+ descrip + "\"\n\tnumber=\"" + 
			number + "\"\n\tinspector=" + inspector + "\n";
		int numFSMs = fsms.size();
		FSM f = null;
		for (int i = 0; i < numFSMs; i++) {
			f = fsms.get(i);
			result += f.toString();
		}
		return result;
	}
	
	public void addProperty(String name, String value) {
		if (platformChanges == null)
			platformChanges = new Properties();
		platformChanges.setProperty(name, value);
	}
	
	public Properties getProperties() {
		return this.platformChanges;
	}
	
	public boolean documentConfigurableProperty(String name) {
		return SettingConstants.documentConfigurableProperty(name);
//		if (name != null &&
//				name.equals("FSM Process Duplicate Messages") ||
//				name.equals("SIP Default Transport Protocol") ||
//				name.equals("No Response Timeout") ||
//				name.equals("SIP Inspector") ||
//				name.equals("SIP Inspector Type"))
//			return true;
//		return false;
	}
	
	public String getTestStats() {
		String result = "   Documented Requirements Summary:\n";
		ListIterator<FSM> iter = fsms.listIterator();
		//boolean newline = false;
		while (iter.hasNext()) {
			//newline = true;
			FSM fsm = iter.next();
			result += fsm.getName() +":\n" + fsm.getTestStats();
			
		}
		//if (newline)
		//	result += "\n";
		return result;
	}
	public boolean useValidate() {
		boolean result = false;
		ListIterator<FSM> iter = fsms.listIterator();
		while (iter.hasNext() && !result) {
			FSM fsm = iter.next();
			result = fsm.useValidate();
		}
		return result;
	}
	
//	public void setAutoProv(ProvisioningData pd) {
//		// This means we are going to extend each FSM with a new initial state 
//		// created by the platform during the execution.
//		boolean result = false;
//		ListIterator<FSM> iter = fsms.listIterator();
//		boolean msgSender = true;
//		while (iter.hasNext() && !result) {
//			FSM fsm = (FSM)iter.next();
//			try {
//				// Only one FSM needs to set the policy and provisioning files
//				// all the others simply need a new state 
//				if (msgSender) {
//					fsm.addState(new AutoProvState(fsm, pd));
//					fsm.setInitialState(AutoProvState.NAME);
//					msgSender = false;
//				}
//				else {
//					fsm.addState(new AutoProvWaitState(fsm));
//					fsm.setInitialState(AutoProvWaitState.NAME);
//				}
//				
//			}
//			catch (PC2Exception pce) {
//
//			}
//		}
//	}
	
	/**
	 * This method determines if all of the verification have been
	 * conducted and what the cumulative results for all of the 
	 * FSMs from this test.
	 * 
	 * @return - true if every FSM completed all of its verifies,
	 * 		false otherwise.
	 */
	public boolean validateTest() {
		boolean result = false;
		ListIterator<FSM> iter = fsms.listIterator();
		if (iter.hasNext()) {
			FSM fsm = iter.next();
			result = fsm.validateTest();
			while (iter.hasNext()) {
				fsm = iter.next();
				result = result & fsm.validateTest();
			}
		}
		return result;
	}
}
