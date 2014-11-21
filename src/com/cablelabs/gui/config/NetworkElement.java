package com.cablelabs.gui.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class NetworkElement implements Cloneable{

	private String label = null;
	private SettingTV stv = null;
	private File f = null;
	
	public NetworkElement(String label, File f) {
		this.label = label;
		this.f = f;
		this.stv = new SettingTV();
	}

	protected void addSetting(String tag, String value, String toolTip) {
		this.stv.addSetting(tag, value, toolTip);
	}
	
	protected void addSetting(String tag, String value) {
		this.stv.addSetting(tag, value);
	}
	
	public Object clone() throws CloneNotSupportedException {
		NetworkElement retval = new NetworkElement(this.label, this.f);
		if (retval != null) {
			if (this.stv != null) 
				retval.stv = (SettingTV) this.stv.clone();
		}	
		return retval;
	}
	
	protected void setFile(File f) {
		this.f = f;
	}
	
	protected File getFile() {
		return f;
	}
	
	protected String getLabel() {
		return label;
	}
	
	protected SettingTV getSettingsTV() {
		return this.stv;
	}
	
	protected void setLabel(String label) {
		this.label = label;
	}
	
	public String toString() {
		return label + "=" + stv;
	}
	
	public void write() {
		if(stv != null) {
			FileOutputStream output;
			try {
				output = new FileOutputStream(f);	
				stv.write(output, label);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	
}
