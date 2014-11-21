/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import gov.nist.javax.sip.address.*;

/**
 * The Request-Route header is added to a request by any proxy that insists on
 * being in the path of subsequent requests for the same call leg.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 *@author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class RecordRoute
	extends AddressParametersHeader
	implements javax.sip.header.RecordRouteHeader {

	private static final long serialVersionUID = 1L;
	
	/**
	 * constructor
	 * @param address address to set
	 */
	public RecordRoute(AddressImpl address) {
		super(NAME);
		this.address = address;
	}

	/**
	 * default constructor
	 */
	public RecordRoute() {
		super(RECORD_ROUTE);

	}

	/** Encode into canonical form.
	 *@return String containing the canonicaly encoded header.
	 */
	public String encodeBody() {
		StringBuffer retval = new StringBuffer();
		if (address.getAddressType() == AddressImpl.ADDRESS_SPEC) {
			retval.append(LESS_THAN);
		}
		retval.append(address.encode());
		if (address.getAddressType() == AddressImpl.ADDRESS_SPEC) {
			retval.append(GREATER_THAN);
		}

		if (!parameters.isEmpty())
			retval.append(SEMICOLON + this.parameters.encode());
		return retval.toString();
	}
}
/*
 * $Log: RecordRoute.java,v $
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
