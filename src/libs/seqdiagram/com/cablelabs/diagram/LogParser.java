package com.cablelabs.diagram;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cablelabs.log.LogAPI;
import com.cablelabs.log.LogCategory;

public class LogParser {

	private static final LogAPI logger = LogAPI.getInstance();
	private static final String subCat = "LogParser";
	
	private static final Pattern eventSendProcPattern = Pattern.compile("FSM (\\(.+?\\)) - State \\(.+?\\) (?:processing|sent) event \\(.*?\\) sequencer=(\\d+)\\.");
	private static final Pattern ipPortPattern = Pattern.compile("IP\\|Ports of ((:?UE|PCSCF|SCSCF)[0-9]) (:) ");
	
	private static final String SEPARATOR =    "+-------------------------------------------------------------------------+";
	private static final String SEPARATOR_SM = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -";
	private static final String SIP = "SIP";
	private static final String STUN = "STUN";

	private static final String TURN = "TURN";

    private static final String UTILITY = "Utility";
    protected static final String CRLF = "\r\n";
    private Configuration curConfig = null;
    
    // The key for this is the sequencer value
	private HashMap<String, Event> unknownFSM = new HashMap<String, Event>();

    protected LinkedHashMap<String, Configuration> configs = new LinkedHashMap<String, Configuration>();
    protected HashMap<String,String> ipNELabelMap = new HashMap<String,String>();

    protected ArrayList<String> platformIPs = new ArrayList<String>();
    protected LinkedList<String> openSockets = new LinkedList<String>();
    
	protected Long startTime = null;

	public LogParser() {

	}

	public LinkedHashMap<String, Configuration> parse(File f) {
        if (f.exists() && f.canRead() && f.isFile()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(f));
                String line = in.readLine();

                StringBuilder logMsgSB = new StringBuilder();

                Pattern levelMsgPat = Pattern.compile("^(ALL|CONSOLE|APP|TEST) .* .* (OFF|ALL|FATAL|ERROR|WARN|INFO|DEBUG|TRACE)");

                ArrayList<String> logLevels = new ArrayList<String>();
                ArrayList<String> logMessages = new ArrayList<String>();
                final Pattern datePattern = Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}");

                while (line != null) {
                    // Test whether the current line is apart of the previous
                    // log message or the start of a new one.
                    // This is done but verifying the first field in a date
                    // in the following format: yyyy-mm-dd


                    if (levelMsgPat.matcher(line).find()) {
                        if (logMsgSB != null && logMsgSB.length() > 0) {
                            logMessages.add(logMsgSB.toString());
                            logMsgSB = new StringBuilder();
                        }
                        logLevels.add(line);
                    }
                    else if (datePattern.matcher(line).find()) {
                        if (logMsgSB != null && logMsgSB.length() > 0) {
                            logMessages.add(logMsgSB.toString());
                            logMsgSB = new StringBuilder();
                        }
                        logMsgSB.append(line + CRLF);
                    }
                    else {
                        logMsgSB.append(line + CRLF);
                    }


                    line = in.readLine();
                }

                if (logMsgSB != null && logMsgSB.length() > 0) {
                    logMessages.add(logMsgSB.toString());
                    logMsgSB = null;
                }

                if (logMessages.size() > 0) {
                    startTime = getTimeStamp(logMessages.get(0));
                }

                logger.debug(LogCategory.APPLICATION, subCat,  "Parser - Parsed log levels");
                for (String lv : logLevels)
                    logger.debug(LogCategory.APPLICATION, subCat,  lv);

                
                logger.debug(LogCategory.APPLICATION, subCat,  SEPARATOR);
                logger.debug(LogCategory.APPLICATION, subCat,  "Parser - Processing log messages");
                for (String log : logMessages) {
//                    logger.debug(LogCategory.APPLICATION, subCat,  SEPARATOR_SM);
//                    logger.debug(LogCategory.APPLICATION, subCat,  log);
                    parseLogMsg(log);
                }
                logger.debug(LogCategory.APPLICATION, subCat,  SEPARATOR);


                if (unknownFSM.size() > 0) {
                    logger.error(LogCategory.APPLICATION, subCat,  "The following events are not assigned to a fsm, log may be incomplete.");
                    Iterator<String> iter = unknownFSM.keySet().iterator();
                    while (iter.hasNext()) {
                        logger.debug(LogCategory.APPLICATION, subCat,  unknownFSM.get(iter.next()).getMessage() + "\n" +SEPARATOR_SM);
                    }
                }
                logger.debug(LogCategory.APPLICATION, subCat,  SEPARATOR);


