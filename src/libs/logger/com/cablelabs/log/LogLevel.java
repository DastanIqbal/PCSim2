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

import org.apache.log4j.Level;


public class LogLevel {

	protected LogCategory cat = null;
	protected String subCat = null;
	protected Level level = null;

	public LogLevel(LogCategory c, String s, Level l) {
		this.cat = c;
		this.subCat = s;
		this.level = l;
	}
}
