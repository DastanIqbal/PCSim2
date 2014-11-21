/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.sim;


import gov.nist.javax.sip.message.SIPResponse;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

import javax.sip.InvalidArgumentException;
import javax.sip.address.SipURI;
import javax.sip.address.TelURL;
import javax.sip.address.URI;
import javax.sip.header.AcceptHeader;
import javax.sip.header.AuthenticationInfoHeader;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.MimeVersionHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.RSeqHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ReferToHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.TargetDialogHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.cablelabs.fsm.Extension;
import com.cablelabs.fsm.Mod;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.MsgRef;
import com.cablelabs.fsm.NetworkElements;
import com.cablelabs.fsm.Reference;
import com.cablelabs.fsm.SDPConstants;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.fsm.SIPMsg;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.models.PC2Models;
import com.cablelabs.models.PresenceServer;
import com.cablelabs.tools.RefLocator;
import com.cablelabs.tools.SDPLocator;
import com.cablelabs.tools.SIPLocator;
import com.cablelabs.utility.UtilityAttribute;
import com.cablelabs.utility.UtilityEvent;

/**
 * This class constructs the SIP messages for the application layer
 * it takes the data from the configuration files and previous
 * messages to construct a new SIP message for transmission to an
 * external network element.
 * 
 * @author ghassler
 *
 */
public class SIPManufacturer {
	
	/**
	 * This is a utility class for creating different header fields
	 * within a message.
	 */
	private SIPUtils utils = null;
	//private static SystemSettings ss = null;
	
	/**
	 * The SIP stack's Message Factory
	 */
	private static MessageFactory messageFactory = null;
	
	/**
	 * Private logger for the class
	 */
	private LogAPI logger = LogAPI.getInstance();
	
	/**
	 * Local reference to the SIPLocator for modifying 
	 * the SIP portion of messages.
	 */
	private SIPLocator sipLocator = SIPLocator.getInstance();
	
	/**
	 * Local reference to the SIPLocator for modifying 
	 * the SIP portion of messages.
	 */
	private SDPLocator sdpLocator = SDPLocator.getInstance();
	
	/**
	 * Local reference to the Reference Locator for modifying 
	 * the SIP portion of messages from a msg_ref.
	 */
	private RefLocator refLocator = RefLocator.getInstance();
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private static final String subCat = SIPDistributor.subCat;

//	 Starts at 71 so that as each Via is added it is subtracts
//   one from the value.
	private final int maxHops = 71; 
	
	private MsgQueue q = MsgQueue.getInstance();
	
	private boolean useGRUU = false;
	
	protected Hashtable<String, RegistrarData> gruuDB = null;
	
	protected Hashtable<String, RegistrarData> aorGruuIndex = null;
	
 	/**
	 * Constructor.
	 *
	 */
	public SIPManufacturer() {
		messageFactory = SIPDistributor.getMessageFactory();
		utils = new SIPUtils(this);
		SystemSettings ss = SystemSettings.getInstance();
		Extension ext = ss.getGRUU();
		if (ext == Extension.SUPPORTED ||
			ext == Extension.REQUIRED)
			useGRUU = true;
		
		gruuDB = new Hashtable<String, RegistrarData>();
		aorGruuIndex = new Hashtable<String, RegistrarData>();
	}
	
	/**
	 * Creates a ACK message from the invite message passed in as an argument
	 * with the corresponding To tag.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param sipData - sip information about this dialog.
	 * @param req - the original INVITE message that the ACK is in response to.
	 * @param response - the final response to the INVITE message.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - a SIP ACK message
	 * @throws ParseException
	 * @throws IllegalStateException
	 */
	public Request buildAck(Send send, SIPRoute rte, PC2SipData sipData, Request req, 
			Response resp, NetworkElements nes) throws ParseException, IllegalStateException {	
		String method = Request.ACK;
		
		URI uri = null;
		
		if (sipData.getFinalResponse() == 200) {
			if (sipData.getSSInitiated() &&  sipData.getResponseContact() != null) {
				uri = utils.createRequestURI(sipData.getResponseContact());
				logger.debug(PC2LogCategory.SIP, subCat, "SIPManufacturer using Response's Contact for Request-URI");
			}
			else {
				uri = utils.createRequestURI(sipData.getRequestContact());
				logger.debug(PC2LogCategory.SIP, subCat, 
				"SIPManufacturer using Request's Contact for Request-URI");
			}
		}
		else {
				uri = utils.createRequestURI(req.getRequestURI().toString());
				logger.debug(PC2LogCategory.SIP, subCat, 
						"SIPManufacturer using Request's Request-URI for Request-URI");
			
		}
		
		logger.info(PC2LogCategory.SIP, subCat, 
				"Request Contact=" + sipData.getRequestContact() 
				+ "\nResponse Contact=" + sipData.getResponseContact());
		ListIterator<Header> iter = req.getHeaders(ViaHeader.NAME);
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		while (iter.hasNext()) {
			ViaHeader viaOrig = (ViaHeader)iter.next();
			ViaHeader via = (ViaHeader)viaOrig.clone();
			int statusCode = resp.getStatusCode();
			if (sipData.isDialogAccepted() && 
					// we have to check the current response as well
					// as this could be a response to a REINVITE
					(statusCode >= 200 && 
							statusCode <= 299)) {
				String branch = via.getBranch() + "_ack";
				via.setBranch(branch);
			}
			viaHeaders.add(via);
		}
		ToHeader toHeader = (ToHeader)req.getHeader(ToHeader.NAME);
		FromHeader fromHeader = (FromHeader)req.getHeader(FromHeader.NAME);
		if (toHeader.getTag() == null && sipData.getToTag() != null) {
			if (sipData.getSSInitiated())
				toHeader.setTag(sipData.getToTag());
			else 
				fromHeader.setTag(sipData.getToTag());
		}
//		
		CSeqHeader cSeqHeader = (CSeqHeader)req.getHeader(CSeqHeader.NAME);
		int seqNo = cSeqHeader.getSequenceNumber();
		try {
			cSeqHeader = utils.createCSeqHeader(seqNo, method);
		}
		catch (Exception ex) {
			
		}
		
		// Create the request.
		Request request =
			messageFactory.createRequest(
					uri, // (URI)req.getRequestURI(),
					method,
					(CallIdHeader)req.getHeader(CallIdHeader.NAME),
					cSeqHeader, 
					fromHeader,
					toHeader,
					viaHeaders,
					(MaxForwardsHeader)req.getHeader(MaxForwardsHeader.NAME));
		String dutType = SystemSettings.getSettings("DUT").getProperty(SettingConstants.DEVICE_TYPE);
		
		if (rte.target.getProperty(SettingConstants.DEVICE_TYPE).equals("AS")) {
			try {
				insertRoutingForAS(request, rte, nes);
			}
			catch (Exception e) {
				logger.warn(PC2LogCategory.SIP, subCat,
						"Encountered an exception when trying to add routing " +
						"info to ACK message.");
			}
		}
		else if (!sipData.getSSInitiated()) {
			iter = resp.getHeaders(RecordRouteHeader.NAME);
			while (iter.hasNext()) {
				RecordRouteHeader rr = (RecordRouteHeader)iter.next();
				String transport = rr.getParameter("transport");
				RouteHeader rh = utils.createRouteHeader(rr, transport);
				if (rh != null) 
					request.addHeader(rh);
			}
		}
		else if (rte.srcDeviceType.equals(SettingConstants.UE) && 
				(dutType.equals("CN") || 
						dutType.equals("PCSCF"))){
			String srcPCSCF = rte.src.getProperty(SettingConstants.PCSCF);
			if (srcPCSCF != null) {
				if (srcPCSCF.equals("DUT"))
					srcPCSCF = SettingConstants.PCSCF + "0";
				Properties myP = SystemSettings.getSettings(srcPCSCF);
				// String realP = myP.getProperty(SettingConstants.SIMULATED);
				String srcSCSCF = myP.getProperty(SettingConstants.SCSCF);
				if (srcSCSCF != null) {
					if (srcSCSCF.equals("DUT"))
						srcSCSCF = SettingConstants.SCSCF + "0";
					Properties myS = SystemSettings.getSettings(srcSCSCF);
					String transport = null;
					if (SystemSettings.useTransportParameter())
						transport = rte.transport.toString();
					RouteHeader rh = utils.createRouteHeader(myP, false, 
							rte.localPort, transport);
					if (rh != null) 
						request.addHeader(rh);
					rh = utils.createRouteHeader(myS, false, 
							rte.localPort, transport);
					if (rh != null) 
						request.addHeader(rh);
				}
			}
		}
		else if (rte.srcDeviceType.equals(SettingConstants.UE) && dutType.equals(SettingConstants.UE)) {
			ListIterator<Header> rrh = resp.getHeaders(RecordRouteHeader.NAME);
			utils.addRouteFromRecordRoute(rrh, request);
		}
		
		// Lastly determine if we need to include a SDP body
		if (send.getIncludeSDP()) {
//			Create ContentTypeHeader
			try {
				ContentTypeHeader contentTypeHeader = utils.createContentTypeHeader(null, null);
				String remoteDirectionTag = null;
				Object body = resp.getContent();
				String sdp = null;
				if (body instanceof String) {
					sdp = (String)body;
				}
				else if (body instanceof byte []) {
					byte [] bytes = (byte [])body;
					sdp = new String(bytes, 0, bytes.length);

				}
				if (sdp != null) {
					int begin = sdp.indexOf(SDPConstants.A_CUR_QOS_L);
					if (begin != -1) {
						begin += 18;
						int end = sdp.indexOf("\r\n", begin);
						remoteDirectionTag = sdp.substring(begin, end);
					}
				}

				String sdpData = utils.createSDPData(rte.src, rte.srcNE, false, 
						remoteDirectionTag, false, false, PC2SipData.SESSION_ID,
						sipData.getSessionVersion());

				request.setContent(sdpData, contentTypeHeader);
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.SIP, subCat, 
						"Manufacturer couldn't add a SDP body to the ACK request.\n" + ex.getMessage());
			}
		}
		
