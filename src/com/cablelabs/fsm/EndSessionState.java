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

import java.util.LinkedList;
import java.util.ListIterator;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.utility.UtilityAttribute;
import com.cablelabs.utility.UtilityMessage;

public class EndSessionState extends State implements Cloneable {

	private LogAPI logger = LogAPI.getInstance();
	
	/**
	 * This flag states whether a BYE is needed when the state receives an
	 * ACK.
	 */
	private boolean sendBye = false;
	
	/**
	 * This flag states whether a CANCEL was sent and we need to await the 
	 * 200-CANCEL, xxx-INVITE and then send ACK
	 * 
	 */
	private boolean cancelSent = false;
	
	/**
	 *  This flag indicates whether we are waiting for the response to the CANCEL or
	 *  not.
	 */
	private boolean cancelResp = false;
	
	/** 
	 * A local flag indicating that it has issued an onhook
	 * message.
	 */
	private boolean onHookSent = false;
	/**
	 * This is a flag to control when to issue the SessionTerminated event.
	 */
	private boolean sessionComplete = true;
	
	/**
	 * The FSM that we are apart of.
	 */
	//private FSM owner = null;

	/** 
	 * This flag allows the script to instruct the terminate and
	 * clean up to allow the device to remain off hook.
	 */
	private boolean ignoreOffHook = false;
	
