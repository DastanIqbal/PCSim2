/*
######################################################################################
##                                                                                  ##
0## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

public class Verify extends If implements Action {

	/**
	 * A flag indicating whether the verification passed or
	 * failed
	 */
	protected boolean passed = false;
	
	/**
	 * A flag indicating that the verification was executed
	 */
	protected boolean executed = false;
	
	/**
	 * A string label indicating that this verification is 
	 * duplicated with another verification and the test only
	 * needs one of the group to pass.
	 */
	protected String group = null;
	
	protected LogAPI logger = LogAPI.getInstance();
	
	protected static final String subCat = "Verify";
	
	/**
	 * This is which step in the test case this is 
	 * class is verifying.
	 */
	protected String step = null;
	/**
	 * This is comma separated list of the REQPRO requirement
	 * numbers this class is verifying.
	 */
	protected String requirements = null;
	
	/**
	 * This is the state that the verify is associated
	 * with during the execution of the test.
	 */
	protected State state = null;
	
	/**
	 * Constructor.
	 *
	 */
	public Verify(State s) {
		super();
		this.state = s;
		
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = null;
		if (executed) {
			if (passed)
			result = "VERIFY PASSED";
			else 
				result = "VERIFY FAILED";
		}
		else
			result = "Verify untested";
		
		if (step != null)
			result += " Step " + step;
		if (requirements != null) 
			result += " Requirement " + requirements;
		if (group != null)
			result += " Group[" + group + "]";
		
		result += " - \n\t";
		if (cond instanceof ComparisonOp) 
			result += ((ComparisonOp)cond).display();
		else if (cond instanceof LogicalOp) 
			result += ((LogicalOp)cond).display();
		if (thenActions != null)
			result += "\npassed actions \n" + thenActions.toString();
//		if (elseif != null) 
//		result += elseif.toString();
		if (elseActions != null) 
			result += "\nfailed actions \n" + elseActions.toString();
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
		Verify retval = (Verify)super.clone();
		// executed and passed should not be cloned.
		if (this.group != null)
			retval.group = new String(this.group);
		if (step != null)
			retval.step = new String(this.step);
		if (requirements != null) 
			retval.requirements = new String(this.requirements);
		retval.state = null;
		
		return retval;
	}
	
	/**
	 * Common operation to perform the action described by the
	 * derived class.
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.verify(this, msgQueueIndex);
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
		 executed = true;
		 if (cond instanceof LogicalOp) {
			 if (((LogicalOp)cond).evaluate(ce, event)) {
				 passed = true;
				 // Use the log category LogMsg because the
				 // user can not disable this category
				 logger.info(PC2LogCategory.LOG_MSG, subCat, toString());
				 
				 // Execute the passed actions if there are any
				 if (thenActions != null) {
					 thenActions.executeActions(api, event.getMsgQueueIndex());
				 }
			 }
//			 else if (elseif != null) {
//				 return elseif.performOp(api, ce, event);
//			 }
			 else {
				// Use the log category LogMsg because the
				 // user can not disable this category
				 passed = false;
				 logger.error(PC2LogCategory.LOG_MSG, subCat, toString());
				 
				 if (elseActions != null) {
					 elseActions.executeActions(api, event.getMsgQueueIndex());
				 }
			 }
		 }
		 else if (cond instanceof ComparisonOp) {
			 if (((ComparisonOp)cond).evaluate(ce, event)) {
				 passed = true;
				// Use the log category LogMsg because the
				 // user can not disable this category
				 logger.info(PC2LogCategory.LOG_MSG, subCat, toString());
				 
				 if (thenActions != null) {
					 thenActions.executeActions(api, event.getMsgQueueIndex());
				 }
			 }
//			 else if (elseif != null) {
//				 return elseif.performOp(api, ce, event);
//			 }
			 else {
				// Use the log category LogMsg because the
				 // user can not disable this category
				 passed = false;
				 logger.error(PC2LogCategory.LOG_MSG, subCat, toString());
				 
				 if (elseActions != null) {
					 elseActions.executeActions(api, event.getMsgQueueIndex());
				 }
			 }
		 }
		 return true;
	 }
	 
	 public String getGroup() {
		 return this.group;
	 }
	 
	 public String getRequirements() {
		 return this.requirements;
	 }
	 
	 public State getState() {
		 return this.state;
	 }
	 
	 public String getStep() {
		 return this.step;
	 }
	 
	 /** 
	  * For the verify to have succeeded it needs to have both been executed and
	  * passed 
	  * @return
	  *
	  */
	 public boolean passed() {
		 return (executed && passed);
	 }
	 
	 public void setGroup(String group) {
		 this.group = group;
	 }
	 
	 @Override
	 public void setElseif(ElseIf elseif) {
		 logger.warn(PC2LogCategory.LOG_MSG, subCat, 
				 "The elseif tag is not valid for the verify parent tag.");
	 }
	 
	 public void setStep(String s) {
		 this.step = s;
	 }
	 
	 public void setState(State s) {
		 this.state = s;
	 }
	 
	 public void setRequirements(String r) {
		 this.requirements = r;
	 }
}
