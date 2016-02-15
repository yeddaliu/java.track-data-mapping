package data.mapping.config;

public enum ConfigPropertyList {

	DEFAULT("./resource/conf/app.conf"),
	MUSICSRC("./resource/conf/musicsrc.conf");

	private String path;

	private ConfigPropertyList(String path) {
		this.path = path;
	}

	public String getPath() {
		return this.path;
	}

}
