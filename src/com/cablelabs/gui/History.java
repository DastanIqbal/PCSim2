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

public class History {
	
	static private History hist = null;
	
	protected File lastPCFDirectory = new File(".");
	protected File lastDCFDirectory = new File(".");
	protected File lastScriptsDirectory = new File(".");
	protected File lastBatchDirectory = new File(".");
	protected File lastBatchFile = null;
	protected static final String PLATFORM_VERSION = PC2UI.version + "   build: " + PC2UI.build;
	
	public static final int MAX_NUM_HISTORY_FILES = 4;
	protected String [] histLabels = new String[MAX_NUM_HISTORY_FILES];
	protected File [] histFiles = new File [MAX_NUM_HISTORY_FILES];
	
	protected int histCount = 0;
	protected static final File HISTORY_FILE = new File("../config/.history");
	
	private History() {
		
	}

	/**
	 * Provides a reference to the class. Since the class
	 * is a singleton, if it doesn't already exist, it will
	 * create an instance and then return a reference to it.
	 * 
	 * @return the single instantiation of the class.
	 */
	public synchronized static History getInstance() {
		if (hist == null) {
			hist = new History();
		}	
		return hist;
	}
	
	public void addHistoryFile(File f) {
		if (f.exists() && 
				f.canRead() && 
				f.isFile() &&
				histCount < MAX_NUM_HISTORY_FILES) {
			histFiles[histCount] = f;
			histLabels[histCount] = (histCount+1) + ".  " + histFiles[histCount].getName();
			
		}
		else {
			histFiles[histCount] = f;
			histLabels[histCount] =  (histCount+1) + "*.  " + histFiles[histCount].getName();
		}
		histCount++;
		
	}
}
