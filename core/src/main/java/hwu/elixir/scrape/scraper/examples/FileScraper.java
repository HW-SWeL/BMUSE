package hwu.elixir.scrape.scraper.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.eclipse.rdf4j.query.algebra.In;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.scraper.ScraperCore;
import hwu.elixir.scrape.scraper.ScraperFilteredCore;
import hwu.elixir.utils.Helpers;

/**
 * Scrapes a list of URLs which come from a given file OR
 * scrapes a single specified URL.
 * 
 * Output is quads written to a file in a location specified in application.properties.
 *
 */
public class FileScraper extends ScraperFilteredCore {

	private static final String propertiesJarFile = "application.properties";
	private static final String configurationJarFile = "configuration.properties";
	private static final String propertiesLocalFile = "localProperties.properties";

	private static String locationOfSitesFile = "";
	private static String outputFolderOriginal = "";
	private static String outputFolder = System.getProperty("user.home");
	private static long contextCounter = 0L;
	private static int maxLimitScrape = 5; //Default setting if none is set on the application.properties file
	private static boolean isDynamic = true; //Default setting if none is set on the application.properties file

	private static ArrayList<String> urlsToScrape = new ArrayList<>();

	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
	private static Logger logger = LoggerFactory.getLogger(System.class.getName());

	/**
	 * Updates properties based on properties file in src > main > resources
	 * Please note that this will only read the properties from the file
	 * only the first time it is executed, after that it will always read
	 * from local file and any changes to the application.properties will not be picked up
	 */
	private void readProperties() {
		Properties properties = null;
		Properties configurationProperties = null;

		configurationProperties = readConfigurationPropertiesFromJar();

		File contextCounterFile = new File(propertiesLocalFile);
		if (contextCounterFile.exists()) {
			properties = readPropertiesFromLocalFile();
		} else {
			properties = readPropertiesFromJar();
		}
		
		outputFolderOriginal = properties.getProperty("outputFolder").trim();
		outputFolder = outputFolderOriginal + "_" + Helpers.getDateForName() + "/";
		locationOfSitesFile = properties.getProperty("locationOfSitesFile").trim();
		contextCounter = Long.parseLong(properties.getProperty("contextCounter").trim());
		logger.info(configurationProperties.getProperty("maxLimitScrape").trim());
		maxLimitScrape = Integer.parseInt(configurationProperties.getProperty("maxLimitScrape").trim());
		isDynamic = Boolean.parseBoolean(configurationProperties.getProperty("isDynamic").trim());

		
		displayPropertyValues();
	}

	
	/** 
	 * Displays values of properties read from properties file.
	 */
	private void displayPropertyValues() {
		logger.info("outputFolder: " + outputFolder);
		logger.info("locationOfSitesFile: " + locationOfSitesFile);
		logger.info("contextCounter: " + contextCounter);
		logger.info("Max limit of URLs to scrape: " + maxLimitScrape);
		logger.info("Dynamic scrape: " + isDynamic);
		logger.info("\n\n\n");
	}
	
	
	/**
	 * Read properties from local file. 
	 * 
	 * @return
	 */
	private Properties readPropertiesFromLocalFile() {
		logger.info("Reading properties from local file");
		Properties properties = null;
		Reader reader = null;
		try {
			reader = new FileReader(propertiesLocalFile);
			properties = new Properties();
			properties.load(reader);
		} catch (IOException e) {
			return readPropertiesFromJar();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignoring
				}
			}
		}
		return properties;
	}
	
	/**
	 * Read properties from JAR. Will be called if no local properties file exists.
	 * 
	 * @return
	 */
	private Properties readPropertiesFromJar() {
		logger.info("Reading properties from jar file");
		ClassLoader classLoader = ScraperCore.class.getClassLoader();
		InputStream is = classLoader.getResourceAsStream(propertiesJarFile);
		if (is == null) {
			logger.error("     Cannot find " + propertiesJarFile + " file");
			throw new IllegalArgumentException(propertiesJarFile + "file is not found!");
		}

		Properties properties = new Properties();
		try {
			properties.load(is);
			properties.setProperty("contextCounter", "0");
		} catch (IOException e) {
			logger.error("Cannot load application.properties ", e);
			shutdown();
			System.exit(-1);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// ignoring 
			}
		}
		return properties;
	}


	/**
	 * Read configuration properties from JAR. Will be called every time you run the scraper.
	 *
	 * @return
	 */
	private Properties readConfigurationPropertiesFromJar() {
		logger.info("Reading configuration properties from jar file");
		ClassLoader classLoader = ScraperCore.class.getClassLoader();
		InputStream is = classLoader.getResourceAsStream(configurationJarFile);
		if (is == null) {
			logger.error("     Cannot find " + configurationJarFile + " file");
			throw new IllegalArgumentException(configurationJarFile + "file is not found!");
		}

		Properties properties = new Properties();
		try {
			properties.load(is);
			properties.setProperty("contextCounter", "0");
		} catch (IOException e) {
			logger.error("Cannot load configuration.properties ", e);
			shutdown();
			System.exit(-1);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// ignoring
			}
		}
		return properties;
	}

	/**
	 * Write contextCounter to local properties file
	 * 
	 */
	private void updateContextCounter() {
		Properties properties = null;
		Writer writer = null;
		try {
			writer = new FileWriter(propertiesLocalFile);
			properties = new Properties();
			properties.setProperty("locationOfSitesFile", locationOfSitesFile);
			properties.setProperty("outputFolder", outputFolderOriginal);
			properties.setProperty("contextCounter", Long.toString(contextCounter));
			properties.setProperty("maxLimitScrape", Integer.toString(maxLimitScrape));
			properties.setProperty("isDynamic", Boolean.toString(isDynamic));
			
			properties.store(writer, "updating contextCounter");
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
	 * Read the file (specified in application.properties) and puts each URL into a
	 * list for later scraping
	 * 
	 */
	private void readFileList() {
		readProperties();

		if (locationOfSitesFile.equals("") || locationOfSitesFile == null) {
			logger.error("Please set *locationOfSitesFile* in src > main > resources > application.properties");
			shutdown();
			System.exit(-1);
		}

		File sitesList = new File(locationOfSitesFile);
		if (!sitesList.exists()) {
			logger.error("Cannot find file *" + locationOfSitesFile
					+ "*. Please set correct value in src > main > resources > application.properties");
			shutdown();
			System.exit(-1);
		}

		try (BufferedReader in = new BufferedReader(new FileReader(sitesList))) {
			String line = "";
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("http")) {
					urlsToScrape.add(line);
				}
			}
		} catch (Exception e) {
			logger.error("Problem reading sites from file *" + locationOfSitesFile + "*.");
			shutdown();
			System.exit(-1);
		}

		logger.info("Read " + urlsToScrape.size() + " urls to scrape from " + locationOfSitesFile+".\n");
	}

	/**
	 * Method that parses a sitemap and then returns the list of URLs of all samples as Elements
	 *
	 *
	 */
	public Elements getSitemapList(String url, String sitemapURLKey) throws IOException {

		Document doc = null;

		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		Elements elements = doc.select(sitemapURLKey);
		//Document doc = Jsoup.parse(html, "", Parser.xmlParser());

		return elements;
	}

	
	/**
	 * Reads a list of URLs from a specified file; scrapes each one sequentially and writes the parsed (bio)schema 
	 * markup to a NQuads file inside a specified directory. 
	 * 
	 * Each file is given a unique name based on the order (sequential number) in which it was scraped.
	 * This number is not reset, instead auto-incrementing with each scrape or run of this scraper. 
	 *  
	 */
	public void scrapeAllUrls() {

		// Load the settings from the application.properties file
		readFileList();

		for (String url : urlsToScrape) {
			boolean result = false;

			logger.info("Attempting to scrape: " + url);

			// Check if the word sitemap is part of the URL (assumes that the URL is a sitemap if true)
			if (url.toLowerCase().indexOf("sitemap") != -1) {
				int sitemapCount = 0;
				Elements sitemapList = null;
				try {
					sitemapList = getSitemapList(url, "loc"); //loc will only parse the url of the sample
				} catch (IOException e) {
					e.printStackTrace();
				}
				sitemapList.toArray();
				logger.info("Sitemap found in URL: " + url);
				// get sitemap list
				// for url in sitemap list
				for (Element sitemapURL : sitemapList){
					logger.info("Attempting to scrape: " + sitemapURL.text());
					try {
						result = scrape(sitemapURL.text(), outputFolder, null, contextCounter++);
					} catch (FourZeroFourException e) {
						logger.error(url + "returned a 404.");
					} catch (JsonLDInspectionException e) {
						logger.error("The JSON-LD could be not parsed for " + url);
					} catch (CannotWriteException e) {
						logger.error("Problem writing file for " + url + " to the " + outputFolder + " directory.");
					} catch (MissingMarkupException e) {
						logger.error("Problem obtaining markup from " + url + ".");
					}
					displayResult(sitemapURL.text(), result, outputFolder);
					sitemapCount++;
					if (maxLimitScrape < sitemapCount) {
						logger.info("MAX SITEMAP LIMIT REACHED: " + maxLimitScrape);
						logger.info("Scraping over");
						break;
					}
				}

			} else {
				try {
					result = scrape(url, outputFolder, null, contextCounter++);
				} catch (FourZeroFourException e) {
					logger.error(url + "returned a 404.");
				} catch (JsonLDInspectionException e) {
					logger.error("The JSON-LD could be not parsed for " + url);
				} catch (CannotWriteException e) {
					logger.error("Problem writing file for " + url + " to the " + outputFolder + " directory.");
				} catch (MissingMarkupException e) {
					logger.error("Problem obtaining markup from " + url + ".");
				}

				displayResult(url, result, outputFolder);
			}


		}
		logger.info("Scraping over");
		updateContextCounter();
		shutdown();
	}
	
	public static void main(String[] args) throws FourZeroFourException, JsonLDInspectionException {
		logger.info("STARTING SCRAPE: " + formatter.format(new Date(System.currentTimeMillis())));
		FileScraper core = new FileScraper();
	
		// to scrape all URLs in the file specified in applications.properties
		core.scrapeAllUrls();
		
		logger.info("ENDING SCRAPE: " + formatter.format(new Date(System.currentTimeMillis())));
		System.exit(0);
	}

}
