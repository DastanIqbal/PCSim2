/*
 * Reason.java
 * Reason            =  "Reason" HCOLON reason-value *(COMMA reason-value)
 * reason-value      =  protocol *(SEMI reason-params)
 * protocol          =  "SIP" / "Q.850" / token
 * reason-params     =  protocol-cause / reason-text
 *                     / reason-extension
 * protocol-cause    =  "cause" EQUAL cause
 * cause             =  1*DIGIT
 * reason-text       =  "text" EQUAL quoted-string
 * reason-extension  =  generic-param
 */

package gov.nist.javax.sip.header;

import java.text.ParseException;
import gov.nist.core.*;

/** 
 * Definition of the Reason SIP Header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class Reason
	extends ParametersHeader
	implements javax.sip.header.ReasonHeader {
	
	private static final long serialVersionUID = 1L;	

	public final String TEXT = ParameterNames.TEXT;
	public final String CAUSE = ParameterNames.CAUSE;

	protected String protocol;

	/** Get the cause token.
	 *@return the cause code.
	 */
	public int getCause() {
		return getParameterAsInt(CAUSE);
	}

	/**
	 * Set the cause.
	 *
	 *@param cause - cause to set.
	 */
	public void setCause(int cause) throws javax.sip.InvalidArgumentException {
		this.parameters.set(new NameValue("cause", new Integer(cause)));
	}

	/** Set the protocol
	 *
	 *@param protocol - protocol to set.
	 */

	public void setProtocol(String protocol) throws ParseException {
		this.protocol = protocol;
	}

	/** Return the protocol.
	 *
	 *@return the protocol.
	 */
	public String getProtocol() {
		return this.protocol;
	}

	/** Set the text.
	 *
	 *@param text -- string text to set.
	 */
	public void setText(String text) throws ParseException {
		this.parameters.set("text", text);
	}

	/** Get the text.
	 *
	 *@return text parameter.
	 *
	 */
	public String getText() {
		return this.parameters.getParameter("text");
	}

	/** Set the cause.
	
	/** Creates a new instance of Reason */
	public Reason() {
		super(NAME);
	}

	/** Gets the unique string name of this Header. A name constant is defined in
	 * each individual Header identifying each Header.
	 *
	 * @return the name of this specific Header
	 */
	public String getName() {
		return NAME;

	}

	/**
	 * Encode the body of this header (the stuff that follows headerName).
	 * A.K.A headerValue.
	 */
	protected String encodeBody() {
		StringBuffer s = new StringBuffer();
		s.append(protocol);
		if (parameters != null && !parameters.isEmpty())
			s.append(SEMICOLON).append(parameters.encode());
		return s.toString();
	}

}
/*
 * $Log: Reason.java,v $
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
