package gov.nist.javax.sip.parser;
import java.text.ParseException;
import gov.nist.javax.sip.header.*;

/**
 * Parser for WWW authenitcate header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:32 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class WWWAuthenticateParser extends ChallengeParser {

	/**
	 * Constructor
	 * @param wwwAuthenticate -  message to parse
	 */
	public WWWAuthenticateParser(String wwwAuthenticate) {
		super(wwwAuthenticate);
	}

	/**
	 * Cosntructor
	 * @param  lexer - lexer to use.
	 */
	protected WWWAuthenticateParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message 
	 * @return SIPHeader (WWWAuthenticate object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {
		if (debug)
			dbg_enter("parse");
		try {
			headerName(TokenTypes.WWW_AUTHENTICATE);
			WWWAuthenticate wwwAuthenticate = new WWWAuthenticate();
			super.parse(wwwAuthenticate);
			return wwwAuthenticate;
		} finally {
			if (debug)
				dbg_leave("parse");
		}
	}
}
/*
 * $Log: WWWAuthenticateParser.java,v $
 * Revision 1.4  2004/01/22 13:26:32  sverker
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
