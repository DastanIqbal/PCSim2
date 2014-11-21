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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;

import com.cablelabs.log.MonitorListener;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.tools.SIPLocator;

/**
 * This is the global message queue container class for the 
 * platform engine. It stores all of the message that any
 * stack as sent up to a FSM for processing. Each message is
 * marked with the FSM that processed the event. 
 * 
 * The class provides accessors for retrieving elements that
 * meet specific criteria for evaluation and comparison opertations
 * during processing. 
 * 
 * @author ghassler
 *
 */
public class MsgQueue implements MonitorListener {

	/**
	 * The physical container for all of the messages processed
	 * by an FSM during the test.
	 */
	private LinkedList<MsgEvent>q;
	
	/**
	 * Logger
	 */
	private LogAPI logger = LogAPI.getInstance();
	
	/**
	 * An index table to retrieve the original request message 
	 * associated to a SIP dialog based upon CallId
	 */
	private Hashtable<String, SIPMsg> callIdIndex;
	
	/**
	 * An index table to retrieve the request message 
	 * associated to a SIP resposne based upon CallId, method and CSeq number
	 */
	private Hashtable<String, SIPMsg> callIdAndMethodIndex;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	final private String subCat = "";
	
	private SIPLocator locator = SIPLocator.getInstance();
	/**
	 * Constants for msg and hdr instance values that are 
	 * not numeric.
	 */
	public static final String LAST = "last";
	public static final String ANY = "any";
	public static final String FIRST = "first";
	public static final String PREV = "prev";
	public static final String CURRENT = "current";
	
	private static MsgQueue queue = null;
	
	private int index=0;
	
	private boolean flushMonitor = false;
	/**
	 * Private constructor to force a singleton for the application.
	 *
	 */
	private MsgQueue() {
		//this.logger = logger; // Logger.getLogger(FSM.class);
		q = new LinkedList<MsgEvent>();
		callIdIndex = new Hashtable<String, SIPMsg>();
		callIdAndMethodIndex = new Hashtable<String, SIPMsg>();
	}
	
	/**
	 * Retreives the single instance of the MsgQueue if it 
	 * already exists. If it doesn't exist it will create it.
	 *
	 */
	public static MsgQueue getInstance() {
		if (queue == null) {
			queue = new MsgQueue();
		}
		return queue;
	}

