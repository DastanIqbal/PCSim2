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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AcceptEncodingHeader;
import javax.sip.header.AcceptHeader;
import javax.sip.header.AcceptLanguageHeader;
import javax.sip.header.AlertInfoHeader;
import javax.sip.header.AllowHeader;
import javax.sip.header.AuthenticationInfoHeader;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.CallInfoHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentEncodingHeader;
import javax.sip.header.ContentLanguageHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.InReplyToHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.MimeVersionHeader;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.OrganizationHeader;
import javax.sip.header.PriorityHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.ProxyRequireHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ReplyToHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UnsupportedHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sip.InspectionListener;
import com.cablelabs.tools.SIPLocator;

/**
 * This class performs the testing of all the steps identified in Appendix B
 * of the SIP ATP. It task is to log any errors with the contents of the message
 * and to report when an error was found to the class that invoked it.
 * 
 * @author ghassler
 *
 */
public class SIPInspector {

	private static SIPInspector inspector = null; 
	/**
	 * A SIPDistributor Logger
	 */
	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger(SIPDistributor.class);
	
	/**
	 * Listener for failures that occurred in the stack when processing the 
	 * message event.
	 */
	private InspectionListener listener = InspectionListener.getInstance();

	private static final String CRLF = "\r\n";
	
	private boolean inspectorAffectsTest = SystemSettings.getInstance().useInspector();

	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Inspector";

	/**
	 * This specifies how the inspector should conduct its examination of the SIP messages
	 * based upon the Platform acting as a CN or a UE. Valid values are CN or UE. By default
	 * it will be the CN.
	 * 
	 */
	private String type = "UE";

	private SIPLocator locator = SIPLocator.getInstance();
	
	private Hashtable<String, Integer> passTests = new Hashtable<String, Integer>();
	/**
	 * Private constructor of the singleton class.
	 *
	 */
	private SIPInspector() {
		// Determine what type of inspector we should be
		Properties p = SystemSettings.getSettings(SettingConstants.PLATFORM);
		if (p != null) {
			String t = p.getProperty(SettingConstants.SIP_INSPECTOR_TYPE);
			if (t != null)
				this.type = t;
		}
	}
	
	protected void clear() {
		passTests.clear();
	}
	
	/**
	 * Provides a reference to the class. Since the class
	 * is a singleton, if it doesn't already exist, it will
	 * create an instance and then return a reference to it.
	 * 
	 * @return the single instantiation of the class.
	 */
	
	public synchronized static SIPInspector getInstance() {
		if (inspector == null) {
			inspector = new SIPInspector();
		}	
		return inspector;
	}
	
	private String getInspectorType() {
		// Determine what type of inspector we should be
		Properties p = SystemSettings.getSettings(SettingConstants.PLATFORM);
		if (p != null) {
			String t = p.getProperty(SettingConstants.SIP_INSPECTOR_TYPE);
			if (t != null)
				return t;
			
		}
		
		return this.type;
	}
	
	/**
	 * This method validates each request message conforms to the requirements
	 * in Appendix B of the SIP-ATP
	 * 
	 * @param req - the request message that is associated to the response.
	 * @param insideDialog - the request message is apart of an existing dialog.
	 * @param integrityProtected - the request message was received on an 
	 * 		integrityProtected transport channel.
	 * @param dialogInitiatingRequest - the request message that started this
	 * 		dialog.
	 * @param origToTag - the original To tag assigned to this dialog.
	 * @param initiatedDialog - the platform initiated the dialog.
	 * @param respContact - the value of the Contact header used in the response.
	 * @param respRecRouteHeaders - the value of the Record-Route headers 
	 * 		used in the response.
	 * @param sipData - the data container for the dialog.
	 * @return - true if there is an error with the message, false otherwise
	 */
	public boolean inspect(Request req, boolean insideDialog, boolean integrityProtected,
			String dialogInitiatingRequest, String origToTag, boolean initiatedDialog, 
			String respContact, String respRecRouteHeaders, PC2SipData sipData) {
		boolean invalid = false;
		try {
			// We only invoke the actual operations when we are inspecting
			// a DUT of type UE and this class is inspecting as a CN
			inspectorAffectsTest = SystemSettings.getInstance().useInspector();
			
			String inspectorType = getInspectorType();
			logger.debug(PC2LogCategory.SIP, subCat, "Inspector inspect request message using type(" 
					+ inspectorType + ") affects test(" + inspectorAffectsTest);
			if (inspectorType.equals("UE")) {
				invalid = commonRequest(req, insideDialog, integrityProtected, 
						dialogInitiatingRequest, origToTag, initiatedDialog,
						respContact, respRecRouteHeaders, sipData);
			}
		}
		catch (Exception e) {
			// If we encountered an exception, something is wrong with the message
			// so log the event and return invalid.
			logger.warn(PC2LogCategory.SIP, subCat, 
					"Inspection encountered an exception while processing " +
					"request message, returning invalid.", e);
			invalid = true;
		}
		// Before returning the result, take the stackFailure flag into account
		invalid = invalid || listener.getStackFailure();
		// Reset the stackFailure flag
		listener.reset();
		return invalid;
	}
	
	/**
	 * This method validates each response message conforms to the requirements
	 * in Appendix B of the SIP-ATP
	 * 
	 * @param resp - the response message just received.
	 * @param req - the request message that is associated to the response.
	 * @param insideDialog - the request message is apart of an existing dialog.
	 * @param integrityProtected - the request message was received on an 
	 * 		integrityProtected transport channel.
	 * @param sipData - the data container for the dialog.
	 * @return - true if there is an error with the message, false otherwise
	 */
	public boolean inspect(Response resp, String req, 
			boolean insideDialog, boolean integrityProtected,
			PC2SipData sipData) {
		boolean invalid = false;
		try {
			// We only invoke the actual operations when we are inspecting
			// a DUT of type UE and this class is inspecting as a CN
			inspectorAffectsTest = SystemSettings.getInstance().useInspector();
			String inspectorType = getInspectorType();
			logger.debug(PC2LogCategory.SIP, subCat, "Inspector inspect request message using type(" 
					+ inspectorType + ") affects test(" + inspectorAffectsTest);
			if (inspectorType.equals("UE")) {
				invalid = commonResponse(resp, req, insideDialog, 
						integrityProtected, sipData);
			}
		}
		catch (Exception e) {
			// If we encountered an exception, something is wrong with the message
			// so log the event and return invalid.
			logger.warn(PC2LogCategory.SIP, subCat,
					"Inspection encountered an exception while processing " +
					"request message, returning invalid.", e);
			invalid = true;
		}
		// Before returning the result, take the stackFailure flag into account
		invalid = invalid || listener.getStackFailure();
		// Reset the stackFailure flag
		listener.reset();
		return invalid;
	}
	
	/**
	 * This method validates all of the tests that are common to all request
	 * messages. It also invokes other methods to test specific requirements
	 * on a specific type of message.
	 * 
	 * @param req - the request message that is associated to the response.
	 * @param insideDialog - the request message is apart of an existing dialog.
	 * @param integrityProtected - the request message was received on an 
	 * 		integrityProtected transport channel.
	 * @param origToTag - the original To Tag assigned to the dialog.
	 * @param initiatedDialog - the platform initiated the dialog.
	 * @param respContact - the value of the Contact header used in the response.
	 * @param respRecRouteHeaders - the value of the Record-Route headers 
	 * 		used in the response.
	 * @param sipData - the data container for the dialog.
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonRequest(Request req, boolean insideDialog, boolean integrityProtected,
			String origReq, String origToTag, boolean initiatedDialog, String respContact, 
			String respRecRouteHeaders, PC2SipData sipData) {
		boolean invalid = false;
		String method = req.getMethod();
		// Note the request message always returns the message as upper case even when it arrives
		// as lower case
		/*	Stack is performing this test now
  		if (!req.getSIPVersion().equals("SIP/2.0")) {
			logger.error(method + " message contains invalid version. (RFC336) ");
			invalid = true;
		}
		if (!req.getSIPVersion().equalsIgnoreCase("SIP/2.0")) {
			if (inspectorAffectsTest) logger.error(method + " message contains invalid version. " + 
					" The version is not in upper case. (RFC337) ");
			invalid = true;
		}*/
		if (req.getContent() != null && 
				(req.getHeader(ContentTypeHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						method + " message contains a message body but does " + 
				"contain the Content-Type header. (RFC350) ");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains a message body but does " + 
				"contain the Content-Type header. (RFC350) ");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC350");
		}
//		else if (inspectorAffectsTest) {
//			logger.info(PC2LogCategory.SIP, subCat,
//					"VERIFY PASSED" + " Requirement (RFC350) -\n\t(" 
//					+ req.getMethod() + "[current] contains Content-Type and a body)");
//		}
		
		CSeqHeader cseq = ((CSeqHeader)req.getHeader(CSeqHeader.NAME));
		if (cseq == null) { 
			String errMsg = "The value of the CSeq method parameter doesn't exist " + 
			"in the request message[method]. (RFC1120 && RFC356) ";
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
			else 
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
			invalid = true;
		}	
		else if	(!cseq.getMethod().equalsIgnoreCase(method)) {
			String errMsg = "The value of the response's CSeq method parameter [" 
				+ cseq.getMethod() + "doesn't match the method defined in the Request-Line[" 
				+ method + "]. (RFC364 && RFC634)";
			
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
			else 
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC356");
			updateTests("RFC1120");
			updateTests("RFC364");
			updateTests("RFC634");
		}
		
//		else if (inspectorAffectsTest) {
//			logger.info(PC2LogCategory.SIP, subCat,
//					"VERIFY PASSED" + " Requirement (RFC364, RFC634) -\n\t(" 
//					+ req.getMethod() + "[current] == CSeq[first].method)");
//		}
//AppB.2 Step 28 here // stack does this
		//else if ((long)cseq.getSequenceNumber() > (long)2147483648)
		if (method.equalsIgnoreCase("ACK")) {
			invalid = (commonProcB2Ack(req) || invalid);
		}
		else if (method.equalsIgnoreCase("BYE")) {
			invalid = (commonProcB2Bye(req) || invalid);
		}	
		else if (method.equalsIgnoreCase("CANCEL")) {
			invalid = (commonProcB2Cancel(req) || invalid);
		}			
		else if (method.equalsIgnoreCase("INVITE")) {
			invalid = (invalid || commonProcB2Invite(req) || invalid);
			if (!insideDialog)
				invalid = (commonProcB6(req, method) || invalid);
		}					
		else if (method.equalsIgnoreCase("OPTIONS") ) {
			invalid = (invalid || commonProcB2Options(req) || invalid);
			if (!insideDialog)
				invalid = (commonProcB6(req, method) || invalid);
		}							
		else if (method.equalsIgnoreCase("REGISTER")) {
			invalid = (commonProcB2Register(req) || invalid);
		}

