/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/

package gov.nist.javax.sip.stack;

/**
 * This class stores a message along with some other informations
 * Used to log messages.
 *
 *@version  JAIN-SIP-1.1 $Revision: 1.5 $ $Date: 2004/01/22 13:26:33 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * @author Marc Bednarek  <br/>
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
class MessageLog {

	private String message;

	private String source;

	private String destination;

	private long timeStamp;

	private boolean isSender;

	private String firstLine;

	private String statusMessage;

	private String tid;

	private String callId;

	private int debugLine;

	public boolean equals(Object other) {
		if (!(other instanceof MessageLog)) {
			return false;
		} else {
			MessageLog otherLog = (MessageLog) other;
			return otherLog.message.equals(message)
				&& otherLog.timeStamp == timeStamp;
		}
	}

	/**
	 * Constructor
	 */

	public MessageLog(
		String message,
		String source,
		String destination,
		String timeStamp,
		boolean isSender,
		String firstLine,
		String statusMessage,
		String tid,
		String callId,
		int lineCount) {
		if (message == null || message.equals(""))
			throw new IllegalArgumentException("null msg");
		this.message = message;
		this.source = source;
		this.destination = destination;
		try {
			long ts = Long.parseLong(timeStamp);
			if (ts < 0)
				throw new IllegalArgumentException("Bad time stamp ");
			this.timeStamp = ts;
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(
				"Bad number format " + timeStamp);
		}
		this.isSender = isSender;
		this.firstLine = firstLine;
		this.statusMessage = statusMessage;
		this.tid = tid;
		this.callId = callId;
		this.debugLine = lineCount;
	}

	protected long getTimeStamp() {
		return this.timeStamp;
	}

	public MessageLog(
		String message,
		String source,
		String destination,
		long timeStamp,
		boolean isSender,
		String firstLine,
		String statusMessage,
		String tid,
		String callId,
		int lineCount) {
		if (message == null || message.equals(""))
			throw new IllegalArgumentException("null msg");
		this.message = message;
		this.source = source;
		this.destination = destination;
		if (timeStamp < 0)
			throw new IllegalArgumentException("negative ts");
		this.timeStamp = timeStamp;
		this.isSender = isSender;
		this.firstLine = firstLine;
		this.statusMessage = statusMessage;
		this.tid = tid;
		this.callId = callId;
		this.debugLine = lineCount;
	}

	public String flush(long startTime) {
		String log;

		if (statusMessage != null) {
			log =
				"<message\nfrom=\""
					+ source
					+ "\" \nto=\""
					+ destination
					+ "\" \ntime=\""
					+ (timeStamp - startTime)
					+ "\" \nisSender=\""
					+ isSender
					+ "\" \nstatusMessage=\""
					+ statusMessage
					+ "\" \ntransactionId=\""
					+ tid
					+ "\" \ncallId=\""
					+ callId
					+ "\" \nfirstLine=\""
					+ firstLine.trim()
					+ "\" \ndebugLine=\""
					+ debugLine
					+ "\">\n";
			log += "<![CDATA[";
			log += message;
			log += "]]>\n";
			log += "</message>\n";
		} else {
			log =
				"<message\nfrom=\""
					+ source
					+ "\" \nto=\""
					+ destination
					+ "\" \ntime=\""
					+ (timeStamp - startTime)
					+ "\" \nisSender=\""
					+ isSender
					+ "\" \ntransactionId=\""
					+ tid
					+ "\" \ncallId=\""
					+ callId
					+ "\" \nfirstLine=\""
					+ firstLine.trim()
					+ "\" \ndebugLine=\""
					+ debugLine
					+ "\">\n";
			log += "<![CDATA[";
			log += message;
			log += "]]>\n";
			log += "</message>\n";
		}
		return log;
	}
	/**
	 * Get an XML String for this message
	 */

	public String flush() {
		String log;

		if (statusMessage != null) {
			log =
				"<message\nfrom=\""
					+ source
					+ "\" \nto=\""
					+ destination
					+ "\" \ntime=\""
					+ timeStamp
					+ "\" \nisSender=\""
					+ isSender
					+ "\" \nstatusMessage=\""
					+ statusMessage
					+ "\" \ntransactionId=\""
					+ tid
					+ "\" \nfirstLine=\""
					+ firstLine.trim()
					+ "\" \ncallId=\""
					+ callId
					+ "\" \ndebugLine=\""
					+ debugLine
					+ "\" \n>\n";
			log += "<![CDATA[";
			log += message;
			log += "]]>\n";
			log += "</message>\n";
		} else {
			log =
				"<message\nfrom=\""
					+ source
					+ "\" \nto=\""
					+ destination
					+ "\" \ntime=\""
					+ timeStamp
					+ "\" \nisSender=\""
					+ isSender
					+ "\" \ntransactionId=\""
					+ tid
					+ "\" \ncallId=\""
					+ callId
					+ "\" \nfirstLine=\""
					+ firstLine.trim()
					+ "\" \ndebugLine=\""
					+ debugLine
					+ "\" \n>\n";
			log += "<![CDATA[";
			log += message;
			log += "]]>\n";
			log += "</message>\n";
		}
		return log;
	}
}
/*
 * $Log: MessageLog.java,v $
 * Revision 1.5  2004/01/22 13:26:33  sverker
 * Issue number:
 * Obtained from:
 * Submitted by:  sverker
 * Reviewed by:   mranga
 *
 * Major reformat of code to conform with style guide. Resolved compiler and javadoc warnings. Added CVS tags.
 *
 * CVS: ----------------------------------------------------------------------
 * CVS: Issue number:
 * CVS:   If this change addresses one or more issues,
 * CVS:   then enter the issue number(s) here.
 * CVS: Obtained from:
 * CVS:   If this change has been taken from another system,
 * CVS:   then name the system in this line, otherwise delete it.
 * CVS: Submitted by:
 * CVS:   If this code has been contributed to the project by someone else; i.e.,
 * CVS:   they sent us a patch or a set of diffs, then include their name/email
 * CVS:   address here. If this is your work then delete this line.
 * CVS: Reviewed by:
 * CVS:   If we are doing pre-commit code reviews and someone else has
 * CVS:   reviewed your changes, include their name(s) here.
 * CVS:   If you have not had it reviewed then delete this line.
 *
 */
