/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.tools;

public class MultipartLocation {

	protected int startLocation = -1;
	protected int endLocation = -1;
	protected String boundaryTag = null;
	
	public MultipartLocation() {
		
	}
	
	public MultipartLocation(int begin, int end) {
		this.startLocation = begin;
		this.endLocation = end;
	}

	public MultipartLocation(int begin, int end, String boundary) {
		this.startLocation = begin;
		this.endLocation = end;
		this.boundaryTag = boundary;
	}
	
	public boolean isValid() {
		if (startLocation != -1 && endLocation != -1 && boundaryTag != null)
			return true;
		
		return false;
	}
}
