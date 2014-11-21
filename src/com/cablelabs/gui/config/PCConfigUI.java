/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


 */
package com.cablelabs.gui.config;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import com.cablelabs.log.LogAPI;
import com.cablelabs.log.LogAPIConfig;
import com.cablelabs.log.PC2LogCategory;

public class PCConfigUI extends JFrame implements ComponentListener, TreeSelectionListener, 
WindowListener, WindowStateListener, WindowFocusListener, MouseListener, ActionListener {

	private static final long serialVersionUID = 1L;
	public static int HEIGHT = 500;
	public static int WIDTH = 650;
	protected static PCConfigUI mainFrame = null;
//	public static final String DUT = "DUT Configuration";
//	public static final String TS = "Test Scripts";
	public static final LogAPI logger;
	protected File workingDir = null;

	static {
		if (LogAPI.isConfigured()) {
			logger = LogAPI.getInstance();
		}
		else {
			LogAPIConfig config = new LogAPIConfig()
			.setCategoryClass(PC2LogCategory.class)
			.setLogPrefix("PCSim2_");

			logger = LogAPI.getInstance(config);
		}
	}

	final protected static String subCat = "";
//	protected static LinkedList<Process> children = new LinkedList<Process>();

	public static String version = "1.0-beta";
	public static String vendor = "Cable Television Laboratories, Inc.";
	public static String build = "test-build";

	// Modify Table
	protected ModifyTable mtable = null;
	
	// PCMenuBar
	protected PCMenuBar menuBar = null;
	
	// Popup Menu for Platform
	protected final static String[] platItems = new String[] { "PCSCF", "SCSCF", "UE" };
	protected JPopupMenu popup = null;
	protected JMenuItem PCSCF = null;
	protected JMenuItem SCSCF = null;
	protected JMenuItem UE = null;
	
	// Table
	protected PCTable propTable = null;
	
	// Tree
	protected JPanel displayPane = null;
	private JScrollPane displayView = null;
	private DefaultTreeModel dtm = null;
	public static MutableTreeNode node;
	public static TreePath path;
	private DefaultMutableTreeNode root = null;
	private JSplitPane splitPane = null;
	private DefaultMutableTreeNode subfile = null;
	protected JTree tree = null;
	private JScrollPane treeView  = null;
	
	// ToolBar
    private JToolBar toolBar = null;
//    private JButton button = null;
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
    
	public PCConfigUI() {

		mainFrame = this;
		setLayout(new BorderLayout());
		addWindowListener(this);
		addWindowStateListener(this);
		addWindowFocusListener(this);
		addComponentListener(this);

		tree = new JTree();	
		tree.addMouseListener(this);
		tree.setEditable(true);
		tree.setModel(null);
		treeView = new JScrollPane(tree);

		tree.addTreeSelectionListener(this);

		createTable();

		//Add the scroll panes to a split pane.
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setRightComponent(displayView);
		splitPane.setLeftComponent(treeView);

		Dimension minimumSize = new Dimension(WIDTH-300, HEIGHT);
		displayView.setMinimumSize(minimumSize);
		treeView.setMinimumSize(minimumSize);
		splitPane.setDividerLocation(200);
		splitPane.setPreferredSize(new Dimension(WIDTH-100, HEIGHT));

		//Add the split pane to this panel.
		add(splitPane);

		Dimension minSize = new Dimension (WIDTH, HEIGHT);
		mainFrame.setMinimumSize(minSize);

		setTitle("PCSim2 v" + version);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		//Link to PCMenuBar
		menuBar = new PCMenuBar();
		setJMenuBar(menuBar);
		
	    // ToolBar Menu	
	    toolBar = new JToolBar();
	    buildToolBar(toolBar);	 
	    toolBar.setFloatable(false);
        toolBar.setRollover(true);
        
		propTable.loadConfiguration();			
	}

	public void actionPerformed(ActionEvent ev) {
		String action = ev.getActionCommand();
		if(action.equals("PCSCF") || action.equals("SCSCF")	|| action.equals("UE")) {
			NetworkElement ne = propTable.getTableElement(action);
			display(ne, action, null, true);
			dtm = (DefaultTreeModel)tree.getModel();
		}
		else if(action.equals("New")) {				 
	        //Make sure we have nice window decorations.
	        JFrame.setDefaultLookAndFeelDecorated(true);
	        JDialog.setDefaultLookAndFeelDecorated(true);
	 
	        //Instantiate the controlling class.
	        JFrame frame = new JFrame("Select Properties File");
	 
	        //Create and set up the content pane.
	        PCConfigUI demo = new PCConfigUI();
	 
	        //Add components to it.
	        Container contentPane = frame.getContentPane();
	        contentPane.add(demo.createButtonPane(), BorderLayout.PAGE_END);
	 
	        //Display the window.
	        frame.pack();
	        frame.setLocationRelativeTo(null); //center it
	        frame.setVisible(true);
		}
		else if(action.equals("Open")) {
			menuBar.actionPerformed(ev);
		}
		else if(action.equals("Save")) {
			menuBar.actionPerformed(ev);
		}
		else if(action.equals("Save As")) {
			menuBar.actionPerformed(ev);
		}
		else if(action.equals("Close")) {
			menuBar.actionPerformed(ev);
		}
		else if(action.equals("DUT") || action.equals("DUT Provisioning") || action.equals("Platform")) {
//			menuBar.actionPerformed(ev);
			workingDir = new File(".");
			JFileChooser chooser = new JFileChooser(workingDir);
			chooser.setMultiSelectionEnabled(false);
			int option = chooser.showOpenDialog(this);
			if (option == JFileChooser.APPROVE_OPTION) {
	        	File sf = chooser.getSelectedFile();
	        	if (sf != null) {
	        		startNewConfiguration(sf, action);
	        	}
			}
		}
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

	protected void closeBatch() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub

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

	protected void createTable() {
		displayPane = new JPanel();	
		propTable = new PCTable();
		displayView = new JScrollPane(propTable);
		displayPane.add(displayView, BorderLayout.NORTH);
	}
	
	protected void createTree(LinkedList <File> files) {    
		tree.setModel(null);
		ListIterator <File> iter = files.listIterator();

		if(iter.hasNext()) {
			root = new DefaultMutableTreeNode(iter.next().getName(), true);
			dtm = new DefaultTreeModel(root);
			tree.setModel(dtm);
			while(iter.hasNext()) {
				dtm = (DefaultTreeModel) tree.getModel();
				root = (DefaultMutableTreeNode) dtm.getRoot();
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(iter.next().getName());
				root.add(child);
				dtm.reload(root);
			}
		}
	}	

	protected void display(NetworkElement ne, String label, File f, Boolean newFile) {
		if(f != null) {
			workingDir = new File(f.getAbsolutePath());
		}
		else if(f == null && workingDir != null) {
			workingDir = new File(".");
		}
		if(newFile != null && newFile ) {
			dtm = (DefaultTreeModel)tree.getModel();
			JFileChooser chooser = new JFileChooser( );
			chooser.setCurrentDirectory(workingDir);
			chooser.setMultiSelectionEnabled(false);
			int option = chooser.showOpenDialog(PCConfigUI.this);
			if (option == JFileChooser.APPROVE_OPTION) {
				File sf = chooser.getSelectedFile();
				if (sf != null) {
					//  create a new node
					dtm = (DefaultTreeModel) tree.getModel();
					root = (DefaultMutableTreeNode) dtm.getRoot();
					DefaultMutableTreeNode child = new DefaultMutableTreeNode(sf.getName());
					root.add(child);
					dtm.reload(root);
				}
				try {
					NetworkElement newNE = (NetworkElement) ne.clone();
					newNE.setFile(sf);
					mtable.add(label, newNE);
					propTable.populateTable(label);
				} 
				catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}			
			}
		}
		else if (newFile != null && !newFile) {
			LinkedList <File> files = new LinkedList <File> ();
			files.add(f);
			propTable.populateTable(label);
//			createTree(files);
		}
	}

	static protected PCConfigUI getMainFrame() {
		return mainFrame;
	}

	public static void main(String[] args) {
		PCConfigUI first = new PCConfigUI();
		first.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		first.setVisible(true);
	}

	// Mouse click for subfiles
	public void mouseClicked(MouseEvent e) {
		if(SwingUtilities.isRightMouseButton(e)) {
			path = tree.getPathForLocation(e.getX(), e.getY());
			Rectangle pathBounds = tree.getUI().getPathBounds(tree, path);
			if(pathBounds != null && pathBounds.contains(e.getX(), e.getY())) {
				popup = new JPopupMenu();
				for(int j = 0; j < platItems.length; j++) {
					JMenuItem pi = new JMenuItem(platItems[j]);
					if (j == 0) {
						PCSCF = pi;	
					}
					else if (j == 1) {
						SCSCF = pi;
					}
					else if (j == 2) {
						UE = pi; 
					}
					pi.addActionListener(this);
					popup.add(pi);
				}
				popup.show(tree, pathBounds.x, pathBounds.y + pathBounds.height);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	protected void startNewConfiguration(File f, String action) {
		LinkedList <File> files = new LinkedList <File> ();
		workingDir = new File(f.getAbsolutePath());
		files.add(f);
		createTree(files);
		NetworkElement ne = propTable.getTableElement(action);
		try {
			NetworkElement newNE = (NetworkElement) ne.clone();			
			newNE.setFile(f);
			mtable = new ModifyTable(action, newNE);
			propTable.populateTable(action);
		} 
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}
	
	protected void startExistingConfiguration(LinkedList<File> files, ModifyTable table, String label) {
		workingDir = new File (files.getFirst().getAbsolutePath());
		createTree(files);
		mtable = table;
//		propTable.populateTable(label);
		display(null, label, files.getFirst(), false);
	}
	
	protected void saveAsFile(File f) {
		mtable.updateFile(f);
	}
	
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree
				.getLastSelectedPathComponent();

		//		System.out.println(node + "\n " + e);
//		Object nodeInfo = node.getUserObject();
		
		if (node == null) {
			return;
		}
		else {
			//			TreePath tp = new TreePath(node);
			//            tree.scrollPathToVisible(tp);
			//            tree.setSelectionPath(tp);
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowGainedFocus(WindowEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowLostFocus(WindowEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowStateChanged(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	protected void write() {
		if(mtable != null) {
			mtable.write();
		}
	}
}
