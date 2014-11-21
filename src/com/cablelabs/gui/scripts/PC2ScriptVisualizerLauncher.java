/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/

package com.cablelabs.gui.scripts;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.parser.TSDocument;
import com.cablelabs.parser.TSParser;

/**
 * This is a common place to deal with launching the PC2ScriptVisualizer and
 * cleanly deal with any resulting exceptions from not having the needed libraries.
 *
 * @author rvail
 *
 */
public class PC2ScriptVisualizerLauncher {

    /**
     * The basic logger if we have an issue with the parser
     */
    private static LogAPI logger = LogAPI.getInstance();

    /**
     * The subcategory to use when logging
     *
     */
    private static final String subCat = "ScriptVisualizerLauncher";

    /**
    * The error message to give when unable to find the needed libaries to open the visulizer.
    */
    private static final String UNABLE_TO_FIND_JUNG_ERROR = "Unable to find jung(vers. 2.2.0.1) libaries in lib/jung/ can't launch visulizer.";
    
    /**
     * This will open the passed in document in a window for the user.
     * It will cleanly deal with any exceptions just NoClassDefFoundErrors
     * when the needed libaries can not be found.
     *
     * @param doc the document to load in the visualizer
     * @param quitOnExit if the app should quit when closing the visualizer.
     */
    public static void openVisulizer(TSDocument doc, boolean quitOnExit) {
        try {
            logger.debug(PC2LogCategory.Reader, subCat, "Opening parsed script in visualizer...");
            
            ClassLoader classLoader = PC2ScriptVisualizerLauncher.class.getClassLoader();
            Class<?> scImplClass = classLoader.loadClass("com.cablelabs.gui.scripts.visualizer.PC2ScriptVisualizer");            
            PC2ScriptVisualizerI sv = (PC2ScriptVisualizerI)scImplClass.newInstance();
            sv.setDocument(doc);
            sv.setShouldExitOnClose(quitOnExit);
            sv.show();
        }
        catch (NoClassDefFoundError e) {
            // Jung is not installed
            logger.error(PC2LogCategory.UI, subCat, UNABLE_TO_FIND_JUNG_ERROR);
        }
        catch (ClassNotFoundException e) {
            // Jung is not installed
            logger.error(PC2LogCategory.UI, subCat, UNABLE_TO_FIND_JUNG_ERROR);
        }
        catch (Exception e) {
            logger.error(PC2LogCategory.UI, subCat, "Unable to show Script Visualizer: " + e.getMessage());
        }
    }

    /**
     * Helper method to parse a file into a TSDocument and open it in a visualizer.
     *
     * @param filePath
     */
    public static void showFile(String filePath) {
        TSParser parser = new TSParser(false);
        try {
            TSDocument doc = parser.parse(filePath);
            openVisulizer(doc, false);
        }
        catch (Exception e) {
            logger.error(PC2LogCategory.UI, subCat, "Unable to launch visulizer: " + e.getMessage());
            return;
        }
    }

}
