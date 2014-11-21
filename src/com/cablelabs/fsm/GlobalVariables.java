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

import java.util.Hashtable;


/**
 * The GlobalVariables class is a container for any variables
 * that the test script declares while processing. When the
 * script 
 * 
 * @author Garey Hassler
 *
 */
public class GlobalVariables {

	private Hashtable<String, Variable> variables = new Hashtable<String, Variable>();

	static private GlobalVariables gv = null;
	
	private GlobalVariables() {
		
	}
	/**
	 * Provides a reference to the class. Since the class
	 * is a singleton, if it doesn't already exist, it will
	 * create an instance and then return a reference to it.
	 * 
	 * @return the single instantiation of the class.
	 */
	public synchronized static GlobalVariables getInstance() {
		if (gv == null) {
			gv = new GlobalVariables();
		}	
		return gv;
	}
	
	/**
	 * Adds a new variable to the table.
	 * 
	 * @param name - the name of the variable
	 * @param var - the value of the variable
	 */
	public void put(String name, Variable var) {
		variables.put(name, var);
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public Variable get(String name) {
		return variables.get(name);
	}
	
	public boolean remove(String name) {
		Object vr = variables.remove(name);
		if (vr != null)
			return true;
		return false;
	}
	
	public void clear() {
		variables.clear();
	}
}
