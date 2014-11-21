/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.sim;

import java.util.LinkedList;

public class RegistrarData {

	// For the platform aor is the same as pui
	protected String aor = null;
	protected String phoneNumber = null;
	protected String contact = null;
	protected String instanceID = null;
	protected String publicGruu = null;
	protected LinkedList<String> tempGruus = null;
	protected String callID = null;
	protected int tempGruuCounter = 0;
	protected String tempGruuDomain = null;
	protected String gr = null;
	
	public RegistrarData(String urn, String phoneNum, String contact, 
			String aor, String callID) {
		this.phoneNumber = phoneNum;
		this.contact = contact;
		this.aor = aor;
		this.callID = callID;
		this.publicGruu = aor;
		this.gr = urn;
		assignTemporaryGRUU();
		
	}
	
	public String getAOR() {
		return this.aor;
	}
	
	public String getCallID() {
		return this.callID;
		
	}
	public String getContact() {
		return this.contact;
	}
	
	public String getInstanceID() {
		return this.instanceID;
	}
	
	public String getPhoneNumber() {
		return this.phoneNumber;
	}
	
//	public String getPublicGRUU() {
//		return aor;
//	}
	
	public String getTemporaryGRUU() {
		if (tempGruus != null)
			return tempGruus.getFirst();
		return null;
	}
	
	public String assignTemporaryGRUU() {
		if (aor != null) {
			if (tempGruus == null)
				this.tempGruus = new LinkedList<String>();
			String temp = ((Long)System.currentTimeMillis()).toString();
			if (tempGruuDomain == null) {
				int amp = aor.indexOf("@");
				if (amp != -1) 
					tempGruuDomain = aor.substring(amp+1);
				else 
					tempGruuDomain = aor;
					 
				String tempGruu = "tgruu." + temp + "@" + tempGruuDomain;
				tempGruus.addFirst(tempGruu);
				return tempGruu;
			}
		}
		return null;
	}
	
	public void setCallID(String callId) {
		if (callID == null || !callID.equals(callId)) {
			callID = callId;
			if (tempGruus != null)
				tempGruus.clear();
		}
	}
	
	public String getGrParameter() {
		return this.gr;
	}
	
	public String getPubGruuParameter() {
		return aor + ";gr=" + gr;
	}
	
	public String getTempGruuDomain() {
		return tempGruuDomain;
	}
	public String getTempGruuParameter() {
		return "sip:" + tempGruus.getFirst() + ";gr";
	}
}
