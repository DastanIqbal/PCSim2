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
 * The interface class for an FSM to request a specific Action to be executed
 * by the system. It is the mechanism used by an FSM to communicate with a 
 * specific protocol stack or another FSM.
 * 
 * @author ghassler
 *
 */
public interface FSMAPI {

	/**
	 * Assigns the information defined by the reference to a previously
	 * declared variable.
	 * 
	 * @param a
	 * @return - true if successful, false otherwise
	 */
	public boolean assign(Assign a, int msgQueueIndex);
	
	/**
	 * Starts or Stops an packet capture of the network.
	 * 
	 * @param c
	 * @return - true if successful, false otherwise
	 */
	public boolean capture(Capture c);
	
	/**
	 * Allows PresenceServer model to change the status of 
	 * a simulated device's presence status.
	 * 
	 * @param ne - the network element label to change.
	 * @param status - the new status of the device.
	 */
	public boolean changeStatus(String ne, PresenceStatus status);
	
	/**
	 * Creates a global variable for a FSM to store data and 
	 * retrieve during a test.
	 * @return
	 */
	public boolean createVariable(Variable v, int msgQueueIndex);
	
	/**
	 * This method allows an Action to get the FSM's UID
	 * value.
	 * @return
	 */
	public int getFsmUID();
	
	/**
	 * Logs the requested message to the console and the log file.
	 * @param msg
	 * @return - true if successful, false otherwise
	 */
	public boolean log(LogMsg msg);
	
	/**
	 * Updates the test cases final result with new information of 
	 * still passing or failed.
	 * @param r
	 * @return - true if successful, false otherwise
	 */
	public boolean pass(Result r);
	
	/**
	 * Delivers the specific protocol message to the correct stack implementation
	 * for construction and delivery.
	 * 
	 * @param s
	 * @return - true if successful, false otherwise
	 */
	public boolean proxy(Proxy p);
	
	/**
	 * Delivers the specific protocol message to the correct stack implementation
	 * for construction and delivery.
	 * 
	 * @param s
	 * @return - true if successful, false otherwise
	 */
	public boolean send(Send s);
	
	/**
	 * Has the FSM go to sleep.
	 * @param s
	 * @return - true if successful, false otherwise
	 */
	public boolean sleep(Sleep s);
	
	/**
	 * Streams the contents of a file that was recorded in the 
	 * proper format and retransmits it to a specific IP address
	 * and port.
	 * 
	 * @param s - the information pertaining to the stream 
	 * @return - true if successful, false otherwise
	 */
	public boolean stream(Stream s, int msgQueueIndex);
	
	/**
	 * Retransmits the last message for the given protocol 
	 * 
	 * @param r
	 * @param msgQueueIndex
	 * @return - true if successful, false otherwise
	 */
	public boolean retransmit(Retransmit r, int msgQueueIndex);
	
	/**
	 * Verify operation that evaluates criteria for a test
	 * @param v - the Verify
	 * @param msgQueueIndex - the current message queue's index
	 * 
	 * @return - true if successful, false otherwise
	 */
	public boolean verify(Verify v, int msgQueueIndex);
	
	/**
	 * The interface to allow the delivery of a generated event
	 * from one FSM to another FSM.
	 * 
	 * @param event - the event
	 * @param target - the name of the target FSM
	 * @return
	 */
	public boolean processEvent(Generate g);
	
	/**
	 * Notify the thread to shutdown.
	 *
	 */
	public void shutdown();
	
	/**
	 * Allows the FSM to notify the model that it is complete
	 *
	 */
	public void fsmComplete();
}
