/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.utility;

import java.util.StringTokenizer;
import com.cablelabs.log.*;

/**
 * This class is to allow values in the TLV that are array type. This class can 
 * accommodate up to a two dimensional array. The syntax for the value portion
 * of the TLV is fixed and follows the format below. NOTE: SP represents a space and
 * delimiters are String values
 * 
 * V = AttributeName SP number of rows SP number of columns SP number of elements per cell SP row delimiter SP column delimiter SP element delimiter SP data
 * 
 * An example might look like the following in ethereal or in String format
 * 
 * varBind 2 2 3 \r\n ][ , [oid1,type1,value1][oid2,type2,value2][\r\n[oid3,type3,value3][oid4,type4,value4][\r\n
 * 
 * @author Garey Hassler
 *
 */
public class UtilityArrayAttribute extends UtilityAttribute {

	private int rows = 0;
	private int columns = 0;
	private int numOfElements = 0;
	private String rowDelimiter = null;
	private String columnDelimiter = null;
	private String elementDelimiter = null;
	private String name = null;
	private static  final long serialVersionUID = 1;
	// table array is [row][column][element]
	// The table is zero based arrays
	private String [][][] table = null;
	
	private static LogAPI logger = LogAPI.getInstance(); // Logger.getLogger(UtilityStack.class);

	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Stack";
	
	public UtilityArrayAttribute(String name, String value) throws IllegalArgumentException {
		super("array", null);
		this.name = name;
		decode(value);
		
	}

	public UtilityArrayAttribute(String name, int rows, int columns, int numOfElements, 
			String rowDel, String colDel, String elementDel) throws IllegalArgumentException {
		super("array", null);
		this.name = name;
		this.rows = rows;
		this.columns = columns;
		this.numOfElements = numOfElements;
		this.rowDelimiter = rowDel;
		this.columnDelimiter = colDel;
		this.elementDelimiter = elementDel;
		this.table = new String [rows][columns][numOfElements];
	}

	public String getValue() {
		String value = "";
		if (table != null && rows > 0) {
			value = name + " " + rows + " " 
			+ columns + " " + numOfElements + " " 
			+ rowDelimiter + " " + columnDelimiter + " " 
			+ elementDelimiter + " ";
			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < columns; j++) {
					for (int k = 0; k < numOfElements; k++) {
						if (k > 0)
							value += elementDelimiter + table[i][j][k];
						else
							value += table[i][j][k];
					}
					value += columnDelimiter;
				}
				value += rowDelimiter;
			}
		}
		return value;
	}

	public int length() {
		String value = getValue();
		if (value != null)
			return value.length();
		return -1;
	}
	
	public void setValue(String value) throws IllegalArgumentException {
		decode(value);
	}

	public String encode() {
		String attr = super.getName() + " " + length() + " " + getValue();
		return attr;
	}

	public void addElement(int row, int column, int element, String value) throws IndexOutOfBoundsException {
		if (row >= 0 && row < rows) {
			if (column >= 0 && column < columns) {
				if (element >= 0 && element < numOfElements) {
					table[row][column][element] = value;
				}
				else {
					String errMsg = "Element[" + row + "][" + column + "][" 
					+ element + "] doesn't exist because element is invalid index.";
					throw new IndexOutOfBoundsException(errMsg);
				}
			}
			else {
				String errMsg = "Element[" + row + "][" + column + "][" 
				+ element + "] doesn't exist because column is invalid index.";
				throw new IndexOutOfBoundsException(errMsg);
			}
		}
		else {
			String errMsg = "Element[" + row + "][" + column + "][" 
			+ element + "] doesn't exist because row is invalid index.";
			throw new IndexOutOfBoundsException(errMsg);
		}

	}

