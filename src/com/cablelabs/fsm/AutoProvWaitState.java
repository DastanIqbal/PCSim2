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

public class AutoProvWaitState extends State implements Cloneable {

	protected static final int DEFAULT_TIMEOUT = 90000;
	
	public static final String NAME = "AutoProvWaitState";
	/**
	 * Constructor. 
	 * 
	 * @param name - the name of the state which should be END for 
	 * 		any XML documents to parse properly.
	 * @param fsm - the FSM that this state is associated.
	 */
	public AutoProvWaitState(FSM fsm ) {
		super(NAME, fsm);
		this.owner = fsm;
		this.timeout = DEFAULT_TIMEOUT;
		String origInit = fsm.getInitialState();
		Transition t = new Transition(NAME, origInit, TimeoutConstants.TIMER_EXPIRED);
		addTransition(t);
		t = fsm.getState(origInit).findTransition(EventConstants.REGISTERED);
		if (t != null) {
			Transition reg = new Transition(NAME, t.getTo(), t.getEvent());
			addTransition(reg);
		}
		
		try {
			fsm.addState(this);
			fsm.setInitialState(NAME);
		}
		catch (PC2Exception pce) {
			
		}
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		AutoProvWaitState retval = (AutoProvWaitState)super.clone();
		return retval;
	}
}
