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
 * This is an interface for abstracting the comparison operations to be 
 * executed by an FSM upon classes that may not be defined by system for
 * the Parse GUI. It allows the construction of objects by the Parser, but
 * their execution to be performed by another class. The class that performs
 * this operation is the Examiner.
 * 
 * @author ghassler
 *
 */
public interface ComparisonEvaluator {

	/**
	 * The base declaration for the method that is invoked by the FSM when 
	 * a comparison operation is needed.
	 * 
	 * @param operator - the String representation of the operation to perform
	 * 		upon the left and right operands. Currently eq, neq, lt, lte, gt, gte,
	 * 		count, contains, dnc and digest are all that are supported.
	 * @param left - the left operand
	 * @param right - the right operand
	 * @param event - the current event that triggered the request for the
	 * 		comparison operation to be performed.
	 * @param ignoreCase - a flag indicating the comparison should ignore the
	 * 		case of the string.
	 * @param dateFormat - a string defining the date format to search for
	 * 
	 * @return - true if the operation is valid, false otherwise
	 */
	public boolean evaluate(Object source, String operator, Reference left, 
			Reference right, MsgEvent event, Boolean ignoreCase, String dateFormat);
}
