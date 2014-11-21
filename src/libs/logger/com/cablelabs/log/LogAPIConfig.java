/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.log;

/**
 * A Container to hold all the configuration parameters for the LogAPI.
 * 
 * @author rvail
 *
 */
public class LogAPIConfig {
    
    /*
     * Possible features
     * 
     * -loading from a file
     * -dumping to a file/String
     * 
     */

    // Default values
    
    /**
     * The default LogCategory class for this app.
     */
    private Class<? extends LogCategory> DEFAULT_LOG_CATEGORY_CLASS = LogCategory.class;
    
    /**
     * The default log message pattern to use in the Console's appender.
     */
    public static final String DEFAULT_CONSOLE_PATTERN = "%-5p %m%n";
    
    /**
     * The default log message pattern to use in the application and test
     * log files.
     */
    public static final String DEFAULT_FILE_PATTERN = "%m"; // "%d %-5p %m - %F %L [%t]%n";
    
    /**
     * The default maximum size of a single application log file.
     */
    public static final String DEFAULT_MAX_FILE_SIZE = "2000KB";
    
    /**
     * The default relative path to the log directory
     */
    public static final String DEFAULT_LOG_DIRECTORY  = "../logs/";
    
    /**
     * The default behavior about creating the log directory if it does not exist.
     */
    public static final boolean DEFAULT_CREATE_LOG_DIR_IF_NOT_EXISTS = false;
    
    /**
     * The default file name prefix of the log file.
     */
    public static final String DEFAULT_LOG_PREFIX = "Log_";
    
    /**
     * The default file name suffix of the log file.
     */
    public static final String DEFAULT_LOG_SUFFIX = "";
    
    /**
     * The file name extension of the log file.
     */
    public static final String DEFAULT_LOG_EXTENSION = ".log";
     
    /**
     * The maximum number of log files to keep before rolling over.
     */
    public static final int DEFAULT_MAX_APP_LOGS = 10;
    
    /**
     * The path to the log level configuration file.
     */
    public static final String DEFAULT_LOG_LEVEL_CONFIG_PATH = "../config/LogConfig.txt";
     
    //************************************************************************************//
    // current values
    
    /**
     * A Reference to the LogCategory class for this app.
     */
    private Class<? extends LogCategory> logCatClass = DEFAULT_LOG_CATEGORY_CLASS;
    
    /**
     * The log message pattern to use in the Console's appender.
     */
    private String consolePattern = DEFAULT_CONSOLE_PATTERN;
    
    /**
     * The log message pattern to use in the application and test
     * log files.
     */
    private String filePattern = DEFAULT_FILE_PATTERN;
    
    /**
     * The maximum size of a single application log file.
     */
    private String maxFileSize = DEFAULT_MAX_FILE_SIZE;
    
    /**
     * The relative path to the log directory
     */
    private String logDirectory  = DEFAULT_LOG_DIRECTORY;
    
    /**
     * If true log directory will be created if it does not exist.
     */
    private boolean createLogDirIfNoExists = DEFAULT_CREATE_LOG_DIR_IF_NOT_EXISTS;
    
    /**
     * The file name prefix of the log file.
     */
    private String logPrefix = DEFAULT_LOG_PREFIX;
    
    /**
     * The file name suffix of the log file.
     */
    private String logSuffix = DEFAULT_LOG_SUFFIX;
    
    /**
     * The file name extension of the log file.
     */
    private String logExtension = DEFAULT_LOG_EXTENSION;
    
    /**
     * The maximum number of log files to keep before rolling over.
     */
    private int maxAppLogs = DEFAULT_MAX_APP_LOGS;
    
    /**
     * The path to the log level configuration file.
     */
    private String logLevelConfigPath = DEFAULT_LOG_LEVEL_CONFIG_PATH;
    
    //************************************************************************************//
    public LogAPIConfig() {
        
    }
    
    public Class<? extends LogCategory> getCategoryClass() {
        return logCatClass;
    }
    
    public LogAPIConfig setCategoryClass(Class<? extends LogCategory> logCatClass) {
        this.logCatClass = logCatClass;
        return this;
    }

    
    /**
     * @return the current consolePattern.
     */
    public String getConsolePattern() {
        return consolePattern;
    }

    
    /**
     * @param consolePattern the consolePattern to use.
     */
    public LogAPIConfig setConsolePattern(String consolePattern) {
        this.consolePattern = consolePattern;
        return this;
    }

    
    /**
     * @return the current filePattern.
     */
    public String getFilePattern() {
        return filePattern;
    }

    
    /**
     * @param filePattern the filePattern to use.
     */
    public LogAPIConfig setFilePattern(String filePattern) {
        this.filePattern = filePattern;
        return this;
    }

    
    /**
     * @return the current maxFileSize.
     */
    public String getMaxFileSize() {
        return maxFileSize;
    }

    
    /**
     * @param maxFileSize the maxFileSize to use.
     */
    public LogAPIConfig setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
        return this;
    }

    
    /**
     * @return the current logDirectory.
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    
    /**
     * @param logDirectory the logDirectory to use.
     */
    public LogAPIConfig setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
        
        // Ensure this always ends with a slash
        if (!this.logDirectory.endsWith("/")) {
            this.logDirectory += "/";
        }
        
        return this;
    }
    
    /**
     * @return the current createLogDirIfNoExists setting.
     */
    public boolean getCreateLogDirIfNoExists() {
        return createLogDirIfNoExists;
    }

    
    /**
     * @param createLogDirIfNoExists the behavior to use.
     */
    public LogAPIConfig setCreateLogDirIfNoExists(boolean createLogDirIfNoExists) {
        this.createLogDirIfNoExists = createLogDirIfNoExists;
        return this;
    }

    /**
     * @return the current logPrefix.
     */
    public String getLogPrefix() {
        return logPrefix;
    }
    
    /**
     * @param logPrefix the logPrefix to use.
     */
    public LogAPIConfig setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
        return this;
    }

    /**
     * @return the current logPrefix.
     */
    public String getLogSuffix() {
        return logSuffix;
    }
    
    /**
     * @param logSuffix the logSuffix to use.
     */
    public LogAPIConfig setLogSuffix(String logSuffix) {
        this.logSuffix = logSuffix;
        return this;
    }
    
    /**
     * @return the current logExtension.
     */
    public String getLogExtension() {
        return logExtension;
    }

    
    /**
     * @param logExtension the logExtension to use.
     */
    public LogAPIConfig setLogExtension(String logExtension) {
        this.logExtension = logExtension;
        return this;
    }

    
    /**
     * @return the current maxAppLogs.
     */
    public int getMaxAppLogs() {
        return maxAppLogs;
    }

    
    /**
     * @param maxAppLogs the maxAppLogs to use.
     */
    public LogAPIConfig setMaxAppLogs(int maxAppLogs) {
        this.maxAppLogs = maxAppLogs;
        return this;
    }

    
    /**
     * @return the current logLevelConfigPath.
     */
    public String getLogLevelConfigPath() {
        return logLevelConfigPath;
    }

    
    /**
     * @param logLevelConfigPath the logLevelConfigPath to use.
     */
    public LogAPIConfig setLogLevelConfigPath(String logLevelConfigPath) {
        this.logLevelConfigPath = logLevelConfigPath;
        return this;
    }
    
    
    public String getAppFilePath() {
        return logDirectory + logPrefix + logExtension;
    }
    
}
