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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.parser.PC2XMLException;

public class HistoryParser extends DefaultHandler{

	/**
	 * The XML document reader and interface to the parser.
	 */
	private XMLReader xr;
	
	/**
	 * The locator class maintains the current location within
	 * the XML document for error reporting purposes.
	 */
	private Locator l;
	
	/**
	 * The console and log file interface.
	 */
	private LogAPI logger = null;
	
	/**
	 * A place holder for the current tag being parsed.
	 */
	private String curTag = null;
	
	/**
	 * The current history we are creating from the .history file.
	 */
	private static History history = null;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "History";
	
	/**
	 * The tags used in the file
	 */
	static final public String BATCH = "batch";
	static final public String DCF = "dcf";
	static final public String DIR = "dir";
	static final public String FILE = "file";
	static final public String HISTORY = "history"; 
	static final public String PCF = "pcf";
	static final public String PCSIM2 = "pcsim2";
	static final public String SCRIPTS = "scripts";
	static final public String VERSION = "version";
	
	public HistoryParser() {
		super();
		logger = LogAPI.getInstance(); 
	}
	
	public History parse() throws SAXException, IOException {
		xr = XMLReaderFactory.createXMLReader();
		if (xr != null) {
			xr.setContentHandler(this);
			if (History.HISTORY_FILE.exists()) {
				FileReader reader = new FileReader(History.HISTORY_FILE);
				xr.parse(new InputSource(reader));
			}
			else {
				history = History.getInstance();
				writeHistoryFile();
			}
			return history;
		}
		else 
			throw new SAXException("XMLReader did not get created successfully.");
	}
	
