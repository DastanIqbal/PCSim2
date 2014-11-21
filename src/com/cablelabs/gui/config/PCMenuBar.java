package com.cablelabs.gui.config;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;

import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.log.LogAPI;
import com.cablelabs.log.PC2LogCategory;

public class PCMenuBar extends JMenuBar implements ActionListener, ClipboardOwner {

	private static final long serialVersionUID = 1L;
	private JMenu fileMenu = null;
	private JMenu editMenu = null;

	protected final static String[] newItems = new String[] { "DUT", "DUT Provisioning", "Platform" };
	private String[] fileItems = new String[] { "Open", "Close", "Save", "Save As", "Exit" };
	private String[] editItems = new String[] { "Cut", "Copy", "Paste", "Find"};

	private char[] fileShortcuts = { 'O','C','S','A','X' };
	private char[] editShortcuts = { 'X','C','V','F' };
	
	protected JMenuItem newFile = null;
	protected JMenuItem dut = null;
	protected JMenuItem prov = null;
	protected JMenuItem plat = null;
	protected JMenuItem open = null;
	protected JMenuItem close = null;
	protected JMenuItem save = null;
	protected JMenuItem saveas = null;
	protected JMenuItem exit = null;
	protected JMenuItem cut = null;
	protected JMenuItem copy = null;
	protected JMenuItem paste = null;
//	protected JMenuItem find = null;

	protected JMenuItem seqDiagram = null;
	
	Clipboard clipboard = getToolkit().getSystemClipboard();
	Action cutAction = null;
	Action copyAction = null;
	Action pasteAction = null;
	
	private PCHistoryParser hp = new PCHistoryParser();
	private PCHistory history = null;
	
	private static final String subCat = "ConfigTool";
	private static LogAPI logger = LogAPI.getInstance();
	
	private LinkedList <File> files = null;
	
	private PCConfigUI ui = PCConfigUI.getMainFrame();
    
