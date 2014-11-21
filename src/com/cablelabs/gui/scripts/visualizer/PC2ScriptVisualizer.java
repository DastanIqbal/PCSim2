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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Collection;
import java.util.Enumeration;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.renderers.Renderer;

import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.State;
import com.cablelabs.fsm.Transition;
import com.cablelabs.gui.scripts.PC2ScriptVisualizerI;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.parser.TSDocument;

/**
 * A Container to hold a parsed test script and a JFrame to display a
 * visualization of it.
 *
 * @author rvail
 *
 */
public class PC2ScriptVisualizer implements PC2ScriptVisualizerI {

    /**
     * The basic logger if we have an issue with the parser
     */
    private static LogAPI logger = LogAPI.getInstance();

    /**
     * The subcategory to use when logging
     *
     */
    private static final String subCat = "ScriptVisualizer";

    /**
     * The document to visualize
     */
    private TSDocument doc = null;

    /**
     * The JFrame to load the TSDocument graph into.
     */
    private JFrame jf = null;
    
    /**
     * Flag indicating if the app should quit upon close
     */
    private boolean shouldExitOnClose;

    public PC2ScriptVisualizer() {
        
    }
    
    public PC2ScriptVisualizer(TSDocument doc) {
        if (doc == null) {
            logger.error(PC2LogCategory.UI, subCat, "Attempting to create a visualizer for a null TSDocument.");
            throw new IllegalArgumentException("TSDocument is null");
        }

        this.doc = doc;
    }

    /**
     * Adds a FSM into the passed in graph.
     *
     * @param fsm the FSM to load
     * @param g the graph to load into
     */
    private static void addFSMToGraph(FSM fsm, Graph<State,Transition> g) {
        Enumeration<State> states = fsm.getStates();
        fsm.getInitialState();

        while (states.hasMoreElements()) {
            State st = states.nextElement();
            g.addVertex(st);

            Enumeration<Transition> transitions = st.getTransitions();
            while (transitions.hasMoreElements()) {
                Transition t = transitions.nextElement();
                String to = t.getTo();
                    g.addEdge(t, st, fsm.getState(to));
            }
        }

        // TODO ish: Detect if each component in the graph has an initial state, if it
        // doesn't then it is just dead (ex: any unused reboots from the RebootCapture template)
        // if it is dead remove or don't add any transitions to end so the layout manager can
        // push those vertices away from the main fsm.

        // This code looks for states which connect to END that don't have a path to the initial state, and is a
        // quick implementation of the todo above. This does not take EndSession states into account.

        State end = fsm.getState("END");

        State initial = fsm.getState(fsm.getInitialState());
        DijkstraDistance<State,Transition> dd = new DijkstraDistance<State,Transition>(g);
        for (State st : g.getPredecessors(end)) {
            Number dist = dd.getDistance(initial, st);
            if (dist == null) {
                removeEndTrans(st, g);
                dd = null;
                dd = new DijkstraDistance<State,Transition>(g);
            }
        }

    }

    /**
     * Removes any transisions to the END state from the passed in state in the graph
     *
     * @param st the state to remove transtions to end for
     * @param g the graph st is a member of
     */
    private static void removeEndTrans(State st, Graph<State,Transition> g) {
        Enumeration<Transition> transitions = st.getTransitions();
        while (transitions.hasMoreElements()) {
            Transition t = transitions.nextElement();
            if (t.getTo().equals("END")) {
                g.removeEdge(t);
            }
        }

        Collection<State> pred = g.getPredecessors(st);
        for (State s : pred) {
            if (s == st) continue;
            removeEndTrans(s, g);
        }

    }




    /**
     * @return the document being visualized.
     */
    @Override
    public TSDocument getDocument() {
        return doc;
    }


    /**
     * @param doc the document to be visualized.
     */
    @Override
    public void setDocument(TSDocument doc) {
        this.doc = doc;
        jf = null; // unload the previous documents JFrame
    }
     
