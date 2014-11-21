/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;
import javax.sip.header.*;

/**
* ContentLanguage list of headers. (Should this be a list?)
*/
public final class ContentLanguageList extends SIPHeaderList {
 
	private static final long serialVersionUID = 1L;
	
        /** Default constructor
         */    
	public ContentLanguageList () {
		super(ContentLanguage.class,
			ContentLanguageHeader.NAME);
	}
        
}
