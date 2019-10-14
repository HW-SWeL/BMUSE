package hwu.elixir.scrape.scraper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
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

public class FileScraper extends ScraperCore {

	private static final String propertiesFile = "application.properties";

	private static String locationOfSitesFile = "";
	private static String outputFolder = System.getProperty("user.home");;
	private static long contextCounter = 0L;

	private static ArrayList<String> urlsToScrape = new ArrayList<>();

	private static Logger logger = LoggerFactory.getLogger(System.class.getName());

	/**
	 * Updates properties based on properties file in src > main > resources
	 * 
	 */
	private void processProperties() {
		ClassLoader classLoader = ScraperCore.class.getClassLoader();

		InputStream is = classLoader.getResourceAsStream(propertiesFile);
		if (is == null) {
			logger.error("     Cannot find " + propertiesFile + " file");
			throw new IllegalArgumentException(propertiesFile + "file is not found!");
		}

		Properties prop = new Properties();

		try {
			prop.load(is);
		} catch (IOException e) {
			logger.error("Cannot load application.properties ", e);
			System.out.println("Cannot load application.properties ");
			System.exit(-1);
		}

		outputFolder = prop.getProperty("outputFolder").trim();
		outputFolder += Helpers.getDateForName() + "/";
		logger.info("outputFolder: " + outputFolder);
		System.out.println("outputFolder: " + outputFolder);

		locationOfSitesFile = prop.getProperty("locationOfSitesFile").trim();
		logger.info("locationOfSitesFile: " + locationOfSitesFile);
		System.out.println("locationOfSitesFile: " + locationOfSitesFile);
		logger.info("\n\n\n");
	}

	private void readFileList() {
		processProperties();
		
		if (locationOfSitesFile.equals("") || locationOfSitesFile == null) {
			logger.error("Please set *locationOfSitesFile* in src > main > resources > application.properties");
			System.out.println("Please set *locationOfSitesFile* in src > main > resources > application.properties");
			shutdown();
			System.exit(-1);
		}

		File sitesList = new File(locationOfSitesFile);
		if (!sitesList.exists()) {
			logger.error("Cannot find file *" + locationOfSitesFile
					+ "*. Please set correct value in src > main > resources > application.properties");
			System.out.println("Cannot find file *" + locationOfSitesFile
					+ "*. Please set correct value in src > main > resources > application.properties");
			shutdown();
			System.exit(-1);
		}

		try (BufferedReader in = new BufferedReader(new FileReader(sitesList))) {
			String line = "";
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if(line.startsWith("http")) {
					urlsToScrape.add(line);
				}
			}
		} catch (Exception e) {
			logger.error("Problem reading sites from file *" + locationOfSitesFile + "*.");
			System.out.println("Problem reading sites from file *" + locationOfSitesFile + "*.");
			shutdown();
			System.exit(-1);
		}

		logger.info("Read " + urlsToScrape.size() + " urls to scrape from " + locationOfSitesFile);
		System.out.println("Read " + urlsToScrape.size() + " urls to scrape from " + locationOfSitesFile);

	}

	public void scrapeAllUrls() {
		readFileList();
		for (String url : urlsToScrape) {
			logger.info("Attempting to scrape: " + url);
			System.out.println("Attempting to scrape: " + url);

			boolean result = false;
			try {
				result = scrape(url);
			} catch (FourZeroFourException e) {
				logger.error(url +  "returned a 404.");
				System.out.println(url +  "returned a 404.");				
			} catch (JsonLDInspectionException e) {
				logger.error("The JSON-LD could be not parsed for " + url);
				System.out.println("The JSON-LD could be not parsed for " + url);
			}

			if (result) {
				logger.info(url + " was successfully scraped and written to " + outputFolder);
				System.out.println(url + " was successfully scraped and written to " + outputFolder + ".\n");
			} else {
				logger.error(url + " was NOT successfully scraped.");
				System.out.println(url + " was NOT successfully scraped.\n");
			}
		}
		System.out.println("Scraping over");
		logger.info("Scraping over");
	}

	public boolean scrape(String url) throws FourZeroFourException, JsonLDInspectionException {
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

		String n3 = getTriplesInN3(source);
		if (n3 == null)
			return false;

		Model updatedModel = processTriples(n3, sourceIRI, contextCounter);
		if (updatedModel == null)
			return false;

		File directory = new File(outputFolder);
		if (!directory.exists())
			directory.mkdir();

		String fileName = outputFolder + "/" + contextCounter++ + ".nq";

		try (PrintWriter out = new PrintWriter(new File(fileName))) {
			Rio.write(updatedModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			logger.error("Problem writing file for " + url + " to the " + outputFolder + " directory.");
			System.out.println("Problem writing file for " + url + " to the " + outputFolder + " directory.");
			shutdown();
			System.exit(-1);
		}

		if (!new File(fileName).exists())
			System.exit(0);

		return true;
	}

	public static void main(String[] args) {
		FileScraper core = new FileScraper();		

		core.scrapeAllUrls();
		
		try {
			String url = "https://fairsharing.org/";		
			
			core.scrape(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
