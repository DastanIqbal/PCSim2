/*
 * SessionDescriptionImpl.java
 *
 * Created on January 10, 2002, 3:11 PM
 */
package gov.nist.javax.sdp;

import java.util.*;
import javax.sdp.*;
import gov.nist.javax.sdp.fields.*;
import java.text.ParseException;

/**
 * Implementation of the SessionDescription interface.
 *
 *@version  JSR141-PUBLIC-REVIEW
 *
 *
 *@author Olivier Deruelle <deruelle@nist.gov>
 *@author M. Ranganathan <mranga@nist.gov> <br/>
 *
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class SessionDescriptionImpl implements SessionDescription {

	private static final long serialVersionUID = 1L;
	private TimeDescriptionImpl currentTimeDescription;
	private MediaDescriptionImpl currentMediaDescription;

	protected ProtoVersionField versionImpl;
	protected OriginField originImpl;
	protected SessionNameField sessionNameImpl;
	protected InformationField infoImpl;
	protected URIField uriImpl;
	protected ConnectionField connectionImpl;
	protected KeyField keyImpl;

	protected Vector<TimeDescriptionImpl> timeDescriptions;
	protected Vector<MediaDescriptionImpl> mediaDescriptions;

	protected Vector<ZoneField> zoneAdjustments;
	protected Vector<EmailField> emailList;
	protected Vector<PhoneField> phoneList;
	protected Vector<BandwidthField> bandwidthList;
	protected Vector<AttributeField> attributesList;

	/** Creates new SessionDescriptionImpl */
	public SessionDescriptionImpl() {
		zoneAdjustments = new Vector<ZoneField>();
		emailList = new Vector<EmailField>();
		phoneList = new Vector<PhoneField>();
		bandwidthList = new Vector<BandwidthField>();
		timeDescriptions = new Vector<TimeDescriptionImpl>();
		mediaDescriptions = new Vector<MediaDescriptionImpl>();
		// Bug reported and fixed by Steve Crossley
		attributesList = new Vector<AttributeField>();

	}

	public void addField(SDPField sdpField) throws ParseException {
		try {
			if (sdpField instanceof ProtoVersionField) {
				versionImpl = (ProtoVersionField) sdpField;
			} else if (sdpField instanceof OriginField) {
				originImpl = (OriginField) sdpField;
			} else if (sdpField instanceof SessionNameField) {
				sessionNameImpl = (SessionNameField) sdpField;
			} else if (sdpField instanceof InformationField) {
				if (currentMediaDescription != null)
					currentMediaDescription.setInformationField(
						(InformationField) sdpField);
				else
					this.infoImpl = (InformationField) sdpField;
			} else if (sdpField instanceof URIField) {
				uriImpl = (URIField) sdpField;
			} else if (sdpField instanceof ConnectionField) {
				if (currentMediaDescription != null)
					currentMediaDescription.setConnectionField(
						(ConnectionField) sdpField);
				else
					this.connectionImpl = (ConnectionField) sdpField;
			} else if (sdpField instanceof KeyField) {
				if (currentMediaDescription != null)
					currentMediaDescription.setKey((KeyField) sdpField);
				else
					keyImpl = (KeyField) sdpField;
			} else if (sdpField instanceof EmailField) {
				emailList.add((EmailField)sdpField);
			} else if (sdpField instanceof PhoneField) {
				phoneList.add((PhoneField)sdpField);
			} else if (sdpField instanceof TimeField) {
				currentTimeDescription =
					new TimeDescriptionImpl((TimeField) sdpField);
				timeDescriptions.add(currentTimeDescription);
			} else if (sdpField instanceof RepeatField) {
				if (currentTimeDescription == null) {
					throw new ParseException("no time specified", 0);
				} else {
					currentTimeDescription.addRepeatField(
						(RepeatField) sdpField);
				}
			} else if (sdpField instanceof ZoneField) {
				zoneAdjustments.add((ZoneField)sdpField);
			} else if (sdpField instanceof BandwidthField) {
				if (currentMediaDescription != null)
					currentMediaDescription.addBandwidthField(
						(BandwidthField) sdpField);
				else
					bandwidthList.add((BandwidthField)sdpField);
			} else if (sdpField instanceof AttributeField) {
				if (currentMediaDescription != null) {
					AttributeField af = (AttributeField) sdpField;
;
					// Bug report from Andreas Bystr�m
					currentMediaDescription.addAttribute(
						(AttributeField) af);
				} else {
					attributesList.add((AttributeField)sdpField);
				}

			} else if (sdpField instanceof MediaField) {
				currentMediaDescription = new MediaDescriptionImpl();
				mediaDescriptions.add(currentMediaDescription);
				// Bug report from Andreas Bystr�m
				currentMediaDescription.setMediaField((MediaField) sdpField);
			}
		} catch (SdpException ex) {
			throw new ParseException(sdpField.encode(), 0);
		}
	}

	/** Public clone declaration.
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */
	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {
		Class<?> myClass = this.getClass();
		SessionDescriptionImpl hi;
		try {
			hi = (SessionDescriptionImpl) myClass.newInstance();
		} catch (InstantiationException ex) {
			return null;
		} catch (IllegalAccessException ex) {
			return null;
		}

		hi.versionImpl = (ProtoVersionField) this.versionImpl.clone();
		hi.originImpl = (OriginField) this.originImpl.clone();
		hi.sessionNameImpl = (SessionNameField) this.sessionNameImpl.clone();

		// Was bombing out with null pointer. Steve Crossley sent in a 
		// fix.
		if (this.infoImpl != null) {
			hi.infoImpl = (InformationField) this.infoImpl.clone();
		}
		if (this.uriImpl != null) {
			hi.uriImpl = (URIField) this.uriImpl.clone();
		}
		if (this.connectionImpl != null) {
			hi.connectionImpl = (ConnectionField) this.connectionImpl.clone();
		}
		if (this.keyImpl != null) {
			hi.keyImpl = (KeyField) this.keyImpl.clone();
		}

		hi.timeDescriptions = (Vector<TimeDescriptionImpl>) this.timeDescriptions.clone();

		hi.emailList = (Vector<EmailField>) this.emailList.clone();
		hi.phoneList = (Vector<PhoneField>) this.phoneList.clone();
		hi.zoneAdjustments = (Vector<ZoneField>) this.zoneAdjustments.clone();
		hi.bandwidthList = (Vector<BandwidthField>) this.bandwidthList.clone();
		hi.attributesList = (Vector<AttributeField>) this.attributesList.clone();
		hi.mediaDescriptions = (Vector<MediaDescriptionImpl>) this.mediaDescriptions.clone();
		return hi;
	}

	/** Returns the version of SDP in use.
	 * This corresponds to the v= field of the SDP data.
	 * @return the integer version (-1 if not set).
	 */
	public Version getVersion() {
		return versionImpl;
	}

	/** Sets the version of SDP in use.
	 * This corresponds to the v= field of the SDP data.
	 * @param v version - the integer version.
	 * @throws SdpException if the version is null
	 */
	public void setVersion(Version v) throws SdpException {
		if (v == null)
			throw new SdpException("The parameter is null");
		if (v instanceof ProtoVersionField) {
			versionImpl = (ProtoVersionField) v;
		} else
			throw new SdpException("The parameter must be an instance of VersionField");
	}

	/** Returns information about the originator of the session.
	 * This corresponds to the o= field  of the SDP data.
	 * @return the originator data.
	 */
	public Origin getOrigin() {
		return originImpl;
	}

	/** Sets information about the originator of the session.
	 * This corresponds to the o= field of the SDP data.
	 * @param origin origin - the originator data.
	 * @throws SdpException if the origin is null
	 */
	public void setOrigin(Origin origin) throws SdpException {
		if (origin == null)
			throw new SdpException("The parameter is null");
		if (origin instanceof OriginField) {
			OriginField o = (OriginField) origin;
			originImpl = o;
		} else
			throw new SdpException("The parameter must be an instance of OriginField");
	}

	/** Returns the name of the session.
	 * This corresponds to the s= field of the SDP data.
	 * @return the session name.
	 */
	public SessionName getSessionName() {
		return sessionNameImpl;
	}

	/** Sets the name of the session.
	 * This corresponds to the s= field of the SDP data.
	 * @param sessionName name - the session name.
	 * @throws SdpException if the sessionName is null
	 */
	public void setSessionName(SessionName sessionName) throws SdpException {
		if (sessionName == null)
			throw new SdpException("The parameter is null");
		if (sessionName instanceof SessionNameField) {
			SessionNameField s = (SessionNameField) sessionName;
			sessionNameImpl = s;
		} else
			throw new SdpException("The parameter must be an instance of SessionNameField");
	}

	/** Returns value of the info field (i=) of this object.
	 * @return info
	 */
	public Info getInfo() {
		return infoImpl;
	}

	/** Sets the i= field of this object.
	 * @param i s - new i= value; if null removes the field
	 * @throws SdpException if the info is null
	 */
	public void setInfo(Info i) throws SdpException {
		if (i == null)
			throw new SdpException("The parameter is null");
		if (i instanceof InformationField) {
			InformationField info = (InformationField) i;
			infoImpl = info;
		} else
			throw new SdpException("The parameter must be an instance of InformationField");
	}

	/** Returns a uri to the location of more details about the session.
	 * This corresponds to the u=
	 *     field of the SDP data.
	 * @return the uri.
	 */
	public URI getURI() {
		return uriImpl;
	}

	/** Sets the uri to the location of more details about the session. This
	 * corresponds to the u=
	 *     field of the SDP data.
	 * @param uri uri - the uri.
	 * @throws SdpException if the uri is null
	 */
	public void setURI(URI uri) throws SdpException {
		if (uri == null)
			throw new SdpException("The parameter is null");
		if (uri instanceof URIField) {
			URIField u = (URIField) uri;
			uriImpl = u;
		} else
			throw new SdpException("The parameter must be an instance of URIField");
	}

	/** Returns an email address to contact for further information about the session.
	 * This corresponds to the e= field of the SDP data.
	 * @param create boolean to set
	 * @throws SdpException
	 * @return the email address.
	 */
	public Vector<EmailField> getEmails(boolean create) throws SdpParseException {
		if (emailList == null) {
			if (create)
				emailList = new Vector<EmailField>();
		}
		return emailList;
	}

	/** Sets a an email address to contact for further information about the session.
	 * This corresponds to the e= field of the SDP data.
	 * @param emails email - the email address.
	 * @throws SdpException if the vector is null
	 */
	public void setEmails(Vector<EmailField> emails) throws SdpException {
		if (emails == null)
			throw new SdpException("The parameter is null");
		else
			emailList = emails;
	}

	/** Returns a phone number to contact for further information about the session. This
	 *     corresponds to the p= field of the SDP data.
	 * @param create boolean to set
	 * @throws SdpException
	 * @return the phone number.
	 */
	public Vector<PhoneField> getPhones(boolean create) throws SdpException {
		if (phoneList == null) {
			if (create)
				phoneList = new Vector<PhoneField>();
		}
		return phoneList;
	}

	/** Sets a phone number to contact for further information about the session. This
	 *     corresponds to the p= field of the SDP data.
	 * @param phones phone - the phone number.
	 * @throws SdpException if the vector is null
	 */
	public void setPhones(Vector<PhoneField> phones) throws SdpException {
		if (phones == null)
			throw new SdpException("The parameter is null");
		else
			phoneList = phones;
	}

	/** Returns a TimeField indicating the start, stop, repetition and time zone
	 * information of the
	 *     session. This corresponds to the t= field of the SDP data.
	 * @param create boolean to set
	 * @throws SdpException
	 * @return the Time Field.
	 */
	public Vector<TimeDescriptionImpl> getTimeDescriptions(boolean create) throws SdpException {
		if (timeDescriptions == null) {
			if (create)
				timeDescriptions = new Vector<TimeDescriptionImpl>();
		}
		return timeDescriptions;
	}

	/** Sets a TimeField indicating the start, stop, repetition and time zone
	 * information of the
	 *     session. This corresponds to the t= field of the SDP data.
	 * @param times time - the TimeField.
	 * @throws SdpException if the vector is null
	 */
	public void setTimeDescriptions(Vector<TimeDescriptionImpl> times) throws SdpException {
		if (times == null)
			throw new SdpException("The parameter is null");
		else {
			timeDescriptions = times;
		}
	}

	/** Returns the time zone adjustments for the Session
	 * @param create boolean to set
	 * @throws SdpException
	 * @return a Hashtable containing the zone adjustments, where the key is the
	 * Adjusted Time
	 *          Zone and the value is the offset.
	 */
	public Vector<ZoneField> getZoneAdjustments(boolean create) throws SdpException {
		if (zoneAdjustments == null) {
			if (create)
				zoneAdjustments = new Vector<ZoneField>();
		}
		return zoneAdjustments;
	}

	/** Sets the time zone adjustment for the TimeField.
	 * @param zoneAdjustments zoneAdjustments - a Hashtable containing the zone
	 * adjustments, where the key
	 *          is the Adjusted Time Zone and the value is the offset.
	 * @throws SdpException if the vector is null
	 */
	public void setZoneAdjustments(Vector<ZoneField> zoneAdjustments)
		throws SdpException {
		if (zoneAdjustments == null)
			throw new SdpException("The parameter is null");
		else
			this.zoneAdjustments = zoneAdjustments;
	}

	/** Returns the connection information associated with this object. This may
	 * be null for SessionDescriptions if all Media objects have a connection
	 * object and may be null
	 *     for Media objects if the corresponding session connection is non-null.
	 * @return connection
	 */
	public Connection getConnection() {
		return connectionImpl;
	}

	/** Set the connection data for this entity.
	 * @param conn to set
	 * @throws SdpException if the parameter is null
	 */
	public void setConnection(Connection conn) throws SdpException {
		if (conn == null)
			throw new SdpException("The parameter is null");
		if (conn instanceof ConnectionField) {
			ConnectionField c = (ConnectionField) conn;
			connectionImpl = c;
		} else
			throw new SdpException("Bad implementation class ConnectionField");
	}

	/** Returns the Bandwidth of the specified type.
	 * @param create type - type of the Bandwidth to return
	 * @return the Bandwidth or null if undefined
	 */
	public Vector<BandwidthField> getBandwidths(boolean create) {
		if (bandwidthList == null) {
			if (create)
				bandwidthList = new Vector<BandwidthField>();
		}
		return bandwidthList;
	}

	/** set the value of the Bandwidth with the specified type.
	 * @param bandwidthList to set
	 * @throws SdpException if the vector is null
	 */
	public void setBandwidths(Vector<BandwidthField> bandwidthList) throws SdpException {
		if (bandwidthList == null)
			throw new SdpException("The parameter is null");
		else
			this.bandwidthList = bandwidthList;
	}

	/** Returns the integer value of the specified bandwidth name.
	 * @param name name - the name of the bandwidth type
	 * @throws SdpParseException
	 * @return the value of the named bandwidth
	 */
	public int getBandwidth(String name) throws SdpParseException {
		if (name == null)
			return -1;
		else if (bandwidthList == null)
			return -1;
		for (int i = 0; i < bandwidthList.size(); i++) {
			Object o = bandwidthList.elementAt(i);
			if (o instanceof BandwidthField) {
				BandwidthField b = (BandwidthField) o;
				String type = b.getType();
				if (type != null) {
					if (name.equals(type)) {
						return b.getValue();
					}
				}
			}
		}
		return -1;
	}

	/** Sets the value of the specified bandwidth type.
	 * @param name name - the name of the bandwidth type.
	 * @param value value - the value of the named bandwidth type.
	 * @throws SdpException if the name is null
	 */
	public void setBandwidth(String name, int value) throws SdpException {
		if (name == null)
			throw new SdpException("The parameter is null");
		else if (bandwidthList != null) {
			for (int i = 0; i < bandwidthList.size(); i++) {
				Object o = bandwidthList.elementAt(i);
				if (o instanceof BandwidthField) {
					BandwidthField b = (BandwidthField) o;
					String type = b.getType();
					if (type != null) {
						if (name.equals(type)) {
							b.setValue(value);
						}
					}
				}
			}
		}
	}

	/** Removes the specified bandwidth type.
	 * @param name name - the name of the bandwidth type
	 */
	public void removeBandwidth(String name) {
		if (name != null)
			if (bandwidthList != null) {
				for (int i = 0; i < bandwidthList.size(); i++) {
					Object o = bandwidthList.elementAt(i);
					if (o instanceof BandwidthField) {
						BandwidthField b = (BandwidthField) o;
						try {
							String type = b.getType();
							if (type != null) {
								if (name.equals(type)) {
									bandwidthList.remove(b);
								}
							}
						} catch (SdpParseException e) {
						}
					}
				}
			}
	}

	/** Returns the key data.
	 * @return key
	 */
	public Key getKey() {
		return keyImpl;
	}

	/** Sets encryption key information.
	 * This consists of a method and an encryption key included inline.
	 * @param key key - the encryption key data; depending on method may be null
	 * @throws SdpException if the parameter is null
	 */
	public void setKey(Key key) throws SdpException {
		if (key == null)
			throw new SdpException("The parameter is null");
		if (key instanceof KeyField) {
			KeyField k = (KeyField) key;
			keyImpl = k;
		} else
			throw new SdpException("The parameter must be an instance of KeyField");
	}

	/** Returns the value of the specified attribute.
	 * @param name name - the name of the attribute
	 * @throws SdpParseException
	 * @return the value of the named attribute
	 */
	public String getAttribute(String name) throws SdpParseException {
		if (name == null)
			return null;
		else if (attributesList == null)
			return null;
		for (int i = 0; i < attributesList.size(); i++) {
			Object o = attributesList.elementAt(i);
			if (o instanceof AttributeField) {
				AttributeField a = (AttributeField) o;
				String n = a.getName();
				if (n != null) {
					if (name.equals(n)) {
						return a.getValue();
					}
				}
			}
		}
		return null;
	}

	/** Returns the set of attributes for this Description as a Vector of Attribute
	 * objects in the
	 *     order they were parsed.
	 * @param create create - specifies whether to return null or a new empty
	 * Vector in case no
	 *          attributes exists for this Description
	 * @return attributes for this Description
	 */
	public Vector<AttributeField> getAttributes(boolean create) {
		if (attributesList == null) {
			if (create)
				attributesList = new Vector<AttributeField>();
		}
		return attributesList;
	}

	/** Removes the attribute specified by the value parameter.
	 * @param name name - the name of the attribute
	 */
	public void removeAttribute(String name) {
		if (name != null)
			if (attributesList != null) {
				for (int i = 0; i < attributesList.size(); i++) {
					Object o = attributesList.elementAt(i);
					if (o instanceof AttributeField) {
						AttributeField a = (AttributeField) o;
						try {
							String n = a.getName();
							if (n != null) {
								if (name.equals(n)) {
									attributesList.remove(a);
								}
							}
						} catch (SdpParseException e) {
						}

					}
				}
			}
	}

	/** Sets the value of the specified attribute.
	 * @param name name - the name of the attribute.
	 * @param value value - the value of the named attribute.
	 * @throws SdpException if the name or the value is null
	 */
	public void setAttribute(String name, String value) throws SdpException {
		if (name == null || value == null)
			throw new SdpException("The parameter is null");
		else if (attributesList != null) {
			for (int i = 0; i < attributesList.size(); i++) {
				Object o = attributesList.elementAt(i);
				if (o instanceof AttributeField) {
					AttributeField a = (AttributeField) o;
					String n = a.getName();
					if (n != null) {
						if (name.equals(n)) {
							a.setValue(value);
						}
					}
				}
			}
		}
	}

	/** Adds the specified Attribute to this Description object.
	 * @param attributes - the attribute to add
	 * @throws SdpException if the vector is null
	 */
	public void setAttributes(Vector<AttributeField> attributes) throws SdpException {
		if (attributes == null)
			throw new SdpException("The parameter is null");
		else
			attributesList = attributes;
	}

	/** Adds a MediaDescription to the session description.
	 * These correspond to the m=
	 *    fields of the SDP data.
	 * @param create boolean to set
	 * @throws SdpException
	 * @return media - the field to add.
	 */
	public Vector<MediaDescriptionImpl> getMediaDescriptions(boolean create) throws SdpException {
		if (mediaDescriptions == null) {
			if (create)
				mediaDescriptions = new Vector<MediaDescriptionImpl>();
		}
		return mediaDescriptions;
	}

	/** Removes all MediaDescriptions from the session description.
	 * @param mediaDescriptions to set
	 * @throws SdpException if the parameter is null
	 */
	public void setMediaDescriptions(Vector<MediaDescriptionImpl> mediaDescriptions)
		throws SdpException {
		if (mediaDescriptions == null)
			throw new SdpException("The parameter is null");
		else
			this.mediaDescriptions = mediaDescriptions;
	}

	private String encodeVector(Vector<?> vector) {
		StringBuffer encBuff = new StringBuffer();

		for (int i = 0; i < vector.size(); i++)
			encBuff.append(vector.elementAt(i));

		return encBuff.toString();
	}

	/**
	 * Returns the canonical string representation of the
	 * current SessionDescrption. Acknowledgement - this code
	 * was contributed by Emil Ivov.
	 *
	 * @return Returns the canonical string representation
	 * of the current SessionDescrption.
	 */

	public String toString() {
		StringBuffer encBuff = new StringBuffer();

		//Encode single attributes
		encBuff.append(getVersion() == null ? "" : getVersion().toString());
		encBuff.append(getOrigin() == null ? "" : getOrigin().toString());
		encBuff.append(
			getSessionName() == null ? "" : getSessionName().toString());
		encBuff.append(getInfo() == null ? "" : getInfo().toString());

		//Encode attribute vectors
		try {
			encBuff.append(getURI() == null ? "" : getURI().toString());
			encBuff.append(encodeVector(getEmails(true)));
			encBuff.append(encodeVector(getPhones(true)));
			encBuff.append(
				getConnection() == null ? "" : getConnection().toString());
			encBuff.append(encodeVector(getBandwidths(true)));
			encBuff.append(encodeVector(getTimeDescriptions(true)));
			encBuff.append(encodeVector(getZoneAdjustments(true)));
			encBuff.append(getKey() == null ? "" : getKey().toString());
			encBuff.append(encodeVector(getAttributes(true)));
			encBuff.append(encodeVector(getMediaDescriptions(true)));
			//adds the final crlf
		} catch (SdpException exc) {
			//add exception handling if necessary
		}
		return encBuff.toString();
	}

}
/*
 * $Log: SessionDescriptionImpl.java,v $
 * Revision 1.6  2004/12/13 02:43:44  mranga
 * Submitted by:  mranga
 * Fix documentation bugs. Update change list.
 *
 * Revision 1.5  2004/01/22 13:26:27  sverker
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
