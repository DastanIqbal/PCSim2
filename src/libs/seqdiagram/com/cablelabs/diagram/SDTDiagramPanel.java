package com.cablelabs.diagram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.TextArea;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.cablelabs.log.LogAPI;
import com.cablelabs.log.LogCategory;

public class SDTDiagramPanel extends JPanel implements TreeSelectionListener {


	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final LogAPI logger = LogAPI.getInstance();
	private static final String subCat = "SDTDiagramPanel";

	
//	private static Image backgroundImage = null;
//
//	private static Image computerImage = null;
	
	
	// The basic layout is the following
	//
	// basePanel holds textPanel and the diagramPanel.
	// textPanel is on the left and holds the treePanel and the msgPanel.
	//
	//
	//                     basePanel
	// -----------------------------------------------------------------------
	// |    Log File Tree      |          Sequence Diagram Canvas            |
	// |     (treePanel)       |             (diagramPanel)                  |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |_______________________|                                             |
	// |      	Message        |                                             |
	// |       (msgPanel)      |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// |                       |                                             |
	// -----------------------------------------------------------------------
	private JSplitPane basePanel;
	private SDTCanvas canvas = null;

	private JScrollPane canvasScrollPane;
	private HashMap<String, Configuration> configurations = null;
	
	private TextArea messageTextArea = null;

	private JPanel msgPanel;
	private LogParser parser = null;
	private Configuration selectedConfig = null;
	private String selectedFSM = null;

	private Test selectedTest = null;
	private JLabel selectionLabel = null;

	private JSplitPane textPanel;
	//	private JPanel diagramPanel;
	private LogTree treePanel;
	
	private File displayedFile;

	public SDTDiagramPanel() {
		super();
		parser = new LogParser();

//		Toolkit toolkit = Toolkit.getDefaultToolkit();
//		URL url = SeqDiagram.class.getResource("images/logoMain.gif");
//		backgroundImage = toolkit.getImage(url);
//		url = SeqDiagram.class.getResource("images/comp.gif");
//		computerImage = toolkit.getImage(url);

		initComponents();
		//this.setPreferredSize(new Dimension(600,600));
		this.setMinimumSize(new Dimension(450,630));
	}

	@Override
	public void valueChanged(TreeSelectionEvent tse) {
	    Configuration newSelConfig = null;
	    Test newSelTest = null;
	    String newSelFSM = null;

	    messageTextArea.setText("");
	    boolean selectedEvent = true;
		TreePath tp = tse.getNewLeadSelectionPath( );
		if (tp != null) {
			selectionLabel.setText("Selected: " + tp.getLastPathComponent( ));
			int count = tp.getPathCount();
			if (count > 0 && count <= Configuration.TREE_DEPTH) {
				count--;
				Object [] elements = tp.getPath();
				if (count == 4) {
					logger.debug(LogCategory.APPLICATION, subCat,  "Tree - Selected an Event.");
					DefaultMutableTreeNode conf = (DefaultMutableTreeNode)elements[Configuration.CONFIG_POS];
					newSelConfig = configurations.get(cleanTreeNode(conf));
					if (newSelConfig != null) {
						DefaultMutableTreeNode test = (DefaultMutableTreeNode)elements[Configuration.TEST_POS];
						DefaultMutableTreeNode fsm = (DefaultMutableTreeNode)elements[Configuration.FSM_POS];
						DefaultMutableTreeNode event = (DefaultMutableTreeNode)elements[Configuration.EVENT_POS];
						// Next we need to get the index of the test and event since there can be more than
						// one element in the tree with the same value.
						int testIndex = conf.getIndex(test);
						int eventIndex =  fsm.getIndex(event);
						newSelTest = newSelConfig.getTestByIndex(testIndex);
						newSelFSM = cleanTreeNode(fsm);
						Event e = newSelTest.getFSMsTable().get(newSelFSM).getEvent(eventIndex);
						if (e != null) {
						    selectedEvent = true;
							messageTextArea.setText(e.getMessage());
							canvas.selectEvent(e);
						}
					}
					else {
						Iterator<String> keys = configurations.keySet().iterator();
						boolean first = true;
						String validValues = "";
						while (keys.hasNext()) {
							if (first) {
								first = false;
								validValues = keys.next();
								//if (validValues.equals())
							}
							else
								validValues += ", " + keys.next();
						}
						System.err.println("System could not find configuration("
								+ conf.toString() + ") in the elements[" + validValues
								+ "] to display message details.");


					}
				}
				else if (count == 3 ) {
					messageTextArea.setText("");
					DefaultMutableTreeNode conf = (DefaultMutableTreeNode)elements[Configuration.CONFIG_POS];
					newSelConfig = configurations.get(cleanTreeNode(conf));
					if (newSelConfig != null) {
						DefaultMutableTreeNode test = (DefaultMutableTreeNode)elements[Configuration.TEST_POS];
						DefaultMutableTreeNode fsm = (DefaultMutableTreeNode)elements[Configuration.FSM_POS];
						int testIndex = conf.getIndex(test);
						newSelTest = newSelConfig.getTestByIndex(testIndex);
						newSelFSM = cleanTreeNode(fsm);
					}
					logger.debug(LogCategory.APPLICATION, subCat,  "Tree - Selected an FSM.");
				}
				else if (count == 2) {
					messageTextArea.setText("");
					DefaultMutableTreeNode conf = (DefaultMutableTreeNode)elements[Configuration.CONFIG_POS];
					newSelConfig = configurations.get(cleanTreeNode(conf));
					if (newSelConfig != null) {
						DefaultMutableTreeNode test = (DefaultMutableTreeNode)elements[Configuration.TEST_POS];
						int testIndex = conf.getIndex(test);
						newSelTest = newSelConfig.getTestByIndex(testIndex);
					}
					logger.debug(LogCategory.APPLICATION, subCat,  "Tree - Selected a Test.");
				}
				else if (count == 1) {
					messageTextArea.setText("");
					DefaultMutableTreeNode conf = (DefaultMutableTreeNode)elements[Configuration.CONFIG_POS];
					newSelConfig = configurations.get(cleanTreeNode(conf));
					logger.debug(LogCategory.APPLICATION, subCat,  "Tree - Selected a Configuration.");
				}
				else {
					messageTextArea.setText("");
					logger.debug(LogCategory.APPLICATION, subCat,  "Tree - Selected the Log File name.");
				}
			}
		}
		else if (treePanel != null) {
			//treePanel.setSelection(null);
			selectionLabel.setText("Nothing selected.");
		}

		// Update if needed.
		boolean configDiff = (selectedConfig == null && newSelConfig != null)    || (selectedConfig != null && !selectedConfig.equals(newSelConfig));
		boolean testDiff = (selectedTest == null && newSelTest != null) || (selectedTest != null && !selectedTest.equals(newSelTest));
		boolean fsmDiff = (selectedFSM == null && newSelFSM != null)   || (selectedFSM != null && !selectedFSM.equals(newSelFSM));

		if (selectedEvent || configDiff || testDiff || fsmDiff) {
			selectedConfig = newSelConfig;
			selectedTest = newSelTest;
			selectedFSM = newSelFSM;

			canvas.refresh();
		}

	}

