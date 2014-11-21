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

/**
 * A transformer that will give the same value for any input
 * Designed for use with the JUNG framework.
 *
 * Note: This was made because the ConstantTransformer<T> from apache commons was unable to
 *       take in both the IN and OUT types.
 *
 * @author rvail
 *
 */
public class ConstTransformer<IN,OUT> implements Transformer<IN, OUT> {

    OUT out;
    public ConstTransformer(OUT out) {
        this.out = out;
    }

    @Override
    public OUT transform(IN in) {
        return out;
    }

}
