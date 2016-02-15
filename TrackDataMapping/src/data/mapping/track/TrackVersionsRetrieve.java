
package data.mapping.track;

import yedda.utility.jdbc.WinConnectionUtil;
import data.mapping.config.AppConfig;
import data.mapping.config.ConfigPropertyList;

import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.io.File;
import java.io.FilenameFilter;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Retrieve track versions info from XML file using SAX parser
 */
public class TrackVersionsRetrieve {
	private static final Log log = LogFactory.getLog("map");
	private AppConfig config;
	private java.sql.Connection myConn;
	private SAXParser sp;
	private TrackVersionXMLReader reader;

	public TrackVersionsRetrieve() throws Exception {
		try {
			config = AppConfig.getInstance();
			config.load(ConfigPropertyList.MUSICSRC.getPath());

			try {
				myConn = WinConnectionUtil.getMysqlDBConnection(
					config.getString("db.ip", config.DEFAULT_DB_IP),
					config.getInt("db.port", config.DEFAULT_DB_PORT),
					config.getString("db.name", config.DEFAULT_DB_TABLE),
					config.getString("db.user", config.DEFAULT_DB_USER),
					config.getString("db.psw", "")
				);
			} catch (SQLException s) {
				if (log.isErrorEnabled())
					log.error("Can't connect database.", s);
				throw s;
			}

			SAXParserFactory sf = SAXParserFactory.newInstance();
			sp = sf.newSAXParser();
			reader = new TrackVersionXMLReader(myConn);
		} catch (ConfigurationException e) {
			if (log.isErrorEnabled())
				log.error("Can't load properties:" + ConfigPropertyList.MUSICSRC.getPath(), e);
			throw e;
		} catch (Exception s) {
			throw s;
		}
	}

	public void startProcess() throws Exception {
		if (log.isInfoEnabled())
			log.info("Start of process.");

		log.debug(config.getString("disc.xml.root", "N/A"));
		File xmlRoot = new File(config.getString("disc.xml.root", ""));
		if (!xmlRoot.exists()) {
			if (log.isErrorEnabled())
				log.error(MessageFormat.format("source root[{0}] doesn't exists..",
						config.getString("disc.xml.root", "")));
			return;
		}

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.startsWith(".") && name.endsWith(".xml");
			}
		};

		File[] flist = xmlRoot.listFiles(filter);
		if (flist == null) {
			if (log.isWarnEnabled())
				log.warn("There is no xml files. Stop process.");
			return;
		}

		if (log.isInfoEnabled())
			log.info("total " + flist.length + " xml Files.");

		for (int i = 0; i < flist.length; i++) {
			// Get filename of file or directory
			if (log.isInfoEnabled())
				log.info("Current File:" + flist[i].getPath());

			try {
				sp.parse(flist[i], reader);
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("[XMLParsingError] " + flist[i], e);
			}
		}

		if (log.isInfoEnabled())
			log.info("End of process.");

	}

	public void destroy() {
		if (config != null) {
			config.clear();
		}
		if (myConn != null) {
			try {
				myConn.close();
			} catch (SQLException ignore) {
			}
		}
		if (reader != null) {
			reader.destroy();
			reader = null;
		}
		sp = null;
	}

	/**
	 * Main entry
	 * @param args
	 */
	public static void main(String[] args) {
		if (log.isInfoEnabled())
			log.info("Start of TrackVersionsRetrieve");
		try {
			TrackVersionsRetrieve retriver = new TrackVersionsRetrieve();
			retriver.startProcess();
			retriver.destroy();
			retriver = null;

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (log.isInfoEnabled())
			log.info("End of TrackVersionsRetrieve");
	}

}

/**
 * Handle XML content and add to mysql database
 */
class TrackVersionXMLReader extends DefaultHandler {
	private static final Log log = LogFactory.getLog("map");
	private AppConfig config;
	private java.sql.Connection myConn;
	private int rootTrackID = 0;
	private int curDiscID = 0;
	private boolean isVersion = false;

	public TrackVersionXMLReader(java.sql.Connection conn) throws Exception {
		super();
		this.myConn = conn;
	}

	public void destroy() {}

	/* Receive notification of character data inside an element. */
	public void characters(char[] ch, int start, int length) {}

