package com.cablelabs.gui.config;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;


public class ModifyTable {
	
	protected Hashtable<String, NetworkElement> table = null;

	private int pcscfCounter = 0;
	private int scscfCounter = 0;
	private int ueCounter = 0;
	private String label = null;
	
	public ModifyTable(String label, NetworkElement ne) {
		table = new Hashtable<String, NetworkElement> ();
		// Add DUT to table
		if (label.equals(PCMenuBar.newItems[0])) {
			updateDUT(ne);
			this.label = label;
			table.put(label, ne);
		}
		// Add DUT Prov to table
		else if (label.equals(PCMenuBar.newItems[1])) {
			updateDUTProv(ne);			
			this.label = label;
			table.put(label, ne);
		}
		// Add Platform to table
		else if (label.equals(PCMenuBar.newItems[2])) {
			updatePlatform(ne);
			this.label = label;
			table.put(label, ne);
		}
	}

	public void add(String label, NetworkElement ne) {
		// Add PCSCF to table
		if (label.equals(PCConfigUI.platItems[0])) {
			String newLabel = label + ++pcscfCounter; 
			updatePCSCF(ne, newLabel);
			table.put(newLabel, ne);
		}
		// Add SCSCF to table
		else if (label.equals(PCConfigUI.platItems[1])) {
			String newLabel = label + ++scscfCounter; 
			updateSCSCF(ne, newLabel);
			table.put(label + ++scscfCounter, ne);
		}
		// Add UE to table
		else if (label.equals(PCConfigUI.platItems[2])) {
			String newLabel = label + ++ueCounter; 
			updateUE(ne, newLabel);
			table.put(label + ++ueCounter, ne);
		}
	}
	
	private void updateDUT(NetworkElement ne) {

	}
	
	private void updateDUTProv(NetworkElement ne) {
		
	}
	
	public void updateFile(File file) {
		
		String path = file.getParentFile().getAbsolutePath();
		NetworkElement ne = table.get(label);
		if(ne != null) {
			ne.setFile(file);
			Enumeration<String> e = table.keys();
			while(e.hasMoreElements()) {
				String key = e.nextElement();
				ne = table.get(key);
				if(ne != null && !ne.getLabel().equals(label)) {
					String name = ne.getFile().getName();
					File f = new File(path + File.separator + name);
					ne.setFile(f);
				}
			}
		}
		write();
	}
	
	private void updatePCSCF(NetworkElement ne, String label) {
		ne.setLabel(label);
		NetworkElement platform = table.get("Platform");
		if(platform != null) {
			platform.addSetting(ne.getLabel(), ne.getFile().getName());
		}
	}
	
	private void updatePlatform(NetworkElement ne) {
		
	}

	private void updateSCSCF(NetworkElement ne, String label) {
		ne.setLabel(label);
		NetworkElement platform = table.get("Platform");
		if(platform != null) {
			platform.addSetting(ne.getLabel(), ne.getFile().getName());
		}
	}
	
	private void updateUE(NetworkElement ne, String label) {
		ne.setLabel(label);
		NetworkElement platform = table.get("Platform");
		if(platform != null) {
			platform.addSetting(ne.getLabel(), ne.getFile().getName());
		}
	}
	
	public void write() {
		// Getting DUT, DUT Provisioning, and Platform entry
		if(table.containsKey(PCMenuBar.newItems[0]) || 
				table.containsKey(PCMenuBar.newItems[1]) || 
				table.containsKey(PCMenuBar.newItems[2])) {
			Enumeration<String> e = table.keys();
			while(e.hasMoreElements()) {
				String key = e.nextElement();
				NetworkElement ne = table.get(key);
				ne.write();
			}
		}
	}
}