		return request;
	}

	/**
	 * Creates a SIP BYE message for the dialog defined by the Request parameter.
	 * It uses the toTag, target and source to construct the various headers of the 
	 * message.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param sipData - sip information about this dialog.
	 * @param req - The original INVITE message that the BYE is being sent for.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - the SIP BYE message
	 * @throws ParseException
	 * @throws IllegalStateException
	 */
	public Request buildBye(Send send, SIPRoute rte, PC2SipData sipData, Request req, 
			NetworkElements nes) throws ParseException, IllegalStateException {	
		try {
			String method = Request.BYE;
			
			
			URI uri = null;
			if (sipData.getSSInitiated()) {
				uri = utils.createRequestURI(sipData.getResponseContact());
				logger.debug(PC2LogCategory.SIP, subCat, 
						"SIPManufacturer using Response's Contact for Request-URI");
			}
			else {
				uri = utils.createRequestURI(sipData.getRequestContact());
				logger.debug(PC2LogCategory.SIP, subCat, 
				"SIPManufacturer using Request's Contact for Request-URI");
			}
			
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
			ListIterator<Header> iter = req.getHeaders(ViaHeader.NAME);			
			ArrayList<ViaHeader> viaHeaders = null;
			ToHeader toHeader = null;
			FromHeader fromHeader = null;
			if (sipData.getSSInitiated()) {
				if ((rte.srcNE.equals(SettingConstants.DUT) ||
						rte.srcNE.startsWith(SettingConstants.UE))
						&& rte.srcDeviceType.equals(SettingConstants.UE)) {

					
					viaHeaders = new ArrayList<ViaHeader>();
					while (iter.hasNext()) {
						ViaHeader vh = (ViaHeader)iter.next();
						String branch = utils.generateBranch(null);
						vh.setBranch(branch);
						viaHeaders.add(vh);
					}
				} 
				else {
					viaHeaders = buildViaHeader(rte.src, rte.target, nes,
							null, rte.transport.toString());
				}
				toHeader = (ToHeader)req.getHeader(ToHeader.NAME);
				toHeader.setTag(sipData.getToTag());
				fromHeader = (FromHeader)req.getHeader(FromHeader.NAME);
				// Use the response Contact information for the Request-URI
				// if we started the dialog
				uri = utils.createRequestURI(sipData.getResponseContact());
			} 
//			else
//			if (sipData.getSSInitiated()) {
//
//				
//				toHeader = (ToHeader)req.getHeader(ToHeader.NAME);
//				toHeader.setTag(sipData.getToTag());
//				fromHeader = (FromHeader)req.getHeader(FromHeader.NAME);
//				// Use the response Contact information for the Request-URI
//				// if we started the dialog
//				uri = utils.createRequestURI(sipData.getResponseContact());
//			}
			else {
				if (rte.dest != null && rte.src != null) {
					viaHeaders = buildViaHeader(rte.src, rte.dest, nes, null, rte.transport.toString());
					ViaHeader srcUE = utils.createViaHeader(rte.src, null, 
							rte.transport.toString(), true);
					if (!(rte.srcNE.equals("DUT") && 
							rte.src.getProperty(SettingConstants.DEVICE_TYPE).equals("UE"))) {
						srcUE.setParameter("received", rte.localAddress);
						srcUE.setParameter("rport", ((Integer)rte.localPort).toString());
					}
					viaHeaders.add(srcUE);
				}
				toHeader = utils.createToHeader((FromHeader)req.getHeader(FromHeader.NAME));
				fromHeader = utils.createFromHeader((ToHeader)req.getHeader(ToHeader.NAME));
				
			}
			int hops = maxHops - viaHeaders.size();
//			 Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						uri, // reqURI,
						method,
						(CallIdHeader)req.getHeader(CallIdHeader.NAME),
						cSeqHeader, 
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
			
			// If the target is an AS we need to add the Route header with
			// the SCSCF and the AS
			if (rte.target.getProperty(SettingConstants.DEVICE_TYPE).equals("AS")) {
				 insertRoutingForAS(request, rte, nes);
			}
			// Only include the Route header if a UE is sending the BYE and the
			// target of the message is not a UE.
			else if ( (rte.srcNE.startsWith("UE") || 
					 rte.src.getProperty(SettingConstants.DEVICE_TYPE).equals("UE")) && 
					 !(rte.targetNE.startsWith("UE") || rte.targetNE.startsWith("DUT"))) {
				MsgQueue q = MsgQueue.getInstance();
				MsgEvent me = q.find(sipData.getListener().getFsmUID(), 
						sipData.getFinalResponse() + "-INVITE", 
						MsgQueue.LAST, sipData.getListener().getCurrentMsgIndex());
				if (me != null && me instanceof SIPMsg) {
					SIPMsg sm = (SIPMsg)me;
					Response resp = sm.getResponse();
					if (resp != null) {
						iter = resp.getHeaders(RecordRouteHeader.NAME);
						utils.addRouteFromRecordRoute(iter, request);
//						LinkedList<RouteHeader> rtes = new LinkedList<RouteHeader>();
//						while (iter.hasNext()) {
//							RecordRouteHeader rrh = (RecordRouteHeader)iter.next();
//							RouteHeader rh = utils.createRouteHeader(rrh);
//							rtes.addFirst(rh);
//						}
//						iter = rtes.listIterator();
//						while (iter.hasNext()) {
//							request.addHeader((RouteHeader)iter.next());
//						}
					}
				}
			}

			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
	}
	

	
	/**
	 * Creates a Cancel message based upon the Request message parameter. It uses
	 * the target and source parameters for constructing headers in the message.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param sipData - sip information about this dialog.
	 * @param request - the original request (e.g. INVITE) message to CANCEL.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - a SIP Cancel message
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	public Request buildCancel(Send send, SIPRoute rte, PC2SipData sipData, Request req, 
			NetworkElements nes) throws ParseException, InvalidArgumentException {
		String method = Request.CANCEL;

		//URI uri = utils.createRequestURI(sipData.getRequestContact());
		// If we have received a response from the peer UE, use their contact as the uri
		// in the Request-URI, if not we need to use the value sent in the INVITE
		String contact = sipData.getResponseContact();
		URI uri = null;
		if (contact != null)
			uri = utils.createRequestURI(contact);
		else 
			uri = req.getRequestURI();
		
		ListIterator<Header> iter = req.getHeaders(ViaHeader.NAME);
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		while (iter.hasNext()) {
			viaHeaders.add((ViaHeader)iter.next());
		}
		
		ToHeader toHeader = (ToHeader)req.getHeader("To");
		if (sipData.getToTag() != null)
			toHeader.setTag(sipData.getToTag());
		CSeqHeader cSeqHeader = utils.createCSeqHeader(
				((CSeqHeader)req.getHeader(CSeqHeader.NAME)).getSequenceNumber(), 
				method);
		
		
		// Create the request.
		Request request =
			messageFactory.createRequest(
					uri, // (URI)req.getRequestURI(),
					method,
					(CallIdHeader)req.getHeader(CallIdHeader.NAME),
					cSeqHeader, 
					(FromHeader)req.getHeader(FromHeader.NAME),
					toHeader,
					viaHeaders,
					(MaxForwardsHeader)req.getHeader(MaxForwardsHeader.NAME));
		
		// If the target is an AS we need to add the Route header with
		// the SCSCF and the AS
		if (rte.target.getProperty(SettingConstants.DEVICE_TYPE).equals("AS")) {
			 insertRoutingForAS(request, rte, nes);
		}
		else {
			iter = req.getHeaders(RouteHeader.NAME);
			while (iter.hasNext()) {
				request.addHeader(iter.next());
			}
		}
		return request;
	}
	
	/**
	 * Creates a new SIP Invite Request message for the source argument
	 * to be sent to the target argument.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - SIP Invite message
	 * @throws IllegalStateException
	 */
	public Request buildInvite(Send send, SIPRoute rte, NetworkElements nes) throws IllegalStateException {

		if (rte.dest == null || rte.src == null) {
			String msg = "A properties file has not been properly loaded. dest=" 
					+ rte.destNE +
					" source=" + rte.srcNE;
			throw new IllegalStateException(msg);
		}
		
		try {
			
			String method = Request.INVITE;

			// create >From Header
			String fromTag = SIPDistributor.createTag(); 
			FromHeader fromHeader = utils.createFromHeader(rte.src, rte.localPort, fromTag);

			
			// create To Header
			ToHeader toHeader = utils.createToHeader(rte.dest, null, false, rte.peerPort);

			// create Request URI
			// GLH ADDR
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			//String addrType = "IP"; //  platform.getProperty(SettingConstants.ADDR_FORMAT);
			String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
			// PCPCSII-75 - see if we can find a value in the contactHeader table
			// to use in the RequestURI of the INVITE
			String phoneNum = rte.dest.getProperty(SettingConstants.PHONE_NUMBER + "1");
			String userName = rte.dest.getProperty(SettingConstants.USER_NAME);
			SipURI requestURI = null;
			if (phoneNum != null && phoneNum.equals(userName)) {
				String contact = PC2Models.getContact(phoneNum);
				if (contact == null) {
					logger.info(PC2LogCategory.SIP, subCat,
							"contact information could not be found for phoneNum, using configuration information");
					requestURI = utils.createRequestURI(rte.dest, rte.peerPort, addrType);
				}
				else 
					requestURI = utils.createRequestSipURI(contact);
			}
			else
				requestURI = utils.createRequestURI(rte.dest, rte.peerPort, addrType);
			
//			 Create a new Cseq header
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
			
			//	Create and add via headers
			// The way to add is from the source to the dest
			ArrayList<ViaHeader> viaHeaders = buildViaHeader(rte.src, rte.target, nes,
					null, rte.transport.toString());
			int hops = viaHeaders.size();

			ViaHeader srcUE = utils.createViaHeader(rte.src, null, 
					rte.transport.toString(), true);
			if (hops > 0)
				srcUE.setParameter("received", rte.localAddress);
			boolean includeRouteHeader = false;
			if ((rte.srcNE.equals("DUT") &&
			      rte.src.getProperty(SettingConstants.DEVICE_TYPE).equals("UE")) ||
				hops == 0) {
				srcUE.setParameter("rport", null);
				if (rte.dest.getProperty(SettingConstants.DEVICE_TYPE).equals("UE"))
					includeRouteHeader = true;
			}
			else
				srcUE.setParameter("rport", ((Integer)rte.localPort).toString());
			
			viaHeaders.add(srcUE);
			hops++;
			
			// Create ContentTypeHeader
			ContentTypeHeader contentTypeHeader = utils.createContentTypeHeader(null, null);
			
			// Create a new CallId header
			CallIdHeader callIdHeader = utils.createCallIdHeader(rte.dest, 
					rte.provider);

			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						requestURI,
						method,
						callIdHeader,
						cSeqHeader,
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
			// Create contact headers
			ContactHeader contactHeader = utils.createContactHeader(rte.src, 
					rte.localAddress, rte.localPort, true);
			request.addHeader(contactHeader);
			
			// Include the route header or record-route header depending if the
			// script is a DUT UE or the CN is sending the message
			if (includeRouteHeader) {
				String srcPCSCF = rte.src.getProperty(SettingConstants.PCSCF);
				if (srcPCSCF != null) {
					if (srcPCSCF.equals("DUT"))
						srcPCSCF = SettingConstants.PCSCF + "0";
					Properties myP = SystemSettings.getSettings(srcPCSCF);
					// String realP = myP.getProperty(SettingConstants.SIMULATED);
					String srcSCSCF = myP.getProperty(SettingConstants.SCSCF);
					if (srcSCSCF != null) {
						if (srcSCSCF.equals("DUT"))
							srcSCSCF = SettingConstants.SCSCF + "0";
						Properties myS = SystemSettings.getSettings(srcSCSCF);
						String transport = null;
						if (SystemSettings.useTransportParameter())
							transport = rte.transport.toString();
						RouteHeader rh = utils.createRouteHeader(myP, false, 
								rte.localPort, transport);
						if (rh != null) 
							request.addHeader(rh);
						rh = utils.createRouteHeader(myS, false, 
								rte.localPort, transport);
						if (rh != null) 
							request.addHeader(rh);
					}
				}
//				 Add P-Access-Network-Info Header
				Header pAccess = utils.createPAccessNetworkInfoHeader(rte.src);
				request.addHeader(pAccess);
			}
			else {
				// If the target is an AS we need to add the Route header with
				// the SCSCF and the AS
				if (rte.target.getProperty(SettingConstants.DEVICE_TYPE).equals("AS")) {
					insertRoutingForAS(request, rte, nes);
					
					Header pAssert = utils.createPAssertedIdentityHeader(rte.src);
					if (pAssert != null)
						request.addHeader(pAssert);
					
					Header pCharging = utils.createPChargingVectorHeader(rte.src, rte.dest);
					if (pCharging != null)
						request.addHeader(pCharging);
					
					Header pChargingFunc = utils.createPChargingFunctionAddressHeader(rte.src, rte.dest);
					if (pChargingFunc != null)
						request.addHeader(pChargingFunc);
				}
				else {
					ArrayList<RecordRouteHeader> rrHeaders = buildRecordRoute(rte.src, rte.target, 
							nes, rte.transport.toString());
					ListIterator<RecordRouteHeader> iter = rrHeaders.listIterator();
					while (iter.hasNext()) {
						Header rr = iter.next();
						//if (req.getMethod().equalsIgnoreCase("INVITE"))
						request.addHeader(rr);
					}
				}
			}
			
			// create the offer
			// Use the SESSION_ID for the value of the SessionVersion when
			// creating a new sdp body. The version in the PC2SipData will
			// be set in the distributor.
			String sdpData = utils.createSDPData(rte.src, rte.srcNE, true, 
					"none", false, false, PC2SipData.SESSION_ID,
					PC2SipData.SESSION_ID);
			
			// Add the offer and the content type and length headers
			request.setContent(sdpData, contentTypeHeader);

			SupportedHeader sh = utils.createSupportHeader("tdialog");
			if (sh != null) 
				request.addHeader(sh);
			
			RequireHeader rh = utils.createRequireHeader(null);
			if (rh != null)
				request.addHeader(rh);
			
			Header pcpi = utils.createPCalledPartyID(rte.dest);
			if (pcpi != null) 
				request.addHeader(pcpi);
			
			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
		
	}
	

	/**
	 * Creates a new SIP Message Request message for the source argument
	 * to be sent to the target argument.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the routing information for this message.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - SIP Invite message
	 * @throws IllegalStateException
	 */
	public Request buildMessage(Send send, SIPRoute rte, NetworkElements nes) 
		throws IllegalStateException {
		if (rte.dest == null || rte.src == null) {
			String msg = "A properties file has not been properly loaded. dest=" 
					+ rte.destNE +
					" source=" + rte.srcNE;
			throw new IllegalStateException(msg);
		}
		
		try {
			
			String method = Request.MESSAGE;
			// create From Header
			String fromTag = SIPDistributor.createTag(); 
			FromHeader fromHeader = utils.createFromHeader(rte.src, rte.localPort, fromTag);
			
			// create To Header
			ToHeader toHeader = utils.createToHeader(rte.dest, null, false, rte.peerPort);
			String addrType = "IP"; 
			SipURI requestURI = utils.createRequestURI(rte.dest, rte.peerPort, addrType);
			
//			 Create a new Cseq header
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
			
			//	Create and add via headers
			// The way to add is from the source to the dest
			ArrayList<ViaHeader> viaHeaders = buildViaHeader(rte.src, rte.target, nes,
					null, rte.transport.toString());
			int hops = viaHeaders.size();

			ViaHeader srcUE = utils.createViaHeader(rte.src, null, 
					rte.transport.toString(), true);
			if (hops > 0)
				srcUE.setParameter("received", rte.localAddress);
			boolean includeRouteHeader = false;
			if ((rte.srcNE.equals("DUT") && 
					rte.src.getProperty(SettingConstants.DEVICE_TYPE).equals("UE")) ||
				hops == 0) {
				srcUE.setParameter("rport", null);
				includeRouteHeader = true;
			}
			else
				srcUE.setParameter("rport", ((Integer)rte.localPort).toString());
			viaHeaders.add(srcUE);
			hops++;
			
			// Create ContentTypeHeader
			ContentTypeHeader contentTypeHeader = utils.createContentTypeHeader("text", 
					"plain");
			
			// Create a new CallId header
			CallIdHeader callIdHeader = utils.createCallIdHeader(rte.dest, 
					rte.provider);

			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						requestURI,
						method,
						callIdHeader,
						cSeqHeader,
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
			
			// Include the route header or record-route header depending if the
			// script is a DUT UE or the CN is sending the message
			if (includeRouteHeader) {
				String srcPCSCF = rte.src.getProperty(SettingConstants.PCSCF);
				if (srcPCSCF != null) {
					if (srcPCSCF.equals("DUT"))
						srcPCSCF = SettingConstants.PCSCF + "0";
					Properties myP = SystemSettings.getSettings(srcPCSCF);
					String srcSCSCF = myP.getProperty(SettingConstants.SCSCF);
					if (srcSCSCF != null) {
						if (srcSCSCF.equals("DUT"))
							srcSCSCF = SettingConstants.SCSCF + "0";
						Properties myS = SystemSettings.getSettings(srcSCSCF);
						String transport = null;
						if (SystemSettings.useTransportParameter())
							transport = rte.transport.toString();
						RouteHeader rh = utils.createRouteHeader(myP, false, 
								rte.localPort, transport);
						if (rh != null) 
							request.addHeader(rh);
						rh = utils.createRouteHeader(myS, false, 
								rte.localPort, transport);
						if (rh != null) 
							request.addHeader(rh);
					}
				}
//				 Add P-Access-Network-Info Header
				Header pAccess = utils.createPAccessNetworkInfoHeader(rte.src);
				request.addHeader(pAccess);
			}
			request.setContent("", contentTypeHeader);
					
			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
		
	}
	
	/**
	 * Creates a NOTIFY message for a Subscribe/Refer response for the dialog
	 * defined by the Request parameter. The target and source parameters are
	 * used to create the headers of the message.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param sipData - the data being retained by the distributor about this dialog.
	 * @param contact -
	 * @param fsmUID - The unique identifier of the FSM sending the NOTIFY.
	 * @param notifyToReferCount - the current Notify to Refer count. Allows the system
	 * 		to determine what body to include in the NOTIFY message.
	 * @param regId - Used in the construction of the regInfo-xml body
	 * @param regVersion - Used in the construction of the regInfo-xml body
	 * @param req - the original SUBSCRIBE message that the NOTIFY message is in response to.
	 * @param nes - the network elements associated with the invoking FSM.
	 * @param utilEvent - the utility Event that may be need to respond to a NOTIFY.
	 * @param eventType - the type of event the request was so that we can construct the 
	 * 		correct default body.
	 * @param curMsgIndex - the index of the current event being processed
	 *  
	 * @return - A SIP NOTIFY message
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	public Request buildNotify(Send send, SIPRoute rte, PC2SipData sipData,
			String contact, int fsmUID, 
			int notifyToReferCount, Byte regId, int regVersion, Request req, 
			NetworkElements nes, UtilityEvent utilEvent, 
			String eventType, int curMsgIndex) 
		throws ParseException, InvalidArgumentException {
		String method = Request.NOTIFY;
		// Get the method of the request that started this dialog
		boolean subscribe = false;
		boolean refer = false;
		boolean standAlone = false;
		if (req == null) 
			standAlone = true;
		else if (req.getMethod().equals(SIPConstants.SUBSCRIBE))
				subscribe = true;
		else if (req.getMethod().equals(SIPConstants.REFER))
			refer = true;

		URI uri = null;
		if ((refer || subscribe) && contact != null)
			uri = utils.createRequestURI(contact);
		else if (standAlone || (refer && contact == null)) {
			// GLH ADDR
			//Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			// String addrType = "IP"; //  platform.getProperty(SettingConstants.ADDR_FORMAT);
			//String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
			uri = utils.createRequestURI(rte.dest, rte.peerPort, null);
		}

		CSeqHeader cSeqHeader = utils.createCSeqHeader(method);

		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		boolean includeRouteHeader = false;
		if (rte.src != null && rte.dest != null) {
			// Get the name of the network label for the PCSCF of the target
			if (rte.srcNE.equals("DUT") && 
					rte.src.getProperty(SettingConstants.DEVICE_TYPE).equals("UE")) {
				ViaHeader destP = utils.createViaHeader(rte.src, 
						null, rte.transport.toString(), true);
				destP.setParameter("rport", null);
				destP.removeParameter("keep-stun");
				viaHeaders.add(destP);
				includeRouteHeader = true;
			}
			else if (rte.srcNE.startsWith("UE") && refer) {
				String transport = rte.transport.toString();
				viaHeaders = buildViaHeader(rte.src, rte.dest, nes, null, transport);
				ViaHeader srcUE = utils.createViaHeader(rte.src, null, 
						transport, true);
				srcUE.setParameter("received", rte.peerAddress);
				srcUE.setParameter("rport", ((Integer)rte.peerPort).toString());
			}
			else if (rte.destNE.equals("DUT")) {
				String pcscf = rte.dest.getProperty(SettingConstants.PCSCF);
				ViaHeader destP = utils.createViaHeader(SystemSettings.getSettings(pcscf), 
						null, rte.transport.toString(), true);
				viaHeaders.add(destP);
			}
			else if (subscribe && 
					(eventType.equals(SettingConstants.SUBSCRIBE_TYPE_REG) ||
							eventType.equals(SettingConstants.SUBSCRIBE_TYPE_MSG_SUMMARY))) {
				// Add the P and S for the device
				String pcscf = rte.dest.getProperty(SettingConstants.PCSCF);
				Properties p = SystemSettings.getSettings(pcscf);
				if (p != null) {
					ViaHeader destP = utils.createViaHeader(p, null, rte.transport.toString(), true);
					if (destP != null) {
						viaHeaders.add(destP);
						String scscf = p.getProperty(SettingConstants.SCSCF);
						if (scscf != null) {
							ViaHeader destS = utils.createViaHeader(SystemSettings.getSettings(scscf), 
									null, rte.transport.toString(), true);
							if (destS != null)
								viaHeaders.add(destS);
						}
					}
				}
			}
			
		}	
				
		FromHeader fromHeader = null;
		ToHeader toHeader = null;
		CallIdHeader callIdHeader = null;
		
		if (subscribe || refer) {
			FromHeader origFrom = (FromHeader)req.getHeader(FromHeader.NAME);
			ToHeader origTo = (ToHeader)req.getHeader(ToHeader.NAME);
			fromHeader = utils.createFromHeader(origTo);
			if (fromHeader.getTag() == null) {
				// See if the information is set in the sipData
				String toTag = sipData.getToTag();
				if (toTag != null)
					fromHeader.setTag(toTag);
			}
			toHeader = utils.createToHeader(origFrom);
			callIdHeader = (CallIdHeader)req.getHeader(CallIdHeader.NAME);
		}
		else if (standAlone) {
			logger.info (PC2LogCategory.SIP, subCat,
					"SIPManufacturer creating From, To and Call-Id headers for standalong NOTIFY message.");
			String fromTag = SIPDistributor.createTag(); 
			fromHeader = utils.createFromHeader(rte.src, rte.localPort, fromTag);
			
			// Since we are creating a NOTIFY without any associated request,
			// make up the to tag
			String toTag = SIPDistributor.createTag();
			// create To Header
			toHeader = utils.createToHeader(rte.dest, toTag, false, rte.peerPort);
//			 Create a new CallId header
			callIdHeader = utils.createCallIdHeader(rte.dest, 
					rte.provider);
		}
		
		int hops = viaHeaders.size();	
//		 Create a new MaxForwardsHeader
		MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
		
		// Create the request.
		Request request =
			messageFactory.createRequest(
					uri, // (URI)req.getRequestURI(),
					method,
					callIdHeader,
					cSeqHeader, 
					fromHeader,
					toHeader,
					viaHeaders,
					maxForwards);
		
		if (subscribe && utilEvent != null && utilEvent.getMessage() != null) {
			String contentType = null;
			String subContentType = null;
			String effectiveBy = null;
			String content = null;
			ListIterator<UtilityAttribute> iter = utilEvent.getMessage().getAttributes();
			while (iter.hasNext()) {
				UtilityAttribute ua = iter.next();
				if (ua.getName().equals("effectiveBy")) {
					effectiveBy = ua.getValue();
				}
				else if (ua.getName().equals("contentType")) {
					String txt = ua.getValue();
					int slash = txt.indexOf("/");
					contentType = txt.substring(0, slash);
					subContentType = txt.substring(slash+1, txt.length());
				}
				else if (ua.getName().equals("content")) {
					content = ua.getValue();
				}
			}
			
			EventHeader origEventHdr = (EventHeader)req.getHeader(EventHeader.NAME);
			EventHeader eh = utils.createEventHeader(origEventHdr.getEventType());
			if (effectiveBy != null)
				eh.setParameter("effective-by", effectiveBy);
			
			MimeVersionHeader mvh = utils.createMimeVersionHeader();


			ContentTypeHeader cth = utils.createContentTypeHeader(contentType, subContentType);
			request.addHeader(eh);
			request.addHeader(mvh);
			request.setContent(content, cth);
		}
		else if (refer) {
			ContentTypeHeader cth = utils.createContentTypeHeader("message", "sipfrag");
			cth.setParameter("version", "2.0");

			EventHeader eh = utils.createEventHeader("refer");
//			EventHeader eh = (EventHeader)req.getHeader(EventHeader.NAME);
			if (eh != null)
				request.addHeader(eh);
			
			SubscriptionStateHeader ss = utils.createSubscriptionStateHeader("active", 600000);
			if (ss != null)
				request.addHeader(ss);
			
			MsgQueue q = MsgQueue.getInstance();
			MsgEvent referEvent = q.find(fsmUID,"REFER", 
					MsgQueue.LAST, curMsgIndex);
			MsgEvent inviteRespEvent = q.find(fsmUID, "xxx-INVITE", 
					MsgQueue.LAST, curMsgIndex);
			if (referEvent.getMsgQueueIndex() < inviteRespEvent.getMsgQueueIndex()) {
				if (inviteRespEvent instanceof SIPMsg) {
					SIPResponse inviteResp = (SIPResponse)((SIPMsg)inviteRespEvent).getResponse();
					if (inviteResp != null) {
						String message = inviteResp.getFirstLine();
						request.setContent(message, cth);
					}
				}
			}
			else {
				String message = null;
				if (notifyToReferCount == 0) {
					message = "SIP/2.0 100 Trying\r\n";
					
				}
				else if (notifyToReferCount == 1) {
					message = "SIP/2.0 180 Ringing\r\n";
					
				}
				else if (notifyToReferCount == 2) {
					message = "SIP/2.0 200 OK\r\n";
					
				}
				if (message != null)
					request.setContent(message, cth);
			}
			
			if (includeRouteHeader) {
				ListIterator<Header> iter = req.getHeaders(RecordRouteHeader.NAME);
			    while (iter.hasNext()) {
			    	RecordRouteHeader rr = (RecordRouteHeader)iter.next();
			    	String transport = null;
					if (SystemSettings.useTransportParameter())
						transport = rte.transport.toString();
					RouteHeader rh = utils.createRouteHeader(rr, transport);
					if (rh != null) 
						request.addHeader(rh);
					
			    }
			}
		}
		else if (subscribe) {
			EventHeader eh = (EventHeader)req.getHeader(EventHeader.NAME);
			if (eh != null) {
				String event = eh.getEventType();
				if (event.equals(SettingConstants.SUBSCRIBE_TYPE_REG)) {
					MsgEvent register = q.find(fsmUID, SIPConstants.REGISTER, 
							MsgQueue.LAST, curMsgIndex); 
					if (register != null) {
						Request regRequest = ((SIPMsg)register).getRequest();
						if (regRequest != null) {
							ContactHeader regContact = (ContactHeader)regRequest.getHeader(ContactHeader.NAME);
							if (regContact != null) {
								URI contactURI = regContact.getAddress().getURI();
								if (contactURI != null) {
									logger.info (PC2LogCategory.SIP, subCat,
											"SIPManufacturer contactURI=" + contactURI.toString());
									String regInfoBody = utils.createRegInfoBody(rte.dest, regId, 
											regVersion, contactURI.toString());
									
									
									ContentTypeHeader cth = utils.createContentTypeHeader("application", "reginfo+xml");
									request.setContent(regInfoBody, cth);
								}
							}
						}
					}
					EventHeader newEventHdr = utils.createEventHeader(event);
					request.addHeader(newEventHdr);
					SubscriptionStateHeader ss = utils.createSubscriptionStateHeader("active", 599999);
					if (ss != null)
						request.addHeader(ss);
					
				}
				else if (event.equals(SettingConstants.SUBSCRIBE_TYPE_MSG_SUMMARY)) {
					MsgEvent register = q.find(fsmUID, SIPConstants.REGISTER, 
							MsgQueue.LAST, curMsgIndex); 
					if (register != null) {
						Request regRequest = ((SIPMsg)register).getRequest();
						if (regRequest != null) {
							ContactHeader regContact = (ContactHeader)regRequest.getHeader(ContactHeader.NAME);
							if (regContact != null) {
								URI contactURI = regContact.getAddress().getURI();
								if (contactURI != null) {
									logger.info (PC2LogCategory.SIP, subCat,
											"SIPManufacturer contactURI=" + contactURI.toString());
									String mwiBody = utils.createMWIBody(rte.dest,  contactURI.toString());
									
									
									ContentTypeHeader cth = utils.createContentTypeHeader("application", "simple-message-summary");
									request.setContent(mwiBody, cth);
								}
							}
						}
					}
					EventHeader newEventHdr = utils.createEventHeader("message-summary");
					request.addHeader(newEventHdr);
					// Get the Expires header to determine whether the subscription
					// is active or inactive
					ExpiresHeader exh = (ExpiresHeader)req.getHeader(ExpiresHeader.NAME);
					if (exh != null) {
						int exp = exh.getExpires();
						if (exp > 0) {
							// Then this should be an initial condition
							Header ssh= utils.createSubscriptionStateHeader("active", 599999);
							if (ssh != null)
								request.addHeader(ssh);
							request.addHeader(exh);
						}
						else {
							Header ssh= utils.createSubscriptionStateHeader("inactive", -1);
							if (ssh != null)
								request.addHeader(ssh);
						}
					}
					else  {
						// Then this should be a remove condtion
						Header ssh= utils.createSubscriptionStateHeader("inactive", -1);
						if (ssh != null)
							request.addHeader(ssh);
					}
				}
				
				else if (event.equals(SettingConstants.SUBSCRIBE_TYPE_PRESENCE)) {
					// Get the Expires header to determine whether the subscription
					// is active or inactive
					ExpiresHeader exh = (ExpiresHeader)req.getHeader(ExpiresHeader.NAME);
					if (exh != null) {
						int exp = exh.getExpires();
						if (exp > 0) {
							// Then this should be an initial condition
							Header ssh= utils.createSubscriptionStateHeader("active", -1);
							if (ssh != null)
								request.addHeader(ssh);
							request.addHeader(exh);
						}
						else {
							Header ssh= utils.createSubscriptionStateHeader("inactive", -1);
							if (ssh != null)
								request.addHeader(ssh);
						}
					}
					else  {
						// Then this should be a remove condtion
						Header ssh= utils.createSubscriptionStateHeader("inactive", -1);
						if (ssh != null)
							request.addHeader(ssh);
					}
						
					
					
					String body = PresenceServer.createPresenceBody(rte.dest);
					ContentTypeHeader cth = utils.createContentTypeHeader("application", "pidf+xml");
					request.setContent(body, cth);
				}
				else if (event.equals(SettingConstants.SUBSCRIBE_TYPE_UA_PROFILE)) {
					ExpiresHeader exh = (ExpiresHeader)req.getHeader(ExpiresHeader.NAME);
					if (exh != null) {
						int exp = exh.getExpires();
						if (exp > 0) {
							// Then this should be an initial condition
							Header ssh= utils.createSubscriptionStateHeader("active", -1);
							if (ssh != null)
								request.addHeader(ssh);
							request.addHeader(exh);
						}
						else {
							Header ssh= utils.createSubscriptionStateHeader("inactive", -1);
							if (ssh != null)
								request.addHeader(ssh);
						}
					}
					else  {
						// Then this should be a remove condtion
						Header ssh= utils.createSubscriptionStateHeader("inactive", -1);
						if (ssh != null)
							request.addHeader(ssh);
					}
					
					String body = utils.createUAProfileBody(rte.dest);
					ContentTypeHeader cth = utils.createContentTypeHeader("application", "xml");
					request.setContent(body, cth);
					
					EventHeader origEventHdr = (EventHeader)req.getHeader(EventHeader.NAME);
					if (origEventHdr != null)
						request.addHeader(origEventHdr);
				}
				else if (event.equals(SettingConstants.SUBSCRIBE_TYPE_DIALOG)) {
//					logger.warn(PC2LogCategory.SIP, subCat, 
//					"SIPManufacture doesn't support construction of the body for the dialog event yet.");
					String body = utils.createDialogEventProfile(rte.src, rte.dest);
					ContentTypeHeader cth = utils.createContentTypeHeader("application", "dialog-info+xml");
					request.setContent(body, cth);
					
					SubscriptionStateHeader ss = utils.createSubscriptionStateHeader("active", 600000);
					if (ss != null)
						request.addHeader(ss);
					
					EventHeader newEventHdr = utils.createEventHeader("dialog");
					request.addHeader(newEventHdr);
					
					ListIterator<Header> iter = req.getHeaders(RecordRouteHeader.NAME);
					while (iter.hasNext()) {
						RecordRouteHeader rr = (RecordRouteHeader)iter.next();
						String transport = rr.getParameter("transport");
						RouteHeader rh = utils.createRouteHeader(rr, transport);
						if (rh != null) 
							request.addHeader(rh);
					}
				}
			}
		}
		else if (standAlone) {
			if (eventType != null) {
				if (eventType.equals(SettingConstants.SUBSCRIBE_TYPE_REG)) {
					MsgEvent register = q.find(fsmUID, SIPConstants.REGISTER, 
							MsgQueue.LAST, curMsgIndex); 
					if (register != null) {
						Request regRequest = ((SIPMsg)register).getRequest();
						if (regRequest != null) {
							ContactHeader regContact = (ContactHeader)regRequest.getHeader(ContactHeader.NAME);
							if (regContact != null) {
								URI contactURI = regContact.getAddress().getURI();
								if (contactURI != null) {
									logger.info (PC2LogCategory.SIP, subCat,
											"SIPManufacturer contactURI=" + contactURI.toString());
									String regInfoBody = utils.createRegInfoBody(rte.dest, regId, regVersion,
											contactURI.toString());
									ContentTypeHeader cth = utils.createContentTypeHeader("application", "reginfo+xml");
									request.setContent(regInfoBody, cth);
								}
							}
						}
					}
				}
				else if (eventType.equals(SettingConstants.SUBSCRIBE_TYPE_PRESENCE)) {
					Header ssh= utils.createSubscriptionStateHeader("active", -1);
					if (ssh != null)
						request.addHeader(ssh);
							
					ExpiresHeader exh = utils.createExpiresHeader(3600);
					if (exh != null)
						request.addHeader(exh);
								
					String body = PresenceServer.createPresenceBody(rte.dest);
					ContentTypeHeader cth = utils.createContentTypeHeader("application", "pidf+xml");
					request.setContent(body, cth);
				}
				else if (eventType.equals("message-summary")) {
					String body = utils.createMessageSummary(rte.src);
					ContentTypeHeader cth = utils.createContentTypeHeader("application", 
							"simple-message-summary");
					request.setContent(body, cth);
				}
				else if (eventType.equals("ua-profile")) {
					String body = utils.createUAProfileBody(rte.src);
					ContentTypeHeader cth = utils.createContentTypeHeader("application", 
							"simple-message-summary");
					request.setContent(body, cth);
				}
				else if (eventType.equals("dialog")) {
					logger.warn(PC2LogCategory.SIP, subCat, 
							"SIPManufacture doesn't support construction of the body for the dialog event yet.");
//					String body = utils.createDialogProfile(rte.src);
//					ContentTypeHeader cth = utils.createContentTypeHeader("application", 
//							"simple-message-summary");
//					request.setContent(body, cth);
				}
			}
		}
		return request;
	}



	/**
	 * Creates a SIP Options message. It uses the target and source information
	 * to create the various headers. This method is intended to send an OPTIONS
	 * message outside of a dialog.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - A SIP OPTIONS message
	 * @throws IllegalStateException
	 */
	public Request buildOptions(Send send, SIPRoute rte,  
			NetworkElements nes) 	throws IllegalStateException {
		if (rte.dest == null || rte.src == null) {
			String msg = "A properties file has not been properly loaded. target=" 
					+ rte.destNE +
					" source=" + rte.srcNE;
			throw new IllegalStateException(msg);
		}
		
		try {
			
			String method = Request.OPTIONS;
			
			// create >From Header
			String fromTag = SIPDistributor.createTag(); 
			FromHeader fromHeader = utils.createFromHeader(rte.src, rte.peerPort, fromTag);
				
			// create To Header
			ToHeader toHeader = utils.createToHeader(rte.dest, null, false, rte.peerPort);
						
			// create Request URI
			//Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			//String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
			SipURI requestURI = utils.createRequestURI(rte.dest, rte.peerPort, null);
			
			// Create a new Cseq header
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);

			ArrayList<ViaHeader> viaHeaders = buildViaHeader(rte.src, rte.dest, nes, null, 
					rte.transport.toString());
			ViaHeader srcUE = utils.createViaHeader(rte.src, null, 
					rte.transport.toString(), true);
			srcUE.setParameter("rport", null);
			viaHeaders.add(srcUE);
			int hops = viaHeaders.size();
			
			// Create ContentTypeHeader
			ContentTypeHeader contentTypeHeader = utils.createContentTypeHeader(null, null);
			
			// Create a new CallId header
			CallIdHeader callIdHeader = utils.createCallIdHeader(rte.dest, rte.provider);
			
			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						requestURI,
						method,
						callIdHeader,
						cSeqHeader,
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
			// Create contact headers
			ContactHeader contactHeader = utils.createContactHeader(rte.src, 
					rte.provider.getListeningPoint().getHost(), rte.peerPort, false);
			request.addHeader(contactHeader);
			
			
			ArrayList<RecordRouteHeader> rrHeaders = buildRecordRoute(rte.src, rte.dest, nes, 
					rte.transport.toString());
			ListIterator<RecordRouteHeader> iter = rrHeaders.listIterator();
			while (iter.hasNext()) {
				Header rr = iter.next();
				request.addHeader(rr);
			}			
			
			SupportedHeader sh = utils.createSupportHeader("tdialog");
			if (sh != null) 
				request.addHeader(sh);
			
			RequireHeader rh = utils.createRequireHeader(null);
			if (rh != null)
				request.addHeader(rh);
			
			// Use the SESSION_ID for the value of the SessionVersion when
			// creating a new sdp body. The version in the PC2SipData will
			// be set in the distributor.
			String sdpData = utils.createSDPData(rte.src, rte.srcNE, 
					false, null, false, false, PC2SipData.SESSION_ID,
					PC2SipData.SESSION_ID);
			
			request.setContent(sdpData, contentTypeHeader);

			if (rte.target.getProperty(SettingConstants.DEVICE_TYPE).equals("AS")) {
				insertRoutingForAS(request, rte, nes);
				
				Header pAssert = utils.createPAssertedIdentityHeader(rte.src);
				if (pAssert != null)
					request.addHeader(pAssert);
				
//				Header pCharging = utils.createPChargingVectorHeader(rte.src, rte.dest);
//				if (pCharging != null)
//					request.addHeader(pCharging);
//				
//				Header pChargingFunc = utils.createPChargingFunctionAddressHeader(rte.src, rte.dest);
//				if (pChargingFunc != null)
//					request.addHeader(pChargingFunc);
			}
			
			if (rte.srcDeviceType.equals(SettingConstants.UE)) {
				String srcPCSCF = rte.src.getProperty(SettingConstants.PCSCF);
				if (srcPCSCF != null) {
					if (srcPCSCF.equals("DUT"))
						srcPCSCF = SettingConstants.PCSCF + "0";
					Properties myP = SystemSettings.getSettings(srcPCSCF);
					// String realP = myP.getProperty(SettingConstants.SIMULATED);
					String srcSCSCF = myP.getProperty(SettingConstants.SCSCF);
					if (srcSCSCF != null) {
						if (srcSCSCF.equals("DUT"))
							srcSCSCF = SettingConstants.SCSCF + "0";
						Properties myS = SystemSettings.getSettings(srcSCSCF);
						String transport = null;
						if (SystemSettings.useTransportParameter())
							transport = rte.transport.toString();
						RouteHeader rteh = utils.createRouteHeader(myP, false, 
								rte.localPort, transport);
						if (rteh != null) 
							request.addHeader(rteh);
						rteh = utils.createRouteHeader(myS, false, 
								rte.localPort, transport);
						if (rteh != null) 
							request.addHeader(rteh);
					}
				}
//				Add P-Access-Network-Info Header
				Header pAccess = utils.createPAccessNetworkInfoHeader(rte.src);
				request.addHeader(pAccess);
			}
			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
		
	}
	
	/**
	 * Creates a SIP Options message. It uses the target and source information
	 * to create the various headers. It uses the toTag from the dialog to construct
	 * the message. This method is intended to send an OPTIONS message from 
	 * within a dialog.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the routing information for this message.
	 * @param sipData - sip information about this dialog.
	 * @param req - the original dialog request when the message is being sent within an existing dialog.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - A SIP OPTIONS message
	 * @throws IllegalStateException
	 */
	public Request buildOptions(Send send, SIPRoute rte, PC2SipData sipData, Request req, 
			NetworkElements nes) throws ParseException, InvalidArgumentException {
		String method = Request.OPTIONS;
		
		URI uri = null;
		if (sipData.getSSInitiated()) {
			uri = utils.createRequestURI(sipData.getResponseContact());
			logger.debug(PC2LogCategory.SIP, subCat, 
			"SIPManufacturer using Response's Contact for Request-URI");
		}
		else {
			uri = utils.createRequestURI(sipData.getRequestContact());
			logger.debug(PC2LogCategory.SIP, subCat, 
			"SIPManufacturer using Request's Contact for Request-URI");
		}
		
		ListIterator<Header> iter = req.getHeaders(ViaHeader.NAME);
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		while (iter.hasNext()) {
			viaHeaders.add((ViaHeader)iter.next());
		}
	
		ToHeader toHeader = (ToHeader)req.getHeader("To");
		if (sipData.getToTag() != null)
			toHeader.setTag(sipData.getToTag());
		CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
		
		AcceptHeader accept = utils.createAcceptHeader();
		// Create the request.
		Request request =
			messageFactory.createRequest(
					uri, // (URI)req.getRequestURI(),
					method,
					(CallIdHeader)req.getHeader(CallIdHeader.NAME),
					cSeqHeader, 
					(FromHeader)req.getHeader(FromHeader.NAME),
					toHeader,
					viaHeaders,
					(MaxForwardsHeader)req.getHeader(MaxForwardsHeader.NAME));
		request.addHeader(accept);
		return request;
	}

	/**
	 * Creates a PRACK message for a non-100 provisional response for the dialog
	 * defined by the Request parameter. The target and source parameters are
	 * used to create the headers of the message.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the routing information for this message.
	 * @param sipData - sip information about this dialog.
	 * @param req - the original INVITE message that the PRACK message is in response to.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - A SIP PRACK message
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	public Request buildPrack(Send send, SIPRoute rte, PC2SipData sipData, 
			Request req, NetworkElements nes) 
		throws ParseException, InvalidArgumentException {
		
		String method = Request.PRACK;
		ListIterator<Header> iter = req.getHeaders(ViaHeader.NAME);
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		while (iter.hasNext()) {
			viaHeaders.add((ViaHeader)iter.next());
		}
		
		URI uri = null;
		if (sipData.getSSInitiated() &&  sipData.getResponseContact() != null) {
			uri = utils.createRequestURI(sipData.getResponseContact());
			logger.debug(PC2LogCategory.SIP, subCat, 
			"SIPManufacturer using Response's Contact for Request-URI");
		}
		else {
			uri = utils.createRequestURI(sipData.getRequestContact());
			logger.debug(PC2LogCategory.SIP, subCat, 
			"SIPManufacturer using Request's Contact for Request-URI");
		}
		
		ToHeader toHeader = (ToHeader)req.getHeader("To");
		if (sipData.getToTag() != null)
			toHeader.setTag(sipData.getToTag());
		CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
		
		// Create the request.
		Request request =
			messageFactory.createRequest(
					uri, // (URI)req.getRequestURI(),
					method,
					(CallIdHeader)req.getHeader(CallIdHeader.NAME),
					cSeqHeader, 
					(FromHeader)req.getHeader(FromHeader.NAME),
					toHeader,
					viaHeaders,
					(MaxForwardsHeader)req.getHeader(MaxForwardsHeader.NAME));
		
		
		
		// Create the RAck header from the original request message
		MsgEvent respEvent = q.find(sipData.getListener().getFsmUID(), 
				"18x-Invite", MsgQueue.LAST, sipData.getListener().getCurrentMsgIndex());
		if (respEvent != null && respEvent instanceof SIPMsg ) {
			SIPMsg respMsg = (SIPMsg)respEvent;
			Response resp = respMsg.getResponse();
			if (resp != null) {
				RSeqHeader rSeq = (RSeqHeader)resp.getHeader(RSeqHeader.NAME);
				if (rSeq != null) {
					Header extensionHeader =
						utils.createRAckHeader(rSeq, 
								(CSeqHeader)req.getHeader(CSeqHeader.NAME));		
					request.addHeader(extensionHeader);
				}
				else {
					logger.error(PC2LogCategory.SIP, subCat, 
							"Couldn't create RACK header for PRACK message because the last 18x didn't contain a RSeq header.");
				}
			}
			else {
				logger.error(PC2LogCategory.SIP, subCat, 
						"Couldn't create RSeq header for PRACK message because the last 18x didn't have any response data.");
			}
			// Next if the source of the message is the DUT and it is a UE include
			// the Route Header
			if (rte.srcNE.equals("DUT") && 
					rte.srcDeviceType.equals(SettingConstants.UE)) {
				ListIterator<Header> rrIter = resp.getHeaders(RecordRouteHeader.NAME);
				utils.addRouteFromRecordRoute(rrIter, request);
			}
			
			
		}
		else {
			logger.error(PC2LogCategory.SIP, subCat, 
					"Couldn't create RSeq header for PRACK message because couldn't find last 18x-Invite.");
		}

		// If the target is an AS we need to add the Route header with
		// the SCSCF and the AS
		if (rte.target.getProperty(SettingConstants.DEVICE_TYPE).equals("AS")) {
			 insertRoutingForAS(request, rte, nes);
		}


		return request;
	}
	
	/**
	 * Creates a PUBLISH message outside of a dialog. It creates the message to be sent when
	 * reporting session metrics outside of a dialog. The target and source parameters are
	 * used to create the headers of the message.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the routing information for this message.
	 * @param req - the original INVITE message that the PUBLISH message is being generated for.
	 * @param nes - the network elements associated with the invoking FSM.
	 * @param body - the type of body to create in the request.
	 * 
	 * @return - A SIP PUBLISH message
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	public Request buildPublish(Send send, SIPRoute rte, Request req, 
			NetworkElements nes, String body) throws ParseException, 
			InvalidArgumentException {

		String method = Request.PUBLISH;
		if (rte.dest == null || rte.src == null) {
			String msg = "A properties file has not been properly loaded. target=" + 
					rte.targetNE +
					" source=" + rte.srcNE;
			throw new IllegalStateException(msg);
		}
		
		try {
			// create >From Header
			String fromTag = SIPDistributor.createTag(); 
			FromHeader fromHeader = utils.createFromHeader(rte.src, rte.peerPort, fromTag);
			
			// create To Header
			ToHeader toHeader = null;
			// create Request URI
			SipURI requestURI = null;
			boolean presencePublish = false;
//			 GLH ADDR Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			// String addrType = "IP"; //  platform.getProperty(SettingConstants.ADDR_FORMAT);
			if (body == null) {
				// This is for publishing the session report information
				// assign To Header
				toHeader = utils.createToHeader(rte.src, null, false, rte.peerPort);
				// assign Request URI
				requestURI = utils.createRequestURI(rte.src, rte.peerPort, null);
			}
			else if (body.equalsIgnoreCase("open") ||
					body.equalsIgnoreCase("closed")) {
				// assign To Header
				toHeader = utils.createToHeader(rte.dest, null, false, rte.peerPort);
				
				// assign Request URI
				requestURI = utils.createRequestURI(rte.dest, rte.peerPort, null);
				presencePublish = true;
			}
						

			
			// Create a new Cseq header
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
						
			//	Create and add via headers
			// The way to add is from the source to the dest
			ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
			ViaHeader srcUE = utils.createViaHeader(rte.src, null, 
					rte.transport.toString(), true);
			srcUE.setParameter("rport", null);
			viaHeaders.add(srcUE);
			int hops = viaHeaders.size();
			
			// Create a new CallId header
			CallIdHeader callIdHeader = utils.createCallIdHeader(rte.dest, 
					rte.provider);
			
			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						requestURI,
						method,
						callIdHeader,
						cSeqHeader,
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
						
			// Create event header
			if (body == null) {
				EventHeader event = utils.createEventHeader("vq-rtcpxr");
				request.addHeader(event);
				
				// Create ContentTypeHeader
				ContentTypeHeader contentTypeHeader =
					utils.createContentTypeHeader("application", "vq-rtcpxr");
				String sessRpt = utils.createSessionReport();
				request.setContent(sessRpt, contentTypeHeader);
			}
			else if (presencePublish) {
				EventHeader event = utils.createEventHeader(SettingConstants.SUBSCRIBE_TYPE_PRESENCE);
				request.addHeader(event);
				
				// Create ContentTypeHeader
				ContentTypeHeader contentTypeHeader =
					utils.createContentTypeHeader("application", "pidf+xml");
				String pidf = utils.createPidfBody(rte.src, body);
				request.setContent(pidf, contentTypeHeader);
			}
			if (rte.target.getProperty(SettingConstants.DEVICE_TYPE).equals("AS")) {
				Header pAssert = utils.createPAssertedIdentityHeader(rte.src);
				if (pAssert != null)
					request.addHeader(pAssert);
			}
			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Creates a new SIP Refer Request message for the source argument
	 * to be sent to the target argument.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the routing information for this message.
	 * @param sipData - the connection details of the message.
	 * @param req - the original dialog forming  message that the REFER message is being generated for.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - SIP REFER message
	 * @throws IllegalStateException
	 */
	public Request buildRefer(Send send, SIPRoute rte, PC2SipData sipData, 
			Request req, NetworkElements nes) throws IllegalStateException {
		if (rte.dest == null || rte.src == null) {
			String msg = "A properties file has not been properly loaded. target=" 
					+ rte.targetNE +
					" source=" + rte.srcNE;
			throw new IllegalStateException(msg);
		}
		
		try {
			
			String method = Request.REFER;

			// get From and To Headers
			String fromTag = SIPDistributor.createTag();
			FromHeader fromHeader = utils.createFromHeader(rte.src, rte.localPort, fromTag);;
//			 create To Header
			ToHeader toHeader = utils.createToHeader(rte.dest, null, false, rte.localPort);
			 
			// create Request URI
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			//String addrType = "IP"; //  platform.getProperty(SettingConstants.ADDR_FORMAT);
			String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
			SipURI requestURI = utils.createRequestURI(rte.dest, rte.peerPort, addrType);
				
			// Create a new Cseq header
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
			
			//	Create and add via headers
			// The way to add is from the source to the dest
			ArrayList<ViaHeader> viaHeaders = buildViaHeader(rte.src, rte.dest, 
					nes, null, rte.transport.toString());
			int hops = viaHeaders.size();
			
			ViaHeader srcUE = null;
			boolean includeExpires = false;
			if (rte.srcNE.equals("DUT") && rte.src.getProperty(SettingConstants.DEVICE_TYPE).equals("UE")) {
				srcUE = utils.createViaHeader(rte.src, null, rte.transport.toString(), false);
				srcUE.setParameter("rport", null);
			}
			else {
				srcUE = utils.createViaHeader(rte.src, null, rte.transport.toString(), true);
				srcUE.setParameter("rport", ((Integer)rte.localPort).toString());
				srcUE.setParameter("received", rte.localAddress);
				includeExpires = true;
			}
			viaHeaders.add(srcUE);
			hops++;
			
			// Create a CallId header
			CallIdHeader callIdHeader = utils.createCallIdHeader(rte.dest, 
					sipData.getProvider());

			if (sipData.getCallId() == null)
				sipData.setCallId(callIdHeader.toString());
			
			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						requestURI,
						method,
						callIdHeader,
						cSeqHeader,
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
			// Create contact headers
			ContactHeader contactHeader = utils.createContactHeader(rte.src, 
					rte.localAddress, rte.localPort, false);
			request.addHeader(contactHeader);
			
			ArrayList<RecordRouteHeader> rrHeaders = buildRecordRoute(rte.src, 
					rte.dest, nes, rte.transport.toString());
			ListIterator<RecordRouteHeader> iter = rrHeaders.listIterator();
			while (iter.hasNext()) {
				Header rr = iter.next();
				//if (req.getMethod().equalsIgnoreCase("INVITE"))
				request.addHeader(rr);
			}
		
 			EventHeader eh = utils.createEventHeader(Request.REFER.toLowerCase());
			if (eh != null)
				request.addHeader(eh);

			if (includeExpires) {
				ExpiresHeader exh = utils.createExpiresHeader(0);
				if (exh != null)
					request.addHeader(exh);
			}
			
			SupportedHeader s = utils.createSupportHeader("tdialog");
			if (s != null) 
				request.addHeader(s);
			
			Header pcpi = utils.createPCalledPartyID(rte.dest);
			if (pcpi != null) 
				request.addHeader(pcpi);
			
			// In order to build the REFER we need to get the 200-INVITE message from the
			// referFSM
//			FSMListener fsmListener = Stacks.getFSMListenerByName(send.getReferFSM())
//			if (fsmListener != null) {
//				MsgEvent referTo = q.find(fsmListener.getFsmUID(), "200-INVITE", MsgQueue.LAST);
//				if (referTo != null && referTo instanceof SIPMsg) {
//					SIPMsg sipMsg = (SIPMsg)referTo;
//					if (sipMsg.hasSentMsg())
			ReferToHeader rt = utils.createReferToHeader();
			if (rt != null)
				request.setHeader(rt);
//				}
//			}
			
			MsgEvent finalResp = q.find(sipData.getListener().getFsmUID(), 
					"200-INVITE", MsgQueue.FIRST, sipData.getListener().getCurrentMsgIndex());
			if (finalResp != null &&
					finalResp instanceof SIPMsg) {
				Response response = ((SIPMsg)finalResp).getResponse();
				String callId = ((CallIdHeader)response.getHeader(CallIdHeader.NAME)).getCallId();
				TargetDialogHeader td = utils.createTargetDialogHeader(callId, sipData.getFromTag(), sipData.getToTag());
				if (td != null)
					request.setHeader(td);
			}
//			if (rte.target.getProperty(SettingConstants.DEVICE_TYPE).equals("AS")) {
//				insertRoutingForAS(request, rte, nes);
//				
//				Header pAssert = utils.createPAssertedIdentityHeader(rte.src);
//				if (pAssert != null)
//					request.addHeader(pAssert);
//				
//				Header pCharging = utils.createPChargingVectorHeader(rte.src, rte.dest);
//				if (pCharging != null)
//					request.addHeader(pCharging);
//				
//				Header pChargingFunc = utils.createPChargingFunctionAddressHeader(rte.src, rte.dest);
//				if (pChargingFunc != null)
//					request.addHeader(pChargingFunc);
//			}
			
			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
		
	}
	
	/**
	 * Creates a new SIP Invite Request message for the source argument
	 * to be sent to the target argument.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param sipData - the data being retained by the distributor about this dialog.
	 * @param origReq - the original INVITE request.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - a new SIP Invite message
	 * @throws IllegalStateException
	 */

	public Request buildReInvite(Send send, SIPRoute rte, PC2SipData sipData, 
			Request origReq, NetworkElements nes) throws IllegalStateException {

		if (rte.dest == null || rte.src == null) {
			String msg = "A properties file has not been properly loaded. target=" 
					+ rte.targetNE +
					" source=" + rte.srcNE;
			throw new IllegalStateException(msg);
		}
		
		try {
			URI origReqURI = null;
			String contactValue = null;
			if (sipData.getSSInitiated() &&  sipData.getResponseContact() != null) {
				origReqURI = utils.createRequestURI(sipData.getResponseContact());
				logger.debug(PC2LogCategory.SIP, subCat, 
				"SIPManufacturer using Response's Contact for Request-URI");
				contactValue = sipData.getRequestContact();
			}
			else {
				origReqURI = utils.createRequestURI(sipData.getRequestContact());
				logger.debug(PC2LogCategory.SIP, subCat, 
				"SIPManufacturer using Request's Contact for Request-URI");
				//origReq.getRequestURI();
				contactValue = sipData.getResponseContact();
			}
			String method = Request.INVITE;
			// create >From Header
			FromHeader fromHeader = null;
			ToHeader toHeader = null;
			if (sipData.getSSInitiated()) {
				fromHeader = utils.createFromHeader(rte.src, rte.localPort, sipData.getFromTag());
				toHeader = utils.createToHeader(rte.dest, sipData.getToTag(), false, rte.localPort);	
			}
			else {
				FromHeader origFrom = (FromHeader)origReq.getHeader(FromHeader.NAME);
				ToHeader origTo = (ToHeader)origReq.getHeader(ToHeader.NAME);
				fromHeader = utils.createFromHeader(origTo);
				toHeader = utils.createToHeader(origFrom);
			}
			
			// Create a new Cseq header
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
			
			//	Create and add via headers
			ArrayList<ViaHeader> viaHeaders = buildViaHeader(rte.src, rte.dest, 
					nes, null, rte.transport.toString());
			int hops = viaHeaders.size();
			ViaHeader srcUE = utils.createViaHeader(rte.src, null, rte.transport.toString(), true);
			srcUE.setParameter("received", "11.11.11.15");
			srcUE.setParameter("rport","4888");
			viaHeaders.add(srcUE);
			hops++;
			
			// Create ContentTypeHeader
			ContentTypeHeader contentTypeHeader = utils.createContentTypeHeader(null, null);
			
			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						origReqURI,
						method,
						(CallIdHeader)origReq.getHeader(CallIdHeader.NAME),
						cSeqHeader,
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
			// Create contact headers
			ContactHeader contactHeader = null;
			// If we initiated the dialog, then the contact can be constructed the 
			// same way we did the original INVITE message.
			if (sipData.getSSInitiated()) {
				contactHeader = utils.createContactHeader(rte.src, 
					sipData.getProvider().getListeningPoint().getHost(), rte.localPort, true);
			}
			else {
				// If we received the original INVITE, then we want to use the
				// original Request-URI to create the contact header.
//				// URI reqURI = origReq.getRequestURI();
//				// contactHeader = utils.createContactHeader(reqURI, rte.dest);
//				Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
//				//String addrType = "IP"; //  platform.getProperty(SettingConstants.ADDR_FORMAT);
//				String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
//				// PCPCSII-75 - see if we can find a value in the contactHeader table
//				// to use in the RequestURI of the INVITE
//				String phoneNum = rte.src.getProperty(SettingConstants.PHONE_NUMBER);
//				String userName = rte.src.getProperty(SettingConstants.USER_NAME);
				SipURI requestURI = null;
//				if (phoneNum.equals(userName)) {
//					String contact = PC2Models.getContact(phoneNum);
//					if (contact == null) {
//						logger.info(PC2LogCategory.SIP, subCat,
//								"contact information could not be found for phoneNum, using configuration information");
//						requestURI = utils.createRequestURI(rte.src, rte.localPort, addrType);
//					}
//					else 
//						requestURI = utils.createRequestSipURI(contact);
//				}
//				else
//					requestURI = utils.createRequestURI(rte.src, rte.localPort, addrType);
				if (contactValue != null)
					requestURI = utils.createRequestSipURI(contactValue);
				
				if (requestURI != null) 
					contactHeader = utils.createContactHeader(requestURI, rte.src);
			}
			if (contactHeader != null) {
				request.addHeader(contactHeader);
			}
			
			
			ArrayList<RecordRouteHeader> rrHeaders = buildRecordRoute(rte.src, rte.dest,
					nes, rte.transport.toString());
			ListIterator<RecordRouteHeader> iter = rrHeaders.listIterator();
			while (iter.hasNext()) {
				Header rr = iter.next();
				request.addHeader(rr);
			}						
			
			String sdpData = utils.createSDPData(rte.src, rte.srcNE, 
					true, null, true, false, PC2SipData.SESSION_ID,
					sipData.getSessionVersion());
			
			request.setContent(sdpData, contentTypeHeader);

			SupportedHeader sh = utils.createSupportHeader("tdialog");
			if (sh != null) 
				request.addHeader(sh);
			
			RequireHeader rh = utils.createRequireHeader(null);
			if (rh != null)
				request.addHeader(rh);
			
			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
		
	}
	/**
	 * Creates a SIP Register message. It uses the target and source information
	 * to create the various headers. This method is intended to send a REGISTER
	 * message.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param fsmUID - The unique identifier of the FSM sending the NOTIFY.
	 * @param req - the original Register request if applicable.
	 * @param prevResp - The Response to the original Register message.
	 * @param nes - the network elements associated with the invoking FSM.
	 * @param includeDigest - whether to include information for digest in the message
	 * @param curMsgIndex - the index of the current event being processed
	 * 
	 * @return - A SIP REGISTER message
	 * @throws IllegalStateException
	 */
	public Request buildRegister(Send send, SIPRoute rte, int fsmUID,
			Request req, Response prevResp, 
			NetworkElements nes, boolean includeDigest, int curMsgIndex) 
		throws IllegalStateException {
		if (rte.dest == null || rte.src == null) {
			String msg = "A properties file has not been properly loaded. target=" 
					+ rte.targetNE +
					" source=" + rte.srcNE;
			throw new IllegalStateException(msg);
		}

		
		try {
			boolean checkAuthorization = false;
			String method = Request.REGISTER;
			
			// Create a new Cseq header
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);

			FromHeader fromHeader = null;
			ToHeader toHeader = null;
			if (req == null) {
				// create From Header
				String fromTag = SIPDistributor.createTag();
				// Don't base the From and To on the Domain but the Address Format setting
//				fromHeader = utils.createDomainFromHeader(rte.src, rte.peerPort, fromTag);
//				// create To Header
//				toHeader = utils.createDomainToHeader(rte.src, null, false, rte.peerPort);
				
				fromHeader = utils.createFromHeader(rte.src, rte.localPort, fromTag);
				// create To Header
				toHeader = utils.createToHeader(rte.src, null, false, rte.peerPort);
			}
			else {
				fromHeader = (FromHeader)req.getHeader(FromHeader.NAME);
				toHeader = (ToHeader)req.getHeader(ToHeader.NAME);
				ToHeader respToHeader = (ToHeader)prevResp.getHeader(ToHeader.NAME);
				if (respToHeader != null)
					toHeader.setTag(respToHeader.getTag());
			}
			
			// create Request URI
			SipURI requestURI = null;
			if (rte.destDeviceType.equals(SettingConstants.AS))
				requestURI = utils.createRegisterRequestURI(rte.target, 0);
			else 
				requestURI = utils.createRegisterRequestURI(rte.src, rte.localPort);
			
	
			//	Create and add via headers
			// The way to add is from the source to the dest
			ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
			ViaHeader srcUE = utils.createViaHeader(rte.src, null, rte.transport.toString(), true);
			srcUE.removeParameter("lr");
			srcUE.removeParameter("keep-stun");
			srcUE.setParameter("rport", null);
			viaHeaders.add(srcUE);
			int hops = viaHeaders.size();
			
			CallIdHeader callIdHeader = null;
			// Create a new CallId header
			if (req == null) {
				callIdHeader = utils.createCallIdHeader(rte.dest, rte.provider);
			}
			else {
				callIdHeader = (CallIdHeader)req.getHeader(CallIdHeader.NAME);
				checkAuthorization = true;
			}
			
			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						requestURI,
						method,
						callIdHeader,
						cSeqHeader,
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
			// Create contact headers
			ContactHeader contactHeader = utils.createContactHeader(rte.src,
					rte.localAddress, rte.localPort, false);
			
			
			contactHeader.setParameter("expires", "600000");
			// Add sip instance parameter if GRUU is not disabled.
			if (useGRUU) {
				String uuid = rte.src.getProperty(SettingConstants.UUID);
				if (uuid != null) {
					contactHeader.setParameter("+sip.instance", "\"<urn:uuid:" + uuid + ">\""); 
				}
			}
			request.addHeader(contactHeader);
			

			// Add Route Header
			RouteHeader route = null;
			String plabel = rte.src.getProperty(SettingConstants.PCSCF);
			if (plabel.equals("DUT"))
				plabel = SettingConstants.PCSCF + "0";
			Properties pcscf = SystemSettings.getSettings(plabel);

			if (pcscf != null) {
				String transport = null;
				if (SystemSettings.useTransportParameter())
					transport = rte.transport.toString();
				route = utils.createRouteHeader(pcscf, true, rte.peerPort, transport);
				if (route != null) {
					request.addHeader(route);
				}
			}
			
			
			// Add Supported Header
			SupportedHeader sh = utils.createSupportHeader("path, sec-agree");
			if (sh != null) 
				request.addHeader(sh);
			
			// Add Require Header
			RequireHeader rh = utils.createRequireHeader(null);
			if (rh != null)
				request.addHeader(rh);
			
			// Add Expires Header
			ExpiresHeader eh = utils.createExpiresHeader(null);
			if (eh != null)
				request.addHeader(eh);
			
			// Add P-Access-Network-Info Header
			Header pAccess = utils.createPAccessNetworkInfoHeader(rte.src);
			request.addHeader(pAccess);
			
			// Next see if previous response to register was 401
			WWWAuthenticateHeader wwwAH = null;
			AuthorizationHeader ah = null;
			MsgEvent respEvent = q.find(fsmUID, "Response", 
					MsgQueue.LAST, curMsgIndex);
			if (respEvent != null && respEvent instanceof SIPMsg && checkAuthorization) {
				Response resp = ((SIPMsg)respEvent).getResponse();
				if (resp.getStatusCode() == 401)
					wwwAH = (WWWAuthenticateHeader)resp.getHeader(WWWAuthenticateHeader.NAME);
				// If we received a 200 response last time, we want to send the same
				// Authorization we sent in the previous REGISTER
				else if (resp.getStatusCode() == 200) {
					ah = (AuthorizationHeader)req.getHeader(AuthorizationHeader.NAME);
				}
			}	 
			
			// If we didn't have a 200 response last time, then
			// we need to create a new Authorization header
			if (ah == null) {
				int cSeqNo = cSeqHeader.getSequenceNumber();
				ah = utils.createAuthorizationHeader(method, rte.src, 
					cSeqNo, null, wwwAH);
			}
			
			if (ah != null) {
				request.addHeader(ah);
			}

			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
		
	}
	/**
	 * Creates a SIP Response message for the given Request with the provided
	 * status code. It can also include SDP when the includeSDP parameter is 
	 * set to true.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param sipData - the data being retained by the distributor about this dialog.
	 * @param request - the original request being responded to.
	 * @param nes - the network elements associated with the invoking FSM.
	 * @param statusCode - the status code of the response.
	 * @param includeSDP - whether to include SDP in the response or not.
	 *
	 * 
	 * @return - a SIP Response message.
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	public Response buildResponse(Send send, SIPRoute rte, PC2SipData sipData, 
			Request request, NetworkElements nes, int statusCode,  
			boolean includeSDP) 
	throws ParseException, InvalidArgumentException {
		Response response = messageFactory.createResponse(statusCode, request);
		
		// Remove any extraneous headers created by the stack in the response
		response.removeHeader(MaxForwardsHeader.NAME);
		if (statusCode == 100) {
			response.removeHeader(RecordRouteHeader.NAME);
			response.removeHeader(ContactHeader.NAME);
		}
		else if (statusCode > 179) {
			ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
			if (toHeader.getTag() == null &&
					sipData.getToTag() != null) {
				toHeader.setTag(sipData.getToTag());
			}
		}
		
		if (rte.src != null) {
			String method = request.getMethod();

			// Operate on the Contact Header
			ContactHeader contactHeader = null;
			if (method.equals(Request.REGISTER)) {
				if (statusCode == 200) {
					contactHeader = (ContactHeader)request.getHeader(ContactHeader.NAME);
					if (contactHeader != null) {
						int expires = contactHeader.getExpires();
						if (expires < 0)
							contactHeader.setExpires(600000);
						response.addHeader(contactHeader);
						
					}
				}
			}
			else if (statusCode == 200 && method.equals(Request.BYE)){
				// Don't add Contact Header
			}
			else if (statusCode > 100){
// BRKPT
//				if (method.equals("INVITE" )) {
//					int glh = 0;
//				}
				// PCPCSII-72 : Only use the requestURI in the Contact
				// header when it is not a urn:service:sos. If it is
				// a urn:service:sos, create the default type of contact
				// based upon the source.
				URI reqURI = request.getRequestURI();
				if (!reqURI.toString().contains("urn:service:sos")) {
					contactHeader = utils.createContactHeader(reqURI, rte.dest);
					if (contactHeader != null) {
						response.addHeader(contactHeader);
					}
				}
				else {
					contactHeader = utils.createContactHeader(rte.src,
							rte.localAddress, rte.localPort, false);
					if (contactHeader != null) {
						response.addHeader(contactHeader);
					}
				}
			}
// BRKPT
// if (method.equals("SUBSCRIBE" )){
//	int glh = 0;
//}
			// Check to see if we need to fill in the rport and received parameters
			// in the Via Header
			//if (!(rte.srcNE.equals("DUT"))) {
			if (!(rte.srcNE.equals("DUT") && rte.srcDeviceType.equals("UE"))) {
				ViaHeader via = (ViaHeader)response.getHeader(ViaHeader.NAME);
				via.setParameter("rport", ((Integer)rte.peerPort).toString());
				via.setReceived(rte.peerAddress);
			}
			boolean dialogForming = dialogForming(method);
			
			// See if we need include a Record-Route header
			if (statusCode >= 180 && statusCode <= 200 && dialogForming) {
				RequireHeader rh = utils.createRequireHeader(null);
				logger.debug(PC2LogCategory.SIP, subCat, "RequireHeader " + rh);
				if (rh != null)
					response.addHeader(rh);
				
				String transport = rte.transport.toString();
				ArrayList<RecordRouteHeader> rrHeaders = buildRecordRoute(rte.src, 
						rte.dest, nes, transport);
				ListIterator<RecordRouteHeader> iter = rrHeaders.listIterator();
				while (iter.hasNext()) {
					Header rr = iter.next();
					response.addHeader(rr);
				}
			}
			
			// Next see if this is a REGISTER response and apply the special
			// rules for it.
			if (request.getMethod().equals(SIPConstants.REGISTER)) {
				if	(statusCode == 401) {
					WWWAuthenticateHeader www = utils.createWWWAuthenticateHeader(rte.src, 
							rte.dest);
					// Now we need to see if the stale parameter should be included.
					// If the response parameter is not empty, then we will include it when
					// ever we send a 401.
					AuthorizationHeader ah = (AuthorizationHeader)request.getHeader(AuthorizationHeader.NAME);
					if (ah != null) {
						String ahResponse = ah.getResponse();
						if (ahResponse != null && ahResponse.length() > 0) {
							www.setStale(true);
						}
					}
					logger.debug(PC2LogCategory.SIP, subCat, "WWWAuthenticateHeader " + www);
					response.addHeader(www);
				}
				else if (statusCode == 200) {
					String pcscf = rte.dest.getProperty(SettingConstants.PCSCF);
					Properties p = SystemSettings.getSettings(pcscf);
					String scscf = p.getProperty(SettingConstants.SCSCF);
					Properties s = SystemSettings.getSettings(scscf);
					String supported = "path, sec-agree, reg";
					String require = "outbound";
					// NOTE GRUU : The registrar SHOULD NOT include the "gruu" option tag in the Require
					//  or Supported header field of the response.

					SupportedHeader sup = utils.createSupportHeader(supported);
					if (sup != null)
						response.addHeader(sup);
					
					RequireHeader req = utils.createRequireHeader(require);
					if (req != null)
						response.addHeader(req);

					if (nes.contains(pcscf) && nes.contains(scscf)) {
						Header path = utils.createPathHeader(s, p);
						if (path != null) {
							response.addHeader(path);
						}
					}
					
					Header serviceRoute = utils.createServiceRouteHeader(s);
					if (serviceRoute != null)
						response.addHeader(serviceRoute);
					
					FromHeader from = (FromHeader)request.getHeader(FromHeader.NAME);
					Header pAssocUri = utils.createPAssociatedURI(from.getAddress());
					if (pAssocUri != null)
						response.addHeader(pAssocUri);
					
					// We now need to get the qop value used in the Register for the 
					// AuthenticationInfo header
					if (request != null) {
						AuthorizationHeader ah = (AuthorizationHeader)request.getHeader(AuthorizationHeader.NAME);
						if (ah != null) {
							String qop = ah.getQop();
							if (qop != null) {
								AuthenticationInfoHeader aih = utils.createAuthenticationInfoHeader(null);
								if (aih != null) {
									aih.setQop(qop);
									String rspAuth = utils.calculateRspAuth(ah, rte.dest);
									if (rspAuth != null) {
										String cNonce = ah.getCNonce();
										if (cNonce != null)
											aih.setCNonce(cNonce);
										String nc = ah.getNonceCount();
										if (nc != null)
											aih.setNonceCount(nc);
										aih.setResponse(rspAuth);
									}
									response.addHeader(aih);
								}
							}
						}
					}
					
					// Now see if we need to add the GRUU information.
					if (useGRUU) {
						ContactHeader ch = (ContactHeader)request.getHeader(ContactHeader.NAME);
						if (ch != null) {
							String urn = ch.getParameter("sip.instance");
							if (urn != null) {
								RegistrarData rd = gruuDB.get(urn);
								CallIdHeader ci = (CallIdHeader)request.getHeader(CallIdHeader.NAME);
								String callId = ci.getCallId();
								if (rd != null && callId.equals(rd.getCallID()) ) {
									rd.assignTemporaryGRUU();
								}
								else {
									// If rd is not null then this means that the
									// call id changed, so delete the current
									// data an start fresh.
									if (rd != null) {
										aorGruuIndex.remove(rd.getAOR());
										gruuDB.remove(urn);
									}
									
									String aor = rte.dest.getProperty(SettingConstants.FQDN);
									String phoneNum = rte.dest.getProperty(SettingConstants.PHONE_NUMBER + "1");
									String contact = ch.getAddress().toString();
									rd = new RegistrarData(urn, phoneNum, contact, aor, callId);
									rte.dest.setProperty(SettingConstants.PUBLIC_GRUU, rd.getPubGruuParameter());
									rte.dest.setProperty(SettingConstants.TEMPORARY_GRUU, rd.getTempGruuParameter());
									rte.dest.setProperty(SettingConstants.GR, rd.getGrParameter());
									gruuDB.put(urn, rd);
									aorGruuIndex.put(aor, rd);
								}
								
								if (rd != null) {
									String pubGRUU = "\"" + rd.getPubGruuParameter() + "\"";
									String tempGRUU = "\"" + rd.getTempGruuParameter() + "\"";
									ch.setParameter(SettingConstants.PUBLIC_GRUU, pubGRUU);
									ch.setParameter(SettingConstants.TEMPORARY_GRUU, tempGRUU);
								}
									
							}
						}
					}
					
				}
			}
			else if (request.getMethod().equals(SIPConstants.INVITE)) {
				if	(statusCode == 407) {
					ProxyAuthenticateHeader pah = utils.createProxyAuthenticateHeader(rte.src, 
							rte.dest);
					logger.debug(PC2LogCategory.SIP, subCat, "ProxyAuthenticateHeader " + pah);
					response.addHeader(pah);
				}
				else if (statusCode == 200) {
// PCPCSII-72 & 73 :
//					String pcscf = rte.dest.getProperty(SettingConstants.PCSCF);
//					Properties p = SystemSettings.getSettings(pcscf);
//					String scscf = p.getProperty(SettingConstants.SCSCF);
//					Properties s = SystemSettings.getSettings(scscf);
					String supported = "path, sec-agree, outbound, tdialog";
					SupportedHeader sup = utils.createSupportHeader(supported);
					if (sup != null)
						response.addHeader(sup);

// PCPCSII-72 & 73 : - Vikas wants these headers only in the 200-REGISTER Message
//					if (nes.contains(pcscf) && nes.contains(scscf)) {
//						Header path = utils.createPathHeader(s, p);
//						if (path != null) {
//							response.addHeader(path);
//						}
//					}
//					
//					Header serviceRoute = utils.createServiceRouteHeader(s);
//					if (serviceRoute != null)
//						response.addHeader(serviceRoute);
					
					// We now need to get the qop value used in the Invite for the 
					// AuthenticationInfo header
					if (request != null) {
						AuthorizationHeader ah = (AuthorizationHeader)request.getHeader(AuthorizationHeader.NAME);
						if (ah != null) {
							String qop = ah.getQop();
							if (qop != null) {
								AuthenticationInfoHeader aih = utils.createAuthenticationInfoHeader(null);
								if (aih != null) {
									aih.setQop(qop);
									response.addHeader(aih);
								}
							}
						}
						ProxyAuthorizationHeader pah = (ProxyAuthorizationHeader)request.getHeader(ProxyAuthorizationHeader.NAME);
						if (pah != null) {
							String qop = pah.getQop();
							if (qop != null) {
								String value = "qop=auth,cnonce=\"" + pah.getCNonce();
								String nc = pah.getNonceCount();
								if (nc != null)
									value += "\",nc=" + nc;
								String rspAuth = utils.calculateRspAuth(pah, rte.dest);
								if (rspAuth != null) {
									value += ",rspauth=\"" + rspAuth + "\"";
								}
								Header pai = utils.createProxyAuthenticationInfoHeader(value);
								response.addHeader(pai);
							}
						}
					}
				}
				else if (statusCode >= 100 && statusCode <= 199) {
					String supported = "path, sec-agree, outbound, tdialog";
					SupportedHeader sup = utils.createSupportHeader(supported);
					if (sup != null)
						response.addHeader(sup);
				}
			}

			// Next see if this is a REFER message and apply its special rules
			// if it is one.
			if (statusCode >= 200 && statusCode <= 202 && method.equals(SIPConstants.REFER)) {
				CSeqHeader cseq = (CSeqHeader)response.getHeader(CSeqHeader.NAME);
				// We need to change the method name to REFER since we used
				// an INVITE message to construct the response
				if (cseq != null)
					cseq.setMethod(SIPConstants.REFER);
				
					EventHeader eh = utils.createEventHeader(Request.REFER.toLowerCase());
				if (eh != null)
					response.addHeader(eh);
				
				if (rte.srcNE.startsWith("UE") && !rte.srcNE.equals("UE0")) {
		//			String transport = sipData.getProvider().getListeningPoint().getTransport();
					ArrayList<RecordRouteHeader> rrHeaders = buildRecordRoute(rte.src, 
							rte.dest, nes, rte.transport.toString());
					ListIterator<RecordRouteHeader> iter = rrHeaders.listIterator();
					while (iter.hasNext()) {
						Header rr = iter.next();
						response.addHeader(rr);
					}
				}
			}
		
//			 Add the RSeq Header if it is needed 
			if (statusCode >= 180 && statusCode <= 189 && 
					(SystemSettings.getInstance().getReliability() == Extension.REQUIRED)) {
				RSeqHeader rseq = utils.createRSeqHeader();
				response.addHeader(rseq);
			}
			
			
			// See if we are supposed to automatically add an SDP body
			if (includeSDP) {
//				Create ContentTypeHeader
				ContentTypeHeader contentTypeHeader = utils.createContentTypeHeader(null, null);
				String remoteDirectionTag = null;
				Object body = request.getContent();
				String sdp = null;
				if (body instanceof String) {
					sdp = (String)body;
				}
				else if (body instanceof byte []) {
					byte [] bytes = (byte [])body;
					sdp = new String(bytes, 0, bytes.length);
						
				}
				if (sdp != null) {
					int begin = sdp.indexOf(SDPConstants.A_CUR_QOS_L);
					if (begin != -1) {
						begin += SDPConstants.A_CUR_QOS_L.length();
						int end = sdp.indexOf("\r\n", begin);
						remoteDirectionTag = sdp.substring(begin, end);
					}
				}

				String sdpData = utils.createSDPData(rte.src, rte.srcNE, false, 
						remoteDirectionTag, false, false, PC2SipData.SESSION_ID,
						sipData.getSessionVersion());
				
				if (statusCode == 200) {
					// set the media's direction mode based upon the REINVITE
					String value = sdpLocator.getSDPParameter("mode", "value", 
							MsgQueue.FIRST, MsgQueue.FIRST, null, request.toString());
					if (value != null) {
						if (value.equals("recvonly"))
							sdpData = sdpData.replaceFirst("sendrecv", "sendonly");
						else if (value.equals("sendonly"))
							sdpData = sdpData.replaceFirst("sendrecv", "recvonly");
						else if (value.equals("inactive")) 
							sdpData = sdpData.replaceFirst("sendrecv", "inactive");
					}
				}
				response.setContent(sdpData, contentTypeHeader);


			}
			else if (send.getIncludeMultipartBody()) {
//				Create ContentTypeHeader
				ContentTypeHeader contentTypeHeader = utils.createContentTypeHeader("multipart", "mixed");
				contentTypeHeader.setParameter("boundary", "boundary1");
				
				String remoteDirectionTag = null;
				Object body = request.getContent();
				String sdp = null;
				if (body instanceof String) {
					sdp = (String)body;
				}
				else if (body instanceof byte []) {
					byte [] bytes = (byte [])body;
					sdp = new String(bytes, 0, bytes.length);
						
				}
				if (sdp != null) {
					int begin = sdp.indexOf(SDPConstants.A_CUR_QOS_L);
					if (begin != -1) {
						begin += 18;
						int end = sdp.indexOf("\r\n", begin);
						remoteDirectionTag = sdp.substring(begin, end);
					}
				}

				String mb = utils.createMultipartBody(send.getBodies(), rte.src, 
						rte.srcNE, false, 
						remoteDirectionTag, false, PC2SipData.SESSION_ID,
						sipData.getSessionVersion());
				
//				if (statusCode == 200) {
//					// set the media's direction mode based upon the REINVITE
//					String value = sdpLocator.getSDPParameter("mode", "value", 
//							MsgQueue.FIRST, request.toString());
//					if (value != null) {
//						if (value.equals("recvonly"))
//							sdpData = sdpData.replaceFirst("sendrecv", "sendonly");
//						else if (value.equals("sendonly"))
//							sdpData = sdpData.replaceFirst("sendrecv", "recvonly");
//						else if (value.equals("inactive")) 
//							sdpData = sdpData.replaceFirst("sendrecv", "inactive");
//					}
//				}
				if (mb != null)
					response.setContent(mb, contentTypeHeader);


			}
			
			if (method.equals(Request.SUBSCRIBE)) {
				if (statusCode == 200) {
					long entity = PresenceServer.getEntityTag(rte.srcNE);
					if (entity > 0) {
						String entityTag = Long.toString(entity);
						Header sim = utils.createSIPIfMatchHeader(rte.src, entityTag);
						if (sim != null)
							response.addHeader(sim);
					}
					ExpiresHeader eh = request.getExpires();
					int expires = 600000;
					if (eh != null) {
						int reqExpires = eh.getExpires();
						if (reqExpires > 0) {
							if (reqExpires < 600000) {
								expires = reqExpires;
							}
						}
					}
					eh = utils.createExpiresHeader(expires);
					if (eh != null)
						response.addHeader(eh);
				}
			}
//			 Add the SIP-ETag
			else if (method.equals(SIPConstants.PUBLISH) && statusCode == 200) {
				long entity = PresenceServer.getEntityTag(rte.destNE);
				if (entity > 0) {
					String entityTag = Long.toString(entity);
					Header set = utils.createSIPETagHeader(rte.dest, entityTag);
					if (set != null)
						response.addHeader(set);
				}
				ExpiresHeader eh = request.getExpires();
				int expires = 600000;
				if (eh != null) {
					int reqExpires = eh.getExpires();
					if (reqExpires > 0) {
						if (reqExpires < 600000) {
							expires = reqExpires;
						}
					}
				}
				eh = utils.createExpiresHeader(expires);
				if (eh != null)
					response.addHeader(eh);
			}
			
			if (method.equalsIgnoreCase(SIPConstants.CANCEL)) {
				// 200-CANCEL should not have a Contact Header
				response.removeHeader(ContactHeader.NAME);
			}
				
			// Update the data container that we had a final response.
			if (statusCode >= 200 && 
					sipData.getFinalResponse() <= 0 && 
					(method.equals(SIPConstants.INVITE) ||
							method.equals(SIPConstants.SUBSCRIBE) ||
							method.equals(SIPConstants.REFER))) {
				sipData.setFinalResponse(statusCode);
			}
			
			// TODO GAREY -  Publish response needs to include the SIP-Etag header
			// we assign this value and reuse in future PUBLISH response.
//			The rules for what type of subscribe event it is is based upon the values
//			in the table below:
//			      +-----------+-------+---------------+---------------+
//			      | Operation | Body? | SIP-If-Match? | Expires Value |
//			      +-----------+-------+---------------+---------------+
//			      | Initial   | yes   | no            | > 0           |
//			      | Refresh   | no    | yes           | > 0           |
//			      | Modify    | yes   | yes           | > 0           |
//			      | Remove    | no    | yes           | 0             |
//			      +-----------+-------+---------------+---------------+
			return response;
		}

		return null;
	}
	/**
	 * Creates a SIP Subscribe message. It uses the target and source information
	 * to create the various headers. This method is intended to build the Subscribe
	 * message to be sent by the platforms.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param req - the original dialog forming message that the SUBSCRIBE message is being generated for.
	 * @param nes - the network elements associated with the invoking FSM.
	 * @param peerPort - the transport port number being used for the message.
	 *   
	 * @return - A SIP SUBSCRIBE message
	 * @throws IllegalStateException
	 */
	public Request buildSubscribe(Send send, SIPRoute rte, Request req, 
			NetworkElements nes, String type) throws IllegalStateException {
		if (rte.dest == null || rte.src == null) {
			String msg = "A properties file has not been properly loaded. target=" 
					+ rte.targetNE +
					" source=" + rte.srcNE;
			throw new IllegalStateException(msg);
		}
		
		try {
			String method = Request.SUBSCRIBE;
			
			// First make sure that type is set
			if (type == null)
				type = SettingConstants.SUBSCRIBE_TYPE_REG;
			
			Integer expires = null;
			boolean dialog = true;
			// create >From Header
			String fromTag = SIPDistributor.createTag(); // "888";
			FromHeader fromHeader = utils.createFromHeader(rte.src, rte.localPort, 
					fromTag);
			
			// create To Header
			// The To Header depends upon the type of event we are using.
			ToHeader toHeader = null;
			// create Request URI
			SipURI requestURI = null;
			
			Properties platform = SystemSettings.getSettings("Platform");
			//String addrType = "IP"; 
			String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
			if (type.equals(SettingConstants.SUBSCRIBE_TYPE_REG) || 
					type.equals(SettingConstants.SUBSCRIBE_TYPE_MSG_SUMMARY) ||
					type.equals(SettingConstants.SUBSCRIBE_TYPE_UA_PROFILE)) {
				toHeader = utils.createToHeader(rte.src, null, false, rte.localPort);
				requestURI = utils.createRequestURI(rte.src, rte.localPort, addrType);
			}
			else if (type.equals(SettingConstants.SUBSCRIBE_TYPE_PRESENCE)) {
				toHeader = utils.createToHeader(rte.dest, null, false, rte.peerPort);
				requestURI = utils.createRequestURI(rte.dest, rte.peerPort, addrType);
			}
			else if (type.equals(SettingConstants.SUBSCRIBE_TYPE_DIALOG)) {
				toHeader = utils.createToHeader(rte.dest, null, false, rte.peerPort);
				//requestURI = utils.createRequestSipURI("sip:" + rte.dest.getProperty(SettingConstants.IP));
				requestURI = utils.createRequestURI(rte.dest, rte.peerPort, addrType);
				expires = 0;
				dialog = true;
			}
			else {
				String msg = "The type parameter is set to an unrecognized value." 
					+ " The only valid values are reg and presence.";
				throw new IllegalStateException(msg);
			}
			
			// Create a new Cseq header
			CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
			
			//	Create and add via headers
			// The way to add is from the source to the dest
			ArrayList<ViaHeader> viaHeaders = buildViaHeader(rte.src, rte.dest, nes,
					null, rte.transport.toString());
			
			String srcPCSCF = rte.src.getProperty(SettingConstants.PCSCF);
			String srcSCSCF = SystemSettings.getSettings(srcPCSCF).getProperty(SettingConstants.SCSCF);

			ViaHeader srcUE = utils.createViaHeader(rte.src, null, 
					rte.transport.toString(), true);
			srcUE.removeParameter("lr");
			srcUE.removeParameter("keep-stun");
			srcUE.setParameter("rport", null);

			viaHeaders.add(srcUE);
			int hops = viaHeaders.size();
			
			// Create a new CallId header
			CallIdHeader callIdHeader = utils.createCallIdHeader(rte.dest, rte.provider);
			
			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(maxHops-hops);
			
			// Create the request.
			Request request =
				messageFactory.createRequest(
						requestURI,
						method,
						callIdHeader,
						cSeqHeader,
						fromHeader,
						toHeader,
						viaHeaders,
						maxForwards);
			// Create contact headers
			ContactHeader contactHeader = utils.createContactHeader(rte.src,
					rte.localAddress, rte.localPort, false);
			request.addHeader(contactHeader);
			

			// Add Route Header
			Properties pcscf = SystemSettings.getSettings(srcPCSCF);
			Properties scscf = SystemSettings.getSettings(srcSCSCF);
			String transport = null;
			if (SystemSettings.useTransportParameter())
				transport = rte.transport.toString();
			if (!dialog) {
				RouteHeader routePCSCF = utils.createRouteHeader(pcscf, false, rte.localPort, transport);
				RouteHeader routeSCSCF = utils.createRouteHeader(scscf, false, rte.localPort, transport);
				if (routePCSCF != null) 
					request.addHeader(routePCSCF);
			
				if (routeSCSCF != null) 
					request.addHeader(routeSCSCF);
				String supported = "reg, ua-profile, tdialog, message-summary";
				SupportedHeader sup = utils.createSupportHeader(supported);
				if (sup != null)
					request.addHeader(sup);
				
				// Add P-Access-Network-Info Header
				Header pAccess = utils.createPAccessNetworkInfoHeader(rte.src);
				request.addHeader(pAccess);
			}
			else {
				if (rte.srcDeviceType.equals(SettingConstants.UE)) {
					if (srcPCSCF != null) {
						if (srcPCSCF.equals("DUT"))
							srcPCSCF = SettingConstants.PCSCF + "0";
						Properties myP = SystemSettings.getSettings(srcPCSCF);
						// String realP = myP.getProperty(SettingConstants.SIMULATED);
						if (srcSCSCF != null) {
							if (srcSCSCF.equals("DUT"))
								srcSCSCF = SettingConstants.SCSCF + "0";
							Properties myS = SystemSettings.getSettings(srcSCSCF);
							if (SystemSettings.useTransportParameter())
								transport = rte.transport.toString();
							RouteHeader rteh = utils.createRouteHeader(myP, false, 
									rte.localPort, transport);
							if (rteh != null) 
								request.addHeader(rteh);
							rteh = utils.createRouteHeader(myS, false, 
									rte.localPort, transport);
							if (rteh != null) 
								request.addHeader(rteh);
						}
					}
//					Add P-Access-Network-Info Header
					Header pAccess = utils.createPAccessNetworkInfoHeader(rte.src);
					request.addHeader(pAccess);
				}
				else {
					ArrayList<RecordRouteHeader> rrHeaders = buildRecordRoute(rte.src, rte.target, 

							nes, rte.transport.toString());
					ListIterator<RecordRouteHeader> iter = rrHeaders.listIterator();
					while (iter.hasNext()) {
						Header rr = iter.next();
						request.addHeader(rr);
					}
				}
			}
			// Add Event Header
			
			EventHeader eh = utils.createEventHeader(type);
			if (eh != null)
				request.addHeader(eh);
			
			// Add Expires Header
			ExpiresHeader exh = utils.createExpiresHeader(expires);
			if (exh != null)
				request.addHeader(exh);
			
			return request;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
		
	}
	
	/**
	 * Creates a SIP Update message based upon the original INVITE message of the dialog
	 * It uses the toTag, target, and source parameters to construct the various headers
	 * of the message.
	 * 
	 * @param send - the Send contain to allow the manufacture to make decisions about
	 * 		any bodies that might be needed.
	 * @param rte - the Route information to build the message.
	 * @param sipData - the connection details of the message.
	 * @param req - the original INVITE for the dialog.
	 * @param nes - the network elements associated with the invoking FSM.
	 * @param includeSDP - a flag indicating that SDP should be included in the message.
	 * 
	 * @return - a SIP UPDATE message
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	public Request buildUpdate(Send send, SIPRoute rte, PC2SipData sipData, Request req,
			NetworkElements nes, boolean includeSDP) throws ParseException, InvalidArgumentException {
		
		String method = Request.UPDATE;
		URI uri = null;
		
		if (sipData.getSSInitiated() && sipData.getResponseContact() != null) {
			uri = utils.createRequestURI(sipData.getResponseContact());
			logger.debug(PC2LogCategory.SIP, subCat, 
			"SIPManufacturer using Response's Contact for Request-URI");
		}	
		else {
			uri = utils.createRequestURI(sipData.getRequestContact());
			logger.debug(PC2LogCategory.SIP, subCat, 
			"SIPManufacturer using Request's Contact for Request-URI");
		}
		
		CSeqHeader cSeqHeader = utils.createCSeqHeader(method);
		int hops = maxHops - 1;
		ArrayList<ViaHeader> viaHeaders = null;
		if (sipData.getFinalResponse() >= 200 && sipData.getSSInitiated()) {
			//	Create and add via headers
			// The way to add is from the source to the dest
			viaHeaders = buildViaHeader(rte.src, rte.target, nes,
					null, rte.transport.toString());
			hops = viaHeaders.size();

			ViaHeader srcUE = utils.createViaHeader(rte.src, null, 
					rte.transport.toString(), true);
			if (hops > 0)
				srcUE.setParameter("received", rte.localAddress);
		}
		else { 
			ListIterator<Header> iter = req.getHeaders(ViaHeader.NAME);
			viaHeaders = new ArrayList<ViaHeader>();
			while (iter.hasNext()) {
				viaHeaders.add((ViaHeader)iter.next());

			}
			hops = maxHops - viaHeaders.size();
		}
		
		ToHeader toHeader = null;
		FromHeader fromHeader = null;
		if (sipData.getSSInitiated()) {
			toHeader = (ToHeader)req.getHeader(ToHeader.NAME);
			toHeader.setTag(sipData.getToTag());
			fromHeader = (FromHeader)req.getHeader(FromHeader.NAME);
		}
		else {
			toHeader = utils.createToHeader((FromHeader)req.getHeader(FromHeader.NAME));
			fromHeader = utils.createFromHeader((ToHeader)req.getHeader(ToHeader.NAME));
			fromHeader.setTag(sipData.getToTag());
		}

//		 Create a new MaxForwardsHeader
		MaxForwardsHeader maxForwards = utils.createMaxForwardsHeader(hops);
		
		// Create the request.
		Request request =
			messageFactory.createRequest(
					uri, // (URI)req.getRequestURI(),
					method,
					(CallIdHeader)req.getHeader(CallIdHeader.NAME),
					cSeqHeader, 
					fromHeader,
					toHeader,
					viaHeaders,
					maxForwards);
		
		// Create contact headers
		ContactHeader contactHeader = utils.createContactHeader(rte.src, 
				sipData.getProvider().getListeningPoint().getHost(), rte.localPort, true);
		request.addHeader(contactHeader);
		
		// If the target is an AS we need to add the Route header with
		// the SCSCF and the AS
		if (rte.targetDeviceType.equals(SettingConstants.AS)) {
			 insertRoutingForAS(request, rte, nes);
		}
		
		// If the source of the message is the DUT and device type is an UE then
		// include the SDP
		// create the offer
		if ((rte.srcNE.equals("DUT") && 
				rte.srcDeviceType.equals(SettingConstants.UE)) ||
				includeSDP) {
			String sdpData = utils.createSDPData(rte.src, rte.srcNE, 
					true, "sendrecv", false, false, PC2SipData.SESSION_ID,
					sipData.getSessionVersion());
//			 Create ContentTypeHeader
			ContentTypeHeader contentTypeHeader = utils.createContentTypeHeader(null, null);
			
//			 Add the offer and the content type and length headers
			request.setContent(sdpData, contentTypeHeader);
		}
		
		return request;
	}

	/**
	 * A utility method for creating the Via Header for a new request message.
	 * 
	 * @param src - the properties for the network element sending this message.
	 * @param dest - the properties for the network element receiving this message.
	 * @param nes - the network elements associated with the invoking FSM.
	 * @param transport - the transport protocol being used for this message.
	 * 
	 * @return - a list of RecordRoute Headers
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected ArrayList<RecordRouteHeader> buildRecordRoute(Properties src, 
			Properties dest, NetworkElements nes, 
			String transport)  throws ParseException, InvalidArgumentException {
		LinkedList<String> routeSeq = getRoutingSeq(src, dest, nes);
		ArrayList<RecordRouteHeader> rrHeaders = new ArrayList<RecordRouteHeader>();
		if (routeSeq.size() > 0) {
			ListIterator<String> iter = routeSeq.listIterator();
			while(iter.hasNext()) {
				String ne = iter.next();
				RecordRouteHeader rrh = utils.createRecordRouteHeader(SystemSettings.getSettings(ne), 
						transport);
				if (rrh != null)
					rrHeaders.add(rrh);
			}
		}
		return rrHeaders;
	}
	/**
	 * A utility method for creating the Via Header for a new request message.
	 * 
	 * @param src - the properties for the network element sending this message.
	 * @param dest - the properties for the network element receiving this message.
	 * @param nes - the network elements associated with the invoking FSM.
	 * @param cSeqNo - the sequence number of this message.
	 * @param transport - the transport protocol being used for this message.
	 * 
	 * @return - a list of Via Headers
	 * 
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected ArrayList<ViaHeader> buildViaHeader(Properties src, 
			Properties dest, NetworkElements nes,
			String branch, String transport)  throws ParseException, InvalidArgumentException {
		LinkedList<String> routeSeq = getRoutingSeq(src, dest, nes);
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		if (routeSeq.size() > 0) {
			ListIterator<String> iter = routeSeq.listIterator();
			while(iter.hasNext()) {
				String ne = iter.next();
				ViaHeader via = utils.createViaHeader(SystemSettings.getSettings(ne), 
						branch, transport, true);
				
				viaHeaders.add(via);
			}
		}

		return viaHeaders;
	}
	

	
	private boolean dialogForming(String method) {
		if (method != null &&
				(method.equals(SIPConstants.INVITE) ||
						method.equals(SIPConstants.REFER) ||
						method.equals(SIPConstants.SUBSCRIBE))) {
			return true;
		}
		return false;
	}
	/**
	 * A common routine to build up the list of network element labels to be included in a 
	 * header of a SIP message. 
	 * 
	 * @param src - the property information for the originiator of the message.
	 * @param target - the property information for the receiver of the message.
	 * @param nes - the network elements being simulated by the FSM.
	 * 
	 * @return a linked list of the network element labels to include in the header starting
	 * with the network element closest to the target.
	 * @throws InvalidArgumentException
	 */
	protected LinkedList<String> getRoutingSeq(Properties src, Properties target, 
			NetworkElements nes) throws InvalidArgumentException {
		// Elements that should be included in the header are added
		// to this list in reverse order. In other words base upon elements
		// closest to the egress device first.
		LinkedList<String> routingSeq = new LinkedList<String>();
		String srcNE = src.getProperty(SettingConstants.NE);
		if (srcNE != null && srcNE.endsWith("0")) {
			routingSeq.equals(srcNE);
		}
		else if (srcNE != null)  {
			String srcType = src.getProperty(SettingConstants.DEVICE_TYPE);
			String destType = target.getProperty(SettingConstants.DEVICE_TYPE);
			if (srcType.equals("UE") || 
					(srcType.equals("SCSCF") && destType.equals("UE"))) {
				// Since this message is to appear to traverse 
				// from a UE to another UE the sequence is 
				// the destination's PCSCF, destination's SCSCF, source's
				// SCSCF (if different then destination's), and then
				// source's PCSCF
				String srcPCSCF = src.getProperty(SettingConstants.PCSCF);
				String srcSCSCF = null;
				String destPCSCF = null;
				String destSCSCF = null;
				if (srcPCSCF != null && nes.contains(srcPCSCF)) {
					srcSCSCF = SystemSettings.getSettings(srcPCSCF).getProperty(SettingConstants.SCSCF);
					if (srcSCSCF != null && nes.contains(srcSCSCF)) {
						if (destType.equals("UE")) {
							destPCSCF = target.getProperty(SettingConstants.PCSCF);
							
							if (destPCSCF != null && nes.contains(destPCSCF)) {
								destSCSCF = SystemSettings.getSettings(destPCSCF).getProperty(SettingConstants.SCSCF);
								if (destSCSCF != null && nes.contains(destSCSCF)) {
									routingSeq.add(destPCSCF);
									if (!destSCSCF.equals(srcSCSCF))
										routingSeq.add(destSCSCF);
								}
								else {
									logger.debug(PC2LogCategory.SIP, subCat, 
											"Manufacturer could not add the network element label (" 
											+ destSCSCF + ") to the list of elements for a header because it doesn't exist in the elements list for the FSM.");
								}
								
							}
							else {
								logger.debug(PC2LogCategory.SIP, subCat, 
										"Manufacturer could not add the network element label (" 
										+ destPCSCF + ") to the list of elements for a header because it doesn't exist in the elements list for the FSM.");
							}
							routingSeq.add(srcSCSCF);
						}
						else if (destType.equals("AS")) {
							routingSeq.add(srcPCSCF);
						}
					}
					else {
						logger.debug(PC2LogCategory.SIP, subCat, 
								"Manufacturer could not add the network element label (" 
								+ srcPCSCF + ") to the list of elements for a header because it doesn't exist in the elements list for the FSM.");
					}
					routingSeq.add(srcPCSCF);
				}
				else {
					logger.debug(PC2LogCategory.SIP, subCat, 
							"Manufacturer could not add the network element label (" 
							+ srcPCSCF + ") to the list of elements for a header because it doesn't exist in the elements list for the FSM.");
				}
			}
			else if (srcType.equals("PCSCF") && destType.equals("UE")) {
				// Since this message is to appear to be a dialog with the core
				// we only want to add the SCSCF and the PCSCF 
				String srcSCSCF = src.getProperty(SettingConstants.SCSCF);
				if (srcSCSCF != null && nes.contains(srcSCSCF)) {
					routingSeq.add(srcSCSCF);
				}
				else {
					logger.debug(PC2LogCategory.SIP, subCat, 
							"Manufacturer could not add the network element label (" 
							+ srcSCSCF + ") to the list of elements for a header because it doesn't exist in the elements list for the FSM.");
				}
				routingSeq.add(srcNE);
						
			}
		}
		else {
			logger.error(PC2LogCategory.SIP, subCat, 
					"Manufacturer could not find the NE property to build a SIP header.");
		}
		
		ListIterator<String> iter = routingSeq.listIterator();
		String msg = null;
		
		while (iter.hasNext()) {
			if (msg == null)
				msg = iter.next() + " ";
			else
				msg += iter.next() + " ";
		}
			
		
		if (msg == null) 
			msg = "empty ";
		
		logger.debug(PC2LogCategory.SIP, subCat,
				"RoutingSeq created the following sequence " 
				+ msg + "for the header.");
		
		return routingSeq;	
	}
	
	public Properties getSCSCFProperty(String ue) {
		Properties p = SystemSettings.getSettings(ue);
		Properties scscf = null;
		Properties pcscf = null;
		if (p != null) {
			pcscf = SystemSettings.getSettings(p.getProperty(SettingConstants.PCSCF));
			if (pcscf != null) {
				scscf = SystemSettings.getSettings(pcscf.getProperty(SettingConstants.SCSCF));
			}
		}
		return scscf;
		
	}
	
	/**
	 * This method inserts the appropriate Route headers for a message intended to be sent
	 * to an AS.
	 * 
	 * @param request - The original request message.
	 * @param rte - the Route information to build the message.
	 * @param nes - the network elements being simulated by the FSM.
	 *  
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void insertRoutingForAS(Request request, SIPRoute rte, NetworkElements nes) 
	throws InvalidArgumentException, ParseException {
		// If the target is an AS we need to add the Route header with
		// the SCSCF and the AS
		String srcPCSCF = rte.src.getProperty(SettingConstants.PCSCF);
		if (srcPCSCF != null) {
			Properties myP = SystemSettings.getSettings(srcPCSCF);
			String srcSCSCF = myP.getProperty(SettingConstants.SCSCF);
			if (srcSCSCF != null) {
				Properties myS = SystemSettings.getSettings(srcSCSCF);
				Properties myAS = rte.target;
				if (myAS != null && 
						rte.targetDeviceType.equals(SettingConstants.AS)) {
					String transport = null;
					if (SystemSettings.useTransportParameter())
						transport = rte.transport.toString();
					RouteHeader asRH = utils.createRouteHeader(myAS, false, 
							rte.localPort, transport);
					RouteHeader sRH = utils.createRouteHeader(myS, false, 
							rte.localPort, transport);
					if (asRH != null && sRH != null) {
						// Before adding the header get the call dialog identifier
						String dialogID = myAS.getProperty(SettingConstants.DIALOG_ID);
						String dialogIDParam = myAS.getProperty(SettingConstants.DIALOG_ID_PARAM);
						if (dialogID != null && dialogIDParam != null) {
							URI u = asRH.getAddress().getURI();
							if (u.isSipURI()) {
								((SipURI)u).setParameter(dialogIDParam, dialogID);
							}
							else {
								((TelURL)u).setParameter(dialogIDParam, dialogID);
							}
						}
						else {
							logger.debug(PC2LogCategory.SIP, subCat, 
									"Could not include the dialog id parameter in the first Route header " +
									"because dialog id parameter=[" + dialogIDParam + " and dialog id["
									+ dialogID + "].");
						}
						request.addHeader(asRH);
						request.addHeader(sRH);
					}
				}
			}
		}
	}
	
	/**
	 * Performs all of the updates to a Request message that is being
	 * proxied through the platform.
	 * 
	 * @param origReq - the request message to operate upon.
	 * @param rte - the Route information to build the message.
	 * @param nes - the network elements associated with the invoking FSM.
	 * 
	 * @return - the altered message
	 */
	public Message updateRequest(SIPRoute rte, Request origReq,  
			NetworkElements nes, PC2SipData sipData, String ginContactAddress ) {
		try {
			// What type of device are we trying to send the message to
			String srcDevice = rte.src.getProperty(SettingConstants.DEVICE_TYPE);
			String destDevice = rte.dest.getProperty(SettingConstants.DEVICE_TYPE);
			Request req = (Request)origReq.clone();
			if (srcDevice.equals("SCSCF") || srcDevice.equals("UE")) {
				String branch = ((ViaHeader)req.getHeader(ViaHeader.NAME)).getBranch();
				//int cSeqNo = ((CSeqHeader)req.getHeader(CSeqHeader.NAME)).getSequenceNumber();
				String method = origReq.getMethod();
				// First add received and rport to the top Via in the request
				ViaHeader via = (ViaHeader)req.getHeader(ViaHeader.NAME);
				via.setParameter("received", rte.localAddress);
				via.setParameter("rport", ((Integer)rte.localPort).toString());

				ArrayList<ViaHeader> viaHeaders = buildViaHeader(rte.src, 
						rte.dest, nes, branch, rte.transport.toString());
				ListIterator<ViaHeader> viter = viaHeaders.listIterator();
				int hops = viaHeaders.size();
				
				while (viter.hasNext()) {
					via = viter.next();
					if (sipData.isDialogAccepted() &&
							method.equals(SIPConstants.ACK)) {
						branch = via.getBranch() + "_ack";
						logger.debug(PC2LogCategory.SIP, subCat,
								"Setting the branch parameter in the Via header to branch=[" 
								+ branch + "].");
						via.setBranch(branch);
					}
					req.addHeader(via);
				}

				// We need to add Record-Route if the messgae is a dialog 
				// forming request.
				if (dialogForming(method)) {
					ArrayList<RecordRouteHeader> rrHeaders = buildRecordRoute(
							rte.src, rte.dest, nes, rte.transport.toString());
					ListIterator<RecordRouteHeader> iter = rrHeaders.listIterator();
					while (iter.hasNext()) {
						Header rr = iter.next();
						logger.debug(PC2LogCategory.SIP, subCat,
								"Adding " + rr + " to " + method + " request message.");
						req.addHeader(rr);
					}
					
					// We also need to see if the Request-URI is a tel URI and
					// if it is we need to substitue the value with the contact 
					// information
					URI origURI = req.getRequestURI();
					if (!origURI.isSipURI() && origURI instanceof TelURL) {
						String phoneNum = ((TelURL)origURI).getPhoneNumber();
						String contact = PC2Models.getContact(phoneNum);
						if (ginContactAddress != null) {
							int index = contact.indexOf("@");
							if (index != -1)
								contact = contact.substring(0, index) + ginContactAddress;
						}
						if (contact != null) {
							URI newURI = utils.createRequestURI(contact);
							logger.debug(PC2LogCategory.SIP, subCat,
									"Replacing Request-URI(" + origURI + ") with new URI(" + newURI + ".");
							req.setRequestURI(newURI);
						}
					}
					else if (origURI.isSipURI()) {
						String phoneNum = ((SipURI)origURI).getUser();
						String contact = PC2Models.getContact(phoneNum);
						if (ginContactAddress != null) {
							int index = contact.indexOf("@");
							if (index != -1)
								// The plus one on the index value is to keep the @ symbol from the original request uri
								contact = contact.substring(0, index+1) +  ginContactAddress;
							else {
								// In this case the contact looks like sip:10.47.8.8:5060;bnc
								index = contact.indexOf(":");
								if (index != -1) {
									// The plus one is to get the : 
									contact = contact.substring(0, index + 1) + phoneNum + "@" + contact.substring(index+1);
								}
							}
						}
						if (contact != null) {
							URI newURI = utils.createRequestURI(contact);
							logger.debug(PC2LogCategory.SIP, subCat,
									"Replacing Request-URI(" + origURI + ") with new URI(" + newURI + ".");
							req.setRequestURI(newURI);
						}
					}
					
					// Lastly we need to include the P-Called-Party-ID header if it doesn't exist
					Header pcpi = req.getHeader("P-Called-Party-ID");
					if (pcpi == null) {
						pcpi = utils.createPCalledPartyID(rte.dest);
						if (pcpi != null) 
							req.addHeader(pcpi);
					} 
				}
				
				MaxForwardsHeader maxFwd = (MaxForwardsHeader)req.getHeader(MaxForwardsHeader.NAME);
				if (maxFwd != null) {
					logger.debug(PC2LogCategory.SIP, subCat,
							"Setting Max-Forwards to " + ((maxHops-1)-hops) + " hops.");
					maxFwd.setMaxForwards((maxHops-1)-hops);
				}
				
				if (destDevice.startsWith("UE")) {
					int rmRoutes = 70 - hops;

					ListIterator<Header> routeIter = req.getHeaders(RouteHeader.NAME);
					ArrayList<RouteHeader> newRoute = new ArrayList<RouteHeader>();
					if (routeIter.hasNext()) {
						if (--rmRoutes < 0) 
							newRoute.add((RouteHeader)routeIter.next());
						else
							routeIter.next();
					}
					req.removeHeader(RouteHeader.NAME);
					ListIterator<RouteHeader> newRouteIter = newRoute.listIterator();
					while (newRouteIter.hasNext())
						req.addHeader(newRouteIter.next());
					
					// PCPCSII-80
					req.removeHeader("P-Preferred-Identity");
					
				
					
				}
				else if (destDevice.startsWith("AS")) {
					req.removeHeader(RouteHeader.NAME);
					Properties scscf0 = SystemSettings.getSettings("SCSCF0");
					String transport = null;
					if (SystemSettings.useTransportParameter())
						transport = rte.transport.toString();
					RouteHeader srh = utils.createRouteHeader(scscf0, false, rte.localPort, transport);
					RouteHeader asrh = utils.createRouteHeader(rte.dest, false, rte.peerPort, transport);
					req.addHeader(asrh);
					req.addHeader(srh);

				}
			}
			else if (srcDevice.equals("AS")) {
				ListIterator<Header> routeIter = req.getHeaders(RouteHeader.NAME);
				req.removeHeader(RouteHeader.NAME);
				if (routeIter.hasNext()) {
					// Drop AS route
					routeIter.next();
					// Now add the rest back to the message
					while (routeIter.hasNext()) {
						RouteHeader rh = (RouteHeader)routeIter.next();
						req.addHeader(rh);
					}
					
				}
			}
			
			return req;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
	}
	
	/**
	 * 
	 * Performs all of the updates to a Response message that is being
	 * proxied through the platform.
	 * 
	 * @param origResp - the response message to update.
	 * 
	 * @return - the altered response message
	 */
	public Message updateResponse(Response origResp, NetworkElements nes) { 
		int rmCount = 0;			
		try {
			Response resp = (Response)origResp.clone();
			ListIterator<Header> viaIter = resp.getHeaders(ViaHeader.NAME);
//			ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
			while (viaIter.hasNext()) {
				ViaHeader via = (ViaHeader)viaIter.next();
				String branch = via.getBranch();
				int offset = branch.indexOf((SIPUtils.mc + "_"));
				if (offset != -1) {
					// move the offset past magic cookie
					offset += SIPUtils.mc.length() + 1;
					int end = branch.indexOf("-", offset);
					if (end != -1) {
						String ne = branch.substring(offset, end);
						ListIterator<String> iter = nes.getElements();
						boolean match = false;
						while (iter.hasNext() && !match) {
							if (ne.equals(iter.next())) {
								match = true;
//								String cSeq = ((Integer)((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getSequenceNumber()).toString();
								viaIter.remove();
								rmCount++;
								logger.info(PC2LogCategory.SIP, subCat, 
										"Removing Via[" + via + "] from proxied response message.");
							}
						}
					}
				}
//				String cSeq = ((Integer)((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getSequenceNumber()).toString();
//				String branchEnd = "cscf" + cSeq;
//				if (!branch.substring((branch.length()-branchEnd.length()), branch.length()).equals(branchEnd)) 
//					viaHeaders.add(via);
//				else 
//					rmCount++;
				
			}
//			resp.removeHeader(ViaHeader.NAME);
//			ListIterator<ViaHeader> newVia = viaHeaders.listIterator();
//			while (newVia.hasNext()) {
//				resp.addHeader((ViaHeader)newVia.next());
//			}
			int cSeq = ((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getSequenceNumber();
			if (cSeq == 100) {
				ListIterator<Header> iter = resp.getHeaders(RecordRouteHeader.NAME);
				int count = rmCount;
				while (iter.hasNext() && count > 0) {
					// We have to do a next on the iterator so that we can
					// remove the element.
					@SuppressWarnings("unused")
					RecordRouteHeader rrh = (RecordRouteHeader)iter.next();
					iter.remove();
					count--;
				}
				if (!iter.hasNext()) 
					resp.removeHeader(RecordRouteHeader.NAME);
			}
			// Lastly reduce the MaxForwards count by the rmCount variable
			MaxForwardsHeader maxFwd = (MaxForwardsHeader)resp.getHeader(MaxForwardsHeader.NAME);
			if (maxFwd != null) 
				maxFwd.setMaxForwards((maxFwd.getMaxForwards()-rmCount));
			
			// PCPCSII-80
			resp.removeHeader("P-Preferred-Identity");
			
			return resp;
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.SIP, subCat, ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * This is the starting method for making modifications to a SIP
	 * message. The current message being constructed is passed into the method along 
	 * with the unique identifier of the FSM making the modifications and the modifications
	 * to make.
	 * 
	 * @param fsmUID - the unique ID of the FSM making the change to the message
	 * @param mods - the modifications to make on the SIP message
	 * @param message - the current Request or Response message being constructed.
	 * @param compact - Whether the message should use the compact form of 
	 * 		the headers or not.
	 * 
	 * @return - the modified message to be sent.
	 */
	public String modifyMessage(int fsmUID, LinkedList<Mod> mods, 
			Message message, boolean compact) {
		String result = null;
		if (mods != null) {
			LinkedList<Mod> sipMods = new LinkedList<Mod>();
			ListIterator<Mod> iter = mods.listIterator();
			while (iter.hasNext()) {
				Mod mod = iter.next();
				String body = mod.getBody();
				String hdr = mod.getHeader();
				if (hdr != null && 
						(SDPConstants.isValidSDPHeader(hdr) ||
								hdr.equals("SDP") ||
								(body != null && body.equals("SDP"))) ) {
					try {
						modSDPData(fsmUID, mod, message);
					}
					catch (Exception ex) {
						logger.warn(PC2LogCategory.SIP, subCat, 
								"SIPManufacturer encountered error while trying to modify SDP" + ex.getMessage() + ex.getStackTrace());
					}
				}
				else if (mod.getBody() != null) {
					try {
						modSIPBody(fsmUID, mod, message);
					}
					catch (Exception ex) {
						logger.warn(PC2LogCategory.SIP, subCat, 
						"SIPManufacturer encountered error while trying to modify SDP");
					}
				}
				else {
					sipMods.add(mod);
				}
			}
			// Now that all of the SDP modifications have been completed,
			// we are going to make all of the SIP modifications on a 
			// String so that negative test cases can be sent over the
			// transport channel.
			String modMsg = message.toString();
			if (sipMods.size() > 0) {
				logger.trace(PC2LogCategory.SIP, subCat, "modMsg=" + modMsg);
				iter = sipMods.listIterator();
				while(iter.hasNext()) {
					Mod mod = iter.next();
					try {
						modMsg = modSIPData(fsmUID, mod, modMsg);
						logger.trace(PC2LogCategory.SIP, subCat, "modMsg=" + modMsg);
					}
					catch (Exception ex) {
						logger.warn(PC2LogCategory.SIP, subCat, "Skipping mod, " 
								+ mod + " because encountered exception.\n" 
								+ ex.getMessage() + "\n" + ex.getStackTrace());
					}
					
				}
			}
			logger.trace(PC2LogCategory.SIP, subCat, "modMsg=" + modMsg);
			if (compact)
				result = shortForm(modMsg);
			else 
				result = modMsg;
			// return modMsg;
		}
		else {
			logger.debug(PC2LogCategory.SIP, subCat, 
					"No modifiers are required for the message.");
			if (compact)
				result = shortForm(message.toString());
			else
				result = message.toString();
			// return message.toString();
		}
		
		return result;
	}

	/**
	 * This is the starting method for making modifications to the SDP portion of a SIP
	 * message. The current message being constructed is passed into the method along 
	 * with the unique identifier of the FSM making the modifications and the modification
	 * to make.
	 * 
	 * @param fsmUID - the unique ID of the FSM making the change to the message
	 * @param mod - the modification to make on the SDP body
	 * @param message - the current Request or Response message being constructed.
	 */
	private void modSDPData(int fsmUID, Mod mod, Message message) {
		// Get the content header to verify that the body is of type SDP
		ContentTypeHeader ct = (ContentTypeHeader)message.getHeader(ContentTypeHeader.NAME);
		String sdp = null;
		if (ct != null) {
			Object content = message.getContent();
			if (content instanceof byte[]) {
				sdp = new String((byte [])content);
			}
			else {
				sdp = content.toString();
			}
		}
		int newlineLen = 2;
		boolean success = false;
		
		boolean sdpChange = false;
		String boundary = null;
		if (ct != null) {
			if (ct.getContentSubType().equals(MsgRef.SDP_MSG_TYPE)) {
				sdpChange = true;
			}
			else if (ct.getContentType().equals("multipart") &&
					ct.getContentSubType().equals("mixed")) {
				sdpChange = true;
				boundary = ct.getParameter("boundary");
			}
		}	
		else { 
			if (ct == null && 
					mod.getHeader().equals("SDP") &&
					mod.getModType().equals("add")) {
				try {
					ct = utils.createContentTypeHeader(null, null);
					sdpChange = true;
				} 
				catch (Exception ex) {
					logger.error(PC2LogCategory.SIP, subCat,  
					"Could not create ContentTypeHeader to add SDP during modify message operation.");
				}
			}
		}

		// The basic premise is to locate the starting and ending location within the 
		// existing body that the modification is to be performed upon. Then get
		// the information from the reference, make the change at the location and finally
		// to put the body back into the message
		if (sdpChange && 
				ct != null)	{
			// This section handles the addition of a parameter or a header to the SDP body
			if (mod.getModType().equals("add")) {
				if (mod.getParam() != null) {
					// locate the correct header instance for the data to be added to
					String hdrInstance = mod.getHeaderInstance();
					int [] hdrLocation = sdpLocator.locateSDPHeader(sdp, 
							hdrInstance, mod.getBodyInstance(), boundary, mod.getHeader());
					// Locate the end of the header, then insert data just prior to the "\r\n"
					if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
						String newParam = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
						if (newParam != null) {
							int [] paramLocation = sdpLocator.locateSDPParam(mod.getHeader(), 
									mod.getParam(), hdrLocation, sdp );
							if (paramLocation[0] != -1 && paramLocation[1] != -1) {
								// Now append the data to the end location
								String newSDP = sdp.substring(0,paramLocation[1]) + newParam + 
								sdp.substring((paramLocation[1]),sdp.length());
								//System.out.println(newSDP);
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SDP modifier appending(" + 
										sdp.substring(paramLocation[0],paramLocation[1]) + 
										") to form (" + sdp.substring(paramLocation[0], paramLocation[1]) 
										+ newParam + ").");

								try {
									message.setContent(newSDP, ct);
									success = true;
								}
								catch (ParseException pe) {
									logger.warn(PC2LogCategory.SIP, subCat, 
											"SIPManufacturer had error during SDP mod processing", pe);
								}
							}

						}
					}
				}
				else if (mod.getHeader() != null) {
					String hdrInstance = mod.getHeaderInstance();
					int [] hdrLocation = sdpLocator.locateSDPHeader(sdp, hdrInstance, 
							mod.getBodyInstance(), boundary, mod.getHeader());
					// Locate the end of the header, then insert data just prior to the "\r\n"
					if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
						String value = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
						String key = SDPConstants.getKey(mod.getHeader());
						String newSDP = sdp.substring(0, (hdrLocation[1]+newlineLen)) + key + value
						+ "\r\n" + sdp.substring((hdrLocation[1]+(newlineLen)), sdp.length());
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SDP modifier adding header(" + key + value + ") plus CRLF to message.");
						try {
							message.setContent(newSDP, ct);
							success = true;
						}
						catch (ParseException pe) {
							logger.warn(PC2LogCategory.SIP, subCat, 
									"SIPManufacturer had error during SDP mod processing", pe);
						}

					}
					else if (hdrLocation[0] == -1) {
						// An offset of -1 means that this can be appended to the end of the 
						// existing sdp except in the case of the header is ice-lite as this
						// need to be added to the session-attribute.
						String hdr = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
						if (hdr.equals(SDPConstants.ICE_LITE)) {
							hdrLocation = sdpLocator.locateSDPHeader(sdp, 
									MsgQueue.FIRST, mod.getBodyInstance(), boundary, SDPConstants.T);
							if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
								// We need to offset +2 of the ending position for the CRLF
								String newSDP = sdp.substring (0,hdrLocation[1]+2) + hdr 
								+ "\r\n" + sdp.substring(hdrLocation[1]+2);
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SDP modifier adding header(" + hdr + ") plus CRLF to session-attributes.");
								try {
									message.setContent(newSDP, ct);
									success = true;
								}
								catch (ParseException pe) {
									logger.warn(PC2LogCategory.SIP, subCat, 
											"SIPManufacturer had error during SDP mod processing", pe);
								}
							}
						}
						else {
							String newSDP = null;
							if (sdp != null)
								newSDP = sdp + hdr + "\r\n";
							else
								newSDP = hdr + "\r\n";
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SDP modifier adding header(" + hdr + ") plus CRLF to message.");
							try {
								message.setContent(newSDP, ct);
								success = true;
							}
							catch (ParseException pe) {
								logger.warn(PC2LogCategory.SIP, subCat, 
										"SIPManufacturer had error during SDP mod processing", pe);
							}
						}
					}
				}
			}
			// This section handles the deletion of a parameter or a header to the SDP body
			else if (mod.getModType().equals("delete")) {
				// Detemine if this is a deletion of a parameter or an entire header
				if (mod.getParam() != null) {
					String hdrInstance = mod.getHeaderInstance();
					int [] hdrLocation = sdpLocator.locateSDPHeader(sdp, hdrInstance,
							mod.getBodyInstance(), boundary, mod.getHeader());
					// Locate the end of the header, then delete data from header "\r\n"
					if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
						int [] paramLocation = sdpLocator.locateSDPParam(mod.getHeader(), 
								mod.getParam(), hdrLocation, sdp);
						if (paramLocation[0] != -1 && paramLocation[1] != -1) {
							String newSDP = sdp.substring(0,paramLocation[0]) + sdp.substring(paramLocation[1], sdp.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SDP modifier deleting parameter to produce new header (" + 
									sdp.substring(paramLocation[0],paramLocation[1]) + ") for message.");
							try {
								message.setContent(newSDP, ct);
								success = true;
							}
							catch (ParseException pe) {
								logger.warn(PC2LogCategory.SIP, subCat, 
										"SIPManufacturer had error during SDP mod processing", pe);
							}
						}

					}
				}
				else if (mod.getHeader() != null) {
					String hdrInstance = mod.getHeaderInstance();
					if (mod.getHeader().equals("SDP")) {
						message.removeHeader(ContentTypeHeader.NAME);
						message.removeContent();
						success = true;
					}
					else {
						int [] hdrLocation = sdpLocator.locateSDPHeader(sdp, hdrInstance, 

								mod.getBodyInstance(), boundary, mod.getHeader());
						// Locate the end of the header, then delete data from header "\r\n"
						if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {

							String newSDP = sdp.substring(0,hdrLocation[0]) + sdp.substring((hdrLocation[1]+newlineLen), sdp.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SDP modifier deleting (" + 
									sdp.substring(hdrLocation[0],hdrLocation[1]) 
									+ ") and CRLF from message.");
							try {
								message.setContent(newSDP, ct);
								success = true;
							}
							catch (ParseException pe) {
								logger.warn(PC2LogCategory.SIP, subCat, 
										"SIPManufacturer had error during SDP mod processing", pe);
							}
						}	

					}
				}
				else if (mod.getHeader() == null && mod.getParam() == null) {
					message.removeHeader(ContentTypeHeader.NAME);
					message.removeContent();
					success = true;
				}
			}

			// This section handles the replacement of a parameter or a header to the SDP body
			else if (mod.getModType().equals("replace")) {
				if (mod.getParam() != null) {
					// locate the correct header instance for the data to be added to
					String hdrInstance = mod.getHeaderInstance();
					int [] hdrLocation = sdpLocator.locateSDPHeader(sdp, hdrInstance, 
							mod.getBodyInstance(), boundary, mod.getHeader());
					// Locate the end of the header, then insert data just prior to the "\r\n"
					if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
						String newParam = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
						if (newParam != null) {
							int [] paramLocation = sdpLocator.locateSDPParam(mod.getHeader(), 
									mod.getParam(), hdrLocation, sdp);

							if (paramLocation[0] != -1 && paramLocation[1] != -1) {
								String newSDP = sdp.substring(0,paramLocation[0]) + newParam + 
								sdp.substring(paramLocation[1],sdp.length());
								//System.out.println(newSDP);
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SDP modifier replacing(" + 
										sdp.substring(paramLocation[0],paramLocation[1]) 
										+ ") with (" + newParam + ").");
								try {
									message.setContent(newSDP, ct);
									success = true;
								}
								catch (ParseException pe) {
									logger.warn(PC2LogCategory.SIP, subCat, 
											"SIPManufacturer had error during SDP mod processing", pe);
								}
							}
						}

					}

				}
				else if (mod.getHeader() != null) {
					// locate the correct header instance for the data to be added to
					String hdrInstance = mod.getHeaderInstance();
					int [] hdrLocation = sdpLocator.locateSDPHeader(sdp, hdrInstance, 
							mod.getBodyInstance(), boundary, mod.getHeader());
					// Locate the end of the header, then insert data just prior to the "\r\n"
					if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
						String newParam = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
						if (newParam != null) {
							String newSDP = sdp.substring(0,hdrLocation[0]) + newParam +
							sdp.substring(hdrLocation[1],sdp.length());
							try {
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SDP modifier replacing(" + 
										sdp.substring(hdrLocation[0],hdrLocation[1]) 
										+ ") with (" + newParam + ").");
								message.setContent(newSDP, ct);
								success = true;
							}
							catch (ParseException pe) {
								logger.warn(PC2LogCategory.SIP, subCat, 
										"SIPManufacturer had error during SDP mod processing", pe);
							}
						}

					}
					else if (mod.getHeader().equals("SDP")) {
						String newSDP = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
						if (newSDP != null) {
							try {
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SDP modifier replacing(" + 
										sdp 
										+ ") with (" + newSDP + ").");
								message.setContent(newSDP, ct);
								success = true;
							}
							catch (ParseException pe) {
								logger.warn(PC2LogCategory.SIP, subCat, 
										"SIPManufacturer had error during SDP mod processing", pe);
							}
						}
					}
					else if (mod.getHeader() == null && mod.getParam() == null) {
						String newSDP = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
						if (newSDP != null) {
							try {
								message.setContent(newSDP, ct);
								success = true;
							}
							catch (ParseException pe) {
								logger.warn(PC2LogCategory.SIP, subCat, 
										"SIPManufacturer had error during SDP mod processing", pe);
							}
						}
					}
				}
			}
		}
		
		
		
		if (!success) {
			String method = "";
			if (message instanceof Request)
				method = ((Request)message).getMethod();
			else if (message instanceof Response) 
				method = ((Response)message).getStatusCode() + "-" + ((CSeqHeader)((Response)message).getHeader(CSeqHeader.NAME)).getMethod();
			logger.warn(PC2LogCategory.SIP, subCat, 
					"SIPManufacturer could not perform mod [" + mod + " to " +
					method + " message.");
		}
	}
	/**
	 * This method takes a SIP message and performs all of the modifications
	 * to the SIP data. It does not modify any of the body. The modSDPData
	 * method will perform all modifications upon that body.
	 * 
	 * @param fsmUID - The FSM that is sending the message
	 * @param mod - The modification to make on the current message
	 * @param message - The current message to operate upon.
	 * 
	 * 
	 * @return - the altered message
	 */
	private String modSIPData(int fsmUID, Mod mod, String message) {
		String result = message;
		// NOTE: The message will be treated as a string once it is 
		// modified for SIP.
		// This method starts by obtaining the reference information
		// for the new value.
		// Next decides what type of modification is to be made.
		// Then decides how deep into the message the modification is 
		// being made. 
		String hdr = mod.getHeader();
		String param = mod.getParam();
		String modType = mod.getModType();
		String newValue = null;
		
		if (!modType.equals("delete")) {
			Reference ref = mod.getRef();
			newValue = refLocator.getReferenceInfo(fsmUID, ref, null);
			// Next see if the reference to the mod is an add or substract reference
			if (ref instanceof MsgRef) {
				MsgRef mr = (MsgRef)ref;
				if (mr.isArithmeticRef()) {
					try {
						int val = 0;
						if (newValue != null) {
							val = Integer.parseInt(newValue);
							if (mr.isAddRef()) {
								val += mr.getArithmeticMod();
								newValue = ((Integer)val).toString();
							}
							else if (mr.isSubRef()) {
								val -= mr.getArithmeticMod();
								newValue = ((Integer)val).toString();
							}
						}
						// Assume the previous value is zero if the reference is not
						// present
						else {
							if (mr.isAddRef()) {
								newValue = ((Integer)mr.getArithmeticMod()).toString();
							}
							else {
								newValue = ((Integer)val).toString();
							}
						}
					}
					catch (NumberFormatException nfe) {
						logger.warn(PC2LogCategory.SIP, subCat, 
								"add_ref could not complete properly because reference=" + newValue 
								+ " is not a number.");
					}
				}
			}
		}
		if (hdr != null) {
			if (modType.equals("add") && newValue != null) {
				int [] hdrLocation = sipLocator.locateSIPHeader(hdr, 
						mod.getHeaderInstance(), message, true, false);
				if (param != null && hdrLocation[0] != -1 &&
						hdrLocation[1] != -1) {
					int [] paramLocation = sipLocator.locateSIPParameter(hdr, param, hdrLocation, message);
					if (paramLocation[0] != -1 && paramLocation[1] != -1 && paramLocation[2] != -1) {
						if (mod.getBefore()) {
							result = message.substring(0, paramLocation[0]) +
							newValue + message.substring(paramLocation[0], message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier pre-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + newValue.length())) + ").");
						}
						else {
							String value = newValue;
							result = message.substring(0, paramLocation[2]);
							if (message.charAt(paramLocation[2]) != '\r' && 
									paramLocation[3] == SIPLocator.GENERIC_PARAM) {
								// If the hdr doesn't allow multiple headers we should
								// add a comma not a semi-colon
								if (SIPConstants.multipleHeadersAllowed(hdr))
									value += ";";
								else 
									value += ",";
							}
							else if (message.charAt(paramLocation[2]) != '\r' &&
									paramLocation[3] == SIPLocator.VALUE_PARAM) {
								 // DO NOTHING 
							}
							else if (message.charAt(paramLocation[2]) == '\r' ||
									message.charAt(paramLocation[2]) == ','&&
									paramLocation[3] == SIPLocator.VALUE_COMMA_PARAM) 
								value = "," + value;
							else if (message.charAt(paramLocation[2]) != '\r' &&
									paramLocation[3] == SIPLocator.VALUE_COMMA_PARAM) 
								value += ",";
							
							
							result += value + message.substring((paramLocation[2]), message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
					}
					else if (paramLocation[0] == -1 && paramLocation[1] == -1 && paramLocation[2] == -1 &&
							 paramLocation[3] == SIPLocator.GENERIC_PARAM) {
						String value = null;
						if (SIPConstants.multipleHeadersAllowed(hdr))
							value = ";" + param + "=" + newValue;
						else 
							value = "," + param + "=" + newValue;
						result = message.substring(0, hdrLocation[1]) + value + message.substring((hdrLocation[1]), message.length());
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIP modifier post-appending to (" + 
								message.substring(hdrLocation[0],hdrLocation[1]) + 
								") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
					}
				}
				// We are adding a whole header
				else if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
					int index = hdrLocation[1];
					if (mod.getBefore())
						index = hdrLocation[0];
					if (mod.getSeparate()) {
						if (mod.getBefore()) {
							String value = hdr + ": " + newValue + "\r\n";
							result = message.substring(0,index) + 
							value + message.substring(index, message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier pre-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
						else {
							String value = "\r\n" +	hdr + ": " + newValue;
							result = message.substring(0,index) + value + message.substring(index, message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier pre-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
					}
					else if (hdrLocation[2] == SIPLocator.NO_COMMA) {
						if (mod.getBefore()) {
							// Calculate the realIndex by taking index plus length of header + : + SP
							int realIndex = index+hdr.length()+2;
							String value = newValue + ",";
							result = message.substring(0,realIndex) + value +
							message.substring(realIndex,message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
						else {
							String value = "\r\n" +	newValue;
							result = message.substring(0, index) + value + message.substring(index, message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
					}
					else if (hdrLocation[2] == SIPLocator.COMMA_DELIMITED_FRONT) {
						if (mod.getBefore() ) {
							// Calculate the realIndex by taking index plus length of header + : + SP
							int realIndex = index+hdr.length()+2;
							String value = newValue + ",";
							result = message.substring(0,realIndex) + value +
							message.substring(realIndex,message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
						else {
							String value = "," + newValue;
							result = message.substring(0, index) + value +  message.substring(index, message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
					}
					else if (hdrLocation[2] == SIPLocator.COMMA_DELIMITED_MIDDLE) {
						if (mod.getBefore() ) {
							// Calculate the realIndex by taking index plus length of header + : + SP
							int realIndex = index+hdr.length()+2;
							String value = newValue + ",";
							result = message.substring(0,realIndex) + value + message.substring(realIndex,message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
						else {
							String value = "," + newValue;
							result = message.substring(0, index) + value +  message.substring(index, message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
					}
					else if (hdrLocation[2] == SIPLocator.COMMA_DELIMITED_END) {
						if (mod.getBefore() ) {
							// Calculate the realIndex by taking index plus length of header + : + SP
							int realIndex = index+hdr.length()+2;
							String value = newValue + ",";
							result = message.substring(0,realIndex) + value + message.substring(realIndex,message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
						else {
							result = message.substring(0, index) + 
							newValue + message.substring(index, message.length());
							logger.debug(PC2LogCategory.SIP, subCat, "SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + newValue.length())) + ").");
						}
					}
					else if (hdrLocation[2] == SIPLocator.CRLF) {
						if (mod.getBefore() ) {
							// Calculate the realIndex by taking index plus length of header + : + SP
							if (hdr.equals(SIPLocator.REQUEST_LINE)) {
								result = newValue + message;
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SIP modifier prepending to (" + 
										message.substring(hdrLocation[0],hdrLocation[1]) + 
										") to form (" + (newValue + message.substring(hdrLocation[0],hdrLocation[1])) + ").");
							}
							else {
								int realIndex = index+hdr.length()+2;
								String value = newValue + ",";
								result = message.substring(0,realIndex) 
									+ value + message.substring(realIndex,message.length());
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SIP modifier inserting to (" + 
										message.substring(hdrLocation[0],hdrLocation[1]) + 
										") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
							}
							
						}
						else {
							String value = "," + newValue;
							result = message.substring(0, index) + value + message.substring(index, message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier post-appending to (" + 
									message.substring(hdrLocation[0],hdrLocation[1]) + 
									") to form (" + result.substring(hdrLocation[0],(hdrLocation[1] + value.length())) + ").");
						}
					}
				}
				else if (hdrLocation[0] == -1 && hdrLocation[1] == -1) {
					int body = message.indexOf("\r\n\r\n");
					if (body != -1) {
						String value = "\r\n" + hdr + ": " + newValue;
						result = message.substring(0, body) + value + message.substring(body, message.length());
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIP modifier post-appending to the end of the message to form (" + 
								result.substring(body,(body + value.length())) + ").");
					}
				}
				
			}
			else if (modType.equals("delete")) {
				// First see if the hdr_instance is all or something else.
				String hdrInstance = mod.getHeaderInstance(); 
				if (hdrInstance.equalsIgnoreCase(MsgQueue.ANY)) {
					result = new String(message);
// BRKPT
//					if (hdr.equals("Supported")) {
//						int glh = 0;
//					}
					int [] hdrLocation = sipLocator.locateSIPHeader(hdr, 
							hdrInstance, result, true, false);
					while (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
						String removing = result.substring(hdrLocation[0],hdrLocation[1]+2);
						result = new String(result.substring(0, hdrLocation[0]) + 
						result.substring((hdrLocation[1] + 2), result.length()));
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIP modifier removing (" + 
								removing + 
						") from message.");
						hdrLocation = sipLocator.locateSIPHeader(hdr, 
								hdrInstance, result, true, false);
					}
				}
				else {
					int [] hdrLocation = sipLocator.locateSIPHeader(hdr, 
							hdrInstance, message, true, false);
				
				if (param != null && hdrLocation[0] != -1 &&
						hdrLocation[1] != -1) {
					int [] paramLocation = sipLocator.locateSIPParameter(hdr, param, hdrLocation, message);
					if (paramLocation[0] != -1 && paramLocation[1] != -1 && paramLocation[2] != -1) {
						if (paramLocation[3] == SIPLocator.VALUE_COMMA_PARAM &&
								hdrLocation[2] == SIPLocator.COMMA_DELIMITED_FRONT ||
								hdrLocation[2] == SIPLocator.COMMA_DELIMITED_MIDDLE) {
	
							// The plus one for the ending offset is for the comma
							result = message.substring(0, paramLocation[0]) +
								message.substring(paramLocation[2]+1, message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier removing (" + 
									message.substring(paramLocation[0],paramLocation[2]) + 
									") from message resulting in header (" +
									result.substring(hdrLocation[0], (hdrLocation[1]-(paramLocation[2]-paramLocation[0]))) +
							").");
							//System.out.println(result);
						}
						else {
							if (message.charAt(paramLocation[2]) == '\r' && 
									paramLocation[3] == SIPLocator.VALUE_COMMA_PARAM) {
								// The plus two for the ending offset is for the CRLF
								result = message.substring(0, hdrLocation[0]) +
								message.substring(hdrLocation[1]+2, message.length());
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SIP modifier removing the complete header(" + 
										message.substring(hdrLocation[0],(hdrLocation[1]+2)) + 
										") from message.");
							}
							else {
								// We need to determine if a leading ';' should also be removed
								int start = paramLocation[0];
								if ((paramLocation[3] == SIPLocator.GENERIC_PARAM ||
										paramLocation[3] == SIPLocator.VALUE_PARAM) &&
										(message.charAt(start-1) == ';' ||
												(!SIPConstants.multipleHeadersAllowed(hdr) &&
														message.charAt(start-1) == ',')))
									start--;
								// In case this was a quoted string see if we need to adjust
								// then end to include deleting the quote charater
								if (message.charAt(paramLocation[2]) == '"')
									paramLocation[2]++;
									//if (SIPConstants.multipleHeadersAllowed(hdr))
								result = message.substring(0, start) +
										message.substring(paramLocation[2], message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier removing (" + 
									message.substring(start,paramLocation[2]) + 
									") from message resulting in header (" +
									result.substring(hdrLocation[0], 
											(hdrLocation[1]-(paramLocation[2]-start))) +
									").");
							}
							//System.out.println(result);
						}
					}
				}
				
				// We are deleting a whole header
				else if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
					if (hdrLocation[2] == SIPLocator.NO_COMMA ||
							hdrLocation[2] == SIPLocator.CRLF) {
						// the plus two for the ending offset is for the CRLF
						result = message.substring(0, hdrLocation[0]) + 
						message.substring((hdrLocation[1] + 2), message.length());
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIP modifier removing (" + 
								message.substring(hdrLocation[0],hdrLocation[1]+2) + 
						") from message.");
					}
					else if (hdrLocation[2] == SIPLocator.COMMA_DELIMITED_FRONT) {
						// Need to move past the header 
						int index = hdrLocation[0] + hdr.length() + 2;
						// The plus one for the ending offset is for the comma
						result = message.substring(0, index) + 
						message.substring((hdrLocation[1] + 1), message.length());
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIP modifier removing (" + 
								message.substring(index,hdrLocation[1]+1) + 
						") from message.");
					}
					else if (hdrLocation[2] == SIPLocator.COMMA_DELIMITED_MIDDLE) {
						// The plus one for the ending offset is for the comma
						result = message.substring(0, hdrLocation[0]) + 
						message.substring((hdrLocation[1] + 1), message.length());
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIP modifier removing (" + 
								message.substring(hdrLocation[0],hdrLocation[1]+1) + 
						") from message.");
					}
					else if (hdrLocation[2] == SIPLocator.COMMA_DELIMITED_END) {
						// We need to subtract one if the preceeding character is a ','
						// if it is equal to an end.
						if (message.charAt(hdrLocation[0]-1) == ',')
							result = message.substring(0, (hdrLocation[0]-1)) + 
								message.substring((hdrLocation[1]), message.length());
						else
							result = message.substring(0, (hdrLocation[0])) + 
							message.substring((hdrLocation[1]), message.length());
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIP modifier removing (" + 
								message.substring(hdrLocation[0],hdrLocation[1]) + 
						") from message.");
					}
				}
				else {
					logger.info(PC2LogCategory.SIP, subCat, 
							"SIPManufacturer header[" + hdr 
							+ "] not removed from message because it couldn't be located.");
				}
			}
			}
			else if (modType.equals("replace")&& newValue != null) {
				int [] hdrLocation = sipLocator.locateSIPHeader(hdr, 
						mod.getHeaderInstance(), message, true, false);
				if (param != null && hdrLocation[0] != -1 &&
						hdrLocation[1] != -1) {
					int [] paramLocation = sipLocator.locateSIPParameter(hdr, param, hdrLocation, message);
					if (paramLocation[0] != -1 && paramLocation[1] != -1 && paramLocation[2] != -1) {
						if (paramLocation[3] == SIPLocator.GENERIC_PARAM) {
							result = message.substring(0, paramLocation[1]) +
							newValue + message.substring(paramLocation[2], message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier replacing(" + 
									message.substring(paramLocation[1],paramLocation[2]) + 
									") with (" +  newValue + ").");
						}
						else if (paramLocation[3] == SIPLocator.VALUE_PARAM) {
							result = message.substring(0, paramLocation[1]) + newValue +
							message.substring(paramLocation[2], message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier replacing(" + 
									message.substring(paramLocation[1],paramLocation[2]) + 
									") with (" +  newValue + ").");
						}
					}
					else if (paramLocation[0] == -1 && paramLocation[1] == -1) {
						if (paramLocation[3] == SIPLocator.GENERIC_PARAM) {
//							 If the hdr doesn't allow multiple headers we should
							// add a comma not a semi-colon
							String value = "";
							if (SIPConstants.multipleHeadersAllowed(hdr))
								value += ";";
							else 
								value += ",";
							value += param + "=" + newValue;
							result = message.substring(0, hdrLocation[1]) + value +
							message.substring(hdrLocation[1], message.length());
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIP modifier adding(" + 
									value  + 
									") to message to form(" + result.substring(hdrLocation[0], (hdrLocation[1]+value.length())) +
							") since parameter doesn't exist.");
						}
					}
				}
				// We are replacing a whole header
				else if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
					String value = null;
					if (newValue.startsWith(hdr))
						value = newValue;
					else if (hdrLocation[2] != -1 && 
							(hdrLocation[2] == SIPLocator.COMMA_DELIMITED_END ||
									hdrLocation[2] == SIPLocator.COMMA_DELIMITED_MIDDLE))
						value = newValue;
					else 
						value = hdr + ": " + newValue;
					result = message.substring(0, hdrLocation[0])  +
					value + message.substring((hdrLocation[1]), message.length());
					logger.debug(PC2LogCategory.SIP, subCat, 
							"SIP modifier replacing(" + message.substring(hdrLocation[0], hdrLocation[1]) + 
					") with (" + value + ").");
				}
				else if (hdrLocation[0] == -1 && hdrLocation[1] == -1) {
					int index = message.indexOf("\r\n\r\n");
					if (param == null) {
						String value = "\r\n" + hdr + ": " + newValue;
						result = message.substring(0,index) + value + 
						message.substring(index,message.length());
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIP modifier adding(" + value + 
						") to end of SIP message since unable to locate header.");
					}
					else {
						// Since the header doesn't exist we need to know if the parameter should include
						// the parameter name or not
						int [] paramLocation = sipLocator.locateSIPParameter(hdr, param, hdrLocation, message);
						if (paramLocation[0] == -1 && paramLocation[1] == -1 && paramLocation[2] == -1) {
							if (paramLocation[3] == SIPLocator.GENERIC_PARAM) {
								String value = "\r\n" + hdr + ": " + param + "=" + newValue;
								result = message.substring(0,index) + value + 
								message.substring(index,message.length());
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SIP modifier adding(" + value + 
								") to end of SIP message since unable to locate header.");
							}
							else if (paramLocation[3] == SIPLocator.VALUE_PARAM) {
								String value = "\r\n" + hdr + ": " + newValue;
								result = message.substring(0,index) + value + 
								message.substring(index,message.length());
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SIP modifier adding(" + value + 
								") to end of SIP message since unable to locate header.");
							}
						}
					}
				}
			}
		}
		else {
			logger.warn(PC2LogCategory.SIP, subCat, 
					"SIPManufacture could not obtain the value indicated by "
					+ "the reference of a Mod operation on header[" +
					hdr + "].");
		}
		return result;
	}
	/**
	 * This is the starting method for making modifications to the body portion of a SIP
	 * message. The current message being constructed is passed into the method along 
	 * with the unique identifier of the FSM making the modifications and the modification
	 * to make.
	 * 
	 * @param fsmUID - the unique ID of the FSM making the change to the message
	 * @param mod - the modification to make on the SDP body
	 * @param message - the current Request or Response message being constructed.
	 */
	private void modSIPBody(int fsmUID, Mod mod, Message message) {
		// Get the content header to verify that the body is of type SDP
		ContentTypeHeader ct = (ContentTypeHeader)message.getHeader(ContentTypeHeader.NAME);
		Object content = message.getContent();
		String bodyType = mod.getBody();
		String subType = null;
		String mediaType = null;
		// See if we have only been give the subType
		if (bodyType.contains("/")) {
			// We have an media and subtype
			int index = bodyType.indexOf("/");
			if (index != -1) {
				subType = bodyType.substring((index+1));
				mediaType = bodyType.substring(0,index);
			}
		}
		else {
			subType = bodyType;
		}
		
		if (content instanceof String) {
			String body = content.toString();

//if (subType.equals("simple-message-summary")) {
//	int glh =0;
//}
	
//			int newlineLen = 2;
			boolean success = false;
			if (ct != null) {
				if (ct.getContentSubType().equals(subType)) {
					// The modification performs the following steps in order.
					//   1. Get the information from the reference.
					//	 2. Locate the starting and ending location within the 
					//      existing body that the modification is to be performed upon.
					//   3. Make the change at the location.
					String newInfo = null;
					String hdr = mod.getHeader();
					String param = mod.getParam();
					if (mod.getRef() != null)
						newInfo = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
					int [] location = sipLocator.locateSIPBody(subType, hdr, 
							mod.getHeaderInstance(), param, 
							mod.isXMLValue(), body, false);
					String newBody = null;
					if (location[0] != -1 && location[1] != -1) {

						if (mod.getModType().equals("add") && newInfo != null) {
							if (mod.getBefore()) { 
								if (param != null)
									newBody = body.substring(0, location[0])
										+ " " + param + "=\"" + newInfo 
										+ "\"" + body.substring(location[0]);
								else
									newBody = body.substring(0, location[0])
									+ " " + newInfo 
									+ body.substring(location[0]);
							}
							else {
								if (param != null)
									newBody = body.substring(0, location[1]) 
									+ " " + param + "=\"" + newInfo 
									+ "\"" + body.substring(location[1]);
								else
									newBody = body.substring(0, location[1])
									+ " " + newInfo 
									+ body.substring(location[1]);
							}
						}
						else if (mod.getModType().equals("replace") && newInfo != null) {
							// Next we need to see if the attribute is already in the 
							// message or if this is actually an add and we should
							// include it in the insertion.
							if (body.charAt(location[0]) == '>' ||
								body.charAt(location[0]) == '/') {
								if (param != null)
									newBody = body.substring(0, location[0])
									+ " " + param + "=\"" + newInfo 
									+ "\"" + body.substring(location[1]);
								else
									newBody = body.substring(0, location[0])
									+ " " + newInfo 
									+ body.substring(location[1]);
							}
							else {
								newBody = body.substring(0, location[0])
									+ newInfo + body.substring(location[1]);
							}
						}
						else if (mod.getModType().equals("delete")) {
							if (location[2] == SIPLocator.CRLF)
								newBody = body.substring(0, location[0]) + body.substring((location[1]+2));
							else
								newBody = body.substring(0, location[0]) + body.substring(location[1]);
							
						}

					}
					/**
					 * If the location doesn't exist at all we can add to the existing
					 * body with the new information.
					 */
					else if ((mod.getModType().equals("add") ||
							mod.getModType().equals("replace")) && newInfo != null) {
						
						if (SIPConstants.isMWIHeader(hdr) && param == null)
							newInfo = hdr + ": " + newInfo + "\r\n";
						if (mod.getBefore()) 
							newBody =  newInfo + body;
						else 
							newBody = body + newInfo;
					}

					if (newBody != null) {
						try {
							message.setContent(newBody, ct);
							success = true;
						}
						catch (ParseException pe) {
							logger.warn(PC2LogCategory.SIP, subCat, 
									"SIPManufacturer had error during SDP mod processing", pe);
						}
					}
				}
			}

			if (!success) {
				String method = "";
				if (message instanceof Request)
					method = ((Request)message).getMethod();
				else if (message instanceof Response) 
					method = ((Response)message).getStatusCode() + "-" + ((CSeqHeader)((Response)message).getHeader(CSeqHeader.NAME)).getMethod();
				logger.warn(PC2LogCategory.SIP, subCat, 
						"SIPManufacturer could not perform mod [" + mod + " to " +
						method + " message.");
			}
		}
		else if (content == null && 
				mod.getModType().equals("add") &&
				mediaType != null &&
				subType != null) {
			boolean success = false;
			try {
				ContentTypeHeader cth = utils.createContentTypeHeader(mediaType, subType);
				if (cth != null) {
					String newbody = refLocator.getReferenceInfo(fsmUID, mod.getRef(), null);
					if (newbody != null) {
						message.setContent(newbody, cth);
						success = true;
					}
				}
			}
			catch (Exception pe) {

			}
			if (!success) {
				String method = "";
				if (message instanceof Request)
					method = ((Request)message).getMethod();
				else if (message instanceof Response) 
					method = ((Response)message).getStatusCode() + "-" + ((CSeqHeader)((Response)message).getHeader(CSeqHeader.NAME)).getMethod();
				logger.warn(PC2LogCategory.SIP, subCat, 
						"SIPManufacturer could not perform mod [" + mod + " to " +
						method + " message.");
			}
		}
		else {

			String method = "";
			if (message instanceof Request)
				method = ((Request)message).getMethod();
			else if (message instanceof Response) 
				method = ((Response)message).getStatusCode() + "-" + ((CSeqHeader)((Response)message).getHeader(CSeqHeader.NAME)).getMethod();
			logger.warn(PC2LogCategory.SIP, subCat, 
					"SIPManufacturer could not perform mod [" + mod + " to " +
					method + " message.");
		}
	
	}
	
	protected String shortForm(String origMsg) {
		String result = new String(origMsg);
		
		// The process is fairly straight forward, simply 
		// place each occurrence of a header with their
		// compact or short form.
		// Allow-Events = u
		// Call-ID = i
		// Contact = m
		// Content-Encoding = e
		// Content-Length = l
		// Content-Type = c
		// Event = o
		// From = f
		// Refer-To = r
		// Subject = s
		// Supported = k
		// To = t
		// Via = v
		result = result.replaceAll("Allow-Events:", "u:");
		result = result.replaceAll("Call-ID:", "i:");
		result = result.replaceAll("Contact:", "m:");
		result = result.replaceAll("Content-Encoding:", "e:");
		result = result.replaceAll("Content-Length:", "l:");
		result = result.replaceAll("Content-Type:", "c:");
		result = result.replaceAll("Event:", "o:");
		result = result.replaceAll("From:", "f:");
		result = result.replaceAll("Refer-To:", "r:");
		result = result.replaceAll("Subject:", "s:");
		result = result.replaceAll("Supported:", "k:");
		result = result.replaceAll("To:", "t:");
		result = result.replaceAll("Via:", "v:");
		
		return result;
	}
}



