package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.address.*;
import java.text.ParseException;

/**
 * Parser for AlertInfo header.
 *
 * @version  JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  
 * @author M. Ranganathan <mranga@nist.gov>  
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class AlertInfoParser extends ParametersParser {

	/**
	 * Creates a new instance of AlertInfo Parser
	 * @param alertInfo  the header to parse 
	 */
	public AlertInfoParser(String alertInfo) {
		super(alertInfo);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected AlertInfoParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the AlertInfo  String header
	 * @return SIPHeader (AlertInfoList  object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {

		if (debug)
			dbg_enter("AlertInfoParser.parse");
		AlertInfoList list = new AlertInfoList();

		try {
			headerName(TokenTypes.ALERT_INFO);

			while (lexer.lookAhead(0) != '\n') {
				AlertInfo alertInfo = new AlertInfo();
				alertInfo.setHeaderName(SIPHeaderNames.ALERT_INFO);

				this.lexer.SPorHT();
				this.lexer.match('<');
				URLParser urlParser = new URLParser((Lexer) this.lexer);
				GenericURI uri = urlParser.uriReference();
				alertInfo.setAlertInfo(uri);
				this.lexer.match('>');
				this.lexer.SPorHT();

				super.parse(alertInfo);
				list.add(alertInfo);

				while (lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
					this.lexer.SPorHT();

					alertInfo = new AlertInfo();

					this.lexer.SPorHT();
					this.lexer.match('<');
					urlParser = new URLParser((Lexer) this.lexer);
					uri = urlParser.uriReference();
					alertInfo.setAlertInfo(uri);
					this.lexer.match('>');
					this.lexer.SPorHT();

					super.parse(alertInfo);
					list.add(alertInfo);
				}
			}
			return list;
		} finally {
			if (debug)
				dbg_leave("AlertInfoParser.parse");
		}
	}
}
/*
 * $Log: AlertInfoParser.java,v $
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
