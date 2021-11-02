package hwu.elixir.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;


import hwu.elixir.scrape.scraper.ScraperCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ScraperProperties extends Properties {

	private static final long serialVersionUID = -4636476511803637295L;

	/** Singleton Properties object */
	private static ScraperProperties properties = null;

	//the configuration.properties file is only parsed the first time the scraper starts
	private static final String configurationJarFile = "configuration.properties";
	//after the initial setup the information is stored to the localconfig.properties file, to re-
	//set the system you can delete the file and then it will be created again when you run the scraper
	//with the new desired setup
	private static final String configurationLocalFile = "localconfig.properties";
	private static Logger logger = LoggerFactory.getLogger(ScraperProperties.class.getName());
	private String dateTime;
	private static String finalAppVersion = "0.5.1";

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
			logger.info("scraper properties" + props.toString());
			File localConfigFile = new File(configurationLocalFile);
			if (localConfigFile.exists())
				props.readPropertiesFromLocalFile();

			properties.put("scraperVersion", setScraperVersion());
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

		logger.info("scraper implementation version:  " + this.getScraperVersion());
		logger.info("outputFolder:                    " + this.getOutputFolder());
		logger.info("chromiumDriverLocation:          " + this.getChromiumDriverLocation());
		logger.info("locationOfSitesFile:             " + this.getLocationOfSitesFile());
		logger.info("contextCounter:                  " + this.getContextCounter());
		logger.info("Max no. URLs to scrape:          " + this.getMaxLimitScrape());
		logger.info("Schema.org context URL:          " + this.getSchemaContext());
		logger.info("Dynamic scrape (global setting): " + this.dynamic());

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
	 *
	 */
	private void readPropertiesFromJar() {
		ClassLoader classLoader = ScraperProperties.class.getClassLoader();
		logger.info("class loader name" + classLoader.getClass().getName());
		try {
			InputStream is = classLoader.getResourceAsStream(configurationJarFile);
			this.load(is);
			logger.info(is.toString());
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

	/**
	 * method that parses the jar or pom.xml file and gets the version of BMUSE
	 */
	private static String setScraperVersion() {

		// try to get the version from the jar file, if a jar file is generated
		String appVersion = ScraperCore.class.getPackage().getImplementationVersion();

		if (appVersion == null) {
			try {
				//This will only read from the pom file of the project's root directory, please note that the core,
				//service and webapp pom are not read
				// File.separator will work both in windows and linux
				File fXmlFile = new File("." + File.separator +"pom.xml");
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(fXmlFile);

				//optional, but recommended to normalise the document
				//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
				doc.getDocumentElement().normalize();

				appVersion = doc.getElementsByTagName("version").item(0).getTextContent();

			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}

		// if version not in jar or pom then it must be from war file
		// TODO find a better way of dealing with the method been called from the web service
		if (appVersion == null){
			// This is only set in case the scraper core is called from the web service,
			// otherwise it will be overwritten by the jar or pom entry
			appVersion = finalAppVersion;
		}
		
		return  appVersion;
	}

	public void setContextCounter(long contextCounter) {
		properties.put("contextCounter", Long.toString(contextCounter));
	}

	public long getContextCounter() {
		return Long.parseLong(properties.getProperty("contextCounter"));
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

	public String getScraperVersion() {
		return properties.getProperty("scraperVersion");
	}
}
