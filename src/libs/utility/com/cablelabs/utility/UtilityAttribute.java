/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.utility;

import java.io.Serializable;

public class UtilityAttribute implements Serializable, Cloneable {

	private String name = null;
	private String value = null;
	private static final long serialVersionUID = 1;
	
	public UtilityAttribute(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int length() {
		return this.value.length();
	}
	
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String encode() {
		String buf = this.name + " " + this.value.length() + " " + this.value;
		return buf;
	}

	public boolean equals(Object other) {
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		UtilityAttribute that = (UtilityAttribute) other;
		return (this.name.equals(that.getName()) &&
				this.value.equals(that.getValue()));
	}

	
	public Object clone() {
		String n = new String(this.name);
		String v = new String(this.value);
		return new UtilityAttribute(n,v);
		
	}
	
	public String toString() {
		String result = name + " " + value.length() + " " + value;
		return result;
	}
}
