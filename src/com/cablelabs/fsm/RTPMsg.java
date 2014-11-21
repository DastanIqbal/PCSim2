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

import com.cablelabs.common.Transport;

public class RTPMsg extends MsgEvent {

	byte [] rtp = null;
	
	private String payloadType = null;

	public RTPMsg(int uid, long time, int seq,
			Transport t,
			String srcIP, int srcPort, 
			String destIP, int destPort,
			String name, Boolean sent, byte [] data) {
		super(uid, time, seq, t, srcIP, srcPort, destIP, destPort, name, sent);
		this.rtp = data;
		setPayloadType();
	}
	
	public byte [] getRTP() {
		return this.rtp;
	}
	
	public String getPayloadType() {
		return this.payloadType;
	}
	
	private void setPayloadType() {
		if (rtp[1] == 0) 
			this.payloadType = "PCMU";
		else if (rtp[1] == 14)
			this.payloadType = "MPEG2";
		else 
			this.payloadType = "RTP";
	}
	
	public void setUID(int uid) {
		this.fsmUID = uid;
	}
}
