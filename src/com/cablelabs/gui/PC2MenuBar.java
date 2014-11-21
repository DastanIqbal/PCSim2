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


import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;

import com.cablelabs.fsm.EventConstants;
import com.cablelabs.log.PC2LogCategory;

public class PC2MenuBar extends JMenuBar implements ActionListener, ClipboardOwner{
	
	static final long serialVersionUID = 1;
	//String[] fileItems = new String[] { "New", "Open", "Save", "Exit" };
	private JMenu fileMenu = null;
	private JMenu editMenu = null;
	private JMenu toolsMenu = null;
	private JMenu helpMenu = null;
	private String[] fileItems = new String[] { "Open", "Close", "Save", "Save As", "Exit" };
	private String[] helpItems = new String[] { "About", "License"};
	private String[] editItems = new String[] { "Cut", "Copy", "Paste", "Find" } ;// { "Undo",  };
	private String[] toolsItems = new String[] { "Launch Sequence Diagram Tool" };
	protected JMenuItem [] histItems = new JMenuItem [History.MAX_NUM_HISTORY_FILES];
	private char[] fileShortcuts = { 'O','C','S','A','X' };
	private char[] helpShortcuts = { 'A','L'};
	private char[] editShortcuts = { 'X','C','V','F' }; //	{ 'Z', };
	private char[] histShortcuts = { '1', '2', '3', '4' };
	private char[] toolsShortcuts = { 'D' };
//	private File lastPCFDirectory = new File(".");
//	private File lastDCFDirectory = new File(".");
//	private File lastScriptsDirectory = new File(".");
//	private File lastBatchDirectory = new File(".");
//	private File lastFile = null;
	private final String ext = ".bat";
	protected JMenuItem open = null;
	protected JMenuItem close = null;
	protected JMenuItem save = null;
	protected JMenuItem saveAs = null;
	protected JMenuItem exit = null;
	protected JMenuItem cut = null;
	protected JMenuItem copy = null;
	protected JMenuItem paste = null;
	protected JMenuItem find = null;
	protected JMenuItem about = null;
	protected JMenuItem license = null;
	protected JMenuItem callIdDemo = null;
	protected JMenuItem voiceMail1Demo = null;
	protected JMenuItem voiceMail2Demo = null;
	protected JMenuItem voiceMail3Demo = null;
	protected JMenuItem seqDiagram = null;
	
	protected boolean batchFileOpen = false;
	protected JDialog findDialog = null;
	
	// The FSM to deliver an user event.
	private String fsm = null;
	
//	public static final int MAX_NUM_HISTORY_FILES = 4;
//	protected String [] histLabels = new String[MAX_NUM_HISTORY_FILES];
//	protected File [] histFiles = new File [MAX_NUM_HISTORY_FILES];
//	protected JMenuItem [] histItems = new JMenuItem [MAX_NUM_HISTORY_FILES];
//	protected int histCount = 0;
//	protected static final File HISTORY_FILE = new File("../config/.history");
	
	Clipboard clipboard = getToolkit().getSystemClipboard();
	Action cutAction = null;
	Action copyAction = null;
	Action pasteAction = null;
	//protected PC2TextPane textPane = null;
	private HistoryParser hp = new HistoryParser();
	private History history = null;
	
