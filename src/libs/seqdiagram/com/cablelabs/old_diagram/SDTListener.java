package com.cablelabs.old_diagram;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;

import tools.tracesviewer.AlertFrame;
import tools.tracesviewer.ScriptFrame;

public class SDTListener {


	//	public TracesSessions sessions = null;
//	public TracesSessionsList sessionList = null;
//	public TracesSessionsDisplayer tracesSessionsDisplayer;
//	protected JMenuItem animationMenuItem;
	public boolean ANIMATION_STARTED;
public SDTCanvas canvas = null;
	//public AboutFrame aboutFrame;
	//public HelpBox helpBox;
	public ScriptFrame scriptFrame;
//	protected TracesAnimationThread animationThread = null;

	/** Creates new ListenerTraceViewer */
	public SDTListener() {
//		this.canvas = canvas;
//		this.sessionList = sess;
//		this.sessions = sess.getTracesSessions();;
//		this.animationMenuItem = animationMenuItem;
//		this.animationThread = animationThread;

		ANIMATION_STARTED = false;
//		aboutFrame = new AboutFrame();
//		tracesSessionsDisplayer = new TracesSessionsDisplayer();
//		helpBox = new HelpBox();
		scriptFrame = new ScriptFrame();
	}


	public void animationActionPerformed(ActionEvent evt) {
//		if (canvas.arrows.size() == 0) {
//			new AlertFrame(
//				"Please hit Refresh, first!",
//				JOptionPane.ERROR_MESSAGE);
//		}
//		else if (ANIMATION_STARTED) {
//
//			animationMenuItem.setBackground(Color.lightGray);
//			animationThread.stop();
//			ANIMATION_STARTED = false;
//		}
//		else {
//
//			animationThread.start();
//
//			animationMenuItem.setBackground(Color.green);
//			ANIMATION_STARTED = true;
//		}
	}

	public void debugActionPerformed(ActionEvent evt) {
//		TracesMessage debug = canvas.debugTracesMessage;
		//logger.debug(LogCategory.APPLICATION, "******************BEGIN******************************");
		//logger.debug(LogCategory.APPLICATION, debug.beforeDebug);
		//logger.debug(LogCategory.APPLICATION, debug.afterDebug);
		//logger.debug(LogCategory.APPLICATION, "******************END********************************");
//		if (debug == null)
//			return;
//
//		if (debug.getBeforeDebug() != null
//			&& debug.getBeforeDebug() != null
//			&& !debug.getBeforeDebug().trim().equals("")
//			&& !debug.getAfterDebug().trim().equals("")) {
//			DebugWindow debugWindow =
//				new DebugWindow(
//					debug.getBeforeDebug(),
//					debug.getAfterDebug(),
//					debug.getDebugLine());

//			debugWindow.show();
//		}
	}

//	public void helpMenuMouseEvent(MouseEvent evt) {
//		helpBox.show();
//	}

//	public void aboutMenuMouseEvent(MouseEvent evt) {
//		aboutFrame.animationThread.start();
//		aboutFrame.show();
//	}

	public void displayAllSessionsMouseEvent(MouseEvent evt) {
//		tracesSessionsDisplayer.show(sessions);
	}



	public void refreshActionPerformed(MouseEvent evt) {
		if (ANIMATION_STARTED)
			new AlertFrame(
				"You must stop the animation before refreshing the traces!",
				JOptionPane.ERROR_MESSAGE);
		else {
//			TracesSessions tracesSessions =
//				tracesViewer.refreshTracesSessions();
//			tracesViewer.tracesSessionsList.setTracesSessions(tracesSessions);
//			tracesViewer.tracesSessionsList.updateTracesCanvas();

//			if (tracesSessionsDisplayer.isVisible())
//				tracesSessionsDisplayer.show(sessions);
		}

	}

	public void scriptActionPerformed(ActionEvent evt) {

	}

	public void tracesSessionsListStateChanged(ItemEvent e) {
//		if (ANIMATION_STARTED) {
//			animationMenuItem.setBackground(Color.lightGray);
//			animationThread.stop();
//			ANIMATION_STARTED = false;
//		}
//		sessionList.updateTracesCanvas(e);
	}
}
