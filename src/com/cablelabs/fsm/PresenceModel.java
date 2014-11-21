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


public class PresenceModel extends Model {
	
	private LinkedList<ChangeStatus> elements = null;
	
	public PresenceModel(LinkedList<ChangeStatus> e) {
		this.elements = e;
	}
	
	public PresenceModel() {
		this.elements = new LinkedList<ChangeStatus>();
	}
	
	public void addElement(ChangeStatus cs) {
		if (elements != null) {
			elements.add(cs);
		}
	}
	
	public LinkedList<ChangeStatus> getElements() {
		return this.elements;
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = super.toString();

		if (elements != null) {
			ListIterator<ChangeStatus> iter = elements.listIterator();
			
			while (iter.hasNext()) {
				ChangeStatus item = iter.next();
				if (item != null)
					result += "\n    label=" + item.getLabel() + " status=" + item.getStatus();
			}
		}
		return result;
	}
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		PresenceModel retval = (PresenceModel)super.clone();
		if (retval != null) {
			if (this.elements != null) {
				retval.elements = new LinkedList<ChangeStatus>();
				ListIterator<ChangeStatus> iter = this.elements.listIterator();
				while (iter.hasNext()) {
					ChangeStatus cs = iter.next();
					ChangeStatus newCS = (ChangeStatus)cs.clone();
					retval.elements.add(newCS);
				}
			}
			
		}
		return retval;
	}
}
