package data.mapping.config;

import org.apache.commons.configuration.*;

public class AppConfig extends PropertiesConfiguration {
	// private final String DEFAULT_LOCALE = "zh_TW";
	private final String DEFAULT_ENCODE = "utf-8";
	private final String DEFAULT_SRC = ConfigPropertyList.DEFAULT.getPath();

	private static AppConfig instance = null;

	public final String DEFAULT_DB_IP = "localhost";
	public final int DEFAULT_DB_PORT = 3306;
	public final String DEFAULT_DB_TABLE = "trackmapping";
	public final String DEFAULT_DB_USER = "root";

	public static AppConfig getInstance() throws ConfigurationException {
		if (instance == null) {
			instance = new AppConfig();
		}
		return instance;
	}

	private AppConfig() throws ConfigurationException {
		this.setEncoding(DEFAULT_ENCODE);
		this.setAutoSave(false);
		this.setListDelimiter(',');
	}

	public void load() throws ConfigurationException {
		super.load(DEFAULT_SRC);
	}

	public void load(String targetPath) throws ConfigurationException {
		super.load(targetPath);
	}
}