		if (insideDialog) {
			// If the Request is assiociated to an existing dialog (insideDialog)
			// then it needs to be validated against the common procedures defined
			// in section 4 of Appendix B.
			if (origReq != null ) {
				// Now determine if the platform initiated the dialog or if the 
				// DUT did.
				if (initiatedDialog) {
					invalid = (commonProcB5(req, origReq, origToTag, 
							method, initiatedDialog) || invalid);
				}
				else {	
					invalid = (commonProcB4(req, origReq, origToTag, 
							method, initiatedDialog, respContact,
							respRecRouteHeaders, sipData.getFinalResponse()) || invalid);
				}
			}
			else {
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat,
							"Common procedures for a Request message within a dialog " + 
							"could not be completed because origReq=" + origReq + ". (RFC630)");
				else 
					logger.error(PC2LogCategory.SIP, subCat,
							"Common procedures for a Request message within a dialog " + 
							"could not be completed because origReq=" + origReq + ". (RFC630)");
				invalid = true;
			}
			
		}
		// Get the Authorization header to verify it doesn't contain
		// any duplicate parameters
		AuthorizationHeader ah = (AuthorizationHeader)req.getHeader(AuthorizationHeader.NAME);
		if (ah != null) {
			invalid = (requestAuthorizationHeader(ah, method) || invalid);
		}
		if (req.getHeader(CallIdHeader.NAME) == null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"The Call-ID header does not exist in the request " + 
				"message[" + method + "]. (RFC1120 and RFC356) ");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"The Call-ID header does not exist in the request " + 
			"message[" + method + "]. (RFC1120 and RFC356) ");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC356");
			updateTests("RFC1120");
		}
		if (req.getHeader(MaxForwardsHeader.NAME) == null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"The Max-Forwards header does not exist in the request " + 
				"message[" + method + "]. (RFC1120, RFC367 and RFC356) ");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"The Max-Forwards header does not exist in the request " + 
			"message[" + method + "]. (RFC1120, RFC367 and RFC356) ");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC356");
			updateTests("RFC367");
			updateTests("RFC1120");
		}
		URI uri = req.getRequestURI();
		String u = uri.toString();
		if (u.contains("@") && uri.isSipURI()) {
			SipURI suri = (SipURI)uri;
			if (suri.getUser().length() <= 0) {
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat,
						"The Request-URI field does not contain a user in the request " + 
						"message[" + method + "]. (RFC1077) ");
				else 
					logger.error(PC2LogCategory.SIP, subCat,
						"The Request-URI field does not contain a user in the request " + 
						"message[" + method + "]. (RFC1077) ");
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC1077");
			}
		}
		// The stack throws an exception and doesn't inform the inspector of the error
//		while (iter.hasNext()) {
//			String sp = ((ViaHeader)iter.next()).getProtocol();
//			if (sp == null || (!(sp.equals("UDP") || sp.equals("TCP")))) {
//				if (inspectorAffectsTest) else logger.error("The transport field of the Via Header is not valid for the request message[" +
//						method + "]. (RFC369)");
//				invalid = true;
//			}
//		}
		if (!integrityProtected) {
			ListIterator<Header> iter = req.getHeaders(ViaHeader.NAME);
			while(iter.hasNext()) {
				ViaHeader via = (ViaHeader)iter.next();
				Iterator<String> paramIter = via.getParameterNames();
				String rport = null;
				String rportValue = null;
				//boolean hasMagicCookie;
//				boolean hasBranchParam = false;
				String branch = null;
				while (paramIter.hasNext()) {
					String name = paramIter.next();
					if (name.equals("rport")) {
						rport = name;
						rportValue = via.getParameter("rport");
					}
					if (name.equals("branch")) {
						branch = via.getBranch();
					}
				}
				String inspectorType = getInspectorType();
				if (inspectorType.equals("CN")) {
					if (rport == null) {

						if (inspectorAffectsTest) 
							logger.fatal(PC2LogCategory.SIP, subCat,
									"The rport parameter of the Via header doesn't exist " + 
									"in the request message[" + method + 
							"]. (REQ8735 and RFC2029) ");
						else 
							logger.error(PC2LogCategory.SIP, subCat,
									"The rport parameter of the Via header doesn't exist " + 
									"in the request message[" + method + 
							"]. (REQ8735 and RFC2029) ");
						invalid = true;
					}
					else if (rportValue != null && rportValue.length() > 0) {
						if (inspectorAffectsTest) 
							logger.fatal(PC2LogCategory.SIP, subCat,
									"The rport parameter of the Via header contains a " + 
									" value in the request message[" + method + 
							"]. (REQ8735 and RFC2029) ");
						else 
							logger.error(PC2LogCategory.SIP, subCat,
									"The rport parameter of the Via header contains a " + 
									" value in the request message[" + method + 
							"]. (REQ8735 and RFC2029) ");
						invalid = true;
					}
					else if (inspectorAffectsTest) {
						updateTests("RFC2029");
						updateTests("REQ8735");
					}
				}
				if (branch == null) {
					if (inspectorAffectsTest) 
						logger.fatal(PC2LogCategory.SIP, subCat,
								"The branch parameter of the Via header doesn't exist " + 
								"in the request message[" + method + "]. (RFC370)");
					else 
						logger.error(PC2LogCategory.SIP, subCat,
							"The branch parameter of the Via header doesn't exist " + 
							"in the request message[" + method + "]. (RFC370)");
					invalid = true;
				}
						
				else if (!branch.substring(0,7).equals(SIPUtils.mc)) {
					if (inspectorAffectsTest) 
						logger.fatal(PC2LogCategory.SIP, subCat,
							"The branch parameter of the Via header doesn't begin with magic cookie " + 
							"in the request message[" + method + "]. (RFC372 and RFC1171)");
					else 
						logger.error(PC2LogCategory.SIP, subCat,
							"The branch parameter of the Via header doesn't begin with magic cookie " + 
							"in the request message[" + method + "]. (RFC372 and RFC1171)");
					invalid = true;
				}
				else if (inspectorAffectsTest) {
					updateTests("RFC372");
					updateTests("RFC1171");
					updateTests("REQ8735");
				}
			}
		}
		return invalid;
	}
	
	/**
	 * This method validates all of the tests that are common to all response
	 * messages. It also invokes other methods to test specific requirements
	 * on a specific type of message.
	 * 
	 * @param resp - the response message just received.
	 * @param req - the request message that is associated to the response.
	 * @param insideDialog - the request message is apart of an existing dialog.
	 * @param integrityProtected - the request message was received on an 
	 * 		integrityProtected transport channel.
	 * @param sipData - the data container for the dialog.
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonResponse(Response resp, String req, 
			boolean insideDialog, boolean integrityProtected, PC2SipData sipData) {
		boolean invalid = false;
		int statusCode = resp.getStatusCode();
		if (req == null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					"Inspection could not be conducted upon response[" + statusCode + 
			"] because system didn't find a matching request.");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"Inspection could not be conducted upon response[" + statusCode + 
					"] because system didn't find a matching request.");
			return true;
		}
			
/*	Stack is performing this test now
		 if (!resp.getSIPVersion().equals("SIP/2.0")) {
			if (inspectorAffectsTest) else logger.error("Response message contains invalid version. (RFC336) ");
			invalid = true;
		}
		if (!resp.getSIPVersion().equalsIgnoreCase("SIP/2.0")) {
			if (inspectorAffectsTest) else logger.error("Response message contains invalid version. " + 
					" The version is not in upper case. (RFC337) ");
			invalid = true;
		} 
*/
		if (resp.getContent() != null && 
				(resp.getHeader(ContentTypeHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					"Response message contains a message body but does " + 
				"contain the Content-Type header. (RFC350) ");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"Response message contains a message body but does " + 
			"contain the Content-Type header. (RFC350) ");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC350");
		}
		CSeqHeader cseq = ((CSeqHeader)resp.getHeader(CSeqHeader.NAME));
		if (cseq == null) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						"Response message does not contain the CSeq Header. " + 
				"(RFC1122 & RFC418) ");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"Response message does not contain the CSeq Header. " + 
			"(RFC1122 & RFC418) ");
			invalid = true;
		}
		else {
			if (inspectorAffectsTest) {
				updateTests("RFC418");
				updateTests("RFC1122");
			}
			// CSEQ must exist for any of these tests to be executed.
			String method = cseq.getMethod();
			String reqMethod = locator.getSIPParameter(SIPLocator.REQUEST_LINE, 
					"method", MsgQueue.FIRST, req); // req.getMethod();
			if (!method.equals(reqMethod)) {
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat,
						"Response message does not contain the same method value in the CSeq Header [" + 
						method + "] as the request message's [" + reqMethod + "]. (RFC364) ");
				else 
					logger.error(PC2LogCategory.SIP, subCat,
						"Response message does not contain the same method value in the CSeq Header [" + 
				method + "] as the request message's [" + reqMethod + "]. (RFC364) ");
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC364");
			}
			String respMethod = statusCode + "-" + method;
//			CSeqHeader reqCSeqHdr = (CSeqHeader)req.getHeader(CSeqHeader.NAME);
			String reqCSeq = locator.getSIPHeader(CSeqHeader.NAME, 
					MsgQueue.FIRST, req); // reqCSeqHdr.toString();
			String respCSeq = cleanHdr(cseq.toString());
			if (req != null && 
					(!respCSeq.equals(reqCSeq))) {
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat,
							statusCode + "-"+ method + "'s CSeq header[" + respCSeq +
							"] does not match the " 
							+ reqMethod + "'s CSeq header[" + reqCSeq + "." 
							+ " (RFC418) ");
				else 
					logger.error(PC2LogCategory.SIP, subCat,
							"The CSeq header in the response " + 
							"message[" + statusCode + "-"+ method + 
							"] value=[respCSeq] does not match the request message[" 
							+ reqMethod + "] CSeq header[" + reqCSeq + "." 
							+ " (RFC418) ");
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC418");
			}
			
			String reqCallId = locator.getSIPParameter(CallIdHeader.NAME, "value",
					MsgQueue.FIRST, req);
