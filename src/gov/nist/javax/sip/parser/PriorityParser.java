package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for Priority header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * @version 1.0
 */
public class PriorityParser extends HeaderParser {

	/**
	 * Creates a new instance of PriorityParser 
	 * @param priority the header to parse 
	 */
	public PriorityParser(String priority) {
		super(priority);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected PriorityParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String header
	 * @return SIPHeader (Priority object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {

		if (debug)
			dbg_enter("PriorityParser.parse");
		Priority priority = new Priority();
		try {
			headerName(TokenTypes.PRIORITY);

			priority.setHeaderName(SIPHeaderNames.PRIORITY);

			this.lexer.SPorHT();
			this.lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();

			priority.setPriority(token.getTokenValue());

			this.lexer.SPorHT();
			this.lexer.match('\n');

			return priority;
		} finally {
			if (debug)
				dbg_leave("PriorityParser.parse");
		}
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	String p[] = {
	        "Priority: emergency\n"
	        };
		
	for (int i = 0; i < p.length; i++ ) {
	    PriorityParser parser = 
		  new PriorityParser(p[i]);
	    Priority prio= (Priority) parser.parse();
	    System.out.println("encoded = " + prio.encode());
	}		
	}
	 */
}
/*
 * $Log: PriorityParser.java,v $
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
