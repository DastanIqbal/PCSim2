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

public class ProvisioningData {
	protected String policyFileName = null;
	protected String provFileName = null;
	protected String testCaseName = null;
		
	public ProvisioningData(String tc, String policy, String prov) {
		this.testCaseName = tc;
		this.policyFileName = policy;
		this.provFileName = prov;
	}

	public String getPolicyFileName() {
		return policyFileName;
	}

	public void setPolicyFileName(String policyFileName) {
		this.policyFileName = policyFileName;
	}

	public String getProvFileName() {
		return provFileName;
	}

	public void setProvFileName(String provFileName) {
		this.provFileName = provFileName;
	}

	public String getTestCaseName() {
		return testCaseName;
	}

	public void setTestCaseName(String testCaseName) {
		this.testCaseName = testCaseName;
	}
	
//	public String getGeneratedName() {
//		return this.generatedName;
//	}
//	public boolean isGenerated() {
//		if (generatedName != null) {
//			return true;
//		}
//		return false;
//	}
//	
//	public void setGeneratedName(String fileName) {
//		this.generatedName = fileName;
//	}
	
}
