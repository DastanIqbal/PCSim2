/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.gui.scripts.visualizer;

import org.apache.commons.collections15.Transformer;

import com.cablelabs.fsm.State;

/**
 * Creates a String label from a State.
 *
 * @author rvail
 *
 */
class StateStringer implements Transformer<State,String> {

    @Override
    public String transform(State input) {
        if (input == null) return "null";

        return input.getName();
    }
}