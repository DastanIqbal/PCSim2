/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

public class PC2FindDialog extends JDialog implements ActionListener, PropertyChangeListener {

	static final long serialVersionUID = 1;
	protected JFrame owner = null;
	protected Container rootPane = null;
	protected JPanel buttonPanel = new JPanel();
	protected JPanel textPanel = new JPanel();
	protected JLabel findLabel = new JLabel("Find:", JLabel.LEFT);
	protected JTextField findText = new JTextField(24);
	protected final String FIND = "Find / Find Next";
//	protected final String FIND_NEXT = "Find Next";
	protected final String CLOSE = "Close";
	protected JButton findButton = new JButton(FIND);
//	protected JButton findNextButton = new JButton(FIND_NEXT);
	protected JButton closeButton = new JButton(CLOSE);
	protected boolean initialized = false;
	protected BorderLayout findLayout = new BorderLayout();
	protected PC2TextPane findPane = null;
	protected String lastPattern = null;

	public PC2FindDialog(JFrame owner) throws HeadlessException {
		super(owner, "Find", false, owner.getGraphicsConfiguration());
		this.owner = owner;
		setSize(350,100);
		rootPane = getContentPane();
		rootPane.setLayout(findLayout);
		findText.addActionListener(this);
		textPanel.add(findLabel, BorderLayout.WEST);
		textPanel.add(findText, BorderLayout.EAST);
		rootPane.add(textPanel, BorderLayout.CENTER);
		findButton.addActionListener(this);
		buttonPanel.add(findButton);
//		findNextButton.addActionListener(this);
//		buttonPanel.add(findNextButton);
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);
		rootPane.add(buttonPanel, BorderLayout.SOUTH);
		setLocationRelativeTo(owner);
		
	}
	
	@Override
	public void actionPerformed(ActionEvent ev) {
		String action = ev.getActionCommand();
		
		// action will be equal to the string in the findText field if it was the source.
		if (ev.getSource().equals(findText)) {
			action = FIND;
		}
		
		if (action.equals(FIND)) {
			String pattern = findText.getText();
			if (owner instanceof PC2UI && pattern != null && !(pattern.equals(""))) {
				PC2UI ui = (PC2UI)owner;
				JTabbedPane tp = ui.tabPane.pane;
				int tab = tp.getModel().getSelectedIndex();
				if (tab == 0) {
					if (findPane != null && ((findPane != ui.tabPane.consolePane) ||
							(lastPattern != null && !lastPattern.equals(pattern)))) {
						findPane.clearHighlight();
					}
					findPane = ui.tabPane.consolePane;
					findPane.highlight(pattern);
					lastPattern = pattern;
				}
				else if (tab == 3) {
					if (findPane != null && ((findPane != ui.tabPane.logTextPane)  ||
							(lastPattern != null && !lastPattern.equals(pattern)))) {
						findPane.clearHighlight();
					}
					findPane = ui.tabPane.logTextPane;
					findPane.highlight(pattern);
					lastPattern = pattern;
				}
			}
		}
//		else if (action.equals(FIND_NEXT)) {
//			if (findPane != null)
//				findPane.nextHighlight();
//		}
		else if (action.equals(CLOSE)) {
			if (findPane != null)
				findPane.clearHighlight();
			findPane = null;
			setVisible(false);
		}
			
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		
	}
	
//	public void show() {
//		if (!isVisible()) {
//			setVisible(true);
//		}
//	}
}
