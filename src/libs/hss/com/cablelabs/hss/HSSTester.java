package com.cablelabs.hss;

//import java.util.*;
import java.io.*;

public class HSSTester {

	public String directory = "../config/hss_db";
	

	public HSSData db = new HSSData();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HSSTester t = new HSSTester();
		File f = new File(t.directory); 
		System.out.println(f);
		
		if (f.exists() && f.isDirectory() && f.canRead()) {
			HSSParser parser = new HSSParser();
			try {
				File[] files = f.listFiles();
				for (int i =0; i < files.length; i++) {
					Subscriber s = parser.parse(files[i].getName());
					t.db.addSubscriber(s.getPrivateUserId(), s);
					if (s != null) {
						String xml = s.encode();
						if (xml != null)
							System.out.println(xml);
					}
				}
			}
			catch (Exception ex) {
				System.err.println("ERROR, " + ex.getMessage() + "\n" + ex.getStackTrace());
			}
		}
	}

}
