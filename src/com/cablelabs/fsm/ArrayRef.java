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


public class ArrayRef {

	/**
	 * This allows a conditional to be used to locate a specific
	 * row or column in the array for further processing at a 
	 * later time. 
	 * 
	 * NOTE: This attribute is mutually exclusive with
	 * the indexes attribute. 
	 */
	private Conditional cond = null;
	
	/**
	 * This is the index values to use when obtaining a specific
	 * element within an array
	 * 
	 * NOTE: This attribute is mutually exclusive with
	 * the indexes attribute. 
	 */
	private ArrayIndex indexes = null; 

	public ArrayRef(Conditional c) {
		this.cond = c;
	}
	
	public ArrayRef(ArrayIndex ai) {
		this.indexes = ai;
	}
	
	public Conditional getCond() {
		return cond;
	}

	public void setCond(Conditional cond) throws PC2Exception {
		if (indexes == null) {
			this.cond = cond;
		}
		else 
			throw new PC2Exception("A request to set the conditional operator in an ArrayRef when it already contains an index is invalid.");
	}

	public ArrayIndex getIndexes() {
		return indexes;
	}

	public void setIndexes(ArrayIndex ai) throws PC2Exception{
		if (cond == null) {
			indexes = ai;
		}
		else 
			throw new PC2Exception("A request to set the index to an ArrayRef when it already contains a conditional is invalid.");
	}

//	public MsgRef getMsg() {
//		return msg;
//	}

	@Override
	public String toString() {
		String result = "";
		if (indexes != null) {
			result += indexes;
		}
		else if (cond != null)
			result += "\n" + cond + "\n";
		
		return result;
	}
	
	/**
	 * Creates a copy of the class
	 */
	@Override
	public Object clone()  throws CloneNotSupportedException {
		ArrayRef retval = (ArrayRef)super.clone();
		if (retval != null) {
//			if (this.msg != null) 
//				retval.msg = (MsgRef)this.msg.clone();
			if (this.cond != null)
				retval.cond = (Conditional)this.cond.clone();
			// A shallow copy of the LinkedList should be sufficient
			// Since the data shouldn't change during the test
			if (this.indexes != null) {
				retval.indexes = (ArrayIndex)this.indexes.clone();
			}
		}	
		
		return retval;
		
	}
	
	public String display() {
		String result = " ArrayRef ";
		if (indexes != null) {
			result += indexes;
		}
		else if (cond != null)
			result += "\n" + cond + "\n";
		
		return result;
	}
}
