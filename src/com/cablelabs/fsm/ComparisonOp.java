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
 * A container class for a single comparison operation within a
 * PC 2.0 Simulator XML document. It holds the operator
 * and the two operands necessary to complete the comparison 
 * analysis of the two operands.
 * 
 * @author ghassler
 *
 */
public class ComparisonOp  implements Conditional  { 
	
	/**
	 * The specific operator to perform.
	 */
	private String operator;
	
	/**
	 * The left operand.
	 */
	private Reference left;
	
	/**
	 * The right operand.
	 */
	private Reference right;
	
	/**
	 * A flag indicating that the <eq>, <neq>, <startsWith> and <endsWith> operators
	 * should perform their test ignoring the case of the string.
	 */
	private Boolean ignoreCase = null;
	
	/**
	 * This contains a string defining the format an isDate comparison op should use.
	 */
	private String dateFormat = null; 
	
	private Boolean decision = null;
	
	/**
	 * This contains the string representation of the 
	 * comparison operation to be performed by the Conditional
	 * Valid Values: eq, neq, gt, lt, gte, lte, contains, dnc, count
	 * 				 ipv4, ipv6, null, notnull, startsWith, endsWith, and digest
	 * Default Value: none
	 */
	public ComparisonOp(String operator) {
		this.operator = operator;
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
			String l = null;
			String r = null;
			String op = null;
			
			if (operator.equals("digest"))
					return "(" + operator + ")";
			else if (operator.equals("ipv4") && left != null) {
				if (decision == null || !decision)
					return "*( " + left.display() + " is an IPv4 address" + " )";
				else
					return "( " + left.display() + " is an IPv4 address" + " )";
			}
			else if (operator.equals("ipv6") && left != null) {
				if (decision == null || !decision)
					return "*( " + left.display() + " is an IPv6 address" + " )";
				else
					return "( " + left.display() + " is an IPv6 address" + " )";
			}
			else if (operator.equals("isDate")) {
			    if (dateFormat == null)
	                return "(" + left.display() + " is a valid date )";
	            else
	                return "(" + left.display() + " is a valid date with format '" + dateFormat + "')";
			}
			else {
				l = ((left != null) ? left.display() : null);

				r = ((right != null) ? right.display() : null);

				if (operator.equals("eq"))
					op = new String("==");
				else if (operator.equals("neq"))
					op = new String("!=");
				else if (operator.equals("lt"))
					op = new String("<");
				else if (operator.equals("gt"))
					op = new String(">");
				else if (operator.equals("lte"))
					op = new String("<=");
				else if (operator.equals("gte"))
					op = new String(">=");
				else 
					op = operator;

				if (decision == null || !decision)
					return "*(" + l + " " + op + " " + r + ")";
				else 
					return "(" + l + " " + op + " " + r + ")";
			}
		
	}
	
	/**
	 * Gets the operator.
	 * 
	 */
	public String getOperator() {
		return operator;
	}
	
	/**
	 * Gets the left operand.
	 */
	public Reference getLeft() {
		return left;
	}
	
	/**
	 * Sets the left operand.
	 * @param left - the left operand.
	 */
	public void setLeft(Reference left) {
		this.left = left;
	}
	
	/**
	 * Gets the right operand.
	 */
	public Reference getRight() {
		return right;
	}
	
	/**
	 * Sets the right operand.
	 * @param right - the right operand.
	 */
	public void setRight(Reference right) {
		this.right = right;
	}
	
	/**
	 * Sets the ignoreCase flag.
	 * @param flag - true or false.
	 */
	public void setIgnoreCase(boolean flag) {
		this.ignoreCase = flag;
	}
	
	/**
	 * Gets the ignoreCase flag
	 * @return
	 */
	public boolean getIgnoreCase() {
		return this.ignoreCase;
	}
	
	/**
	 * Sets the dateFormat String
	 * @param format
	 */
	public void setDateFormat(String format) {
	    this.dateFormat = format;
	}
	
	/**
	 * Gets the dateFormat string
	 * @return
	 */
	public String getDateFormat() {
	    return this.dateFormat;
	}
	
	/**
	 * The method that the FSM invokes to initiate the comparison
	 * operation. The Examiner class performs the actual operation
	 * through the ComparisonEvaluator interface.
	 * 
	 * @return - true if the comparison is valid, false otherwise
	 */
	@Override
    public boolean evaluate(ComparisonEvaluator compEval, MsgEvent event) {
		if (compEval != null) {
			 decision = compEval.evaluate(this, operator, left, right, event, ignoreCase, dateFormat);
			 return decision;
		}
		return false;
	}

