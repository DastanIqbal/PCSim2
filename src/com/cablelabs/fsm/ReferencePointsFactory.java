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

import java.util.ListIterator;
import java.util.Timer;

import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.RequireHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.utility.UtilityAttribute;

/**
 * The generator of all ReferencePoint events. It maintains the status of 
 * message processing for an FSM as each is about to be consumed and following
 * its completion.
 * 
 * @author ghassler
 *
 */
public class ReferencePointsFactory {

	/**
	 * The FSM that owns this factory.
	 */
	protected FSM fsm= null;

	/**
	 * Logger
	 * 
	 */
	private LogAPI logger = LogAPI.getInstance();

	/**
	 * Accessor to the SystemSettings
	 */
	private SystemSettings ss = null;
	// Detected message occurrences
	protected boolean inviteRcvd = false;
	protected boolean inviteSent = false;
	protected boolean tryingRcvd = false;
	protected boolean tryingSent = false;
	protected boolean progressRcvd = false;
	protected boolean progressSent = false;
	protected boolean ringingRcvd = false;
	protected boolean ringingSent = false;
	protected boolean initPrackRcvd = false;
	protected boolean initPrackSent = false;
	protected boolean initPrackOKRcvd = false;
	protected boolean initPrackOKSent = false;
	protected boolean qosLocalAchieved = false;
	protected boolean qosRemoteAchieved = false;
	protected boolean finalPrackRcvd = false;
	protected boolean finalPrackSent = false;
	protected boolean finalPrackOKRcvd = false;
	protected boolean finalPrackOKSent = false;
	protected boolean inviteOKSent = false;
	protected boolean inviteOKRcvd = false;
	protected boolean inviteAckRcvd = false;
	protected boolean inviteAckSent = false;
	protected boolean byeSent = false;
	protected boolean byeRcvd = false;
	protected boolean cancelSent = false;
	protected boolean cancelRcvd = false;
	protected boolean cancelRespSent = false;
	protected boolean last183 = false;
	//protected boolean byeOKSent = false;
	//protected boolean byeOKRcvd = false;
	protected boolean dialogComplete = false;
	protected boolean proxy = false;

	// Factory generated events
	protected boolean provRspRxUAC = false;
	protected boolean earlyDialogUAC = false;
	protected boolean alertingUAC = false;
	protected boolean dialogConfirmedUAC = false;
	protected boolean earlyDialogUAS = false;
	protected boolean dialogConfirmedUAS = false;
	protected boolean provRspTxUAS = false;
	protected boolean inviteReceivedUAS = false;
	protected boolean alertingUAS = false;
	
	// These flags are used to help the END state determine what
	// messages still need to be sent to clean up the peer network
	// element when a test is terminating in the middle of a dialog
	// initiated by an INVITE
	protected boolean relSent = false;
	protected boolean relRcvd = false;
	protected boolean termSent = false;
	protected boolean termRcvd = false;
	protected boolean sessComplete = false;
	protected boolean finalResp = false;
//	protected UtilityMsg offHookMsg = null;
	// This is a static array of the voice ports on a voicetronix card for
	// offhook/onhook events. This allows one fsm to have a device
	// go offhook while another fsm has the same device go onhook.
	protected static final int VOICE_PORTS = 4;
	private static UtilityMsg [] offHookMsgs = new UtilityMsg [VOICE_PORTS];
	
	// Extension requirements
	protected boolean responseWith100Rel = false;

	// The statusCode of the INVITE dialog
	protected int inviteStatusCode = -1;
	
	protected String id = null;
	protected int cSeqNo = -1;
	
	protected String stopService = null;
	protected String stopPort = null;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = null;
	
	// Maximum time to establish a dialog
	private final long MAX_DIALOG_EST = 32000;
	
	/**
	 * This holds the timer for a dialog was initiated. If the 
	 * dialog has not established in 32 seconds, then the dialog
	 * is assumed to have failed and no further processing is
	 * necessary by the factory. The timer runs from Invite to
	 * ACK
	 */
	protected Timer dialogEstTimer;
	
	/** 
	 * Thread for the dialog timer.
	 */
	private RFPDialogTask dialogEstTask = null;
	/**
	 * Constructor.
	 * @param fsm
	 */
	public ReferencePointsFactory (FSM fsm) {
		 this.fsm = fsm;
//		 logger = fsm.getLogger(); // Logger.getLogger(FSM.class);
		 this.subCat = fsm.getSubcategory();
		 ss = SystemSettings.getInstance();
	 }

