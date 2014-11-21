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

import java.util.Enumeration;
import java.util.Properties;

/**
 * This is a container class for the model element of a PC 2.0 
 * Simulator XML document
 * @author ghassler
 *
 */
public class Model implements Cloneable{

	/**
	 * The model name
	 */
	private String name;
	
	/**
	 * Any properties (attributes) defined for the model.
	 */
	private Properties props;
	
		
	/**
	 * Constructor
	 *
	 */
	public Model() {
		
	}
	
	/**
	 * Sets the name of the model's
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the model's name
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	public String getProperty(String key) {
		if (props == null)
			return null;
		else {
			return props.getProperty(key);
		}
	}
	/**
	 * Gets the properties for the model.
	 * @return
	 */
	public Properties getProps() {
		return props;
	}
	
	/**
	 * Gets a handle to the all of the properties
	 * @return
	 */
	public Enumeration<Object> getKeys() {
		return this.props.keys();
	}
	
	/**
	 * Adds the property (attribute) of the model to the class and
	 * checks for any duplicates.
	 * 
	 * @param key - the key to retrieving the property
	 * @param value - is current value.
	 * @return - true if the key is stored, false if it is a duplicate.
	 */
	public boolean setProperty(String key, String value) {
	
		if (props == null)
		{
			props = new Properties();
		}
		if (props.containsKey(key)) {
			System.out.println("Duplicate key: " + key + " identified for model name " + name);
			return false;
		}
		else {
			props.setProperty(key, value);
			//propKeys.addLast(key);
		}
		return true;
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = name;

		if (props != null) {
			Enumeration<Object> keys = props.keys();
			
			while (keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				if (key != null)
					result += "\n    attr-name=" + key + " value=" + props.getProperty(key);
			}
		}
		return result;
	}
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		Model retval = (Model)super.clone();
		if (retval != null) {
			if (this.name != null)
				retval.name = new String(this.name);
			if (this.props != null)
				retval.props = (Properties)this.props.clone();
		}
		return retval;
	}
}
