/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.tools;

import java.util.LinkedList;
import java.util.Properties;

import com.cablelabs.common.Transport;
import com.cablelabs.fsm.CaptureRef;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.GlobalVariables;
import com.cablelabs.fsm.Literal;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.PlatformRef;
import com.cablelabs.fsm.Reference;
import com.cablelabs.fsm.SDPRef;
import com.cablelabs.fsm.SIPBodyRef;
import com.cablelabs.fsm.SIPMsg;
import com.cablelabs.fsm.SIPRef;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.fsm.UtilityMsg;
import com.cablelabs.fsm.UtilityRef;
import com.cablelabs.fsm.VarExprRef;
import com.cablelabs.fsm.VarRef;
import com.cablelabs.fsm.Variable;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.models.PC2Models;

/**
 * This class provides a convenient way of retrieving a reference value. Given the 
 * fsmUID and the Reference class to identify the information to be retrieved and 
 * the getModReference method will return a String representation of the information
 * sought or null.
 *  
 * @author Garey Hassler
 *
 */
public class RefLocator {

	/**
	 * Private logger for the class
	 */
	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger("Locators");
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Locator";
	
	/**
	 * This class is a singleton.
	 */
	private static RefLocator locator = null;
	
	/**
	 * Local reference to the SIPLocator in order to obtain
	 * the SIP portion of a message.
	 */
	private SIPLocator sipLocator = SIPLocator.getInstance();
	
	/**
	 * Local reference to the SDPLocator in order to obtain 
	 * the SDP portion of a message.
	 */
	private SDPLocator sdpLocator = SDPLocator.getInstance();
	
	private CaptureLocator capLocator = CaptureLocator.getInstance();
	
	/**
	 * Local reference to the UtilityLocator in order to obtain 
	 * the Utility message information.
	 */
	private UtilityLocator utilLocator = UtilityLocator.getInstance();
	
	public static final String SRC_ADDRESS = "SourceAddress";
	
	public static final String DEST_ADDRESS = "DestinationAddress";
	
	public static final String IP = "IP";
	
	public static final String PORT = "Port";
	
	public static final String TIMESTAMP = "TIMESTAMP";
	
	public static final String TRANSPORT = "Transport";
	/**
	 * Private Constructor
	 *
	 */
	private RefLocator() {

	}
	
	/**
	 * Retreives the single instance of the RefLocator if it 
	 * already exists. If it doesn't exist it will create it prior
	 * to returning it.
	 *
	 */
	public synchronized static RefLocator getInstance() {
		if (locator == null) {
			locator = new RefLocator();
		}
		return locator;
	}
	