	/**
	 * Creates a string representation of the comparison operation to
	 * be performed upon invocation of the evaluate method.
	 * 
	 * @return - the string representation of the comparison to be 
	 * 		performed.
	 */
	@Override
    public String toString() {
		String l = null;
		String r = null;
		String op = null;
		
		if (operator.equals("digest"))
				return "(" + operator + ")";
		else if (operator.equals("ipv4") && left != null)
				return "( " + left + " is an IPv4 address" + " )";
		else if (operator.equals("ipv6") && left != null)
				return "( " + left + " is an IPv6 address" + " )";
		else if (operator.equals("isDate")) {
		    if (dateFormat == null)
		        return "(" + left + " is a valid date )";
		    else
		        return "(" + left + " is a valid date with format '" + dateFormat + "')";
		}
		else if (operator.equals("null") && left != null)
			return "( " + left + " == null )";
		else if (operator.equals("notnull") && left != null)
			return "( " + left + " != null )";
		else {
			l = ((left != null) ? left.toString() : null);

			r = ((right != null) ? right.toString() : null);

			if (operator.equals("eq"))
				op = new String("==");
			else if (operator.equals("neq"))
				op = new String("!=");
			else if (operator.equals("lt"))
				op = new String("<");
			else if (operator.equals("gt"))
				op = new String(">");
			else if (operator.equals("lte"))
				op = new String("<=");
			else if (operator.equals("gte"))
				op = new String(">=");
			else 
				op = operator;

			return "(" + l + " " + op + " " + r + ")";
		}
	}

	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
    public Object clone() throws CloneNotSupportedException {
		ComparisonOp retval = (ComparisonOp)super.clone();
		if (retval != null) {
			if (this.operator != null) 
				retval.operator = new String(this.operator);
			if (this.left != null)
				retval.left = (Reference)this.left.clone();
			if (this.right != null)
				retval.right = (Reference)this.right.clone();
		}	
		
		return retval;
	}

	@Override
    public int getWildcardIndex() {
		if ((left instanceof UtilityRef &&
				((UtilityRef)left).hasArrayReference() &&
				((UtilityRef)left).getArrayReference().getIndexes().hasWildcard())) 
			return ((UtilityRef)left).getArrayReference().getIndexes().getWildcard();
		else if ((right instanceof UtilityRef &&
				((UtilityRef)right).hasArrayReference() &&
				((UtilityRef)right).getArrayReference().getIndexes().hasWildcard()))
			return ((UtilityRef)right).getArrayReference().getIndexes().getWildcard();
		
		return -1;
	}
	
	@Override
    public boolean hasWildcardIndex() {
		if ((left instanceof UtilityRef &&
				((UtilityRef)left).hasArrayReference() &&
				((UtilityRef)left).getArrayReference().getIndexes().hasWildcard()) ||
				(right instanceof UtilityRef &&
						((UtilityRef)right).hasArrayReference() &&
						((UtilityRef)right).getArrayReference().getIndexes().hasWildcard())) 
			return true;
		return false;
	}
	
	@Override
    public void updateWildcardIndex(int newIndex) {
		if ((left instanceof UtilityRef &&
				((UtilityRef)left).hasArrayReference() &&
				((UtilityRef)left).getArrayReference().getIndexes().hasWildcard())) {
			UtilityRef ur = (UtilityRef)left;
			ArrayRef ar = ur.getArrayReference();
			ArrayIndex ai = ar.getIndexes();
			ai.setIndex(ai.getWildcard(), newIndex); 
		}
		if ((right instanceof UtilityRef &&
				((UtilityRef)right).hasArrayReference() &&
				((UtilityRef)right).getArrayReference().getIndexes().hasWildcard())) {
			UtilityRef ur = (UtilityRef)right;
			ArrayRef ar = ur.getArrayReference();
			ArrayIndex ai = ar.getIndexes();
			ai.setIndex(ai.getWildcard(), newIndex); 
		}
		//((ArrayIndex)((ArrayRef)right).getIndexes()).hasWildcard())
	}
	
	@Override
    public void resetWildcardIndex() {
		updateWildcardIndex(ArrayIndex.WILDCARD_VALUE);
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
		updateUID(left, newUID, origUID);
		updateUID(right, newUID, origUID);
		
	}
	
	private void updateUID(Reference ref, int newUID, int origUID) {
		if (ref instanceof MsgRef) {
			if (((MsgRef)ref).getUID() == origUID)
				((MsgRef)ref).setUID(newUID);
		}
		else if (ref instanceof VarExprRef) 
			((VarExprRef)ref).updateUID(newUID, origUID);

	}
}
