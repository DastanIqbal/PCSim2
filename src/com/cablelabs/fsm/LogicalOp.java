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

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;


/**
 * This is the container class for a logical operation defined within the
 * PC 2.0 Simulator's XML document. It contains both the left
 * and right operator associated with the AND/OR operator. 
 * @author ghassler
 *
 */
public class LogicalOp implements Conditional {

	/**
	 * A flag designating that the logical operator is an AND or an
	 * OR
	 */
	private boolean andOp;
	
	/**
	 * The right operand to the logical operator.
	 */
	private Conditional right;
	
	/**
	 * The left operand to the logical operator.
	 */
	private Conditional left;
	
	/**
	 * A logger for debugging purposes.
	 */
	private LogAPI logger = LogAPI.getInstance();
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = null;
	
	/**
	 * Constructor.
	 * 
	 * @param and - true if the logical operand is AND, false if it is an OR
	 */
	public LogicalOp (String subcat, boolean and) {
//		this.logger = logger; // Logger.getLogger(FSM.class);
		this.subCat = subcat;
		this.andOp = and;
		
	}
	
	/**
	 * The display method is different because it attempts to try and format 
	 * the information in a more user friendly format than the toString
	 * method. 
	 * 
	 * Class that implement this method should attempt to display the
	 * information in a msg[msg_instance].hdr[hdr_instance].field format
	 * to keep it consistent.
	 * @return
	 */
	@Override
	public String display() {
		String result = null;
//		if (left == null || right == null) {
//			int glh = 0;
//		}
		if (andOp) 
			result = left.display() + " and\n\t" + right.display();
		else
			result = left.display() + " or\n\t" + right.display();
		return result;
	}
	/**
	 * The performs the logical comparison operation by having each operand
	 * produce its' own boolean evaluation and combining the results of the
	 * two operand.
	 * 
	 * @param compEval - the comparison evaluator that will perform the boolean
	 * 			evaluation for the operands.
	 * @param event - the current event that initiated the comparison.
	 */
	@Override
	public boolean evaluate(ComparisonEvaluator compEval, MsgEvent event) {
		if (compEval != null) {
			boolean r = false;
			boolean l = false;
			boolean result;
			if (left != null && right != null) {
				if (left instanceof LogicalOp) {
					l = ((LogicalOp)left).evaluate(compEval, event);
					logger.debug(PC2LogCategory.FSM, subCat,
							"LogicalOp get left-hand side's logical value. Result is " + l);
				}
				else if (left instanceof ComparisonOp) {
					l = ((ComparisonOp)left).evaluate(compEval, event);
					logger.debug(PC2LogCategory.FSM, subCat,
							"LogicalOp get left-hand side's comparison value. Result is " + l );
				}
				if (right instanceof LogicalOp) {
					r = ((LogicalOp)right).evaluate(compEval, event);
					logger.debug(PC2LogCategory.FSM, subCat,
							"LogicalOp get right-hand side's logical value. Result is " + r);
				}
				else if (right instanceof ComparisonOp) {
					r = ((ComparisonOp)right).evaluate(compEval, event);
					logger.debug(PC2LogCategory.FSM, subCat,
							"LogicalOp get right-hand side's comparison value. Result is " + r);
				}
				if (andOp) {
					
					result = r && l;
					logger.debug(PC2LogCategory.FSM, subCat,
							"LogicalOp returning result=" + result + " for " + l + " && " + r);
				}
				else {
					result = l || r;
					logger.debug(PC2LogCategory.FSM, subCat,
							"LogicalOp returning result=" + result + " for " + l + " || " + r);
				}
				
				logger.debug(PC2LogCategory.FSM, subCat,
						"LogicalOp final result=" + result);
				return result;
			}
		}
		return false;
	}

	@Override
	public int getWildcardIndex() {
		int leftWild = left.getWildcardIndex();
		int rightWild = right.getWildcardIndex();
		if (leftWild == -1)
			return rightWild;
		else 
			return leftWild;
	}
	
	@Override
	public boolean hasWildcardIndex() {
		return (left.hasWildcardIndex() || right.hasWildcardIndex());
	}
	
	@Override
	public void updateWildcardIndex(int newIndex) {
		left.updateWildcardIndex(newIndex);
		right.updateWildcardIndex(newIndex);
	}
	
	@Override
	public void resetWildcardIndex() {
		left.resetWildcardIndex();
		right.resetWildcardIndex();
	}
	
	/**
	 * Gets the right operand.
	 * @return
	 */
	public Conditional getRight() {
		return right;
	}
	
	/**
	 * Gets the left operand.
	 * @return
	 */
	public Conditional getLeft() {
		return left;
	}

	/**
	 * Test whether the class represents an AND logical operator or an
	 * OR.
	 * 
	 * @return - true if it represents an AND operator, false otherwise.
	 */
	public boolean isAndOperator() {
		return andOp;
	}


	/**
	 * Sets the right operand.
	 * @param r - the operand to assign to the right-side.
	 * 		of the operator.
	 */
	public void setRight(Conditional r) {
		this.right = r;
	}
	
	/**
	 * Sets the left operand.
	 * @param l - the operand to assign to the left-side
	 * 		of the operator.
	 * 
	 */
	public void setLeft(Conditional l) {
		this.left = l;
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = null;
//		if (left == null || right == null) {
//			int glh = 0;
//		}
		if (andOp) 
			result = left.toString() + " and\n\t" + right.toString();
		else
			result = left.toString() + " or\n\t" + right.toString();
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
		LogicalOp retval = (LogicalOp)super.clone();
		if (retval != null ) {
			retval.andOp = this.andOp;
			if (this.right != null) 
				retval.right = (Conditional)this.right.clone();
			if (this.left != null)
				retval.left = (Conditional)this.left.clone();
		}	

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
	@Override
	public void updateUIDs(int newUID, int origUID) {
		left.updateUIDs(newUID, origUID);
		right.updateUIDs(newUID, origUID);
		
	}


}
