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

public enum CaptureAttributeType {

	// Use the system's default algorithm to determine the field
	// to return
	DEFAULT, 
	// Specific fields that can be obtained. NOTE: Not every field is
	// always available. It is dependent on the message type and the
	// output of the tool.
	CAP_LEN,
	HIDE,
	NAME, 
	NUM,
	POS ,
	SHOW,
	SHOWNAME, 
	SIZE, 
	TIMESTAMP, 
	UNMASKED_VALUE, 
	VALUE;
	
}
