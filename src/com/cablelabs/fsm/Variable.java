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
import com.cablelabs.tools.RefLocator;
import com.cablelabs.tools.UtilityLocator;
import com.cablelabs.utility.UtilityArrayAttribute;

public class Variable implements ActiveOp, Action {

	private LogAPI logger = LogAPI.getInstance();
	/**
	 * This is the global variable name that the
	 * script assigned to some string or array
	 * for future referencing
	 * 
	 */
	private String name = null;
	
	/**
	 * The reference to retreive the value from when performOp
	 * is invoked
	 */
	private Reference ref = null;
	
	/**
	 * This object is either a String or a multi-dimensional array
	 * of Strings.
	 */
	private Object variable = null;
	
    /**
     * This is a container describing the number of dimensions of
     * the array and what is the maximum number of elements foreach
     * dimension of the array. 
     * 
     */
	private ArrayIndex indexes = null;
    
	/**
	 * A handle to the Ref Locator for retrievial of the data we need
	 */
	static private RefLocator refLocator = RefLocator.getInstance();
	
	/**
	 * A handle to the UtilityLocator to obtain the UtilityArrayAttribute
	 * for further processing
	 */
	static private UtilityLocator utilLocator = UtilityLocator.getInstance();
	
//	static private SIPLocator sipLocator = SIPLocator.getInstance();
	
	static private GlobalVariables gv = GlobalVariables.getInstance();
	
	/**
	 * A handle to the MsgQueue for obtaining the message being referred to
	 * in the ref attribute.
	 */
	static private MsgQueue q = MsgQueue.getInstance();
	
	private boolean containsSIPMsg = false;
	
	private boolean containsSDPBody = false;
	
	public Variable(String name) {
		this.name = name;
	}

	
	public String getName() {
		return name;
	}
	
	public String getElement(Integer [] index) {

		if (variable instanceof String [][][] &&
				index.length == 3) {
			String [][][] value = (String[][][])variable;
			try {
				return value[index[0]][index[1]][index[2]];
			}
			catch (ArrayIndexOutOfBoundsException oob) {
				if (index[0] >= indexes.get(0)) {
					logger.error(PC2LogCategory.FSM, "", 
							"The first index is beyond the bounds of the variable=" + name + ". Argument said to use "
							+ index[0] + ", but value should be less than " + indexes.get(0));
				}
				if (index[1] >= indexes.get(1)) {
					logger.error(PC2LogCategory.FSM, "", 
							"The second index is beyond the bounds of the variable=" + name + ". Argument said to use "
							+ index[1] + ", but value should be less than " + indexes.get(1));
				}
				if (index[2] >= indexes.get(2)) {
					logger.error(PC2LogCategory.FSM, "", 
							"The third index is beyond the bounds of the variable=" + name + ". Argument said to use "
							+ index[2] + ", but value should be less than " + indexes.get(2));
				}
			}
		}
		return null;
	}
	
	public String getElement() {
		if (variable instanceof String) {
			return (String)variable;
		}
		return null;
	}
	
//	public ArrayIndex getIndexes() {
//		return indexes;
//	}

	public Object getVariable() {
		return variable;
	}
	
