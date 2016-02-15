package data.mapping.track;

/* handle windows OS db connection */
import yedda.utility.jdbc.WinConnectionUtil;
import data.mapping.config.AppConfig;
import data.mapping.config.ConfigPropertyList;

import java.util.Hashtable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Transfer track data from MS access file to mysql database
 */
public class TrackMap {
	private static final Log log = LogFactory.getLog("map");
	private AppConfig config;
	private java.sql.Connection myConn;

	/**
	 * constructor
	 * @throws Exception
	 */
	public TrackMap() throws Exception {
		if (log.isDebugEnabled())
			log.debug("construct TrackMap.");

		try {
			config = AppConfig.getInstance();
			config.load(ConfigPropertyList.MUSICSRC.getPath());

			myConn = WinConnectionUtil.getMysqlDBConnection(
				config.getString("db.ip", config.DEFAULT_DB_IP),
				config.getInt("db.port", config.DEFAULT_DB_PORT),
				config.getString("db.name", config.DEFAULT_DB_TABLE),
				config.getString("db.user", config.DEFAULT_DB_USER),
				config.getString("db.psw", "")
			);
		} catch (ConfigurationException e) {
			if (log.isErrorEnabled())
				log.error("Can't load properties:" + ConfigPropertyList.MUSICSRC.getPath(), e);
			throw e;
		} catch (SQLException s) {
			if (log.isErrorEnabled())
				log.error("Can't connect database.", s);
			throw s;
		}
	}

