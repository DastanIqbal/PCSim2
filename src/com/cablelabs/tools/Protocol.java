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

import java.util.LinkedList;
import java.util.ListIterator;

import com.cablelabs.fsm.CaptureAttributeType;

public class Protocol {
	/**
	 * <proto name="frame" showname="Frame 3 (139 bytes on wire, 139 bytes captured)" size="139" pos="0">
	 */
	
	protected String protocol = null;
	protected String name = null;
	protected String size = null;
	protected String showname = null;
	protected String pos = null;
	protected LinkedList<Field> fields = null; 
	protected boolean tunnelingProtocol = false;
	protected String tunnelName = null;
	
	protected LinkedList<Protocol> subProtocols = null;
	protected int subProtocolPosition = -1;
	
	public Protocol(String protocol, String name, String showname, String size, String pos) throws IllegalArgumentException {
		if (protocol == null)
			throw new IllegalArgumentException("The protocol parameter can not be null.");
		if (name == null)
			throw new IllegalArgumentException("The name parameter can not be null.");
		
		this.protocol = protocol;
		this.name = name;
		this.showname = showname;
		this.size = size;
		this.pos = pos;
	}
	
	protected boolean addField(Field f) {
		if (fields == null) {
			fields = new LinkedList<Field>();
		}
		
		if (f != null && fields != null) {
			fields.add(f);
			return true;
		}
		return false;
	}
	
	public String abbreviatedString() {
		String result = name + "\n";
		ListIterator<Field> iter = fields.listIterator();
		while (iter.hasNext()) {
			Field f = iter.next();
			result += name + f.abbreviatedString() + "\n";
			if (f.hasSubFields()) {
				ListIterator<Field> iter2 = f.subFields.listIterator();
				while (iter2.hasNext()) {
					Field sf = iter2.next();
					result += name + sf.abbreviatedString() + "\n";
				}
			}
		}
		return result;
	}
	
	public String getAttribute(CaptureAttributeType cat) {
		if (cat == CaptureAttributeType.DEFAULT) {
			if (name != null)
				return this.name;
			else if (showname != null)
				return this.showname;
		}
		else if (cat == CaptureAttributeType.SIZE)
			return this.size;
		else if (cat == CaptureAttributeType.SHOWNAME)
			return this.showname;
		else if (cat == CaptureAttributeType.SHOW)
			return this.name;
		else if (cat == CaptureAttributeType.POS)
			return this.pos;
		else {
			String key = cat.toString().toLowerCase();
			Field f = getField(key);
			if (f != null) 
				return f.getAttribute(CaptureAttributeType.DEFAULT);
		}
		return null;
	}
	
	public Field getField(String name) {
		if (name != null && fields != null) {
			ListIterator<Field> iter = fields.listIterator();
			while (iter.hasNext()) {
				Field f = iter.next();
				// String n = f.getName();
				if (f.name.equals(name))
					return f;
				else if (f.hasSubFields()) {
					ListIterator<Field> iter2 = f.subFields.listIterator();
					while (iter2.hasNext()) {
						Field sf = iter2.next();
						if (sf.name.equals(name)) {
							return sf;
						}
					}
				}
			}
		}
		return null;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTunnelName() {
		return tunnelName;
	}
	
	public ListIterator<Protocol> getSubProtocols() {
		if (subProtocols!= null)
			return this.subProtocols.listIterator();
		
		return null;
	}
	public Protocol getSubProtocol(String name) {
		ListIterator<Protocol> iter = subProtocols.listIterator();
		while (iter.hasNext()) {
			Protocol tmp = iter.next();
			if (tmp.getName().equals(name))
				return tmp;
		}
		return null;
	}
	
	public Protocol getSubProtocol(int i) {
		if (subProtocols != null)
			return this.subProtocols.get(i);
		
		return null;
	}
	
	protected void addSubProtocol(Protocol p) throws IllegalArgumentException {
		if (subProtocolPosition == -1) {
			if (fields != null)
				subProtocolPosition = fields.size();
			else 
				subProtocolPosition = 0;
			subProtocols = new LinkedList<Protocol>();
			subProtocols.add(p);
		}
		else if (fields == null &&
				subProtocols != null) {
			subProtocols.add(p);
		}
		else if (subProtocolPosition == fields.size() &&
				subProtocols != null) {
			subProtocols.add(p);
		}
		else {
			throw new IllegalArgumentException("The subProtocol attribute has already been set for this class.\n" 
					+ this);
		}
	}
	
	public boolean hasFields() {
		if (fields != null) 
			return true;
		
		return false;
	}
	
	public boolean hasSubProtocol() {
		if (subProtocolPosition != -1)
			return true;
		return false;
	}
	
	public boolean isTunnelingProtocol() {
		return tunnelingProtocol;
	}
	
	public void setTunnelingName(String name) {
		tunnelName = name;
		tunnelingProtocol = true;
	}
	
	@Override
	public String toString() {
		String result = "        <proto";
		if (name != null && name.equals(protocol))
			result += " name=\"" + name + "\"";
		else
			result += " name=\"" + protocol + "." + name + "\"";
		
		if (pos != null)
			result += " pos=\"" + pos + "\"";
		
		if (showname != null)
			result += " showname=\"" + showname + "\"";

		if (size != null)
			result += " size=\"" + size + "\"";
		
		
		if (fields != null) {
			ListIterator<Field> iter = fields.listIterator();
			int position = 0;
			// First see if there is a subProtocol and if
			// so is it the first position.
			if (subProtocolPosition != -1 &&
					subProtocolPosition == position)  {
				ListIterator<Protocol> iter2 = subProtocols.listIterator();
				while (iter2.hasNext()) {
					Protocol subProtocol = iter2.next();
					result += "\n" + subProtocol.toString();
				}
			}
			while (iter.hasNext()) {
				result += "\n" + iter.next();
				position++;
				if (subProtocolPosition != -1 &&
						subProtocolPosition == position)  {
					ListIterator<Protocol> iter2 = subProtocols.listIterator();
					while (iter2.hasNext()) {
						Protocol subProtocol = iter2.next();
						result += "\n" + subProtocol.toString();
					}
				}
			}
			result += "\n        </proto>";
		}
		else
			result += "/>";
		return result;
	}
	
	/**
	 * This method attempts to reduce the number of layers of fields by collapsing them into something more managable as 
	 * Wireshark has started nesting the layers of fields with no meaningful difference. As an example the scripting changes from
	 * 
	 * 		Trap.data.snmpV2_trap.variable_bindings.1-3-6-1-2-1-1-3-0 
	 * to
	 * 
	 * 		Trap.1-3-6-1-2-1-1-3-0
	 */
	protected void collapseSNMPFields() {
		if (hasFields()) {
			LinkedList<Field> newList = new LinkedList<Field>();
			ListIterator<Field> iter = fields.listIterator();
			while (iter.hasNext()) {
				Field f = iter.next();
				if (f.hasSubFields() && f.getName().equals("data")) {
					recursiveCollapse(newList, f);
					f.clearSubFields();
				}
				else 
					newList.add(f);
			}
			fields = newList;
		}

	}
	
	private void recursiveCollapse(LinkedList<Field> list, Field f) {
		int size = f.getSubFieldSize();

		for (int i = 0; i<size; i++) {
			if (f.hasSubFields()) {
				Field sf = f.getSubField(i);

				if (sf != null && !(sf.getName().substring(0,1).matches("[0-9]"))) {
					recursiveCollapse(list, sf);
					sf.clearSubFields();

				}
				sf.setPrefix(null);
				
				list.add(sf);
			}
		}
	}
}