//			((CallIdHeader)req.getHeader(CallIdHeader.NAME)).toString();
			String respCallId = ((CallIdHeader)resp.getHeader(CallIdHeader.NAME)).getCallId().toString();
			if (!respCallId.equals(reqCallId)) {
				String errMsg = statusCode + "-" + method + 
				"'s Call-Id[" + respCallId + "] doesn't match the " 
				+ reqMethod + "'s Call-Id[" + reqCallId + "]. (RFC362)";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC362");
			}
			
			boolean verifyVia = false;
			if (method.equalsIgnoreCase("ACK")) {
				invalid = (commonProcB3Ack(resp, method, statusCode) || invalid);
				verifyVia = true;
			}
			else if (method.equalsIgnoreCase("BYE")) {
				invalid = (commonProcB3Bye(resp, method, statusCode) || invalid);
				verifyVia = true;
			}	
			else if (method.equalsIgnoreCase("CANCEL")) {
				invalid = (commonProcB3Cancel(resp, method, statusCode) || invalid);
				verifyVia = true;
			}			
			else if (method.equalsIgnoreCase("INVITE")) {
				invalid = (invalid || commonProcB3Invite(resp, method, statusCode) || invalid);
				verifyVia = true;
				if (statusCode > 100 && statusCode <= 200)
					invalid = (commonProcB7(resp, req, respMethod, reqMethod) || invalid);
			}					
			else if (method.equalsIgnoreCase("OPTIONS")) {
				invalid = (invalid || commonProcB3Options(resp, method, statusCode) || invalid);
				verifyVia = true;
				// OPTIONS is not a dialog forming request
//				if (statusCode > 100 && statusCode <= 200)
//					invalid = (commonProcB7(resp, req, respMethod, reqMethod) || invalid);
			}							
			else if (method.equalsIgnoreCase("REGISTER")) {
				invalid = (commonProcB3Register(resp, method, statusCode) || invalid);
				verifyVia = true;
			}
		
			if (resp.getHeader(CallIdHeader.NAME) == null) {
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat,
							"The Call-ID header does not exist in the response " + 
							"message[" + statusCode + "-"+ method + "]. (RFC1120 and RFC356) ");
				else 
					logger.error(PC2LogCategory.SIP, subCat,
						"The Call-ID header does not exist in the response " + 
						"message[" + statusCode + "-"+ method + "]. (RFC1120 and RFC356) ");
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC356");
				updateTests("RFC1120");
			}
			String reqFromHdr = locator.getSIPHeader(FromHeader.NAME, MsgQueue.FIRST, req);
			String respFromHdr = cleanHdr(resp.getHeader(FromHeader.NAME).toString());
			
			if (req != null) {
				  boolean fromInvalid = compareHdr(
						reqFromHdr, "request",
						respFromHdr, "response",
						"From", "From", 
						"RFC416", 1, 1);
				if (fromInvalid) {
					String errMsg = statusCode + "-"+ method +"'s From header[" + respFromHdr 
					+ "] does not match the " + reqMethod + "'s From header[" 
					+ reqFromHdr + "]. (RFC416) ";
					if (inspectorAffectsTest) 
						logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
					else 
						logger.error(PC2LogCategory.SIP, subCat, errMsg);
					invalid = fromInvalid || invalid;
				}
				else if (inspectorAffectsTest) {
					updateTests("RFC416");
				}
			}
			
			if (statusCode == 401 &&
					resp.getHeader(ProxyAuthenticateHeader.NAME) != null) {
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat,
							statusCode + "-" + method + 
					" message contains Proxy-Authenticate header. (RFC1126)");
				else 
					logger.error(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Proxy-Authenticate header. (RFC1126)");
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC1126");
			}
			
			if (verifyVia) {
				// ListIterator<Header> reqIter = req.getHeaders(ViaHeader.NAME);
				String [] reqVias = locator.getAllSIPHeader(ViaHeader.NAME, req);
				ListIterator<Header> respIter = resp.getHeaders(ViaHeader.NAME);
				int index=0;
				while (respIter.hasNext() && reqVias[index] != null) {
					String respVia = ((ViaHeader)respIter.next()).toString();
					invalid = compareHdr(reqVias[index], reqMethod, respVia, respMethod,
								ViaHeader.NAME, ViaHeader.NAME,  "RFC419 and RFC420",
								index+1, index+1) || invalid;
					index++;
				}
				if (respIter.hasNext()) {
					String errMsg = respMethod + "'s Via header instance[" + (index+1) 
					+ "] value=[" + respIter.next() + "] contains an unmatched header with the " 
					+ reqMethod + "'s Via instance[" 
					+ (index+1) +"]. (RFC419 and RFC420)";
					if (inspectorAffectsTest)
						logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
					else
						logger.error(PC2LogCategory.SIP, subCat, errMsg);
				}
				if (reqVias[index] != null) {
					String errMsg = reqMethod + "'s Via header instance[" + (index+1) 
					+ "] value=[" + reqVias[index] + "] contains an unmatched header with the " 
					+ respMethod + "'s Via instance[" 
					+ (index+1) +"]. (RFC419 and RFC420)";
					if (inspectorAffectsTest)
						logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
					else
						logger.error(PC2LogCategory.SIP, subCat, errMsg);
				}
					
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC419");
				updateTests("RFC420");
			}
		}
		
		return invalid;
	}
	
	public boolean requestAuthorizationHeader(AuthorizationHeader ah, String method ) {
		boolean invalid = false;
		int userName = 0;
		int realm = 0;
		int nonce = 0;
		int digestURI = 0;
		int dresponse = 0;
		int algorithm = 0;
		int cnonce = 0;
		int opaque = 0;
		int messageQop = 0;
		int nonceCount = 0;
		int authParam = 0;
		Iterator<String> iter = ah.getParameterNames();
		while(iter.hasNext()) {
			String param = iter.next();
			if (param.equals("username")) 
				userName++;
			else if (param.equals("realm"))
				realm++;
			else if (param.equals("nonce"))
				nonce++;
			else if (param.equals("digest-uri"))
				digestURI++;
			else if (param.equals("dresponse"))
				dresponse++;
			else if (param.equals("algorithm"))
				algorithm++;
			else if (param.equals("cnonce"))
				cnonce++;
			else if (param.equals("opaque"))
				opaque++;
			else if (param.equals("message-qop"))
				messageQop++;
			else if (param.equals("nonce-count"))
				nonceCount++;
			else if (param.equals("auth-param"))
				authParam++;
		}
		if (userName > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						method + " message contains " + userName 
						+ " username parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + userName 
					+ " username parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (realm > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						method + " message contains " + realm 
						+ " realm parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + realm 
					+ " realm parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (nonce > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					method + " message contains " + nonce 
					+ " nonce parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + nonce 
					+ " nonce parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (digestURI > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						method + " message contains " + digestURI 
						+ " digest-uri parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + digestURI 
					+ " digest-uri parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (dresponse > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					method + " message contains " + dresponse 
					+ " dresponse parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + dresponse 
					+ " dresponse parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (algorithm > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					method + " message contains " + algorithm 
					+ " algorithm parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + algorithm 
					+ " algorithm parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (cnonce > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					method + " message contains " + cnonce 
					+ " cnonce parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + cnonce 
					+ " cnonce parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (opaque > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					method + " message contains " + opaque 
					+ " opaque parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + opaque 
					+ " opaque parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (messageQop > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					method + " message contains " + messageQop 
					+ " message-qop parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + messageQop 
					+ " message-qop parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (nonceCount > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					method + " message contains " + nonceCount 
					+ " nonce-count parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + nonceCount 
					+ " nonce-count parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		if (authParam > 1) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						method + " message contains " + authParam 
						+ " auth-param parameters in the Authorization header. (RFC1142)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains " + authParam 
					+ " auth-param parameters in the Authorization header. (RFC1142)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1142");
		}
		return invalid;
	}
	
	/**
	 * This method tests the ACK message for the specific requirements defined in
	 * subsection 2 of Appendix B.
	 * 
	 * @param req - the request message.
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB2Ack(Request req) {
		boolean invalid = false;
		if (req.getHeader(AcceptHeader.NAME) != null) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
				"ACK message contains Accept header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Accept header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(AcceptEncodingHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"ACK message contains Accept-Encoding header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Accept-Encoding header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(AcceptLanguageHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"ACK message contains Accept-Language header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Accept-Language header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(AlertInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"ACK message contains Alert-Info header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Alert-Info header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(AllowHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"ACK message contains Allow header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Allow header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(CallInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"ACK message contains Call-Info header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Call-Info header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(ExpiresHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"ACK message contains Expires header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Expires header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(InReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
				"ACK message contains In-Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains In-Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(OrganizationHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"ACK message contains Organization header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Organization header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		if (req.getHeader(PriorityHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"ACK message contains Priority header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Priority header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"ACK message contains Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(SubjectHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"ACK message contains Subject header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Subject header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(SupportedHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"ACK message contains Supported header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"ACK message contains Supported header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		return invalid;
	}
	
	/**
	 * This method tests the BYE message for the specific requirements defined in
	 * subsection 2 of Appendix B.
	 * 
	 * @param req - the request message.
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB2Bye(Request req) {
		boolean invalid = false;
		if (req.getHeader(AlertInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"BYE message contains Alert-Info header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"BYE message contains Alert-Info header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(CallInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"BYE message contains Call-Info header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"BYE message contains Call-Info header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ContactHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"BYE message contains Contact header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"BYE message contains Contact header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ExpiresHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"BYE message contains Expires header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"BYE message contains Expires header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(InReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"BYE message contains In-Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"BYE message contains In-Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(OrganizationHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"BYE message contains Organization header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"BYE message contains Organization header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(PriorityHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"BYE message contains Priority header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"BYE message contains Priority header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"BYE message contains Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
						"BYE message contains Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(SubjectHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"BYE message contains Subject header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
						"BYE message contains Subject header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		return invalid;
	}
	
	/**
	 * This method tests the CANCEL message for the specific requirements defined in
	 * subsection 2 of Appendix B.
	 * 
	 * @param req - the request message.
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB2Cancel(Request req) {
		boolean invalid = false;
		if (req.getHeader(AcceptHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"CANCEL message contains Accept header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
						"CANCEL message contains Accept header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(AcceptEncodingHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"CANCEL message contains Accept-Encoding header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
						"CANCEL message contains Accept-Encoding header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(AcceptLanguageHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"CANCEL message contains Accept-Language header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
						"CANCEL message contains Accept-Language header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(AlertInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Alert-Info header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Alert-Info header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(AllowHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Allow header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Allow header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(CallInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Call-Info header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Call-Info header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ContactHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"CANCEL message contains Contact header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Contact header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ContentDispositionHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Content-Disposition header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Content-Disposition header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ContentEncodingHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Content-Encoding header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Content-Encoding header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ContentLanguageHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Content-Language header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Content-Language header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ContentTypeHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Content-Type header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Content-Type header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ExpiresHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"CANCEL message contains Expires header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Expires header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(InReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains In-Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains In-Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(MimeVersionHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains MIME-Version header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains MIME-Version header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(OrganizationHeader.NAME) != null) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Organization header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Organization header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(PriorityHeader.NAME) != null) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Priority header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Priority header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ProxyAuthorizationHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Proxy-Authorization header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Proxy-Authorization header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ProxyRequireHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Proxy-Require header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Proxy-Require header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(RequireHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Require header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Require header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(SubjectHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"CANCEL message contains Subject header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"CANCEL message contains Subject header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		return invalid;
	}
	
	/**
	 * This method tests the INVITE message for the specific requirements defined in
	 * subsection 2 of Appendix B.
	 * 
	 * @param req - the request message.
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB2Invite(Request req) {
		boolean invalid = false;
		if (req.getHeader(ContactHeader.NAME) == null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"INVITE message does not contain Contact header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"INVITE message does not contain Contact header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		return invalid;
	}
	
	/**
	 * This method tests the OPTIONS message for the specific requirements defined in
	 * subsection 2 of Appendix B.
	 * 
	 * @param req - the request message.
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB2Options(Request req) {
		boolean invalid = false;
		if (req.getHeader(AlertInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"OPTIONS message contains Alert-Info header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"OPTIONS message contains Alert-Info header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ExpiresHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"OPTIONS message contains Expires header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"OPTIONS message contains Expires header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(InReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"OPTIONS message contains In-Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"OPTIONS message contains In-Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(PriorityHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"OPTIONS message contains Priority header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"OPTIONS message contains Priority header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"OPTIONS message contains Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"OPTIONS message contains Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(SubjectHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"OPTIONS message contains Subject header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"OPTIONS message contains Subject header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		return invalid;
	}
	
	/**
	 * This method tests the REGISTER message for the specific requirements defined in
	 * subsection 2 of Appendix B.
	 * 
	 * @param req - the request message.
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB2Register(Request req) {
		boolean invalid = false;
		if (req.getHeader(AlertInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
			"REGISTER message contains Alert-Info header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"REGISTER message contains Alert-Info header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(InReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"REGISTER message contains In-Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"REGISTER message contains In-Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(PriorityHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"REGISTER message contains Priority header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"REGISTER message contains Priority header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(RecordRouteHeader.NAME) != null) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
				"REGISTER message contains Record-Route header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"REGISTER message contains Record-Route header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"REGISTER message contains Reply-To header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"REGISTER message contains Reply-To header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		if (req.getHeader(SubjectHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
				"REGISTER message contains Subject header. (RFC1124)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"REGISTER message contains Subject header. (RFC1124)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1124");
		}
		
		return invalid;
	}
	
	
	/**
	 * This method tests the response to an ACK message for the 
	 * specific requirements defined in subsection 3 of Appendix B.
	 * 
	 * @param resp - the response message just received.
	 * @param method - the method of the response's CSeq header
	 * @param statusCode - the statusCode of the response message
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB3Ack(Response resp, String method, 
			int statusCode) {
		boolean invalid = false;
		
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(AllowHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Allow header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Allow header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}

		return invalid;
	}
	
	/**
	 * This method tests the response to an BYE message for the 
	 * specific requirements defined in subsection 3 of Appendix B.
	 * 
	 * @param resp - the response message just received.
	 * @param method - the method of the response's CSeq header
	 * @param statusCode - the statusCode of the response message
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB3Bye(Response resp, String method, 
			int statusCode) {
		boolean invalid = false;
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(AcceptHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Accept header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Accept header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(AcceptEncodingHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Accept-Encoding header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Accept-Encoding header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(AcceptLanguageHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Accept-Language header. (RFC1126)");
				else
					logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Accept-Language header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 405 &&
				(resp.getHeader(AllowHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Allow header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Allow header. (RFC1122)");
			invalid = true;
		}
		if (resp.getHeader(CallInfoHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Call-Info header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Call-Info header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if ((statusCode >= 100 && statusCode < 200) &&
				(resp.getHeader(ContactHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Contact header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(ContactHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}

		if (resp.getHeader(ExpiresHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Expires header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Expires header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 423 &&
				(resp.getHeader(MinExpiresHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Expires header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Min-Expires header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(OrganizationHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Organization header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Organization header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 407 &&
				(resp.getHeader(ProxyAuthenticateHeader.NAME) == null)) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Proxy-Authenticate header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Proxy-Authenticate header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (resp.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Reply-To header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Reply-To header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 420 &&
				(resp.getHeader(UnsupportedHeader.NAME) == null)) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Unsupported header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Unsupported header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (statusCode == 401 &&
				(resp.getHeader(WWWAuthenticateHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain WWW-Authenticate header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain WWW-Authenticate header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		return invalid;
	}
	
	/**
	 * This method tests the response to an CANCEL message for the 
	 * specific requirements defined in subsection 3 of Appendix B.
	 * 
	 * @param resp - the response message just received.
	 * @param method - the method of the response's CSeq header
	 * @param statusCode - the statusCode of the response message
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB3Cancel(Response resp, String method, 
			int statusCode) {
		boolean invalid = false;
		if (((statusCode >= 200 && statusCode < 300) || 
				statusCode == 415) &&
				(resp.getHeader(AcceptHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Accept header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Accept header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (((statusCode >= 200 && statusCode < 300)  || 
				statusCode == 415) &&
				(resp.getHeader(AcceptEncodingHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Accept-Encoding header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Accept-Encoding header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (((statusCode >= 200 && statusCode < 300)  || 
				statusCode == 415) &&
				(resp.getHeader(AcceptLanguageHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Accept-Language header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Accept-Language header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(AllowHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Allow header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Allow header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(AuthenticationInfoHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Authentication-Info header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Authentication-Info header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(CallInfoHeader.NAME) != null) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Call-Info header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Call-Info header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		

		if ((statusCode >= 100 && statusCode < 200) &&
				(resp.getHeader(ContactHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Contact header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(ContactHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Contact header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (((statusCode >= 300 && statusCode < 400) ||
				(statusCode == 485)) &&
				(resp.getHeader(ContactHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(ContentDispositionHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Content-Disposition header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Content-Disposition header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(ContentEncodingHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Content-Encoding header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Content-Encoding header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(ContentLanguageHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Content-Language header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Content-Language header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(ContentTypeHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Content-Type header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Content-Type header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(ExpiresHeader.NAME) != null){
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Expires header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Expires header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 423 &&
				(resp.getHeader(MinExpiresHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Min-Expires header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Min-Expires header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(MimeVersionHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains MIME-Version header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains MIME-Version header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(OrganizationHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Organization header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Organization header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 407 &&
				(resp.getHeader(ProxyAuthenticateHeader.NAME) != null)) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Proxy-Authenticate header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Proxy-Authenticate header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Reply-To header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Reply-To header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(RequireHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Require header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Require header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 420 &&
				(resp.getHeader(UnsupportedHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Unsupported header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Unsupported header. (RFC1122)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 401 &&
				(resp.getHeader(WWWAuthenticateHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.error(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains WWW-Authenticate header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains WWW-Authenticate header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		if (statusCode == 407 &&
				(resp.getHeader(WWWAuthenticateHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains WWW-Authenticate header. (RFC1126)");
			else logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains WWW-Authenticate header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		return invalid;
	}

	/**
	 * This method tests the response to an INVITE message for the 
	 * specific requirements defined in subsection 3 of Appendix B.
	 * 
	 * @param resp - the response message just received.
	 * @param method - the method of the response's CSeq header
	 * @param statusCode - the statusCode of the response message
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB3Invite(Response resp, String method, 
			int statusCode) {
		boolean invalid = false;
		if (statusCode == 405 &&
				(resp.getHeader(AllowHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Allow header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Allow header. (RFC1122)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(ContactHeader.NAME) == null)) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Contact header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Contact header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (statusCode == 423 &&
				(resp.getHeader(MinExpiresHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Min-Expires header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Min-Expires header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		if (statusCode == 407 &&
				(resp.getHeader(ProxyAuthenticateHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Proxy-Authenticate header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Proxy-Authenticate header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (statusCode == 420 &&
				(resp.getHeader(UnsupportedHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Unsupported header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Unsupported header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (statusCode == 401 &&
				(resp.getHeader(WWWAuthenticateHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain WWW-Authenticate header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain WWW-Authenticate header. (RFC1122)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		return invalid;
	}
	
	/**
	 * This method tests the response to an OPTIONS message for the 
	 * specific requirements defined in subsection 3 of Appendix B.
	 * 
	 * @param resp - the response message just received.
	 * @param method - the method of the response's CSeq header
	 * @param statusCode - the statusCode of the response message
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB3Options(Response resp, String method, 
			int statusCode) {
		boolean invalid = false;
		if (statusCode == 405 &&
				(resp.getHeader(AllowHeader.NAME) == null)) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Allow header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Allow header. (RFC1122)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if ((statusCode >= 100 && statusCode < 200) &&
				(resp.getHeader(ContactHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(ExpiresHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Expires header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Expires header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		if (statusCode == 423 &&
				(resp.getHeader(MinExpiresHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Min-Expires header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Min-Expires header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 407 &&
				(resp.getHeader(ProxyAuthenticateHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Proxy-Authenticate header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Proxy-Authenticate header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (resp.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Reply-To header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Reply-To header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 420 &&
				(resp.getHeader(UnsupportedHeader.NAME) == null)) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Unsupported header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Unsupported header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (statusCode == 401 &&
				(resp.getHeader(WWWAuthenticateHeader.NAME) == null)) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain WWW-Authenticate header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain WWW-Authenticate header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		
		return invalid;
	}
	
	/**
	 * This method tests the response to an REGISTER message for the 
	 * specific requirements defined in subsection 3 of Appendix B.
	 * 
	 * @param resp - the response message just received.
	 * @param method - the method of the response's CSeq header
	 * @param statusCode - the statusCode of the response message
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB3Register(Response resp, String method, 
			int statusCode) {
		boolean invalid = false;
		if (statusCode == 405 &&
				(resp.getHeader(AllowHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Allow header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Allow header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if ((statusCode >= 100 && statusCode < 200) &&
				(resp.getHeader(ContactHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Contact header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 423 &&
				(resp.getHeader(MinExpiresHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Min-Expires header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Min-Expires header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (statusCode == 407 &&
				(resp.getHeader(ProxyAuthenticateHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Proxy-Authenticate header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Proxy-Authenticate header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if ((statusCode >= 200 && statusCode < 300) &&
				(resp.getHeader(RecordRouteHeader.NAME) != null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Record-Route header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Record-Route header. (RFC1126)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (resp.getHeader(ReplyToHeader.NAME) != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message contains Reply-To header. (RFC1126)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message contains Reply-To header. (RFC1126)");
			invalid = true;
		}	
		else if (inspectorAffectsTest) {
			updateTests("RFC1126");
		}
		
		if (statusCode == 420 &&
				(resp.getHeader(UnsupportedHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain Unsupported header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain Unsupported header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		if (statusCode == 401 &&
				(resp.getHeader(WWWAuthenticateHeader.NAME) == null)) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						statusCode + "-" + method + 
				" message does not contain WWW-Authenticate header. (RFC1122)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					statusCode + "-" + method + 
			" message does not contain WWW-Authenticate header. (RFC1122)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC1122");
		}
		
		return invalid;
	}

	/**
	 * This method tests a request messages sent inside a dialog (an INVITE, 
	 * SUBSCRIBE, or REFER message) for conformity to the original 
	 * dialog creating request message as defined in Appendix
	 * B subsection 4 of the SIP-ATP document.
	 * 
	 * 
	 * @param req - the request message just received.
	 * @param resp - the original dialog establishing resp message.
	 * @param origToTag - the original To Tag assigned to the dialog.
	 * @param method - the method of the request message.
	 * @param initiatedDialog - the platform initiated the dialog.
	 * @param respRecRouteHeaders - the Record-Route headers used in the response message.
	 * @param finalResp - the final status code for the dialog if it exists
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB4(Request req, String origReq, 
			String origToTag, String method, boolean initiatedDialog,
			String respContact, String respRecRouteHeaders, 
			int finalResp) {
		boolean invalid = false;
		String origMethod = locator.getSIPParameter(CSeqHeader.NAME, "method", 
				MsgQueue.FIRST, origReq); // origReq.getMethod();
		String newMethod = req.getMethod();
		ToHeader to = (ToHeader)req.getHeader(ToHeader.NAME);
		String reqToAddr = locator.getSIPParameter(ToHeader.NAME, "name-addr", 
				MsgQueue.FIRST, origReq);
		// ToHeader origTo = (ToHeader)origReq.getHeader(ToHeader.NAME);
		if (to != null && reqToAddr != null) {
			String toAddr = to.getAddress().toString();
			// Remove the leading < and > symbols so that we are only comparing
			// addr-specs
			reqToAddr = reqToAddr.replaceFirst("<", "");
			reqToAddr = reqToAddr.replaceFirst(">", "");
			toAddr = toAddr.replaceFirst("<", "");
			toAddr = toAddr.replaceFirst(">", "");
			
			if (!toAddr.equals(reqToAddr)){
				String errMsg = method + "'s To header URI[" + toAddr + "] doesn't match the " 
				+ origMethod + "'s To header URI[" + reqToAddr + "]. (RFC625)";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC625");
			}
			
			String toTag = to.getTag();
			if (toTag == null || !toTag.equals(origToTag)) {
				String errMsg = method 
				+ "'s To header tag parameter[" + toTag + "] doesn't match the " 
				+ origMethod + " message To tag[" 
				+ origToTag + "]. (RFC625 and RFC626)";
				if (inspectorAffectsTest)
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC625");
				updateTests("RFC626");
			}
			
		}
		else {
			if (to == null) {
				String errMsg = "Unable to locate To header in current " 
					+ method + " message. (RFC625 and RFC626)";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
			}
			if (reqToAddr == null) {
				String errMsg = "Unable to locate To header in the original " 
					+ origMethod + " message. (RFC625 and RFC626)";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
			}
			invalid = true;
		}
		
		FromHeader from = (FromHeader)req.getHeader(FromHeader.NAME);
		String reqFromAddr = locator.getSIPParameter(FromHeader.NAME, "addr-spec", 
				MsgQueue.FIRST, origReq);
		String fromAddr = locator.getSIPParameter(FromHeader.NAME, "addr-spec", 
				MsgQueue.FIRST, req.toString());
		if (from != null && reqFromAddr != null) {
			// Compare only the addr-spec to cover the condition one is name-addr
			// but peer is using addr-spec.
			
			//String fromAddr = from.getAddress().toString();
			
			if (fromAddr == null || !fromAddr.equals(reqFromAddr)){
				String errMsg = "The URI[" + fromAddr + "] in the From header of the " 
					+ method + " doesn't match original request's From header[" 
					+ reqFromAddr + "]. (RFC627)";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC627");
			}
			String fromTag = from.getTag();
			String origFromTag = locator.getSIPParameter(FromHeader.NAME, "tag", 
					MsgQueue.FIRST, origReq); //origFrom.getTag();
			if (fromTag == null || !fromTag.equals(origFromTag)) {
				String errMsg = method + "'s From tag parameter[" + fromTag + "] doesn't match the " 
					+ origMethod + "'s From tag[" + origFromTag + ". (RFC628)"; 
				if (inspectorAffectsTest)
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				invalid = true;
			}
			else if (inspectorAffectsTest) {
				updateTests("RFC628");
			}
		}
		else {
			if (from == null) {
				String errMsg = "Unable to locate From header in current " 
					+ method + " message. (RFC627 and RFC628)";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
			}
			if (reqToAddr == null) {
				String errMsg = "Unable to locate From header in the original "
					+ origMethod + " message. (RFC627 and RFC628)";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
			}
			
			invalid = true;
		}
			
		CallIdHeader callId = (CallIdHeader)req.getHeader(CallIdHeader.NAME);
//		CallIdHeader origCallId = (CallIdHeader)origReq.getHeader(CallIdHeader.NAME);
		String origCallId = locator.getSIPParameter(CallIdHeader.NAME, "value", 
				MsgQueue.FIRST, origReq);
		if (callId != null && 
				origCallId != null && 
				!callId.getCallId().equals(origCallId)) {
			String errMsg = method + "'s Call-Id[" + callId.getCallId()
				+ " doesn't match " + origMethod + "'s Call-Id[" 
				+ origCallId + ". (RFC630)";
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
			else 
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC630");
		}

		// For RFC 637 simply test that the Request-URI of any in dialog request
		// with the Contact header sent in a non-100 response and as long as the 
		// final response is a success response.
		if (finalResp < 299) {
			URI reqURI = req.getRequestURI();
			if (respContact != null) {
				String ru = reqURI.toString();
				StringTokenizer leftSpecTokens = new StringTokenizer(ru, ";");
				StringTokenizer rightSpecTokens = new StringTokenizer(respContact, ";");
				logger.debug(PC2LogCategory.SIP, subCat, 
				"SIPInspector comparing address and its' parameter information.");
				boolean error =  compareAddr(leftSpecTokens, origMethod,
						rightSpecTokens, newMethod, "Request-URI", "Contact", 
						"RFC637", ru, respContact, 1, 1);
				if (inspectorAffectsTest && !error) {
					updateTests("RFC637");
				}
				invalid = error || invalid;
			}
			else {
				logger.warn(PC2LogCategory.SIP, subCat,
						"The Inspector can't compare the Request-URI of the " + method 
						+ " to the Response Contact URI because the value is currently null" +
				". (RFC637)");
			}
		}
		if (finalResp == 200) {
			if (respRecRouteHeaders != null) {
				ListIterator<Header> rhIter = req.getHeaders(RouteHeader.NAME);
				StringTokenizer tokens = new StringTokenizer(respRecRouteHeaders, ",");
				int instance = 0;
				// First see if there are any elements in the list and move to the
				// end of the list as we need to compare it in the reverse order.
				int count = 0;
				while (rhIter.hasNext()) {
					count++;
					rhIter.next();
				}
				int ri = tokens.countTokens();
				while (tokens.hasMoreElements() && rhIter.hasPrevious()) {
					String token = tokens.nextToken();
					String route = ((RouteHeader)rhIter.previous()).toString();
					invalid = compareHdr(
							token, "response",
							route, newMethod,
							"Record-Route", "Route", 
							"RFC638", ++instance, ri--) || invalid ;
					count--;
				}
				String errMsg = null;
				if (count == 0 && ri > 0)
					errMsg = "Route Header Set in the request message[" + method 
					+ "] does not match the Record-Route Header Set of the " +
					" Record-Route header has more elements, (" + ri + "), than the Route-Set. (RFC638)";
				else if (count > 0 && ri == 0)
					errMsg = "Route Header Set in the request message[" + method 
					+ "] does not match the Recorde-Route Header Set of the " +
					"original dialog forming response because the Route Set has more "
					+ "elements, (" + count + "), than the Record-Route Set. (RFC638)";
				if (errMsg != null) {
					if (inspectorAffectsTest)
						logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
					else 
						logger.error(PC2LogCategory.SIP, subCat, errMsg);
					invalid = true;
				}
				else if (inspectorAffectsTest) {
					updateTests("RFC638");
				}
			}
		}
		
		return invalid;
	}
	/**
	 * This method tests a request message inside a dialog (an INVITE, 
	 * SUBSCRIBE, or REFER message) for conformity to the original 
	 * dialog creating request message as defined in Appendix
	 * B subsection 4 of the SIP-ATP document.
	 * 
	 * 
	 * @param req - the request message just received.
	 * @param resp - the original dialog initiating request message.
	 * @param origToTag - the original To Tag assigned to the dialog.
	 * @param method - the method of the request message.
	 * @param initiatedDialog - the platform initiated the dialog.
	 * 
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB5(Request req, String origReq, 
			String origToTag, String method, boolean initiatedDialog) {
		boolean invalid = false;
		String origMethod = locator.getSIPParameter(CSeqHeader.NAME, "method", 
				MsgQueue.FIRST, origReq); //		origReq.getMethod();
		String newMethod = req.getMethod();
		
		ListIterator<Header> rhIter = req.getHeaders(RouteHeader.NAME);
//		ListIterator<Header> origRRHIter = origReq.getHeaders(RecordRouteHeader.NAME);
		String [] origRRHeaders = locator.getAllSIPHeader(RecordRouteHeader.NAME, origReq);
		if (rhIter != null && origRRHeaders != null) {
			// invalid = compare(origRRHIter, origMethod, rhIter, newMethod, false,
			//		RecordRouteHeader.NAME, RouteHeader.NAME, "RFC487") || invalid;
			int index = 0;
			while (rhIter.hasNext() && origRRHeaders[index] != null) {
				String routeHdr = rhIter.next().toString();
				invalid = compareHdr(origRRHeaders[index], origMethod, 
						routeHdr, newMethod, RecordRouteHeader.NAME, RouteHeader.NAME, "RFC487", 
						index+1, index+1) || invalid;
				index++;
			}
			if (rhIter.hasNext()) {
				String errMsg = "Route Header Set in the request message[" + method 
				+ "] does not match because it is NOT empty and the Record-Route header of the " +
				"original dialog forming request which is empty. (RFC487)";
				if (inspectorAffectsTest)
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
			}
			if (origRRHeaders[index] != null) {
				String errMsg = "Route Header Set is empty in the request message[" + method 
					+ "] which does not match the Record-Route header of the " +
					"original dialog forming request which is not empty. (RFC487)";
				if (inspectorAffectsTest)
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
			}
			else if (inspectorAffectsTest && !rhIter.hasNext()) {
				updateTests("RFC487");
			}
		}
		else {
			String errMsg = null;
			if (rhIter == null && origRRHeaders != null)
				errMsg = "Route Header Set is empty in the request message[" + method 
				+ "] which does not match the Record-Route header of the " +
				"original dialog forming request which is not empty. (RFC487)";
			else if (rhIter != null && origRRHeaders == null)
				errMsg = "Route Header Set in the request message[" + method 
				+ "] does not match because it is NOT empty and the Record-Route header of the " +
				"original dialog forming request which is empty. (RFC487)";
			if (errMsg != null) {
				if (inspectorAffectsTest)
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				invalid = true;
			}
		}
		
		URI uri = req.getRequestURI();
//		ContactHeader contactHdr = (ContactHeader)origReq.getHeader(ContactHeader.NAME);
		String origURI = locator.getSIPParameter(ContactHeader.NAME, "addr-spec", 
				MsgQueue.FIRST, origReq);
		if (uri != null 
				&& origURI != null) {
//				URI origContact = contactHdr.getAddress().getURI();
				boolean error = compareHdr(uri.toString(), origMethod,
					origURI, newMethod, 
					"Contact's URI", "Request-URI", 
					"RFC489", 1, 1);
				if (inspectorAffectsTest && !error) {
					updateTests("RFC489");
				}
				invalid = error || invalid;
		}
		else if (uri == null && origURI != null) {
			String errMsg = "The Request-URI is empty in the request message[" + method 
			+ "] which does not match the Contact header of the orignal dialog forming" +
			" request which is not empty. (RFC489)";
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
			else 
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
		}
		else if (uri != null && origURI == null) {
			String errMsg = "The Request-URI is not empty in the request message[" + method 
			+ "] but does not match the Contact header of the orignal dialog forming" +
			" request which is empty. (RFC489)";
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
			else 
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
		}

		return invalid;
	}
	/**
	 * This method tests a request message outside a dialog an (INVITE 
	 * or OPTIONS) message for all of the tests defined in Appendix
	 * B subsection 6 of the SIP-ATP document.
	 * 
	 * @param resp - the response message just received.
	 * @param req - the request message that is associated to the response.
	 * @param method - the method of the response's CSeq header
	 * @param statusCode - the statusCode of the response message
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB6(Request req, String method) {
		boolean invalid = false;
		String toTag = ((ToHeader)req.getHeader(ToHeader.NAME)).getTag();
		String fromTag = ((FromHeader)req.getHeader(FromHeader.NAME)).getTag();
		if (toTag != null) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						method + " message contains tag parameter in the To header. (RFC360)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message contains tag parameter in the To header. (RFC360)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC360");
		}
		
		if (fromTag == null) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						method + " message does not contain tag parameter in the From header. (RFC361)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					method + " message does not contain tag parameter in the From header. (RFC361)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC361");
		}
		
		if (fromTag.length() <= 0) {
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat,
						"The tag parameter doesn't contain a value in the " +
						"From header of the " + method + " message. (RFC361)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"The tag parameter doesn't contain a value in the " +
					"From header of the " + method + " message. (RFC361)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC361");
		}
		ContactHeader contact = (ContactHeader)req.getHeader(ContactHeader.NAME);
		if (contact == null || !contact.getAddress().getURI().isSipURI()) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"The Contact Header does not contain a SIP or SIPS URI" +
				" in the " + method + " message. (RFC373)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"The Contact Header does not contain a SIP or SIPS URI" +
			" in the " + method + " message. (RFC373)");
			invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC373");
		}
		
		return invalid;
	}

	/**
	 * This method tests a non-100 provisional response or a 200 resposne to
	 * an INVITE or OPTIONS message for all of the tests defined in Appendix
	 * B subsection 7 of the SIP-ATP document.
	 * 
	 * @param resp - the response message just received.
	 * @param req - the request message that is associated to the response.
	 * @param respMethod - the method of the response's CSeq header
	 * @param reqMethod - the statusCode of the response message
	 * @return - true if there is an error with the message, false otherwise
	 */
	private boolean commonProcB7(Response resp, String req, String respMethod, 
			String reqMethod) {
		boolean invalid = false;
		String [] rrHdrs = locator.getAllSIPHeader(RecordRouteHeader.NAME, req); 
		ListIterator<Header> respIter = resp.getHeaders(RecordRouteHeader.NAME);
		int index = 0;
		while (respIter.hasNext() && rrHdrs[index] != null) {
			String respRRHdr = ((RecordRouteHeader)respIter.next()).toString();
			invalid = compareHdr(rrHdrs[index], reqMethod,
				respRRHdr, respMethod, RecordRouteHeader.NAME, 
				RecordRouteHeader.NAME, "(RFC481 && RFC482)", 
				index+1, index+1) || invalid;
			index++;
		}
		if (respIter.hasNext()) {
			String errMsg = respMethod + " contains more Record-Route headers than the " 
			+ reqMethod + " message. Unmatched header=[" + respIter.next() 
			+ "] (RFC481 & RFC483)";
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
			else
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
		}
		if (rrHdrs != null &&
				rrHdrs[index] != null) {
			String errMsg = reqMethod + " contains more Record-Route headers than the " 
			+ respMethod + " message. Unmatched header=[" + rrHdrs[index] 
			+ "] (RFC481 & RFC483)";
			if (inspectorAffectsTest)
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
			else
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
		}
		else if (inspectorAffectsTest && !respIter.hasNext()) {
			updateTests("RFC481");
			updateTests("RFC483");
		}
		
		ContactHeader contact = (ContactHeader)resp.getHeader(ContactHeader.NAME);
		if (contact == null) {
			
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
						"The Contact Header does not exist" +
						" in the " + respMethod + " message. (RFC483)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"The Contact Header does not exist" +
					" in the " + respMethod + " message. (RFC483)");
				invalid = true;
		}
		else if (!contact.getAddress().getURI().isSipURI()) {
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat,
					"The Contact Header does not contain a SIP or SIPS URI" +
					" in the " + respMethod + " message. (RFC484)");
			else 
				logger.error(PC2LogCategory.SIP, subCat,
					"The Contact Header does not contain a SIP or SIPS URI" +
					" in the " + respMethod + " message. (RFC484)");
					invalid = true;
		}
		else if (inspectorAffectsTest) {
			updateTests("RFC483");
			updateTests("RFC484");
		}
		return invalid;
	}
	
	/**
	 * 
	 * This method takes to list iterator and compares the values in each header for 
	 * any changes or additional parameters in the header from that sent in the request.
	 * 
	 * @param leftIter - An interator of the list of left operand to be evaluated
	 * @param leftMsgType - The type of message being compared. This should be
	 * 		'request', 'response', or method value.
	 * @param rightIter - An interator of the list of right operand to be evaluated
	 * @param rightMsgType - The type of message being compared. This should be
	 * 		'request', 'response', or method value.
	 * @param reverse - A flag indicating the evaluation should be made by reversing
	 * 		the order of the right operand
	 * @param leftHeaderName - The name of the left operand's header being evaluated.
	 * @param rightHeaderName - The name of the right operand's header being evaluated.
	 * @param reqNumber - This is the requirement number being evaluated. It should
	 * 		be in the SIP-UE ATP in Appendix B of the Common Procedures.   
	 * 
	 * @return - true if there is an error with the message, false otherwise.
	 */