	public PC2MenuBar( )  {

		fileMenu = new JMenu("File");
		editMenu = new JMenu("Edit");
		toolsMenu = new JMenu("Tools");
		helpMenu = new JMenu("Help");
		//JMenu editMenu = new JMenu("Edit");
		//JMenu otherMenu = new JMenu("Other");
		//JMenu subMenu = new JMenu("SubMenu");
		//JMenu subMenu2 = new JMenu("SubMenu2");

		// Before we can build the file menu items, lets see if there is a history file in
		// the config directory
		try {
			history = hp.parse();
		}
		catch (Exception ex) {
			if (PC2UI.logger != null) {
				PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat, 
						"Encountered error while trying to parse the history file.");
			}
		}
		
//		int hf = 0;
//		if (HISTORY_FILE.exists() && HISTORY_FILE.canRead() && HISTORY_FILE.isFile()) {
//				try {
//					BufferedReader in = new BufferedReader(new FileReader(HISTORY_FILE));
//					String line = in.readLine();
//					while (line != null && 
//							hf < MAX_NUM_HISTORY_FILES) {
//						histFiles[hf] = new File(line);
//						if (histFiles[hf].exists() && 
//								histFiles[hf].canRead() && 
//								histFiles[hf].isFile()) {
//							histLabels[hf] = (hf+1) + ".  " + histFiles[hf].getName();
//						}
//						else {
//							histLabels[hf] =  (hf+1) + "*.  " + histFiles[hf].getName();
//						}
//						hf++;
//						line = in.readLine();
//					}
//					histCount = hf;
//				}
//				catch (FileNotFoundException fnf) {
//					if (PC2UI.logger != null) {
//						PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
//								"UI couldn't find the batch file at " 
//								+ HISTORY_FILE.getAbsolutePath());
//					}
//					fnf.printStackTrace();
//				}
//				catch (IOException io) {
//					if (PC2UI.logger != null) {
//						PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
//								"UI encountered an error while trying to read the batch file[" 
//								+ HISTORY_FILE.getAbsolutePath() + "].");
//					}
//					io.printStackTrace();
//				}
//		}
		
		buildFileMenu();
    
		for (int i=0; i < editItems.length; i++) {
			JMenuItem item = new JMenuItem(editItems[i], editShortcuts[i]);
			if (i == 0) {
				cut = item;
			}
			else if (i == 1) {
				copy = item;
			}
			else if (i == 2) {
				paste = item;
			}
			else if (i == 3) {
				find = item;
			}
			
			// Assemble the File menus with keyboard accelerators.
			item.setAccelerator(KeyStroke.getKeyStroke(editShortcuts[i],
	                Toolkit.getDefaultToolkit( ).getMenuShortcutKeyMask( ), false));
			item.addActionListener(this);
			editMenu.add(item);
		}
		editMenu.insertSeparator(3);
		
		findEditActions();
			
       	// Tools Menu
		for (int i=0; i < toolsItems.length; i++) {
		    JMenuItem item = new JMenuItem(toolsItems[i], toolsShortcuts[i]);
            if (i == 0) {
                seqDiagram = item;
            }
            
            item.addActionListener(this);
            
            toolsMenu.add(item);
		}
		
// 		OCAP
//		JMenuItem callid = new JMenuItem("Call ID Demo");
//		JMenuItem vm1 = new JMenuItem("Voice Mail Demo 1");
//		JMenuItem vm2 = new JMenuItem("Voice Mail Demo 2");
//		JMenuItem vm3 = new JMenuItem("Voice Mail Demo 3");
//		callid.addActionListener(this);
//		vm1.addActionListener(this);
//		vm2.addActionListener(this);
//		vm3.addActionListener(this);
//		toolsMenu.add(callid);
//		
//		toolsMenu.add(vm1);
//		toolsMenu.add(vm2);
//		toolsMenu.add(vm3);
//		toolsMenu.insertSeparator(1);
//		
		// Help Menu
		for (int i=0; i < helpItems.length; i++) {
			JMenuItem item = new JMenuItem(helpItems[i], helpShortcuts[i]);
			if (i == 0)
				about = item;
			else if (i == 1)
				license = item;
			
			item.addActionListener(this);
			helpMenu.add(item);
		}
		helpMenu.insertSeparator(1);

		// Finally, add all the menus to the menu bar.
		add(fileMenu);
		add(editMenu);
		add(toolsMenu);
		add(helpMenu);

		findDialog = new PC2FindDialog(PC2UI.getMainFrame());
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		// Create a file chooser that opens up as an Open dialog.
		String action = ev.getActionCommand();