                in.close();
            }
            catch (FileNotFoundException fnf) {
                System.err.println(
                        "PC2TraceParser could not find the log file["
                                + f.getAbsolutePath() + "] to read.\n");
                fnf.printStackTrace();
            }
            catch (IOException io) {
                System.err.println(
                        "PC2TraceParser encountered an error while trying to read log file["
                                + f.getAbsolutePath() + "].");
                io.printStackTrace();
            }
            catch (IllegalArgumentException ia) {

                System.err.println(
                        "PC2TraceParser encountered an illegal argument exception while"
                                + " trying to read log file[" + f.getAbsolutePath() + "].");
                ia.printStackTrace();
            }
        }

//        if (configs.size() == 2) {
//            Iterator<String> iter = configs.keySet().iterator();
//            String name1 = iter.next();
//            String name2 = iter.next();
//            String name = null;
//            if (name1.equals(Configuration.UNKNOWN_DUT)) {
//                name = name2;
//            } else if (name2.equals(Configuration.UNKNOWN_DUT)) {
//                name = name1;
//            }
//
//            if (name != null) {
//                Configuration unknown = configs.get(Configuration.UNKNOWN_DUT);
//                Configuration real = configs.get(name);
//                if (unknown.tests.size() > 0 && real.tests.size() == 0) {
//                    configs.remove(name);
//                    unknown.name = name;
//                    configs.remove(Configuration.UNKNOWN_DUT);
//                    configs.put(name, unknown);
//                }
//            }
//
//        }
        
        return configs;
    }



	private void assignMessageToFSM(String line) {
	    Matcher m = eventSendProcPattern.matcher(line);
	    if (m.find()) {
	        String fsm = m.group(1);
	        String seq = m.group(2);


	        Event e= unknownFSM.get(seq);
	        boolean found = false;
	        if (e != null && e.getSequence().equals(seq)) {
	            if (curConfig != null) {
	                curConfig.addEvent(fsm, e);
	                logger.debug(LogCategory.APPLICATION, subCat,  "Adding sequencer=" + seq + " to FSM(" + fsm +").");
	                found = true;
	                unknownFSM.remove(seq);
	            }
	        }
	        if (!found) {
	            logger.debug(LogCategory.APPLICATION, subCat,  "Determined that sequencer=" + seq + " was an internal event for FSM(" + fsm + ").");
	        }
        }
	}

    private void parseActiveServers(String logMessage) {

        /*
         * Example message
         *
         * 2012-03-05 14:17:17,249 INFO  Active servers.
         * C:\Users\rvail\Projects\PCSim2Config\ProxyTest\UE-dual_P-dual\PlatformCORE.xls
         * SIP UDP fc00:504:700:0:20f:b5ff:fef5:e752|5060
         * SIP UDP 10.32.0.212|5060
         * STUN UDP 10.32.0.212|3478
         * STUN UDP 10.32.0.212|27000
         * STUN UDP 10.32.0.212|27001
         * STUN UDP 10.32.0.212|28000
         * STUN UDP 10.32.0.212|28001
         * TURN UDP 10.32.0.212|3479
         * Utility UDP 10.32.0.212|10000
         *
         */

        String[] lines = logMessage.split(CRLF);
        String configFile = lines[1].trim();

        platformIPs = new ArrayList<String>();
        openSockets = new LinkedList<String>();
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith(SIP)
                    || line.startsWith(STUN)
                    || line.startsWith(TURN)
                    || line.startsWith(UTILITY)) {

                String[] parts = line.split(" ", 3);
                // parts[0] = stack
                // parts[1] = protocol
                // parts[2] = ip|port
                if (parts.length == 3) {
                    platformIPs.add(parts[2]);
                    openSockets.add(line);
                }
                else {
                    System.err.println("Unable to extract ip|port from(" + line + ")");
                }
            }

        }
        
        if (curConfig != null) {
            curConfig.setPlatformSockets(openSockets);
        }
        
    }

    private void parseDutConfig(String logMessage) {
        int start = logMessage.indexOf("(") + 1;
        int end = logMessage.indexOf("has been added to the", start);
       
        String configFile = logMessage.substring(start, end).trim();
        
        long timestamp = getTimeStamp(logMessage);
        curConfig = new Configuration(logMessage, (timestamp-startTime), openSockets, configFile);
        configs.put(curConfig.name, curConfig);
        logger.debug(LogCategory.APPLICATION, subCat,  "Parsing message events for configuration file=" + curConfig.name);
        
    }

    private void parseIPPorts(String logMessage) {
        Matcher m = ipPortPattern.matcher(logMessage);
        
        m.find();
        
        String ne = m.group(1);
        int end = logMessage.indexOf("- PCSim2.java");
        String ipPortsStr = logMessage.substring(m.start(3)+1, end);
        String[] ipPorts = ipPortsStr.split(",");
        
        
        ArrayList<String> neIPs = new ArrayList<String>();
        for (String ipPort : ipPorts) {
            logDebug(ne + " is using " +ipPort);
            neIPs.add(ipPort.trim());
        }
        
        if (curConfig != null) {
            curConfig.updateNE(ne, neIPs);
        }
    }

    private void parseLogMsg(String logMessage) {

	    if (logMessage.contains(">>>>> RX:") || logMessage.contains("<<<<< TX:")) {
            parseRXTX(logMessage);
        }
        else if (logMessage.contains("INFO  Active servers.")) {
            parseActiveServers(logMessage);
//            updateParsedMessages();// shouldn't need this since Active Servers should come before any messages
        }
        else if (logMessage.contains("The DUT Configuration File(")) {
            parseDutConfig(logMessage);
        }
        else if (ipPortPattern.matcher(logMessage).find()) {
            parseIPPorts(logMessage);
        }
        else if (logMessage.contains("Commencing test ")) {
            parseTest(logMessage);
        }
        else if (logMessage.contains("Servers terminated.")) {
            long timestamp = getTimeStamp(logMessage);
            curConfig.complete(timestamp-startTime);
            logger.debug(LogCategory.APPLICATION, subCat,  "Configuration file is closing=" + curConfig.name);
            curConfig = null;
        }
        else if (logMessage.contains("Test \"") && (logMessage.contains("\" Passed.") || logMessage.contains("\" Failed.")) && curConfig != null) {
            long timestamp = getTimeStamp(logMessage);
            String name = null;
            if (curConfig != null && curConfig.curTest != null) {
                name = curConfig.curTest.name;
            }
            logger.debug(LogCategory.APPLICATION, subCat,  "Identified end of test=" + name);
            curConfig.endTest((timestamp-startTime));
        }

	    // 2012-03-05 14:17:18,435 INFO  FSM (Proxy Term) - State (1) processing event (INVITE) sequencer=14. - State.java 345 [Proxy Term]
	    // 2012-03-05 14:17:18,435 INFO  FSM (Proxy Term) - State (1) sent event (INVITE) sequencer=16. - PC2Models.java 957 [Proxy Term]
	    // 2012-03-05 14:17:18,560 INFO  FSM (Proxy Term) - State (3) sent event (180-INVITE) sequencer=23. - PC2Models.java 957 [Proxy Term]
	    // 2012-02-17 09:50:53,939 INFO  FSM (UE0) - State (Proceeding) processing event (180-INVITE) sequencer=9. - State.java 334 [UE0]
        else if (eventSendProcPattern.matcher(logMessage).find()) {
            assignMessageToFSM(logMessage);
        }
	}

    private void parseRXTX(String logMsgStr) {

        /*
         * Example Message
         *
         *   2012-02-17 09:50:47,767 INFO  <<<<< TX:   Length = 582
         *   Sent from IP|Port=10.4.10.31|5070
         *   To IP|Port=/10.4.1.33|5060
         *   Sequencer=2
         *   Transport=UDP
         *   [REGISTER sip:pclab.com SIP/2.0
         *   Call-ID: f3d7588b14c34b56b606f540eb67aca6@10.4.10.31
         *   CSeq: 1004 REGISTER
         *   From: <sip:UE0@pclab.com>;tag=496059228
         *   To: <sip:UE0@pclab.com>
         *   Via: SIP/2.0/UDP UE0.pclab.com:5070;branch=z9hG4bK_DUT-4;rport
         *   Max-Forwards: 70
         *   Contact: <sip:UE0@UE0.pclab.com:5070>;expires=600000
         *   Route: <sip:pclab.com;keep-stun;lr;transport=udp>
         *   Supported: path, sec-agree
         *   Expires: 600000
         *   P-Access-Network-Info: DOCSIS
         *   Authorization: Digest username="ue0@pclab.com",realm="pclab.com",nonce="",response="",algorithm=MD5,uri="sip:ue0@pclab.com"
         *   Content-Length: 0
         *
         *   ] - LogWriter.java 253 [UE0]
         *
         */
        Pattern eol = Pattern.compile(CRLF);


        String[] lines = eol.split(logMsgStr);

        boolean isSender = isSender(lines[0]);

        long time = getTimeStamp(logMsgStr);
        String timeStr = getTimeStampStr(logMsgStr);
        String from = null, to = null, transport = null, firstLine = null,
                msgStr = null, status = null, transID = null, debug = null, callID = null;

        int sequencer = -1;

        StringBuilder msgSB = new StringBuilder();

        boolean foundMsg = false;
        String msgType = null;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() <= 0) continue;

            if (!foundMsg) foundMsg = (line.charAt(0) == '[');

            if (!foundMsg) {
                if (line.startsWith("Sent from IP|Port=")
                        || line.startsWith("From IP|Port=")) {
                    from = getLineIPValue(line, "=");
                }
                else if (line.startsWith("To IP|Port=")
                        || line.startsWith("Received on IP|Port=")) {
                    to = getLineIPValue(line, "=");
                }
                else if (line.startsWith("Sequencer=")) {
                    try {
                        sequencer = Integer.parseInt(getLineValue(line, "="));
                    }
                    catch (NumberFormatException e) {
                        sequencer = -1;
                        System.err.println("Parser - message does not have a valid integer sequencer value(" + line + ")");
                    }
                }
                else if (line.startsWith("Transport")) {
                    transport = getLineValue(line, "=");
                }
            }
            else {
                if (firstLine == null) {
                    if (line.startsWith("[")) {
                        firstLine = line.substring(1);
                    } else {
                        firstLine = line;
                    }
                    if (firstLine.contains(SIP)) {
                        msgType = SIP;
                    }
                }

                if (line.startsWith("Call-ID:")) {
                    callID = getLineValue(line, ":");
                }
                else if (line.startsWith("Via:")) {
                    transID = getTransactionId(line);
                }

                if (line.startsWith("]")) {
                    msgSB.append("]");
                } else {
                    msgSB.append(line);
                    msgSB.append(CRLF);
                }
            }

        }

        msgStr = msgSB.toString();