	public Reference getRef() {
		return ref;
	}
	
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.createVariable(this, msgQueueIndex);
	}
	
	public void setRef(Reference r) {
		this.ref = r;
	}

	private UtilityArrayAttribute locateArray(UtilityRef ref,  Object event, int msgQueueIndex) {
		boolean nullHeader = (ref.getMsgType() == null);
		if (!nullHeader) {
			if (ref.getMsgInstance().equals(MsgQueue.CURRENT)) {
				if (event != null && event instanceof UtilityMsg) {
					String msgType = ((UtilityMsg)event).getEventType();
					if (msgType != null) {
						return utilLocator.locateArrayValue(ref, (UtilityMsg)event);
					}
				}
			}	
			else if (ref.getMsgInstance().equals(MsgQueue.ANY)) {
				LinkedList<MsgEvent> searchResults = q.findAll(ref.getUID(), 
						ref.getMsgType(), msgQueueIndex);
				ListIterator<MsgEvent> iter = searchResults.listIterator();
				while(iter.hasNext()) {
					Object element = iter.next();
					if (element instanceof UtilityMsg) {
						String msgType = ((UtilityMsg)element).getEventType();
						if (msgType != null) {
							return utilLocator.locateArrayValue(ref, (UtilityMsg)element);
						}
					}
				}
			}
			else {
				MsgEvent msgEvent = q.find(ref.getUID(), ref.getMsgType(), 
						ref.getMsgInstance(), msgQueueIndex);
				if (msgEvent instanceof UtilityMsg) {
					String msgType = ((UtilityMsg)msgEvent).getEventType();
					if (msgType != null) {
						return utilLocator.locateArrayValue(ref, (UtilityMsg)msgEvent);
					}
				}
			}
		}
		return null;
	}

	/**
	 * This method recursively builds the variable attribute from the UtilityArrayAttribute
	 * 
	 * @param dim - the current dimensions we are working upon
	 * @param wildcard - the dimension that was the wildcard
	 * @param match - the specific value of the wildcard that matched.
	 * @param maxIndex - the maximum index for each dimension
	 * @param curIndex - the current indexing value to get from the UtilityArrayAttribute
	 * @param uaa - The UtilityArrayAttribute to obtain the specific element value from.
	 */
	private void loadVariable(int dim, int wildcard, int match, Integer [] maxIndex,
			Integer [] curIndex, UtilityArrayAttribute uaa) {

		if (dim == (maxIndex.length-1)) {
			if (dim == wildcard) {
				curIndex[dim] = match;
				String tmp = uaa.getElement(curIndex);
				// Now create the indexes for the local variable
				Integer [] loadIndex = curIndex.clone();
				loadIndex[wildcard] = 0;
				logger.info(PC2LogCategory.FSM, "",
						" Storing variable[" + curIndex[0] + "][" + curIndex[1] + "][" + curIndex[2] + "]=" + tmp);
				try {
					String [][][] value = ((String[][][])variable);
					value[curIndex[0]][curIndex[1]][curIndex[2]] = tmp;
				}
				catch (ArrayIndexOutOfBoundsException oob) {
					logger.warn(PC2LogCategory.FSM, "",
							"index value" + curIndex[0] + "][" + curIndex[1] + "][" + curIndex[2] + "] is beyond the array's boundary.");
				}
			}
			else {
				for (int i = 0; i < maxIndex[dim]; i++) {
					curIndex[dim] = i;
					String tmp = uaa.getElement(curIndex);
					// Now create the indexes for the local variable
					Integer [] loadIndex = curIndex.clone();
					loadIndex[wildcard] = 0;
					
					logger.info(PC2LogCategory.FSM, "",
							" Storing variable[" + loadIndex[0] + "][" + loadIndex[1] + "][" + loadIndex[2] + "]=" + tmp);
					String[][][] value = (String[][][])variable;
					try {
						value[loadIndex[0]][loadIndex[1]][loadIndex[2]] = tmp;
					}
					catch (ArrayIndexOutOfBoundsException oob) {
						logger.warn(PC2LogCategory.FSM, "",
								"index value" + curIndex[0] + "][" + curIndex[1] + "][" + curIndex[2] + "] is beyond the array's boundary.");
					}
					catch (Exception e) {
						logger.warn(PC2LogCategory.FSM, "",
								"index value" + curIndex[0] + "][" + curIndex[1] + "][" + curIndex[2] + "] had exception" + e.getMessage());
					}
				}
			}
		}
		else {
			
			if (dim == wildcard) {
				curIndex[dim] = match;
				loadVariable((dim+1), wildcard, match, maxIndex, curIndex, uaa);
			}
			else {
				for (int i = 0; i < maxIndex[dim]; i++ ) {
					curIndex[dim] = i;
					loadVariable((dim+1), wildcard, match, maxIndex, curIndex, uaa);
				}
			}
		}
	}
	/**
	 * This method is called by the Responses when an event is received
	 * by the State
	 * 
	 * @param api - the class to call when needing actions executed.
	 * @param ce - the class to perform the conditional's evaluation.
	 * @param event - the current event that cause the condition to be evaluated.
	 * 
	 * @return - true if the task completes successfully, false otherwise.
	 * 
	 * @throws PC2Exception
	 */
	@Override
	public boolean performOp(FSMAPI api, ComparisonEvaluator ce, MsgEvent event) throws PC2Exception {
		if (ref instanceof UtilityRef &&
				((UtilityRef)ref).hasArrayReference()) {
			ArrayRef ar = ((UtilityRef)ref).getArrayReference();
			Conditional cond = ar.getCond();
			if (cond != null //&& cond.hasWildcardIndex()
					) {
				// Get the UtilityArrayAttribute from the message reference
				UtilityArrayAttribute uaa = locateArray((UtilityRef)ref, event, event.getMsgQueueIndex());
				if (uaa != null) {
					Integer wildcard = cond.getWildcardIndex();
					Integer maxIndex = uaa.getMaximumIndex(wildcard);
					if (wildcard != null && 
							wildcard != -1 && 
							maxIndex != null) {
						boolean result = false;
						for (int i = 0; i < maxIndex && !result; i++) {
							// Update the wildcard to the current index value that
							// is being looped upon
							cond.updateWildcardIndex(i);
							result = cond.evaluate(ce, event);
							if (result) {
								// We found a dimension of the array that matches
								// the criteria defined by the Conditional
								// Now calculate the dimensions and copy the indexes
								// values into our on attribute.
								indexes = new ArrayIndex(uaa.getDimensions());
								// Calculate the number of elements in the array
								indexes.setIndex(wildcard, 1);
//								int elements = indexes.get(0);
//								for (int j = 1; j < indexes.length(); j++)
//									elements *= indexes.get(j);
								variable = new String [indexes.get(0)][indexes.get(1)][indexes.get(2)];
								// Set the location of the wildcard
								indexes.setWildcard(wildcard);
								// Set the value at the wildcard location to zero for single
								// dimension
//								indexes.setIndex(wildcard, 0);
								// Load the variable attribute with the data
								if (variable != null) {
									loadVariable(0, wildcard, i, indexes.getIndexes().clone(),
											indexes.getIndexes().clone(), uaa);
								}
//								indexes.setIndex(wildcard, 1);
							}
						}
						cond.resetWildcardIndex();
					}
				}
				else {
					String value = new String(refLocator.getReferenceInfo(((UtilityRef)ref).getUID(), ref, event));
					if (value != null)
						variable = value;
				}
			}
			else if (event instanceof UtilityMsg){
				String value = new String(utilLocator.locateUtilityValue((UtilityRef)ref, (UtilityMsg)event));
				if (value != null)
					variable = value;
			}
		}
		else if (ref instanceof VarRef) {
			VarRef vr = (VarRef)ref;
			GlobalVariables gv = GlobalVariables.getInstance();
			Variable var = gv.get(vr.getName());
			Integer [] indexes = vr.getIndexes();
			if (var != null) {
				if (indexes != null) {
					variable = var.getElement(indexes);
//					if (value != null) {
//					value = checkForBinaryRef(ref, value);
//					ll.add(value);
//					}
				}
				else {
					String value = vr.resolve(var);
					variable = new String(value);
				}
			}
		}
		else if (ref instanceof MsgRef){
			MsgRef mr = (MsgRef)ref;
			String value = refLocator.getReferenceInfo(mr.getUID(), ref, event);
			if (mr.isArithmeticRef()) {
				// First see if the value is a number
				try {
					int num = Integer.parseInt(value);
					if (mr.addRef)
						num += mr.getArithmeticMod();
					else if (mr.subRef)
						num -= mr.getArithmeticMod();
					variable = Integer.toString(num);
				}
				catch (NumberFormatException nfe) {
					logger.warn(PC2LogCategory.FSM, "",
							" The value[" + value 
							+ "] obtained to store in the variable doesn't appear "
							+ "to be a number. No arithmetic operation could be performed." 
							+ " Using the original value obtained.");
					variable = new String(value);
				}
			}
			else if (value != null) {
				// Next lets determine if the message being stored is a
				// complete SIP message or SDP body
				if (mr instanceof SIPRef && 
						((SIPRef)mr).getHeader() == null)
					containsSIPMsg = true;
				else if (mr instanceof SDPRef && 
						((SDPRef)mr).getHeader() == null)
					containsSDPBody = true;
				
				variable = new String(value);
			}
		}
		else if (ref instanceof Literal) {
			variable = ((Literal)ref).getExpr();
		}
		else if (ref instanceof VarExprRef) {
			variable = ((VarExprRef)ref).getExpr(event.getMsgQueueIndex());
			
		}
		if (variable != null) {
			// First see if the variable exists
			String op = "Creating";
			Variable prev = gv.get(name);
			if (prev != null) 
				op = "Replacing";
			gv.put(name, this);
			String msg = "";
			
			if (variable instanceof String[][][]) {
				msg += getTable();
				logger.info(PC2LogCategory.FSM, "", 
					op + " variable=\""
					+ name + "\" contains the elements - [\n" + msg + "]");
			}
			else if (variable instanceof String)
				logger.info(PC2LogCategory.FSM, "", 
					op +" variable=\""
						+ name + "\" contains one element - [" + (String)variable + "]");
		}
		else 
			logger.warn(PC2LogCategory.FSM, "",
					"Nothing stored in variable=" + name + ".");
		
		return true;
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
		if (ref instanceof MsgRef) {
			if (((MsgRef)ref).getUID() == origUID)
				((MsgRef)ref).setUID(newUID);
		}
		else if (ref instanceof VarExprRef) 
			((VarExprRef)ref).updateUID(newUID, origUID);

	}
		
		
	@Override
	public String toString() {
		String result = "\tvar name=" + name;

		if (ref != null)
			result += " ref=" + ref;
		if (indexes != null)
		result += " index=" + indexes;

		if (variable != null)
			if (variable instanceof String)
				result += " variable=" + variable;
			else if (variable instanceof String[][][]) {
				if (indexes != null) {
					result += " index=" + indexes;
					result += "\n" + getTable();
				}
			}
				

		result += "\n";
		
		return result;
	}
	
	private String getTable() {
		if (variable instanceof String[][][]) {
			String msg = "";
			String [][][] value = (String[][][])variable;
			for (int i = 0; i < indexes.get(0); i++)
				for (int j = 0; j < indexes.get(1); j++)
					for (int k=0; k < indexes.get(2); k++)
						msg += "\telement[" + i + "][" + j + "][" + k + "]="+ value[i][j][k] + "\n";

			return msg;
		}
		return null;
	}
	/**
	 * Creates a copy of the class
	 */
	@Override
	public Object clone()  throws CloneNotSupportedException {
		Variable retval = (Variable)super.clone();
		if (retval != null) {
			if (this.name != null) 
				retval.name = new String(this.name);
			// The variable value should never be copied
			// set to null
			retval.variable = null;
			if (this.indexes != null) {
				retval.indexes = (ArrayIndex)this.indexes.clone();
			}
		}	
		
		return retval;
		
	}
	
	public boolean isSIPMsg() {
		return this.containsSIPMsg;
	}

	public boolean isSDPBody() {
		return this.containsSDPBody;
	}
}
