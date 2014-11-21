/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.models;

import java.util.Hashtable;

public class RebootTable {

	/**
	 * The single RebootTable class for the platform.
	 */
	private static RebootTable instance = null;

	/**
	 * This table contains the IP address of the any device that has been 
	 * requested to reboot. The IP address will remain in the table as long 
	 * as the device remains registered to the system. The value for the key
	 * is the label used when issuing the reboot.
	 */
	private Hashtable<String, String> table = null; 
	
	private RebootTable() {
		table = new Hashtable<String, String>();
	}
	
	/**
	 * Obtains a reference to the singleton of the class.
	 * 
	 * @return 
	 */
	public synchronized static RebootTable getInstance() {
		if (instance == null) {
			instance = new RebootTable();
		}	
		return instance;
	}
	
	public boolean contains(String key) {
		return table.containsKey(key);
	}
	
	public void add(String key, String value) {
		table.put(key, value);
	}
	
	public boolean delete(String key) {
		String ip = table.remove(key);
		if (ip != null)
			return true;
		return false;
	}
}
