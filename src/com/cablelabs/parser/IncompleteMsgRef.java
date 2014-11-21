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

import com.cablelabs.fsm.MsgRef;

/**
 * This is a  MsgRef that could resolve the fsmUID while parsing 
 * This may be the result of the reference occurring within one 
 * fsm prior to the referred to FSM being defined.
 * 
 * The parser will maintain a table each occurrance and attempt to
 * resolve each at the end. As long as a references can be resolved
 * the test will continue. Otherwise the parser will throw an
 * exception with the file and line number of the error.
 * @author ghassler
 *
 */
public class IncompleteMsgRef {

	protected int lineNumber = 0;
	protected String fileName = null;
	protected MsgRef ref = null;
	protected String fsmRef = null;
	
	protected IncompleteMsgRef(String fsm, MsgRef mr, String file, int line) {
		this.fsmRef = fsm;
		this.ref = mr;
		this.lineNumber = line;
		this.fileName = file;
	}
	
	protected int getLine() {
		return lineNumber;
	}
	
	protected MsgRef getMsgRef() {
		return ref;
	}
	
	protected String getFile() {
		return fileName;
	}
	
	protected String getFSM() {
		return fsmRef;
	}
	
}
