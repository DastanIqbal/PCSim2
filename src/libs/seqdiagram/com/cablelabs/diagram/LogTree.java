package com.cablelabs.diagram;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class LogTree extends JPanel {

    public static final String DUT = " (DUT)";
    public static final String TEST = " (Test)";
    public static final String FSM = " (FSM)";
    
    
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	protected JScrollPane scrollPane = null;
	//protected JPanel panel = null;
	protected JLabel selectionLabel = null;
	protected JTree tree = null;
	protected DefaultTreeModel treeModel = null;

	public LogTree() {
	    setOpaque(true);
        setLayout(new BorderLayout());
        TitledBorder border = new TitledBorder("Log Contents:");
        border.setTitleJustification(TitledBorder.CENTER);
        setBorder(border);

        treeModel = new DefaultTreeModel(null);
        tree = new JTree(treeModel);
        scrollPane = new JScrollPane(tree);
        scrollPane.setAutoscrolls(true);
        scrollPane.setToolTipText("All of the events recorded in the log file.");

        add(scrollPane, BorderLayout.CENTER);
	}

	public void addListener(TreeSelectionListener listener) {
		if (tree != null)
			tree.addTreeSelectionListener(listener);
	}


	public void init(File sf, Map<String, Configuration> configInfo) {
		if (configInfo != null && sf != null) {
			DefaultMutableTreeNode root = new DefaultMutableTreeNode(sf.getName());

			Iterator<String> confKeys = configInfo.keySet().iterator();
			while (confKeys.hasNext()) {
				String key = confKeys.next();
				Configuration c = configInfo.get(key);
				DefaultMutableTreeNode confNode = new DefaultMutableTreeNode(key + DUT);
				
				ListIterator<Test> testIter = c.getTestsListIterator();
				while (testIter.hasNext()) {
					Test t = testIter.next();
					DefaultMutableTreeNode testNode = new DefaultMutableTreeNode(t.getName() + TEST);
					Map<String, EventList> fsms = t.getFSMsTable();

					Iterator<String> fsmKeys = fsms.keySet().iterator();
					while (fsmKeys.hasNext()) {
						String fsm = fsmKeys.next();
						EventList el = fsms.get(fsm);
						DefaultMutableTreeNode fsmNode = new DefaultMutableTreeNode(fsm + FSM);

						ListIterator<Event> eventIter = el.listIterator();
						while(eventIter.hasNext()) {
							Event e = eventIter.next();
							DefaultMutableTreeNode eventNode = new DefaultMutableTreeNode(e.getFirstLine());
							fsmNode.add(eventNode);
						}

						testNode.add(fsmNode);
					}

					confNode.add(testNode);
				}

				root.add(confNode);
			}

			treeModel.setRoot(root);
			tree.putClientProperty("JTree.lineStyle", "Angled");
			
			// expand all rows
			for (int i = 0; i < tree.getRowCount(); i++) {
			    tree.expandRow(i);
			}
			
		}
	}

	public void removeListener(TreeSelectionListener listener) {
		if (tree != null)
			tree.removeTreeSelectionListener(listener);
	}
	public void setSelection(String text) {
		if (text != null)
			selectionLabel.setText(text);
		else
			selectionLabel.setText("Nothing selected.");
	}

}
