package hwu.elixir.scrape.scraper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.SeleniumException;
import hwu.elixir.utils.Helpers;

/**
 * Scrapes a list of URLs which come from a given file OR
 * scrapes a single specified URL.
 * 
 * Output is quads written to a file in a location specified in application.properties.
 *  
 * 
 * @author kcm
 *
 */
public class FileScraper extends ScraperCore {

	private static final String propertiesJarFile = "application.properties";
	private static final String propertiesLocalFile = "localProperties.properties";

	private static String locationOfSitesFile = "";
	private static String outputFolderOriginal = "";
	private static String outputFolder = System.getProperty("user.home");
	private static long contextCounter = 0L;

	private static ArrayList<String> urlsToScrape = new ArrayList<>();

	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
	private static Logger logger = LoggerFactory.getLogger(System.class.getName());

	/**
	 * Updates properties based on properties file in src > main > resources
	 * 
	 */
	private void readProperties() {
		Properties properties = null;

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
		
		displayPropertyValues();
	}

	private void displayPropertyValues() {
		logger.info("outputFolder: " + outputFolder);
		logger.info("locationOfSitesFile: " + locationOfSitesFile);
		logger.info("contextCounter: " + contextCounter);
		logger.info("\n\n\n");
	}
	
	
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

	private void updateContextCounter() {
		Properties properties = null;
		Writer writer = null;
		try {
			writer = new FileWriter(propertiesLocalFile);
			properties = new Properties();
			properties.setProperty("locationOfSitesFile", locationOfSitesFile);
			properties.setProperty("outputFolder", outputFolderOriginal);
			properties.setProperty("contextCounter", Long.toString(contextCounter));
			
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

	public void scrapeAllUrls() {
		readFileList();
		for (String url : urlsToScrape) {
			logger.info("Attempting to scrape: " + url);

			boolean result = false;
			try {
				result = scrape(url, null);
			} catch (FourZeroFourException e) {
				logger.error(url + "returned a 404.");
			} catch (JsonLDInspectionException e) {
				logger.error("The JSON-LD could be not parsed for " + url);
			}

			displayResult(url, result);
		}
		logger.info("Scraping over");
		updateContextCounter();
		shutdown();
	}
	
	
	private void displayResult(String url, boolean result) {
		if (result) {
			logger.info(url + " was successfully scraped and written to " + outputFolder);
		} else {
			logger.error(url + " was NOT successfully scraped.");
		}
		logger.info("\n\n");		
	}
	
	/** 
	 * Scrape a given URL and write to file in the home directory.
	 * 
	 * @param url The URL to scrape
	 * @throws FourZeroFourException
	 * @throws JsonLDInspectionException
	 */
	public void scrapeASingleURL(String url, String fileName) throws FourZeroFourException, JsonLDInspectionException {
		readProperties();		
		try {			
			displayResult(url, scrape(url, fileName));
		} finally {
			shutdown();
		}		
	}

	/**
	 * Actually scrapes a given URL and writes the output (as quads) to a file specified in the arguments. If not 
	 * specified, ie null, the contextCounter will be used to name the file.
	 * 
	 * The file will be located in the location specified in application.properties
	 * 	 
	 * @param url URL to scrape
	 * @return FALSE if failed else TRUE
	 * @throws FourZeroFourException
	 * @throws JsonLDInspectionException
	 */
	private boolean scrape(String url, String fileName) throws FourZeroFourException, JsonLDInspectionException {
		if (url.endsWith("/") || url.endsWith("#"))
			url = url.substring(0, url.length() - 1);

		String html = "";

		try {
			html = getHtmlViaSelenium(url);
		} catch (SeleniumException e) {
			// try again
			try {
				html = getHtmlViaSelenium(url);
			} catch (SeleniumException e2) {
				return false;
			}
		}

		try {
			html = injectId(html, url);
		} catch (MissingHTMLException e) {
			logger.error(e.toString());
			return false;
		}

		DocumentSource source = new StringDocumentSource(html, url);
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());

		String n3 = getTriplesInNTriples(source);
		if (n3 == null)
			return false;

		Model updatedModel = processTriples(n3, sourceIRI, contextCounter);
		if (updatedModel == null)
			return false;

		File directory = new File(outputFolder);
		if (!directory.exists())
			directory.mkdir();

		if(fileName == null) {
			fileName = outputFolder + "/" + contextCounter++ + ".nq";
		} else {
			fileName = outputFolder + "/" + fileName + ".nq";
		}

		try (PrintWriter out = new PrintWriter(new File(fileName))) {
			Rio.write(updatedModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			logger.error("Problem writing file for " + url + " to the " + outputFolder + " directory.");
			shutdown();
			System.exit(-1);
		}

		if (!new File(fileName).exists())
			System.exit(0);

		return true;
	}

	
	public static void main(String[] args) throws FourZeroFourException, JsonLDInspectionException {
		logger.info("STARTING SCRAPE: " + formatter.format(new Date(System.currentTimeMillis())));
		FileScraper core = new FileScraper();
	
		// to scrape all URLs in the file specified in applications.properties
		core.scrapeAllUrls();
		
		// to scrape a single URL
//		core.scrapeASingleURL("https://www.uniprot.org/uniprot/P46736", "uniprot");

		logger.info("ENDING SCRAPE: " + formatter.format(new Date(System.currentTimeMillis())));
		System.exit(0);
	}

}