	/**
	 * Adds a MsgEvent to the queue.
	 * 
	 * @param event - the event
	 * @throws IllegalArgumentException
	 */
	public synchronized void  add(MsgEvent event) throws IllegalArgumentException {
		// As a sanity check make sure no one is 
		// trying to add a msg type we don't understand
		if (!(event instanceof MsgEvent))  {
				String msg = new String("Someone is trying to add a msg type to the MsgQueue that it" +
				" doesn't understand."); 
				logger.debug(PC2LogCategory.MsgQueue, subCat, msg);
				throw new IllegalArgumentException(msg);
		}
		else {
			if (event.getUID() > 0) {
				event.setMsgQueueIndex(index++);
				q.addLast(event);
				if (event instanceof SIPMsg) {
					SIPMsg msg = (SIPMsg)event;
					if(msg.isRequestMsg()) {
						String method = msg.getRequest().getMethod();;
						String callId = null;
						String cSeqNo = null;
						if (msg.hasSentMsg()) {
							callId = locator.getSIPParameter("Call-ID", "value", 
									FIRST, msg.getSentMsg());
							cSeqNo = locator.getSIPParameter("CSeq", "value", 
									FIRST, msg.getSentMsg());
						}
						else {
							callId = ((CallIdHeader)msg.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
						 	CSeqHeader cs = (CSeqHeader)msg.getRequest().getHeader(CSeqHeader.NAME);
						 	cSeqNo = Integer.toString(cs.getSequenceNumber());
						}
						if (callId != null && method != null && cSeqNo != null) {
							String key = callId + method + cSeqNo;
							callIdAndMethodIndex.put(key, msg);
							logger.debug(PC2LogCategory.MsgQueue, subCat, 
									"Adding key=" + key + " to callID and method message index.");
							if (!callIdIndex.containsKey(callId)) {
								logger.debug(PC2LogCategory.MsgQueue, subCat, 
										"Adding key=" + callId + " to callID message index.");
								callIdIndex.put(callId, msg);
							}
						}
					}
				}
			}
			else {
				logger.warn(PC2LogCategory.MsgQueue, subCat, 
						"Attempted to add MsgEvent to queue with no UID set." + event);
			}
		}
	}
	
	public synchronized void autoFlush() {
		// We need to iterate through all of the messages that have a 
		// timestamp that is older than 120 seconds (120000)
		long target = System.currentTimeMillis() - 120000;
		int msgs = 0;
		ListIterator<MsgEvent> iter = q.listIterator();
		boolean complete = false;
		while (iter.hasNext() && !complete) {
			MsgEvent event = iter.next();
			if (event.getTimeStamp() < target) {
				msgs++;
				// Also, while we are at it remove the item from the indexes
				if (event instanceof SIPMsg) {
					SIPMsg msg = (SIPMsg)event;
					if(msg.isRequestMsg()) {
						String method = msg.getRequest().getMethod();;
						String callId = null;
						String cSeqNo = null;
						if (msg.hasSentMsg()) {
							callId = locator.getSIPParameter("Call-ID", "value", 
									FIRST, msg.getSentMsg());
							cSeqNo = locator.getSIPParameter("CSeq", "value", 
									FIRST, msg.getSentMsg());
						}
						else {
							callId = ((CallIdHeader)msg.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
							CSeqHeader cs = (CSeqHeader)msg.getRequest().getHeader(CSeqHeader.NAME);
							cSeqNo = Integer.toString(cs.getSequenceNumber());
						}
						if (callId != null && method != null && cSeqNo != null) {
							String key = callId + method + cSeqNo;
							callIdAndMethodIndex.remove(key);
							logger.debug(PC2LogCategory.MsgQueue, subCat, 
									"Removing key=" + key + " to callID and method message index.");
							callIdIndex.remove(callId);
							logger.debug(PC2LogCategory.MsgQueue, subCat, 
									"Removing key=" + callId + " to callID message index.");
						}
					}
				}
			}
			else 
				complete = true;
		}
		if (msgs > 0) {
			logger.info(PC2LogCategory.MsgQueue, subCat, 
					"Removing " + msgs + " events from the msgQueue.");
			// Now we know how many to remove
			while (msgs > 0) {
				q.removeFirst();
				msgs--;
			}
		}
	}
	
	/**
	 * Searches the message queue for a specific occurrence of a message
	 * delivered to the FSM with the unique id parameter based upon protocol.
	 * @param uid - the unique id of the FSM that processed the message
	 * @param key - the message key
	 * @param index - instance of the message relative to the current
	 * 		event being processed
	 * @param curIndex - the index of the current event being processed
	 * @return
	 */
	public synchronized MsgEvent findByProcotol(int uid, 
			String protocol, String index, int curIndex ) {
		if (protocol == null)
			return null;
		LinkedList<MsgEvent> ll = null;
		if (index.equals(FIRST)) 
			ll =  find(uid, protocol, 1, false, false, true, curIndex);
		else if (index.equals(LAST)) 
			ll = find(uid, protocol, 1, true, false, true, curIndex);
		else if (index.equals(PREV))
			ll = find(uid, protocol, 2, true, false, true, curIndex);
		else {
			try {
				int o = Integer.parseInt(index);
				ll = find(uid, protocol, o, false, false, true, curIndex);
			}
			catch (NumberFormatException e) {
				// do nothing
			}
		}
		if (ll != null && ll.size() == 1) 
			return ll.getFirst();
		else if (ll == null) {
			logger.debug(PC2LogCategory.MsgQueue, subCat, 
					"MsgQueue did not find any answers for protocol(" + 
					protocol + ") and index(" + index + ") for fsm with uid" + uid + ".");
		}
		else if (ll.size() > 1){
			logger.debug(PC2LogCategory.MsgQueue, subCat, 
					"MsgQueue find resulted in multiple answers for protocol(" + 
					protocol + ") and index(" + index + ") for fsm with uid" + uid + ".");
		}
		return null;
	}
	/**
	 * Searches the message queue for a specific occurrence of a message
	 * delivered to the FSM with the unique id parameter based upon the
	 * name of the event.
	 * @param uid - the unique id of the FSM that processed the message
	 * @param key - the message key
	 * @param index - instance of the message relative to the current
	 * 		event being processed
	 * @param curIndex - the index of the current event being processed
	 * @return
	 */
	public synchronized MsgEvent find(int uid, String key, 
			String index, int curIndex) {
		if (key == null)
			return null;
		LinkedList<MsgEvent> ll = null;
		if (index.equals(FIRST)) 
			ll =  find(uid, key, 1, false, false, false, curIndex);
		else if (index.equals(LAST)) 
			ll = find(uid, key, 1, true, false, false, curIndex);
		else if (index.equals(PREV))
			ll = find(uid, key, 2, true, false, false, curIndex);
		else {
			try {
				int o = Integer.parseInt(index);
				ll = find(uid, key, o, false, false, false, curIndex);
			}
			catch (NumberFormatException e) {
				// do nothing
			}
		}
		if (ll != null && ll.size() == 1) 
			return ll.getFirst();
		else if (ll == null) {
			logger.debug(PC2LogCategory.MsgQueue, subCat, 
					"MsgQueue did not find any answers for key(" + 
					key + ") and index(" + index + ") for fsm with uid" + uid + ".");
		}
		else if (ll.size() > 1){
			logger.debug(PC2LogCategory.MsgQueue, subCat, 
					"MsgQueue find resulted in multiple answers for key(" + 
					key + ") and index(" + index + ") for fsm with uid" + uid + ".");
		}
		return null;
	}
	
	/**
	 * Searches for all of the messages that match the key and were 
	 * processed by the FSM with the unique id identified by the uid parameter.
	 * 
	 * @param uid - the unique id of the FSM that processed the message
	 * @param key - the message key
	 * @param curIndex - the index of the current event being processed
	 * @return - list of messages that meet the parameters.
	 * 
	 */
	public synchronized LinkedList<MsgEvent> findAll(int uid, String key, int curIndex) {
		LinkedList<MsgEvent> ll = find(uid, key, q.size(), false, true, false, curIndex);
		return ll;
	}

	public synchronized void flush() {
		if (!flushMonitor) {
			 logger.addMonitorListener(this);
			 flushMonitor = true;
		 }
//		// We need to iterate to the location of the last
//		// processed message
//		int msgs = 0;
//		ListIterator<MsgEvent> iter = q.listIterator();
//		boolean complete = false;
//		while (iter.hasNext() && !complete) {
//			MsgEvent event = iter.next();
//			if (event.msgQueueIndex < curIndex) {
//				msgs++;
//				// Also, while we are at it remove the item from the indexes
//				if (event instanceof SIPMsg) {
//					SIPMsg msg = (SIPMsg)event;
//					if(msg.isRequestMsg()) {
//						String method = msg.getRequest().getMethod();;
//						String callId = null;
//						String cSeqNo = null;
//						if (msg.hasSentMsg()) {
//							callId = locator.getSIPParameter("Call-ID", "value", 
//									FIRST, msg.getSentMsg());
//							cSeqNo = locator.getSIPParameter("CSeq", "value", 
//									FIRST, msg.getSentMsg());
//						}
//						else {
//							callId = ((CallIdHeader)msg.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
//							CSeqHeader cs = (CSeqHeader)msg.getRequest().getHeader(CSeqHeader.NAME);
//							cSeqNo = Integer.toString(cs.getSequenceNumber());
//						}
//						if (callId != null && method != null && cSeqNo != null) {
//							String key = callId + method + cSeqNo;
//							callIdAndMethodIndex.remove(key);
//							logger.debug(PC2LogCategory.MsgQueue, subCat, 
//									"Removing key=" + key + " to callID and method message index.");
//							callIdIndex.remove(callId);
//							logger.debug(PC2LogCategory.MsgQueue, subCat, 
//									"Removing key=" + callId + " to callID message index.");
//						}
//					}
//				}
//			}
//			else 
//				complete = true;
//		}
//		
//		// Now we know how many to remove
//		while (msgs > 0) {
//			q.removeFirst();
//			msgs--;
//		}
	
	}
	/**
	 * Gets the message at the give location.
	 * @param msgQueueIndex - the entry position within the queue.
	 * @return
	 */
	public synchronized MsgEvent get(int msgQueueIndex) {
		if (msgQueueIndex <= -1)
			return null;
		
		if (msgQueueIndex < index) {
			ListIterator<MsgEvent> iter = q.listIterator();
			while (iter.hasNext()) {
				MsgEvent event = iter.next();
				if (event.getMsgQueueIndex() == msgQueueIndex)
					return event;
				else if (event.getMsgQueueIndex() > msgQueueIndex)
					return null;
			}
		}
		
		return null;
	}
	
	/**
	 * Searches for all of the messages that match the key and were 
	 * processed by the FSM with the unique id identified by the uid parameter.
	 * 
	 * @param uid - the unique id of the FSM that processed the message
	 * @param key - the message key
	 * @param curIndex - the index of the current event being processed
	 * @return - list of messages that meet the parameters.
	 * 
	 */
	public synchronized MsgEvent getPrevious(int uid, String key, int curIndex) {
		ListIterator<MsgEvent> iter = q.listIterator(q.size());
		if (iter.hasPrevious() && q.size() > 2) {
			// Skip the very last message
			iter.previous();
			MsgEvent element = iter.previous();
			if (isMatch(uid, element, key, false, curIndex))
				return element;
		}
		return null;
	}
	
	public int getLast() {
		if (q.isEmpty())
			return -1;
		MsgEvent last = q.getLast();
		if (last != null)
			return last.getMsgQueueIndex();
		else 
			return -1;
	}
	
	/**
	 * Common method to retrieve the requested messages.
	 * 
	 * @param uid - the unique id of the FSM that processed the message
	 * @param key - the message key
	 * @param ndx - the instance of the message to search for relative 
	 * 		to the current event being processed
	 * @param reverse - when true, the method search from the back
	 * @param all - when true, returns all occurrences of the message
	 * @param index - instance of the message relative to the current
	 * 		event being processed
	 * @param curIndex - the index of the current event being processed
	 * @return - a list of the messages that me all of the criteria
	 */
	private LinkedList<MsgEvent> find(int uid, String key, int ndx, 
			boolean reverse, boolean all, boolean protocol, int curIndex) {
		LinkedList<MsgEvent> ll = new LinkedList<MsgEvent>();
		int occurrence = ndx;
		if (q.size() > 0) {
			if (reverse 
					//&& (q.size() >= curIndex)
					) {
				ListIterator<MsgEvent> iter = q.listIterator(q.size());
				boolean complete = false;
				while (iter != null && iter.hasPrevious() && !complete) {
					MsgEvent element = iter.previous();
					if (element.getMsgQueueIndex() <= curIndex) {
						boolean match = isMatch(uid, element, key, protocol, curIndex);
						if (match) {
							occurrence--;
							// When the counter gets to zero we have a match
							if (all)
								ll.addLast(element);
							else if (occurrence == 0 ) {
								ll.addLast(element);
								complete = true;
							}
						}
					}
				}
			}
			else {
				boolean complete = false;
				ListIterator<MsgEvent> iter = q.listIterator();
				while (iter.hasNext() && !complete) {
					MsgEvent element = iter.next();
					boolean match = isMatch(uid, element, key, protocol, curIndex);
					if (match) {
						occurrence--;
						// When the counter gets to zero we have a match
						if (all)
							ll.addLast(element);
						else if (occurrence == 0 ) {
							ll.addLast(element);
							complete = false;
						}
					}
				}
			}
			return ll;
		}

		return null;
	}
	 
	/**
	 * Get the callId for a specific FSM.
	 * 
	 * @param uid - the unique id of the FSM that processed the message
	 * @param callId - the callId to search for.
	 * @return
	 */
	public synchronized SIPMsg findByCallIdAndMethod(String callId, String method, int cSeqNo) {
		String key = callId + method + cSeqNo;
		SIPMsg result = callIdAndMethodIndex.get(key);
		if (result != null)
			logger.debug(PC2LogCategory.MsgQueue, subCat, 
				"Found key=" + key + " in callID and method message index.");
		else 
			logger.debug(PC2LogCategory.MsgQueue, subCat, 
				"Failed to find key=" + key + " in callID and method message index.");
		
		return result;
	}
	
	/**
	 * Get the callId for a specific FSM.
	 * 
	 * @param uid - the unique id of the FSM that processed the message
	 * @param callId - the callId to search for.
	 * @return
	 */
	public synchronized SIPMsg findByCallId(int uid, String callId) {
		SIPMsg result = callIdIndex.get(callId);
		if (result != null)
			logger.debug(PC2LogCategory.MsgQueue, subCat, 
				"Found key=" + callId + " in callID message index.");
		else 
			logger.debug(PC2LogCategory.MsgQueue, subCat, 
				"Failed to find key=" + callId + " in callID message index.");
		
		return result;
	}
	
	/**
	 * Determines if the FSM has been issued a SIP message with the same callID and method
	 * @param uid
	 * @param callId
	 * @return
	 */
/*	public synchronized boolean duplicateRequest(int uid, String callId, String method) {
		SIPMsg msg = findByCallIdAndMethod(uid,callId,method);
		if (msg != null)
			return true;
		return false;
	}*/
	/**
	 * The method tests each message to decide if it is a match for 
	 * the search criteria.
	 * 
	 * @param uid - the unique id of the FSM that processed the message
	 * @param element - the current message being processed.
	 * @param key - the message being sought.
	 * @param protocol - this specifies the value in the key field is a 
	 * 					type of protocol instead of an event name.
	 * @param curIndex - the index of the current event being processed
	 * @return - true if the element matches the criteria, false otherwise
	 */
	private boolean isMatch(int uid, MsgEvent element, String key, 
			boolean protocol, int curIndex) {
		
		boolean match = false;
		// First look if the element is from the correct FSM
		if (uid == element.getUID() && element.msgQueueIndex <= curIndex) {
			// Matching for request messages is fairly straight 
			
			// forward 
			// 1. If the key is "Request" and the msg is
			// a request message then it is a match. 
			// 2. If the key matches the msg's method name, its a
			// match.
			if (element instanceof SIPMsg) {
				if (protocol && key.equals(MsgRef.SIP_MSG_TYPE)) 
					match = true;
				else {
					SIPMsg msg = (SIPMsg)element;
					if (msg.isRequestMsg()) {
//						Request req = msg.getRequest();
						if (key.equalsIgnoreCase("Request") || 
								//(key.equalsIgnoreCase(req.getMethod())))
								(key.equalsIgnoreCase(msg.getEvent())))
							match = true;
					}
					// Matching responses is a little more difficult 
					// 1. If the key is "Response" and the msg is 
					// a response message then it is a match
					// 2. If it is an exact match for the msg's status-code and
					// CSeq line's method parameter then it is a match
					// 3. If the key's status-code is a range (containing an
					// "x" in one of the integer fields, then status-code
					// needs to be tested for a match over the range. If the
					// msg's status-code falls in the range and the method in
					// the CSeq line's method parameter then it is a match
					else if (msg.isResponseMsg()) {
						Response resp = msg.getResponse();
						// As a sanity check verify the key is of type Response
						if (SIPConstants.isResponseType(key)) {
							// Next key into status-code and method as well as
							// get the range for the status-code
							String method = key.substring(4,key.length());
							// Test that the range is not a complete wildcard first
							if (key.substring(0,3).equals("xxx")) {
								if (((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getMethod().equalsIgnoreCase(method))
									match = true;
							}
							else {try {
								int status = Integer.parseInt(key.substring(0,3));
								// This means it is a specific instance of status code
								if (((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getMethod().equalsIgnoreCase(method) &&
										status == resp.getStatusCode()) 
									match = true;

							}
							catch (NumberFormatException nfe) {
								// Since we received an number format exception and we know it is 
								// in a valid format look for the x wild card in the ones column
								// before trying the tens column
								String status = key.substring(0,3);
								int bRange = Integer.parseInt(status.substring(0,1)) * 100;
								int eRange = bRange;
								if (Character.isDigit(status.charAt(1))) {
									int ten = Integer.parseInt(status.substring(1,2)) *10;
									bRange += ten;
									eRange = bRange + 9;
								}
								else {
									eRange += 99;
								}
								if (((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getMethod().equalsIgnoreCase(method)) {
									if (resp.getStatusCode() >= bRange && resp.getStatusCode() <= eRange) {
										match = true;
									}
								}
							}
							}
						}
						else if (key.equalsIgnoreCase("Response")) {
							match = true;
						}
					}
				}
			}
			else if (element instanceof UtilityMsg) {
				if (protocol && key.equals(MsgRef.UTILITY_MSG_TYPE)) 
					match = true;
				else if (key.equals(((UtilityMsg)element).getEventType()))
					match = true;
			}
			else if (element instanceof StunMsg) {
				if (protocol && key.equals(MsgRef.STUN_MSG_TYPE)) 
					match = true;
				else if (key.equals(((StunMsg)element).getMsgType()))
					match = true;
			}
			else if (element instanceof InternalMsg) {
				if (protocol && key.equals(MsgRef.EVENT_MSG_TYPE)) 
					match = true;
				else if (key.equals(((InternalMsg)element).getEvent()))
					match = true;
			}
			else if (element instanceof RTPMsg) {
				if (protocol && key.equals(MsgRef.RTP_MSG_TYPE)) 
					match = true;
				else if (key.equals(((RTPMsg)element).getEventName()))
					match = true;
			}
			
			else {
				logger.warn(PC2LogCategory.MsgQueue, subCat, 
						"Unrecognized message type in MsgQueue!");
			}
		}
		return match;
	}
	
	/**
	 * Used to clear the message queue between
	 * test executions. Information in a global model should 
	 * remain for operations that could occur between
	 * tests, eg. registrar ,therefore the 
	 *
	 */
	public void reset(LinkedList<Integer> fsmUIDs) {
		int ndx = 0;

		while (ndx < q.size()) {
			MsgEvent event = q.get(ndx);
			boolean remove = false;
			if (fsmUIDs.contains(event.getUID())) {
				remove = true;
			}
			// We also need to keep the index tables in 
			// sync with the q, so see if there is any 
			// cleanup needed for this event
			if (remove) {
				if (event instanceof SIPMsg) {
					SIPMsg msg = (SIPMsg)event;
					if(msg.isRequestMsg()) {
						CallIdHeader callId = (CallIdHeader)msg.getRequest().getHeader(CallIdHeader.NAME);
						String method = msg.getRequest().getMethod();
						if (callId != null && method != null) {
							String key = callId.getCallId() + method;
							if (callIdAndMethodIndex.containsKey(key)) {
								callIdAndMethodIndex.remove(key);
								logger.debug(PC2LogCategory.MsgQueue, subCat, 
										"Removing key=" + key + " to callID message index.");
							}
							if (callIdIndex.containsKey(callId.getCallId())) {
								callIdIndex.remove(callId.getCallId());
							}
						}
					}
				}
				q.remove(ndx);
			}
			// Now we need to figure what the next index value should be
			// If we removed something, the index doesn't need to change
			// otherwise increment by one.
			if (!remove)
				ndx++;

		}
		// Instead of resetting to zero, reset to the last index in remaining in the queue.
		//index = 0;
		if (q.size() > 0)
			index = q.getLast().getMsgQueueIndex() + 1;
		else
			index = 0;
	}

	@Override
	public void timerTick() {
		MsgQueue.getInstance().autoFlush();
	}
}
