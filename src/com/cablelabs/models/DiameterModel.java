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

import java.util.Properties;

import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.Model;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.sim.PCSim2;
import com.cablelabs.sim.Stacks;

public class DiameterModel extends PC2Models {

	private String neLabel = null;
	static private final String model = "Diameter";
	/**
	 *  Public Constructor for the Registrar model.
	 * 
	 * @param fsm - the FSM to use for the test.
	 * 
	 */
	public DiameterModel(FSM fsm, String neLabel) {
		super(fsm, Registrar.class.getName(), model);
		this.neLabel = neLabel;	
		fsm.setAPI(this);
		fsm.setComparisonEvaluator(examiner);
		Model model = fsm.getModel();
		Properties modelProp = model.getProps();
		if (modelProp != null) {
			String stack = modelProp.getProperty("stack");
			if (stack != null)
				fsm.setDiameterStack(stack);
		}
	}

	@Override
	public void init() {
		super.init();
		if (neLabel != null)  {
			if (neLabel.startsWith("CDF")) {
				Stacks.setDiameterAccountingListener(this);
			}
//			Properties p = SystemSettings.getSettings(neLabel);
//			
//			if (p != null) {
//				String diaStack = p.getProperty(SettingConstants.DIAMETER_STACK_NAME);
//				if (diaStack != null) {
//					ListIterator<String> iter = fsm.getNetworkElements().getTargets();
//					boolean stackInList = false;
//					while (iter.hasNext() && !stackInList) {
//						String target = iter.next();
//						if (target.equals(diaStack)) {
//							stackInList = true;
//						}
//					}
//					if (stackInList)
//						Stacks.setDiameterAccountingListener(this);
//				}
//			}
		}
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
	
	/**
	 * Implementation for the Send action within a FSM.
	 * 
	 * @return - true when the message was sent, false otherwise
	 */
//	public boolean send(Send s) {
//		if (s.getProtocol().equals("diameter")) {
//			DiameterDistributor dist = Stacks.getDiameterDistributor();
//			if (dist != null) {
//				// Next look for this dialog in the table
//				DiameterMsg msgSent =  dist.send(this, s, 
//						fsm.getNetworkElements(), fsm.getDiameterStack());
//				if (msgSent == null)
//					return false;
//				
//				q.add((MsgEvent)msgSent);
//				return true;
//				
//			}
//		}
//		else {
//			return super.send(s);
//		}
//		return false;
//	}
}
