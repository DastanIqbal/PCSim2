package com.cablelabs.gui.config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.ListIterator;
import java.util.Properties;

public class SettingTV implements Cloneable {
	
	private Properties values = new Properties();
	private Properties toolTips = new Properties();
	
	public SettingTV() {

	}
	
	public void addSetting(String tag, String value, String toolTip) {
		values.put(tag, value);
		if(toolTip != null) {
			toolTips.put(tag, toolTip);
		}
		else {
			toolTips.put(tag, "");
		}
	}
	
	public void addSetting(String tag, String value) {
		values.put(tag, value);
	}
	
	public Object clone() throws CloneNotSupportedException {
		SettingTV retval = new SettingTV();
		if (retval != null ) {
			if (this.values != null) 
				retval.values = (Properties) this.values.clone();
			if (this.toolTips != null) 
				retval.toolTips = (Properties) this.toolTips.clone();
		}	
		return retval;
	}
	
	protected Enumeration<Object> getTags() {
		return values.keys();
	}
	
	protected String getToolTip(String tag) {
		return toolTips.getProperty(tag);
	}
	
	protected Properties getToolTips() {
		return toolTips;
	}
	
	protected String getValue(String tag) {
		return values.getProperty(tag);
	}
	
	protected Properties getValues() {
		return values;
	}
	
	public String toString() {
		return values + "\n" + toolTips;
	}	
	
	protected void write(FileOutputStream output, String label) {
		Enumeration <Object> e = values.keys();
		String buff = new String();
		ArrayList<String> list = new ArrayList<String> ();
		while(e.hasMoreElements()) {
			String tag = (String) e.nextElement();
			list.add(tag);
		}
		Collections.sort(list, new Comparator<String>() {
	         public int compare(String o1, String o2) {
	             return Collator.getInstance().compare(o1, o2);
	         }
	     });
		ListIterator<String> iter = list.listIterator();

		while(iter.hasNext()) {
			String key = iter.next();
//			String key = (String)e.nextElement();
			String value = values.getProperty(key);
			
			String tag = key.replaceAll(" ", "\\\\ ");
			buff += tag + "=" + value + "\n";		
		}

		try {
			if(buff != null && buff.length() > 0) {
				output.write(buff.getBytes());
			}
		} 
		catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