		if (action.equals("Open")) {
			openAction(ev);
		}
		// Create a file chooser that opens up as a Save dialog.
		else if (action.equals("Close")){
			closeAction(ev);
		}
		else if (action.equals("Save")){
			saveAction(ev);
		}
		// Create a file chooser that allows you to pick a directory
		// rather than a file.
		else if (action.equals("Save As")) {
			saveAsAction(ev);
		}
		else if (action.equals("Exit")) {
			exitAction(ev);
		}
		else if (action.equals("Find")) {
			findAction(ev);
		}
		else if (action.equals("About")) {
			if (PC2UI.logger != null) {
				PC2UI.logger.info(PC2LogCategory.UI, PC2UI.subCat,
						"\n\tVendor: " + PC2UI.vendor +
						"\n\tVersion: " + PC2UI.version +
						"   build: " + PC2UI.build + "\n");
			}
		}
		else if (action.equals("License")) {
			PC2LicenseScreen splash = new PC2LicenseScreen();
			splash.showLicense();
			Thread t = new Thread(splash);
			t.setName("LicenseAgreement");
			t.setDaemon(true);
			t.start();
		}
		else if (action.equals("Call ID Demo")) {
			injectEvent(EventConstants.USER_EVENT_1);
		}
		else if (action.equals("Voice Mail Demo 1")) {
			injectEvent(EventConstants.USER_EVENT_2);		
		}
		
