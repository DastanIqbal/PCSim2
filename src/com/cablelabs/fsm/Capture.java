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


public class Capture implements Action {

	private CaptureOp op = null;
	private Reference filter = null;
	private String name = null;
	private String file = null;
	
	public Capture(CaptureOp cp) {
		this.op = cp;
	}
	
	public Capture(CaptureOp cp, String name, String file) {
		this.op = cp;
		if (cp == CaptureOp.PARSE) {
			this.name = name;
			this.file = file;
		}
	}
	
	@Override
    public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.capture(this);
	}
	
	public Reference getFilter() {
		return this.filter;
	}
	
	public CaptureOp getOperation() {
		return this.op;
	}
	
	public boolean isStart() {
		if (op != null &&
				op.equals(CaptureOp.START))
			return true;
		
		return false;
	}
	
	public boolean isStop() {
		if (op != null &&
				op.equals(CaptureOp.STOP))
			return true;
		
		return false;
	}
	
	public boolean isParse() {
		if (op != null &&
				op.equals(CaptureOp.PARSE))
			return true;
		
		return false;
	}
	
	public void setFilter(Reference f) {
		this.filter = f;
	}
	
	@Override
    public String toString() {
		if (isStart())
			return "\tstart_capture\n";
		else if (isStop())
			return "\tstop_capture\n";
		else if (isParse()) {
			String result = "\tparse_capture name=" + name;
			if (file != null)
				result += " file=" + file;
			result += "\n";
			return result;	
		}
		return null;
	}

	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
    public Object clone() throws CloneNotSupportedException {
		Capture retval = (Capture)super.clone();
		retval.op = this.op;
		if (filter != null)
			retval.filter = (MsgRef)this.filter.clone();
		if (name != null)
			retval.name = new String(name);
		if (file != null)
			retval.file = new String(file);
		return retval;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