	/* Receive notification of the beginning of the document. */
	public void startDocument() {}

	/* Receive notification of the end of the document. */
	public void endDocument() {}

	/* Receive notification of the start of an element. */
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (qName.equals("Disc")) {
			if (attributes.getValue("DiscID") != null) {
				curDiscID = Integer.parseInt(attributes.getValue("DiscID"));
			}
		}

		if (qName.equals("Title")) {
			// System.out.println("start Tile:"+qName+"
			// TrackID:"+attributes.getValue("TrackID"));
			if (attributes.getValue("TrackID") != null) {
				rootTrackID = Integer.parseInt(attributes.getValue("TrackID"));
				this.saveRecords(0, rootTrackID);
			} else {
				if (log.isErrorEnabled())
					log.error("[TrackID not exists] DiscID:" + curDiscID + " TrackNo:" + attributes.getValue("TrackNo"));
			}
		}

		if (!isVersion && qName.equals("Versions")) {
			// System.out.println("start Versions");
			isVersion = true;
		}

		if (isVersion && qName.equals("Version")) {
			if (attributes.getValue("TrackID") != null) {
				int tid = Integer.parseInt(attributes.getValue("TrackID"));
				// save
				this.saveRecords(rootTrackID, tid);
			} else {
				if (log.isErrorEnabled())
					log.error("[TrackID not exists] DiscID:" + curDiscID + " rootTrackID:" + rootTrackID + " TrackNo:"
							+ attributes.getValue("TrackNo"));
			}
		}

	}

	/* Receive notification of the end of an element. */
	public void endElement(String uri, String localName, String qName) {
		if (qName.equals("Disc")) {
			curDiscID = 0;
		}
		if (qName.equals("Title")) {
			rootTrackID = 0;
		}
		if (qName.equals("Versions")) {
			// System.out.println("start Versions:"+qName);
			isVersion = false;
		}
	}

	/* Receive notification of the start of a Namespace mapping. */
	public void startPrefixMapping(String prefix, String uri) {
		System.out.println("start prefix:" + prefix + " uri:" + uri);
	}

	/* Receive notification of the end of a Namespace mapping. */
	public void endPrefixMapping(String prefix) {
		System.out.println("end prefix:" + prefix);
	}

	/* Receive notification of a recoverable parser error. */
	public void error(SAXParseException e) {
		e.printStackTrace();
	}

	/* Report a fatal XML parsing error. */
	public void fatalError(SAXParseException e) {
		e.printStackTrace();
	}

	/* Receive notification of ignorable whitespace in element content. */
	public void ignorableWhitespace(char[] ch, int start, int length) {}

	/*
	 * Receive notification of a notation declaration. public void
	 * notationDecl(String name, String publicId, String systemId) {
	 * 
	 * }
	 */
	/*
	 * Receive notification of a processing instruction. public void
	 * processingInstruction(String target, String data) {
	 * 
	 * }
	 */
	/*
	 * Resolve an external entity. public InputSource resolveEntity (String
	 * publicId, String systemId) {
	 * 
	 * }
	 */
	/*
	 * Receive a Locator object for document events. public void
	 * setDocumentLocator(Locator locator) {
	 * 
	 * }
	 */
	/*
	 * Receive notification of a skipped entity. public void
	 * skippedEntity(String name) {
	 * 
	 * }
	 */
	/*
	 * Receive notification of an unparsed entity declaration. public void
	 * unparsedEntityDecl(String name, String publicId, String systemId, String
	 * notationName) {
	 * 
	 * }
	 */
	public void warning(SAXParseException e) {
		e.printStackTrace();
	}

	private void saveRecords(int rootid, int trackid) {
		try {
			Statement stmt1 = myConn.createStatement();

			String cmd1 = "INSERT IGNORE INTO TrackVersionByTrackID (TrackID,RootID) VALUES (" + trackid
					+ "," + rootid + ")";
			if (log.isDebugEnabled())
				log.debug("SaveSQL:" + cmd1);

			if (stmt1.executeUpdate(cmd1) == 0) {
				if (log.isWarnEnabled())
					log.warn("[DataExist] " + trackid + "-" + rootid);
			}

			cmd1 = null;
			stmt1.close();

		} catch (SQLException s) {
			if (log.isErrorEnabled())
				log.error(s);
		}
	}
}
