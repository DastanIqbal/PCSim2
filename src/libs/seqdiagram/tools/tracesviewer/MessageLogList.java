/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/

package tools.tracesviewer;

import java.util.*;

/**
*This class stores a sorted list messages for logging.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>
*@author Marc Bednarek
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public class MessageLogList extends TreeSet<TracesMessage> {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
//	private String logFileName;
	protected String description;
	protected static long startTime;

	static {
		startTime = -1;
	}

	/** Constructor.
	*@param comp comparator for sorting the logs
	*/
	public MessageLogList(Comparator<TracesMessage> comp) {
		super(comp);
	}

	/** set a descriptive string for this log (for id purposes).
	 *@param description is the decriptive string to add.
	 */
	public void addDescription(String description) {
		this.description = description;
	}

	/** Constructor given callId and a comparator.
	*@param callId is the call id for which to store the log.
	*@param comp is the comparator to sort the log records.
	*/

	public MessageLogList(String callId, Comparator<TracesMessage> comp) {
		super(comp);
	}

	/** Add a comparable object to the messgageLog
	*@param obj is the comparable object to add to the message log.
	*/
	@Override
	public synchronized boolean add(TracesMessage obj) {
		TracesMessage log = obj;
		long ts =log.getTime();
		if (ts < startTime || startTime < 0)
			startTime = ts;
		return super.add(obj);
	}

}
