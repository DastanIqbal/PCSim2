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

/**
 * This is a container class for a transition element within a PC 2.0
 * Simulator XML document.
 * 
 * @author ghassler
 *
 */
public class Transition implements Cloneable {
	
	/**
	 * The name of the originating (current) state.
	 */
	protected String from;
	
	/**
	 * The name of the new state to move to
	 */
	protected String to;
	
	/**
	 * The event that triggers moving from the originating state
	 * to the new state.
	 */
	protected String event;
	
	/**
	 * Constructor.
	 * @param from - originating state name
	 * @param to - terminating state name
	 * @param event - triggering event
	 */
	public Transition(String from, String to, String event ) {
		this.from = from;
		this.to = to;
		this.event = event;
	}
	
	public Transition(Transition orig) {
		this.from = new String(orig.getFrom());
		this.to = new String(orig.getTo());
		this.event = new String(orig.getEvent());
	}
	
	/**
	 * Gets the originating state name 
	 * @return
	 */
	 public String getFrom() {
		return from;
	}
	
	/**
	 * Gets the new state's name 
	 * @return
	 */
	 public String getTo() {
		return to;
	}
	
	/**
	 * Gets the triggering event name
	 */
	 public String getEvent() {
		return event;
	}
	
	/**
	 * Sets the new state's name
	 * @param to
	 */
	 public void setTo(String to) {
		this.to = to;
	}
	 
		/** This implements a deep copy of the class for replicating 
		 * FSM information.
		 * 
		 * @throws CloneNotSupportedException if clone method is not supported
		 * @return Object
		 */ 
		@Override
		public Object clone() throws CloneNotSupportedException {
			Transition retval = (Transition)super.clone();
			if (retval != null ) {
				if (this.from != null) 
					retval.from = new String(this.from);
				if (this.to != null) 
					retval.to = new String(this.to);
				if (this.event != null) 
					retval.event = new String(this.event);
			}	

			return retval;
		}
		
		@Override
		public String toString() {
			String result = " from=\"" + from + "\" to=\"" + to + "\" event=\"" + event + "\"";
			return result;
		}
}
