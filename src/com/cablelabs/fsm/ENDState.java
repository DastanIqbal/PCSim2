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

import com.cablelabs.sim.PCSim2;

/**
 * This is the END state defined within the PC 2.0 Simulator XML
 * document. It's role is to attempt to restore a DUT
 * being tested back to a sane state for subsequent test cases
 * to be performed.
 * 
 * @author ghassler
 *
 */
public class ENDState extends EndSessionState implements Cloneable {

	public static final String NAME = "END";
	/**
	 * Constructor. 
	 * 
	 * @param name - the name of the state which should be END for 
	 * 		any XML documents to parse properly.
	 * @param fsm - the FSM that this state is associated.
	 */
	public ENDState(FSM fsm ) {
		super(NAME, fsm);
	}
	
	/**
	 * Initializes the transition table and operations for the END
	 * state
	 */
	@Override
	public void init(FSMAPI api, ComparisonEvaluator ce, int noResponseTimeout) {
		// Lastly call the base class's init
		super.init(api, ce, noResponseTimeout);
	}
	/**
	 * Determines what the next message that the platform needs to
	 * send to get the DUT back into a sane state for the next test.
	 * 
	 * The rules are fairly straight forward. Sessions that are
	 * initiated with a proxy message instead of being sent by
	 * the FSM, must clean up their own dialogs. This leaves only
	 * dialogs the FSM initiated or terminated.
	 * 
	 * This method uses the information contained in the 
	 * ReferencePointsFactory to help determine what message(s) need 
	 * to be sent to reset the devices. 
	 * 
	 * NOTE: if the method can't determine what is the appropriate message
	 * the state simply cleans up and terminates the test.
	 */
	@Override
	public void processPrelude(int msgQueueIndex) {
		super.processPrelude(msgQueueIndex);
	}

	/** 
	 * Processes each event for the state.
	 * 
	 * @param event - the current event to process
	 * @return true if the event is processed, false otherwise
	 * @throws IllegalArgumentException
	 */
	@Override
	public boolean processEvent(MsgEvent event) throws IllegalArgumentException {
		this.disableNoResponseTimer = true;
		return super.processEvent(event);
	}
	
	@Override
	public void complete(int msgQueueIndex) {
		PCSim2.setTestComplete();
		if (noResponseTimer != null)
			noResponseTimer.cancel();
		super.api.fsmComplete();
		super.api.shutdown();
		
	}

	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		ENDState retval = (ENDState)super.clone();
		return retval;
	}
}
