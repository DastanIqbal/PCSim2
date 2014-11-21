/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.utility;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class UtilityRawMessage {

	private byte [] message = null;
	private int messageLength = -1;
	private InetSocketAddress remoteAddress = null;
	private InetSocketAddress localAddress = null;
	
	public UtilityRawMessage(byte[] messageBytes, int messageLength,
               InetAddress remoteAddress, int remotePort,
               InetSocketAddress localSocket)    {
        this.message  = new byte[messageLength];
        this.messageLength = messageLength;
        System.arraycopy(messageBytes, 0, this.message,
                                                       0, messageLength);

        this.remoteAddress = new InetSocketAddress(remoteAddress, remotePort);
        this.localAddress = localSocket;
    }

	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	
	public byte[] getMessage() {
		return message;
	}

	public int getMessageLength() {
		return messageLength;
	}


	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

}