	/**
	 * This method returns the References data as a string to the invoking method
	 * 
	 * @param fsmUID - the unique ID of the reference
	 * @param ref - the reference information to retrieve
	 * @param curEvent - the current event being processed if applicable
	 * @return - a string representing the reference or null
	 */
	public synchronized String getReferenceInfo(int fsmUID, Reference ref, MsgEvent curEvent) {
		// We need to determine what is the current or last message being processed 
		// by the FSM. 
		int msgQueueIndex = 0;
		if (curEvent != null)
			msgQueueIndex = curEvent.getMsgQueueIndex();
		else {
			FSM f = FSM.getFSM(fsmUID);
			if (f != null)
				msgQueueIndex = f.getCurrentMsgQueueIndex();
			else
				msgQueueIndex = MsgQueue.getInstance().getLast();
		}
		
		if (ref instanceof Literal) {
			return ((Literal)ref).getExpr();
		}
		else if (ref instanceof VarExprRef) {
			return ((VarExprRef)ref).getExpr(msgQueueIndex);
		}
		else if (ref instanceof PlatformRef) {
			PlatformRef pr = (PlatformRef)ref;
			Properties p = SystemSettings.getSettings(pr.getNELabel());
			if (p != null) {
				if (pr.getParameter().startsWith(SettingConstants.VOICE_PORT)) {
					return pr.getParameter().substring(SettingConstants.VOICE_PORT.length());
				}
				else {
				 return p.getProperty(pr.getParameter());
				}
			}
		}
		// It is important that SDPRef and SIPBodyRef are tested before SIPRef since
		// they inherit from SIPRef
		else if (ref instanceof SDPRef) {
			SDPRef sdpRef = (SDPRef)ref;
			MsgQueue q = MsgQueue.getInstance();
			String method = sdpRef.getMethod();
			if (sdpRef.isSIPResponseRef())
				method = sdpRef.getStatusCode() + "-" + method;
			SIPMsg msg = null;
			if (sdpRef.getMsgInstance().equals(MsgQueue.CURRENT) &&
					curEvent instanceof SIPMsg)
				msg = (SIPMsg)curEvent;
			else
				msg = (SIPMsg)q.find(sdpRef.getUID(), method, 
						sdpRef.getMsgInstance(),msgQueueIndex);
			if (msg != null) {
				String sdp = null;
				if (sdpRef.isSIPResponseRef()) {
					Object content = msg.getResponse().getContent();
					if (content != null) {
						if (content instanceof String)
							sdp = (String)content;
						else if (content instanceof byte[]) {
							byte [] array = (byte[])content;
							sdp = new String(array);
						}
						else
							sdp = content.toString();
					}
				}
				else	{
					Object content = msg.getRequest().getContent();
					if (content instanceof String)
						sdp = (String)content;
					else if (content instanceof byte[]) {
						byte [] array = (byte[])content;
						sdp = new String(array);
					}
					else
						sdp = content.toString();
				}

				if (sdp != null && sdpRef.getHeader() != null) {
					String boundary = null;
					if (sdp.startsWith("--")) {
						int end = sdp.indexOf("\r\n");
						boundary = sdp.substring(2, end);
					}
					int [] hdrLocation = sdpLocator.locateSDPHeader(sdp, 
							sdpRef.getHdrInstance(), sdpRef.getBodyInstance(), boundary, sdpRef.getHeader());
					if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
						if (sdpRef.getParameter() != null) {
							int [] paramLocation = sdpLocator.locateSDPParam(sdpRef.getHeader(), 
									sdpRef.getParameter(), hdrLocation, sdp);
							if (paramLocation[0] != -1 && paramLocation[1] != -1)
								return sdp.substring(paramLocation[0], paramLocation[1]);
						}
						else 
							return sdp.substring(hdrLocation[0], hdrLocation[1]);
					}
				}
				else 
					return sdp;
			}
			else {
				logger.warn(PC2LogCategory.PCSim2, subCat, 
						"RefLocator failed to find reference in message queue. Reference=" + ref);
			}
		}
		else if (ref instanceof SIPBodyRef) {
			SIPBodyRef sipBodyRef = (SIPBodyRef)ref;
			MsgQueue q = MsgQueue.getInstance();
			String method = sipBodyRef.getMethod();
			if (sipBodyRef.isSIPResponseRef())
				method = sipBodyRef.getStatusCode() + "-" + method;
			SIPMsg msg = null;
			if (sipBodyRef.getMsgInstance().equals(MsgQueue.CURRENT) &&
					curEvent instanceof SIPMsg)
				msg = (SIPMsg)curEvent;
			else
				msg = (SIPMsg)q.find(sipBodyRef.getUID(), sipBodyRef.getMethod(), 
					sipBodyRef.getMsgInstance(), msgQueueIndex);
			if (msg != null) {
				Object xml = null;
				if (msg.hasSentMsg()) {
					xml = msg.getSentMsg();
					int bodyIndex = ((String)xml).indexOf("\r\n\r\n");
					if (bodyIndex != -1)
						xml = ((String)xml).substring(bodyIndex+4);
				}
				else if (sipBodyRef.isSIPResponseRef()) 
					xml = msg.getResponse().getContent();
				else	
					xml = msg.getRequest().getContent();
				if (xml instanceof byte [])
					xml = new String((byte [])xml);
				if (xml != null && xml instanceof String) {
					String body = xml.toString();
					if (sipBodyRef.hasAncestor() && sipBodyRef.isXMLValue()) {
						return sipLocator.getXMLAncestor(body, 
								sipBodyRef.getAncestor(), sipBodyRef.getType(),
								sipBodyRef.getHeader());
					}
					else {
						int [] location = sipLocator.locateSIPBody(sipBodyRef.getType(), 
								sipBodyRef.getHeader(), sipBodyRef.getHdrInstance(),
								sipBodyRef.getParameter(), sipBodyRef.isXMLValue(),
								body, false);
						if (location[0] != -1 && location[1] != -1) {
							return body.substring(location[0], location[1]);
						}
					}
				}
			}
		}
		else if (ref instanceof SIPRef) {
			SIPRef sipRef = (SIPRef)ref;
			MsgQueue q = MsgQueue.getInstance();
			String method = sipRef.getMethod();
			if (sipRef.isSIPResponseRef())
				method = sipRef.getStatusCode() + "-" + method;
			SIPMsg msg = null;
			if (sipRef.getMsgInstance().equals(MsgQueue.CURRENT) && 
					curEvent != null &&
					curEvent instanceof SIPMsg)
				msg = (SIPMsg)curEvent;
			else
				msg =(SIPMsg)q.find(sipRef.getUID(), method, 
						sipRef.getMsgInstance(), msgQueueIndex);
			if (msg != null) {
				String sip = null;
				if (sipRef.isSIPResponseRef()) {
					if (msg.hasSentMsg())
						sip = msg.getSentMsg();
					else
						sip = msg.getResponse().toString();
				}
				else {	
					if (msg.hasSentMsg())
						sip = msg.getSentMsg();
					else
						sip = msg.getRequest().toString();
				}
				if (sip != null && sipRef.getHeader() != null) {
					int [] hdrLocation = sipLocator.locateSIPHeader(sipRef.getHeader(), 
							sipRef.getHdrInstance(), sip, false, false);
					if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
						if (sipRef.getParameter() != null) {
							int [] paramLocation = sipLocator.locateSIPParameter(sipRef.getHeader(), 
									sipRef.getParameter(), hdrLocation, sip);
							if (paramLocation[0] != -1 && paramLocation[1] != -1 && paramLocation[2] != -1) {
								return sip.substring(paramLocation[1], paramLocation[2]);
							}
						}
						else if (sipRef.getParameter() == null){
							return sip.substring(hdrLocation[0], hdrLocation[1]);
						}
					}
				}
				else if (sipRef.getParameter() == null) {
					return sip;
				}
				else {
					logger.warn(PC2LogCategory.PCSim2, subCat,
							"RefLocator failed to find MOD reference in message queue.");
				}
			}
		}
		else if (ref instanceof UtilityRef) {
			UtilityRef utilRef = (UtilityRef)ref;
			MsgQueue q = MsgQueue.getInstance();
			String type = utilRef.getMsgType();
			MsgEvent event = q.find(utilRef.getUID(), type, 
					utilRef.getMsgInstance(), msgQueueIndex);
			if (event != null && event instanceof UtilityMsg) {
				UtilityMsg msg = (UtilityMsg)event;
				if (utilRef.getHeader() != null) {
					return utilLocator.locateUtilityValue(utilRef, msg);
				}
				else {
					logger.warn(PC2LogCategory.PCSim2, subCat,
							"RefLocator failed to find MOD reference in message queue.");
				}
			}
		}
		else if (ref instanceof VarRef) {
			GlobalVariables gv = GlobalVariables.getInstance();
			VarRef vr = (VarRef)ref;
			Variable var = gv.get(vr.getName());
			if (var != null) {
				Integer [] ndxs = vr.getIndexes();
				if (ndxs != null) {
					// This should return a specific element from the array
					return var.getElement(ndxs);
				}
				else {
					// In this case there should only be one element to
					// return to the invoking method.
					
					// Now we need to see if it is a container for a complete
					// message that needs further resolving to a specific value.
					String value = vr.resolve(var);
					
					return value;
				}
			}
		}
		else if (ref instanceof CaptureRef) {
			CaptureRef cr = (CaptureRef)ref;
			PacketDatabase db = PC2Models.getCaptureDB(cr.getDBName());
			LinkedList<String> result = capLocator.getReferenceInfo(db, cr);
			return result.getFirst();
		}
		else {
			logger.warn(PC2LogCategory.PCSim2, subCat,
					"RefLocator invalid MOD reference.");
		}
		return null;
	}
	
	/**
	 * This method obtains details about the actual event, not the
	 * underlying message.
	 */
	public String getEventReference(String hdr, String param, MsgEvent event) {
		if (hdr.equals(SRC_ADDRESS)) {
			if (param == null)
				logger.warn(PC2LogCategory.Examiner, subCat, 
						"The SourceAddress header reference requires" 
						+ " the parameter value be set as well. It is currently null.");
			else if (param.equalsIgnoreCase(IP)) {
				return event.getSrcIP();
			}
			else if (param.equalsIgnoreCase(PORT)) {
				return ((Integer)event.getSrcPort()).toString();
			}
		}
		else if (hdr.equals(DEST_ADDRESS)) {
			if (param == null)
				logger.warn(PC2LogCategory.Examiner, subCat, 
					"The SourceAddress header reference requires" 
					+ " the parameter value be set as well. It is currently null.");
			else if (param.equalsIgnoreCase(IP)) {
				return event.getDestIP();
				
			}
			else if (param.equalsIgnoreCase(PORT)) {
				return((Integer)event.getDestPort()).toString();
			}
		}
		else if (hdr.equals(TIMESTAMP)) {
			logger.info(PC2LogCategory.Examiner, subCat, event.getEventName() + "'s Sequencer=" 
					+ event.getSequencer() + " TIMESTAMP=" + event.getTimeStamp());
			return Long.toString(event.getTimeStamp());
		}
		else if (hdr.equals(TRANSPORT)) {
			Transport t = event.getTransport();
			if (t == null)
				return "NONE";
			else
				return t.toString();
		}
		return null;
	}
	
}
