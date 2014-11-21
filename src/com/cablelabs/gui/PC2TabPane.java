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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import com.cablelabs.log.PC2LogCategory;

public class PC2TabPane extends JPanel implements AncestorListener, 
						ActionListener, ItemListener, ChangeListener, MouseListener {

	static final long serialVersionUID = 1;
	// Static defined column positions for the table
	static final int STATUS_COLUMN = 0; 
	static final int LABEL_COLUMN = 1;
	static final int PUI_COLUMN = 2;
	
	protected JTabbedPane pane = null;
	private int normalWidth = 0;
	private int normalHeight = 0;
	private int maxWidth = 0;
	private int maxHeight = 0;
	private boolean viewNormal = true;
	
	private int controlPaneWidth = 0;

	private PC2UI owner = null;
	
	// Console Viewer Tab elements
	protected PC2TextPane consolePane = null;
	protected JScrollPane consoleScrollPane = null;
	protected PC2Console console = null;
	protected JPanel consolePanel = null;
	
	// Registrar Tab elements
	protected JPanel registrar = null;
	protected final String STATUS = "Status";
	protected final String LABEL = "Label";
	protected final String PUI = "Public User Id";
	protected String registrarColHdrs[] = new String[] {
			    STATUS, LABEL, PUI};
	protected JScrollPane registrarScrollPane = null;
	protected PC2Table regTable = null;
	protected DefaultTableColumnModel regColModel = null;
	protected DefaultTableModel regTableModel = null;
	
	// Test Results Tab elements
	protected JPanel results = null;
	protected PC2Table resultTable = null;
	protected final String RESULT = "Result";
	protected final String DUT = "DUT";
	protected final String TC = "Test Case";
	protected final String RUN = "Run";
	
	protected String resultColHdrs[] = new String[] {
			    RESULT, RUN, TC, DUT};
	protected JScrollPane resultScrollPane = null;
	protected DefaultTableColumnModel resultColModel = null;
	protected DefaultTableModel resultTableModel = null;
	
	
	// Log Viewer Tab elements
	/**
	 * The scroll pane for the log's text
	 */
	protected JScrollPane logScrollPane = null;
	
	/**
	 * The colorized text of the log
	 */
	protected PC2TextPane logTextPane = null;
	
	/**
	 * The container for colorizing and adapting the
	 * text to the correct style for display purposes
	 * within the text pane.
	 */
	protected PC2LogViewer logViewer = null;
	
	/**
	 * This is the root panel for this tab
	 */
	protected JPanel logPanel = null;
	
	/**
	 * The Load button 
	 */
	protected JButton load = null;
	
	/**
	 * The sequence diagram tool button
	 */
	protected JButton sdtBtn = null;
	
	/**
	 * This component holds the name of the file with
	 * its path that is displayed in the text pane.
	 */
	protected JTextField lvFile = null;
	
	/**
	 * This panel holds the file selection components
	 * of the tab, such as the label, file name and
	 * load button
	 */
	protected JPanel logFilePanel = null;
	/**
	 * This is the expected extension of a log
	 * file. It is used as a filter for the open
	 * dialog box
	 */
	protected String logExt = ".log";
	
	/**
	 * This attribute maintains the last directory
	 * used to view a log from. By default it starts
	 * in the {ROOT_DIRECTORY of the platform}/logs
	 */
	protected File lastActiveDirectory = new File("../logs");
	
	/**
	 * This allows the system to know whether the user has
	 * the tab pane as the entire screen on only a portion
	 * of the screen.
	 */
	protected boolean tabFullScreen = false;
	
	private File loadedLog = null;
	
	public PC2TabPane(PC2UI ui, int width, int height) {
		this.owner = ui;
		this.controlPaneWidth = PC2ControlPane.WIDTH + 20;
		this.normalWidth = width - controlPaneWidth; //width/2;
		this.normalHeight = height-65;
		this.maxWidth = width-15;
		this.maxHeight = height-65;
		Dimension size = new Dimension(normalWidth, normalHeight);
		//Layout l = new BorderLayout();
		pane = new JTabbedPane();
		pane.addMouseListener(this);
		
		GridBagLayout bag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		pane.setPreferredSize(size);
//		pane.setBackground(Color.WHITE);
		
		if (width/2 > 150)
			pane.setMinimumSize(new Dimension(150,normalHeight));
		//pane.setMinimumSize(size);
		buildConsole(size);
		

		logPanel = new JPanel();
		
		logTextPane = new PC2TextPane(false, false);
		logScrollPane = new JScrollPane(logTextPane);
		logScrollPane.setAutoscrolls(true);
		
		logViewer = new PC2LogViewer(logScrollPane, logTextPane);
		buildLogViewer();
		
		buildRegistrarPanel(size);
		buildResultsPanel(size);
		
		//logs = new JTextPane();
		ImageIcon consoleImage = createImageIcon("images/comp1.gif");
		pane.addTab("Console", consoleImage, consoleScrollPane, "Console window");
		ImageIcon regImage = createImageIcon("images/connect.jpg");
		pane.addTab("Registrar", regImage, registrar, "Devices registering to PCSim2");
		ImageIcon resultsImage = createImageIcon("images/accept.gif");
		pane.addTab("View Results", resultsImage, results, "Results of the tests conducted");
		ImageIcon logImage = createImageIcon("images/log.jpg");
		pane.addTab("View Log", logImage, logPanel, "View a PCSim2 log file.");
				
		bag.setConstraints(pane, gbc);
		//pane.setLayout(new BorderLayout());
		
		add(pane, BorderLayout.CENTER);
		pane.addAncestorListener(this);
		// now test the mechanism
		//System.err.println( "Hello World" );
		UIManager.put("TabbedPane.selected", new Color(0,128,255));		// 10,121,245
		SwingUtilities.updateComponentTreeUI(pane);
	}
	

//	public OutputStream getConsoleStream() {
//		return (OutputStream)console;
//	}
	
	
	@Override
	public void actionPerformed(ActionEvent ev) {
		String action = ev.getActionCommand();
		if (action.equals("Load")) {
			// Create the dialog
			JFileChooser chooser = new JFileChooser();
			// Limit the selections to a single file
			chooser.setMultiSelectionEnabled(false);
			chooser.addChoosableFileFilter(new PC2FileFilter(logExt, 
					"Log File ( *" + logExt + " )"));
			chooser.setCurrentDirectory(lastActiveDirectory);
			int option = chooser.showOpenDialog(PC2TabPane.this);
			if (option == JFileChooser.APPROVE_OPTION) {
				File sf = chooser.getSelectedFile( );
				if (sf != null) {
					lvFile.setText(sf.getAbsolutePath());
					PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat, "You chose " + sf);
					loadLog(sf);
				}
				else {
					PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
							"You canceled.");
				}//Runtime.getRuntime().exec()
				lastActiveDirectory = chooser.getCurrentDirectory();
			}
		}
		else if (action.equals("Open in SDT")) {
		    PC2UI.openLogInSDT(loadedLog);
		}
	}

	protected void addRegistrarElement(String pui, String neLabel) {
		int [] result = findValue(regTableModel, pui, PUI);
		if (result[0] > -1 && result[1] > -1) {
			int labelCol = regTableModel.findColumn(LABEL);
			if (labelCol > -1) 
				regTableModel.setValueAt(neLabel,result[0],labelCol);
				
			if (PC2UI.logger != null) {
				PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"Updating column values label=" + neLabel + " pui=");
			}
		}
		else {
			regTableModel.addRow(new Object []{PC2RegistrarStatus.INACTIVE, neLabel, pui});
			if (PC2UI.logger != null) {
				PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
						"Adding new registrar information to table for element defined by\nlabel=" 
						+ neLabel + " pui=" + pui + " status=" + PC2RegistrarStatus.INACTIVE);
			}
		}
		if (PC2UI.logger != null) {
			String tableDump = "";
			int rows = regTableModel.getRowCount();
			int cols = regTableModel.getColumnCount();
			for (int i = 0; i < rows && rows != -1; i++) {
				for (int j = 0; j < cols && cols != -1; j++) {
					tableDump += "[" + regTableModel.getValueAt(i,j) + "] ";
				}
				tableDump += "\n";
			}
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
				"\n Table:\n" + tableDump);	
		}
	}
	// Add any special code for this component
	// Ancestor Listener support
	@Override
	public void ancestorMoved( AncestorEvent event ) {
	           // Add code here to handle a split pane divider movement
		//if (PC2UI.logger != null)
		//	PC2UI.logger.debug("Detected Divider moved." + event);
	}

	// Not used
	   @Override
	public void ancestorAdded( AncestorEvent event ) {
	}

	   @Override
	public void ancestorRemoved( AncestorEvent event ) {
	}

	private void buildConsole(Dimension size) {
		consolePanel = new JPanel();
 		Dimension paneSize = new Dimension(size.width-5, size.height-5);
		//consolePanel.setBackground(Color.WHITE);
		//consolePanel.setPreferredSize(paneSize);
		//consolePanel.setMinimumSize(paneSize);
		consolePane = new PC2TextPane(true, true);
		consolePane.init(true, false);
		consolePane.addMouseListener(consolePane);
		consoleScrollPane = new JScrollPane(consolePane);
		consoleScrollPane.setAutoscrolls(true);
		consoleScrollPane.setPreferredSize(paneSize);
		console = new PC2Console(consoleScrollPane, consolePane);
				
		PrintStream out = new PrintStream(console);
		//	redirect standard output stream to the OutputStream
	 	System.setOut( out );

		//	redirect standard error stream to the OutputStream
		System.setErr( out );
		//consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
	}
	
	private void buildLogViewer() {
		GridBagLayout bag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets.right = 5;
		gbc.insets.bottom = 5;
		gbc.insets.left = 5;
		gbc.insets.top = 5;
		
		BorderLayout bl = new BorderLayout();
		logFilePanel = new JPanel(bag);
		logPanel = new JPanel();
		logPanel.setLayout(bl);
		
		JLabel l = new JLabel("File:", SwingConstants.LEFT);
		lvFile = new JTextField(24);
		load = new JButton("Load");
		load.addActionListener(this);
		load.addItemListener(this);
		load.addChangeListener(this);
		bag.setConstraints(l,gbc);
		logFilePanel.add(l);
		
		gbc.gridx = 1;
		bag.setConstraints(lvFile,gbc);
		logFilePanel.add(lvFile);
		
		gbc.gridx = 2;
		bag.setConstraints(load,gbc);
		logFilePanel.add(load);
		
		sdtBtn = new JButton("Open in SDT");
		sdtBtn.addActionListener(this);
		sdtBtn.addItemListener(this);
		sdtBtn.addChangeListener(this);
		sdtBtn.setEnabled(false);
		gbc.gridx = 3;
		logFilePanel.add(sdtBtn, gbc);
		
		
//		logFilePanel.add(l,BorderLayout.WEST);
//		logFilePanel.add(lvFile,BorderLayout.CENTER);
//		logFilePanel.add(load,BorderLayout.EAST);
//		bag.setConstraints(logFilePanel,gbc);
		logPanel.add(logFilePanel, BorderLayout.NORTH);
		
		// Reset right and left to zero
		gbc.insets.right = 0;
		gbc.insets.left = 0;
		gbc.gridy = 1;
		
		logTextPane = new PC2TextPane(false, false);
		//consolePane.setBackground(new Color(240,240,240));
		//consolePane.init(false);
		logTextPane.init(true, false);
		logTextPane.addMouseListener(logTextPane);
		logScrollPane = new JScrollPane(logTextPane);
		logScrollPane.addMouseListener(logTextPane);
		logScrollPane.setAutoscrolls(true);
		logViewer = new PC2LogViewer(logScrollPane, logTextPane);
		//bag.setConstraints(logScrollPane,gbc);
		logPanel.add(logScrollPane, BorderLayout.CENTER);
	}
	private void buildRegistrarPanel(Dimension size) {
		
		registrar = new JPanel();
		Dimension paneSize = new Dimension(size.width-10, size.height-35);
		//registrar.setPreferredSize(paneSize);
		
		//regTable = new JTable(results, colHdrs);
//		tm = new DefaultTableModel();
		
		//tcm = new DefaultTableColumnModel();
//		tm.setDataVector(null,colHdrs);
//		tm.setColumnCount(3);
//		tm.setRowCount(0);
//		regTable = new JTable(tm);
		regTable = new PC2Table();
		regTableModel = (DefaultTableModel)regTable.getModel();
		regTableModel.setDataVector(null,registrarColHdrs);
		regTableModel.setColumnCount(3);
		regTableModel.setRowCount(0);

		regColModel = (DefaultTableColumnModel)regTable.getColumnModel();
		regColModel.setColumnSelectionAllowed(false);
		regTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		regTable.setCellSelectionEnabled(false);
		regTable.setRowSelectionAllowed(false);
		regTable.setFocusable(false);
		
		// Size the columns
		int statusSize = 125;
		int labelSize = 125;
		int ipSize = size.width-10 - statusSize - labelSize;
		PC2StatusCellRenderer statusRenderer = new PC2StatusCellRenderer();
		TableColumn statusCol = regColModel.getColumn(0);
		statusCol.setCellRenderer(statusRenderer);
		statusCol.setPreferredWidth(statusSize);
		TableColumn labelCol = regColModel.getColumn(1);
		labelCol.setPreferredWidth(labelSize);
		TableColumn ipCol = regColModel.getColumn(2);
		ipCol.setPreferredWidth(ipSize);
		
	    registrarScrollPane = new JScrollPane(regTable);
	    registrarScrollPane.setPreferredSize(paneSize);
	    registrar.add(registrarScrollPane, BorderLayout.NORTH);
	}
	
	private void buildResultsPanel(Dimension size) {
		results = new JPanel();
		Dimension paneSize = new Dimension(size.width-10, size.height-35);
		resultTable = new PC2Table();
		resultTableModel = (DefaultTableModel)resultTable.getModel();
		resultTableModel.setDataVector(null,resultColHdrs);
		resultTableModel.setColumnCount(4);
		resultTableModel.setRowCount(0);

		resultColModel = (DefaultTableColumnModel)resultTable.getColumnModel();
		resultColModel.setColumnSelectionAllowed(false);
		resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		resultTable.setCellSelectionEnabled(false);
		resultTable.setRowSelectionAllowed(false);
		resultTable.setFocusable(false);
		
		// Size the columns
		int resultSize = 75;
		int dutSize = 150;
		int tcSize = 150;
		int runSize = 50;
		PC2ResultCellRenderer resultRenderer = new PC2ResultCellRenderer();
		TableColumn resultCol = resultColModel.getColumn(0);
		resultCol.setCellRenderer(resultRenderer);
		resultCol.setPreferredWidth(resultSize);
		TableColumn runCol = resultColModel.getColumn(1);
		runCol.setPreferredWidth(runSize);
		TableColumn tcCol = resultColModel.getColumn(2);
		tcCol.setPreferredWidth(tcSize);
		TableColumn dutCol = resultColModel.getColumn(3);
		dutCol.setPreferredWidth(dutSize);
		
	    resultScrollPane = new JScrollPane(resultTable);
	    resultScrollPane.setPreferredSize(paneSize);
	    results.add(resultScrollPane, BorderLayout.NORTH);

	}
	
	protected void changeRegistrarLabel(String pui , String curLabel, String newLabel) {
		int rows = regTableModel.getRowCount();
		boolean found = false;
		for (int i =0; i< rows && !found; i++) {
			String puiVal = (String)regTableModel.getValueAt(i, PUI_COLUMN);
			String labelVal = (String)regTableModel.getValueAt(i, LABEL_COLUMN);
			if (puiVal != null && 
					puiVal.equals(pui) &&
					labelVal != null &&
					labelVal.equals(curLabel)) {
				regTableModel.setValueAt(newLabel, i, LABEL_COLUMN);
				found = true;
				if (PC2UI.logger != null) {
					PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
							"Changing the NELabel in the registrar table for element defined by pui=[" 
							+ puiVal + "] and currentLabel=[" + labelVal + "] to the new label=[" 
							+ newLabel +"].");
				}
			}
		}
		if (!found && PC2UI.logger != null) {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"GUI could not changing the NELabel in the registrar table for element defined by pui=[" 
					+ pui + "] and currentLabel=[" + curLabel + "] to the new label=[" 
					+ newLabel +"] because no match of pui and label could be found.");
			
		}
	}
	
	protected void changeRegistrarStatus(PC2RegistrarStatus status, String pui, 
			String curLabel) {
		int rows = regTableModel.getRowCount();
		boolean found = false;
		for (int i =0; i< rows && !found; i++) {
			String puiVal = (String)regTableModel.getValueAt(i, PUI_COLUMN);
			String labelVal = (String)regTableModel.getValueAt(i, LABEL_COLUMN);
			if (puiVal != null && 
					puiVal.equals(pui) &&
					labelVal != null &&
					labelVal.equals(curLabel)) {
				regTableModel.setValueAt(status, i, STATUS_COLUMN);
				found = true;
				if (PC2UI.logger != null) {
					PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
							"Changing the Status in the registrar table for element defined by pui=[" 
							+ puiVal + "] and currentLabel=[" + labelVal + "] to the new status=[" 
							+ status +"].");
				}
			}
		}
		if (!found && PC2UI.logger != null) {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"GUI could not changing the Status in the registrar table for element defined by pui=[" 
					+ pui + "] and currentLabel=[" + curLabel + "] to the new label=[" 
					+ status +"] because no match of pui and label could be found.");
			
		}
	}
	
	protected void clearRegistrarElements() {
		while (regTableModel.getRowCount() > 0)
			regTableModel.removeRow(0);
		
		
		if (PC2UI.logger != null) {
			String tableDump = "";
			int rows = regTableModel.getRowCount();
			int cols = regTableModel.getColumnCount();
			for (int i = 0; i < rows && rows != -1; i++) {
				for (int j = 0; j < cols && cols != -1; j++) {
					tableDump += "[" + regTableModel.getValueAt(i,j) + "] ";
				}
				tableDump += "\n";
			}
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
				"\n Table:\n" + tableDump);	
		}
	}
	
	 /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) { //, String description) {
        java.net.URL imgURL = PC2TabPane.class.getResource(path);
        if (imgURL != null) {
        	//return new ImageIcon(imgURL, description);
        	return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    

	private int [] findValue(DefaultTableModel tm, String v, String colHdr) {
		int [] result = new int [2];
		result[0] = -1;
		result[1] = -1;
		int col = -1;
		col = tm.findColumn(colHdr);
		int row = -1;
		if (col > -1) {
			for (int i = 0; i < tm.getRowCount() && row == -1; i++) {
				
				Object o = tm.getValueAt(i,col);
				if (o instanceof String) {
					String val = (String)o;
					if (val.equals(v))
						row = i;
				}
			}
		}
		if (row > -1) {
			result[0] = row;
			result[1] = col;
		}
		return result;
		
	}
	
//	private String getValueAt(DefaultTableModel tm, int row, int col) {
//		String result = null;
//		Object o = tm.getValueAt(row,col);
//		if (o instanceof String) {
//			result = (String)o;
//					
//			
//		}
//		return result;
//	}
	
	@Override
	public void itemStateChanged(ItemEvent ev) {
		PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
				"StateChanged " + ev);
	}
	
	protected void loadLog(File f) {
		if (f.exists() && f.canRead() && f.isFile()) {
			try {
			    loadedLog = f;
				// Make the pane editable to load the file, then
				// not editable afterwards
//				logTextPane.setEditable(true);
				logTextPane.setText("");
				
				BufferedReader in = new BufferedReader(new FileReader(f));
				String line = in.readLine();
				String logMsg = null;
				
				while (line != null) {
					// Test whether the current line is apart of the previous
					// log message or the start of a new one.
					// This is done by verifying the first field in a date
					// in the following format: yyyy-mm-dd
					if (logMsg != null && line.length() > 7 && line.charAt(4) == '-' &&
							line.charAt(7) == '-') {
						logViewer.appendLog(logMsg);
						logMsg = line + "\n";
					}
					// Since it doesn't appear to have the time-date format see
					// if the line begins with one of our logging levels. This
					// could be the file is a cut and paste from a console window
					// instead of one of the systems log files.
					else if (logMsg != null && line.length() > 7 && 
							(logViewer.isConsoleLog(line))) {
						logViewer.appendConsole(logMsg);
						logMsg = line + "\n";
					}
					else if (logMsg == null)
						logMsg = line + "\n";
					else
						logMsg += line + "\n";
					line = in.readLine();
					
				}
				
				if (logMsg != null) {
					if (logViewer.isConsoleLog(logMsg)) 
						logViewer.appendConsole(logMsg);
					else
						logViewer.appendLog(logMsg);
				}
				in.close();
				sdtBtn.setEnabled(true);
//				logTextPane.setEditable(false);
			}
			catch (FileNotFoundException fnf) {
				if (PC2UI.logger != null)
					PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
							"UI could not find the log file[" 
							+ f.getAbsolutePath() + "] to read.\n");
				fnf.printStackTrace();
			}
			catch (IOException io) {
				if (PC2UI.logger != null)
					PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
							"UI encountered an error while trying to read log file[" 
							+ f.getAbsolutePath() + "].");
				io.printStackTrace();
			}
			catch (IllegalArgumentException ia) {
				if (PC2UI.logger != null)
					PC2UI.logger.warn(PC2LogCategory.UI, PC2UI.subCat,
							"UI encountered an illegal argument exception while"
							+ " trying to read log file[" + f.getAbsolutePath() + "].");
				ia.printStackTrace();
			}
		}	
	}
	
	private void maximizePane() {
		Dimension scrollPaneSize = new Dimension(maxWidth-10, maxHeight-35);
		registrarScrollPane.setPreferredSize(scrollPaneSize);
		resultScrollPane.setPreferredSize(scrollPaneSize);
		Dimension size = new Dimension(maxWidth, maxHeight);
		pane.setMaximumSize(size);
		pane.setMinimumSize(size);
		pane.setPreferredSize(size);
		pane.setVisible(false);
		tabFullScreen = true;
		pane.setVisible(true);
		//JViewport vp = consoleScrollPane.getViewport();
		//vp.set
		//setBounds(x,y,normalWidth, normalHeight);
	}

	@Override
	public void mousePressed(MouseEvent e) {
	      // if (PC2UI.logger != null)
	    	//   PC2UI.logger.trace("Mouse pressed; # of clicks: "
	        //            + e.getClickCount() + e);
	    }

	    @Override
		public void mouseReleased(MouseEvent e) {
	    	// if (PC2UI.logger != null)
		    //	   PC2UI.logger.trace("Mouse released; # of clicks: "
	         //           + e.getClickCount() + e);
	    }

	    @Override
		public void mouseEntered(MouseEvent e) {
	    	// if (PC2UI.logger != null)
		    //	   PC2UI.logger.trace("Mouse entered" + e);
	    }

	    @Override
		public void mouseExited(MouseEvent e) {
	    	// if (PC2UI.logger != null)
		    //	   PC2UI.logger.trace("Mouse exited" + e);
	    }
	
	    @Override
		public void mouseClicked(MouseEvent e) {
	    	Object src = e.getSource();
	    	if (src instanceof JTabbedPane) {
	    		JTabbedPane tp = (JTabbedPane)src;
	    		//int index = tp.getModel().getSelectedIndex();
	    		if (e.getClickCount() == 2) { //&& index == 0
	    			if (viewNormal) {
	    				owner.controlPane.minimizePane();
	    				maximizePane();
	    				viewNormal = false;
	    				//PC2UI.paintAll();
	    				
	    				if (PC2UI.logger != null)
	    					PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
	    						"Mouse clicked (# of clicks: "
	    						+ e.getClickCount() + ")" + tp);
 	    			}
	    			else {
	    				viewNormal = true;
	    				restorePane();
	    				owner.controlPane.restorePane();
	    				//PC2UI.paintAll();
	    				if (PC2UI.logger != null)
	    					PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
	    						"Mouse clicked (# of clicks: "
	    						+ e.getClickCount() + ")" + tp);
	    				
	    			}
	    			
	    		}
	    		else {
	    			if (PC2UI.logger != null)
	    				PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
	    					"Mouse clicked (# of clicks: "
	    					+ e.getClickCount() + ")" + tp);
	    		}
	    	}
	    }
	    
		protected void setTestResults(PC2Result r, String runNum, String tcFile, String dutFile) {
			int dutCol = resultTableModel.findColumn(DUT);
			int tcCol = resultTableModel.findColumn(TC);
			int runCol = resultTableModel.findColumn(RUN);
			int row = -1;
			if (dutCol > -1 && tcCol > -1 && runCol > -1) {
				for (int i = 0; i < resultTableModel.getRowCount() && row == -1; i++) {
					Object d = resultTableModel.getValueAt(i,dutCol);
					Object t = resultTableModel.getValueAt(i,tcCol);
					Object rn = resultTableModel.getValueAt(i,runCol);
					if (d instanceof String &&
							t instanceof String &&
							rn instanceof String) {
						String dut = (String)d;
						String tc = (String)t;
						String rNum = (String)rn;
						if (dutFile.equals(dut) &&
								tcFile.equals(tc) &&
								rNum.equals(runNum))
							row = i;
					}
				}
			}
			if (row > -1) {
				int rCol = resultTableModel.findColumn(RESULT);
				if (rCol > -1) 
					resultTableModel.setValueAt(r,row,rCol);
			}
			else
				resultTableModel.addRow(new Object []{r, runNum, tcFile, dutFile});
				
		}

		@Override
		public void stateChanged(ChangeEvent ev) {
			try {
				 if (PC2UI.logger != null)
					 PC2UI.logger.trace(PC2LogCategory.UI, PC2UI.subCat,
							 "PC2TabPane ChangeEvent " + ev.getSource());
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		protected void updateSize(int width, int height) {
			this.normalWidth = width - controlPaneWidth; //width/2;
			this.normalHeight = height-65;
			this.maxWidth = width - 15;
			this.maxHeight = normalHeight;
			if (tabFullScreen)
				maximizePane();
			else
				restorePane();
			
//			Dimension scrollPaneSize = null;
//			Dimension size = null;
//			if (tabFullScreen) {
//				size = new Dimension(maxWidth, maxHeight);
//				scrollPaneSize = new Dimension(maxWidth-10, maxHeight-35);
//			}
//			else {
//				scrollPaneSize =new Dimension(normalWidth-10, normalHeight-35);
//				size = new Dimension(normalWidth, normalHeight);
//			}
//			registrarScrollPane.setPreferredSize(scrollPaneSize);
//			resultScrollPane.setPreferredSize(scrollPaneSize);
//			pane.setMaximumSize(size);
//			pane.setMinimumSize(size);
//			pane.setPreferredSize(size);
//			pane.setVisible(false);
//			pane.setVisible(true);
		}
		
		private void restorePane() {
			Dimension scrollPaneSize = new Dimension(normalWidth-10, 
					normalHeight-35);
			registrarScrollPane.setPreferredSize(scrollPaneSize);
			resultScrollPane.setPreferredSize(scrollPaneSize);
			Dimension size = new Dimension(normalWidth, normalHeight);
			pane.setMaximumSize(size);
			pane.setMinimumSize(size);
			pane.setPreferredSize(size);
			pane.setVisible(false);
			tabFullScreen = false;
			pane.setVisible(true);
			//setBounds(x,y,normalWidth, normalHeight);
		}
		


}