		else if (action.equals("Voice Mail Demo 2")) {
			injectEvent(EventConstants.USER_EVENT_3);		
		}
		else if (action.equals("Voice Mail Demo 3")) {
			injectEvent(EventConstants.USER_EVENT_4);
		}
		else if (action.equals("Launch Sequence Diagram Tool")) {
		    PC2UI.openLogInSDT(null);
		}
		else {
			for (int i =0; i< history.histCount; i++) {
				if (action.equals(history.histLabels[i])) {
					readBatchFile(history.histFiles[i]);
				}
			}
		}

	}

	protected void buildFileMenu() {
		
		boolean closeEnabled= false;
		boolean saveEnabled= false;
		boolean saveAsEnabled= false;
			
		if (close != null)
			closeEnabled = close.isEnabled();
		if (save != null)
			saveEnabled = save.isEnabled();
		if (saveAs != null)
			saveAsEnabled = saveAs.isEnabled();
		fileMenu.removeAll();
		
		// Add the first four elements to the menu item
		for (int i=0; i < fileItems.length-1; i++) {
			JMenuItem item = new JMenuItem(fileItems[i], fileShortcuts[i]);
			if (i == 0)
				open = item;
			else if (i == 1) {
				close = item;
				close.setEnabled(closeEnabled);
			}
			else if (i == 2) {
				save = item;
				save.setEnabled(saveEnabled);
			}
			else if (i == 3) {
				saveAs = item;
				saveAs.setEnabled(saveAsEnabled);
			}
			item.addActionListener(this);

			fileMenu.add(item);
		}
		// Next add any files stored in the history
		if (history.histCount > 0) {
			for (int i=0; i < history.histCount; i++) {
				JMenuItem item = new JMenuItem(history.histLabels[i], histShortcuts[i]);
				item.addActionListener(this);
				histItems[i] = item;
				fileMenu.add(item);
				item.setToolTipText(history.histFiles[i].getAbsolutePath());
			}
		}
		
		// Next build the exit menu item
		exit = new JMenuItem(fileItems[4], fileShortcuts[4]);
		exit.addActionListener(this);
		fileMenu.add(exit);
		
		fileMenu.insertSeparator(2);
		
		fileMenu.insertSeparator(5);
		
		if (history.histCount > 0)
			fileMenu.insertSeparator(5+(history.histCount+1));
		
	}
	protected void closeAction(ActionEvent ev) {
		PC2UI ui = PC2UI.getMainFrame();
		if (ui != null) {
			// First see if a test is currently running.
			if (ui.controlPane.stop.isEnabled()) {
				PC2UI.pc.stopTests();
				ui.controlPane.start.setEnabled(true);
				ui.controlPane.prepareButtons(true);
			}
			if (ui.controlPane.modified ||
			
					ui.controlPane.dutPanel.modified ||
					ui.controlPane.tsPanel.modified) {
				int value = ui.askToSaveBatch();
				if (value == JOptionPane.YES_OPTION) {
					saveAsAction(null);
				}
				ui.clearControlPane();
				setSaveAndClose(false);
				ui.controlPane.dutPanel.modified = false;
				ui.controlPane.tsPanel.modified = false;
				ui.controlPane.modified = false;
			}
			else {

				ui.clearControlPane();
				setSaveAndClose(false);
				ui.controlPane.dutPanel.modified = false;
				ui.controlPane.tsPanel.modified = false;
				ui.controlPane.modified = false;
			}
			history.lastBatchFile = null;
			ui.closeBatch();
		}
	}
	
	@Override
	public void lostOwnership(Clipboard c, Transferable t) {

	}

	public void doClick(ActionEvent event) {
		if (PC2UI.logger != null)
			PC2UI.logger.trace(PC2LogCategory.UI, PC2UI.subCat,
					"Menu item[" + event.getActionCommand() + " was clicked.");
	}

	protected void findAction(ActionEvent ev) {
		try {
			findDialog.setVisible(true);
		}
		catch (HeadlessException he) {
			PC2UI.logger.warn(PC2LogCategory.UI, "", 
					"Could not display Find Dialog box because system is running on headless system.");
		}
	}
	
	private void findEditActions() {
	    JTextPane tp = new JTextPane();
	    Action actions[] = tp.getActions();
	    Hashtable<Object, Action> commands = new Hashtable<Object, Action>();
	    for (int i = 0; i < actions.length; i++) {
	      Action action = actions[i];
	      commands.put(action.getValue(Action.NAME), action);
	    }
	    cutAction = commands.get(DefaultEditorKit.cutAction);
	    copyAction = commands.get(DefaultEditorKit.copyAction);
	    pasteAction = commands.get(DefaultEditorKit.pasteAction);
	   
	    // When you set the action, it changes the text. The
	    // method will update the text and mnemonic for the 
	    // operation.
	    cut.setAction(cutAction);
	    cut.setText(editItems[0]);
	    //cut.setMnemonic(editShortcuts[0]);
	    KeyStroke ctrlXKeyStroke = KeyStroke.getKeyStroke("control X");
	    cut.setAccelerator(ctrlXKeyStroke);
	    
	    copy.setAction(copyAction);
	    copy.setText(editItems[1]);
	    //copy.setMnemonic(editShortcuts[1]);
	    KeyStroke ctrlCKeyStroke = KeyStroke.getKeyStroke("control C");
	    copy.setAccelerator(ctrlCKeyStroke);
	    paste.setAction(pasteAction);
	    paste.setText(editItems[2]);
	   // paste.setMnemonic(editShortcuts[2]);
	    KeyStroke ctrlVKeyStroke = KeyStroke.getKeyStroke("control V");
	    paste.setAccelerator(ctrlVKeyStroke);
	  }
	
	protected void injectEvent(String event) {
		// Show a dialog asking the user to type in a String: 
		String initValue = fsm;
		if (initValue == null)
			initValue = "";
		fsm = JOptionPane.showInputDialog(	 
				"What is the name of the FSM to deliver the event?",
				initValue); 
		if (fsm != null) {
			PC2UI.pc.injectUserEvent(fsm, event);
		}
		else {
			PC2UI.logger.debug(PC2LogCategory.UI, "", 
					"Could not deliver user event(" + event + " to an FSM because value is null.");
		}
	}
	
	protected void openAction(ActionEvent ev) {
		JFileChooser chooser = new JFileChooser( );
		chooser.setMultiSelectionEnabled(false);
		chooser.addChoosableFileFilter(new PC2FileFilter(ext, 
				"PCSim2 Batch File ( *" + ext + " )"));
		chooser.setCurrentDirectory(history.lastBatchDirectory);
		int option = chooser.showOpenDialog(PC2MenuBar.this);
		if (option == JFileChooser.APPROVE_OPTION) {
			File sf = chooser.getSelectedFile( );
			if (sf != null) {
				PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
						"You chose " + sf);
				readBatchFile(sf);
				// readBatchFile does this - setSaveAndClose(true);
			}
		}
		else {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"You canceled.");
		}
		history.lastBatchDirectory = chooser.getCurrentDirectory();
		HistoryParser.writeHistoryFile();
	}
	
	protected void saveAction(ActionEvent ev) {
		JFileChooser chooser = new JFileChooser( );
		if (history.lastBatchFile != null)
			chooser.setSelectedFile(history.lastBatchFile);
		chooser.addChoosableFileFilter(new PC2FileFilter(ext, 
				"PCSim2 Batch File ( *" + ext + " )"));
		chooser.setCurrentDirectory(history.lastBatchDirectory);
		int option = chooser.showSaveDialog(PC2MenuBar.this);
		
		
		if (option == JFileChooser.APPROVE_OPTION) {
			File sf = chooser.getSelectedFile();
			if (sf != null ) {
				String name = sf.getName();
				if (name.endsWith(ext) ||
						name.contains("."))
					saveBatchFile(sf);
					
				else {
					name = sf.getAbsoluteFile() + ext;
					File newSF = new File(name);
					saveBatchFile(newSF);
				}
				history.lastBatchFile = sf;
				updateHistoryList(sf);
			}
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"You saved " + ((chooser.getSelectedFile( )!=null)?
					chooser.getSelectedFile( ).getName( ):"nothing"));
		}
		else {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"You canceled.");
		}
		history.lastBatchDirectory = chooser.getCurrentDirectory();
		HistoryParser.writeHistoryFile();
	}

	protected void saveAsAction(ActionEvent ev) {
		JFileChooser chooser = new JFileChooser( );
		chooser.addChoosableFileFilter(new PC2FileFilter(ext, 
				"PCSim2 Batch File ( *" + ext + " )"));
		chooser.setCurrentDirectory(history.lastBatchDirectory);
		int option = chooser.showSaveDialog(PC2MenuBar.this);
		
		if (option == JFileChooser.APPROVE_OPTION) {
			File sf = chooser.getSelectedFile();
			if (sf != null ) {
				String name = sf.getName();
				if (name.endsWith(ext))
					saveAsBatchFile(sf);
				else {
					name = sf.getAbsoluteFile() + ext;
					File newSF = new File(name);
					saveAsBatchFile(newSF);
				}
				history.lastBatchFile = sf;
				updateHistoryList(sf);
			}
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"You saved " + ((chooser.getSelectedFile( )!=null)?
					chooser.getSelectedFile( ).getName( ):"nothing"));
		}
		else {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"You canceled.");
		}
		history.lastBatchDirectory = chooser.getCurrentDirectory();
		HistoryParser.writeHistoryFile();
	}

	protected void exitAction(ActionEvent ev) {
		setVisible(false);
		PC2UI.close();
	}
	
