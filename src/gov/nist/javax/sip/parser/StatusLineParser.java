package gov.nist.javax.sip.parser;
import gov.nist.javax.sip.header.*;
import java.text.ParseException;

/**
 * Parser for the SIP status line.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:32 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class StatusLineParser extends Parser {
	public StatusLineParser(String statusLine) {
		this.lexer = new Lexer("status_lineLexer", statusLine);
	}

	public StatusLineParser(Lexer lexer) {
		this.lexer = lexer;
		this.lexer.selectLexer("status_lineLexer");
	}

	protected int statusCode() throws ParseException {
		String scode = this.lexer.number();
		if (debug)
			dbg_enter("statusCode");
		try {
			int retval = Integer.parseInt(scode);
			return retval;
		} catch (NumberFormatException ex) {
			throw new ParseException(
				lexer.getBuffer() + ":" + ex.getMessage(),
				lexer.getPtr());
		} finally {
			if (debug)
				dbg_leave("statusCode");
		}

	}

	protected String reasonPhrase() throws ParseException {
		return this.lexer.getRest().trim();
	}

	public StatusLine parse() throws ParseException {
		try {
			if (debug)
				dbg_enter("parse");
			StatusLine retval = new StatusLine();
			String version = this.sipVersion();
			retval.setSipVersion(version);
			lexer.SPorHT();
			int scode = statusCode();
			retval.setStatusCode(scode);
			lexer.SPorHT();
			String rp = reasonPhrase();
			retval.setReasonPhrase(rp);
			lexer.SPorHT();
			return retval;
		} finally {
			if (debug)
				dbg_leave("parse");
		}
	}

	/**
		public static void main(String[] args)  throws ParseException {
			String[] statusLines = {
			 "SIP/2.0 200 OK\n",
			 "BOO 200 OK\n",
			 "SIP/2.0 500 OK bad things happened \n"
			};
			for (int i = 0 ; i < statusLines.length; i++) {
			   try {
			   StatusLineParser slp = new StatusLineParser(statusLines[i]);
			   StatusLine sl = slp.parse();
			   System.out.println("encoded = " + sl.encode());
			   } catch (ParseException ex) {
				System.out.println("error message " + ex.getMessage());
			   }
			}
		}
	*/
}
/*
 * $Log: StatusLineParser.java,v $
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
