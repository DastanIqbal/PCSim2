/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.capture;

import java.io.Writer;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;

public class IOReader extends Thread {
	private InputStream is = null;
	private Writer w = null;
	private Writer w2 = null;
	boolean breakTh = false;

	private BufferedWriter bw;
	private BufferedWriter bw2;
	private BufferedReader br;
	public IOReader(String name, InputStream is, Writer w, Writer w2) {
		super(name);
		this.is = is;
		this.w = w;
		this.w2 = w2;
	}

	public void run() {
		bw = null;
		if(w != null) {
			bw = new BufferedWriter(w);
		}

		bw2 = null;
		if(w2 != null) {
			bw2 = new BufferedWriter(w2);
		}

		br = new BufferedReader(new InputStreamReader(is));
		while(!breakTh) {
			try {
				String line=null;
				if(is.available() <= 0) {
					try {
						Thread.sleep(100);
					} catch(InterruptedException ex) {
					}
				}
				if(breakTh) {
					break;
				}
				while (is.available() > 0 && (line=br.readLine()) != null) {
					if (bw != null) {
						bw.write(line);
						bw.newLine();
					}
					if (bw2 != null) {
						bw2.write(line);
						bw2.newLine();
					}
				}
				if (bw != null) {
					bw.flush();
				}

				if (bw2 != null) {
					bw2.flush();
				}

			} catch(Exception ex) {
				Wireshark.error("Error while reading from std out/err ", ex);
			}
		}
		try {
			finishReadingIfPending();
		} catch(Exception ex) {
			Wireshark.error("Error while completely reading buffer", ex);
		}
		synchronized(this) {
			this.notifyAll();
		}
	}

	synchronized void waitTillBreak() {
		try {
			this.wait();
		} catch(InterruptedException ex) {
		}
	}

	void finishReadingIfPending() throws IOException {
		try {
			String line = null;
			if(!br.ready()) {
				return;
			}
		
			while((line=br.readLine()) != null) {
				if (bw != null) {
					bw.write(line);
					bw.newLine();
				}
				if (bw2 != null) {
					bw2.write(line);
					bw2.newLine();
				}
			}
		} finally {
			if (bw != null) {
				bw.flush();
			}
	
			if (bw2 != null) {
				bw2.flush();
			}
		}
	}

} // IOReader