//	protected void setEditActions(PC2TextPane textPane) {
//		// get the actions for the text panes
//		Action actions[] = textPane.getActions();
//		Action cutAction = PC2TextPane.findAction(actions, DefaultEditorKit.cutAction);
//	    Action copyAction = PC2TextPane.findAction(actions, DefaultEditorKit.copyAction);
//	    Action pasteAction = PC2TextPane.findAction(actions, DefaultEditorKit.pasteAction);
//	    cut.setAction(cutAction);
//	    copy.setAction(copyAction);
//	    paste.setAction(pasteAction);
//	}

	private void readBatchFile(File f) {
		PC2UI ui = PC2UI.getMainFrame();
		if (ui != null && f.exists() && f.canRead() && f.isFile()) {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"Reading batch file=" + f.getName());
			boolean loaded = true;
			try {
				BufferedReader in = new BufferedReader(new FileReader(f));
				String line = in.readLine();
				if (line != null && line.equals("PCSim2 2.0 batch file")) {
					//boolean success = false;
					DefaultListModel dutModel = ui.controlPane.dutPanel.model;
					dutModel.removeAllElements();
					DefaultListModel tsModel = ui.controlPane.tsPanel.model;
					tsModel.removeAllElements();
					line = in.readLine();
					while (line != null) {
						if (line.startsWith("ps=")) {
							String psFileName = line.substring(3,line.length());
							File psFile = new File(psFileName);
							if (psFile.exists() && psFile.canRead() && psFile.isFile()) {
								ui.controlPane.psFile.setText(psFileName);
								PC2UI.pc.setPlatformSettings(psFileName);
								ui.controlPane.platformSet = true;
							}
							else if (PC2UI.logger != null) {
								PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
										"Platform Settings file could not be loaded because"
										+ psFile + "  file exists=" + psFile.exists() + " can read=" 
										+ psFile.canRead() + " is a file=" + psFile.isFile() + ".");
							}
						}
						else if (line.startsWith("dutPrimary=")) {
							String selected = line.substring(11,line.length());
							boolean flag = true;
							if (selected.equals("true")) {
								ui.controlPane.primarySelectionChanged(
										ui.controlPane.dutPanel, flag);
							}
							else if (selected.equals("false")) {
								flag = false;
								ui.controlPane.primarySelectionChanged(
										ui.controlPane.dutPanel, flag);
							}	
							else if (PC2UI.logger != null) {
								PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
										"Failed to identify the primary control selection. Defaulting to DUT as primary.");
								ui.controlPane.primarySelectionChanged(
										ui.controlPane.dutPanel, flag);
							}
						}	
						else if (line.startsWith("dut=")) {
							String name = line.substring(4,line.length());
							File d = new File(name);
							if (d.exists() && d.canRead() && d.isFile()) {
								dutModel.addElement(d);
								PC2UI.pc.addDUTConfig(d);
								ui.controlPane.dutSet = true;
							}
							else if (PC2UI.logger != null) {
								PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
										"DUT Config file could not be loaded because"
									+ d + "  file exists=" + d.exists() + " can read=" 
									+ d.canRead() + " is a file=" + d.isFile() + ".");
							}
						}
						else if (line.startsWith("ts=")) {
							String name = line.substring(3,line.length());
							File t = new File(name);
							if (t.exists() && t.canRead() && t.isFile()) {
								tsModel.addElement(t);
								ui.controlPane.testScriptSet = true;
							}
							else if (PC2UI.logger != null) {
									PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
											"Test Script file could not be loaded because"
										+ t + "  file exists=" + t.exists() + " can read=" 
										+ t.canRead() + " is a file=" + t.isFile() + ".");
							}
						}
						else if (PC2UI.logger != null) {
									PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
											"PCSim2 could not be understand the line=[ "
										+ line 
										+ "  because it is not in an expected format.");
						}
						line = in.readLine();
					}
				}
				else if (PC2UI.logger != null) {
					PC2UI.logger.error(PC2LogCategory.UI, PC2UI.subCat,
							"The file(" 
							+ f.getAbsolutePath() 
							+ ") doesn't appear to be a PCSim2 batch file.");
					loaded = false;
				}
				in.close();
				if (ui.controlPane.dutSet) 
					ui.controlPane.dutPanel.activateListButtons(false);
				if (ui.controlPane.testScriptSet)
					ui.controlPane.tsPanel.activateListButtons(false);
				if (ui.controlPane.platformSet)
					ui.controlPane.psEdit.setEnabled(true);
				if (ui.controlPane.dutSet ||
						ui.controlPane.testScriptSet ||
						ui.controlPane.platformSet)
					if (loaded) {
						setSaveAndClose(true);
					}
				history.lastBatchFile = f;
				updateHistoryList(f);
			}
            catch (FileNotFoundException fnf) {
            	if (PC2UI.logger != null) {
            		PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
            				"UI couldn't find the batch file at " 
            				+ f.getAbsolutePath());
            	}
            	fnf.printStackTrace();
            }
            catch (IOException io) {
            	if (PC2UI.logger != null) {
            		PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
            				"UI encountered an error while trying to read the batch file[" 
            				+ f.getAbsolutePath() + "].");
            	}
            	io.printStackTrace();
            }
		}

	}
	
	private void saveBatchFile(File f) {
		PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
				"Saving batch file=" + f.getName());
		saveAsBatchFile(f);
	}

	private void saveAsBatchFile(File f) {
		if (f != null) {
			if (!f.exists() && f.isFile() && f.canWrite()) {
				PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
						"Overwriting existing batch file=" + f.getName());
			}
			else {
				//if (f.getName().contains("."))
				PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
						"Creating new batch file=" + f.getName());
				//else {
				//	String name = f.PC2UI.logger.debug("Creating new batch file=" + f.getName());
				//}
			}
			try {
				PC2UI ui = PC2UI.getMainFrame();
				if (ui != null) {
					BufferedWriter out = new BufferedWriter(new FileWriter(f, false));
					out.write("PCSim2 2.0 batch file\n");
					if (ui.controlPane.psFile.getText().length() > 0)
						out.write("ps=" + ui.controlPane.psFile.getText() + "\n");
					out.write("dutPrimary=" + ui.controlPane.dutPanel.primary.isSelected() + "\n");
					DefaultListModel dutModel = ui.controlPane.dutPanel.model;
					Enumeration<?> eles = dutModel.elements();
					while (eles.hasMoreElements())
						out.write("dut=" + eles.nextElement() + "\n");
					DefaultListModel tsModel = ui.controlPane.tsPanel.model;
					eles = tsModel.elements();
					while (eles.hasMoreElements())
						out.write("ts=" + eles.nextElement() + "\n");
					out.close();
					setSaveAndClose(true);
					ui.controlPane.dutPanel.modified = false;
					ui.controlPane.tsPanel.modified = false;
					ui.controlPane.modified = false;
				}
			} catch (IOException e) {
			}

		}
	}
	
	protected void setFindDialog(JDialog d) {
		this.findDialog = d;
	}
	
	protected void setSaveAndClose(boolean flag) {
		close.setEnabled(flag);
		save.setEnabled(flag);
		saveAs.setEnabled(flag);
	}
	
	protected void setPaste(boolean flag) {
		paste.setEnabled(flag);
	}
	
	protected void updateHistoryList(File f) {
		// First see if the item appears in the list already
		int index = -1;
		int lastPos = History.MAX_NUM_HISTORY_FILES;
		for (int i = 0; i < History.MAX_NUM_HISTORY_FILES && index == -1; i++) {
			if (history.histFiles[i] != null) 
				if (f.getName().contains(history.histFiles[i].getName()) && 
						f.getAbsolutePath().equals(history.histFiles[i].getAbsolutePath()))
					index = i;
		}
		
		// Test whether the file appears in the list is so, we only
		// need to move the items that appear up to the location of
		// the file
		if (index != -1) {
			if (index == 0) {
				// Since the most recent is at the top and included in the
				// list, simply return without doing any unnecessary work.
				return;
			}
			else {
				lastPos = index;
				for (int i =lastPos; i > 0; i--) {
					history.histLabels[i] = (i+1) + history.histLabels[i-1].substring(1);
					history.histFiles[i] = history.histFiles[i-1];
				}
				history.histFiles[0] = f;
				history.histLabels[0] = "1.  " + f.getName();
				// The count doesn't change as we are simply reordering the
				// list in this condition.
			}
		}
		else {
			// Since the file doesn't appear in the list, everything
			// is straight forward, move everything down one item,
			// and the new file to the top of the list, and then
			// rebuild the file menu items.
			for (int i =(History.MAX_NUM_HISTORY_FILES-1); i > 0; i--) {
				if (history.histLabels[i-1] != null)
					history.histLabels[i] = (i+1) + history.histLabels[i-1].substring(1);
				else
					history.histLabels[i] = null;
				history.histFiles[i] = history.histFiles[i-1];
			}
			history.histFiles[0] = f;
			history.histLabels[0] = "1.  " + f.getName();
			if (history.histCount < History.MAX_NUM_HISTORY_FILES)
				history.histCount++;
			else 
				history.histCount = History.MAX_NUM_HISTORY_FILES;
		}
		
		buildFileMenu();
		
		// Lastly, save the history information to the file
		HistoryParser.writeHistoryFile();
//		try {
//			BufferedWriter out = new BufferedWriter(new FileWriter(HISTORY_FILE, false));
//			for (int i=0; i< MAX_NUM_HISTORY_FILES; i++) {
//				if (histFiles[i] != null) {
//					out.write(histFiles[i].getAbsolutePath() + "\n"); 
//				}
//			}
//			out.close();
//		}
//		catch (IOException e) {
//			if (PC2UI.logger != null) {
//				PC2UI.logger.error(PC2LogCategory.UI, PC2UI.subCat, 
//						"PCSim2 encountered an error while trying to write the history file information.\n" 
//						+ e.getMessage());
//			}
//		}
		
	}
}
