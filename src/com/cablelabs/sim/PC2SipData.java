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

import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.cablelabs.common.Transport;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.tools.SIPLocator;

/**
 * This container class holds the SipStack, SipProvider,
 * the Call Id, the To tag, the From tag, the transport
 * protocol and any other vital information that the 
 * SIPDistributor needs to maintain about a call flow 
 * with the peer device.
 * 
 * @author Garey Hassler
 *
 */
public class PC2SipData {

	/**
	 * The stack to use for this dialog
	 */
	private SipStack stack = null;
	
	/**
	 * The provider for this dialog
	 */
	private SipProvider provider = null;
	/**
	 * The to Tag for this dialog.
	 * 
	 */
	private String toTag = null;
	
	/**
	 * The from Tag for this dialog
	 */
	private String fromTag = null;
	
	/**
	 * The call id for this dialog.
	 */
	private String callId = null;
	
	/**
	 * The transport protocol being used for this
	 * dialog: UDP, TCP or TLS
	 */
	private Transport transport = null;
	
	/**
	 * This flag indicates whether this dialog was
	 * initiated by the Platform or not. It is used
	 * to control the construction of the To and 
	 * From Headers.
	 */
	private boolean ssInitiated = false;
	
	/**
	 * The listener that has subscribed to receive
	 * packets pertaining to this dialog
	 */
	private FSMListener listener = null;

	/**
	 * This flag is used to control the sending of the SDP Answer
	 * in a response. If the Answer hasn't been sent by the 200
	 * to Invite, it will be sent in the 200 message.
	 */
	private boolean sentSDPAnswer = false;
	
	/**
	 * This is a counter for the number of Notify messages sent in
	 * response to a REFER message. It controls the construction of
	 * the sipfrag body in the message. By default the value 100
	 * is sent in the first response and 200 is sent in the second
	 * response.
	 */
	private int notify2ReferCount = 0;
	
	private int finalResponse = 0;
	
	/**
	 * The reg-info body needs to increment the version number each
	 * time it sends a new document. This is the value it uses.
	 */
	private int regInfoCounter = -1;
	
	/**
	 * Each AOR of the device needs to have a unique id in the 
	 * id attribute of the registration tag in a reg-info body.
	 * This assignes it 
	 */
	private byte regInfoUniqueId = -1;
	
	/**
	 * This the global counter for assigning a unique id for the 
	 * reg-info body.
	 */
	private static byte uniqueId = 0;
	
	/**
	 * The key for this instance of SIP Dialog Information
	 */
	private String key = null;
	
	private SIPLocator locator = SIPLocator.getInstance();
	/**
	 * This contains the addr_spec portion of the Contact 
	 * header in the original Request for the dialog. It is also
	 * used for transactions that are outside of a dialog.
	 */
	private String origRequestContactAddrSpec = null;
	
	/**
	 * This contains the addr_spec portion of the Contact 
	 * header in the first Response to a dialog forming 
	 * request.
	 */
	private String origResponseContactAddrSpec = null;
	
	private boolean origAckSentRcvd = false;
	
	private int peerCommPort = -1;
	
	private String answerRecRouteHeaders = null;
	
	public static long SESSION_ID = 970829L;
	
	private long sessionVersion = -1;
	/**
	 * 
	 * @param callId
	 */
	
	public PC2SipData(String key, String callId, 
			SipProvider provider, String fromTag, Transport transport) {
		this.key = key;
		this.callId = callId;
		this.provider = provider;
		this.stack = provider.getSipStack();
		this.transport = transport;
		this.fromTag = fromTag;
		this.regInfoUniqueId = ++uniqueId;
	}
	
	public PC2SipData(String key, SipProvider provider, Transport transport) {
		this.key = key;
		this.provider = provider;
		this.stack = provider.getSipStack();
		this.transport = transport;
		this.regInfoUniqueId = ++uniqueId;
	}
	
	public String getFromTag() {
		return fromTag;
	}

	public void setFromTag(String fromTag) throws IllegalStateException {
		if (this.fromTag == null)
			this.fromTag = fromTag;
		else
			throw new IllegalStateException("Can't set the From tag for a dialog a second time.");
	}

	public SipProvider getProvider() {
		return provider;
	}

	public void setProvider(SipProvider provider) {
		this.provider = provider;
	}

	public SipStack getStack() {
		return stack;
	}

	public void setStack(SipStack stack) {
		this.stack = stack;
	}
	
	public String getToTag() {
		return toTag;
	}

	public void setToTag(String toTag) throws IllegalStateException {
		if (this.toTag == null)
			this.toTag = toTag;
		else
			throw new IllegalStateException("Can't set the To tag for a dialog a second time.");
	}

	public Transport getTransport() {
		return transport;
	}

	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	public boolean isTransportUDP() {
		if (transport == Transport.UDP) {
			return true;
		}
		return false;
	}
	
	public boolean isTransportTCP() {
		if (transport == Transport.TCP) {
			return true;
		}
		return false;
	}
	
	public boolean isTransportTLS() {
		if (transport == Transport.TLS) {
			return true;
		}
		return false;
	}

	public boolean isIntegrityProtected() {
		return isTransportTLS();
	}
	
	public String getCallId() {
		return callId;
	}
	
	public void setCallId(String callId) {
		this.callId = callId;
	}
	
	public String getDialogKey() {
		return this.key;
	}
	
