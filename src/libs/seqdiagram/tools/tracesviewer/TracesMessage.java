package tools.tracesviewer;

import java.io.*;

public class TracesMessage implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	String messageFrom = null;
	String messageTo = null;
	long messageTime = -1;
	String messageDisplayTime = null;
	String messageString = null;
	String messageFirstLine = null;
	String messageStatusInfo = null;
	String messageTransactionId = null;
	String debugLine = null;
	int sequencer = 0;

	String beforeDebug;
	String afterDebug;

	public TracesMessage() {
	}

	public TracesMessage(
		String messageFrom,
		String messageTo,
		long messageTime,
		String messageDisplayTime,
		String messageFirstLine,
		String messageString,
		String messageStatusInfo,
		String messageTransactionId,
		String debugLine,
		int sequencer) {
		this.messageFrom = messageFrom;
		this.messageTo = messageTo;
		this.messageTime = messageTime;
		this.messageDisplayTime = messageDisplayTime;
		this.messageString = messageString;
		this.messageFirstLine = messageFirstLine;
		this.messageStatusInfo = messageStatusInfo;
		this.messageTransactionId = messageTransactionId;
		this.debugLine = debugLine;
		this.sequencer = sequencer;
	}

	public void setFrom(String from) {
		messageFrom = from;
	}

	public void setTo(String to) {
		messageTo = to;
	}

	public void setTime(long time) {
		messageTime = time;
	}
	
	public void setDisplayTime(String time) {
        messageDisplayTime = time;
    }

	public void setMessageString(String str) {
		messageString = str;
	}

	public void setFirstLine(String FirstLine) {
		messageFirstLine = FirstLine;
	}

	public void setStatusInfo(String statusInfo) {
		messageStatusInfo = statusInfo;
	}

	public void setTransactionId(String transactionId) {
		messageTransactionId = transactionId;
	}

	public String getFrom() {
		return messageFrom;
	}

	public String getTo() {
		return messageTo;
	}

	public long getTime() {
		return messageTime;
	}
	
	public String getTimeDisplay() {
        return messageDisplayTime;
    }

	public String getMessageString() {
		//System.out.println("messageContent:"+messageString);
		return messageString;
		//+
		//"\n-------------------\n"      +
		//"|debugLogLine = " + debugLine +" |"  +
		//"\n-------------------";
	}

	public String getFirstLine() {
		return messageFirstLine;
	}

	public String getStatusInfo() {
		return messageStatusInfo;
	}

	public String getTransactionId() {
		return messageTransactionId;
	}

	// PC 2.0 add the following gets to allow
	// the new listener access to the fields
	public String getBeforeDebug() {
		return beforeDebug;
	}

	public String getAfterDebug() {
		return afterDebug;
	}

	public String getDebugLine() {
		return debugLine;
	}

	public int getSequencer() {
		return sequencer;
	}
}
