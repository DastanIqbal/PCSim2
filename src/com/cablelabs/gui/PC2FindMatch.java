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

public class PC2FindMatch {

	/**
	 * This is the offset within the document where the pattern match
	 * occurred.
	 */
	protected int position = -1;
	/**
	 * This is the tag returned by a call to addHighlighter
	 */
	protected Object tag = null;
	
	static final long serialVersionUID = 1;
	
	public PC2FindMatch(int pos, Object tag) {
		this.position = pos;
		this.tag = tag;
	}
	
	public Object getTag() {
		return this.tag;
	}
	
	public void setHighlighter(Object tag) {
		this.tag = tag;
	}
	
	public int getPosition() {
		return this.position;
	}
}
