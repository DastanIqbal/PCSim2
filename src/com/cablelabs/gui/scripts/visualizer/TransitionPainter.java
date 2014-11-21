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

import java.awt.Color;
import java.awt.Paint;

import org.apache.commons.collections15.Transformer;

import com.cablelabs.fsm.Transition;


/**
 * Chooses a color for a transition.
 * Designed for use with the JUNG framework.
 *
 * @author rvail
 *
 */
public class TransitionPainter implements Transformer<Transition, Paint> {

    @Override
    public Paint transform(Transition t) {
        // Transitions to an end state are red
        if (t.getTo().equals("END")) return Color.RED;

        return Color.BLACK;
    }

}
