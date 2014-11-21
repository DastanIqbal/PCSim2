package com.cablelabs.diagram;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class LogFileFilter extends FileFilter {

	private String description = null;
	private String ext = null;

	public LogFileFilter(String ext, String descrip) {
		super();
		this.ext = ext;
		this.description = descrip;
	}
	@Override
	public boolean accept(File f) {
		if (f.isFile() && f.canRead()) {
			String name = f.getName();
			if (name.endsWith(ext)) {
					return true;
			}
		}
		return false;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

}
