/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
/**
 * 
 */
package com.cablelabs.models;


import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.MsgRef;
import com.cablelabs.fsm.SDPConstants;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.fsm.SIPMsg;
import com.cablelabs.fsm.SDPRef;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.sim.PC2Protocol;
import com.cablelabs.sim.PCSim2;
import com.cablelabs.sim.Stacks;
import com.cablelabs.sim.StunDistributor;
import com.cablelabs.tools.RefLocator;

/**
 * @author ghassler
 *
 */
public class Stun extends PC2Models {

	static private final String model = "Stun";
	/**
	 * Constructor for the Stun model.
	 * 
	 * @param fsm - the FSM to use for the test.
	 */
	public Stun(FSM fsm) {
		super(fsm, Stun.class.getName(), model);
		fsm.setAPI(this);
		fsm.setComparisonEvaluator(examiner);
	}
	
	
	/**
	 * Initializes the model for processing
	 */
	@Override
	public void init() {
		super.init();
		Stacks.addStunListener(PC2Protocol.STUN, 
				SystemSettings.getSettings("DUT").getProperty(SettingConstants.IP), 
				SystemSettings.getSettings("DUT").getProperty(SettingConstants.IPv6_ZONE),
				this );
	}
	
	/**
	 * Initializes the current FSM, and begins executing each of the messages that
	 * gets delivered to the FSM.
	 */
	@Override
	public void run() {
		// First set the state into the initial state
		logger.info(PC2LogCategory.Model, subCat,
				"Beginning Stun model thread.");
		try {
			fsm.init(queue, this);
		}
		catch (IllegalStateException ise) {
			PCSim2.setTestPassed(false);
			String err = "Stun model failed during state machine initialization." + 
			    " Test terminated. Declaring test case failure.";
			logger.fatal(PC2LogCategory.Model, subCat,
					err);
			shuttingDown = true;
			PCSim2.setTestComplete();
		}
		
		super.run();
	}
	
	/**
	 * Implementation for the Send action within a FSM.
	 * 
	 * @return - true when the message was sent, false otherwise
	 */
	@Override
	public boolean send(Send s) {
        if (s.getProtocol().equals(MsgRef.STUN_MSG_TYPE) || 
				s.getProtocol().equals(MsgRef.RTP_MSG_TYPE)) {
			locateRequest(s);
			StunDistributor dist = Stacks.getStunDistributor();
			if (dist != null) {
				if (s.useIceLite()) {
					// Get the internal message that generated this event to obtain the 
					// FSM's unique id
					MsgEvent gen = q.findByProcotol(fsm.getUID(), MsgRef.EVENT_MSG_TYPE, 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
					if (gen != null && 
							gen instanceof InternalMsg) {
						InternalMsg im = (InternalMsg)gen;
						String name = im.getSender();
						if (name != null) {
							int sendUID = FSM.getFSM(name).getUID();
							// Now we need to get the last request and response to 
							// retrieve the SDP information
							MsgEvent req = q.find(sendUID, SIPConstants.INVITE,
									MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
							MsgEvent resp = q.find(sendUID, SIPConstants.RESPONSE, 
									MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
							if (req != null && resp != null &&
									req instanceof SIPMsg &&
									resp instanceof SIPMsg) {
								SIPMsg offer = (SIPMsg)req;
								SIPMsg answer = (SIPMsg)resp;
								MsgEvent useMsg = null;
								RefLocator locator = RefLocator.getInstance();
								SDPRef ref = new SDPRef("sdp");
								if (!offer.platformSent()) {
									useMsg = offer;
									ref.setMethod(offer.getEventName());
								}
								else if (!answer.platformSent()) {
									useMsg = answer;
									ref.setStatusCode(answer.getEventName().substring(0, 3));
									ref.setMethod(answer.getEventName().substring(4));
								}
								else {
									// This is only a precaution to prevent issues
									useMsg = im;
								}
								ref.setHeader(SDPConstants.ICE_UFRAG);
								ref.setParameter(SDPConstants.VALUE);
								String username = locator.getReferenceInfo(fsm.getUID(), ref, useMsg);
								if (username != null)
									s.setPeerICEUsername(username);
								ref.setHeader(SDPConstants.ICE_PWD);
								String password = locator.getReferenceInfo(fsm.getUID(), ref, useMsg);
								if (password != null)
									s.setPeerICEPassword(password);
							}
						}
					}
				}
				
				MsgEvent msgSent =  dist.send(this, s, fsm.getNetworkElements());
				
				if (msgSent == null) {
					logger.warn(PC2LogCategory.LOG_MSG, subCat, 
							"FSM (" + fsm.getName() + ") - State (" 
							+ fsm.getCurrentStateName() 
							+ ") failed to send any message for send action(" + s   
							+ ".");
							return false;
				}
					
				if (msgSent != null) {
		
					q.add(msgSent);
//					 LOG USING LOGMSG category so that the user can't disable which
					// would break the trace tool.
					logger.info(PC2LogCategory.LOG_MSG, subCat, 
							"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
							+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");
				}
				return true;
			}
		}
        else 
        	return super.send(s);
		
        return false;
	}
}
