/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.parser;


import java.io.File;
import java.util.LinkedList;
import java.util.ListIterator;

import org.xml.sax.SAXParseException;

import com.cablelabs.gui.scripts.PC2ScriptVisualizerLauncher;
import com.cablelabs.log.LogAPIConfig;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

/**
 * The application class for parsing a PC 2.0 Test Script
 * XML Document. It provides a command line executable for
 * for the TSParser so that documents can be parsed and
 * validated at creation.
 *
 * The application requires 1 argument fully-qualified name
 * of the Test Script document to parse. The results of this
 * application is a TSDocument containing the resulting objects
 * for introduction into the Platform engine.
 *
 * @author ghassler
 *
 */
public class TSReader {

	/**
	 * List of Test Script files to use execute this batch of
	 * tests.
	 */
	private LinkedList<String> testScriptFiles = new LinkedList<String>();

	/**
	 * The basic logger if we have an issue with the parser
	 */
	private static final LogAPI logger;

	/**
	 * The subcategory to use when logging
	 *
	 */
	private static String subCat = "";

	/**
	 * If true the parsed file will be open in the script visualizer.
	 */
	private boolean shouldOpenInVisualizer = false;

	static {
	    LogAPIConfig config = new LogAPIConfig()
	                .setCategoryClass(PC2LogCategory.class)
	                .setLogPrefix("Parser");
	    
	    logger = LogAPI.getInstance(config, true);
	}
	
	/**
	 * Public constructor.
	 */
	public TSReader() {
	}

	/**
	 * Provides global access to the application logger.
	 * @return logger - the applications logger.
	 */
	public static LogAPI getLogger() {
		return logger;
	}

	private static void showHelp() {
		String msg = "java -jar pc2_parser-0.1.jar file | options  \n\n"
			+ "\tfile:\n"
			+ "\twhere file is the name of a single test script file\n"
			+ "\toptions:\n"
			+ "\t-t <test script file(s)>\n\t\t"
			+ "where <test script file(s)> is space separated list of file(s).\n"
			+ "\t-dir <test script directory(s)>\n\t\t"
			+ "where <test script directory(s)> is space separated list of directories\n\t\t"
			+ "of test script files to parse.\n"
			+ "\t-h help\n\n"
			+ "\t-v visualize after parsing (only valid if parsing one test script)"
			+ "NOTE: All files need to be include the absolute or relative-path\n"
			+ "       as part of the file name.";
		logger.info(PC2LogCategory.Reader, subCat, msg);
	}

	private static boolean isArgOption(String arg) {
		if (arg.equals("-t") || arg.equals("-dir") || arg.equals("-v")) {
			return true;
		}
		return false;
	}

	private static void logPackage() {
//	  Package [] pa = Package.getPackages();
//	  for (int i = 0; i < pa.length; i++) {
//		  logger.info("Package Name[" + i + "]=" + pa[i].getName());
//	  }
      Package p = Package.getPackage("com.cablelabs.parser");
      if (p == null) {
        logger.info(PC2LogCategory.Reader, subCat,
        		"com.cablelabs.parser not loaded");
        return;
      }
      logger.info(PC2LogCategory.Reader, subCat,
    		  "com.cablelabs.parser version " + p.getSpecificationVersion() +
        " build-" +  p.getImplementationVersion());
    }



	private boolean processArgs(String [] args) {
		boolean valid = true;
		LinkedList<String> invalidArgs = new LinkedList<String>();

		// Process the arguments
		logPackage();

		if (args.length > 1) {
			logger.trace(PC2LogCategory.Reader, subCat, "Beginning to process application arguments.");
			int count = args.length;
			for (int i=0; i < count; i++) {
				logger.trace(PC2LogCategory.Reader, subCat, "Argument["+i+"]="+args[i]);

				if (args[i].equals("-t")) {
					do {
						String fileName = args[++i];
						logger.trace(PC2LogCategory.Reader, subCat, "Argument["+i+"]="+fileName);

						String reason = validateFile(fileName);
						if (reason == null)
							testScriptFiles.add(fileName);
						else {
							invalidArgs.add("-t " + fileName + reason);
						}
					}	while ((i+1) < count && !isArgOption(args[i+1]));
				}
				else if (args[i].equals("-h")) {
					 showHelp();
					 valid = false;
				}
				else if (args[i].equals("-dir")) {
					do {
						String fileName = args[++i];
						logger.trace(PC2LogCategory.Reader, subCat, "Argument["+i+"]="+fileName);
						String reason = validateDir(fileName);
						if (reason != null) {
							logger.error(PC2LogCategory.Reader, subCat, "Encountered error when validating -dir option.");
						}
					}	while ((i+1) < count && !isArgOption(args[i+1]));
				}
				else if (args[i].equals("-v")) {
				    shouldOpenInVisualizer = true;
				}
				else
					invalidArgs.add(args[i]);
			}

		}
		else if (args.length == 1) {
			String fileName = args[0];
			String reason = validateFile(fileName);
			if (reason == null)
				testScriptFiles.add(fileName);
			else {
				invalidArgs.add(" " + fileName + reason);
			}
		}

		if (shouldOpenInVisualizer && testScriptFiles.size() > 1) {
		    invalidArgs.add("-v (only valid if parsing one test script)");
		}

		if (invalidArgs.size() > 0) {
			valid = false;
			logger.fatal(PC2LogCategory.Reader, subCat, "The following arguments are invalid:");

			for (int i = 0; i < invalidArgs.size(); i++)
				logger.fatal(PC2LogCategory.Reader, subCat, "\t" + invalidArgs.get(i));

			showHelp();
		}
		else
			logger.trace(PC2LogCategory.Reader, subCat,
					"Processing of application arguments completed successfully.");
		return valid;
	}

