/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 *******************************************************************************/
package tools.tracesviewer;

import java.util.Comparator;

/** A class that is used for comparing log records.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

// PC 2.0 make class public
public class LogComparator implements Comparator<TracesMessage> {
	@Override
	public int compare(TracesMessage m1, TracesMessage m2) {
		
	    return m1.sequencer - m2.sequencer;
	    
//			long ts1 = m1.getTime();
//			long ts2 = m2.getTime();
//			
//			if (ts1 < ts2)
//				return -1;
//			else if (ts1 > ts2)
//				return 1;
//			else {
//				// Bug fix contributed by Pierre Sandström
//				return  m1 != m2 ? 1: 0;
//			}
	}

	@Override
	public boolean equals(Object obj2) {
		return super.equals(obj2);

	}

}