//	public String getElement(int row, int column, int element) throws IndexOutOfBoundsException {
//		if (row >= 0 && row < rows) {
//			if (column >= 0 && column < columns) {
//				if (element >= 0 && element < numOfElements) {
//					return table[row][column][element];
//				}
//				else {
//					String errMsg = "Element[" + row + "][" + column + "][" 
//					+ element + "] doesn't exist because element is invalid index.";
//					throw new IndexOutOfBoundsException(errMsg);
//				}
//			}
//			else {
//				String errMsg = "Element[" + row + "][" + column + "][" 
//				+ element + "] doesn't exist because column is invalid index.";
//				throw new IndexOutOfBoundsException(errMsg);
//			}
//		}
//		else {
//			String errMsg = "Element[" + row + "][" + column + "][" 
//			+ element + "] doesn't exist because row is invalid index.";
//			throw new IndexOutOfBoundsException(errMsg);
//		}
//
//	}
//
//	public String [] getElements(int row, int column) throws IndexOutOfBoundsException {
//		if (row >= 0 && row < rows) {
//			if (column >= 0 && column < columns) {
//				String [] eles = new String [numOfElements];
//				for (int i = 0; i< numOfElements; i++) {
//					eles[i] = table[row][column][i];
//				}
//				return eles;
//			}
//			else {
//				String errMsg = "Element[" + row + "][" + column 
//				+ "] doesn't exist because column is invalid index.";
//				throw new IndexOutOfBoundsException(errMsg);
//			}
//		}
//		else {
//			String errMsg = "Element[" + row + "][" + column 
//			+ "] doesn't exist because row is invalid index.";
//			throw new IndexOutOfBoundsException(errMsg);
//		}
//
//	}