	private String validateDir(String dirName) {
		String reason = null;
		File f = new File(dirName);
		if (!f.exists())
			reason = " does not exist.";
		else if (!f.isDirectory())
			reason = " isn't a file.";
		else if (!f.canRead())
			reason = " can't be read.";
		if (reason == null) {
			File [] files = f.listFiles();
			for (int i = 0; i < files.length; i++) {
				String fileName = dirName + File.separator + files[i].getName();
				reason = validateFile(fileName);
				if (reason == null) {
					testScriptFiles.add(fileName);
					logger.info(PC2LogCategory.Reader, subCat,
							"Adding file="+ files[i].toString() );
				}
			}
		}
        return reason;
	}

	private static String validateFile(String fileName) {
		String reason = null;
		File f = new File(fileName);
		if (!f.exists())
			reason = " does not exist.";
		else if (!f.isFile())
			reason = " isn't a file.";
		else if (!f.canRead())
			reason = " can't be read.";

        return reason;
	}



	public void run() {
	    if (testScriptFiles.size() > 0) {

	        ListIterator<String> iter = testScriptFiles.listIterator();
	        while (iter.hasNext()) {
	            String fileName = iter.next();
	            File f = new File (fileName);
	            if (f != null) {
	                logger.info(PC2LogCategory.Reader, subCat, "Using input document " + fileName);

	                TSParser tsp = new TSParser(true);
	                try {
	                    TSDocument doc = tsp.parse(fileName);
	                    logger.info(PC2LogCategory.Reader, subCat,
	                            "Document " + doc.getName() + " has successfully been parsed!");

	                    if (shouldOpenInVisualizer)
	                        PC2ScriptVisualizerLauncher.openVisulizer(doc, true);
	                }
	                catch (PC2XMLException pe){
	                    String err = "\n** Parsing error in file \n    " + pe.getFileName()
	                            + " at line " + pe.getLineNumber();
	                    if (pe.getSystemId() != null) {
	                        err += ", uri " + pe.getSystemId();
	                    }
	                    if (pe.getPublicId() != null) {
	                        err +=  ", public " + pe.getPublicId();
	                    }
	                    err += "\n";

	                    logger.fatal(PC2LogCategory.Reader, subCat, err, pe);
	                }
	                catch (SAXParseException spe) {
	                    String err = "\n** Parsing error in file \n    " + fileName +
	                            " at line " + spe.getLineNumber();
	                    if (spe.getSystemId() != null) {
	                        err += ", uri " + spe.getSystemId();
	                    }
	                    if (spe.getPublicId() != null) {
	                        err +=  ", public " + spe.getPublicId();
	                    }
	                    err += "\n";

	                    logger.fatal(PC2LogCategory.Reader, subCat, err, spe);
	                }
	                catch (Exception e) {
	                    e.printStackTrace();

	                }
	            }
	        }


	    }
	}
	/**
	 * The main function for the Test Script Reader program.
	 * This program accepts a PC 2.0 XML Test Script Document
	 * as an argument, parses the file and reports any anomalies
	 * it detects with the syntax. It is important to note that
	 * due to the nature of the XML definition and the need to
	 * support negative testing, the parser can't detect all
	 * invalid data that may exist within the document. It can
	 * only validate that the tags and required arguments for
	 * those tags are properly formatted.
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
//		if (logger == null) {
//	      Logger log = Logger.getLogger(TSReader.class);
//	      logger = log;
//		}
		// Log4j
//		String logFileName = "./config/LogConfig.txt";
//		File l = new File(logFileName);
//		if (l.exists())
//			PropertyConfigurator.configure(logFileName);
//		else
//			BasicConfigurator.configure();

		TSReader reader = new TSReader();
		if (reader.processArgs(args))
		    reader.run();
	}
}
