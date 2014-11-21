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
import java.util.Properties;


/**
 * A container class for the NE element of a PC 2.0
 * Simulator XML document. It contains the network
 * element information that the simulator is attempting
 * to emulate as well as the extensions to use during
 * the test.
 * 
 * @author ghassler
 *
 */
public class NetworkElements implements Cloneable {
	
	/**
	 * The type of simulator, terminating or originating.
	 */
	private String simType;
	
	/**
	 * The network elements the simulator is attempting to
	 * emulate. 
	 * 
	 * NOTE: The order is maintained from the test 
	 * script and the first element in the list is the 
	 * intended target for all correspondence with a peer
	 * network device.
	 */
	private LinkedList<String> elements;
	
	/**
	 * A container for all of the UEs to simulate.
	 */
	private LinkedList<String> ues = null;
	
	/**
	 * A container for all of the P-CSCFs to simulate.
	 */
	private LinkedList<String> pcscf = null;

	/**
	 * A container for all of the S-CSCFs to simulate.
	 */
	private LinkedList<String> scscf = null;

	/**
	 * A container for all of the I-CSCFs to simulate.
	 */
	private LinkedList<String> icscf = null;

	/**
	 * A container for all of the HSSs to simulate.
	 */
	private LinkedList<String> hss = null;

	/**
	 * A container for all of the ASs to simulate.
	 */
	private LinkedList<String> as = null;

	/**
	 * A container for all of the intended targets of the 
	 * messages the platform receives during a test.
	 */
	private LinkedList<String> targets = null;
	
	/**
	 * A container for all of the supported extensions
	 */
	private LinkedList<String> supportedExtensions;
	
	/**
	 * A container for all of the required extensions
	 */
	private LinkedList<String> requireExtensions;
	
	/**
	 * A container for all of the disabled extensions
	 */
	private LinkedList<String> disableExtensions;
	
	/**
	 * The maximum number of extensions that can be contained
	 * in the supported, required, and disbled containers
	 */
//	public static final int MAX_EXTENSIONS = 10;
	
	/**
	 * Constructor
	 * @param type - the simulation type 
	 * @param elements - list of elements to emulate.
	 */
	public NetworkElements (String type, LinkedList<String> elements, 
			LinkedList<String> targets) {
		this.simType = type;
		this.elements = elements;
		this.targets = targets;
		updateElementLists();
	}
	
	/**
	 * A class that updates each of the individual network element 
	 * lists.
	 *
	 */
	private void updateElementLists() {
		ListIterator<String> iter = elements.listIterator();
		
		if (ues != null) ues.clear();
		if (pcscf != null) pcscf.clear();
		if (scscf != null) scscf.clear();
		if (icscf != null) icscf.clear();
		if (hss != null) hss.clear();
		if (as != null) as.clear();
		
		while (iter.hasNext()) {
			String element = iter.next();
			if (element.length() > 2 && element.substring(0,2).equals("UE")) {
				if (ues == null) {
					ues = new LinkedList<String>();
				}
				if (!ues.contains(element))
				    ues.add(element);
			}
			else if (element.length() > 5 && element.substring(0,5).equals("PCSCF")) {
				if (pcscf == null) {
					pcscf = new LinkedList<String>();
				}
				if (!pcscf.contains(element))
				    pcscf.add(element);
			}
			else if (element.length() > 5 && element.substring(0,5).equals("SCSCF")) {
				if (scscf == null) {
					scscf = new LinkedList<String>();
				}
				if (!scscf.contains(element))
				    scscf.add(element);
			}
			else if (element.length() > 5 && element.substring(0,5).equals("ICSCF")) {
				if (icscf == null) {
					icscf = new LinkedList<String>();
				}
				if (!icscf.contains(element))
				    icscf.add(element);
			}
			else if (element.length() > 3 && element.substring(0,3).equals("HSS")) {
				if (hss == null) {
					hss = new LinkedList<String>();
				}
				if (!hss.contains(element))
				    hss.add(element);
			}
			else if (element.length() > 2 && element.substring(0,2).equals("AS")) {
				if (as == null) {
					as = new LinkedList<String>();
				}
				if (!as.contains(element))
				    as.add(element);
			}
		}
	}
	
	/**
	 * A method to verify that all of the element attempting to
	 * be emulated have property information defined in the platform.
	 * @return - true if property information could be obtained for
	 * 		all of the elements, false otherwise.
	 */
	public void certify() throws PC2Exception{
//		boolean result = true;
		if (elements != null) {
			ListIterator<String> iter = elements.listIterator();
			while (iter.hasNext()) {
				String element = iter.next();
				Properties p = SystemSettings.getSettings(element);
				if (p == null) {
					throw new PC2Exception(
							"Test is being declared a failure because the element(" +
							element + ") couldn't be found in the configuration settings.");
//					result = false;
				}
			}
		}
//		return result;
	}

	/**
	 * Gets the maximum number of extensions supported by the platform for
	 * a single setting (supported, required or disabled)
	 * @return
	 */
//	static public int getMaxExtensions() {
//		return MAX_EXTENSIONS;
//	}
	
