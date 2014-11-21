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
 * This class defines all of the extension constants supported by the platform
 * for parsing a PC 2.0 Simulator XML document.
 * 
 * @author ghassler
 *
 */
public class ExtensionConstants {

	// Currently supported extensions
	public static final String gruu = "gruu";
	public static final String precondition = "precondition";
	public static final String rel = "100rel";
	
	/**
	 * Determines if the ext argument is a known extension or not.
	 * 
	 * @param ext - a case sensitive string representation of the extension. 
	 * @return - true if the extension is known by the system, false otherwise.
	 */
	static public boolean isValidExt(String ext) {
		if (gruu.equals(ext) ||
				precondition.equals(ext) ||
				rel.equals(ext)) {
			return true;
		}
		return false;
	}
}
