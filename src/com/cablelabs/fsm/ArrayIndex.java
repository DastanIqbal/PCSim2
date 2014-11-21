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
 * This class defines a specific index and its' associated
 * type within an array parameter of a Utility message. 
 * 
 * @author Garey Hassler
 *
 */
public class ArrayIndex implements Reference {

	final public static int WILDCARD_VALUE = -1;
	/**
	 * The location of a wildcard (-1) entry in the array.
	 * If the attribute is null. One does not exist. If it
	 * is set it is the index to the location where it 
	 * resides in the array.
	 * 
	 */
	private Integer wildcard = null;

	/**
	 * This contains the value for each index of the
	 * multi-dimensional array that is being descibed.
	 * A value of -1 is being used to represent a wildcard
	 * for retrieval of looping operations upon this 
	 * dimension of the array.
	 * 
	 */
	private Integer[] indexes = null;
		
	/**
	 * Constructor
	 * @param dim - The number of dimensions for the array
	 * @param value - the index value
	 */
//	public ArrayIndex(int dim) throws IllegalArgumentException {
//		if (dim > 0 ) {
//			indexes = new Integer [dim];
//		}
//		else
//			throw new IllegalArgumentException("The dimension must be a positive integer value.");
//	}

	/**
	 * Constructor
	 * @param dim - The number of dimensions for the array
	 * @param value - the index value
	 */
	public ArrayIndex(Integer [] ndxs) throws IllegalArgumentException {
		if (ndxs.length > 0 ) {
			indexes = ndxs;
		}
		else
			throw new IllegalArgumentException("The dimension must be a positive integer value.");
	}
	public void setIndex(int index, int value) throws IllegalArgumentException {
		if (index < indexes.length) {
			if (value >= 0)
				indexes[index] = value;
			else if (value == WILDCARD_VALUE && 
					(wildcard == null || index == wildcard)) {
				indexes[index] = value;
				wildcard = index;
			}
			else 
				throw new IllegalArgumentException("The value for an index into an array must be 0 or greater.");
		}
//		else
//			throw new IllegalArgumentException("The index is outside of the " 
//					+ dimensions + " dimensions of the indexes.");
	}

	/**
	 * Sets the index location within the indexes where the 
	 * wildcard index can be found.
	 * @param index
	 */
	public void setWildcard(int index) {
		this.wildcard = index;
	}
	public int length() {
		if (indexes != null) 
			return indexes.length;
		return 0;
	}

	public int get(int index) throws IllegalArgumentException {
		if (indexes != null && index < indexes.length)
			return indexes[index];
		else
			throw new IllegalArgumentException("The value for an index into an array must be 0 or greater.");
	}
	
	public Integer getWildcard() {
		return wildcard;
	}
	
	public Integer [] getIndexes() {
		return indexes;
	}
	
	public boolean hasWildcard() {
		if (wildcard != null)
			return true;
		return false;
	}
	@Override
	public String toString() {
		String result = "";
//		if (dimensions >= 0) {
			for (int i = 0; i < indexes.length; i++) {
				if (indexes[i] != null)
					result += "[" + indexes[i] + "]";
				else
					result += "[-]";
			}
//		}
		
		return result;
	}
	
	@Override
	public Object clone()  throws CloneNotSupportedException {
		ArrayIndex retval = (ArrayIndex)super.clone();
		if (retval != null) {
//			if (this.dimensions != null) 
//				retval.dimensions = this.dimensions;
			// We only need a shallow copy of the indexes
			if (this.indexes != null)
				retval.indexes = this.indexes;
		}	
		
		return retval;
		
	}
	
	@Override
	public String display() {
		return toString();
	}
}
