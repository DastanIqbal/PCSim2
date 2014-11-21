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

/** 
 * This interface defines common operations
 * that must exist for every distributor.
 * 
 * @author ghassler
 *
 */
public interface Distributor {
	 
	/**
	 * This method should return all of the
	 * IP and port information for each of the
	 * stacks that the distributor created
	 * during a series of tests.
	 * @return
	 */
	public String getStackAddresses();
}
