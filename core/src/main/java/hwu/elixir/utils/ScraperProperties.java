package hwu.elixir.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScraperProperties extends Properties {

	private static final long serialVersionUID = -4636476511803637295L;

	/** Singleton Properties object */
	private static ScraperProperties properties = null;

	private static final String configurationJarFile = "configuration.properties";
	private static final String configurationLocalFile = "localconfig.properties";

	private static Logger logger = LoggerFactory.getLogger(ScraperProperties.class.getName());

	private String dateTime;

	/**
	 * Non public constructor: this class should be instantiated only using
	 * getInstance()
	 */
	private ScraperProperties() {
	}

	/**
	 * Loads properties from the properties file in src > main > resources, and
	 * overrides the properties with file localconfig.properties from the local file
	 * system, if it exists.
	 * 
	 * File localconfig.properties will be created at first execution if it does not
	 * exist.
	 */
	public static synchronized ScraperProperties getInstance() {
		if (properties == null) {
			properties = new ScraperProperties();
			properties.dateTime = Helpers.getDateForName();
			
			// Read the default configuration and optionally override it with a local file
			ScraperProperties props = new ScraperProperties();
			props.readPropertiesFromJar();
			File localConfigFile = new File(configurationLocalFile);
			if (localConfigFile.exists())
				props.readPropertiesFromLocalFile();

			properties.put("outputFolder", props.getProperty("outputFolder").trim());
			properties.put("chromiumDriverLocation", props.getProperty("chromiumDriverLocation").trim());
			properties.put("locationOfSitesFile", props.getProperty("locationOfSitesFile").trim());
			properties.put("maxLimitScrape", props.getProperty("maxLimitScrape").trim());
			properties.put("dynamic", props.getProperty("dynamic").trim());
			properties.put("schemaContext", props.getProperty("schemaContext").trim());

			if (props.containsKey("contextCounter"))
				properties.put("contextCounter", props.getProperty("contextCounter").trim());
			else
				properties.put("contextCounter", "0");

			properties.displayPropertyValues();
		}

		return properties;
	}

	/**
	 * Displays values of properties read from properties file.
	 */
	private void displayPropertyValues() {
		logger.info("outputFolder:           " + this.getOutputFolder());
		logger.info("chromiumDriverLocation: " + this.getChromiumDriverLocation());
		logger.info("locationOfSitesFile:    " + this.getLocationOfSitesFile());
		logger.info("contextCounter:         " + this.getContextCounter());
		logger.info("Max no. URLs to scrape: " + this.getMaxLimitScrape());
		logger.info("Schema.org context URL: " + this.getSchemaContext());
		logger.info("Dynamic scrape:         " + this.dynamic());
	}

	/**
	 * Read properties from local file.
	 * 
	 * @return
	 */
	private void readPropertiesFromLocalFile() {
		Reader reader = null;
		try {
			reader = new FileReader(configurationLocalFile);
			this.load(reader);
			logger.info("Local configuration read from file " + configurationLocalFile);
		} catch (IOException e) {
			logger.error("Cannot load " + configurationLocalFile, e);
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ee) {
					// ignoring
				}
			}
		}
	}

	/**
	 * Read configuration properties from JAR. Will be called every time you run the
	 * scraper.
	 *
	 * @return
	 */
	private void readPropertiesFromJar() {
		ClassLoader classLoader = ScraperProperties.class.getClassLoader();
		try {
			InputStream is = classLoader.getResourceAsStream(configurationJarFile);
			this.load(is);
			logger.info("Default configuration read from jar file");
		} catch (Exception e) {
			logger.error("Cannot load " + configurationJarFile, e);
		}
	}

	/**
	 * Write properties to the local file
	 */
	public void updateConfig() {
		Writer writer = null;
		try {
			writer = new FileWriter(configurationLocalFile);
			properties.store(writer, null);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// ignoring
				}
			}
		}
	}

	public long getContextCounter() {
		return Long.parseLong(properties.getProperty("contextCounter"));
	}

	public void setContextCounter(long contextCounter) {
		properties.put("contextCounter", Long.toString(contextCounter));
	}

	public String getLocationOfSitesFile() {
		return properties.getProperty("locationOfSitesFile");
	}

	public String getOutputFolder() {
		return properties.getProperty("outputFolder") + "/" + dateTime + "/";
	}

	public int getMaxLimitScrape() {
		return Integer.parseInt(properties.getProperty("maxLimitScrape"));
	}

	public boolean dynamic() {
		return Boolean.parseBoolean(properties.getProperty("dynamic"));
	}

	public String getChromiumDriverLocation() {
		return properties.getProperty("chromiumDriverLocation");
	}

	public String getSchemaContext() {
		return properties.getProperty("schemaContext");
	}
}
