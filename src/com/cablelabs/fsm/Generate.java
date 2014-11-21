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
 * The container class for the Generate action defined
 * within the PC 2.0 Simulator XML document.
 * 
 * @author ghassler
 *
 */
public class Generate implements Action {

	/**
	 * The event to deliver to the target FSM
	 */
	private String event = null;
	
	/**
	 * The name of the FSM to deliver the event attribute
	 * for processing.
	 */
	private String target = null;
	
	/**
	 * The name of the FSM that is sending the event attribute
	 * for processing. 
	 */
	private String sender = null;
	
	
	/**
	 * Constructor.
	 * @param event - the event to deliver to the named FSM.
	 * @param target - the name of the FSM to receive the event.
	 */
	public Generate(String event, String target, String sender) {
		this.event  = event;
		this.target = target;
		this.sender = sender;
	}
	
	/**
	 * The interface for processing the Action within the originating
	 * (sending) FSM.
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.processEvent(this);
	}

	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = "\tgenerate event=\"" + event + "\"" + " sender=\"" + sender + "\"";
		if (target != null)
			result += " target=\"" + target + "\"";
		result += "\n";
		return result;
	}

	/**
	 * Gets the event.
	 * @return
	 */
	public String getEvent() {
		return event;
	}
	
	/**
	 * Gets the name of the target FSM
	 * @return
	 */
	public String getTarget() {
		return target;
	}
	
	/*
	 * Gets the name of the sending FSM
	 */
	public String getSender() {
		return sender;
	}
	
	/**
	 * Sets the target
	 * @param target
	 */
	public void setTarget(String target) {
		this.target = target;
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		Generate retval = (Generate)super.clone();
		if (retval != null) {
			if (this.event != null)
				retval.event = new String(this.event);
			if (this.sender != null)
			    retval.sender = new String (this.sender);
			if (this.target != null)
				retval.target = new String(this.target);
		}
		return retval;
	}
	
//	 This method was added as a validator for cloning
//	public String me() {
//		return "G " + super.toString();
//	}
}
