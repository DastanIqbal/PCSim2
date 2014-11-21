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

public class LogStats {

	protected LogCategory category = null;
	protected String subCategory = null;
	protected int warnings = 0;
	protected int errors = 0;
	protected int fatals = 0;

	public LogStats(LogCategory cat, String subCategory) {
		this.category = cat;
		this.subCategory = subCategory;
	}

	@Override
	public String toString() {
		String result = "\t" + category.toString()
			+ " " + subCategory + " - "
			+ " fatal=" + fatals + "     error=" + errors + "     warn=" + warnings;
		return result;
	}

	protected void error() {
		this.errors++;
	}

	protected void fatal() {
		this.fatals++;
	}

	protected void warn() {
		this.warnings++;
	}
}
