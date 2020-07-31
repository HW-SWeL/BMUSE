package hwu.elixir.scrape.scraper.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
import hwu.elixir.scrape.scraper.ScraperFilteredCore;

/**
 * Scrapes a list of URLs which come from a given file OR
 * scrapes a single specified URL.
 * 
 * Output is quads written to a file in a location specified in application.properties.
 *
 */
public class FileScraper extends ScraperFilteredCore {

	private static ArrayList<String> urlsToScrape = new ArrayList<>();

	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
	private static Logger logger = LoggerFactory.getLogger(FileScraper.class.getName());

	/**
	 * Read the file (specified in application.properties) and puts each URL into a
	 * list for later scraping
	 * 
	 */
	private void readFileList() {
		if (properties.getLocationOfSitesFile().equals("") || properties.getLocationOfSitesFile() == null) {
			logger.error("Please set property *locationOfSitesFile*");
			shutdown();
			System.exit(-1);
		}

		File sitesList = new File(properties.getLocationOfSitesFile());
		if (!sitesList.exists()) {
			logger.error("Cannot find file *" + properties.getLocationOfSitesFile() + "*. Please set correct value for locationOfSitesFile");
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
			logger.error("Problem reading sites from file *" + properties.getLocationOfSitesFile() + "*.");
			shutdown();
			System.exit(-1);
		}

		logger.info("Read " + urlsToScrape.size() + " urls to scrape from " + properties.getLocationOfSitesFile() +".\n");
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

		long contextCounter = properties.getContextCounter();
		
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
						result = scrape(sitemapURL.text(), properties.getOutputFolder(), null, contextCounter++);
					} catch (FourZeroFourException e) {
						logger.error(url + "returned a 404.");
					} catch (JsonLDInspectionException e) {
						logger.error("The JSON-LD could be not parsed for " + url);
					} catch (CannotWriteException e) {
						logger.error("Problem writing file for " + url + " to the " + properties.getOutputFolder() + " directory.");
					} catch (MissingMarkupException e) {
						logger.error("Problem obtaining markup from " + url + ".");
					}
					displayResult(sitemapURL.text(), result, properties.getOutputFolder());
					sitemapCount++;
					if (properties.getMaxLimitScrape() < sitemapCount) {
						logger.info("MAX SITEMAP LIMIT REACHED: " + properties.getMaxLimitScrape());
						logger.info("Scraping over");
						break;
					}
				}
			} else {
				try {
					result = scrape(url, properties.getOutputFolder(), null, contextCounter++);
				} catch (FourZeroFourException e) {
					logger.error(url + "returned a 404.");
				} catch (JsonLDInspectionException e) {
					logger.error("The JSON-LD could be not parsed for " + url);
				} catch (CannotWriteException e) {
					logger.error("Problem writing file for " + url + " to the " + properties.getOutputFolder() + " directory.");
				} catch (MissingMarkupException e) {
					logger.error("Problem obtaining markup from " + url + ".");
				}

				displayResult(url, result, properties.getOutputFolder());
			}


		}
		logger.info("Scraping over.");
		properties.setContextCounter(contextCounter);
		properties.updateConfig();
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
