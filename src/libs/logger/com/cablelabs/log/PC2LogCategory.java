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

//public enum PC2LogCategory {
//	ALL, Diameter, Examiner, FSM, LogMsg, Main, Model, MsgQueue, Parser, PCSim2, Reader, SIP, STUN, Settings, UI, UTILITY
//}


public class PC2LogCategory extends LogCategory {

	public static final LogCategory Diameter = new PC2LogCategory("Diameter");
	public static final LogCategory Examiner = new PC2LogCategory("Examiner");
	public static final LogCategory FSM = new PC2LogCategory("FSM");
	public static final LogCategory Main = new PC2LogCategory("Main");
	public static final LogCategory Model = new PC2LogCategory("Model");
	public static final LogCategory MsgQueue = new PC2LogCategory("MsgQueue");
	public static final LogCategory Parser = new PC2LogCategory("Parser");
	public static LogCategory PCSim2 = LogCategory.APPLICATION;
	public static final LogCategory Reader = new PC2LogCategory("Reader");
	public static final LogCategory SIP = new PC2LogCategory("SIP");
	public static final LogCategory STUN = new PC2LogCategory("STUN");
	public static final LogCategory Settings = new PC2LogCategory("Settings");
	public static final LogCategory UI = new PC2LogCategory("UI");
	public static final LogCategory UTILITY = new PC2LogCategory("UTILITY");

	// Required constructors
	public PC2LogCategory() {
	    super();
	}
	
	public PC2LogCategory(String name) {
        super(name);
    }
	
	@Override
	public String getApplicationName() {
		return "PCSim2";
	}
	
	public static void updateAppCategoriesFinished() {
	    PCSim2 = LogCategory.APPLICATION;
	}

}