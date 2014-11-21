package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for the challenge portion of the authentication header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author Olivier Deruelle  <deruelle@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */

public abstract class ChallengeParser extends HeaderParser {

	/**
	 * Constructor
	 * @param String challenge  message to parse to set
	 */
	protected ChallengeParser(String challenge) {
		super(challenge);
	}

	/**
	 * Constructor
	 * @param String challenge  message to parse to set
	 */
	protected ChallengeParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * Get the parameter of the challenge string
	 * @return NameValue containing the parameter
	 */
	protected void parseParameter(AuthenticationHeader header)
		throws ParseException {

		if (debug)
			dbg_enter("parseParameter");
		try {
			NameValue nv = this.nameValue('=');
			header.setParameter(nv);
		} finally {
			if (debug)
				dbg_leave("parseParameter");
		}

	}

	/**
	 * parser the String message
	 * @return Challenge object
	 * @throws ParseException if the message does not respect the spec.
	 */
	public void parse(AuthenticationHeader header) throws ParseException {

		// the Scheme:
		this.lexer.SPorHT();
		lexer.match(TokenTypes.ID);
		Token type = lexer.getNextToken();
		this.lexer.SPorHT();
		header.setScheme(type.getTokenValue());

		// The parameters:
		try {
			while (lexer.lookAhead(0) != '\n') {
				this.parseParameter(header);
				this.lexer.SPorHT();
				if (lexer.lookAhead(0) == '\n' || lexer.lookAhead(0) == '\0')
					break;
				this.lexer.match(',');
				this.lexer.SPorHT();
			}
		} catch (ParseException ex) {
			throw ex;
		}
	}
}
/*
 * $Log: ChallengeParser.java,v $
 * Revision 1.4  2004/01/22 13:26:31  sverker
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
