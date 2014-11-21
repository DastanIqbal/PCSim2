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
 * A container for the LOG Action defined within a PC 2.0 Simulator
 * XML document.
 * 
 * @author ghassler
 *
 */
public class LogMsg implements Action { 

	/**
	 * The message to log to the console and any log files currently
	 * in use by the platform.
	 */
	private String expr;
	
	/**
	 * The debug level of the message. By default all messages are
	 * configured to be an INFO severity.
	 */
	private String level = "info";
	
	/**
	 * Defines whether this message should wait for a response from the
	 * user before proceeding.
	 */
	private boolean promptUser = false;
	
	/**
	 * Defines if this log message is a verification request or
	 * simply a notice message.
	 */
	private boolean verify = false;
	
	/**
	 * Defines the whether yes is the expected response to the
	 * request for verification or not. This flag is only used
	 * when promptUser and verify are both true.
	 */
	private boolean yesExpected = true;
	
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
	 * This is which step in the test case this is 
	 * class is verifying.
	 */
	protected String step = null;
	/**
	 * This is comma separated list of the REQPRO requirement
	 * numbers this class is verifying.
	 */
	protected String requirements = null;
	
	protected String group = null;
	/**
	 * This is the state that the verify is associated
	 * with during the execution of the test.
	 */
	protected State state = null;

	/**
	 * Constructor.
	 * @param expr
	 */
	public LogMsg(State s, String expr) {
		this.state = s;
		this.expr = expr;
	}
	
	/**
	 * Constructor. 
	 * @param expr
	 * @param level
	 * @param prompt
	 */
	public LogMsg(String expr, String level, boolean prompt) {
		this.expr = expr;
		this.level = level;
		this.promptUser = prompt;
	}

	/**
	 * Constructor 
	 * @param expr
	 * @param level
	 * @param prompt
	 * @param verify
	 * @param yesExpected
	 */
	public LogMsg(String expr, String level, boolean prompt, 
			boolean verify, boolean yesExpected) {
		this.expr = expr;
		this.level = level;
		this.promptUser = prompt;
		this.verify = verify;
		this.yesExpected = yesExpected;
	}
	/**
	 * Gets the message to log.
	 * @return
	 */
	public String getExpr() {
		return expr;
	}
	
	/**
	 * Gets the debug level of the message.
	 * @return
	 */
	public String getLevel() {
		return level;
	}
	
	/**
	 * Gets the flag whether to prompt the user.
	 * @return
	 */
	public boolean getPromptUser() {
		return promptUser;
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
	 
	 public String getGroup() {
		 return this.group;
	 }
	/**
	 * Gets the verify flag
	 * @return
	 */
	public boolean isVerify() {
		return verify;
	}
	
	/**
	 * Gets the yesExpected flag
	 * @return
	 */
	public boolean isYesExpected() {
		return yesExpected;
	}
	
	/**
	 * Sets the debug level of the message.
	 * @param level
	 */
	public void setLevel(String level) {
		this.level = level;
	}
	
	public void setPassed(boolean flag) {
		this.passed = flag;
	}
	/**
	 * Sets the prompt user flag to the specified value.
	 */
	public void setPromptUser(boolean prompt) {
	
		this.promptUser = prompt;
	}
	
	/**
	 * Sets the verify flag to the specified value
	 */
	public void setVerify(boolean verify) {
		this.verify = verify;
	}
	
	/**
	 * Sets the yesExpected flag to the specified value.
	 */
	public void setYesExpected(boolean yesExpected) {
		this.yesExpected = yesExpected;
	}
	
	public void setGroup(String g) {
		this.group = g;
	}
	/** 
	 * This method performs the operation specified by 
	 * the action
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		executed = true;
		api.log(this);
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = "\tlog expr=\"" + expr + "\"";
		if (level != null) 
			result += " level=\"" + level + "\"";
		if (promptUser) {
			result += " promptUser=\"" + promptUser + "\""; 
			if (verify) {
				result += " verify=\"" + verify 
					+ "\" yesExpected=\"" + yesExpected + "\""
					+ "\" step=\"" + step + "\"" 
					+" requirements=\"" + requirements + "\""
					+" group=\"" + group + "\"";
			}
		}
		result += "\n";
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
		LogMsg retval = (LogMsg)super.clone();
		if (retval != null ) {
			if (this.expr != null) 
				retval.expr = new String(this.expr);
			if (this.level != null)
				retval.level = new String(this.level);
			retval.promptUser = this.promptUser;
			retval.verify = this.verify;
			retval.yesExpected = this.yesExpected;
			if (step != null)
				retval.step = new String(this.step);
			if (requirements != null) 
				retval.requirements = new String(this.requirements);
			if (group != null) 
				retval.group = new String(this.group);
			retval.state = null;
			// executed and passed should not be cloned.
		}	

		return retval;
	}
	
	 public void setState(State s) {
		 this.state = s;
	 }
	 
	 public void setStep(String s) {
		 this.step = s;
	 }
	 
	 public void setRequirements(String r) {
		 this.requirements = r;
	 }
	 
	public boolean passed () {
		return (executed & passed);
	}

//	public String me() {
//		return "L " + super.toString();
//	}
}