	public void updateTrackDesc() {
		java.sql.Connection conn = null;
		try {
			conn = WinConnectionUtil.getAccessDBConnection(config.getString("access.src", ""));

			Statement stmt1 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			Statement stmt2 = myConn.createStatement();

			String cmd1 = "SELECT [tTrack.TrackID], [tTrack.DurationAcronym] FROM tTrack;";
			if (log.isDebugEnabled())
				log.debug("accessSQL1:" + cmd1);

			ResultSet rs1 = stmt1.executeQuery(cmd1);
			rs1.beforeFirst();
			while (rs1.next()) {
				String cmd2 = String.format("UPDATE TrackMap SET TrackDesc='%1$s' WHERE TrackID=%1$d",
						rs1.getString("DurationAcronym"), rs1.getInt("TrackID"));
				// System.out.println(cmd2);

				if (stmt2.executeUpdate(cmd2) == 0) {
					if (log.isWarnEnabled())
						log.warn("[TrackNoUpdated] " + cmd2);
				}

			}
			rs1.close();
			stmt1.close();
			stmt2.close();

		} catch (SQLException s) {
			if (log.isErrorEnabled())
				log.error(s);
		} finally {

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException ignore) {
				}
			}
		}

	}

	public void createTrack() {
		java.sql.Connection conn = null;
		try {
			conn = WinConnectionUtil.getAccessDBConnection(config.getString("access.src", ""));

			Statement stmt1 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			Statement stmt2 = myConn.createStatement();

			String cmd1 = "SELECT [tDiscTrack.TrackID], [tDiscTrack.TrackNo], [tDiscTrack.TrackNoIndex], [tDiscTrack.DiscID], [tTrack.DurationAcronym] "
					+ "FROM tDiscTrack INNER JOIN tTrack ON tDiscTrack.TrackID = tTrack.TrackID;";
			if (log.isDebugEnabled())
				log.debug("accessSQL1:" + cmd1);

			ResultSet rs1 = stmt1.executeQuery(cmd1);
			rs1.last();
			int rowCount1 = rs1.getRow();
			if (rowCount1 == 0) {
				if (log.isWarnEnabled())
					log.warn("Get no access info.");
				return;
			}

			rs1.beforeFirst();
			while (rs1.next()) {
				int id = rs1.getInt("TrackID");
				String cmd2 = String.format(
						"INSERT INTO TrackMap (TrackID, TrackNo, TrackNoIndex, TrackDesc, DiscID, CDID, ItemID, ItemFlow, ItemIdx, Note) VALUES (%1$d,%2$d,%3$d,'%4$s',%5$d,'',0,0,0,'');",
						id, rs1.getInt("TrackNo"), rs1.getInt("TrackNoIndex"), rs1.getString("DurationAcronym"),
						rs1.getInt("DiscID"));
				// System.out.println(cmd2);

				if (stmt2.executeUpdate(cmd2) == 0) {
					if (log.isWarnEnabled())
						log.warn("[DataExist] TrackID:" + id);
				} else {
					if (log.isInfoEnabled())
						log.info("[DataInsert] TrackID:" + id);
				}

			}
			rs1.close();
			stmt1.close();
			stmt2.close();

		} catch (SQLException s) {
			if (log.isErrorEnabled())
				log.error(s);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e);
		} finally {

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException ignore) {
				}
			}
		}

	}

	public void updateTrackMap() {
		updateTrackMap(0);
	}

	public void updateTrackMap(int inDiscID) {

		try {
			Statement stmt1 = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			Statement stmt2 = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			Statement stmt3 = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			Statement stmt4 = myConn.createStatement();

			String cmd1 = "SELECT DiscID,CDID,SeriesID,CD_Idx,Note FROM DiscMap WHERE CDID!='';";
			if (inDiscID != 0) {
				cmd1 = "SELECT DiscID,CDID,SeriesID,CD_Idx,Note FROM DiscMap WHERE DiscID=" + inDiscID + ";";
			}
			if (log.isDebugEnabled())
				log.debug("SQL1:" + cmd1);

			ResultSet rs1 = stmt1.executeQuery(cmd1);
			rs1.last();
			int rowCount1 = rs1.getRow();
			if (rowCount1 == 0) {
				if (log.isWarnEnabled())
					log.warn("Get no Disc info:" + inDiscID);
				return;
			}

			rs1.beforeFirst();
			while (rs1.next()) {

				String cdid = rs1.getString("CDID");
				int cdidx = rs1.getInt("CD_Idx");
				int discid = rs1.getInt("DiscID");

				// get cd_item info
				String cmd2 = String.format(
						"SELECT Item_No, Item_ID FROM musiclibrary.cd_item WHERE CD_ID='%1$s' AND Item_ID>=%2$d AND Item_ID<%3$d",
						cdid, (cdidx * 10000), ((cdidx + 1) * 10000));
				if (log.isDebugEnabled())
					log.debug("SQL2:" + cmd2);

				ResultSet rs2 = stmt2.executeQuery(cmd2);
				rs2.last();
				int rowCount2 = rs2.getRow();
				// System.out.println("count:"+rowCount2);

				if (rowCount2 == 0) {
					if (log.isWarnEnabled())
						log.warn("[DiscNoMatch]DiscID:" + discid + " CDID:" + cdid + " CDIdx:" + cdidx + " SeriesID:"
								+ rs1.getString("SeriesID") + " Note:" + rs1.getString("Note"));
					continue;
				}

				if (log.isInfoEnabled())
					log.info("[DiscMatched]Track count:" + rowCount2 + " (DiscID:" + discid + " CDID:" + cdid
							+ " CDIdx:" + cdidx + " SeriesID:" + rs1.getString("SeriesID") + ")");

				Hashtable<String, Integer> hash = new Hashtable<String, Integer>();
				rs2.beforeFirst();
				while (rs2.next()) {
					hash.put(String.valueOf(rs2.getInt("Item_ID")), new Integer(rs2.getInt("Item_No")));
				}
				rs2.close();
				rs2 = null;

				String cmd3 = "SELECT * FROM TrackMap WHERE DiscID=" + discid;
				if (log.isDebugEnabled())
					log.debug("SQL3:" + cmd3);

				ResultSet rs3 = stmt3.executeQuery(cmd3);
				rs3.beforeFirst();
				while (rs3.next()) {

					if (rs3.getString("Manual").compareToIgnoreCase("Y") == 0) {
						if (log.isWarnEnabled())
							log.warn("[TrackManualSet]Disc:" + discid + " CDID:" + cdid + " CDIdx:" + cdidx
									+ " TrackID:" + rs3.getInt("TrackID") + " TrackNo:" + rs3.getInt("TrackNo")
									+ " TrackIdx:" + rs3.getInt("TrackNoIndex") + " ItemID:" + rs3.getString("ItemID"));
						continue;
					}

					int itemid = (10000 * cdidx) + (rs3.getInt("TrackNo") * 100) + rs3.getInt("TrackNoIndex");
					if (!hash.containsKey(String.valueOf(itemid))) {
						if (log.isWarnEnabled())
							log.warn("[TrackNotMatched]Disc:" + discid + " CDID:" + cdid + " CDIdx:" + cdidx
									+ " TrackID:" + rs3.getInt("TrackID") + " TrackNo:" + rs3.getInt("TrackNo")
									+ " TrackIdx:" + rs3.getInt("TrackNoIndex") + " ItemID:" + itemid + " Note:"
									+ rs3.getString("Note"));
						continue;
					}

					if (log.isInfoEnabled())
						log.info("[CurrentTrack]Disc:" + discid + " CDID:" + cdid + " CDIdx:" + cdidx + " TrackID:"
								+ rs3.getInt("TrackID") + " ItemID:" + itemid);

					int itemno = hash.get(String.valueOf(itemid)).intValue();
					int itemIdx = rs3.getInt("TrackNoIndex");
					int itemFlow = rs3.getInt("TrackNo");

					String cmd4 = String.format(
							"UPDATE TrackMap SET CDID='%1$s',ItemID=%2$d,ItemNo=%3$d,ItemFlow=%4$d,ItemIdx=%5$d WHERE TrackID=%6$d",
							cdid, itemid, itemno, itemFlow, itemIdx, rs3.getInt("TrackID"));
					if (stmt4.executeUpdate(cmd4) == 0) {
						if (log.isWarnEnabled())
							log.warn("[DBUpdErr]" + cmd4);
					}

				}
				rs3.close();
				rs3 = null;

				hash.clear();
				hash = null;

			}
			rs1.close();
			stmt1.close();
			stmt2.close();
			stmt3.close();
			stmt4.close();

		} catch (SQLException s) {
			s.printStackTrace();
			log.error(s);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e);
		} finally {

		}

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
	}

	/**
	 * Main entry
	 * @param args
	 */
	public static void main(String[] args) {
		if (log.isInfoEnabled())
			log.info("Start of TrackMap");
		try {
			TrackMap map = new TrackMap();
			map.createTrack();
			map.destroy();
			map = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (log.isInfoEnabled())
			log.info("End of TrackMap");
	}
}