    /**
     * @return the state of shouldExitOnClose
     */
    @Override
    public boolean isShouldExitOnClose() {
        return shouldExitOnClose;
    }
  
    /**
     * @param shouldExitOnClose
     */
    @Override
    public void setShouldExitOnClose(boolean shouldExitOnClose) {
        this.shouldExitOnClose = shouldExitOnClose;
    }

    /**
     * Loads the current TSDocument (doc) into a JFrame and packs it.
     * Does not show the JFrame.
     *
     * If the doc has already been loaded then it does not reload.
     */
    public void load() {
        if (jf != null) return; // already loaded

        if (doc == null) {
            logger.error(PC2LogCategory.UI, subCat, "Attempting to load PC2ScriptVisualizer but the TSDocument is null");
            return;
        }

        // Create the graph from the FSMs in doc
        Graph<State,Transition> g = new DirectedSparseMultigraph<State,Transition>();

        for (FSM fsm : doc.getFsms()) {
            addFSMToGraph(fsm, g);
        }

        // Create a Viewer for the graph
        VisualizationViewer<State, Transition> vv = new VisualizationViewer<State, Transition>(new KKLayout<State, Transition>(g));

        vv.getRenderContext().setVertexFillPaintTransformer(new StatePainter(doc.getFsms()));

        // Add vertex labels
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.AUTO);
        vv.getRenderContext().setVertexLabelTransformer(new StateStringer());
        vv.getRenderContext().setVertexFontTransformer(new BoldFontTransformer<State>());

        // Add edge labels
        vv.getRenderContext().getEdgeLabelRenderer().setRotateEdgeLabels(true);
        vv.getRenderContext().setEdgeLabelTransformer(new TransitionStringer());

        // Set the drawing transformers
        vv.getRenderContext().setEdgeDrawPaintTransformer(new TransitionPainter());
        vv.getRenderContext().setArrowDrawPaintTransformer(new TransitionPainter());
        vv.getRenderContext().setEdgeStrokeTransformer(new ConstTransformer<Transition, Stroke>(new BasicStroke(1.5f)));

        // Add scroll and zoom with mouse wheel
        final DefaultModalGraphMouse<State,Transition> graphMouse = new DefaultModalGraphMouse<State,Transition>();
        vv.setGraphMouse(graphMouse);

        // Set the systems default look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            logger.warn(PC2LogCategory.UI, subCat, "Unable to set system look and feel");
        }

        // Get the bounds of the working area on screen
        //(aka ScreenSize - UnuseableArea, where UnusableArea is space occupied by something like the start menu on windows)
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = ge.getMaximumWindowBounds();
        vv.setSize(bounds.width, bounds.height);
        vv.setBackground(Color.WHITE);

        // Load a JPanel with the viewer and the mode combo box
        JPanel jp = new JPanel();
        jp.setLayout(new BorderLayout());
        jp.add(vv, BorderLayout.CENTER);
        jp.setMinimumSize(new Dimension(600,400));
        jp.setSize(bounds.width, bounds.height);

        JComboBox modeBox = graphMouse.getModeComboBox();
        modeBox.addItemListener(graphMouse.getModeListener());
        graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        jp.add(modeBox, BorderLayout.SOUTH);

        // Create a JFrame to hold everything, pack it, and set its location
        jf = new JFrame();
        jf.getContentPane().add(jp);        
        jf.setTitle(doc.getName() + " - " + doc.getFileName());
        jf.pack();
        Dimension winSize = new Dimension((int)(bounds.width*0.95), (int)(bounds.height * 0.95));
        jf.setSize(winSize);
        jf.setLocation((bounds.width - winSize.width) / 2, (bounds.height - winSize.height) / 2);

    }

    /**
     * Makes the JFrame visible
     */
    @Override
    public void show() {
        load();
        if (shouldExitOnClose) {
            jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } else {
            jf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }
        jf.setVisible(true);
    }
    
    


}