	public void display(File f ) {
		resetData();
		configurations = parser.parse(f);
	    treePanel.init(f, configurations);
	    displayedFile = f;
	    canvas.refresh();
	}
	private void initComponents() {
		this.setLayout(new GridBagLayout());
		
		
		/********************    BASE PANEL         ********************************/
		basePanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		basePanel.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1d;
		c.weighty = 1d;
		c.fill = GridBagConstraints.BOTH;
		add(basePanel, c);


		/***************************  LISTS PANEL  ********************************/
		textPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		basePanel.setLeftComponent(textPanel);


		/*************************** TREE PANEL  ********************************/

		treePanel = new LogTree();
		treePanel.addListener(this);
		textPanel.setTopComponent(treePanel);
		treePanel.setPreferredSize(new Dimension(300, 300));

		/***************************  Message PANEL  ********************************/
		msgPanel = new JPanel();
		msgPanel.setPreferredSize(new Dimension(300, 300));
		// if set to false, we see the container's background
		msgPanel.setOpaque(false);
		msgPanel.setLayout(new BorderLayout());
		msgPanel.setToolTipText("The currently selected message in the Tree and Sequence Diagram.");
		messageTextArea = new TextArea();
		messageTextArea.setBackground(Color.white);
		messageTextArea.setEditable(false);
		messageTextArea.setFont(new Font("Dialog", 1, 12));
		messageTextArea.setForeground(Color.black);

		TitledBorder border2 = new TitledBorder("Message Details:");
		border2.setTitleJustification(TitledBorder.CENTER);
		msgPanel.setBorder(border2);
		msgPanel.add(messageTextArea, BorderLayout.CENTER);

		selectionLabel = new JLabel("Nothing selected.");
		selectionLabel.setOpaque(false);
	    msgPanel.add(selectionLabel, BorderLayout.SOUTH);
		//PercentLayoutConstraint msgPanelConstraint = new PercentLayoutConstraint(0, 61, 100, 35);
		//textPanel.add(msgPanel);
	    textPanel.setBottomComponent(msgPanel);

	/******************** DIAGRAM PANEL ****************************************/

		//PercentLayoutConstraint diagramPanelConstraint = new PercentLayoutConstraint(30, 0, 70, 100);
		canvas = new SDTCanvas(this);
		canvasScrollPane = new JScrollPane(canvas);
		canvasScrollPane.setAutoscrolls(true);
		canvasScrollPane.getVerticalScrollBar().setUnitIncrement(8);
		canvasScrollPane.getHorizontalScrollBar().setUnitIncrement(8);
		//basePanel.add(canvasScrollPane, diagramPanelConstraint);
		basePanel.setRightComponent(canvasScrollPane);

		validateTree();

		updateWeightsAndSizes();
	}
	
	private void resetData() {
	    displayedFile = null;
	    parser = new LogParser();
		canvas.unselectAllShapes();
		this.selectedConfig = null;
		this.selectedFSM = null;
		this.selectedTest = null;
	}

    private void updateWeightsAndSizes() {
	    basePanel.setResizeWeight(0d);
	    basePanel.setContinuousLayout(true);
	    textPanel.setMinimumSize(new Dimension(100, 100));
        canvasScrollPane.setMinimumSize(new Dimension(200,200));

        textPanel.setResizeWeight(0.66d);
        textPanel.setContinuousLayout(true);
        treePanel.setMinimumSize(new Dimension(100,100));
        msgPanel.setMinimumSize(new Dimension(100,100));

        basePanel.setDividerLocation(0.2f);
    }

    protected TextArea getMessageTextArea() {
    	return this.messageTextArea;
    }
    
	protected Configuration getSelectedConfiguration() {
		return selectedConfig;
	}

	protected SDTIterator getSelectionEventIter() {
	    return new SDTIterator(configurations, selectedConfig, selectedTest, selectedFSM);
	}

	protected String getDisplayedFileName() {
	    if (displayedFile != null) {
	        return displayedFile.getName();
	    }
	    return null;
	}
	
	private static String cleanTreeNode(DefaultMutableTreeNode node){
	    String t = node.toString();
	    int idx = t.lastIndexOf("(");
	    if (idx > -1) {
	        return t.substring(0, idx).trim();
	    }
	    return t;
	}
	
}
