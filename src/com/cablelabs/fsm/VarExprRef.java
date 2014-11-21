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

import com.cablelabs.tools.RefLocator;

public class VarExprRef implements Reference {

	private LinkedList<Object> expr = new LinkedList<Object>();
	private int fsmUID = -1;
	private RefLocator refLocator = RefLocator.getInstance();
	
	/**
	 * Constructor.
	 */
	public VarExprRef(int fsmUID) {
		this.fsmUID = fsmUID;
	}

	protected void updateUID(int newUID, int origUID) {
		if (fsmUID == origUID) {
			fsmUID = origUID;
			ListIterator<Object> iter = expr.listIterator();
			while (iter.hasNext()) {
				Object o = iter.next();
				if (o instanceof MsgRef) {
					if (((MsgRef)o).getUID() == origUID)
						((MsgRef)o).setUID(newUID);
				}
			}
		}
	}
	/**
	 * Gets the literal expression.
	 * @return
	 */
	public String getExpr(int msgQueueIndex) {
		String result = "";
		ListIterator<Object> iter = expr.listIterator();
		while (iter.hasNext()) {
			Object o = iter.next();
			if (o instanceof Literal)
				result += ((Literal)o).getExpr();
			else if (o instanceof MsgRef) {
				MsgEvent event = MsgQueue.getInstance().get(msgQueueIndex);
				String value = refLocator.getReferenceInfo(fsmUID,(Reference)o, event);
				if (value != null) {
					MsgRef mr = (MsgRef)o;
					if (mr.isArithmeticRef()) {
						try {
							int val = Integer.parseInt(value);
							if (mr.isAddRef()) {
								val += mr.getArithmeticMod();
								value = ((Integer)val).toString();
							}
							else if (mr.isSubRef()) {
								val -= mr.getArithmeticMod();
								value = ((Integer)val).toString();
							}
						}
						catch (NumberFormatException e) {
							value = "";
						}
					}
					else if (mr.getFirstChar() != null && 
							(mr instanceof MsgRef || mr instanceof CaptureRef)) {

						try {
							int first = -1;
							String start = mr.getFirstChar();
							if (mr.isFirstAnOffsetFromLength()) {
								first = value.length() - Integer.parseInt(start);
							}
							else 
								first = Integer.parseInt(start);
							String last = mr.getLastChar();
							int end = -1;
							if (last != null) {
								if (mr.isLastAnOffsetFromLength()) {
									end = value.length() - Integer.parseInt(last);
								}
								else 
									end = Integer.parseInt(last);

								value = value.substring(first, end);
							}
							else 
								value = value.substring(first);
						}
						catch (NumberFormatException nfe) {

						}
										
						// This indicates that the user wants the length of the value instead of 
						// the actual value.
						if (mr.useLength()) {
							value = Integer.toString(result.length());
						}
						if (mr.getEscape()) {
							value = value.replaceAll("=", "%3d");
							value = value.replaceAll(";", "%3b");
							value = value.replaceAll("@", "%40");
							value = value.replaceAll("\"", "%22");
						}
						result += value;
					}
					else 
						result += value;
				}
			}
		}
		return result;
	}
	
	public void addLiteral(Literal value) {
		expr.add(value);
	}
	
	public void addMsgRef(MsgRef ref) {
		expr.add(ref);
	}
	
	public ListIterator<Object> getExprList() {
		return this.expr.listIterator();
	}
	
	@Override
	public String toString() {
		String result = "var_exp contains " + expr.size() + " elements.\n\t\t  Expr=";
		ListIterator<Object> iter = expr.listIterator();
		while (iter.hasNext()) {
			Object o = iter.next();
			if (o instanceof Literal)
				result += ((Literal)o).getExpr();
			else if (o instanceof MsgRef)
					result += ((MsgRef)o).toString();
		
		}
		if (fsmUID > 0)
			result += " fsmUID=" + fsmUID;
		
		result += " ";
		
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
		VarExprRef retval = (VarExprRef)super.clone();
		if (retval != null ) {
			if (this.expr != null) {
				retval.expr = new LinkedList<Object>();
				ListIterator<Object> iter = this.expr.listIterator();
				while (iter.hasNext()) {
					Object o = iter.next();
					if (o instanceof MsgRef) {
						MsgRef ref = (MsgRef)((MsgRef)o).clone();
						retval.expr.add(ref);
					}
					else if (o instanceof Literal) {
						Literal ref = (Literal)((Literal)o).clone();
						retval.expr.add(ref);
					}
				}
			}
		}	

		return retval;
		
		
	}

	/**
	 * This class should not be in a Verify, because it 
	 * is currently only applicable as a child to mod tag
	 */
	@Override
	public String display() {
		return null;
	}
}
