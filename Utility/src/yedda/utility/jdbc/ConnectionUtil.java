/**
 * MacConnectionUtil
 * 
 * provide drivers:
 * mysql
 */
package yedda.utility.jdbc;

import java.sql.*;

public class ConnectionUtil {

	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException ce) {
			ce.printStackTrace();
			// System.err.println("Class not found for some of drivers");
			System.exit(0);
		}

	}

	public static java.sql.Connection getAccessDBConnection(String filename) throws SQLException {

		filename = filename.replace('\\', '/').trim();

		String Prefix = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
		String Suffix = ";DriverID=22;READONLY=true}";
		String dbURL = Prefix + filename + Suffix;

		return DriverManager.getConnection(dbURL, "", "");
	}

	public static java.sql.Connection getMysqlDBConnection(String username, String password) throws SQLException {
		return getMysqlDBConnection("localhost", 3306, "test", username, password);
	}

	public static java.sql.Connection getMysqlDBConnection(String dbName, String username, String password)
			throws SQLException {
		return getMysqlDBConnection("localhost", 3306, dbName, username, password);
	}

	public static java.sql.Connection getMysqlDBConnection(String ip, int port, String dbName, String username,
			String password) throws SQLException {
		String dbURL = "jdbc:mysql://" + ip + ":" + port + "/" + dbName;
		return DriverManager.getConnection(dbURL, username, password);
	}
}