	public PCMenuBar() {		
		
	    fileMenu = new JMenu("File");
		editMenu = new JMenu("Edit");
		
		try {
			history = hp.parse();
		}
		catch (Exception ex) {
			if (PCConfigUI.logger != null) {
				PCConfigUI.logger.debug(PC2LogCategory.UI, PCConfigUI.subCat, 
						"Encountered error while trying to parse the history file.");
			}
		}
	    
	    //File Menu	
		buildFileMenu();
	   
		//Edit Menu
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
//				find = item;
			}
			item.setAccelerator(KeyStroke.getKeyStroke(editShortcuts[i],
	                Toolkit.getDefaultToolkit( ).getMenuShortcutKeyMask( ), false));
			item.addActionListener(this);
			editMenu.add(item);
		}
		editMenu.insertSeparator(3);
		findEditActions();
		
	    add(fileMenu);
	    add(editMenu);
	}
	
	@Override
	public void actionPerformed(ActionEvent ev) {
		String action = ev.getActionCommand();
		
		if(action.equals("DUT") || action.equals("DUT Provisioning") || action.equals("Platform")) {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(history.lastBatchDirectory);
			chooser.setMultiSelectionEnabled(false);
			int option = chooser.showOpenDialog(PCMenuBar.this);
	        if (option == JFileChooser.APPROVE_OPTION) {
	        	File sf = chooser.getSelectedFile();
	        	if (sf != null) {
	        		ui.startNewConfiguration(sf, action);
	        	}
	        }
		}
		else if(action.equals("Open")) {
			openAction(ev);
		}
		else if(action.equals("Close")) {
			closeAction(ev);
		}
		else if(action.equals("Save")) {
			saveAction(ev);
		}
		else if(action.equals("Save As")) {
			saveAsAction(ev);
		}
		else if(action.equals("Exit")) {
			exitAction(ev);
		}
		else if(action.equals("Find")) {
//			findAction(ev);
		}
//		else if(action.equals("About")) {
//
//		}
//		else if(action.equals("License")) {
//		
//		}
	}	

	protected void buildFileMenu() {
		//New File
		JMenu subMenu = new JMenu("New");
		for (int j = 0; j < newItems.length; j++) {
			JMenuItem ni = new JMenuItem(newItems[j]);
			if (j == 0) {
				dut = ni;
			}
			else if (j == 1) {
				prov = ni;
			}
			else if (j == 1) {
				plat = ni;
			}
			ni.addActionListener(this);
			subMenu.add(ni);
		}
		fileMenu.add(subMenu); // add submenu to fileMenu

		for (int i = 0; i < fileItems.length-1; i++) {
			JMenuItem item = new JMenuItem(fileItems[i], fileShortcuts[i]);
			if (i == 0) {
				open = item;
			}
			else if (i == 1) {
				close = item;
//				close.setEnabled(closeEnabled);
			}
			else if (i == 2) {
				save = item;
//				save.setEnabled(saveEnabled);
			}
			else if (i == 3) {
				saveas = item;
//				saveas.setEnabled(saveAsEnabled);
			}
			item.addActionListener(this);

			fileMenu.add(item);
		}
		
		// Next build the exit menu item
		exit = new JMenuItem(fileItems[4], fileShortcuts[4]);
		exit.addActionListener(this);
		fileMenu.add(exit);
		
		fileMenu.insertSeparator(3);
		
		fileMenu.insertSeparator(6);
	}
	
	protected void closeAction(ActionEvent ev) {
		System.exit(0);
	}
	
	private String createLinkLocalAddress(String propertyValue) {
   		if (propertyValue == null) {
   			return "";
   		}
   		if (propertyValue.length() == 11) {
   			propertyValue = "0" + propertyValue;
   		}
   		
   		// First two quads of link-local address are pre-defined constant "fe80::"
   		StringBuffer strBuffer = new StringBuffer("fe80::");
   			
   		// In first octet of MAC Address, invert 6th bit (zero based)
   		int bitmask = 0x02;
   		int tempInt = Integer.parseInt(propertyValue.substring(0, 2), 16);
   		int reversedOctetInt = (tempInt ^ bitmask);
   		String reversedOctetString = Integer.toHexString(reversedOctetInt);
    		// Append modified MAC Address formatted in IPv6 notation
   		strBuffer.append(reversedOctetString);
   		strBuffer.append(propertyValue.substring(2, 4));
   		strBuffer.append(":");
   		strBuffer.append(propertyValue.substring(4, 6));
   		// By algorithm for forming LinkLocal Address, insert contant "ff:fe" here.
   		strBuffer.append("ff:");
   		strBuffer.append("fe");
   		strBuffer.append(propertyValue.substring(6, 8));
   		strBuffer.append(":");
   		strBuffer.append(propertyValue.substring(8, 12));
   		
   		return strBuffer.toString().toUpperCase();
   	}
   	
   	private String createMACAddressWithColons(String v) {
   		if (v.length() == 11) {
   			return "0" + v.substring(0,1) + ":" + v.substring(1,3) + ":" + v.substring(3,5) + ":" + v.substring(5,7) +":" +
   		            v.substring(7,9) + ":" + v.substring(9);
   		}
   		else if (v.length() == 12){
   			return  v.substring(0,2) + ":" + v.substring(2,4) + ":" + v.substring(4,6) + ":" + v.substring(6,8) +":" +
   		            v.substring(8,10) + ":" + v.substring(10);
   		}
   		return null;
   	}
   	
   	private ModifyTable createModifyTable(Hashtable<String, Properties> table, String label) {
		ModifyTable mt = null;
		
		Properties p = table.get(label);
//		Enumeration<String> e = table.keys();
//		while(e.hasMoreElements()) {
		NetworkElement ne = new NetworkElement(label, files.getFirst());
		Enumeration<Object> en = p.keys();
		while(en.hasMoreElements()) {
			String tag = (String) en.nextElement();
			String value = p.getProperty(tag);
			String toolTip = ui.propTable.getToolTip(label, tag);
			ne.addSetting(tag, value, toolTip);
		}
		mt = new ModifyTable(label, ne);
		
		Enumeration<String> e = table.keys();
		while(e.hasMoreElements()) {
			String nextlabel = e.nextElement();
			p = table.get(nextlabel);
			if(nextlabel.startsWith("UE") || nextlabel.startsWith("PCSCF") || nextlabel.startsWith("SCSCF")) {
				NetworkElement newNE = new NetworkElement(nextlabel, files.getFirst());
				Enumeration<Object> en1 = p.keys();
				while(en1.hasMoreElements()) {
					String tag = (String) en1.nextElement();
					String value = p.getProperty(tag);
					String toolTip = ui.propTable.getToolTip(nextlabel, tag);
					newNE.addSetting(tag, value, toolTip);
				}
				mt.add(nextlabel, newNE);
			}
		}
		
//   		try {
//   			Enumeration<String> e = table.keys();
//   			while(e.hasMoreElements()) {
//   				String label = e.nextElement();
//   				Properties p = table.get(label);
//   				NetworkElement ne = new NetworkElement(label, files.getFirst());
//   				Enumeration<Object> en = p.keys();
//   				while(en.hasMoreElements()) {
//   					String tag = (String) en.nextElement();
//   					String value = p.getProperty(tag);
//   					String toolTip = ui.propTable.getToolTip(label, tag);
//   					ne.addSetting(tag, value, toolTip);
//   				}
//   				if(mt == null) {
//   					mt = new ModifyTable(label, ne);
//   				}
//   				else {	
//   					mt.add(label, ne);
//   				}
//   			}
//   		}
//   		catch(Exception e) {
//   			System.err.println(e.getStackTrace());
//   		}
   		return mt;
	}
   	
   	private void exitAction(ActionEvent ev) {
		System.exit(0);
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
	
	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// TODO Auto-generated method stub
	}

	protected void openAction(ActionEvent ev) {
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);
		chooser.setCurrentDirectory(history.lastBatchDirectory);
		int option = chooser.showOpenDialog(PCMenuBar.this);
		if (option == JFileChooser.APPROVE_OPTION) {
			File sf = chooser.getSelectedFile();
			if (sf != null) {				
				readPropFile(sf);
			}
		}
	}
	
	private void readPropFile(File f) {
		Properties p = new Properties();
		
		try {
			p.load(new FileInputStream(f));
			String label = p.getProperty("NE");
			String prov = p.getProperty("Provisioning Settings");
			Hashtable<String, Properties> table = new Hashtable<String, Properties>();
			if(label != null && prov == null && (label.equals("DUT"))) {
				table.put(label, p);
				readDUTParameters(p, table, f);
				ModifyTable mt = createModifyTable(table, label);
				ui.startExistingConfiguration(files, mt, label);
			}
			else if(label != null && prov != null && label.equals("DUT")) {
				table.put(label, p);
				readDUTParameters(p, table, f);
				ModifyTable mt = createModifyTable(table, label);
				ui.startExistingConfiguration(files, mt, label);
			}
			else if(label != null && label.equals("Platform")) {
				table.put(label, p);
				readPlatformParameters(p, table, f);
				ModifyTable mt = createModifyTable(table, label);
				ui.startExistingConfiguration(files, mt, label);
			}			
		}
		catch (Exception e) {

		}	
	}
	
	private void readDUTParameters(Properties p, Hashtable<String, Properties> table, File f) {
    	try {
    		files = new LinkedList <File> ();
    		files.add(f);
    		String value = p.getProperty(SettingConstants.MAC_ADDRESS);
    		if (value != null) {
//    			p.setProperty("LinkLocalAddress", createLinkLocalAddress(value));
    			String colonMAC = createMACAddressWithColons(value);
    			p.setProperty(SettingConstants.MAC_ADDRESS, colonMAC);
//    			p.setProperty(SettingConstants.MAC_ADDRESS + " Colon", colonMAC);
    		}
    		// PCPCSII-125  
    		// Create a colon verions of each MAC Address in the configuration files
    		value = p.getProperty(SettingConstants.CABLE_MODEM_MAC_ADDRESS);
    		if (value != null) {
    			String colonMAC = createMACAddressWithColons(value);
    			p.setProperty(SettingConstants.CABLE_MODEM_MAC_ADDRESS, colonMAC);
//    			p.setProperty(SettingConstants.CABLE_MODEM_MAC_ADDRESS + " Colon", colonMAC);
    		}
//    		String deviceType = p.getProperty(SettingConstants.DEVICE_TYPE);
//    		if (deviceType != null) {
//    			table.put(deviceType+"0", p);
//    			logger.debug(PC2LogCategory.PCSim2, subCat, 
//    					"Adding label=" + deviceType + "0 to system settings.");
//    		}
    		
    		table.put(SettingConstants.DUT, p);    		
    	}
    	catch (Exception e) {
    		
		}    	
	}
	
	private void readPlatformParameters(Properties p, Hashtable<String, Properties> table, File f) {
		try {
			files = new LinkedList <File> ();
			files.add(f);
			String d = f.getParent();
			Enumeration<Object> e = p.keys();
			while(e.hasMoreElements()) {
				String key = (String)e.nextElement();
				if (key.startsWith(SettingConstants.UE) ||
						key.startsWith(SettingConstants.PCSCF) ||
						key.startsWith(SettingConstants.SCSCF)) {
					String propFile = p.getProperty(key);
//					if (propFile.endsWith(".properties")) {
						Properties ne = new Properties();
						File temp = new File(d + File.separator + propFile);
						files.add(temp);
						ne.load(new FileInputStream(d + File.separator + propFile));
						String label = ne.getProperty(SettingConstants.NE);
						if (label != null) {
							if (label.startsWith(SettingConstants.UE)) {
								String sim = p.getProperty(SettingConstants.SIMULATED);
								if (sim != null &&
										(sim.equalsIgnoreCase("false") ||
												sim.equalsIgnoreCase("no")||
												sim.equalsIgnoreCase("disable"))) {
									String pui = p.getProperty(SettingConstants.PUI);
									String pui2 = p.getProperty(SettingConstants.PUI2);
									if (pui != null) {
										table.put(pui, p);
										logger.debug(PC2LogCategory.PCSim2, subCat, 
												"Adding label=" + pui + " to system settings.");
									}
									if (pui2 != null && !pui2.equals(pui)) {
										table.put(pui2, p);
										logger.debug(PC2LogCategory.PCSim2, subCat, 
												"Adding label=" + pui2 + " to system settings.");
									}
								}
							}
							
							table.put(label,  ne);
						}
//					}
				}
			}
			table.put(SettingConstants.PLATFORM, p);
		} 
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	protected void populateToolTips(Hashtable<String, Properties> table) {
		
	}
   	
   	protected void saveAction(ActionEvent ev) {
   		if(ui != null) {
   			ui.write();
   		}
   	}
   	
   	private void saveAsAction(ActionEvent ev) {
   		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(history.lastBatchDirectory);
		int option = chooser.showSaveDialog(PCMenuBar.this);
		if (option == JFileChooser.APPROVE_OPTION) {
			File sf = chooser.getSelectedFile();
			if (sf != null) {
				ui.saveAsFile(sf);
			}
		}
   		history.lastBatchDirectory = chooser.getCurrentDirectory();
   		PCHistoryParser.writeHistoryFile();
   	}
}
