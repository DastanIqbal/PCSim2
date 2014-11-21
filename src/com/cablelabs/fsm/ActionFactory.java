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

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

/**
 * This is a container class to hold all of the Actions
 * that may be executed sequentially within a single
 * State operation. It also the interface to the FSM to
 * start each of the the action's execution on behalf of
 * the State.
 *
 * The platform currently only supports an ActionFactory
 * in each of the following operations:
 * prelude, postlude, then and else. When a state contains
 * more than one operation, each operation has its' own
 * ActionFactory.
 *
 * @author ghassler
 *
 */
public class ActionFactory implements Cloneable {

	/**
	 * Actual container of Actions for the factory.
	 */
	protected LinkedList<Action> actions;

	/**
	 * Logger
	 */
	private LogAPI logger = LogAPI.getInstance();

	/**
	 * The subcategory to use when logging
	 *
	 */
	private String subCat = null;

	/**
	 * A flag indicating that this action factory should only execute
	 * its' actions once or everytime.
	 */
	private boolean once = false;

	/**
	 * An internal flag indicating that the actions have been executed
	 * at least once.
	 */
	private boolean executed = false;

	/**
	 * Constructor
	 *
	 */
	public ActionFactory(String subcat, boolean once) {
		//this.logger = logger; // Logger.getLogger(FSM.class);
		this.subCat = subcat;
		this.once = once;
		this.actions = new LinkedList<Action>();
	}

	/**
	 * Copy Constructor
	 *
	 *
	 */
//	public ActionFactory(ActionFactory orig) {
//		this.actions = new LinkedList<Action>(orig.actions);
//	}
	/**
	 * Adds a new Action to the factory.
	 * @param action - adds any class that derives from the Action class
	 * 			to the container.
	 */
	public void addAction(Action action) {
		actions.add(action);
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
		ListIterator<Action> iter = actions.listIterator();
		while (iter.hasNext()) {
			Action a = iter.next();
			if (a instanceof Send) {
				Send s = (Send)a;
				LinkedList<Mod> mods = s.getModifiers();
				if (mods != null) {
					ListIterator<Mod> mIter = mods.listIterator();
					while (mIter.hasNext()) {
						Mod m = mIter.next();
						Reference ref = m.getRef();
						if (ref instanceof MsgRef) {
							if (((MsgRef)ref).getUID() == origUID)
								((MsgRef)ref).setUID(newUID);
						}
						else if (ref instanceof VarExprRef)
							((VarExprRef)ref).updateUID(newUID, origUID);
					}
				}
			}
			else if (a instanceof Proxy) {
				Proxy p = (Proxy)a;
				LinkedList<Mod> mods = p.getModifiers();
				if (mods != null) {
					ListIterator<Mod> mIter = mods.listIterator();
					while (mIter.hasNext()) {
						Mod m = mIter.next();
						Reference ref = m.getRef();
						if (ref instanceof MsgRef) {
							if (((MsgRef)ref).getUID() == origUID)
								((MsgRef)ref).setUID(newUID);
						}
						else if (ref instanceof VarExprRef)
							((VarExprRef)ref).updateUID(newUID, origUID);
					}
				}
			}

		}
	}

	/**
	 * When invoked, it loops through all of the elements in the
	 * container and call's their respective execute method.
	 * @param api - The FSM API to perform the actual Action operation
	 * 			for an element.
	 * @throws PC2Exception - This is thrown when an error occurs with an
	 * 			element in the container.
	 */
	public void executeActions(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		if (once && executed) {
			logger.debug(PC2LogCategory.FSM, subCat,
					"The actions have executed once already.");
			return;
		}
		int index = msgQueueIndex;
		for (int i = 0; i < actions.size(); i++) {
			try {
				Action a = actions.get(i);
				a.execute(api, index);
				executed= true;
			}
			catch (Exception e) {
				logger.warn(PC2LogCategory.FSM, subCat,
						"Exception occurred during processing of action event. ", e);

			}
		}
	}

	public void setOnce(boolean flag) {
		this.once = flag;
	}

	/**
	 * A string representation of the contents of the container.
	 *
	 * @return - a String listing of the actions currently in the container.
	 */
	@Override
    public String toString() {
		String result = null;
		for (int i = 0; i < actions.size(); i++) {
			try {
				Action a = actions.get(i);
				if (result == null) {
					if (once)
						result = "Actions occur once:\n" + a.toString();
					else
						result = "Actions occur everytime:\n" + a.toString();
				}
				else
					result += a.toString();
			}
			catch (Exception e) {
				// ignore for now
			}
		}
		return result;
	}

//	 This method was added as a validator for cloning
//	public String me() {
//		String result = "\t" + super.toString() + "  ";
//		for (int i = 0; i < actions.size(); i++) {
//			try {
//				Action a = (Action)actions.get(i);
//				if (a.me().equals("null"))
//					System.out.println(result);
//				if (result == null)
//					result = a.me();
//				else
//					result += " " + a.me();
//			}
//			catch (Exception e) {
//				// ignore for now
//			}
//		}
//
//		return result;
//	}

	/** This implements a deep copy of the class for replicating
	 * FSM information.
	 *
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */
	@Override
    public Object clone() throws CloneNotSupportedException {
		ActionFactory retval = (ActionFactory)super.clone();
		retval.once = this.once;
		if (retval != null ) {
			if (this.actions != null) {
				retval.actions = new LinkedList<Action>();
				ListIterator<Action> iter = this.actions.listIterator();
				while (iter.hasNext()) {
					Action a = (Action)iter.next().clone();
					if (a != null)
						retval.actions.add(a);
				}
			}
		}
//		System.out.println("this " + this.me());
//		System.out.println("retval " + retval.me());
		return retval;
	}
}
