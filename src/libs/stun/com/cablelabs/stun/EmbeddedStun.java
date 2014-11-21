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

import java.net.*;

/**
 * This is a data container for information between another 
 * stack that owns a socket and the Stun Server. When 
 * a SIP or another protocol detects a Stun message, it can 
 * request processing of the packet by the EmbeddedStunListener class.
 * That class stores important information about the request
 * in this class.
 * 
 * @author ghassler
 *
 */
public class EmbeddedStun {
	
	/**
	 * The incoming data packet.
	 */
	private DatagramPacket packet = null;
	
	/**
	 * The incoming message.
	 */
	private byte [] msg = null;
	
	/**
	 * The incoming message's length
	 */
	private int msgLen = -1;
	
	/**
	 * The UDP socket that the packet was received on.
	 */
	private DatagramSocket datagramSocket = null;
	
	/**
	 * The TCP socket that a message was received on.
	 */
	private Socket socket = null;

	/**
	 * The NetAccessPointDescriptor within the STUN stack.
	 */
	//private StunStack stack = StunStack.getInstance();
	
	/**
	 * A flag specifying that communications are using TCP
	 * or UDP.
	 */
	private boolean usingTCP = false;
	
	/**
	 * Constructor.
	 * @param packet - the data packet to process
	 * @param socket - the UDP socket the packet was received upon.
	 */
	public EmbeddedStun(DatagramPacket packet, DatagramSocket socket ) {
		this.packet = packet;
		this.datagramSocket = socket;
		this.socket = null;
		this.usingTCP = false;
		this.msgLen = -1;
	}
	
	/**
	 * Constructor.
	 * @param packet - the data packet to process
	 * @param socket - the UDP socket the packet was received upon.
	 */
	public EmbeddedStun(byte [] msg, int len, Socket socket ) {
		this.packet = null;
		this.datagramSocket = null;
		this.socket = socket;
		this.usingTCP = true;
		this.msg = msg;
		this.msgLen = len;
	}
	/**
	 * Get the packet
	 * 
	 */
	public DatagramPacket getPacket() {
		return packet;
	}
	/**
	 * Set the packet
	 * @param packet
	 */
	public void setPacket(DatagramPacket packet) {
		this.packet = packet;
	}
	
	/**
	 * Get the message data
	 */
	public byte [] getMsg() {
		return msg;
	}
	
	/**
	 * Get the message length
	 */
	public int getMsgLen() {
		return msgLen;
	}
	
	/**
	 * Get the UDP Socket
	 * 
	 */
	public DatagramSocket getDatagramSocket() {
		return datagramSocket;
	}
	
	/**
	 * Set the UDP Socket
	 * @param socket
	 */
	public void setDatagramSocket(DatagramSocket socket) {
		this.datagramSocket = socket;
		this.socket = null;
	}
	
	/**
	 * Get the TCP Socket
	 * 
	 */
	public Socket getSocket() {
		return socket;
	}
	
	/**
	 * Set the TCP Socket
	 * @param socket
	 */
	public void setSocket(Socket socket) {
		this.socket = socket;
		this.datagramSocket = null;
		this.usingTCP = true;
	}

	
	public boolean useTCP() {
		return this.usingTCP;
	}
}
