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
 * A container class for one If-then-elseif-else block of elements within
 * a PC 2.0 Simulator XML document.
 * 
 * @author ghassler
 *
 */
public class FlowControl implements Cloneable, ActiveOp {

	/**
	 * The root Logical or Comparison condition to evaluate.
	 */
	protected Conditional cond;
	
	/**
	 * The Actions to execute if the conditon evaluates to true.
	 */
	protected ActionFactory thenActions;
	
	/**
	 * The Actions to execute if the condition evaluates to false.
	 */
	
	protected ActionFactory elseActions;
	/**
	 * Subsequent condition to be evaluated if this class's condition
	 * evaluates to false;
	 */
	protected ElseIf elseif;
	
	/**
	 * Gets the condition container.
	 * 
	 */
	public Conditional getCond() {
		return cond;
	}

	/**
	 * Sets the condition container.
	 * @param cond
	 */
	public void setCond(Conditional cond) {
		this.cond = cond;
	}

	/**
	 * Gets the ActionFactory for the else operation.
	 * 
	 */
	public ActionFactory getElseActions() {
		return elseActions;
	}

	/**
	 * Sets the ActionFactory to be execute for the else operation.
	 * @param elseActions
	 */
	public void setElseActions(ActionFactory elseActions) {
		this.elseActions = elseActions;
	}

	/**
	 * Gets the elseif condition to be evaluated.
	 * @return
	 */
	public ElseIf getElseif() {
		return elseif;
	}

	/**
	 * Sets the elseif conditional to be evaluated.
	 * @param elseif
	 */
	public void setElseif(ElseIf elseif) {
		this.elseif = elseif;
	}

	/**
	 * Gets the ActionFactory to be executed for the then operation.
	 * @return
	 */public ActionFactory getThenActions() {
		return thenActions;
	}

	/**
	 * Sets the ActionFactory to be executed for the then operation.
	 * @param thenActions
	 */
	 public void setThenActions(ActionFactory thenActions) {
		this.thenActions = thenActions;
	}
	
	/**
	 * Performs the actual operation of evaluate the conditional through
	 * the structure and invokes the appropriate ActionFactory based upon the
	 * outcome of conditional's evaluation.
	 * 
	 * @param api - the class to call when needing actions executed.
	 * @param ce - the class to perform the conditional's evaluation.
	 * @param event - the current event that cause the condition to be evaluated.
	 * @return true if the task completes successfully, false otherwise.
	 * @throws PC2Exception
	 */
	 @Override
	public boolean performOp(FSMAPI api, ComparisonEvaluator ce, MsgEvent event) throws PC2Exception {
		 if (cond instanceof LogicalOp) {

			 if (((LogicalOp)cond).evaluate(ce, event)) {
				 if (thenActions != null) {
					 thenActions.executeActions(api, event.getMsgQueueIndex());
				 }
			 }
			 else if (elseif != null) {
				 return elseif.performOp(api, ce, event);
			 }
			 else {
				 if (elseActions != null) {
					 elseActions.executeActions(api, event.getMsgQueueIndex());
				 }
			 }
		 }
		 else if (cond instanceof ComparisonOp) {
			 if (((ComparisonOp)cond).evaluate(ce, event)) {
				 if (thenActions != null) {
					 thenActions.executeActions(api, event.getMsgQueueIndex());
				 }
			 }
			 else if (elseif != null) {
				 return elseif.performOp(api, ce, event);
			 }
			 else {
				 if (elseActions != null) {
					 elseActions.executeActions(api, event.getMsgQueueIndex());
				 }
			 }
		 }

		 return true;

	 }

	/**
	 * Retrieves a string representation of the if-then-elseif-else logic the 
	 * class currently has defined within it.
	 */
	 @Override
	public String toString() {
		String result = new String();
		if (cond instanceof ComparisonOp) 
			result = ((ComparisonOp)cond).toString();
		else if (cond instanceof LogicalOp) 
			result = ((LogicalOp)cond).toString();
		if (thenActions != null)
			result += "\nthen\n" + thenActions.toString();
		if (elseif != null) 
			result += elseif.toString();
		if (elseActions != null) 
			result += "\nelse\n" + elseActions.toString();
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
		FlowControl retval = (FlowControl)super.clone();
		if (retval != null) {
			if (this.cond != null)
				retval.cond = (Conditional)this.cond.clone();
			if (this.thenActions != null) 
				retval.thenActions = (ActionFactory)this.thenActions.clone();
			if (this.elseActions != null) 
				retval.elseActions = (ActionFactory)this.elseActions.clone();
			if (this.elseif != null)
				retval.elseif = (ElseIf)this.elseif.clone();
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
	 public void updateUIDs(int newUID, int origUID) {
		 cond.updateUIDs(newUID, origUID);
		 if (thenActions != null)
			 thenActions.updateUIDs(newUID, origUID);
		 if (elseActions != null)
			 elseActions.updateUIDs(newUID, origUID);
		 if (elseif != null)
			 elseif.updateUIDs(newUID, origUID);
//		 else if (cond instanceof ComparisonOp)
//			 ((ComparisonOp)cond).updateUIDs(newUID, origUID);
	 }
		
//	 This method was added as a validator for cloning
//	 public String me() {
//			String result = new String();
//			if (thenActions != null)
//				result += "\t\tthen  " + thenActions.me();
//			
//			if (elseActions != null) 
//				result += "else\t" + elseActions.me();
//			return result;
//		}
}
