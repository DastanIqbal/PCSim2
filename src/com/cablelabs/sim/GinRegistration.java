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

import com.cablelabs.fsm.Send;

/** 
 * This interface defines common operations
 * that must exist for every distributor.
 * 
 * @author ghassler
 *
 */
public interface GinRegistration {

	/**
	 * This method should retrieves contact header from a register message that supports
	 * the GIN registration process 
	 * @return
	 */
	public boolean getGinRegistrationInfo(Send s, String neLabel);
}
