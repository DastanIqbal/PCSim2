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

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class PCTable extends JTable {
	
	static final long serialVersionUID = 1;
	protected static DefaultTableModel propTableModel = null;
	protected DefaultTableColumnModel propColModel = null;
	private static final String CONFIG_FILE = ".." + File.separator + "config" + File.separator + "tools" + File.separator + "Config.properties";

	private Hashtable<String, NetworkElement> configTable = null;
	
	private String currentLabel = null;
	
	public PCTable() {
		String pheader[] = new String[] {"Tag", "Value"};		

		propTableModel = (DefaultTableModel)getModel();
		propTableModel.setDataVector(null,pheader);
		propTableModel.setColumnCount(2);
		propTableModel.setRowCount(0);

		propColModel = (DefaultTableColumnModel)getColumnModel();
		propColModel.setColumnSelectionAllowed(false);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setCellSelectionEnabled(false);
		setRowSelectionAllowed(false);
		setFocusable(false);
		
		// Size the columns
		int tagSize = 200;
		int valueSize = 200;
		
		TableColumn tagCol = propColModel.getColumn(0);
		tagCol.setPreferredWidth(tagSize);
		TableColumn valueCol = propColModel.getColumn(1);
		valueCol.setPreferredWidth(valueSize);
		
	}	
	
	protected void addTableElement(NetworkElement ne) {
		if(ne != null) {
			propTableModel.setRowCount(0);
			SettingTV table = configTable.get(ne.getLabel()).getSettingsTV();			
			Enumeration <Object> tags = table.getTags();
			ArrayList<String> list = new ArrayList<String> ();
			while(tags.hasMoreElements()) {
				String tag = (String) tags.nextElement();
				list.add(tag);
			}
			Collections.sort(list, new Comparator<String>() {
		         public int compare(String o1, String o2) {
		             return Collator.getInstance().compare(o1, o2);
		         }
			});
			ListIterator<String> iter = list.listIterator();
			while(iter.hasNext()) {
				String tag = iter.next();
					propTableModel.addRow(new Object[] {tag, table.getValue(tag)});
			}
		}
	}
	
	protected DefaultTableColumnModel getTableColumn() {
		return this.propColModel;
	}
	
	protected NetworkElement getTableElement(String label) {
		if(configTable != null) {
			return configTable.get(label);
		}
		return null;
	}
	
	protected DefaultTableModel getTableModel() {
		return this.propTableModel;
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	
	protected String getToolTip(String label, String tag) {
		String result = null;
		String temp = label;
		if(label.startsWith("UE") || label.startsWith("PSCSF") || label.startsWith("SCSCF")) {
			temp = label.substring(0, label.length()-1);
		}
		if(configTable != null) {
			NetworkElement ne = configTable.get(temp);
			if(ne != null) {
				result = ne.getSettingsTV().getToolTip(tag);
			}
		}
		return result;
		
	}
	
	protected void loadConfiguration() {
		Properties p = new Properties();
		configTable = new Hashtable<String, NetworkElement> ();
		String tag = null;
		String val = null;
		String tip = null;
		
		try {
			p.load(new FileInputStream(CONFIG_FILE));
			Enumeration <Object> keys = p.keys();
			if(keys != null && keys.hasMoreElements()) {
				while(keys.hasMoreElements()) {
					Object key = keys.nextElement();
					if(key instanceof String) {
						String k = (String) key;
						String value = p.getProperty(k);
						if(value != null) {
							StringTokenizer token = new StringTokenizer (k, ".");
							if(token.countTokens() == 2) {
								String label = token.nextToken();
								tag = token.nextToken();
								NetworkElement ne = configTable.get(label);    
								if(ne == null) {
									ne = new NetworkElement(label, null);
									configTable.put(label, ne);
								}
								token = new StringTokenizer(value, "|");
								if(token.countTokens() == 2) {
									val = token.nextToken();
									tip = token.nextToken();
									ne.addSetting(tag, val, tip);
								}		
								else {
									System.out.println("Bad Value" + token);
								}
							}
							else {
								System.out.println("Bad Label" + token);
							} 
						}
					}
				}
				// Seperate DUT and DUT Prov
				NetworkElement ne = configTable.get("DUT");
				NetworkElement prov = configTable.get("DUT Provisioning");
				SettingTV setting = ne.getSettingsTV();
				SettingTV newSetting = prov.getSettingsTV();
				Enumeration <Object> e = setting.getTags();
				while(e.hasMoreElements()) {
					tag = (String) e.nextElement();
					String value = setting.getValue(tag);
					tip = setting.getToolTip(tag);
					newSetting.addSetting(tag, value, tip);
				}
			}
		}
		catch (Exception e) {
	
		}	
	}	
	
	protected void populateTable (String label) {
		this.currentLabel = label;
		
		// Add DUT element
		if(currentLabel.equals(PCMenuBar.newItems[0])) {
			NetworkElement ne = configTable.get("DUT");
			if(ne != null) {
				addTableElement(ne);
			}
		}
		// Add DUT Provisioning element
		else if(currentLabel.equals(PCMenuBar.newItems[1])) {
			NetworkElement ne = configTable.get("DUT");
			NetworkElement ne2 = configTable.get("DUT Provisioning");
			if(ne != null && ne2 != null) {
				addTableElement(ne);
				addTableElement(ne2);
			}
		}
		// Add Platform element
		else if(currentLabel.equals(PCMenuBar.newItems[2])) {
			NetworkElement ne = configTable.get("Platform");
			if(ne != null) {
				addTableElement(ne);
//				propFiles = new Hashtable<String, NetworkElement> ();
			}
		}
		// Add PCSCF element
		else if(currentLabel.equals(PCConfigUI.platItems[0])) {
			NetworkElement ne = configTable.get("PCSCF");
			if(ne != null) {
				addTableElement(ne);
			}
		}
		// Add SCSCF element
		else if(currentLabel.equals(PCConfigUI.platItems[1])) {
			NetworkElement ne = configTable.get("SCSCF");
			if(ne != null) {
				addTableElement(ne);
			}
		}
		// Add UE element
		else if(currentLabel.equals(PCConfigUI.platItems[2])) {
			NetworkElement ne = configTable.get("UE");
			if(ne != null) {
				addTableElement(ne);
			}
		}
	}
	
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if(c instanceof JComponent) {
        		JComponent jc = (JComponent) c;

        		String tag = getValueAt(row, 0).toString(); // Get tag values
        		String tip = "";
        		NetworkElement ne = configTable.get(currentLabel);
        		if(ne != null) {
        			tip = ne.getSettingsTV().getToolTip(tag);
        			if(tip == null && currentLabel.equals(PCMenuBar.newItems[1])) {
        				ne = configTable.get("DUT");
        				tip = ne.getSettingsTV().getToolTip(tag);
        			}
        		}
        		if(tip == null) {
        			tip = "";
        		}
        		jc.setToolTipText(tip);
        	}
        return c;
    }
}