	 /**
	  * Clears the interal flags of the factory.
	  *
	  */
	 public void clear() {
		 stopDialogEstTimer();
		 inviteRcvd = false;
		 inviteSent = false;
		 tryingRcvd = false;
		 tryingSent = false;
		 progressRcvd = false;
		 progressSent = false;
		 ringingRcvd = false;
		 ringingSent = false;
		 initPrackRcvd = false;
		 initPrackSent = false;
		 initPrackOKRcvd = false;
		 initPrackOKSent = false;
		 qosLocalAchieved = false;
		 qosRemoteAchieved = false;
		 finalPrackRcvd = false;
		 finalPrackSent = false;
		 finalPrackOKRcvd = false;
		 finalPrackOKSent = false;
		 inviteOKSent = false;
		 inviteOKRcvd = false;
		 inviteAckRcvd = false;
		 inviteAckSent = false;
		 provRspRxUAC = false;
		 earlyDialogUAC = false;
		 alertingUAC = false;
		 dialogConfirmedUAC = false;
		 earlyDialogUAS = false;
		 dialogConfirmedUAS = false;
		 provRspTxUAS = false;
		 inviteReceivedUAS = false;
		 alertingUAS = false;
		 byeSent = false;
		 byeRcvd = false;
		 cancelSent = false;
		 cancelRcvd = false;
		 dialogComplete = false;
		 relSent = false;
		 relRcvd = false;
		 termSent = false;
		 termRcvd = false;
		 sessComplete = false;
		 finalResp = false;
		 last183 = false;
		 inviteStatusCode = -1;
		 id = null;
		 stopService = null;
		 stopPort = null;
	 }

	 /**
	  * Processes the sent request message for changes to the
	  * reference flags.
	  * @param req
	  */
	 private void sent(Request req) {
		 boolean flagsChanged = true;
		 String method = req.getMethod();
		 String reqCallId = ((CallIdHeader)req.getHeader(CallIdHeader.NAME)).getCallId();
		 int cseqNo = ((CSeqHeader)req.getHeader(CSeqHeader.NAME)).getSequenceNumber();
		 // We can't use cSeqNo in the id field because the
		 // cSeq number will change for intra-dialog messages 
		 // which we need for things like PRACK / reliability 
		 // operations
		 String reqID = reqCallId; // + cSeqNo;
		 if (id == null && method.equals(Request.INVITE)) {
			 id = reqID;
			 logger.info(PC2LogCategory.FSM, subCat, 
					 "Reference Point Factory assigning id=" + id + ".");
		 }
		 
		 if (id != null && id.equals(reqID)) {
			 if (method.equalsIgnoreCase(SIPConstants.INVITE)) {

				 if (!inviteSent && !inviteAckSent) {
					 inviteSent = true;
					 cSeqNo = cseqNo;
					 if (dialogEstTimer == null &&
							 !dialogComplete)
						 startDialogEstTimer();
				 }
			 }
			 else if (method.equalsIgnoreCase(SIPConstants.ACK)) {
				 if (inviteSent && !inviteAckSent) {
					 inviteAckSent = true;
					 stopDialogEstTimer();
				 }
			 }
			 else if (method.equalsIgnoreCase(SIPConstants.BYE)) {
				 if (!byeSent && !byeRcvd) {
					 byeSent = true;
				 }
			 }
			 else if (method.equalsIgnoreCase(SIPConstants.CANCEL)) {
				 if (inviteSent && !byeSent && !byeRcvd && !inviteOKRcvd) {
					 cancelSent = true;
				 }
			 }
			 else if (method.equalsIgnoreCase(SIPConstants.PRACK)) {
				 if (ss.getReliability() == Extension.REQUIRED) { 
					 if (!(ss.getPrecondition() == Extension.REQUIRED)) {
						 finalPrackSent = true;
					 }
					 else if (ss.getPrecondition() == Extension.REQUIRED){
						 initPrackSent = true;
					 }
				 }
			 }
			 else 
				 flagsChanged = false;

			 Object body = req.getContent();
			 if (body != null) {
				 String sdp = null;
				 if (body instanceof String)
					 sdp = (String)body;
				 else if (body instanceof byte[]) {
					 byte [] bytes = (byte [])body;
					 sdp = new String(bytes, 0, bytes.length);
				 }
				 if (sdp.contains(SDPConstants.LOCAL_SENDRECV)) {
					 qosLocalAchieved = true;
					 flagsChanged = true;
				 }
				 if (sdp.contains(SDPConstants.REMOTE_SENDRECV)) {
					 qosRemoteAchieved = true;
					 flagsChanged = true;
				 }
			 }
		 }
		 else if (method.equalsIgnoreCase(SIPConstants.BYE)) {
			 if (!byeSent && !byeRcvd) {
				 byeSent = true;
			 }
		 }
		 else {
			 logger.info(PC2LogCategory.FSM, subCat, 
			 "Reference Point Factory ignoring event " + method 
			 + " because it doesn't match the callId of the original request message.");
			 flagsChanged = false;
		 }
		 
		 if (flagsChanged)
			 generateEvents();
	 }

