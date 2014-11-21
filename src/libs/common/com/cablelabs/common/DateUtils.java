/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

import com.cablelabs.log.LogCategory;
import com.cablelabs.log.LogAPI;


public class DateUtils {
    
    /**
     * Logger
     */
    private static LogAPI logger = null;
    
    /**
     * The subcategory to use when logging.
     */
    private static final String subCat = "DateUtils";
    
    /**
     * A mapping of regular expressions to date format strings.
     * This is a LinkedHashMap so we can add the date formats with a higher probability of occurring first.
     */
    private static final LinkedHashMap<String,String> FORMATS = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        private void add(final String reg, final String format) {
            if (this.containsKey(reg)) {
                System.err.println("DateUtils:Attempting to override key('"+ reg+ "') alreay in FORMATS map");
                return;
            }
            put(reg, format);
        }
        {
            // snmp datetime
            add("^\\d{4}-\\d{1,2}-\\d{1,2},\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3},[-+]\\d{2}:\\d{2}$", "yyyy-MM-dd,HH:mm:ss.S,XXX");
            add("^\\d{4}-\\d{1,2}-\\d{1,2},\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3},[-+]\\d{4}?$", "yyyy-MM-dd,HH:mm:ss.S,XX");
            add("^\\d{4}-\\d{1,2}-\\d{1,2},\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3},[-+]\\d{2}?$", "yyyy-MM-dd,HH:mm:ss.S,X");
            add("^\\d{4}-\\d{1,2}-\\d{1,2},\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}$", "yyyy-MM-dd,HH:mm:ss.S");
            add("^\\d{4}-\\d{1,2}-\\d{1,2},\\d{1,2}:\\d{1,2}:\\d{1,2}$", "yyyy-MM-dd,HH:mm:ss");
            add("^\\d{4}-\\d{1,2}-\\d{1,2},\\d{1,2}:\\d{1,2}$", "yyyy-MM-dd,HH:mm");
            
            // syslog provisioning complete
            add("[a-z]{3}\\s\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}", "MMM dd HH:mm:ss");
            add("[a-z]{3}\\s\\d{1,2}\\s\\d{1,2}:\\d{1,2}", "MMM dd HH:mm");
            add("[a-z]{3}\\s\\d{1,2}", "MMM dd");
            add("[a-z]{3}\\s\\d{1,2}\\s\\d{2}", "MMM dd YY");
            add("[a-z]{3}\\s\\d{1,2}\\s\\d{4}", "MMM dd YYYY");
            
            // ISO 8601
            add("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd"); 
            add("^\\d{4}\\d{1,2}\\d{1,2}$", "yyyyMMdd");
            add("^\\d{4}-\\d{1,2}}$", "yyyy-MM");
            add("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}$", "yyyy-MM-dd HH:mm"); 
            add("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$", "yyyy-MM-dd HH:mm:ss");
            add("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}[-+]\\d{2}$", "yyyy-MM-dd HH:mm:ssX"); // tz example '-06'
            add("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}[-+]\\d{4}$", "yyyy-MM-dd HH:mm:ssXX"); // tz example '-0600'
            add("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}[-+]\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ssXXX"); // tz example '-06:00'
            add("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}$", "yyyy-MM-dd'T'HH:mm"); 
            add("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}$", "yyyy-MM-dd'T'HH:mm:ss");
            add("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}\\d{2}$", "yyyy-MM-dd'T'HH:mm:ssX");
            add("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}\\d{4}$", "yyyy-MM-dd'T'HH:mm:ssXX");
            add("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}[-+]\\d{2}:\\d{2}$", "yyyy-MM-dd'T'HH:mm:ssXXX");
            add("^\\d{8}T\\d{6}$", "yyyyMMdd'T'HHmmss");
            add("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
            
            // other possibles
            add("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
            add("^\\d{12}$", "yyyyMMddHHmm");
            add("^\\d{14}$", "yyyyMMddHHmmss");            
            
//            add("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");  
//            add("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
//            add("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
//            add("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
//            add("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
            
//            add("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");        
//            add("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
//            add("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
//            add("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
//            add("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
         
//            add("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
//            add("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
//            add("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
//            add("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");
//            add("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy HH:mm:ss");
        }
    };

    
    private synchronized static void init() {
        if (logger == null) logger = LogAPI.getInstance();
    }

    /**
     * Parses a date with an unknown format string
     * 
     * @param dateStr - the string to parse into a date
     * @return the date represented by dateStr if its format can be figured out
     */
    public static Date parse(String dateStr) {
        init();
        String format = getDateFormat(dateStr);
        if (format == null) {
            // SimpleDateParser is unable to parse a time zone that is not padded to 2 digits
            // Test if this is an snmp date without 2 digits on the time zone and try again with the padded form
            
            if (dateStr.matches("^\\d{4}-\\d{1,2}-\\d{1,2},\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3},[-+]\\d$")) {
                
                String digit = "0"  + dateStr.charAt(dateStr.length()-1);
                String dateStrAlt = dateStr.substring(0, dateStr.length()-1) + digit;
                logger.debug(LogCategory.APPLICATION, subCat, "Date string(" 
                        + dateStr + ") does not have its time zone in a format java will parse, altering to(" + dateStrAlt + ")");
                return parse(dateStrAlt);
            }
            else if (dateStr.matches("^\\d{4}-\\d{1,2}-\\d{1,2},\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3},[-+]\\d{1,2}:\\d{1,2}$")) {
                int commaIdx = dateStr.lastIndexOf(",");
                String tz = dateStr.substring(commaIdx+1);
                int colonIdx = tz.indexOf(":");
                String tzHr = tz.substring(1, colonIdx); // start at 1 to skip the [-+] character
                if (tzHr.length() == 1) tzHr = "0" + tzHr;
                String tzMin = tz.substring(colonIdx+1);
                if (tzMin.length() == 1) tzMin = "0" + tzMin;
                String dateStrAlt = dateStr.substring(0, commaIdx+2) + tzHr + ":"+ tzMin; // add +2 to the commaIdx to keep the , and [-+] characters
                logger.debug(LogCategory.APPLICATION, subCat, "Date string(" 
                        + dateStr + ") does not have its time zone in a format java will parse, altering to(" + dateStrAlt + ")");
                return parse(dateStrAlt);
            }
            return null;
        }
        
        return parse(dateStr, format);
    }
    
    /**
     * Parses a date that is expected to be in a particular format
     * 
     * @param dateStr - the date string to parse
     * @param format  - the format the date should be in
     * @return the Date represented by dateStr if it is valid for the passed
     *         in format, otherwise null
     */
    public static Date parse(String dateStr, String format) {
        init();
        
        if (format == null) {
            logger.warn(LogCategory.APPLICATION, subCat, "Date format string is null, unable to parse date");
            return null;
        }     
        
        SimpleDateFormat df = new SimpleDateFormat(format);
        
        Date result = null;
        try {
            result = df.parse(dateStr);
        } catch (ParseException e) {
            // Unfortunately this is not a valid date for the given format.
            logger.debug(LogCategory.APPLICATION, subCat, "Date string(" + dateStr + ") does not match required format(" + format + ")");
        }
        
        if (result != null) {
            Calendar now = Calendar.getInstance();
            
            Calendar c = Calendar.getInstance();
            c.setTime(result);
            
            // Fill in better defaults if the date did not contain them.
            if (!format.contains("y")) {
                c.set(Calendar.YEAR, now.get(Calendar.YEAR));
            }
            if (!format.contains("M")) {
                c.set(Calendar.MONTH, now.get(Calendar.MONTH));
            }
            if (!format.contains("d")) {
                c.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
            }
            if (!format.contains("h") && !format.contains("H") && !format.contains("k") && !format.contains("K")) {
                c.set(Calendar.HOUR_OF_DAY, 0);
            }
            if (!format.contains("M")) {
                c.set(Calendar.MINUTE, 0);
            }
            if (!format.contains("s")) {
                c.set(Calendar.SECOND, 0);
            }
            if (!format.contains("S")) {
                c.set(Calendar.MILLISECOND, 0);
            }
            
            result = c.getTime();
        }
        
        return result;
    }
    
    /**
     * Determines the format of a passed in date string. Returns null if the format can not be determined.
     * 
     * @param dateStr - the string to determine the format of
     * @return a string with the format of dateStr
     */
    public static String getDateFormat(String dateStr) {
        init();
        if (dateStr == null || dateStr.length() <= 0) {
            logger.debug(LogCategory.APPLICATION, subCat, "Searching for format of null or empty date string(" + dateStr + ")");
            return null;
        }
        
        String lowerDateStr = dateStr.trim().toLowerCase();
        for (String regexp : FORMATS.keySet()) {
            if (lowerDateStr.matches(regexp)) {
                return FORMATS.get(regexp);
            }
        }
        logger.debug(LogCategory.APPLICATION, subCat, "Unable to find format for passed in date string(" + dateStr + ")");
        return null; // Unknown format.
    }       

}
