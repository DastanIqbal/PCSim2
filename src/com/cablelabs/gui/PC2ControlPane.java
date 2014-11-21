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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cablelabs.log.PC2LogCategory;

public class PC2ControlPane extends JPanel implements ActionListener, ItemListener, ChangeListener {

	static final long serialVersionUID = 1;
	protected static final int HEIGHT = 740;
	protected static final int WIDTH = 380;
//	private int normalWidth = 0;
//	private int normalHeight = 0;
//	private int minWidth = 0;
//	private int minHeight = 0;
	protected int x = 410;
	protected int y = 0;
	protected JPanel psPanel = null;
	protected JButton psSet = null;
	protected JButton psEdit = null;
	protected PC2ConfigPane dutPanel = null;
	protected PC2ConfigPane tsPanel = null;
	protected JTextPane reg = null;
	protected JPanel testPanel = null;
	protected final static String startLabel = "Start Tests";
	protected final static String stopLabel = "Stop Tests";
	protected JButton start = null;
	protected JButton stop = null;
	protected JTextField psFile = null;
	//protected File lastActiveDirectory = new File(".");
	protected boolean platformSet = false;
	protected boolean dutSet = false;
	protected boolean testScriptSet = false;
	protected boolean modified = false;
	protected PC2ImagePane logoPane = null;
	
	public PC2ControlPane( int width, int height) {
		super();
//		this.normalWidth = WIDTH;
//		this.normalHeight = HEIGHT;
//		this.minHeight = normalHeight;
		this.x = width - x;
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		//setSize(new Dimension( width/2-100, height-60));
		buildPSPanel();
		dutPanel = new PC2ConfigPane(PC2UI.DUT, ".xls", true);
		tsPanel = new PC2ConfigPane(PC2UI.TS, ".xml", false);
		Dimension max = new Dimension(WIDTH, 200);
		dutPanel.setMaximumSize(max);
		tsPanel.setMaximumSize(max);
		reg = new PC2RegPane();
		buildTestPanel();
		
		add(psPanel);
		add(dutPanel);
		add(tsPanel);
		//add(reg);
		logoPane = new PC2ImagePane();
		logoPane.setBounds(x, y+10, 
				PC2ImagePane.WIDTH, PC2ImagePane.HEIGHT);
//		logoPane.setVisible(false);
//		logoPane.setVisible(true);
		
		add(testPanel);
		add(logoPane);
		
		Dimension size = new Dimension(WIDTH,HEIGHT);
		setMaximumSize(size);
		setPreferredSize(size);
		setBounds(x,y,WIDTH,HEIGHT);
		
		
		
	}

	protected void prepareButtons(boolean flag) {
		psSet.setEnabled(flag);
		psEdit.setEnabled(flag);
		dutPanel.setAllButtons(flag);
		tsPanel.setAllButtons(flag);
	}
    
	private void buildPSPanel() {
		psPanel = new JPanel();
		GridBagLayout tbag = new GridBagLayout();
		GridBagConstraints tgbc = new GridBagConstraints();
		JPanel textPanel = new JPanel();
		tgbc.insets.bottom = 5;
		//JLabel psLabel = new JLabel("Platform Settings", JLabel.LEFT);
		JLabel psLabel = new JLabel("Platform Settings");
		textPanel.setLayout(tbag);
		
		
		tbag.setConstraints(psLabel,tgbc);
		textPanel.add(psLabel);
		
		psFile = new JTextField(24);
		tgbc.gridy = 1;
		tbag.setConstraints(psFile,tgbc);
		textPanel.add(psFile);

		psSet = new JButton ("Set");
		psSet.addActionListener(this);
		psSet.addItemListener(this);
		psSet.addChangeListener(this);
		
		psEdit = new JButton ("Edit");
		psEdit.setEnabled(false);
		psEdit.addActionListener(this);
		psEdit.addItemListener(this);
		psEdit.addChangeListener(this);
		
		//psFrame = new JFrame();
		
		GridBagLayout bbag = new GridBagLayout();
		GridBagConstraints bgbc = new GridBagConstraints();
		JPanel buttonPanel = new JPanel();
		bgbc.insets.bottom = 5;
		bgbc.fill = GridBagConstraints.HORIZONTAL;
		buttonPanel.setLayout(bbag);
		
		bgbc.gridy = 0;
		bbag.setConstraints(psSet, bgbc);
		buttonPanel.add(psSet);
		bgbc.gridy = 1;
		bbag.setConstraints(psEdit, bgbc);
		buttonPanel.add(psEdit);
		psPanel.setLayout(new BoxLayout(psPanel, BoxLayout.X_AXIS));
		psPanel.add(textPanel);
		psPanel.add(buttonPanel);
		Dimension max = new Dimension(WIDTH, 150);
		psPanel.setMaximumSize(max);
		
	}
	
	private void buildTestPanel() {
		start = new JButton(startLabel);
		stop = new JButton(stopLabel);
		
		enableTestButtons();
		
		start.addActionListener(this);
		start.addItemListener(this);
		start.addChangeListener(this);
		
		stop.addActionListener(this);
		stop.addItemListener(this);
		stop.addChangeListener(this);
		testPanel = new JPanel();
		JPanel bPanel = new JPanel();
		GridBagLayout bag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
	    Insets i = gbc.insets;
	    i.right = 40;
	    gbc.fill = GridBagConstraints.VERTICAL;
	    //bPanel.setLayout(new BoxLayout(bPanel, BoxLayout.X_AXIS));
		
	    bPanel.setLayout(bag);
		bag.setConstraints(start, gbc);
		bPanel.add(start);
		gbc.gridx =1;
		bag.setConstraints(start, gbc);
		bPanel.add(stop);
		testPanel.add(bPanel);
		Dimension max = new Dimension(WIDTH, 50);
		testPanel.setMaximumSize(max);
	}
	
