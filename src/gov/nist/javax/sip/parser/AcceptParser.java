package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for Accept header.
 *
 * @version  JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov> 
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class AcceptParser extends ParametersParser {

	/**
	 * Creates a new instance of Accept Parser
	 * @param accept  the header to parse 
	 */
	public AcceptParser(String accept) {
		super(accept);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected AcceptParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the Accept  String header
	 * @return SIPHeader (AcceptList  object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {

		if (debug)
			dbg_enter("AcceptParser.parse");
		AcceptList list = new AcceptList();

		try {
			headerName(TokenTypes.ACCEPT);

			Accept accept = new Accept();
			accept.setHeaderName(SIPHeaderNames.ACCEPT);

			this.lexer.SPorHT();
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			accept.setContentType(token.getTokenValue());
			this.lexer.match('/');
			this.lexer.match(TokenTypes.ID);
			token = lexer.getNextToken();
			accept.setContentSubType(token.getTokenValue());
			this.lexer.SPorHT();

			super.parse(accept);
			list.add(accept);

			while (lexer.lookAhead(0) == ',') {
				this.lexer.match(',');
				this.lexer.SPorHT();

				accept = new Accept();

				this.lexer.match(TokenTypes.ID);
				token = lexer.getNextToken();
				accept.setContentType(token.getTokenValue());
				this.lexer.match('/');
				this.lexer.match(TokenTypes.ID);
				token = lexer.getNextToken();
				accept.setContentSubType(token.getTokenValue());
				this.lexer.SPorHT();
				super.parse(accept);
				list.add(accept);

			}
			return list;
		} finally {
			if (debug)
				dbg_leave("AcceptParser.parse");
		}
	}
}
/*
 * $Log: AcceptParser.java,v $
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
