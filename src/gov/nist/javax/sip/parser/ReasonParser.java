package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for Reason header.
 *
 * @version  JAIN-SIP-1.1
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class ReasonParser extends ParametersParser {

	/**
	 * Creates a new instance of ReasonParser 
	 * @param reason the header to parse
	 */
	public ReasonParser(String reason) {
		super(reason);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected ReasonParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * @return SIPHeader (ReasonParserList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {
		ReasonList reasonList = new ReasonList();
		if (debug)
			dbg_enter("ReasonParser.parse");

		try {
			headerName(TokenTypes.REASON);
			this.lexer.SPorHT();
			while (lexer.lookAhead(0) != '\n') {
				Reason reason = new Reason();
				this.lexer.match(TokenTypes.ID);
				Token token = lexer.getNextToken();
				String value = token.getTokenValue();

				reason.setProtocol(value);
				super.parse(reason);
				reasonList.add(reason);
				if (lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
					this.lexer.SPorHT();
				} else
					this.lexer.SPorHT();

			}
		} catch (ParseException ex) {
			ex.printStackTrace();
			System.out.println(lexer.getRest());
		} finally {
			if (debug)
				dbg_leave("ReasonParser.parse");
		}

		return reasonList;
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	    String r[] = {
	        "Reason: SIP ;cause=200 ;text=\"Call completed elsewhere\"\n",
	        "Reason: Q.850 ;cause=16 ;text=\"Terminated\"\n",
	        "Reason: SIP ;cause=600 ;text=\"Busy Everywhere\"\n",
	        "Reason: SIP ;cause=580 ;text=\"Precondition Failure\","+
	        "SIP ;cause=530 ;text=\"Pre Failure\"\n",
	        "Reason: SIP \n"
	    };
	    
	    for (int i = 0; i < r.length; i++ ) {
	        ReasonParser parser =
	        new ReasonParser(r[i]);
	        ReasonList rl= (ReasonList) parser.parse();
	        System.out.println("encoded = " + rl.encode());
	    }    
	}
	 */
}
/*
 * $Log: ReasonParser.java,v $
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
