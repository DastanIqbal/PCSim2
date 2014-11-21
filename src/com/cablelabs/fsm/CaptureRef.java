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


public class CaptureRef extends MsgRef {

	/**
	 * The message type of the captured message
	 *
	 * e.g. Discover, Offer, NAK for DHCP or
	 * 	eth, ip, for lower level protocols
	 */
	protected String msgType;

	/**
	 * The name of the field being referred to
	 */
	protected String field;

	/**
	 * The name of the attribute of the field to obtain from the message
	 */
	protected CaptureAttributeType attrType = CaptureAttributeType.DEFAULT;

	/**
	 * The name of the database of message to obtain the reference information
	 * from.
	 */
	protected String dbName = null;

	/**
	 * Specifies that the value, which should be a string in octet form, should
	 * be converted to some other data type, e.g. a string.
	 */
	protected String converter = null;

	/**
	 * Specifies that the reference value being retrieve should have a value added
	 * to it.
	 */
	protected String add = null;

	/**
	 * Constructor
	 * @param type
	 * @param name
	 */
	public CaptureRef(String type, String name) {
		super(type);
		super.setMsgInstance(MsgQueue.FIRST);
		this.dbName = name;
	}

	/**
	 * Gets the msgType
	 * @return
	 */
	public String getMsgType() {
		return msgType;
	}

	/**
	 * Gets the field
	 * @return
	 */
	public String getField() {
		return field;
	}


	/**
	 * Gets the attribute type
	 * @return
	 */
	public CaptureAttributeType getAttributeType() {
		return this.attrType;
	}

	/**
	 * Sets the attribute type
	 * @param cat 
	 */
	public void setAttributeType(CaptureAttributeType cat) {
		this.attrType = cat;
	}

	/**
	 * Gets the name of the database to use
	 * @return
	 */
	public String getDBName() {
		return dbName;
	}

	/**
	 * Sets the name of the database to use
	 * @param name
	 */
	public void setDBName(String name) {
		this.dbName = name;
	}

	/**
	 * Sets the msgType
	 * @param mt
	 */
	public void setMsgType (String mt) {
		this.msgType = mt;
	}

	/**
	 * Sets the field
	 * @param field
	 */
	public void setField(String field) {
		this.field = field;
		// This doesn't seem applicable to a capture field
		// checkForMsgEventHeader(hdr);
	}


	/**
	 * Gets the converter field
	 * @return 
	 */
	public String getConverter() {
		return converter;
	}

	/**
	 * Sets the converter field
	 * @param converter
	 */
	public void setConverter(String converter) {
		this.converter = converter;
	}

	/**
	 * Gets the add field
	 * @return 
	 */
	public String getAdd() {
		return add;
	}

	/**
	 * Sets the add field
	 * @param add
	 */
	public void setAdd(String add) {
		this.add = add;
	}

	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
    public String toString() {
		String result = super.toString();
		result += msgType + "[" + super.msgInstance + "]";
		if (field != null)
			result += "." + field + "[" + super.hdrInstance + "]";

		if (attrType != null &&
				attrType != CaptureAttributeType.DEFAULT)
			result += "." + attrType.toString().toLowerCase();

		if (dbName != null)
				result += " in name=" + dbName;
		if (converter != null)
			result += " convertTo=" + converter;
		if (add != null)
			result += " add=" + add;

		return result;
	}

	/** This implements a deep copy of the class for replicating
	 * FSM information.
	 *
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */
	@Override
    public Object clone() throws CloneNotSupportedException {
		CaptureRef retval = (CaptureRef)super.clone();
		if (retval != null ) {
			if (this.msgType != null)
				retval.msgType = new String(this.msgType);
			if (this.field != null)
				retval.field = new String(this.field);
			if (this.attrType != null)
				retval.attrType = this.attrType;
			if (this.dbName != null)
				retval.dbName = new String(this.dbName);
			if (this.converter != null)
				retval.converter = new String(this.converter);
			if (this.add != null)
				retval.add = new String(this.add);
			if (this.add != null)
				retval.add = new String(this.add);
		}

		return retval;
	}

	@Override
    public String display() {
		String result = "";
		result += msgType;

		if (msgInstance != null)
			result += "[" + msgInstance + "]";
		if (field != null)
			result += "." + field + "[" + hdrInstance + "]";

		if (attrType != null && attrType != CaptureAttributeType.DEFAULT)
			result += "." + attrType.toString();

		if (dbName != null)
			result += " in name=" + dbName;

		return result;
	}
}
