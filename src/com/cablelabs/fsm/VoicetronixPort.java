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


/**
 * This class holdes the mapping of one voicetronix port to the network element label and specific line number. 
 * It is designed to accommodate multiple voicetronix cards, but initially we only expect one card per machine.
 * 
 * @author ghassler
 *
 */
public class VoicetronixPort {

	/**
	 * This is the network element label for this port on the Voicetronix board
	 */
	private String neLabel = null;
	
	/**
	 * This is the physical port number on the Voicetronix board
	 */
	private int port = -1;
	
	/**
	 * This is the board number (in the situation where there are multiple boards on the computer
	 */
	private int board = -1;
	
	/**
	 * This is the actual line on the UE that is connected to the Voicetronix port
	 */
	private int line = -1;
	
	/**
	 * Constructor
	 * @param neLabel - the network element label for the port
	 * @param line - the physical port on the voicetronix card (currently 0-3)
	 */
	public VoicetronixPort(int port, String neLabel, int line) {
		this.port = port;
		this.neLabel = neLabel;
		this.line = line;
        // default value for board
		this.board = 1;

	}
	
	/**
	 * Constructor
	 * @param neLabel - the network element label for the port
	 * @param line - the physical port on the voicetronix card (currently 0-3)
	 */
	public VoicetronixPort(int port, String neLabel, int line, int board) {
		this.port = port;
		this.neLabel = neLabel;
		this.line = line;
        // default value for board
		this.board = board;

	}
	
	public int getPort() {
		return port;
	}
	
	public String getNELabel() {
		return neLabel;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getBoard() {
		return board;
	}
}