/*	private boolean compare(ListIterator<Header> leftIter, String leftMsgType,
			ListIterator<Header> rightIter, String rightMsgType, boolean reverse, 
			String leftHeaderName,	String rightHeaderName, String reqNumber) {
		boolean done = false;
		boolean invalid = false;
		Header left = null;
		Header right = null;
		int rightInstance = 0;
		
		if (reverse) {
			// First we need to move to the last element in the list
			while (rightIter.hasNext()) {
				right = (Header)rightIter.next();
				rightInstance++;
			}
			// Now the right list should be at the last element
			if (leftIter.hasNext() && right != null) {
				left = (Header)leftIter.next();
				
			}
			else {
				String errMsg = "The "  
					+ leftHeaderName + " header in the " + leftMsgType 
					+ " message doesn't match the " 
					+ rightHeaderName + " header in the " 
					+ rightMsgType 
					+ " message. (" + reqNumber + ")";
				
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
							
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				done = true;
				invalid = true;
			}
		}
		else {
			if (leftIter.hasNext() && rightIter.hasNext()) {
				left = (Header)leftIter.next();
				right = (Header)rightIter.next();
			}
			else {
				String errMsg = "The "  
					+ leftHeaderName + " header in the " + leftMsgType 
					+ " message doesn't match the " 
					+ rightHeaderName + " header in the " 
					+ rightMsgType 
					+ " message. (" + reqNumber + ")";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
							
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				done = true;
				invalid = true;
			}
		}
		int instance = 0;
			
		while (!done) {
			++instance;
			int ri = instance;
			if (reverse)
				ri = --rightInstance;
			invalid = compareHdr(left.toString(), leftMsgType,
					right.toString(), rightMsgType, 
					leftHeaderName, rightHeaderName, // internalMethod,
					reqNumber, instance, ri) || invalid ;
			
			right = null;
			if (reverse && rightIter.hasPrevious())
				right = (Header)rightIter.previous();
			else if (rightIter.hasNext()) 
				right = (Header)rightIter.next();
			
			if (right == null) {
				done = true;
				if (leftIter.hasNext()) {
					String errMsg = "The "  
						+ leftHeaderName + " header in the " + leftMsgType 
						+ " message doesn't appear to have a match in the " 
						+ rightHeaderName + " header in the " 
						+ rightMsgType 
						+ " message. (" + reqNumber + ")";
					if (inspectorAffectsTest) 
						logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
					
					else 
						logger.error(PC2LogCategory.SIP, subCat, errMsg);
					
					invalid = true;	
				}
				
			}
			else {
				left = null;
				if (leftIter.hasNext())
					left = (Header)leftIter.next();

				if (left == null) {
					done = true;
					String errMsg = "The "  
						+ leftHeaderName + " header in the " + leftMsgType 
						+ " message doesn't appear to have a match in the " 
						+ rightHeaderName + " header in the " 
						+ rightMsgType 
						+ " message. (" + reqNumber + ")";
						if (inspectorAffectsTest) 
							logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
						
						else 
							logger.error(PC2LogCategory.SIP, subCat, errMsg);
						
						invalid = true;	
				}
			}
				
		}
		
		return invalid;
	}
*/	
	/**
	 * Compares to headers to see if they are equivalent or not.
	 * The expected format of the headers is an optionally present
	 * value separated by a space followed by a name-addr/addr-spec
	 * and any associated parameters
	 * 
	 * Examples that should work include items like the From header or Via header.
	 * 		From: from-spec
	 * 		from-spec = ( name-addr / addr-spec ) *( SEMI from-param )
	 * 		from-param  =  tag-param / generic-param
	 * 		tag-param   =  "tag" EQUAL token
	 *  
	 * @param left - The left hand operand for the evaluation
	 * @param leftMsgType - The type of message being compared. This should be
	 * 		'request', 'response', or method value.
	 * @param right - The right hand operand for the evaluation
	 * @param rightMsgType - The type of message being compared. This should be
	 * 		'request', 'response', or method value.
	 * @param leftHeaderName - The header name of the left operand being evaluated.
	 * @param rightHeaderName - The header name of the right operand being evaluated.
	 * @param reqNumber - This is the requirement number being evaluated. It should
	 * 		be in the SIP-UE ATP in Appendix B of the Common Procedures.
	 * @param leftInstance - the instance of the header is being used in the left side.
	 * @param rightInstance - the instance of the header is being used in the right side.
	 *  
	 * @return - true if an error is identified, false otherwise.
	 * 
	 */
	private boolean compareHdr(String left, String leftMsgType,
			String right, String rightMsgType, 
			String leftHeaderName, String rightHeaderName, 
			String reqNumber, int leftInstance, 
			int rightInstance) {
		boolean invalid = false;
		left = left.replaceAll(CRLF, "");
		// Remove the header name if it appears
		left = left.replaceFirst((leftHeaderName+": "), "");
		right = right.replaceAll(CRLF, "");
		// Remove the header name if it appears
		right = right.replaceFirst((rightHeaderName+": "), "");
		String leftValue = null;
		String rightValue = null;
		// First we need to tokenize on spaces in case there
		// is a display-name field or not
		StringTokenizer leftTokens = new StringTokenizer(left, " ");
		StringTokenizer rightTokens = new StringTokenizer(right, " ");
		if (leftTokens.hasMoreTokens() && rightTokens.hasMoreTokens()) {
			int leftCount = leftTokens.countTokens();
			int rightCount = rightTokens.countTokens();
			logger.debug(PC2LogCategory.SIP, subCat, 
					"SIPInspector comparing headers leftHdr=[" + leftHeaderName 
					+  "] leftCount=[" + leftCount + "] rightHdr=[" + rightHeaderName  
					 + "] and rightCount=[" + rightCount + "]");
			if (leftCount >=1 && leftCount <= 2 &&
					rightCount >= 1 && rightCount <= 2 &&
					leftCount == rightCount) {
												
				// Next we need to determine if there is only one element
				// or two as expected
				leftValue = leftTokens.nextToken();
				rightValue = rightTokens.nextToken();
				if  (leftCount == 2) {
					logger.debug(PC2LogCategory.SIP, subCat, 
							"SIPInspector comparing headers fields left=[" + leftValue + "] equals right=[" + rightValue + "]");
					if (!leftValue.equals(rightValue)) {
						String errMsg = "The "  
							+ getHeaderInstance(leftInstance) 
							+ " " + leftHeaderName + " header in the " + leftMsgType 
							+ " message doesn't match the " 
							+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
							+ rightMsgType 
							+ " message. (" + reqNumber + ")";
						if (inspectorAffectsTest) 
							logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
						
						else 
							logger.error(PC2LogCategory.SIP, subCat, errMsg);
						invalid = true;
					}
					else {
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIPInspector comparing headers fields matched. left=[" + leftValue + "] equals right=[" + rightValue + "]");
						
						leftValue = leftTokens.nextToken();
						rightValue = rightTokens.nextToken();
					}
				}
				if (!invalid) {
					// Next since we should be down to only the name-addr/addr-spec
					// create a new set of tokens based upon the '>'
					StringTokenizer leftAddrTokens = new StringTokenizer(leftValue, ">");
					StringTokenizer rightAddrTokens = new StringTokenizer(rightValue, ">");
					if (leftAddrTokens.hasMoreTokens() && rightAddrTokens.hasMoreTokens()) {
						int leftAddrCount = leftAddrTokens.countTokens();
						int rightAddrCount = rightAddrTokens.countTokens();
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIPInspector comparing address count left=[" + leftAddrCount + "] and right=[" + rightAddrCount + "]");
						if (leftAddrCount > 0 && 
								leftAddrCount <= 2 &&
								rightAddrCount > 0 && 
								rightAddrCount <= 2 &&
								leftAddrCount == rightAddrCount) {
							// Since the count match the should both be either name-addr
							// or addr-spec. The next thing to do is to determine which
							// type they are.
							 
							// We have a name-addr and possible parameters to the header
							// Create a new set of tokens based upon the first token.
							leftValue = leftAddrTokens.nextToken();
							rightValue = rightAddrTokens.nextToken();
							StringTokenizer leftSpecTokens = new StringTokenizer(leftValue, ";");
							StringTokenizer rightSpecTokens = new StringTokenizer(rightValue, ";");
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIPInspector comparing address and its' parameter information.");
							invalid = compareAddr(leftSpecTokens, leftMsgType,
									rightSpecTokens, rightMsgType, leftHeaderName, rightHeaderName, 
									//internalMethod, 
									reqNumber, leftValue, rightValue, 
									leftInstance, rightInstance) || invalid;
							if (leftAddrCount == 2) {
								leftValue = leftAddrTokens.nextToken();
								rightValue = rightAddrTokens.nextToken();
								// We know that there are parameters to the header, so
								// now verify them as well
								StringTokenizer leftParamTokens = new StringTokenizer(leftValue, ";");
								StringTokenizer rightParamTokens = new StringTokenizer(rightValue, ";");
//								 The next item is to compare the parameters since the address matches
								// Take all of the remaining tokens in the resp and create a linked list 
								// from it since order doesn't matter after the first token.
								LinkedList<String> rightParams = new LinkedList<String>();
								while (rightParamTokens.hasMoreTokens()) {
									rightParams.add(rightParamTokens.nextToken());
								}
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SIPInspector comparing parameters of the header.");
								invalid = compareParams(leftParamTokens, leftMsgType,
										rightParams, rightMsgType, leftHeaderName, rightHeaderName, 
										// internalMethod, 
										reqNumber, leftValue, rightValue, 
										leftInstance, rightInstance) || invalid;
							}
						}
						else {
							// This means one of the entries has the a name-addr while the
							// other has an addr-spec.
							String errMsg = "The "  
								+ getHeaderInstance(leftInstance) 
								+ " " + leftHeaderName + " header in the " + leftMsgType 
								+ " message doesn't match the " 
								+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
								+ rightMsgType 
								+ " message.\n [" + leftValue + "] != [" + rightValue + "]. (" + reqNumber + ")";
							if (inspectorAffectsTest) 
								logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
							
							else 
								logger.error(PC2LogCategory.SIP, subCat, errMsg);
							invalid = true;
						}
					}
					else {
						String errMsg = "The "  
							+ getHeaderInstance(leftInstance) 
							+ " " + leftHeaderName + " header in the " + leftMsgType 
							+ " message doesn't match the " 
							+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
							+ rightMsgType 
							+ " message.\n [" + leftValue + "] != [" + rightValue + "]. (" + reqNumber + ")";
						if (inspectorAffectsTest) 
							logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
						
						else 
							logger.error(PC2LogCategory.SIP, subCat, errMsg);
						invalid = true;
					}
				}
				else {
					String errMsg = "The "  
						+ getHeaderInstance(leftInstance) 
						+ " " + leftHeaderName + " header in the " + leftMsgType 
						+ " message doesn't match the " 
						+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
						+ rightMsgType 
						+ " message.\n [" + leftValue + "] != [" + rightValue + "]. (" + reqNumber + ")";
					if (inspectorAffectsTest) 
						logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
					
					else 
						logger.error(PC2LogCategory.SIP, subCat, errMsg);
					invalid = true;
				}
			}
			else {
				if (leftTokens.hasMoreTokens()) 
					leftValue = leftTokens.nextToken();
				if (rightTokens.hasMoreTokens()) {
					rightValue = rightTokens.nextToken();
				}

				String errMsg = "The "  
					+ getHeaderInstance(leftInstance) 
					+ " " + leftHeaderName + " header in the " + leftMsgType 
					+ " message doesn't match the " 
					+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
					+ rightMsgType 
					+ " message.\n [" + leftValue + "] != [" + rightValue + "]. (" + reqNumber + ")";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
				
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				invalid = true;
			}
		}
		else {
			
			if (leftTokens.hasMoreTokens()) 
				leftValue = leftTokens.nextToken();
			if (rightTokens.hasMoreTokens()) {
				rightValue = rightTokens.nextToken();
				String errMsg = "The "  
					+ getHeaderInstance(leftInstance) 
					+ " " + leftHeaderName + " header in the " + leftMsgType 
					+ " message doesn't match the " 
					+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
					+ rightMsgType 
					+ " message. [" + leftValue + "] != [" + rightValue + "]. (" + reqNumber + ")";
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
						
			else 
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
			}
			invalid = true;
		}
		return invalid;
	}
	
	/**
	 * 
	 * @param left - The tokenized list of the left operand name-addr
	 * @param leftMsgType - The type of message being compared. This should be
	 * 		'request', 'response', or method value.
	 * @param right - The tokenized list of the right operand name-addr
	 * @param rightMsgType  The type of message being compared. This should be
	 * 		'request', 'response' or method value.
	 * @param leftHeaderName - The header name of the left operand being evaluated.
	 * @param rightHeaderName - The header name of the right operand being evaluated.
	 * @param reqNumber - This is the requirement number being evaluated. It should
	 * 		be in the SIP-UE ATP in Appendix B of the Common Procedures.
	 * @param leftValue - The original untokenized left operand for logging an error
	 * @param rightValue - The original untokenized right operand for logging an error.
	 * @param leftInstance - the instance of the header is being used in the left side.
	 * @param rightInstance - the instance of the header is being used in the right side.
	 * @return - true if an error is encountered, false otherwise
	 */
	private boolean compareAddr(StringTokenizer left, String leftMsgType,
			StringTokenizer right, String rightMsgType,
			String leftHeaderName, String rightHeaderName, 
			String reqNumber, String leftValue, String rightValue,
			int leftInstance, int rightInstance) {
		boolean invalid = false;
		int leftCount = left.countTokens();
		int rightCount = right.countTokens();
		
		boolean ipv6 = false;
		// First see if the URI contains an IPv6 address
		if (leftCount > 2 && rightCount > 2 ||
				(leftValue.contains("[") && leftValue.contains("]") &&
				rightValue.contains("[") && rightValue.contains("]"))) {
			ipv6 = true;
		}
		else if (leftValue.contains("[") && !rightValue.contains("[") ||
				rightValue.contains("[") && !leftValue.contains("[")) {
			logger.warn(PC2LogCategory.SIP, subCat, 
					"SIPInspector comparing address information encountered left address appears to be an IPv6 address [" + leftValue 
					+ "] tokens while the right address does not =[" + rightValue + "].");
		}
		
		if ((leftCount > 0) && 
				(rightCount > 0) ) {
			// &&	leftCount <= rightCount) {
			String leftAddr = left.nextToken();
			String rightAddr = right.nextToken();
			if (leftAddr.charAt(0) == '<' &&
					rightAddr.charAt(0) == '<') {
				leftAddr = leftAddr.replaceFirst("<", "");
				rightAddr = rightAddr.replaceFirst("<", "");
			}
			
			// Next we need to separate the URI type, address and port
			StringTokenizer leftURI = new StringTokenizer(leftAddr, ":");
			StringTokenizer rightURI = new StringTokenizer(rightAddr, ":");
			
			// Now verify that the URI types match
			if (leftURI.hasMoreElements() && rightURI.hasMoreElements()) {
				String leftType = leftURI.nextToken();
				String rightType = rightURI.nextToken();
				logger.debug(PC2LogCategory.SIP, subCat, 
						"SIPInspector comparing leftType=[" + leftType + "] equals rightType=[" + rightType + "]");
				
				if (leftType.equals(rightType)) {
					logger.debug(PC2LogCategory.SIP, subCat, 
							"SIPInspector uri type matchs leftType=[" + leftType + "] equals rightType=[" + rightType + "]");
					
					if (leftURI.hasMoreElements() && rightURI.hasMoreElements()) {
						leftAddr = leftURI.nextToken();
						rightAddr = rightURI.nextToken();
						logger.debug(PC2LogCategory.SIP, subCat, 
								"SIPInspector comparing leftAddr=[" + leftAddr + "] equals rightAddr=[" + rightAddr + "]");

						if (leftAddr.equals(rightAddr)) {
							logger.debug(PC2LogCategory.SIP, subCat, 
									"SIPInspector addresses match leftAddr=[" + leftAddr + "] equals rightAddr=[" + rightAddr + "]");

							if (ipv6) {
								int l = leftURI.countTokens();
								int r = rightURI.countTokens();
								if (l == r) {
									for (int i=0; i <(l-1); i++) {
										String lt = leftURI.nextToken();
										String rt = rightURI.nextToken();
										if (!(lt.equals(rt))) {
											logger.error(PC2LogCategory.SIP, subCat, 
												"SIPInspector comparing address information encountered left address has [" + leftAddr 
												+ "] tokens while the right address has =[" + rightAddr + "] are not equal. [" 
												+ lt + " != " + rt + "].");
										}
									}
								}
							}
							// Next check the ports if available
							String leftPort = "5060";
							String rightPort = "5060";
							boolean check = false;
							if (leftURI.hasMoreElements()){
								check = true;
								leftPort = leftURI.nextToken();
							}
							if (rightURI.hasMoreElements()){
								check = true;
								rightPort = rightURI.nextToken();
							}
							
							
							
							// Now that the address are equal, test the ports if they exist
							if (check &&
									leftPort.equals(rightPort)) {
								logger.debug(PC2LogCategory.SIP, subCat, 
										"SIPInspector ports match leftPort=[" + leftPort + "] equals rightPort=[" + rightPort + "]");
							}
							else if (check) {
								logger.warn(PC2LogCategory.SIP, subCat, 
										"SIPInspector ports does not match leftPort=[" + leftPort + "] equals rightPort=[" + rightPort + "]");
							}
							
							//	 The final address check is to make sure there are no more tokens
							if (leftURI.hasMoreElements() || rightURI.hasMoreElements()) {
								logger.warn(PC2LogCategory.SIP, subCat, 
								"SIPInspector comparing address information encountered left address has [" + leftURI.countTokens() 
								+ "] tokens while the right address has =[" + rightURI.countTokens() + "] tokens remaining.");
							}
							
							// The next item is to compare the parameters since the address matches
							// Take all of the remaining tokens in the resp and create a linked list 
							// from it since order doesn't matter after the first token.
							LinkedList<String> rightParams = new LinkedList<String>();
							while (right.hasMoreTokens()) {
								rightParams.add(right.nextToken());
							}
							invalid = compareParams(left, leftMsgType, rightParams, 
									rightMsgType, leftHeaderName, rightHeaderName, // internalMethod, 
									reqNumber, leftValue, rightValue, leftInstance, rightInstance) || invalid;
						}
						else {
							String errMsg = "The name-addr/addr-spec field of the "  
								+ getHeaderInstance(leftInstance) 
								+ " " + leftHeaderName + " header in the " + leftMsgType 
								+ " message value=(" + leftAddr + ") doesn't match the name-addr/addr-spec field of the " 
								+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
								+ rightMsgType + " message value=(" 
								+ rightAddr + "). (" + reqNumber + ")";
							if (inspectorAffectsTest) 
								logger.fatal(PC2LogCategory.SIP, subCat, errMsg);

							else 
								logger.error(PC2LogCategory.SIP, subCat, errMsg);
							invalid = true;
						}
					}
				}
			}
		}
		else {
			String errMsg = "The " + getHeaderInstance(leftInstance) 
			+ " " + leftHeaderName + " header in the " + leftMsgType 
			+ " message has more parameters[" + leftValue + "] than the "
			+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
			+ rightMsgType 
			+ " message, parameters[" + rightValue + "]. (" + reqNumber + ")";
			if (inspectorAffectsTest) 
				logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
						
			else 
				logger.error(PC2LogCategory.SIP, subCat, errMsg);
			invalid = true;
		}
		
		return invalid;
	}
	
	/**
	 * This method loops throught the tokens of the left operand and determines
	 * if there is a matching entry in the right operand list. The method will
	 * mark a parameter as an error if the parameter is not an exact match. Also
	 * it will log any additional operand in the right list that did not get
	 * matched to a left operand.
	 * 
	 *	 
	 * @param leftTokens - The tokenized list of the left operand parameters
	 * @param leftMsgType - The type of message being compared. This should be
	 * 		'request', 'response', or method value.
	 * @param rightList - The list of parameters that comprise the right operand. 
	 * @param @param rightMsgType  The type of message being compared. This should be
	 * 		'request', 'response', or method value.
	 * @param leftHeaderName - The header name of the left operand being evaluated.
	 * @param rightHeaderName - The header name of the right operand being evaluated.
	 * @param reqNumber - This is the requirement number being evaluated. It should
	 * 		be in the SIP-UE ATP in Appendix B of the Common Procedures.
	 * @param leftValue - The original untokenized left operand for logging an error
	 * @param rightValue - The original untokenized right operand for logging an error.
	 * @param leftInstance - the instance of the header is being used in the left side.
	 * @param rightInstance - the instance of the header is being used in the right side.
	 *
	 * @return - true if an error is encountered, false otherwise
	 */
	private boolean compareParams(StringTokenizer leftTokens, String leftMsgType, 
			LinkedList<String> rightList, String rightMsgType,
			String leftHeaderName, String rightHeaderName, 
			String reqNumber, String leftValue, String rightValue,
			int leftInstance, int rightInstance) {
		boolean invalid = false;
		while (leftTokens.hasMoreTokens()) {
			String token = leftTokens.nextToken();
			ListIterator<String> iter = rightList.listIterator();
			boolean found = false;
			while (iter.hasNext() && !found) {
				String respParam = iter.next();
				logger.debug(PC2LogCategory.SIP, subCat, 
						"SIPInspector comparing token=[" + token + "] equals param=[" + respParam + "]");
				if (token.equals(respParam)) {
					found = true;
					// rightList.remove(respParam);
					iter.remove();
					logger.debug(PC2LogCategory.SIP, subCat, 
							"SIPInspector match found for token=[" + token + "] equals param=[" + respParam 
							+ "], removing=[" + respParam + "]");
				}
				// See if the original token had no value, but has been assigned a value by the DUT
				// e.g. the rport sent by the core may be empty, but filled in by an UE.
				else if (respParam.startsWith(token) && 
						respParam.length() > token.length() && 
						respParam.charAt(token.length()) == '=' ) {
					found = true;
					// rightList.remove(respParam);
					iter.remove();
					logger.debug(PC2LogCategory.SIP, subCat, 
							"SIPInspector match found for token=[" + token + "] equals param=[" + respParam 
							+ "], removing=[" + respParam + "]");
				}
			}
			if (!found) {
				String errMsg = "The " + getHeaderInstance(leftInstance) 
				+ " " + leftHeaderName + " header in the " + leftMsgType + " message does NOT match the "
				+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " + rightMsgType 
				+ " message.\nThe param[" + token 
				+ "] in the " + leftHeaderName + "[" 
				+ leftInstance + "]  could not be matched to a value the " 
				+ getHeaderInstance(rightInstance)  + " " + rightHeaderName 
				+ " parameters [" 
				+ rightValue + "]. (" + reqNumber + ")";
				if (inspectorAffectsTest) 
					logger.fatal(PC2LogCategory.SIP, subCat, errMsg);
							
				else 
					logger.error(PC2LogCategory.SIP, subCat, errMsg);
				invalid = true;
			}
		}
		ListIterator<String> iter = rightList.listIterator();
		String extraParams = null;
		while (iter.hasNext()) {
			if (extraParams == null)
				extraParams = iter.next();
			else 
				extraParams += ";" + iter.next();
		}
		
		if (extraParams != null) {
			String errMsg = "The " + getHeaderInstance(leftInstance) 
			+ " " + leftHeaderName + " header in the " + leftMsgType 
			+ " message does not contain the extra parameters[" + extraParams + 
			"] found in "
			+ getHeaderInstance(rightInstance) + " " + rightHeaderName + " header in the " 
			+ rightMsgType 
			+ " message.  (" + reqNumber + ")";
			logger.warn(PC2LogCategory.SIP, subCat, errMsg);
						
		}
		return invalid;
	}
	
	private String getHeaderInstance(int instance) {
		switch (instance) {
		case 1 :
			return "first";
		case 2:
			return "second";
		case 3:
			return "third";
		case 4:
			return "fourth";
		case 5:
			return "fifth";
		case 6:
			return "sixth";
		case 7:
			return "seventh";
		case 8:
			return "eighth";
		case 9:
			return "ninth";
		case 10:
			return "tenth";
		default :
			return (instance + "th");
		}
	}
	
	private String cleanHdr(String value) {
		int offset = value.indexOf(": ");
		if (offset != -1) {
			if (value.endsWith("\r\n"))
				value = value.replaceAll("\r\n", "");
			
			return value.substring((offset+2));
		}
		return value;
	}
	
	public void setInspection(boolean flag) {
		this.inspectorAffectsTest = flag;
	}
	
	public void updateTests(String requirement) {
		Integer count = passTests.get(requirement);
		if (count == null)
			count = new Integer(0);
		
		count++;
		return;
	}
}