	/**
	 * A method to set the document locator for error reporting.
	 */
	@Override
	public void setDocumentLocator(Locator l) {
	    this.l = l;
	} 
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called by the parser upon initiating parsing allowing
	 * the application to create any variables or infrastructure it
	 * may need. The HistoryParser simply logs that it is starting and 
	 * creates the final History container.
	 */
	@Override
	public void startDocument() throws SAXException {
		logger.info(PC2LogCategory.UI, subCat, "HistoryParser - Starting to parse " + History.HISTORY_FILE);
		
	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called by the parser has completed parsing the document.
	 * This method verifies mandatory elements occur within the 
	 * document before return control back to the invoking class.
	 */
	@Override
	public void endDocument() throws SAXException {

	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called at the start of each element and is were the 
	 * HistoryParser begins to separate the various tags into their 
	 * respective objects. Validation of each mandatory attribute
	 * and children elements begins in this method.
	 * 
	 * (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
		
		logger.trace(PC2LogCategory.UI, subCat, "starting element (" + qName + ")");
		logger.trace(PC2LogCategory.UI, subCat, "   number of attributes: " + atts.getLength());
		curTag = qName;
		boolean unrecognizedTag = false;
		int count = atts.getLength();
		if (qName.equals(PCSIM2)) {
			for (int i = 0; i < count; i++) {
				String attr = atts.getLocalName(i);
				if (attr.equals(VERSION)) {
					// Add any versioning operations that might be needed in the future here
					// for this initial release though, there is no operation as the version
					// is assigned automatically to the History class.
				}
				else {
					logger.trace(PC2LogCategory.UI, subCat, 
							"The attribute[" + attr + "] is an unexpected attribute for the " 
							+ PCSIM2 + " tag. It has been ignored.");
				}
			}
			if (history == null)
				history = History.getInstance();
			return;
		}
		else {
			
			switch (qName.charAt(0)) {
			
			case 'b':
				if (qName.equals(BATCH)) {
					for (int i = 0; i < count; i++) {
						String attr = atts.getLocalName(i);

						if (attr.equals(DIR)) {
							File f = new File(atts.getValue(i));
							if (f.isDirectory() && f.canRead())
								history.lastBatchDirectory = f;
						}
						else {
							logger.trace(PC2LogCategory.UI, subCat, 
									"The attribute[" + attr + "] is an unexpected attribute for the " 
									+ BATCH + " tag. It has been ignored.");
						}
					}
				}
				else 
					unrecognizedTag = true;
				break;
			
			case 'd':
				if (qName.equals(DCF)) {
					for (int i = 0; i < count; i++) {
						String attr = atts.getLocalName(i);

						if (attr.equals(DIR)) {
							File f = new File(atts.getValue(i));
							if (f.isDirectory() && f.canRead())
								history.lastDCFDirectory = f;
						}
						else {
							logger.trace(PC2LogCategory.UI, subCat, 
									"The attribute[" + attr + "] is an unexpected attribute for the " 
									+ DCF + " tag. It has been ignored.");
						}
					}
				}
				else 
					unrecognizedTag = true;
				break;
			case 'h':
				if (qName.equals(HISTORY)) {
					for (int i = 0; i < count; i++) {
						String attr = atts.getLocalName(i);

						if (attr.equals(FILE)) {
							String value = atts.getValue(i);
							File f = new File(value);
							if (f.isFile() && f.canRead())
								history.addHistoryFile(f);
						}
						else {
							logger.trace(PC2LogCategory.UI, subCat, 
									"The attribute[" + attr + "] is an unexpected attribute for the " 
									+ DCF + " tag. It has been ignored.");
						}
					}
				}
				else 
					unrecognizedTag = true;
				break;
			case 'p':
				if (qName.equals(PCF)) {
					for (int i = 0; i < count; i++) {
						String attr = atts.getLocalName(i);

						if (attr.equals(DIR)) {
							File f = new File(atts.getValue(i));
							if (f.isDirectory() && f.canRead())
								history.lastPCFDirectory = f;
						}
						else {
							logger.trace(PC2LogCategory.UI, subCat, 
									"The attribute[" + attr + "] is an unexpected attribute for the " 
									+ PCF + " tag. It has been ignored.");
						}
					}
				}
				else 
					unrecognizedTag = true;
				break;

			case 's':
				if (qName.equals(SCRIPTS)) {
					for (int i = 0; i < count; i++) {
						String attr = atts.getLocalName(i);

						if (attr.equals(DIR)) {
							File f = new File(atts.getValue(i));
							if (f.isDirectory() && f.canRead())
								history.lastScriptsDirectory = f;
						}
						else {
							logger.trace(PC2LogCategory.UI, subCat, 
									"The attribute[" + attr + "] is an unexpected attribute for the " 
									+ SCRIPTS + " tag. It has been ignored.");
						}
					}
				}
				else 
					unrecognizedTag = true;
				break;
			default :
				throw new PC2XMLException(History.HISTORY_FILE.getName(), 
						"Encountered unexpected tag(" + qName + ") element.", l);
			}
		}

		if (unrecognizedTag)
			throw new PC2XMLException(History.HISTORY_FILE.getName(),
					"Encountered unexpected tag(" + qName + ") element.", l);
	
	}
	
	@Override
	public void endElement(String uri, String name, String qName) throws SAXException {
		logger.trace(PC2LogCategory.UI, subCat, "ending element (" + qName + ")");

		curTag = null;
	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called for the charaters that lie between tags. Since the
	 * parse doesn't guarantee delivery of all of the characters between
	 * the start and end tags, the HistoryParser must be prepared to receive
	 * more than one callout before receiving notification of the ending
	 * tag.
	 * 
	 *  (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		String data =  new String(ch,start,length);
		logger.trace(PC2LogCategory.Parser, subCat, 
				curTag + " - CDATA: " + length + " characters. Data=[" + data + "].");
		
		logger.trace(PC2LogCategory.Parser, subCat, 
				"Ignoring characters(" + data + ").");
	
	}
	
	static protected void writeHistoryFile() {
		if ((History.HISTORY_FILE.isFile()  &&
				History.HISTORY_FILE.canWrite()) ||
				(!History.HISTORY_FILE.exists())){
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(History.HISTORY_FILE, false));
				out.write("<?xml version=\"1.0\"?>\n");
				out.write("<pcsim2 version=\"" + History.PLATFORM_VERSION + "\">\n");
				out.write("\t<pcf dir=\"" + history.lastPCFDirectory.getAbsolutePath() + "\"/>\n");
				out.write("\t<dcf dir=\""+ history.lastDCFDirectory.getAbsolutePath() + "\"/>\n");
				out.write("\t<scripts dir=\"" + history.lastScriptsDirectory.getAbsolutePath() + "\"/>\n");
				out.write("\t<batch dir=\"" + history.lastBatchDirectory.getAbsolutePath() + "\"/>\n");
				
				
				for (int i=0; i< History.MAX_NUM_HISTORY_FILES; i++) {
					if (history.histFiles[i] != null) {
						out.write("\t<history file=\"" + history.histFiles[i].getAbsolutePath() + "\"/>\n"); 
					}
				}
				out.write("</pcsim2>");
				out.close();
			}
			catch (IOException e) {
				if (PC2UI.logger != null) {
					PC2UI.logger.error(PC2LogCategory.UI, PC2UI.subCat, 
							"PCSim2 encountered an error while trying to write the history file information.\n" 
							+ e.getMessage());
				}
			}
		}
	}
	
}
