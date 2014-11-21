package com.cablelabs.old_diagram;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.io.File;
import java.net.URL;
import java.awt.Image;
import java.awt.Toolkit;

import org.apache.log4j.helpers.ISO8601DateFormat;

import com.cablelabs.diagram.Actor;

import tools.tracesviewer.LogComparator;
import tools.tracesviewer.MessageLogList;
import tools.tracesviewer.TracesMessage;
import tools.tracesviewer.TracesViewer;

public class PC2TraceParser {
    private static final String CRLF = "\r\n";

    private static final String PCSIM2 = "PCSim2";
    private static final String SIP = "SIP";
    private static final String STUN = "STUN";
    private static final String TURN = "TURN";
    private static final String UTILITY = "Utility";

    private static final String SEPARATOR =    "+-------------------------------------------------------------------------+";
    private static final String SEPARATOR_SM = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -";

    private static final Pattern datePattern = Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}");

    protected ArrayList<String> platformIPs = new ArrayList<String>();
    protected Long startTime = null;
    protected HashMap<String, MessageLogList> messageLogs = new HashMap<String, MessageLogList>();
    
    private HashMap<String,String> ipNELabelMap = new HashMap<String, String>();

    public PC2TraceParser() {
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            URL url = PC2TraceParser.class.getResource("images/comp.gif");
            if (url != null) {
                Image pc2Image = toolkit.getImage(url);
                if (pc2Image != null) {
                    Actor.setImage(PCSIM2, pc2Image);
                }
            }
        }
        catch (Exception e) {
            System.err.println("Unable to load image from toolkit " + e.getLocalizedMessage());
        }
    }

    //	private String getCallId(String line, String firstLine) {
    //		if (firstLine.contains("SIP")) {
    //			String hdr = "Call-ID: ";
    //			String param = "branch=";
    //			int callIdIndex = line.indexOf(hdr);
    //			callIdIndex += hdr.length(); // move past the hdr
    //			int branchIndex = line.indexOf(param, callIdIndex);
    //			branchIndex += param.length(); // move past the param label
    //			int semi = line.indexOf(";", branchIndex);
    //			int crlf = line.indexOf(CRLF, branchIndex);
    //			if (semi != -1 &&
    //					(crlf == -1 || semi < crlf)) {
    //				return line.substring(branchIndex, semi);
    //			}
    //			else if (crlf != -1)
    //				return line.substring(branchIndex, crlf);
    ////			int [] hdrLocation = locator.locateSIPHeader(hdr, instance, line);
    ////			if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
    ////				int [] paramLocation = locator.locateSIPParameter(hdr, param,
    ////						hdrLocation, line);
    ////				if (SIPLocator.validParamLocation(paramLocation))
    ////					return line.substring(paramLocation[1], paramLocation[2]);
    ////			}
    //		}
    //		return null;
    //	}

    public HashMap<String, MessageLogList> parse(File f) {
        if (f.exists() && f.canRead() && f.isFile()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(f));
                String line = in.readLine();

                //String logMsg = null;
                StringBuilder logMsgSB = new StringBuilder();

                Pattern levelMsgPat = Pattern.compile("^(ALL|CONSOLE|APP|TEST) .* .* (OFF|ALL|FATAL|ERROR|WARN|INFO|DEBUG|TRACE)");
                long timestamp = 0;

                ArrayList<String> logLevels = new ArrayList<String>();
                ArrayList<String> logMessages = new ArrayList<String>();


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

                System.out.println("Parser - Parsed log levels");
                for (String lv : logLevels)
                    System.out.println(lv);

                System.out.println(SEPARATOR);
                System.out.println("Parser - Processing log messages");
                for (String log : logMessages) {
                    System.out.println(SEPARATOR_SM);
                    System.out.println(log);
                    parseMessage(log);
                }
                System.out.println(SEPARATOR);

                //				while (line != null) {
                //					// Test whether the current line is apart of the previous
                //					// log message or the start of a new one.
                //					// This is done but verifying the first field in a date
                //					// in the following format: yyyy-mm-dd
                //					if (logMsg != null && messageEvent &&
                //							line.length() > 7 && datePattern.matcher(line).find()) {
                //
                //						parseMessage(logMsg, timestamp);
                //						messageEvent = false;
                //						logMsg = null;
                //						timestamp = 0;
                //					}
                //					if (startTime == null) {
                //                        StringTokenizer tokens = new StringTokenizer(line);
                //                        if (line.length() > 7 && line.charAt(4) == '-' &&
                //                        line.charAt(7) == '-' && tokens.countTokens() >= 2) {
                //                            String start = new String();
                //                            start = tokens.nextToken();
                //                            start += " " + tokens.nextToken();
                //                            startTime = getTimeStamp(start);
                //
                //                            System.out.println("Start time = " + startTime);
                //                        }
                //                    }
                //					
                //					if (line.startsWith(SIP) || line.startsWith("STUN")) {
                //					    String[] parts = line.split(" ");
                //					    
                //					}
                //					else if (line.contains(">>>>> RX:") || line.contains("<<<<< TX:")) {
                //						logMsg = line + CRLF;
                //						messageEvent = true;
                //						timestamp = getTimeStamp(line);
                //					}
                //					else if (messageEvent) {
                //						logMsg += line + CRLF;
                //					}
                //					
                //
                //					line = in.readLine();
                //				}


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
        return messageLogs;
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

        for (String line : lines) {
            if (line.startsWith(SIP)
                    || line.startsWith(STUN)
                    || line.startsWith(TURN)
                    || line.startsWith(UTILITY)) {

                String[] parts = line.trim().split(" ", 3);
                // parts[0] = stack
                // parts[1] = protocol
                // parts[2] = ip|port
                if (parts.length == 3)
                    platformIPs.add(parts[2]);
                else
                    System.err.println("Unable to extract ip|port from(" + line + ")");
            }

        }
    }

    private void parseMessage(String logMessage) {

        if (logMessage.contains(">>>>> RX:") || logMessage.contains("<<<<< TX:")) {
            parseRXTX(logMessage);
        } 
        else if (logMessage.contains("INFO  Active servers.")) {
            parseActiveServers(logMessage);
            updateParsedMessages();
        }

    }

    private void parseRXTX(String logMsgStr) {

        /*
         * Example SIP Message
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

        String isSender = isSender(lines[0]);

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
                    //TODO if from is the DUT or platform set the string as such
                }
                else if (line.startsWith("To IP|Port=") 
                        || line.startsWith("Received on IP|Port=")) {
                    to = getLineIPValue(line, "=");
                    //TODO if to is the DUT or platform set the string as such
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

        System.out.println("Message (" + sequencer + ") --- \n\n" 
                + "FROM=" + from 
                + "\nTO=" + to 
                + "\nTIME=" + time
                + "\nSENDER=" + isSender
                + "\nTRANS_ID=" + transID
                + "\nCALL_ID=" + callID
                + "\nLINE1=" + firstLine +
                "\n\n" + msgStr + " --- END");

        //	    TracesMessage(
        //	            String messageFrom,
        //	            String messageTo,
        //	            String messageTime,
        //	            String messageFirstLine,
        //	            String messageString,
        //	            String messageStatusInfo,
        //	            String messageTransactionId,
        //	            String debugLine,
        //	            String sequencer) 

        if (from == null || to == null || sequencer == -1) {
            @SuppressWarnings("unused")
            int brkPt = 1;
        }


        TracesMessage msg = new TracesMessage(from, to, time, timeStr,
                firstLine, msgStr, status, transID, debug, sequencer);

        updateMessage(msg);

        MessageLogList messageLogList = messageLogs.get(callID);
        if (messageLogList == null) {
            messageLogList = new MessageLogList(new LogComparator());
            messageLogs.put(callID, messageLogList);
        }
        messageLogList.add(msg);


        /////////////////////////////////////////////	    

        //		String isSender = isSender(msgStr);

        //		int line1 = msgStr.indexOf(CRLF);
        //		line1 += 2;
        //		int line2 = msgStr.indexOf("=", line1);
        //		line2 += 1;
        //		if (msgStr.charAt(line2) == '/')
        //			line2++;
        //		int endLine2 = msgStr.indexOf(CRLF, line2);
        //		String temp = msgStr.substring(line2, endLine2);
        //		String from = null;
        //		if (temp.equals(platform))
        //			from = "PCSim2";
        //		else if (temp.equals(dut))
        //			from = "DUT";
        //		else
        //			from = temp;
        //
        //		line2 = msgStr.indexOf(CRLF, line1);
        //		int line3 = msgStr.indexOf("=", line2 + 2);
        //		line3 +=2;
        //		if (msgStr.charAt(line3) == '/')
        //			line3++;
        //		int endLine3 = msgStr.indexOf(CRLF, line3);
        //		temp = msgStr.substring(line3, endLine3);
        //		String to = null;
        //		if (temp.equals(platform))
        //			to = "PCSim2";
        //		else if (temp.equals(dut))
        //			to = "DUT";
        //		else
        //			to = temp;

        //		endLine3+=3; // move past \r\n[
        //		int endLine4 = msgStr.indexOf(CRLF, endLine3);
        //		String firstLine = msgStr.substring(endLine3, endLine4);

        //		String transId = getTransactionId(logMsgStr, firstLine);
        //		String callId = "1"; // getCallId(line, firstLine);

        //		int msgEnd = logMsgStr.indexOf("] - ", endLine3);
        //		String message = logMsgStr.substring(endLine3,msgEnd);
        //		System.out.println("Message (" + (timestamp-startTime)
        //				+ ") --- \n\n" + "FROM=" + from + "\nTO=" + to +
        //				"\nTIME=" + timestamp + "\nSENDER=" + isSender + "\nTRANS_ID="
        //				+ transId + "\nCALL_ID=" + callId + "\nLINE1=" + firstLine +
        //				"\n\n" + message + " --- END");
        //
        //		TracesMessage messageLog = new TracesMessage(from,
        //				to,
        //				Long.toString(timestamp),
        //				firstLine,
        //				message,
        //				"",
        //				transId,
        //				"0",
        //				"");
        //
        //		MessageLogList messageLogList =
        //			messageLogs.get(callId);
        //		if (messageLogList == null) {
        //			messageLogList = new MessageLogList(new LogComparator());
        //			messageLogs.put(callId, messageLogList);
        //		}
        //		messageLogList.add(messageLog);

    }

    /**
     * Updates the passed in message's 
     * from and to fields to 'PCSim2' as appropriate.
     * 
     * @param msg The message to update
     */
    private void updateMessage(TracesMessage msg) {
        if (platformIPs == null) return;
        if (platformIPs.contains(msg.getFrom())) {
            msg.setFrom(PCSIM2);
        }
        if (platformIPs.contains(msg.getTo())) {
            msg.setTo(PCSIM2);
        }
    }

    /**
     * Goes through all messages in messageLogs and updates their 
     * from and to fields to PCSim2 as appropriate.
     */
    private void updateParsedMessages() {
        if (messageLogs == null) return;

        Enumeration<MessageLogList> elems = messageLogs.elements();
        if (elems == null) return;

        while (elems.hasMoreElements()) {
            MessageLogList mll = elems.nextElement();
            Iterator<TracesMessage> iter = mll.iterator();

            if (iter == null)
                continue;

            while (iter.hasNext()) {
                TracesMessage msg = iter.next();
                updateMessage(msg);
            }

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

    private static long getTimeStamp(String msg) {
        //		System.out.println("Orig time - " + line);
        String year = msg.substring(0,4);
        String month = msg.substring(5,7);
        String day = msg.substring(8,10);
        String hour = msg.substring(11,13);
        String min = msg.substring(14,16);
        String sec = msg.substring(17,19);
        String usec = msg.substring(20,23);
        //		System.out.println("year=" + year + " month=" + month + " day=" + day
        //				+ " hour=" + hour + " min=" + min + " sec=" + sec + " usec=" + usec);
        GregorianCalendar startTime = new GregorianCalendar(Integer.parseInt(year),
                Integer.parseInt(month)-1,
                Integer.parseInt(day),
                Integer.parseInt(hour),
                Integer.parseInt(min),
                Integer.parseInt(sec));

        //		System.out.println("StartTime=" + startTime);
        long real = startTime.getTimeInMillis() + Long.parseLong(usec);
        //		Date date = new Date(real);
        //		DateFormat df = (DateFormat)new ISO8601DateFormat();
        //		String ts = df.format(date);
        //		if (ts.equals(line)) {
        //			System.out.println("Conversion correct.\n" + line + "\n" + ts);
        //		}
        //		else
        //			System.out.println("Conversion failed.  \n" + line + "\n" + ts);

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
        //		if (firstLine.contains("SIP")) {
        //			// This is actually the first branch parameter in the first Via header
        //			String hdr = "Via";
        ////			String instance = "first";
        //			String param = "branch";
        //			int viaIndex = line.indexOf(hdr);
        //			viaIndex += hdr.length(); // move past the hdr
        //			int branchIndex = line.indexOf(param, viaIndex);
        //			branchIndex += param.length(); // move past the param label
        //			int semi = line.indexOf(";", branchIndex);
        //			int crlf = line.indexOf(CRLF, branchIndex);
        //			int comma = line.indexOf(",", branchIndex);
        //			if (semi != -1 &&
        //					(comma == -1 || semi < comma) &&
        //					(crlf == -1 || semi < crlf)) {
        //				return line.substring(branchIndex, semi);
        //			}
        //			else if (comma != -1 && (crlf == -1 || comma < crlf))
        //				return line.substring(branchIndex, comma);
        //			else if (crlf != -1)
        //				return line.substring(branchIndex, crlf);
        ////			int [] hdrLocation = locator.locateSIPHeader(hdr, instance, line);
        ////			if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
        ////				int [] paramLocation = locator.locateSIPParameter(hdr, param,
        ////						hdrLocation, line);
        ////				if (SIPLocator.validParamLocation(paramLocation))
        ////					return line.substring(paramLocation[1], paramLocation[2]);
        ////			}
        //		}
        //		return null;

        String[] parts = line.split(";");
        for (String part : parts){
            if (part.startsWith("branch=")) {
                return part.substring(part.indexOf("=")+1);
            }
        }
        return null;
    }

    private static String isSender(String line) {
        if (line.contains("<<<<< TX:"))
            return new String("true");
        return new String("false");
    }
}
