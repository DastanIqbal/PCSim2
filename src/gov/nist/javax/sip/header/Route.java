/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 ******************************************************************************/
package gov.nist.javax.sip.header;

import gov.nist.javax.sip.address.*;

/**
 * Route  SIPHeader Object
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class Route
	extends AddressParametersHeader
	implements javax.sip.header.RouteHeader {

	private static final long serialVersionUID = 1L;
	
	/** Default constructor
	 */
	public Route() {
		super(NAME);
	}

	/** Default constructor given an address.
	 *
	 *@param address -- address of this header.
	 *
	 */

	public Route(AddressImpl address) {
		super(NAME);
		this.address = address;
	}

	/**
	 * Equality predicate.
	 * Two routes are equal if their addresses are equal.
	 *
	 *@param that is the other object to compare with.
	 *@return true if the route addresses are equal.
	 */
	public boolean equals(Object that) {
		if (!this.getClass().equals(that.getClass()))
			return false;
		Route thatRoute = (Route) that;
		return this.address.getHostPort().equals(
			thatRoute.address.getHostPort());
	}

	/**
	 * Hashcode so this header can be inserted into a set.
	 *
	 *@return the hashcode of the encoded address.
	 */
	public int hashCode() {
		return this.address.getHostPort().encode().toLowerCase().hashCode();
	}

	/**
	 * Encode into canonical form.
	 * Acknowledgement: contains a bug fix for a bug reported by
	 * Laurent Schwizer
	 *
	 *@return a canonical encoding of the header.
	 */
	public String encodeBody() {
		boolean addrFlag = address.getAddressType() == AddressImpl.NAME_ADDR;
		StringBuffer encoding = new StringBuffer();
		if (!addrFlag) {
			encoding.append("<").append(address.encode()).append(">");
		} else {
			encoding.append(address.encode());
		}
		if (!parameters.isEmpty()) {
			encoding.append(SEMICOLON).append(parameters.encode());
		}
		return encoding.toString();
	}
}
/*
 * $Log: Route.java,v $
 * Revision 1.2  2004/01/22 13:26:29  sverker
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
