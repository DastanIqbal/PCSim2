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

import java.util.LinkedList;
import java.util.ListIterator;


/**
 * This is a container class for storing the response element of a PC
 * 2.0 XML document.
 * 
 * @author ghassler
 *
 */
public class Responses implements Cloneable {

	/**
	 * A list of operations to be performed whenever a state receives
	 * an event.
	 */
	protected LinkedList<ActiveOp> operations;
	
	/**
	 * Logger
	 */
//	private LogAPI logger = null;
	
	/**
	 * Constructor
	 *
	 */
	public Responses() {
		operations = new LinkedList<ActiveOp>();
//		logger = Logger.getLogger(FSM.class);
	}
	
	public Responses(Responses orig) {
		operations = new LinkedList<ActiveOp>(orig.operations);
	}
	
	/**
	 * Adds a new IF test to the list.
	 * @param test
	 */
	public void addActiveOp(ActiveOp test) {
		operations.add(test);
	}
	
	/**
	 * Retrieves the requested operation located at position
	 * index.
	 * 
	 * @param index - the element being requested.
	 * 
	 * @return
	 */
	public ActiveOp getFlowControl(int index) {
		return operations.get(index);
	}
	
	/**
	 * Removes the specified element from the list of tests to
	 * perform.
	 *
	 * @param index - the index of the element to remove
	 * 
	 * @return - the removed element or null
	 */
	public ActiveOp removeStateOp(int index) {
		return operations.remove(index);
	}
	
	/**
	 * Process a new event.
	 * @param api - the FSMAPI to invoke during processing
	 * @param ce - the comparison evaluator to use during logical 
	 * 			and comparison operations
	 * @param event - the current event to process.
	 * @throws PC2Exception
	 */
	public void execute(FSMAPI api, ComparisonEvaluator ce, 
			 MsgEvent event) throws PC2Exception {
		ActiveOp op = null;
		for (int i = 0; i < operations.size(); i++) {
			try {
				op = operations.get(i);
				op.performOp(api, ce, event);
			}
			catch (Exception e) {
				String err = "Exception encountered during response processing of the " + i + " element.";
				throw new PC2Exception(err + e.getMessage() + "\n" + e.getStackTrace());
			}
		}
	}
	
	public int size() {
		return operations.size();
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = "";
		for (int i = 0; i < operations.size(); i++) {
			ActiveOp op = operations.get(i);
			if (op instanceof FlowControl)
				result += "\n" + op;
			else if (op instanceof Variable)
				result += "\n" + op;
		}
		
		return result;
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		Responses retval = (Responses)super.clone();
		if (retval != null ) {
			if (this.operations != null) {
				retval.operations = new LinkedList<ActiveOp>();
				ListIterator<ActiveOp> iter = this.operations.listIterator();
				while (iter.hasNext()) {
					ActiveOp op = iter.next();
					if (op instanceof FlowControl) {
						FlowControl fc = (FlowControl)op;
						FlowControl newFC = (FlowControl)fc.clone();
						retval.operations.add(newFC);
					}
				}
			}
//			retval.logger = Logger.getLogger(FSM.class);
		}	
//		System.out.println("this " + this.me());
//		System.out.println("retval " + retval.me());
		return retval;
	}
	
	/**
	 * Allows for the fsmUIDs to be updated to the correct value when
	 * a FSM has been cloned.
	 * 
	 * @param newUID - The new FSM UID value to use if the current value
	 * 		matches the origUID parameter.
	 * @param origUID - The FSM UID value to verify is set as the current
	 * 		value before updating.
	 */
	protected void updateUIDs(int newUID, int origUID) {
		ListIterator<ActiveOp> iter = operations.listIterator();
		while (iter.hasNext()) {
			ActiveOp op = iter.next();
			if (op instanceof FlowControl) {
				((FlowControl)op).updateUIDs(newUID, origUID);
			}
			else if (op instanceof Variable)
				((Variable)op).updateUIDs(newUID, origUID);
		}
	}
//	 This method was added as a validator for cloning
//	public String me() {
//		String result = "\t" + super.toString() + "\n";
//		for (int i = 0; i < tests.size(); i++) {
//			result += tests.get(i).me();
//		}
//		
//		return result;
//	}
}
