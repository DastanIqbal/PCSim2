/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.models;

import com.cablelabs.fsm.FSM;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.sim.PCSim2;


/**
 * This is the Register model.Most of the operations for this 
 * class are common to all models and are defined in the PC2Models 
 * base class.
 * 
 * @author ghassler
 *
 */
public class Register extends PC2Models {

	static private final String model = "Register";
	/**
	 * Constructor for the Register model.
	 * 
	 * @param fsm - the FSM to use for the test.
	 */
	public Register(FSM fsm) {
		super(fsm,Register.class.getName(), model);
		fsm.setAPI(this);
		fsm.setComparisonEvaluator(examiner);
	}
	
	@Override
	public void init() {
		super.init();
	}


	@Override
	public void run() {
		// First set the state into the initial state
		
		logger.info(PC2LogCategory.Model, subCat,
				"Beginning " + fsm.getName() + " thread.");
		try {
			fsm.init(queue, this);
		}
		catch (IllegalStateException ise) {
			PCSim2.setTestPassed(false);
			String err = "Session model failed during state machine initialization." + 
			" Test terminated. Declaring test case failure.";
			logger.fatal(PC2LogCategory.Model, subCat, err);
			shuttingDown = true;
			PCSim2.setTestComplete();
		}
		
		super.run();
		
	}
}
