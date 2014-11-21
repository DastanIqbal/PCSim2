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
 * This interfaces defines communications that originate in a protocol stack to be
 * delivered to a specific FSM. Once the stack obtains the FSM that is subscribed to
 * receive events from another network element, the stack invokes the processEvent
 * operation for actual delivery of the event to the FSM.
 * 
 * @author ghassler
 *
 */
public interface FSMListener {

	/**
	 * Defintion for delivering an event to an FSM.
	 * @param event - the new event
	 * @return
	 * @throws IllegalArgumentException
	 */
	public boolean processEvent(MsgEvent event) throws IllegalArgumentException;

	/**
	 * Allows the retrival of an event based upon the call id for 
	 * message response created by the stacks.
	 * @param callid
	 * @return
	 */
	public SIPMsg findByCallIdAndMethod(String callid, String method, int CSeqNo);

	/**
	 * Allows the stacks to obtain the unique identifier of the FSM that
	 * an event is being delivered for processing.
	 * @return
	 */
	public int getFsmUID();

	/**
	 * Allows one FSM to see if another FSM is in the Registered state.
	 * @return
	 */
	public boolean isRegistered();

	/**
	 * Allows the stack get the name of the FSM.
	 * @return
	 */
	public String getFSMName();
	
	/**
	 * Allows the stacks to know the index of the current event being 
	 * processed by the FSM.
	 * @return
	 */
	public int getCurrentMsgIndex();
}
