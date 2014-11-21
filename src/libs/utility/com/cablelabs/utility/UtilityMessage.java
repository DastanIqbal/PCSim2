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
import java.util.LinkedList;
import java.util.ListIterator;

public class UtilityMessage implements Cloneable, Serializable {

	private String type;
	private String version = "2.0";
	private String transactionID = null;
	private LinkedList<UtilityAttribute> attributes = new LinkedList<UtilityAttribute>();
	private static final long serialVersionUID = 1;
	
	public UtilityMessage(String msgType, String transID) {
		this.type = msgType;
		this.transactionID = transID;
	}
	
	public UtilityMessage(String msgType, String transID, String version) {
		this.type = msgType;
		this.transactionID = transID;
		this.version = version;
	}
	
	public String getType() {
		return this.type;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return this.version;
	}
	
	public String getTransactionID() {
		return this.transactionID;
	}
	
	public void setTransactionID(String tID) {
		this.transactionID = tID;
	}
	
	public void addAttribute(UtilityAttribute attr) {
		this.attributes.add(attr);
	}
	
	public ListIterator<UtilityAttribute> getAttributes() {
		return this.attributes.listIterator();
	}
	
	public UtilityAttribute getAttribute(String name) {
		UtilityAttribute result = null;
		ListIterator<UtilityAttribute> iter = this.attributes.listIterator();
		while (iter.hasNext() && result == null) {
			UtilityAttribute ua = iter.next();
			if (ua.getName().equals(name))
				result = ua;
		}
		
		return result;
	}
	
	public int getMsgLength() {
		return encode().length();
	}
	
	public boolean  removeAttribute(UtilityAttribute attr) {
		return attributes.remove(attr);
	}
	
	public String encode() {
		// First build the attributes string to get
	    // the length of all the attributes together
		String attrs = "";
		
		if (attributes.size() > 0) {
			ListIterator<UtilityAttribute> iter = this.attributes.listIterator();
			// The count variable is used to add a space between
			// each TLV attribute.
			int count = 0;
			while (iter.hasNext()) {
				if (count > 0)
					attrs += " ";
				attrs += ((UtilityAttribute)iter.next()).encode();
				count++;
				
			}
		}
		String buf = "PC " + version + " " + type + " " + transactionID + 
			" " + attrs.length() + ((attrs.length() > 0) ? (" " + attrs) : "");
		return buf;
	}
	
	public boolean equals(Object other) {
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		UtilityMessage that = (UtilityMessage) other;
		
		// First check the type, version and transactionID. 
		// This is were most objects will fail to match
		if (!(this.type.equals(that.getType()) &&
				this.version.equals(that.getVersion()) &&
				this.transactionID.equals(that.getTransactionID()))) {
			return false;
		}
		
		// If we have made it passed the other fields, try the list of
		// attributes. NOTE. The order of attributes is not important
		// so this will have to be a brute force operation.
		ListIterator<UtilityAttribute> iter = that.getAttributes();
		while (iter.hasNext()) {
			UtilityAttribute thatUA = (UtilityAttribute)iter.next();
			boolean match = false;
			ListIterator<UtilityAttribute> iter2 = this.attributes.listIterator();
			while (iter2.hasNext() && !match) {
				if (((UtilityAttribute)iter2.next()).equals(thatUA)) {
					match = true;
				}
			}
			if (!match)
				return false;
		}
		
		return true;
		
	}
	
	public Object clone() {
		String t = new String(this.type);
		String v = new String(this.version);
		String transID = new String(this.transactionID);
		UtilityMessage newUA = new UtilityMessage(t,transID);
		newUA.setVersion(v);
		ListIterator<UtilityAttribute> iter = this.attributes.listIterator();
		while(iter.hasNext()) {
			newUA.addAttribute((UtilityAttribute)iter.next());
		}
		return newUA;
	} 
	
	public String toString() {
		return encode();
	}
}
