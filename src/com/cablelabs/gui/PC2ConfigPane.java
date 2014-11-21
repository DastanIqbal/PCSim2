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

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.cablelabs.gui.scripts.PC2ScriptVisualizerLauncher;
import com.cablelabs.log.PC2LogCategory;

public class PC2ConfigPane extends JPanel 
	implements ActionListener,  ChangeListener, ItemListener, MouseListener, PopupMenuListener {

	static final long serialVersionUID = 1;
	private String name = null;
	protected JPanel editPanel = new JPanel();
	protected JScrollPane scrollPane = null;
	protected JRadioButton primary = null; 
	//private boolean primaryActive = false;
	protected DefaultListModel model = new DefaultListModel();
	protected JList list = new JList(model);
	//protected LinkedList<File> fileList = new LinkedList<File>();
	
	// JButton names for configuration pane
	protected final String MOVE_UP = "Move Up";
	protected final String MOVE_DOWN = "Move Down";
	protected final String ADD = "Add";
	protected final String CLEAR = "Clear";
	protected final String CLEAR_ALL = "Clear All";
	
	// JButtons for configuration pane
	protected JButton moveUp = new JButton (MOVE_UP);
	protected JButton moveDown = new JButton (MOVE_DOWN);
	protected JButton add = new JButton(ADD);
	protected JButton clear = new JButton (CLEAR);
	protected JButton clearAll = new JButton (CLEAR_ALL);
	
	protected String fileExt = null;
	private boolean dutConfig = false;
	private boolean tsConfig = false;
	//private File lastActiveDirectory = new File(".");
	protected boolean modified = false;
	
	protected JPopupMenu popup = null;
	protected JButton popupEdit = null;
	protected final String popupLabel = "Edit";
	protected final String viewPopupLabel = "View";
	
	public PC2ConfigPane(String name, String ext, boolean selectPrimary) {
		super();
		this.name = name;
		this.fileExt = ext;
		if (name.equals(PC2UI.DUT)) 
			dutConfig = true;
		else if (name.equals(PC2UI.TS))
			tsConfig = true;
		//this.description = descrip;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		TitledBorder border = new TitledBorder(this.name);
		primary = new JRadioButton("Primary", selectPrimary);
		primary.addActionListener(this);
		
		popup = new JPopupMenu();
		{ // scope off item so it is not accidentally used elsewhere
    		JMenuItem item;
    		popup.add(item = new JMenuItem(popupLabel));
    		item.addActionListener(this);
    		
    		if (tsConfig) {
    		    popup.add(item = new JMenuItem(viewPopupLabel));
    		    item.addActionListener(this);
    		}
		}
		
		popup.setBorder(new BevelBorder(BevelBorder.RAISED));
		popup.addPopupMenuListener(this);
		popup.setEnabled(false);
		
        
		addListeners(moveUp);
		addListeners(moveDown);
		addListeners(add);
		addListeners(clear);
		addListeners(clearAll);
		
				
		disableListButtons();
		
		GridBagLayout bag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
	    Insets i = gbc.insets;
	    i.bottom = 8;
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    //editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.Y_AXIS));
		editPanel.setLayout(bag);
		bag.setConstraints(primary, gbc);
		editPanel.add(primary);
		
		gbc.gridy = 1;
		bag.setConstraints(add, gbc);
		editPanel.add(add);
		
		i.bottom = 5;
		gbc.gridy= 2;
		bag.setConstraints(moveUp, gbc);
		editPanel.add(moveUp);

		gbc.gridy = 3;
		bag.setConstraints(moveDown, gbc);
		editPanel.add(moveDown);
		
		gbc.gridy = 4;
		bag.setConstraints(clear, gbc);
		editPanel.add(clear);
		
		gbc.gridy = 5;
		bag.setConstraints(clearAll, gbc);
		editPanel.add(clearAll);

		list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		list.setVisibleRowCount(-1);
		list.addMouseListener(this);
		
		scrollPane = new JScrollPane(list);
		scrollPane.setBorder(border);
		scrollPane.setAutoscrolls(true);
		
		add(scrollPane);
		add(editPanel);
	
	}

	private void addListeners(JButton button) {
		button.addActionListener(this);
		button.addItemListener(this); 
		button.addChangeListener(this); 
	}
	
    @Override
	public void actionPerformed(ActionEvent ev) {
    	// Get the action command
    	String action = ev.getActionCommand();
    	if (dutConfig && action.equals(popupLabel) ) {
    		int index = list.getSelectedIndex();
    		DefaultListModel model = (DefaultListModel)list.getModel();
    		File dut = (File)model.get(index);
    		PC2UI.launchEditor(dut, true);
    	}
    	else if (tsConfig && action.equals(popupLabel) ) {
    		int index = list.getSelectedIndex();
    		DefaultListModel model = (DefaultListModel)list.getModel();
    		File ts = (File)model.get(index);
    		PC2UI.launchEditor(ts, false);
    	}
    	else if (tsConfig && action.equals(viewPopupLabel) ) {
    	    int index = list.getSelectedIndex();
            DefaultListModel model = (DefaultListModel)list.getModel();
            File ts = (File)model.get(index);
            PC2ScriptVisualizerLauncher.showFile(ts.getAbsolutePath());
    	}
    	else {
    		if (action.equals(MOVE_UP)) {
    			moveUpAction(ev);
    		}
    		else if (action.equals(MOVE_DOWN)){
    			moveDownAction(ev);
    		}
    		else if (action.equals(ADD)) {
    			addAction(ev);
    		}
    		else if (action.equals(CLEAR)) {
    			clearAction(ev);
    		}
    		else if (action.equals(CLEAR_ALL)) {
    			clearAllAction(ev);
    		}
    		else if (action.equals("Primary")) {
    			System.err.println("Primary actionPerformed");
    			Container c = this.getParent();
    			boolean selected = primary.isSelected();
    			if (c instanceof PC2ControlPane) {
    				((PC2ControlPane)c).primarySelectionChanged(this, selected);
    			}
    		}
    		modified = true;
    	}
    
    }
    @Override
	public void stateChanged(ChangeEvent ev) {
		if (PC2UI.logger != null)
			PC2UI.logger.trace(PC2LogCategory.UI, PC2UI.subCat,
					"ConfigPane ChangeEvent!");
	}
    
    @Override
	public void itemStateChanged(ItemEvent ev) {
    	if (PC2UI.logger != null)
    		PC2UI.logger.trace(PC2LogCategory.UI, PC2UI.subCat,
    				"ConfigPane ItemEvent!");
	}
    
    protected void activateListButtons(boolean modifier) {
    	moveUp.setEnabled(true);
		moveDown.setEnabled(true);
		clear.setEnabled(true);
		clearAll.setEnabled(true);
		Container parent = this.getParent();
		if (parent != null) {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"Parent " + parent);
			if (parent instanceof PC2ControlPane) {
				PC2ControlPane cp = (PC2ControlPane)parent;
				if (dutConfig)
					cp.dutSet = true;
				else if (tsConfig)
					cp.testScriptSet = true;
				cp.modified = modifier;
				cp.enableTestButtons();
			}
		}
    }
    private void addAction(ActionEvent ev) {
        JFileChooser chooser = new JFileChooser( );
        chooser.setMultiSelectionEnabled(true);
        String psExt = ".xls";
		chooser.addChoosableFileFilter(new PC2FileFilter(psExt, 
				"Platform Settings ( *" + psExt + " )"));
        chooser.addChoosableFileFilter(new PC2FileFilter(fileExt, 
				name + " ( *" + fileExt + " )"));
//        if (fileExt.equals(".xls")) {
//        	String propExt = ".properties";
//			chooser.addChoosableFileFilter(new PC2FileFilter(propExt, 
//					"DUT Properties ( *" + propExt + " )"));
//        }
        History history = History.getInstance();
        File dir = null;
        if (dutConfig)
        	dir = history.lastDCFDirectory;
        else if (tsConfig)
        	dir = history.lastScriptsDirectory;
        else
        	dir = new File(".");
		chooser.setCurrentDirectory(dir);
		int option = chooser.showOpenDialog(PC2ConfigPane.this);
		if (option == JFileChooser.APPROVE_OPTION) {
			File[] sf = chooser.getSelectedFiles( );
	        if (sf.length > 0)  {
	    	  DefaultListModel model = (DefaultListModel)list.getModel();
	    	  String fl = null;
	    	  for (int i = 0; i < sf.length; i++) {
	              model.addElement(sf[i]);
	              //fileList.add(sf[i]);  
	              if (i == 0)
	            	  fl = sf[i].getName();
	              else
	            	  fl += ", " + sf[i].getName();
	              if (name.equals(PC2UI.DUT))
	            	  PC2UI.pc.addDUTConfig(sf[i]);
	       		}
	    	    PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
	    	    		"You chose " + fl);
				activateListButtons(true);
				JScrollBar sb = scrollPane.getHorizontalScrollBar();
				int value = sb.getValue();
				PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
						"Value=" + value);
				sb.setValue(100);
			}
		}
		else {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"You canceled.");
		}
		if (dutConfig) {
			history.lastDCFDirectory = chooser.getCurrentDirectory();
			HistoryParser.writeHistoryFile();
		}
		else if (tsConfig) {
			history.lastScriptsDirectory = chooser.getCurrentDirectory();
			HistoryParser.writeHistoryFile();
		}
    }
    protected void clearAction(ActionEvent ev) {
    	// NOTE ev may be null value
    	int index = list.getSelectedIndex();
    	if (index > -1) {
    		if (name.equals(PC2UI.DUT)) {
    			File f = (File) model.get(index);
    			PC2UI.pc.removeDUTConfig(f);
    		}
    		model.remove(index);

    		int size = model.getSize();

    		if (size == 0) { //Nobody's left, disable firing.
    			disableListButtons();

    		} else { //Select an index.
    			if (index == model.getSize()) {
    				//removed item in last position
    				index--;
    			}

    			list.setSelectedIndex(index);
    			list.ensureIndexIsVisible(index);
    		}
    	}

    }
    
    protected void setAllButtons(boolean flag) {
    	add.setEnabled(flag);
    	moveUp.setEnabled(flag);
    	moveDown.setEnabled(flag);
		clear.setEnabled(flag);
		clearAll.setEnabled(flag);
		primary.setEnabled(flag);
    }
    private void disableListButtons() {
    	moveUp.setEnabled(false);
		moveDown.setEnabled(false);
		clear.setEnabled(false);
		clearAll.setEnabled(false);
		Container parent = this.getParent();
		if (parent != null) {
			PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
					"Parent " + parent);
			if (parent instanceof PC2ControlPane) {
				PC2ControlPane cp = (PC2ControlPane)parent;
				if (dutConfig)
					cp.dutSet = false;
				else if (tsConfig)
					cp.testScriptSet = false;
				cp.enableTestButtons();
			}
		}
    }
    
    protected void clearAllAction(ActionEvent ev) {
    	// NOTE event may be null value
    	if (name.equals(PC2UI.DUT)) {
    		for (int i=0; i < model.getSize(); i++) {
    			File f = (File) model.get(i);
    			PC2UI.pc.removeDUTConfig(f);
    		}
    	}
    	model.removeAllElements();
    	disableListButtons();
    }
 
    @Override
	public void mouseClicked(MouseEvent e) {
    }
    @Override
	public void mouseEntered(MouseEvent e) {
    }
    
    @Override
	public void mouseExited(MouseEvent e) {
    }
    @Override
	public void mousePressed(MouseEvent e) {
    	// Need to check for popup for Linux/Unix system on press to 
    	// operate on the correct event
    	checkPopup(e);
    }
    
    @Override
	public void mouseReleased(MouseEvent e) {
    	checkPopup(e);
    }

    private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger( )) {
        	Object src = e.getSource();
        	if (src instanceof JList) {
        		int index = list.getSelectedIndex();
        		if (index > -1 && popup != null) {
        			popup.show(e.getComponent(),
                            e.getX(), e.getY());

        		}
        	}
        	
        }
    }

	    
    private void moveUpAction(ActionEvent ev) {
    	int index = list.getSelectedIndex();
        
        if (index > 0) {
        	File element = (File) model.remove(index);
        	model.insertElementAt(element, index-1);
        	list.setSelectedIndex(index-1);
        }
    }
    
    private void moveDownAction(ActionEvent ev) {
    	int index = list.getSelectedIndex();
        
        int size = model.getSize();
   	
        if (index != -1 && index < (size-1)) {
        	File element = (File) model.remove(index);
        	model.insertElementAt(element, index+1);
        	list.setSelectedIndex(index+1);
        }
        PC2UI.logger.debug(PC2LogCategory.UI, PC2UI.subCat,
        		"index=" + index + ", size=" + size);
    }
 
    @Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//        System.out.println("Popup menu will be visible!");
    }
    @Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
//       System.out.println("Popup menu will be invisible!");
    }
    @Override
	public void popupMenuCanceled(PopupMenuEvent e) {
 //       System.out.println("Popup menu is hidden!");
    }

    public void setPrimary(boolean active) {
    	primary.setSelected(active);
    }
    

}