//        logger.debug(LogCategory.APPLICATION, subCat,  "Message (" + sequencer + ") --- \n\n"
//                + "FROM=" + from
//                + "\nTO=" + to
//                + "\nTIME=" + time
//                + "\nSENDER=" + isSender
//                + "\nTRANS_ID=" + transID
//                + "\nCALL_ID=" + callID
//                + "\nLINE1=" + firstLine +
//                "\n\n" + msgStr + " --- END");

        if (from == null || to == null || sequencer == -1) {
            @SuppressWarnings("unused")
            int brkPt = 1;
        }

        // Event(String to, String from, String seqNo, String firstLine,
        //         String message, Long timestamp, String timestampStr, Long offset, boolean sent)

        long offset = time-startTime;

        Event evt = null;

            evt = new Event(to, from, Integer.toString(sequencer), sequencer + " - " + firstLine, msgStr, time,
                    timeStr, offset, isSender, curConfig);


        unknownFSM.put(Integer.toString(sequencer), evt);
    }

    private void parseTest(String logMsg) {
        String test = null;
        String pattern = "test \"";
        int startIndex = logMsg.indexOf(pattern);

        if (startIndex != -1) {
            startIndex += pattern.length();

            int endIndex = logMsg.indexOf("\"", startIndex);
            if (endIndex != -1)
                test = logMsg.substring(startIndex,endIndex);

        }
        
        String dut = null;
        pattern = "for DUT \"";
        startIndex = logMsg.indexOf(pattern);
        if (startIndex != -1) {
            startIndex += pattern.length();
            
            int endIndex = logMsg.indexOf("\"", startIndex);
            if (endIndex != -1)
                dut = logMsg.substring(startIndex, endIndex);
        }
        
        if (dut != null) {
            File dutF = new File(dut);
            String dutName = dutF.getName();
            Configuration dutConfig = configs.get(dutName);
            if (dutConfig == null) {
                long timestamp = getTimeStamp(logMsg);
                dutConfig = new Configuration(logMsg, (timestamp-startTime), openSockets, dut);
                configs.put(dutName, dutConfig);
            }
            curConfig = dutConfig;
        } else {
            curConfig = configs.get(Configuration.UNKNOWN_DUT);
        }

        if (test != null) {
            long timestamp = getTimeStamp(logMsg);
            
            if (curConfig == null) {
                // This Log file is old and does not have logging setup the new way.
                                
                curConfig = new Configuration(null, (timestamp-startTime), openSockets, Configuration.UNKNOWN_DUT);
                configs.put(curConfig.name, curConfig);
                logger.debug(LogCategory.APPLICATION, subCat,  "Parsing message events for configuration file=" + curConfig.name);
            }
            
            curConfig.startTest(test, (timestamp - startTime));
            logger.debug(LogCategory.APPLICATION, subCat,  "+------------ Identified new test=" + curConfig.curTest.name + " --------------+");
        }
        else {
            System.err.println("Error parseTest sent log message with unexpected format: " + logMsg);
        }


    }

	private static String getLineIPValue(String line, String sep) {
        String ip = getLineValue(line, sep);

        // ipv6 addresses can be enclosed in [] removed them here
        // so easier comparison later

        String[] parts = ip.split("\\|", 2);

        if (parts[0].startsWith("[")) {
            parts[0] = parts[0].substring(1);
        }
        if (parts[0].endsWith("]")) {
            parts[0] = parts[0].substring(0, parts[0].length()-1);
        }
        switch (parts.length) {
        case 1:
            ip = parts[0];
            break;
        case 2:
            ip = parts[0] + "|" + parts[1];
            break;
        }

        return ip;
    }

	private static String getLineValue(String line, String sep) {
        int idx = line.indexOf(sep) + sep.length();
        if (line.charAt(idx) == '/')
            idx++;
        String val = line.substring(idx).trim();
        return val;
    }

	private static long getTimeStamp(String line) {
//		logger.debug(LogCategory.APPLICATION, subCat,  "Orig time - " + line);
		String year = line.substring(0,4);
		String month = line.substring(5,7);
		String day = line.substring(8,10);
		String hour = line.substring(11,13);
		String min = line.substring(14,16);
		String sec = line.substring(17,19);
		String usec = line.substring(20,23);
//		logger.debug(LogCategory.APPLICATION, subCat,  "year=" + year + " month=" + month + " day=" + day
//				+ " hour=" + hour + " min=" + min + " sec=" + sec + " usec=" + usec);
		GregorianCalendar startTime = new GregorianCalendar(Integer.parseInt(year),
				Integer.parseInt(month)-1,
				Integer.parseInt(day),
				Integer.parseInt(hour),
				Integer.parseInt(min),
				Integer.parseInt(sec));
//		logger.debug(LogCategory.APPLICATION, subCat,  "StartTime=" + startTime);
		long real = startTime.getTimeInMillis() + Long.parseLong(usec);
//		Date date = new Date(real);
//		DateFormat df = (DateFormat)new ISO8601DateFormat();
//		String ts = df.format(date);
//		if (ts.equals(line)) {
//			logger.debug(LogCategory.APPLICATION, subCat,  "Conversion correct.\n" + line + "\n" + ts);
//		}
//		else
//			logger.debug(LogCategory.APPLICATION, subCat,  "Conversion failed.  \n" + line + "\n" + ts);

		return real;
	}

	private static String getTimeStampStr(String msg) {
        StringTokenizer tokens = new StringTokenizer(msg);
        switch (tokens.countTokens()) {
        case 0:
            return null;
        case 1:
            return tokens.nextToken();
        default:
            return tokens.nextToken() + " " + tokens.nextToken();
        }
    }

    private static String getTransactionId(String line) {
        String[] parts = line.split(";");
        for (String part : parts){
            if (part.startsWith("branch=")) {
                return part.substring(part.indexOf("=")+1);
            }
        }
        return null;
    }

    private static boolean isSender(String line) {
		// The value sent is based upon the perspective of the platform.
		if (line.contains("<<<<< TX:"))
			return true;
		return false;
	}

    private static void log(Object o) {
        logger.info(LogCategory.APPLICATION, "LogParser", o);
    }
    
    private static void logDebug(Object o) {
        logger.debug(LogCategory.APPLICATION, "LogParser", o);
    }
    
    private static void logError(Object o) {
        logger.error(LogCategory.APPLICATION, "LogParser", o);
    }
    
    
}
