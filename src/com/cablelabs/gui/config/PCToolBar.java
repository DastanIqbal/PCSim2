package com.cablelabs.gui.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;

public class PCToolBar extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JToolBar toolBar = null;
	//  private JButton button = null;
	private JButton newButton = null;
	private JButton openButton = null;
	private JButton saveButton = null;
	private JButton saveAsButton = null;
	private JButton closeButton = null;
	private ImageIcon newIcon = null;
	private ImageIcon openIcon = null;
	private ImageIcon saveIcon = null;
	private ImageIcon saveAsIcon = null;
	private ImageIcon closeIcon = null;

	public PCToolBar() {
	    toolBar = new JToolBar();
	    buildToolBar(toolBar);	 
	    toolBar.setFloatable(false);
        toolBar.setRollover(true);
	}

	protected void buildToolBar(JToolBar toolBar) {		
		newIcon = new ImageIcon(getClass().getResource("images/new.png"));
		openIcon = new ImageIcon(getClass().getResource("images/open.png"));
		saveIcon = new ImageIcon(getClass().getResource("images/save.png"));
		saveAsIcon = new ImageIcon(getClass().getResource("images/saveas.png"));
		closeIcon = new ImageIcon(getClass().getResource("images/exit.png"));

		newButton = new JButton(newIcon);
		newButton.setToolTipText("New");
		newButton.setActionCommand("New");

		openButton = new JButton(openIcon);
		openButton.setToolTipText("Open");
		openButton.setActionCommand("Open");

		saveButton = new JButton(saveIcon);
		saveButton.setToolTipText("Save");
		saveButton.setActionCommand("Save");

		saveAsButton = new JButton(saveAsIcon);
		saveAsButton.setToolTipText("Save As");
		saveAsButton.setActionCommand("Save As");

		closeButton = new JButton(closeIcon);
		closeButton.setToolTipText("Close");
		closeButton.setActionCommand("Close");

		toolBar.add(newButton);
		toolBar.add(openButton);
		toolBar.add(saveButton);
		toolBar.add(saveAsButton);
		toolBar.add(closeButton);

		newButton.addActionListener(this);
		openButton.addActionListener(this);
		saveButton.addActionListener(this);
		saveAsButton.addActionListener(this);
		closeButton.addActionListener(this);

		add(toolBar, BorderLayout.NORTH);
	}

	protected JComponent createButtonPane() {
		// DUT Button
		JButton DUTButton = new JButton("DUT");
		DUTButton.setActionCommand("DUT");
		// DUT Prov Button
		JButton DUTProvButton = new JButton("DUT Provisioning");
		DUTProvButton.setActionCommand("DUT Provisioning");
		// Platform Button
		JButton PlatButton = new JButton("Platform");
		PlatButton.setActionCommand("Platform");

		//Center the button in a panel with some space around it.
		JPanel pane = new JPanel(); //use default FlowLayout
		pane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		pane.add(DUTButton);
		pane.add(DUTProvButton);
		pane.add(PlatButton);

		DUTButton.addActionListener(this);
		DUTProvButton.addActionListener(this);
		PlatButton.addActionListener(this);

		return pane;
	}

	public void actionPerformed(ActionEvent ev) {

	}

}
