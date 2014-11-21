/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun;

import com.cablelabs.stun.attributes.ChannelNumber;
import com.cablelabs.stun.attributes.PeerAddress;

public class Binding {

	protected PeerAddress pa = null;
	protected ChannelNumber cn = null;
	
	public Binding(PeerAddress pa, ChannelNumber cn) {
		this.pa = pa;
		this.cn = cn;
	}
	
	public PeerAddress getPeerAddress() {
		return this.pa;
	}
	
	public ChannelNumber getChannelNumber() {
		return this.cn;
	}
}