//	public String [][] getColumns(int row) throws IndexOutOfBoundsException {
//		if (row >= 0 && row < rows) {
//			String [][] aRow = new String [columns][numOfElements];
//			for (int j = 0; j < columns; j++) {
//				for (int i = 0; i< numOfElements; i++) {
//					aRow[j][i] = table[row][j][i];
//				}
//
//			}
//			return aRow;
//		}
//		else {
//			String errMsg = "Element[" + row 
//			+ "] doesn't exist because row is invalid index.";
//			throw new IndexOutOfBoundsException(errMsg);
//		}
//
//	}
//	
//	public String [][][] getTable() {
//		return table;
//	}
//	
	public void decode(String value) throws IllegalArgumentException {
		StringTokenizer tokens = new StringTokenizer(value, " ");
		if (tokens.countTokens() >= 7) {
			try {
				name = tokens.nextToken();
				rows = Integer.parseInt(tokens.nextToken());
				columns = Integer.parseInt(tokens.nextToken());
				numOfElements = Integer.parseInt(tokens.nextToken());
				rowDelimiter = tokens.nextToken();
				columnDelimiter = tokens.nextToken();
				elementDelimiter = tokens.nextToken();
				String data = "";
				if (rows > 0 && columns > 0  && numOfElements >0 ) {
					table = new String [rows][columns][numOfElements]; 
					logger.debug(PC2LogCategory.UTILITY, subCat,
							"UtilityArrayAttribute is attempting to decode a three dimensional array with " 
							+ rows + " rows " + columns + " columns and " + numOfElements + " elements."); 
				}
				
				else {
					String errMsg = "There is an invalid value in either the rows, " 
						+ "columns or numOfElements fields of the array data";
					throw new IllegalArgumentException(errMsg);
				}
				// Now we need to add the spaces back between all of the tokens as this information 
				// might be in the value portion.
				if (tokens.hasMoreTokens()) {
					data = tokens.nextToken();
				
					while(tokens.hasMoreTokens()) {
						data += " " + tokens.nextToken();
					}
				}
				int start = 0;
				String [] rowBuf = new String [rows];
				for (int i = 0; i < rows; i++) {
					int end = data.indexOf(rowDelimiter, start);
					if (end != -1 && end <= data.length()) {
						rowBuf[i] = data.substring(start,end); 
					}
					start = end+rowDelimiter.length();
					
				}
				
				if (start != data.length()){
					String errMsg = "The data portion of an array does not appear to be formatted " 
						+ "correctly because the parser could not locate the correct number of rows." 
						+ " Last substring parsed=[" + data + "] for delimiter=" + rowDelimiter;
					throw new IllegalArgumentException(errMsg);
				}
				
				if (columns >= 1) {
					String [][] colBuf = new String [rows][columns];
					for (int i = 0; i < rows; i++) {
						start = 0;
						for (int j = 0; j < columns; j ++) {
							int end = rowBuf[i].indexOf(columnDelimiter, start);
							if (end != -1 && end <= rowBuf[i].length()) {
								colBuf[i][j] = rowBuf[i].substring(start,end);
							}
							start = end + columnDelimiter.length();
						}
						if (start != rowBuf[i].length()) {
							String errMsg = "The data portion of an array does not appear to be formatted " 
								+ "correctly because the parser could not locate the correct number of columns." 
								+ " Last substring parsed=[" + rowBuf[i] 
								+ "] for delimiter=" + columnDelimiter;
							throw new IllegalArgumentException(errMsg);
						}
					}
					if (numOfElements >= 1) {
						for (int i = 0; i < rows; i++) {
							for (int j = 0; j < columns; j++) {
								start = 0;
								for (int k = 0; k < (numOfElements-1); k++) {
									int end = colBuf[i][j].indexOf(elementDelimiter,start);
									if (end != -1 && end <= colBuf[i][j].length()) {
										String ele = colBuf[i][j].substring(start,end);
										logger.debug(PC2LogCategory.UTILITY, subCat,
												"UtilityArrayAttribute adding[" + ele 
												+ "] to tableEntry[" + i + "][" + j + "][" + k + "].");
										table[i][j][k] = ele;
									}
									start = end + elementDelimiter.length();
								}
								if (start < colBuf[i][j].length()) {
									String ele = colBuf[i][j].substring(start,colBuf[i][j].length());
									logger.debug(PC2LogCategory.UTILITY, subCat,
											"UtilityArrayAttribute adding[" + ele 
												+ "] to tableEntry[" + i + "][" + j + "][" + (numOfElements-1) + "].");
									table[i][j][(numOfElements-1)] = ele;
								
								}
							}
						}
					}
				}
			}
			catch (NumberFormatException nfe) {
				String errMsg = "The number of rows, columns or elements field is not an integer.";
				throw new IllegalArgumentException(errMsg);
			}
			catch (ArrayIndexOutOfBoundsException oobe) {
				String errMsg = "Attempted to store a value in the table outside of its indexs.";
				throw new IllegalArgumentException(errMsg);
			}
			catch (Exception e) {
				String errMsg = "Error encountered while processing data portion of UtilitiyArrayAttribute.\n"
					+ e.getMessage() + "\n";
				throw new IllegalArgumentException(errMsg);
			}
		}
		else {
			String errMsg = "argument doesn't contain the minimum number of tokens (7)." 
				+ " It only contains " + tokens.countTokens() + " tokens.";
			throw new IllegalArgumentException(errMsg);
		}
	}

	public String getColumnDelimiter() {
		return columnDelimiter;
	}

	public int getColumns() {
		return columns;
	}

	public String getElementDelimiter() {
		return elementDelimiter;
	}

	public int getNumOfElements() {
		return numOfElements;
	}

	public String getName() {
		return name;
	}

	public String getRowDelimiter() {
		return rowDelimiter;
	}

	public int getRows() {
		return rows;
	}

	public Integer [] getDimensions() {
		Integer [] dimensions = new Integer [3];
		dimensions[0] = rows;
		dimensions[1] = columns;
		dimensions[2] = numOfElements;
		return dimensions;
	}
	
	public String getElement(Integer [] ndxs) {
		if (ndxs.length == 3) {
		   logger.info(PC2LogCategory.UTILITY,subCat,
				   "getElement[" + ndxs[0] + "][" + ndxs[1] + "][" + ndxs[2] + "]");
		   return table[ndxs[0]][ndxs[1]][ndxs[2]];
		}
		return null;
	}
	public Integer getMaximumIndex(int arrayIndex) {
		if (arrayIndex == 0)
			return rows;
		else if (arrayIndex == 1)
			return columns;
		else if (arrayIndex == 2)
			return numOfElements;
		return null;
	}
	public String toString() {
		return encode();
	}
}
