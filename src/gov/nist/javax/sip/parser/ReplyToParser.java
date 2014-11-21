package gov.nist.javax.sip.parser;

import java.text.ParseException;
import gov.nist.javax.sip.header.*;

/**
 * Parser for a list of RelpyTo headers.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class ReplyToParser extends AddressParametersParser {

	/**
	 * Creates a new instance of ReplyToParser 
	 * @param replyTo the header to parse
	 */
	public ReplyToParser(String replyTo) {
		super(replyTo);
	}

	/**
	 * Cosntructor
	 * param lexer the lexer to use to parse the header
	 */
	protected ReplyToParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message and generate the ReplyTo List Object
	 * @return SIPHeader the ReplyTo List object
	 * @throws SIPParseException if errors occur during the parsing
	 */
	public SIPHeader parse() throws ParseException {
		ReplyTo replyTo = new ReplyTo();
		if (debug)
			dbg_enter("ReplyTo.parse");

		try {
			headerName(TokenTypes.REPLY_TO);

			replyTo.setHeaderName(SIPHeaderNames.REPLY_TO);

			super.parse(replyTo);

			return replyTo;
		} finally {
			if (debug)
				dbg_leave("ReplyTo.parse");
		}

	}

	/**
	    public static void main(String args[]) throws ParseException {
	        String r[] = {
	            "Reply-To: Bob <sip:bob@biloxi.com>\n"
	        };
	        
	        for (int i = 0; i < r.length; i++ ) {
	            ReplyToParser rt =
	            new ReplyToParser(r[i]);
	            ReplyTo re = (ReplyTo) rt.parse();
	            System.out.println("encoded = " +re.encode());
	        }
	        
	    }
	*/
}
/*
 * $Log: ReplyToParser.java,v $
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