	/** This flag allows the script to instruct the clean up to not
	 * send any messages.
	 */
	private boolean endImmediately = false;
	/**
	 * Constructor. 
	 * 
	 * @param name - the name of the state which should be END for 
	 * 		any XML documents to parse properly.
	 * @param fsm - the FSM that this state is associated.
	 */
	public EndSessionState(String name, FSM fsm ) {
		super(name, fsm);
		//this.owner = fsm;
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
	
	public boolean isComplete() {
		return sessionComplete;
	}
	
	protected boolean checkForOffHook() {
		// We want to issue this only once, so if we have sent it,
		// answer that we haven't sent another.
		if (onHookSent || ignoreOffHook)
			return false;
		else {
			onHookSent = false;
			for (int i=0; i<4; i++) {
					logger.debug(PC2LogCategory.FSM, subCat, "Checking for any lines still off hook.");
					UtilityMsg um = ReferencePointsFactory.getOffHookMsg(i);
					if (um != null) {
						UtilityMessage offHook = um.getUtilityEvent().getMessage();
						// If the offHook attribute is not null, then no onHook was issued for
						// this device.
						if (offHook != null) {
							sessionComplete = false;
							Send s = new Send(MsgRef.UTILITY_MSG_TYPE, UtilityConstants.ONHOOK, true);
							// Let the system determine the target
							s.setTarget(null);
							ListIterator<UtilityAttribute> iter = offHook.getAttributes();
							while (iter.hasNext()) {
								UtilityAttribute ua = iter.next();
								if (ua.getName().equals(SettingConstants.VOICE_PORT)) {
									Mod m = new Mod("replace");
									m.setHeader(ua.getName());
									PlatformRef pr = new PlatformRef(SettingConstants.PLATFORM.toLowerCase());
									pr.setNELabel(SettingConstants.PLATFORM);
									pr.setParameter(SettingConstants.VOICE_PORT+ua.getValue());
									m.setRef(pr);
									s.addModifier(m);
								}
							}
							try {
								onHookSent = true;
								s.execute(super.api, -1);
							}
							catch (PC2Exception ex) {
								logger.error(PC2LogCategory.FSM, subCat,
										"Unable to send " 
										+ s.getMsgType() + " to " 
										+ s.getTarget() + " terminating test.");
							}
						}
					
				}
			}
			return onHookSent;
		}
	}
	
	protected boolean checkPACTServices() {
		ReferencePointsFactory rpf = owner.getReferencePointsFactory();
		boolean result = false;
		if (rpf.stopService != null) {
			Send s = new Send(MsgRef.UTILITY_MSG_TYPE, UtilityConstants.PROV_RESUME_SERVICE);
			s.setTarget(null);
			Mod m = new Mod("add");
			m.setHeader(UtilityConstants.SRV_PROTOCOL_ATTR);
			m.setRef(new Literal(rpf.stopService));
			s.addModifier(m);
			try {
				s.execute(super.api, -1);
				result = true;
			}
			catch (PC2Exception ex) {
				logger.error(PC2LogCategory.FSM, subCat,
						"Unable to send " 
						+ s.getMsgType() + " to " 
						+ s.getTarget() + ".");
			}
		}
		else if (rpf.stopPort != null) {
			Send s = new Send(MsgRef.UTILITY_MSG_TYPE, UtilityConstants.PROV_MANAGE_PORT);
			s.setTarget(null);
			Mod m = new Mod("add");
			m.setHeader(UtilityConstants.SRV_PROTOCOL_ATTR);
			m.setRef(new Literal(rpf.stopPort));
			s.addModifier(m);
			m = new Mod("add");
			m.setHeader(UtilityConstants.MANAGE_PORT_OP);
			m.setRef(new Literal("start"));
			s.addModifier(m);
			try {
				s.execute(super.api, -1);
				result = true;
			}
			catch (PC2Exception ex) {
				logger.error(PC2LogCategory.FSM, subCat,
						"Unable to send " 
						+ s.getMsgType() + " to " 
						+ s.getTarget() + ".");
			}
		}
			
		return result;
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

		// We only want to perform the prelude actions the first
		// time we enter the state
		if (counter == 0)
			super.processPrelude(msgQueueIndex);
		else
			counter++;
		
		String stateName = super.getName();
		
		ReferencePointsFactory rpf = owner.getReferencePointsFactory();
		LinkedList<Send> ll = new LinkedList<Send>();
		// See if we initiated the dialog by sending the INVITE
		// We only want to perform this operation the first time we enter this 
		// method. If we cycle to this state again, the processEvent method should
		// handle any additional messages that may need to be sent.
//		if (name.equals("EndTheCall2")) {
//		int glh = 0;
//	}
		if (endImmediately) {
			ignoreOffHook = true;
			sessionComplete = true;
		}
		else if ((!rpf.sessComplete || !rpf.dialogComplete) && counter == 1) {
			if (rpf.inviteSent) {
				// This means we are the initiator of the dialog 
				// See if we received a bye
				if (rpf.relRcvd) {
					// Send the 200-Bye and we are done
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "200-BYE", true);
					ll.add(s);
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " sending 200-BYE because INVITE was sent and BYE received.");
							
				}
				// See if we sent a bye
				else if (rpf.relSent) {
					// Simply wait for response or No Response Timer to
					// expire
					sessionComplete = false;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " waiting 200-BYE because INVITE was sent and BYE sent.");
				}
				// See if we sent Ack. This means the dialog was connected
				// Simply send BYE and wait for 200-BYE
				else if (rpf.inviteAckSent && !rpf.finalResp) {
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "BYE", true);
					ll.add(s);
					sessionComplete = false;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " sending BYE because ACK was sent and no BYE was received.");
				}
				else if (rpf.inviteOKRcvd && !rpf.inviteAckSent) {
					// Since we received the OK but didn't send the ACK,
					// Send the ACK followed by a BYE
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "ACK", true);
					Send s2 = new Send(MsgRef.SIP_MSG_TYPE, "BYE", true);
					ll.add(s);
					ll.add(s2);
					sessionComplete = false;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " sending ACK and then BYE because 200-INVITE was received and ACK has not been sent.");
				}
				else if (rpf.finalResp && !rpf.inviteAckSent) {
					// Since we received a final response but didn't send the ACK,
					// Send the ACK 
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "ACK", true);
					ll.add(s);
					if (!checkForOffHook())
						sessionComplete = true;
					else 
						sessionComplete = false;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " sending ACK because received final response, but ACK has not been sent.");
				}
				// Otherwise the INVITE hasn't had a response so send a 
				// Cancel
				else if (!rpf.finalResp && rpf.dialogEstTimer != null){
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "CANCEL", true);
					ll.add(s);
					sessionComplete = false;
					cancelSent = true;
					cancelResp = true;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " sending CANCEL because INVITE was sent and there has been no final response.");
				}
			}
			// See if we received the dialog by receiving the INVITE
			else if (rpf.inviteRcvd) {
				// This means our peer initiated the dialog 
				// See if we received a bye
				if (rpf.relRcvd) {
					// Send the 200-Bye and we are done
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "200-BYE", true);
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " Sending 200-BYE because INVITE was received and BYE was received.");
					ll.add(s);
				}
				// See if we sent a bye
				else if (rpf.relSent) {
					// Simply wait for response or No Response Timer to
					// expire
					sessionComplete = false;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " waiting 200-BYE because INVITE was received and BYE was sent.");
				}
				// See if we sent Ack. This means the dialog was completed
				// Simply send BYE and wait for 200-BYE
				else if (rpf.inviteAckRcvd && 
						rpf.inviteStatusCode >=200 &&
						rpf.inviteStatusCode <= 299) {
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "BYE", true);
					ll.add(s);
					sessionComplete = false;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " sending BYE because ACK was received and final response was " 
							+ rpf.inviteStatusCode + ".");
				}
				else if (rpf.inviteStatusCode >= 300) {
					// The dialog failed and there is nothing to 
					// clean up
					sessionComplete = true;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " not sending any message because final response was " 
							+ rpf.inviteStatusCode + ".");
				}
				else if (rpf.inviteOKSent) {
					if (rpf.dialogEstTimer != null) {
						// Since we sent the OK but haven't received the ACK,
						// we need to wait for the ACK and send a BYE
						sendBye = true;
						sessionComplete = false;
						logger.info(PC2LogCategory.FSM, subCat, 
								stateName + " waiting for the ACK message because we sent a 200-INVITE message.");
					}
					else {
						sessionComplete = true;
						logger.info(PC2LogCategory.FSM, subCat, 
								stateName + " the dialog timer has expired, nothing to clean up.");
					}
				}
				// Otherwise the INVITE hasn't had a response so send an 
				// ERROR (500-INVITE) to our peer and wait fo the ACK
				else {
					if (rpf.dialogEstTimer != null) {
						if (rpf.cancelRcvd && !rpf.cancelRespSent) {
							Send cancelResponse = new Send(MsgRef.SIP_MSG_TYPE, "200-CANCEL", true);
							ll.add(cancelResponse);
							logger.info(PC2LogCategory.FSM, subCat, 
									stateName + " sending a 200-CANCEL message because we received a CANCEL message.");
						}
						else if ((rpf.initPrackRcvd && !rpf.initPrackOKSent) || 
								(rpf.finalPrackRcvd && !rpf.finalPrackOKSent)) {
							Send prackResp = new Send(MsgRef.SIP_MSG_TYPE, "200-PRACK", true);
							ll.add(prackResp);
							logger.info(PC2LogCategory.FSM, subCat, 
									stateName + " sending a 200-PRACK message because we received a PRACK message.");
						}

						Send s = new Send(MsgRef.SIP_MSG_TYPE, "500-INVITE", true);
						ll.add(s);
						sessionComplete = false;
						logger.info(PC2LogCategory.FSM, subCat, 
								stateName + " sending a 500-INVITE message because we received an INVITE and the final response is null.");
					}
				}
			}
			// There was no invite sent or received, we are done.
			else {
				sessionComplete = true;
			}
		}
		
		
		if (ll.size() > 0) {
			ListIterator<Send> iter = ll.listIterator();
			while (iter.hasNext()) {
				Send s = iter.next();
				try {
					s.execute(super.api, -1);
				}
				catch (PC2Exception ex) {
					logger.error(PC2LogCategory.FSM, subCat,
							"Unable to send " 
							+ s.getMsgType() + " to " 
							+ s.getTarget() + " terminating test.");
					if (!checkForOffHook())
						sessionComplete = true;
					else
						sessionComplete = false;
				}
			}
				
			
		}
		// Check whether we are done with the test and clean up
		
		if (sessionComplete) {
//			if (!disableNoResponseTimer && noResponseTimer != null) {
//				logger.debug(PC2LogCategory.FSM, subCat,
//						"Stopping state's(" + name + ")no response timer(" + noResponseTimer + ").");
//				noResponseTimer = null;
//			}
			checkPACTServices();
			if (!checkForOffHook()) {
				this.disableNoResponseTimer=true;
				
				complete(msgQueueIndex);
				sessionComplete = false;
			}
		}
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
		// First log when the timer is stopping if it is not the noResponseTimeout
		if (!disableNoResponseTimer && noResponseTimer != null) 
			noResponseTimer.cancel();

		if (!disableNoResponseTimer && 
				!(event.getEventName().equals(TimeoutConstants.NO_RESPONSE_TIMEOUT))) {
			logger.debug(PC2LogCategory.FSM, subCat,
					"Stopping state's(" + name + ")no response timer(" + noResponseTimer + ").");
			noResponseTimer = null;
		}
		
		
		String eventName = event.getEventName();
		if (eventName != null) {
			// LOG USING LOGMSG category so that the user can't disable which
			// would break the trace tool.
			logger.info(PC2LogCategory.LOG_MSG, subCat,
					"FSM (" + owner.getName() + ") - State (" + name + ") processing event (" 
					+ eventName + ") sequencer=" + event.getSequencer() + ".");
//			BRKPT
//			if (eventName.equalsIgnoreCase("487-INVITE")) {
//			int glh = 0;
//			}
			int statusCode = -1;
			if (SIPConstants.isResponseType(eventName)) {
				try {
					statusCode = Integer.parseInt(eventName.substring(0,3)); 
				}
				catch (NumberFormatException nfe) {
					logger.error(PC2LogCategory.SIP, subCat,
							"FSM (" +owner.getName() + ") - State (" + name 
							+ ") encountered an error while trying to convert " 
							+ eventName.substring(0,3) + " to a number.");

				}
			}

			LinkedList<Send> ll = new LinkedList<Send>();
			String stateName = super.getName();
			// ReferencePointsFactory rpf = owner.getReferencePointsFactory();
			// If the event is an ACK and the sendBye flag is true, then
			// send the BYE and wait for the 200-BYE
			if (eventName.equalsIgnoreCase(SIPConstants.ACK)) {
				if (sendBye) {
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "BYE", true);
					ll.add(s);
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " sending a BYE message because the sendBye flag is true.");
					sendBye = false;
					sessionComplete = false;
				}
				else
					sessionComplete = true;
			}
			else if (eventName.equalsIgnoreCase(SIPConstants.BYE)) {
				sessionComplete = true;
			}
			else if (eventName.equalsIgnoreCase("200-BYE")) {
				sessionComplete = true;
			}
			else if (eventName.equalsIgnoreCase(SIPConstants.INVITE) ||
					eventName.equalsIgnoreCase(SIPConstants.REINVITE) ||
					(eventName.endsWith("-" + SIPConstants.INVITE) &&
							statusCode >= 200)) {
				if (cancelSent) {
					Send s = new Send(MsgRef.SIP_MSG_TYPE, "ACK", true);
					ll.add(s);
					cancelSent = false;
					// Check to see if we received the response to the CANCEL before declaring the session
					// complete
					if (cancelResp)
						sessionComplete = false;
					else 
						sessionComplete = true;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " sending a ACK message because we sent a CANCEL message to an INVITE.");
				}
			}
			else if (eventName.equalsIgnoreCase("200-CANCEL")) {
				if (cancelSent && cancelResp) {
					cancelResp = false;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " received response to the CANCEL message.");
				}
				else if (!cancelSent && cancelResp) {
					cancelResp = false;
					logger.info(PC2LogCategory.FSM, subCat, 
							stateName + " received response to the CANCEL message, declaring session complete.");
					sessionComplete = true;
				}
				else {
					logger.warn(PC2LogCategory.FSM, subCat, 
							stateName + " received a " + statusCode + "-" + eventName + " message without a CANCEL message being sent.");
				}
			}
			else if (eventName.equals(TimeoutConstants.NO_RESPONSE_TIMEOUT)) {
				sessionComplete = true;
			}
			else if (eventName.equalsIgnoreCase(UtilityConstants.ONHOOK_COMPLETE) ||
					eventName.equalsIgnoreCase(UtilityConstants.ONHOOK_ERROR)) {
				sessionComplete = true;
			}
			// Basically we will wait in the FSM until the END state is called
			// By another FSM.
			else if (eventName.equals(EventConstants.SESSION_TERMINATED)) {
				if (noResponseTimer != null)
					noResponseTimer.cancel();
				// IF we have a transition let the FSM continue, otherwise we
				// can shut this FSM down.
				if (super.transitionExists(eventName))
					// Clear the reference point information in case we enter another
					// EndSession or END state
					owner.getReferencePointsFactory().clear();
				else
					super.api.shutdown();
			}
				
			if (ll.size() > 0) {
				ListIterator<Send> iter = ll.listIterator();
				while (iter.hasNext()) {
					Send s = iter.next();
					try {
						s.execute(super.api, -1);
					}
					catch (PC2Exception ex) {
						logger.error(PC2LogCategory.FSM, subCat,
								"Unable to send " 
								+ s.getMsgType() + " to " 
								+ s.getTarget() + " terminating test.");
					}
				}


			}
		}
	
		// See if we need to issue the onHook before we are actually
		// done.
		if (sessionComplete) {
			checkPACTServices();
			if (checkForOffHook())
				return true;
		}
		
		// Check whether we are done with the test and clean up
		if (sessionComplete) {
			this.disableNoResponseTimer=true;
			complete(event.msgQueueIndex);
			sessionComplete = false;
		}

		return true;
	}
	
	public void complete(int msgQueueIndex) {
		Generate g = new Generate(EventConstants.SESSION_TERMINATED, null, owner.getName());
		try {
			// First see if we have any postlude procesing to perform
			super.processPostlude(msgQueueIndex);
			g.execute(super.api, 0 );
			
		}
		catch (PC2Exception pce) {
			logger.error(PC2LogCategory.FSM, subCat,
					"EndSessionState " + name + " couldn't issue " 
					+ EventConstants.SESSION_TERMINATED + " event to the FSM.");
		}
	}
	/**
	 * Tests whether a transition exists for a given event.
	 * 
	 * @param key - the name of the event to search in the table for.
	 * @return - always returns true 
	 */
	@Override
	public boolean transitionExists(String key) {
		return true;
	
	}
	/**
	 * Retrieves the name of the next state if it exists in the transition
	 * table
	 * @param key - the event that is triggering the transition
	 * @return - it always returns itself
	 */
	@Override
	public String getNextState(String key) {
		if (key.equals(EventConstants.SESSION_TERMINATED)) {
			Transition t = findTransition(key);
			if (t != null) {
				return t.getTo();
			}
		}
		return name;
	}
	
	public void setIgnoreOffHook() {
		ignoreOffHook = true;
	}
	
	public void setEndImmediately() {
		endImmediately = true;
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		EndSessionState retval = (EndSessionState)super.clone();
		retval.sendBye = this.sendBye;
		retval.cancelSent = this.cancelSent;
		retval.sessionComplete = this.sessionComplete;
		retval.ignoreOffHook = this.ignoreOffHook;
		retval.endImmediately = this.endImmediately;
		return retval;
	}
}