	/**
	 * Adds the passed in extensions to the disabled extensions container.
	 * 
	 * @param exts = extensions to add to the disabled container
	 * 
	 */
	public void addDisableExtensions(LinkedList<String> exts) {
		ListIterator<String> iter = exts.listIterator();
		while(iter.hasNext()) {
			String ext = iter.next();
			disableExtensions.addLast(ext);
		}
	}
	/**
	 * Deletes an extension from the list of require extensions
	 */
	public void removeDisableExtension(String element) {
		disableExtensions.remove(element);
	}
	/**
	 * Gets the disabled extensions container.
	 * @return
	 */
	public ListIterator<String> getDisableExtensions() {
		return disableExtensions.listIterator();
	}
	/**
	 * Gets the disabled extensions container.
	 * @return
	 */
	public void setDisableExtensions(LinkedList<String> disabledExtensions) {
		this.disableExtensions = disabledExtensions;
	}

	/**
	 * Gets the list of all the elements being simulated.
	 * @return
	 */
	public ListIterator<String> getElements() {
		return elements.listIterator();
	}
	
	/**
	 * Adds a new element to the list of elements
	 * @param element
	 */
	public void addElement(String element) {
		elements.add(element);
		updateElementLists();
	}
	
	/**
	 * Deletes an element from the list of elemetns
	 */
	public void removeElement(String element) {
		elements.remove(element);
		updateElementLists();
	}
	
	/**
	 * Sets the list of all the elements being simulated.
	 * @return
	 */
	public void setElements(LinkedList<String> elements) {
		this.elements = elements;
		updateElementLists();
	}

	/**
	 * Adds the passed in extensions to the require extensions container.
	 * 
	 * @param exts = extensions to add to the require container
	 * 
	 */
	public void addRequireExtensions(LinkedList<String> exts) {
		ListIterator<String> iter = exts.listIterator();
		while(iter.hasNext()) {
			String ext = iter.next();
			requireExtensions.addLast(ext);
		}
	}

	/**
	 * Deletes an extension from the list of require extensions
	 */
	public void removeRequireExtension(String element) {
		requireExtensions.remove(element);
	}
	
	/**
	 * Gets the required extensions container.
	 * @return
	 */
	public ListIterator<String> getRequireExtensions() {
		return requireExtensions.listIterator();
	}

	/**
	 * Sets the required extensions container.
	 * @return
	 */
	public void setRequireExtensions(LinkedList<String> requiredExtensions) {
		this.requireExtensions = requiredExtensions;
	}
	
	/**
	 * Gets the simulation type
	 * @return
	 */
	public String getSimType() {
		return simType;
	}
	
	/**
	 * Sets the simulation type
	 * @param sim_type
	 */
	public void setSimType(String sim_type) {
		this.simType = sim_type;
	}

	/**
	 * Adds the passed in extensions to the supported extensions container.
	 * 
	 * @param exts = extensions to add to the supported container
	 * 
	 */
	public void addSupportedExtensions(LinkedList<String> exts) {
		ListIterator<String> iter = exts.listIterator();
		while(iter.hasNext()) {
			String ext = iter.next();
			supportedExtensions.addLast(ext);
		}
	}

	/**
	 * Deletes an extension from the list of require extensions
	 */
	public void removeSupportedExtension(String element) {
		supportedExtensions.remove(element);
	}
	
	/**
	 * Gets the supported extensions container.
	 * @return
	 */
	public ListIterator<String> getSupportedExtensions() {
		return supportedExtensions.listIterator();
	}

	/**
	 * Sets the supported extensions container.
	 * @return
	 */
	public void setSupportedExtensions(LinkedList<String> supportedExtensions) {
		this.supportedExtensions = supportedExtensions;
	}
	
	/**
	 * Adds a new element to the list of elements
	 * @param element
	 */
	public void addTarget(String target) {
		targets.add(target);
	}
	
	/**
	 * Deletes an element from the list of elemetns
	 */
	public void removeTarget(String target) {
		targets.remove(target);
	}
	
	/**
	 * Sets the required extensions container.
	 * @return
	 */
	public void setTargets(LinkedList<String> targets) {
		this.targets = targets;
	}
    /**
     * Gets the list of UEs being simulated.
     * @return
     */
	public ListIterator<String> getUEs() {
    	if (ues != null)
    		return ues.listIterator();
    	return null;
    }
    
    /**
     * Gets the list of P-CSCFs being simulated.
     * @return
     */
	public ListIterator<String> getPCSCFs() {
    	if (pcscf != null)
    		return pcscf.listIterator();
    	return null;
     }
	
    /**
     * Gets the list of S-CSCFs being simulated.
     * @return
     */
	public ListIterator<String> getSCSCFs() {
    	if (scscf != null)
    		return scscf.listIterator();
    	return null;
     }
    
    /**
     * Gets the list of I-CSCFs being simulated.
     * @return
     */
	public ListIterator<String> getICSCFs() {
    	if (icscf != null)
    		return icscf.listIterator();
    	return null;
     }
    