	 /**
	  * Processes the sent response message for changes to the
	  * reference flags.
	  * @param req
	  */
	 private void sent(Response resp) {
		 boolean flagsChanged = true;
		 String cSeqMethod = ((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getMethod();
		 int status = resp.getStatusCode();
		 String reqCallId = ((CallIdHeader)resp.getHeader(CallIdHeader.NAME)).getCallId();
		 int cseqNo = ((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getSequenceNumber();
//		 We can't use cSeqNo in the id field because the
		 // cSeq number will change for intra-dialog messages 
		 // which we need for things like PRACK / reliability 
		 // operations
		 String reqID = reqCallId; // + cSeqNo;
		 if (id == null && cSeqMethod.equals(Request.INVITE)) {
			 id = reqID;
			 logger.info(PC2LogCategory.FSM, subCat, 
					 "Reference Point Factory assigning id=" + id + ".");
		 }
		 if (id != null && id.equals(reqID)) {
			 if (cSeqMethod.equalsIgnoreCase(Request.INVITE) && 
					 !inviteAckSent &&
					 cseqNo == cSeqNo) {
				 
			     if (status == Response.TRYING)  {
					 if (!tryingSent) {
						 tryingSent = true;
					 }
				 }
				 else if (status == Response.RINGING) {
					 if (!ringingSent) {
						 ringingSent = true;
					 }
				 }
				 else if (status == Response.SESSION_PROGRESS) {
					 if (!progressSent) {
						 progressSent = true;
					 }
				 }
				 else if (status == Response.OK) {
					 if (!finalResp) {
						 if (!inviteOKSent) {
							 inviteOKSent = true;
							 inviteStatusCode = status;
						 }
					 }
				 }
				 else if (status > Response.OK && 
						 status <= 699 &&
						 status != 401 && 
						 status != 407) {
					 if (!finalResp) {
						 finalResp = true;
						 inviteStatusCode = status;
					 }
				 }
			 }
			 else if (cSeqMethod.equals(Request.BYE) ||
					 cSeqMethod.equals(Request.CANCEL)) {

				 if (status >= 200 && status <= 699) {
					 if (cSeqMethod.equals(Request.CANCEL))
						 cancelRespSent = true;
					 if (!dialogComplete) {
						 dialogComplete = true;

					 }
				 }
			 }
			 else if (cSeqMethod.equalsIgnoreCase(Request.PRACK) &&
					 status == 200) {
				 if (ss.getReliability() == Extension.REQUIRED) { 
					 if (!(ss.getPrecondition() == Extension.REQUIRED) || initPrackOKSent) {
						 finalPrackOKSent = true;
					 }
					 else if (ss.getPrecondition() == Extension.REQUIRED){
						 initPrackOKSent = true;
					 }
				 }
			 }
			 else if (cSeqMethod.equals(Request.INVITE) && 
					 status > 200 &&
					 status <= 699 &&
					 status != 401 && 
					 status != 407) {
				 if (!finalResp)
					 finalResp = true;
			 }
			 else 
				 flagsChanged = false;

			 if (status >= 180 && status < 190) {
				 if (status == Response.SESSION_PROGRESS) {
					 last183 = true;
					 flagsChanged = true;
				 }
				 else
					 last183 = false;
			 }

			 Object body = resp.getContent();
			 if (body != null) {
				 String sdp = null;
				 if (body instanceof String)
					 sdp = (String)body;
				 else if (body instanceof byte[]) {
					 byte [] bytes = (byte [])body;
					 sdp = new String(bytes, 0, bytes.length);
				 }
				 if (sdp.contains(SDPConstants.LOCAL_SENDRECV)) {
					 qosLocalAchieved = true;
					 flagsChanged = true;
				 }
				 if (sdp.contains(SDPConstants.REMOTE_SENDRECV)) {
					 qosRemoteAchieved = true;
					 flagsChanged = true;
				 }
			 }
		 }
		 else if (cSeqMethod.equals(Request.BYE)) {
			 if (status >= 200 && status <= 699) {
				 if (!dialogComplete) {
					 dialogComplete = true;

				 }
			 }
		 }
		 else {
			 logger.info(PC2LogCategory.FSM, subCat, 
					 "Reference Point Factory ignoring event " + status + "-" + cSeqMethod 
					 + " because it doesn't match the callId of the original request message.");
					 flagsChanged = false;
		 }
		 
		 if (flagsChanged)
			 generateEvents();
	 }
	 
	 /**
	  * Processes the sent message for changes to the
	  * reference flags.
	  * @param req
	  */
	 private void sent(UtilityMsg event) {
		 boolean flagsChanged = false;
		 
		 if (event.getEventName().equalsIgnoreCase(UtilityConstants.OFFHOOK)) {
			 int index = getOffHookIndex(event);
			 if (index != -1)
				 synchronized (offHookMsgs) {
					 offHookMsgs[index] = event;
				 }
		}
		 else if (event.getEventName().equalsIgnoreCase(UtilityConstants.ONHOOK)) {
			 int index = getOffHookIndex(event);
			 if (index != -1)
				 synchronized (offHookMsgs) {
					 offHookMsgs[index] = null;
				 }
		 }
		 else if (event.getEventName().equalsIgnoreCase(UtilityConstants.PROV_STOP_SERVICE)) {
			 stopService = event.getUtilityEvent().getMessage().getAttribute(UtilityConstants.SRV_PROTOCOL_ATTR).getValue();
		 }
		 else if (event.getEventName().equalsIgnoreCase(UtilityConstants.PROV_RESUME_SERVICE)) {
			 stopService = null;
		 }
		 else if (event.getEventName().equalsIgnoreCase(UtilityConstants.PROV_MANAGE_PORT)) {
			 if (event.getUtilityEvent().getMessage().getAttribute(UtilityConstants.MANAGE_PORT_OP) != null) {
				 if (event.getUtilityEvent().getMessage().getAttribute(UtilityConstants.MANAGE_PORT_OP).getValue().equalsIgnoreCase("start"))
					 stopPort = null;
				 else if (event.getUtilityEvent().getMessage().getAttribute(UtilityConstants.MANAGE_PORT_OP).getValue().equalsIgnoreCase("stop"))
					 stopPort = event.getUtilityEvent().getMessage().getAttribute(UtilityConstants.SRV_PROTOCOL_ATTR).getValue();
			 }
		 }
			 
		 if (flagsChanged)
			 generateEvents();
	 }

	 /**
	  * Processes the sent message for changes to the
	  * reference flags.
	  * @param req
	  */
	 private void sent(String event) {
		 boolean flagsChanged = false;
		 
		 if (flagsChanged)
			 generateEvents();
	 }

	 /**
	  * Entry point for the factory to process all events sent
	  * by the FSM. 
	  * @param event
	  */
	 public void sent(MsgEvent event) {

		 if (event instanceof SIPMsg) {
			 SIPMsg msg = (SIPMsg)event;
			 if (msg.isRequestMsg()) {
				 sent(msg.getRequest());
			 }
			 else if (msg.isResponseMsg()) {
				 sent(msg.getResponse());
			 }

		 }
		 else if (event instanceof UtilityMsg)  {
			 UtilityMsg um = (UtilityMsg)event;
			 sent(um);
		 }
		 else if (event instanceof InternalMsg ||
				  event instanceof StunMsg) {
			 sent(event.getEventName());
		 }

	 }


	 /**
	  * Entry point for the factory to process all events received
	  * by the FSM. 
	  * @param event
	  */
	 private void rcvd(Request req) {
		 boolean flagsChanged = true;
		 String method = req.getMethod();
		 String reqCallId = ((CallIdHeader)req.getHeader(CallIdHeader.NAME)).getCallId();
		 int cseqNo = ((CSeqHeader)req.getHeader(CSeqHeader.NAME)).getSequenceNumber();
		 // We can't use cSeqNo in the id field because the
		 // cSeq number will change for intra-dialog messages 
		 // which we need for things like PRACK / reliability 
		 // operations
		 String reqID = reqCallId; // + cSeqNo;
		 if (id == null && method.equals(Request.INVITE)) {
			 id = reqID;
			 logger.info(PC2LogCategory.FSM, subCat, 
				 "Reference Point Factory assigning id=" + id + ".");
		 }

		 if (id != null && id.equals(reqID)) {
			 if (method.equalsIgnoreCase(SIPConstants.INVITE)) {
				 if (!inviteRcvd && !inviteAckRcvd) {
					 inviteRcvd = true;
					 cSeqNo = cseqNo;
					 if (dialogEstTimer == null &&
							 !dialogComplete) 
						 startDialogEstTimer();
				 }
				 if (id == null) {
					 id = reqID;
					 logger.info(PC2LogCategory.FSM, subCat, 
						 "Reference Point Factory assigning id=" + id + ".");
				 }
			 }
			 else if (method.equalsIgnoreCase(SIPConstants.ACK)) {
				 if (inviteRcvd && !inviteAckRcvd) {
					 inviteAckRcvd = true;
					 stopDialogEstTimer();

				 }
			 }
			 else if (method.equalsIgnoreCase(SIPConstants.BYE)) {
				 if (!byeSent && !byeRcvd) {
					 byeRcvd = true;
				 }
			 }
			 else if (method.equalsIgnoreCase(SIPConstants.CANCEL)) {
				 if (inviteRcvd && !byeSent && !byeRcvd && !inviteOKSent) {
					 cancelRcvd = true;
				 }
			 }
			 else if (method.equalsIgnoreCase(SIPConstants.PRACK)) {
				 if (ss.getReliability() == Extension.REQUIRED) { 
					 if (!(ss.getPrecondition() == Extension.REQUIRED) || initPrackRcvd) {
						 finalPrackRcvd = true;
					 }
					 else if (ss.getPrecondition() == Extension.REQUIRED){
						 initPrackRcvd = true;
					 }
				 }
			 }
			 else 
				 flagsChanged = false;

			 Object body = req.getContent();
			 if (body != null) {
				 String sdp = null;
				 if (body instanceof String)
					 sdp = (String)body;
				 else if (body instanceof byte[]) {
					 byte [] bytes = (byte [])body;
					 sdp = new String(bytes, 0, bytes.length);
				 }
				 if (sdp.contains(SDPConstants.LOCAL_SENDRECV)) {
					 qosLocalAchieved = true;
					 flagsChanged = true;
				 }
				 if (sdp.contains(SDPConstants.REMOTE_SENDRECV)) {
					 qosRemoteAchieved = true;
					 flagsChanged = true;
				 }
			 }
		 }
		 else if (method.equalsIgnoreCase(SIPConstants.BYE)) {
			 if (!byeSent && !byeRcvd) {
				 byeRcvd = true;
			 }
		 }
		 else {
			 logger.info(PC2LogCategory.FSM, subCat, 
					 "Reference Point Factory ignoring event " + method 
					 + " because it doesn't match the callId of the original request message.");
					 flagsChanged = false;
		 }

		 if (flagsChanged)
			 generateEvents();
	 }

	 /**
	  * Processes the received response message for changes to the
	  * reference flags.
	  * @param req
	  */
	 private void rcvd(Response resp) {
		 boolean flagsChanged = true;
		 String cSeqMethod = ((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getMethod();
		 int status = resp.getStatusCode();
		 String reqCallId = ((CallIdHeader)resp.getHeader(CallIdHeader.NAME)).getCallId();
		 int cseqNo = ((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getSequenceNumber();
		 // We can't use cSeqNo in the id field because the
		 // cSeq number will change for intra-dialog messages 
		 // which we need for things like PRACK / reliability 
		 // operations
		 String reqID = reqCallId; // + cSeqNo;
		 if (id == null && cSeqMethod.equals(Request.INVITE)) {
			 id = reqID;
			 logger.info(PC2LogCategory.FSM, subCat, 
					 "Reference Point Factory assigning id=" + id + ".");
		 }
		 if (id != null && id.equals(reqID)) {
			 if (cSeqMethod.equals(Request.INVITE) && 
					 !inviteAckRcvd &&
					 cSeqNo == cseqNo) {
				 if (status == Response.TRYING){
					 if (!tryingRcvd) {
						 tryingRcvd = true;
					 }
				 }
				 else if (status == Response.RINGING) {
					 if (!ringingRcvd) {
						 RequireHeader reqHdr = (RequireHeader)resp.getHeader(RequireHeader.NAME);
						 if (reqHdr != null) {
							 String option = reqHdr.getOptionTag();
							 if (option.equals("100rel")) {
								 responseWith100Rel = true;
							 }
							 else
								 responseWith100Rel = false;
						 }
						 ringingRcvd = true;
					 }
				 }
				 else if (status == Response.SESSION_PROGRESS) {
					 if (!progressRcvd) {
						 RequireHeader reqHdr = (RequireHeader)resp.getHeader(RequireHeader.NAME);
						 if (reqHdr != null) {
							 String option = reqHdr.getOptionTag();
							 if (option.equals("100rel")) {
								 responseWith100Rel = true;
							 }
							 else
								 responseWith100Rel = false;
						 }
						 progressRcvd = true;
					 }
				 }
				 else if (status == Response.OK)  {
					 if (!inviteOKRcvd) {
						 inviteOKRcvd = true;
					 }
				 }else if (status >= 300 && 
						 status <= 699 &&
						 status != 401 && 
						 status != 407) {
					 if (!finalResp) 
						 finalResp = true;
					 if (inviteStatusCode == -1)
						 inviteStatusCode = status;
				 }
			 }
			 else if (cSeqMethod.equalsIgnoreCase(SIPConstants.BYE) ||
					 cSeqMethod.equalsIgnoreCase(SIPConstants.CANCEL)) {
				 if (status >= 200 && status <= 699) {
					 if (!dialogComplete) {
						 dialogComplete = true;

					 }
				 }
			 }
			 else if (cSeqMethod.equalsIgnoreCase(SIPConstants.PRACK)) {
				 if (ss.getReliability() == Extension.REQUIRED) { 
					 if (!(ss.getPrecondition() == Extension.REQUIRED)) {
						 finalPrackOKRcvd = true;
					 }
					 else if (ss.getPrecondition() == Extension.REQUIRED){
						 initPrackOKRcvd = true;
					 }
				 }
			 }
			 else 
				 flagsChanged = false;

			 Object body = resp.getContent();
			 if (body != null) {
				 String sdp = null;
				 if (body instanceof String)
					 sdp = (String)body;
				 else if (body instanceof byte[]) {
					 byte [] bytes = (byte [])body;
					 sdp = new String(bytes, 0, bytes.length);
				 }
				 if (sdp.contains(SDPConstants.LOCAL_SENDRECV)) {
					 qosLocalAchieved = true;
					 flagsChanged = true;
				 }
				 if (sdp.contains(SDPConstants.REMOTE_SENDRECV)) {
					 qosRemoteAchieved = true;
					 flagsChanged = true;
				 }
			 }
		 }
		 else if (cSeqMethod.equalsIgnoreCase(SIPConstants.BYE)) {
			 if (status >= 200 && status <= 699) {
				 if (!dialogComplete) {
					 dialogComplete = true;

				 }
			 }
		 }
		 else {
			 logger.info(PC2LogCategory.FSM, subCat, 
					 "Reference Point Factory ignoring event " + status + "-" + cSeqMethod 
					 + " because it doesn't match the callId of the original request message.");
					 flagsChanged = false;
		 }
		 
		 if (flagsChanged)
			 generateEvents();
	 }

	 /**
	  * Processes the received message for changes to the
	  * reference flags.
	  * @param req
	  */
	 private void rcvd(UtilityMsg event) {
		 boolean flagsChanged = false;
		 
		 
		 if (event.getEventName().equalsIgnoreCase(UtilityConstants.OFFHOOK_ERROR)) {
			 int index = getOffHookIndex(event);
			 if (index != -1)
				 synchronized (offHookMsgs) {
					 offHookMsgs[index] = null;
				 }
		 }
		 
		 if (flagsChanged)
			 generateEvents();
	 }
	 
	 /**
	  * Processes the received message for changes to the
	  * reference flags.
	  * @param req
	  */
	 private void rcvd(String event) {
		 boolean flagsChanged = false;
		 if (flagsChanged)
			 generateEvents();
	 }
	 
	 public void rcvd(MsgEvent event) {
		 if (event instanceof SIPMsg) {
			 SIPMsg msg = (SIPMsg)event;
			 if (msg.isRequestMsg())
				 rcvd(msg.getRequest());
			 else if (msg.isResponseMsg())
				 rcvd(msg.getResponse());
		 }
		 else if (event instanceof UtilityMsg) {
			  rcvd((UtilityMsg)event);
		 }
		 else if (event instanceof InternalMsg ||
				  event instanceof StunMsg) 
			  rcvd(event.getEventName());
	 }

	 private int getOffHookIndex(UtilityMsg um) {
		int index = -1;
		ListIterator<UtilityAttribute> iter = um.getUtilityEvent().getMessage().getAttributes();
		while (iter.hasNext()) {
			 UtilityAttribute ua = iter.next();
			 if (ua.getName().equals(SettingConstants.VOICE_PORT)) {
				 int temp = Integer.parseInt(ua.getValue());
				 if (temp >= 0 && temp <= 3)
					 // Make the value zero based
					 index = temp;
			 }
		}
		
		return index;
	 }
	 
	 public static UtilityMsg getOffHookMsg(int index) {
		 // We only want one EndSessionState or END state to
		 // generate the on hook event so remove the message
		 // once one of the states have requested it.
		 synchronized (offHookMsgs) {
			 UtilityMsg um = offHookMsgs[index];
			 return um;
		 }
	 }
		 /**
	  * The method that actually creates the ReferencePointEvent and delivers it
	  * to the FSM for processing.
	  *
	  */
	 protected void generateEvents() {
		 // Test for provisional response received
		 if (!provRspRxUAC && 
				 !(ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 (tryingRcvd || progressRcvd || ringingRcvd)) {
			 logger.debug(PC2LogCategory.FSM, subCat, 
					 "adding reference point event PROV_RSP_RX_UAC to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.PROV_RSP_RX_UAC);
			 fsm.getListener().processEvent(msg);

			 provRspRxUAC = true;
		 }
		 else if (!provRspRxUAC && 
				 (ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 ((progressRcvd || ringingRcvd) &&
						 initPrackSent && initPrackOKRcvd)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event PROV_RSP_RX_UAC to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.PROV_RSP_RX_UAC);
			 fsm.processEvent(msg);

			 provRspRxUAC = true;
		 }

		 // Early dialog is established 
		 if (!earlyDialogUAC &&
				 !(ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 (progressRcvd || ringingRcvd)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event EARLY_DIALOG_UAC to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.EARLY_DIALOG_UAC);
			 fsm.getListener().processEvent(msg);

			 earlyDialogUAC = true;
		 }
		 else if (!earlyDialogUAC &&
				 (ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 ((progressRcvd || ringingRcvd) && 
						 initPrackSent && initPrackOKRcvd)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event EARLY_DIALOG_UAC to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.EARLY_DIALOG_UAC);
			 fsm.getListener().processEvent(msg);

			 earlyDialogUAC = true;
		 }

		 // Terminating UE is ringing or being alerted
		 if (!alertingUAC && 
				 !(ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 !(ss.getPrecondition() == Extension.REQUIRED) &&
				 (progressRcvd || ringingRcvd)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event ALERTING_UAC to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.ALERTING_UAC);
			 fsm.getListener().processEvent(msg);

			 alertingUAC = true;
		 }
		 else if (!alertingUAC && 
				 (ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 ((progressRcvd || ringingRcvd) &&
						 finalPrackSent && finalPrackOKRcvd)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event ALERTING_UAC to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.ALERTING_UAC);
			 fsm.getListener().processEvent(msg);

			 alertingUAC = true;	
		 }
		 else if (!alertingUAC && 
				 ((progressRcvd || ringingRcvd) &&
						 (ss.getPrecondition() == Extension.REQUIRED) &&
						 finalPrackSent && finalPrackOKRcvd && 
						 qosLocalAchieved && qosRemoteAchieved)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event ALERTING_UAC to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.ALERTING_UAC);
			 fsm.getListener().processEvent(msg);

			 alertingUAC = true;	
		 }

		 // Dialog Confirm
		 if (!dialogConfirmedUAC && inviteOKRcvd && !dialogConfirmedUAS) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event DIALOG_CONFIRMED_UAC to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.DIALOG_CONFIRMED_UAC);
			 fsm.getListener().processEvent(msg);

			 dialogConfirmedUAC = true;
		 }

		 // Early dialog is established
		 if (!earlyDialogUAS &&
				 !(ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 (tryingSent || progressSent || ringingSent)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event EARLY_DIALOG_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.EARLY_DIALOG_UAS);
			 fsm.getListener().processEvent(msg);

			 earlyDialogUAS = true;
		 }
		 else if (!earlyDialogUAS &&
				 (ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 (( progressSent || ringingSent) &&
						 initPrackRcvd && initPrackOKSent)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event EARLY_DIALOG_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.EARLY_DIALOG_UAS);
			 fsm.getListener().processEvent(msg);

			 earlyDialogUAS = true;
		 }

		 // Dialog Confirmed
		 if (!dialogConfirmedUAS && !dialogConfirmedUAC &&
				 inviteAckRcvd) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event DIALOG_CONFIRMED_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.DIALOG_CONFIRMED_UAS);
			 fsm.getListener().processEvent(msg);

			 dialogConfirmedUAS = true;
		 }

		 // Provisional response is sent
		 if (!provRspTxUAS &&
				 !(ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 (tryingSent || progressSent || ringingSent)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event PROV_RSP_TX_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.PROV_RSP_TX_UAS);
			 fsm.getListener().processEvent(msg);

			 provRspTxUAS = true;
		 }
		 else if (!provRspTxUAS &&
				 (ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 ((progressSent || ringingSent) && 
						 initPrackRcvd && initPrackOKSent)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event PROV_RSP_TX_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.PROV_RSP_TX_UAS);
			 fsm.getListener().processEvent(msg);

			 provRspTxUAS = true;
		 }

		 // First invite is received and no response sent
		 if (!inviteReceivedUAS && inviteRcvd) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event INVITE_RECEIVED_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.INVITE_RECEIVED_UAS);
			 fsm.getListener().processEvent(msg);

			 inviteReceivedUAS = true;
		 }

		 // Terminating UE is ringing or being alerted
		 if (!alertingUAS &&
				 !(ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 !(ss.getPrecondition() == Extension.REQUIRED) &&
				 (progressSent || ringingSent)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event ALERTING_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.ALERTING_UAS);
			 fsm.getListener().processEvent(msg);

			 alertingUAS = true;
		 }
		 else if (!alertingUAS &&
				 (ss.getReliability() == Extension.REQUIRED || responseWith100Rel) &&
				 ((progressSent || ringingSent) &&
						 finalPrackRcvd && finalPrackOKSent)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event ALERTING_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.ALERTING_UAS);
			 fsm.getListener().processEvent(msg);

			 alertingUAS = true;
		 }
		 else if (!alertingUAS &&
				 (ss.getPrecondition() == Extension.REQUIRED) &&
				 ((progressSent || ringingSent) &&
						 (ss.getReliability() != Extension.REQUIRED || (finalPrackRcvd && finalPrackOKSent)) &&
						 qosLocalAchieved && qosRemoteAchieved)) {
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "adding reference point event ALERTING_UAS to event queue.");
			 InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
					 LogAPI.getSequencer(), ReferencePointConstants.ALERTING_UAS);
			 fsm.getListener().processEvent(msg);

			 alertingUAS = true;
		 }
		 
		 else if (byeSent && !relSent && !termSent && 
				 !termRcvd && !relRcvd && !sessComplete &&
				 (inviteAckRcvd || inviteAckSent)) {
			 relSent = true;
		 }
		 else if (byeRcvd && !relSent && !termSent && 
				 !termRcvd && !relRcvd && !sessComplete &&
				 (inviteAckRcvd || inviteAckSent)) {
			 relRcvd = true;
		 }
		 else if (cancelSent && !termSent && 
				 !termRcvd && !relRcvd && !sessComplete &&
				 !inviteOKRcvd && !inviteOKSent && inviteAckSent) {
			 termSent = true;
		 }
		 else if (cancelRcvd && !termSent && 
				 !termRcvd && !relRcvd && !sessComplete &&
				 !inviteOKRcvd && !inviteOKSent && inviteAckRcvd) {
			 termRcvd = true;
		 }
		 else if (!sessComplete && 
				 inviteAckRcvd && 
				 inviteRcvd && 
				 finalResp)  {
			 sessComplete = true;
		 }
		 else if (!sessComplete && 
				 inviteSent && 
				 inviteAckSent && 
				 finalResp)  {
			 sessComplete = true;
		 }
		 else if (dialogComplete && (termSent || termRcvd || relSent || relRcvd)) {
			 sessComplete = true;
		 }
	 }

