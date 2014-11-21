package com.cablelabs.old_diagram;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class PC2FileFilter extends FileFilter {

	private String ext = null;
	private String description = null;

	public PC2FileFilter(String ext, String descrip) {
		super();
		this.ext = ext;
		this.description = descrip;
	}
	@Override
	public boolean accept(File f) {
		 if (f.isDirectory( ) && f.canRead()) { return true; }

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