    /**
     * Gets the list of HSSs being simulated.
     * @return
     */
	public ListIterator<String> getHSSs() {
    	if (hss != null) 
    		return hss.listIterator();
    	return null;
     }
    
    /**
     * Gets the list of ASs being simulated.
     * @return
     */
	public ListIterator<String> getASs() {
    	if (as != null)
    		return as.listIterator();
    	return null;
     }
   
	/**
     * Gets the list of Targets for this FSM.
     * @return
     */
	public ListIterator<String> getTargets() {
		if (targets != null)
			return targets.listIterator();
		return null;
	}
	/**
	 * Gets the size of the list of elements being simulated.
	 * @return
	 */
	public int getElementsSize() {
		if (elements != null)
			return elements.size();
		return 0;
	}
    /**
     * Gets the size of the list of UEs being simulated.
     * @return
     */
	public int getUESize() {
    	if (ues != null)
    		return ues.size();
    	return 0;
    }
    
    /**
     * Gets the size of the list of P-CSCFs being simulated.
     * @return
     */
	public int getPCSCFSize() {
    	if (pcscf != null)
    		return pcscf.size();
    	return 0;
    }
    
    /**
     * Gets the size of the list of S-CSCFs being simulated.
     * @return
     */
	public int getSCSCFSize() {
    	if (scscf != null)
    		return scscf.size();
    	return 0;
    }
    
    /**
     * Gets the size of the list of I-CSCFs being simulated.
     * @return
     */
	public int getICSCFSize() {
    	if (icscf != null)
    		return icscf.size();
    	return 0;
    }
    
    /**
     * Gets the size of the list of HSSs being simulated.
     * @return
     */
	public int getHSSSize() {
    	if (hss != null)
    		return hss.size();
    	return 0;
    }
    
    /**
     * Gets the size of the list of ASs being simulated.
     * @return
     */
	public int getASSize() {
    	if (as != null)
    		return as.size();
    	return 0;
    }
    
	public int getTargetsSize() {
		if (targets != null) {
			return targets.size();
		}
		return 0;
	}
	
	/**
	 * Determines if the specified network element label is contained
	 * in the list of elements supported by the FSM.
	 * 
	 * @param element
	 * @return
	 */
	public boolean contains(String element) {
		return elements.contains(element);
	}
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = "sim type: " + simType; 
		result += "\n    elements: ";
		if (elements.size() > 0) {
			ListIterator<String> iter = elements.listIterator(); 
			while (iter.hasNext()) {
				result += iter.next() + " ";
			}
		}
		else
			result += " none.";
		
		result += "\n    targets: ";
		if (targets.size() > 0) {
			ListIterator<String> iter = targets.listIterator(); 
			while (iter.hasNext()) {
				result += iter.next() + " ";
			}
		}
		else
			result += " none.";
		
		result += "\n    support extensions: ";
		
		if (supportedExtensions != null) {
			ListIterator<String> iter = supportedExtensions.listIterator(); 
			while (iter.hasNext())
				result += iter.next() + " ";
		}
		else
			result += " none.";
		result += "\n    required extensions: ";
		if (requireExtensions != null) {
			ListIterator<String> iter = requireExtensions.listIterator(); 
			while (iter.hasNext())
				result += iter.next() + " ";
		}
		else
			result += " none.";
		result += "\n    disabled extensions: ";
		if (disableExtensions != null) {
			ListIterator<String> iter = disableExtensions.listIterator(); 
			while (iter.hasNext())
				result += iter.next() + " ";
		}
		else
			result += " none.";
		return result;
	}
	/** This implements a shallow copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {
		NetworkElements retval = (NetworkElements)super.clone();
		if (retval != null ) {
			if (this.simType != null) 
				retval.simType = new String(this.simType);
			// NOTE: The attributes below are only performing a shallow
			// copy because the data should remain static for all FSMs
			// that are replicated from the original.
			if (this.elements != null) 
				retval.elements = (LinkedList<String>)this.elements.clone();
			if (this.targets != null) 
				retval.targets = (LinkedList<String>)this.targets.clone();
			if (this.ues != null) 
				retval.ues = (LinkedList<String>)this.ues.clone();
			if (this.pcscf != null) 
				retval.pcscf = (LinkedList<String>)this.pcscf.clone();	
			if (this.scscf != null) 
				retval.scscf = (LinkedList<String>)this.scscf.clone();
			if (this.icscf != null) 
				retval.icscf = (LinkedList<String>)this.icscf.clone();
			if (this.hss != null) 
				retval.hss = (LinkedList<String>)this.hss.clone();
			if (this.as != null) 
				retval.as = (LinkedList<String>)this.as.clone();
			retval.supportedExtensions = (LinkedList<String>)this.supportedExtensions.clone();
			retval.requireExtensions = (LinkedList<String>)this.requireExtensions.clone();
			retval.disableExtensions = (LinkedList<String>)this.disableExtensions.clone();
			// retval.logger = Logger.getLogger(FSM.class);
		}	

		return retval;
	}	

}