	@Override
	public void actionPerformed(ActionEvent ev) {
		String action = ev.getActionCommand();
		if (action.equals("Set")) {
			// Create the dialog
			JFileChooser chooser = new JFileChooser( );
			// Limit the selections to a single file
			chooser.setMultiSelectionEnabled(false);
			
			String propExt = ".properties";
			chooser.addChoosableFileFilter(new PC2FileFilter(propExt, 
					"Platform Properties ( *" + propExt + " )"));
			String psExt = ".xls";
			chooser.addChoosableFileFilter(new PC2FileFilter(psExt, 
					"Platform Settings ( *" + psExt + " )"));
			History history = History.getInstance();
			chooser.setCurrentDirectory(history.lastPCFDirectory);
	        int option = chooser.showOpenDialog(PC2ControlPane.this);
	        if (option == JFileChooser.APPROVE_OPTION) {
	        	File sf = chooser.getSelectedFile( );
	        	if (sf != null) {
	        		psFile.setText(sf.getAbsolutePath());
	        		PC2UI.pc.setPlatformSettings(psFile.getText());
	        		PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
	        				"You chose " + sf);
	        		platformSet = true;
	        		enableTestButtons();
	        		modified = true;
	        		psEdit.setEnabled(true);
	        	}
	        	
	        }
	        else {
	        	PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
	        			"You canceled.");
	        }
	        history.lastPCFDirectory = chooser.getCurrentDirectory();
	        HistoryParser.writeHistoryFile();
	        
		}
		else if (action.equals("Edit")) {
			File ps = new File(psFile.getText());
			PC2UI.launchEditor(ps, true);
		}
        
		else if (action.equals(startLabel)) {
			if (dutSet && platformSet && testScriptSet) {
				// First disable the configuration and test script buttons
				prepareButtons(false);
				start.setEnabled(false);
				stop.setEnabled(true);
				LinkedList<File> duts = new LinkedList<File>();
				LinkedList<File> testScripts = new LinkedList<File>();
				DefaultListModel model = (DefaultListModel)dutPanel.list.getModel();
				for (int i = 0; i < model.getSize(); i++) {
					duts.add((File)model.getElementAt(i));
				}
				model = (DefaultListModel)tsPanel.list.getModel();
				for (int i = 0; i < model.getSize(); i++) {
					testScripts.add((File)model.getElementAt(i));
				}
				boolean dutPrimary = dutPanel.primary.isSelected();
				PC2UI.pc.startTests(duts, testScripts, dutPrimary);
				
			}
		}
		else if (action.equals(stopLabel)) {
			PC2UI.pc.stopTests();
			start.setEnabled(true);
			prepareButtons(true);
		}
	}

	/**
	 * Clears the control pane for closure.
	 *
	 */
	protected void clearControlPane() {
		psFile.setText("");
		dutPanel.clearAllAction(null);
		tsPanel.clearAllAction(null);
		dutSet = false;
		platformSet = false;
		testScriptSet = false;
		modified = false;
		enableTestButtons();
	}
	
	/**
	 * Updates the Start, Stop, Save, Save As and Close
	 * buttons based upon user interactions
	 *
	 */
	public void enableTestButtons() {
		PC2UI ui = PC2UI.getMainFrame();
		if (ui != null) {
			if (platformSet && dutSet && testScriptSet) {
				start.setEnabled(true);
				stop.setEnabled(false);
				ui.controlSet(true);
			}
			else {
				start.setEnabled(false);
				stop.setEnabled(false);
				ui.controlSet(false);
			}

			if (platformSet || dutSet || testScriptSet) {
				ui.menuBar.setSaveAndClose(true);
			}
			else
				ui.menuBar.setSaveAndClose(false);
		}	
	}
	
	@Override
	public void itemStateChanged(ItemEvent ev) {
		if (PC2UI.logger != null)
			PC2UI.logger.fatal(PC2LogCategory.UI, PC2UI.subCat,
					"StateChanged " + ev);
	}

	protected void minimizePane() {
//		Dimension size = new Dimension(WIDTH, HEIGHT);
//		setMaximumSize(size);
//		setMinimumSize(size);
//		setPreferredSize(size);
		setVisible(false);
	}
	
	
	public void primarySelectionChanged(JPanel p, boolean primaryActive) {
		PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
				"primarySelectionChanged " + primaryActive);
		if (p == dutPanel) {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"DUT primaryChanged");
			tsPanel.setPrimary(!primaryActive);
		}
		else if (p == tsPanel) {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"TS primaryChanged");
			dutPanel.setPrimary(!primaryActive);
		}
		
	}
	
	protected void restorePane() {
//		Dimension size = new Dimension(WIDTH, HEIGHT);
//		setMaximumSize(size);
//		setMinimumSize(size);
//		setPreferredSize(size);
//		setBounds(x,y,WIDTH, HEIGHT);
		setVisible(true);
	}
	
	@Override
	public void stateChanged(ChangeEvent ev) {
		try {
			 if (PC2UI.logger != null)
				 PC2UI.logger.trace(PC2LogCategory.UI, PC2UI.subCat,
						 "ControlPane ChangeEvent " + ev.getSource());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Clears the highlighted test pair in the configuration panels
	 *
	 */
	public void testComplete() {
		dutPanel.list.clearSelection();
		tsPanel.list.clearSelection();
	}

}