	public void setDialogKey(String newKey) {
		this.key = newKey;
	}
	
	public boolean getSSInitiated() {
		return ssInitiated;
	}
	
	public void setSSInitiated(boolean flag) {
		this.ssInitiated = flag;
	}
	
	public void setListener(FSMListener listener) {
		this.listener = listener;
	}
	
	public FSMListener getListener() {
		return listener;
	}
	
	public boolean sentSDPAnswer() {
		return sentSDPAnswer;
	}
	
	public void setSentSDPAnswer(boolean flag) {
		this.sentSDPAnswer = flag;
	}
	
	public int getNotifyToReferCount() {
	    return this.notify2ReferCount;
	}
	
	public void incrementNotifyToReferCount() {
		this.notify2ReferCount++;
	}
	
	public int getFinalResponse() {
		return this.finalResponse;
	}
	
	public void setFinalResponse(int resp) {
		this.finalResponse = resp;
	}
	
	public boolean isDialogAccepted() {
		if (finalResponse >= 200 && finalResponse <= 299) 
			return true;
		return false;
	}
	
	public int getRegInfoVersion() {
		return (++this.regInfoCounter);
	}
	
	public byte getRegInfoUniqueId() {
		return this.regInfoUniqueId;
	}
	
	public String getRequestContact() {
		return this.origRequestContactAddrSpec;
	}
	
	public void setRequestContact(String msg) {
		// Only allow the value to be set once
		if (this.origRequestContactAddrSpec == null) {
			String c = "Contact";
			int [] hdrLocation = locator.locateSIPHeader(c, MsgQueue.FIRST, msg, false, false);
			if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
				int [] paramLocation = locator.locateSIPParameter(c, 
						SIPLocator.ADDR_SPEC, hdrLocation, msg);
				if (paramLocation[0] != -1 &&
						paramLocation[1] != -1 &&
						paramLocation[2] != -1) {
					this.origRequestContactAddrSpec = msg.substring(paramLocation[1], 
							paramLocation[2]);
				}
			}
		}
	}
	
	public String getResponseContact() {
		return this.origResponseContactAddrSpec;
	}
	
	public String getRespRecordRouteHeaders() {
		return this.answerRecRouteHeaders;
	}
	
	public void setResponseContact(String msg) {
		// Only allow the value to be set once
		if (this.origResponseContactAddrSpec == null) {
			String c = "Contact";
			int [] hdrLocation = locator.locateSIPHeader(c, MsgQueue.FIRST, msg, false, false);
			if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
				int [] paramLocation = locator.locateSIPParameter(c, 
						SIPLocator.ADDR_SPEC, hdrLocation, msg);
				if (paramLocation[0] != -1 &&
						paramLocation[1] != -1 &&
						paramLocation[2] != -1) {
					this.origResponseContactAddrSpec = msg.substring(paramLocation[1], 
							paramLocation[2]);
				}
			}
		}
	}
	
	public boolean getOrigAckSentRcvd() {
		return this.origAckSentRcvd;
	}
	
	public void setOrigAckSentRcvd() {
		this.origAckSentRcvd = true;
	}
	
	public void setPeerPort(javax.sip.message.Message msg) {
		if (msg instanceof Request) {
			Request request = (Request)msg;
			ContactHeader ch = (ContactHeader)request.getHeader(ContactHeader.NAME);
			if (ch != null) {
				URI contactURI = ch.getAddress().getURI();
				if (contactURI != null &&
						contactURI.isSipURI()) {
					SipURI cu = (SipURI)contactURI;
					this.peerCommPort = cu.getPort();
				}
			}
		}
		else if (msg instanceof Response) {
			if (peerCommPort == -1) {
				Response resp = (Response)msg;
				if (resp.getStatusCode() > 100) {
					ViaHeader via = (ViaHeader)resp.getHeader(ViaHeader.NAME);
					if (via != null) {
						int temp = via.getPort();
						if (temp == -1)
							peerCommPort = 5060;
					}
				}
			}
		}
	}
	
	public void setPeerPort(String modMsg) {
		// Only allow the value to be set once
		if (this.peerCommPort == -1) {
			String c = "Contact";
			int [] hdrLocation = locator.locateSIPHeader(c, MsgQueue.FIRST, modMsg, false, false);
			if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
				int [] paramLocation = locator.locateSIPParameter(c, 
						SettingConstants.PORT, hdrLocation, modMsg);
				if (paramLocation[0] != -1 &&
						paramLocation[1] != -1 &&
						paramLocation[2] != -1) {
					try {
						this.peerCommPort = Integer.parseInt(modMsg.substring(paramLocation[1], paramLocation[2]));
					}
					catch (NumberFormatException nfe) {
						this.peerCommPort = -1;
					}
							
				}
				else {
					// Since we couldn't find a port value, but we know the Contact header is present,
					// assume the system will use the default values based upon the transport
					if (transport == Transport.TLS)
						this.peerCommPort = 5061;
					else 
						this.peerCommPort = 5060;
					
				}
			}
		}
	}
	
	public int getPeerPort() {
		return this.peerCommPort;
	}

	public void setRespRecordRouteHeaders(String rrHeaders) {
		this.answerRecRouteHeaders = rrHeaders;
	}
	
	public long getSessionVersion() {
		if (sessionVersion == -1) {
			sessionVersion = SESSION_ID;
		}
		else 
			sessionVersion++;
		
		return sessionVersion;
	}
	
}

