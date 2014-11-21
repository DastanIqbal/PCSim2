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
import java.util.List;

import org.apache.commons.collections15.Transformer;

import com.cablelabs.fsm.EndSessionState;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.State;

/**
 * A Transformer to set the color of a State based on its function in a FSM.
 * Designed for use with the JUNG framework.
 *
 * @author rvail
 *
 */
public class StatePainter implements Transformer<State, Paint> {

    /**
     * All the fsms in the graph.
     */
   List<FSM> fsms;

   /**
    * The default color for a state
    */
   private static final Color DEFAULT_C = Color.GRAY;

   /**
    * The default color for a start state
    */
   private static final Color START_C = Color.GREEN;

   /**
    * The default color for an end state
    */
   private static final Color END_C = Color.RED;

    public StatePainter(List<FSM> fsmLst) {
        fsms = fsmLst;
    }

    @Override
    public Paint transform(State st) {

        if (st != null) {
            // End states.
            if (st.getName().equals("END") || st instanceof EndSessionState) return END_C;

            // Start states
            for (FSM fsm : fsms) {
                if (st.getOwner().equals(fsm) && st.getName().equals(fsm.getInitialState())) {
                    return START_C;
                }
            }
        }

        return DEFAULT_C;
    }

}
