package yedda.utility.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoggUtil {
	// static Log logger = LogFactory.getLog(CommonLoggingUtil.class);
	static Log logger;

	static public void debug(String className, String msg) {
		getLog(className);
		logger.debug(msg);
	}

	static public void info(String className, String msg) {
		getLog(className);
		logger.info(msg);
	}

	static public void warn(String className, String msg) {
		getLog(className);
		logger.warn(msg);
	}

	static public void error(String className, String msg) {
		getLog(className);
		logger.error(msg);
	}

	static public void fatal(String className, String msg) {
		getLog(className);
		logger.fatal(msg);
	}

	static private void getLog(String className) {
		logger = LogFactory.getLog(className);
	}
}
