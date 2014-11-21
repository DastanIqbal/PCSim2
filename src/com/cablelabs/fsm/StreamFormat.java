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

public class StreamFormat {

	private String format = null;
	
	/**
	 * The maximum number of bytes to send in each packet 
	 */
	private int size = -1;
	/**
	 * The number of milliseconds between each packet.
	 */
	private long interval = -1;
	
	public StreamFormat(String format, int size, long interval) {
		this.format = format;
		this.size = size;
		this.interval = interval;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
	@Override
	public String toString() {
		String result = " format=" + format + " size=" + size 
			+ " interval=" + interval + " ";
		return result;
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		StreamFormat retval = (StreamFormat)super.clone();
		if (retval != null) {
			if (this.format != null) 
				retval.format = new String(this.format);
			retval.size = this.size;
			retval.interval = this.interval;
		}	
		
		return retval;
	}
}
