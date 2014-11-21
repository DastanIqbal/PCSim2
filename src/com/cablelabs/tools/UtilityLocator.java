/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.tools;

import java.util.Arrays;
import java.util.ListIterator;

import com.cablelabs.fsm.ArrayIndex;
import com.cablelabs.fsm.UtilityMsg;
import com.cablelabs.fsm.UtilityRef;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.utility.UtilityArrayAttribute;
import com.cablelabs.utility.UtilityAttribute;
import com.cablelabs.utility.UtilityMessage;

public class UtilityLocator {

	/**
	 * Private logger for the class
	 */
	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger("Locators");

	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Locator";

	private static UtilityLocator locator = null;

	private static RefLocator refLocator = RefLocator.getInstance();
	
	/**
	 * Private Constructor
	 *
	 */
	private UtilityLocator() {

	}

	/**
	 * Retreives the single instance of the SDPLocator if it 
	 * already exists. If it doesn't exist it will create it prior
	 * to returning it.
	 *
	 */
	public synchronized static UtilityLocator getInstance() {
		if (locator == null) {
			locator = new UtilityLocator();
		}
		return locator;
	}

	public String locateUtilityValue(UtilityRef ref, UtilityMsg msg) {
		// For the time being utility messages shouldn't have duplicate header instances
		// so we ignore the parameter for now. It is being passed into the method on the
		// off chance it becomes required someday.
		String hdr = ref.getHeader();
		String param = ref.getParameter();
		UtilityMessage um = msg.getUtilityEvent().getMessage();
		if (hdr == null) {
			if (ref.getMsgType().equals("Message")) {
				return msg.getEventType();
			}
		}
		else if (ref.isReferenceOnEvent()) {
			return refLocator.getEventReference(hdr, param, msg);
		}
		else if (hdr.equals("transactionId")) {
			return  um.getTransactionID();
		}
		else if (ref.hasArrayReference() && um != null) {
			ArrayIndex ai = ref.getArrayReference().getIndexes();
			if (ai != null) {
				Integer [] indexes = ai.getIndexes();
				if (indexes.length > 0) {
					ListIterator<UtilityAttribute> iter = um.getAttributes();
					while (iter.hasNext()) {
						UtilityAttribute ua = iter.next();
						if (ua != null && ua.getName().equals(hdr)
								&& ua instanceof UtilityArrayAttribute &&
								indexes.length == 3) {
							 return ((UtilityArrayAttribute)ua).getElement(indexes);
						}
					}
					logger.error(PC2LogCategory.UTILITY, subCat,
							"Unable to locate any element for the array attribute at position " + Arrays.toString(indexes));
				}
			}
		}
		else {
			ListIterator<UtilityAttribute> iter = um.getAttributes();
			while (iter.hasNext()) {
				UtilityAttribute ua = iter.next();
				if (ua != null && ua.getName().equals(hdr)) {
					return ua.getValue();
				}
			}
		}
		return null;
	}
	
	public UtilityArrayAttribute locateArrayValue(UtilityRef ref, UtilityMsg msg) {
		String hdr = ref.getHeader();
		UtilityMessage um = msg.getUtilityEvent().getMessage();
		if (hdr != null) {
			ListIterator<UtilityAttribute> iter = um.getAttributes();
			while (iter.hasNext()) {
				UtilityAttribute ua = iter.next();
				if (ua != null && 
						ua instanceof UtilityArrayAttribute && 
						((UtilityArrayAttribute)ua).getName().equals(hdr)) {
					return (UtilityArrayAttribute)ua;
				}
			}
		}
		
		return null;
	}
}
