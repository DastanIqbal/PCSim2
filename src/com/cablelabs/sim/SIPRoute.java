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

import com.cablelabs.common.Transport;
import com.cablelabs.fsm.Proxy;
import com.cablelabs.fsm.Send;

public class SIPRoute extends Route {

	/**
	 * The SIP stack that owns the provider being used to send/receive the
	 * message.
	 */
	protected PC2SipStack stack = null;
	/**
	 * The specific provider of the transport layer.
	 */
	protected SipProvider provider = null;
	
	public SIPRoute(PC2SipStack stack, SipProvider provider, Send s,
			String srcNE, Transport transport) {
		super(s, srcNE, transport,
		provider.getListeningPoint().getHost(),
		provider.getListeningPoint().getPort());
		this.stack = stack;
		this.provider = provider;
	}
	
	public SIPRoute(PC2SipStack stack, SipProvider provider, Proxy p,
			String srcNE, Transport transport) {
		super(p, srcNE, transport,
				provider.getListeningPoint().getHost(),
				provider.getListeningPoint().getPort());
		this.stack = stack;
		this.provider = provider;
	}
	
	public void setPeerPort(PC2SipData sipData) {
		if (sipData != null &&
				sipData.getPeerPort() != -1)
			this.peerPort = sipData.getPeerPort();
	}
	

}
