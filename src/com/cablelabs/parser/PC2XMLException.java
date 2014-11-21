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

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/**
 * An extension of the SAXParseException to allow
 * distinction of an error between the SAXParser and
 * an internal error of the TSParser class.
 * 
 * @author ghassler
 *
 */
public class PC2XMLException extends SAXParseException {

	/**
	 * The version of the class. 
	 */
	static final long serialVersionUID = 1;
	
	/**
	 * The name of the file being parsed that caused the exception
	 */
	protected String fileName = null;
	
   /**
    * Constructor.
    * @param file - the name of the file being parsed when exception 
    * 		occurred.
    * @param s - String message describing the exception.
    * @param l - the location within the document being parsed.
    */ 
	public PC2XMLException (String file, String s, Locator l) {
    	super(s, l);
    	this.fileName = file;
    }
	
	/**
	 * Gets the name of the file that was being parsed when the exception
	 * occurred.
	 * @return
	 */
	public String getFileName() {
		return this.fileName;
	}
}