	 private void startDialogEstTimer() {
		 dialogEstTimer = new Timer(fsm.getName() + " : RFPDialogTimer", true);

		 dialogEstTask = new RFPDialogTask(this);
		 dialogEstTimer.schedule(dialogEstTask, MAX_DIALOG_EST);
		 logger.debug(PC2LogCategory.FSM, subCat,
				 "Starting RFPDialogTimer(" + dialogEstTimer + ") for " + 
				 (MAX_DIALOG_EST/1000) + " secs.");
	 }

	 public void dialogEstTimerExpired(RFPDialogTask t) {
		 if (t == dialogEstTask) {
			 if (dialogEstTimer != null) {
				 dialogEstTimer.cancel();
				 dialogEstTimer = null; 
			 }
			 dialogEstTask.cancel();
			 sessComplete = true;
			 logger.debug(PC2LogCategory.FSM, subCat,
					 "RFPDialogTimer(" + dialogEstTimer + ") for " + 
					 (MAX_DIALOG_EST/1000) 
					 + " secs has expired. Declaring dialog complete.");
			 dialogEstTask = null;
			 
		 }

	 }

	 /**
	  * Terminates the dialog timer if it is still running.
	  *
	  */
	 private void stopDialogEstTimer() {
		 if (dialogEstTimer != null) {
			 dialogEstTimer.cancel();
			 dialogEstTimer = null;
			 if (dialogEstTask != null) {
				 dialogEstTask.cancel();
				 dialogEstTask = null;
			 }
		 }
	 }
	 
	 public void shutdown() {
		 stopDialogEstTimer();
	 }
	 
	 @Override
	public String toString() {
		 String result = super.toString() + "\n" + " hook state table: ";
		 for (int i=0; i<VOICE_PORTS; i++) {
			 result += " voiceport[" + i + "] is " 
				 + ((offHookMsgs[i] == null) ? "onhook." : "offhook.") + "\n";
		 }
		 return result;
	 }
	 
	 public void stopService(String service) {
		 this.stopService = service;
	 }
	 
	 public void managePort(String port) {
		 this.stopPort = port;
	 }
	 
	 public String getStopService() {
		 return this.stopService;
	 }
	 
	 public String getManagePort() {
		 return this.stopPort;
	 }
}
