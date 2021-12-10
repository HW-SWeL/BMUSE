package hwu.elixir.scrape.scraper.examples;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
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

import hwu.elixir.utils.Helpers;

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
	 * Method that parses a sitemap.xml file, compressed (.gz) or uncompressed
	 *
	 *
	 */
	private Document getSitemap(String url) throws IOException {
		Document sitemapContent = new Document(url);

		try {
			int urlLength = url.length();
			logger.info("parse sitemap list");
			String sitemapExt = url.substring(urlLength - 3, urlLength);
			if (sitemapExt.equalsIgnoreCase(".gz")){ // this checks only the extension at the ending
				logger.info("compressed sitemap");
				byte[] bytes = Jsoup.connect(url).ignoreContentType(true).execute().bodyAsBytes();
				sitemapContent = Helpers.gzipFileDecompression(bytes);
			} else {
				sitemapContent = Jsoup.connect(url).maxBodySize(0).get();
			}
		} catch (IOException e){
			logger.error("Jsoup parsing exception: " + e.getMessage());
		}


		return sitemapContent;
	}

	/**
	 * Method that parses a sitemap and then returns the list of URLs of all samples as Elements
	 *
	 *
	 */
	private Elements getSitemapList(String url, String sitemapURLKey) throws IOException {

		Document doc = new Document(url);
		Document urlSitemapListsNested;
		Elements elements = new Elements();
		Elements sitemaps = new Elements();
		boolean sitemapindex = false;
		boolean urlset = false;

		try {
			int urlLength = url.length();
			logger.info("parse sitemap list");
			String sitemapExt = url.substring(urlLength - 3, urlLength);
			if (sitemapExt.equalsIgnoreCase(".gz")){ // this checks only the extension at the ending
				logger.info("compressed sitemap");
				byte[] bytes = Jsoup.connect(url).ignoreContentType(true).execute().bodyAsBytes();
				doc = Helpers.gzipFileDecompression(bytes);
			} else {
				doc = Jsoup.connect(url).maxBodySize(0).get();
			}

		} catch (IOException e) {
			logger.error("Jsoup parsing exception: " + e.getMessage());
		}


		try {

			elements = doc.select(sitemapURLKey);

			// check the html if it is a sitemapindex or a urlset
			Elements content = doc.select("urlset");

			sitemapindex = doc.outerHtml().contains("sitemapindex");
			urlset = doc.outerHtml().contains("urlset");
		} catch (NullPointerException e) {
			logger.error(e.getMessage());
		}

		if (sitemapindex){
			// if sitemapindex get the loc of all the sitemaps
			// added warning for sitemap index files
			logger.warn("please note this is a sitemapindex file which is not currently supported, please use the content (url) of the urlset instead");
			sitemaps = doc.select("loc");
		}

		return elements;
	}

	private String getURLFromTextLine(String url) {
		String URL = url.substring(0, url.indexOf(","));
		return URL;
	}


	/**
	 * Method that returns the dynamic/static setting (per url) from the url file list
	 * @param url
	 * @return flag
	 */
	private String getDynamicStaticFlag (String url) {
		String flag = url.substring(url.indexOf(",") + 1 ).trim();

		if (flag.equalsIgnoreCase("static")){
			flag = "static";
			//we have a static option on the file after the comma
		} else if (flag.equalsIgnoreCase("dynamic")) {
			flag = "dynamic";
		} else {
			flag = "unknown";
		}

		return flag;
	}

	/**
	 * Method that writes URLs that have not been successfully scraped for possible future scraping
	 * The method creates a new text file in the same directory that the nQuads are stored
	 */
	private void unscrapedURLsToFile (String outFolderName, String outFileName, String unscrapedURL, long contextCounter) {

		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter out = null;
		File outputDirectory = new File(outFolderName);

		// If folder does not exist, create the folder
		if (!outputDirectory.exists()){
			outputDirectory.mkdir();
		}

		if (outFileName == null) {
			outFileName = outFolderName + "/" + "unscraped_" + contextCounter + ".txt";
		} else {
			outFileName = outFolderName + "/" + "unscraped_" + outFileName + ".txt";
		}

		try {
			fw = new FileWriter(outFileName, true);
			bw = new BufferedWriter(fw);
			out = new PrintWriter(bw);
			out.println(unscrapedURL);
			out.close();
		} catch (Exception e) {
			logger.error("Problem writing to unscraped file for " + unscrapedURL, e);
			e.printStackTrace();
		}
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
		String outputFolder = properties.getOutputFolder();
		boolean dynamicScrape = properties.dynamic();

		for (String url : urlsToScrape) {
			boolean result = false;



			// If there is a comma on the URL, which indicates a dynamic/static flag
			// or maybe just a mistake, check if static/dynamic is setup as flag and
			// scrape the URL accordingly, if some other text as flag just ignore text and
			// scrape URL with the default approach (dynamic), this is done on a per URL function

			if (url.indexOf(",") != -1){
				String tempFlag = getDynamicStaticFlag(url);
				if (tempFlag.equals("static")){
					dynamicScrape = false;
					logger.info("Static scrape (local setting)");
				} else if (tempFlag.equals("dynamic")){
					dynamicScrape = true;
					logger.info("Dynamic scrape (local setting)");
				} else if (tempFlag.equals("unknown")) {
					//This case is when the flag in not set to dynamic or static, because it is
					//not known what is the case, the safest option is to set to dynamic
					dynamicScrape = true;
					logger.info("Unknown local setting, scraper set to default (dynamic)");
				}

				url = getURLFromTextLine(url);
			}

			logger.info("Attempting to scrape: " + url);

			// Check if the word sitemap is part of the URL (assumes that the URL is a sitemap if true)
			if (url.toLowerCase().indexOf("sitemap") != -1) {
				// fixme a good idea would be to check that the word sitemap is not in the URL, if it is, it would mean that this is a sitemap index
				int sitemapCount = 0;
				int maximumLimit = properties.getMaxLimitScrape();
				boolean scraped = false;
				Elements sitemapList = new Elements();
				try {
					sitemapList = getSitemapList(url, "loc"); //loc will only parse the url of the sample
				} catch (IOException e) {
					e.printStackTrace();
				}
				sitemapList.toArray();
				logger.info("Sitemap found in URL: " + url);
				// get sitemap list
				// for url in sitemap list
				for (Element sitemapURL : sitemapList) {
					logger.info("Attempting to scrape: " + sitemapURL.text());
					try {
						result = scrape(sitemapURL.text(), properties.getOutputFolder(), null, contextCounter++, dynamicScrape);
						scraped = true;
					} catch (FourZeroFourException e) {
						logger.error(url + "returned a 404.");
						unscrapedURLsToFile(outputFolder, null, sitemapURL.text(), contextCounter - 1L);
						scraped = false;
					} catch (JsonLDInspectionException e) {
						logger.error("The JSON-LD could not be parsed for " + sitemapURL.text());
						unscrapedURLsToFile(outputFolder, null, sitemapURL.text(), contextCounter - 1L);
						scraped = false;
					} catch (CannotWriteException e) {
						logger.error("Problem writing file for " + sitemapURL.text() + " to the " + properties.getOutputFolder() + " directory.");
						unscrapedURLsToFile(outputFolder, null, sitemapURL.text(), contextCounter - 1L);
						scraped = false;
					} catch (MissingMarkupException e) {
						logger.error("Problem obtaining markup from " + sitemapURL.text() + ".");
						unscrapedURLsToFile(outputFolder, null, sitemapURL.text(), contextCounter - 1L);
						scraped = false;
					}
					if (scraped) {
						displayResult(sitemapURL.text(), result, properties.getOutputFolder(), contextCounter - 1L);
					} else {
						logger.error("URL " + sitemapURL.text() + " NOT SCRAPED, added to unscraped list");
					}
					sitemapCount++;
					if (maximumLimit < sitemapCount) {
						logger.info("MAX SITEMAP LIMIT REACHED: " + maximumLimit);
						logger.info("Scraping over");
						break;
					}
				}
			} else { // else just scrape as a website that has markup
				try {
					result = scrape(url, properties.getOutputFolder(), null, contextCounter++, dynamicScrape);
				} catch (FourZeroFourException e) {
					logger.error(url + "returned a 404.");
					unscrapedURLsToFile(outputFolder, null, url, contextCounter);
				} catch (JsonLDInspectionException e) {
					logger.error("The JSON-LD could not be parsed for " + url);
					unscrapedURLsToFile(outputFolder, null, url, contextCounter);
				} catch (CannotWriteException e) {
					logger.error("Problem writing file for " + url + " to the " + properties.getOutputFolder() + " directory.");
					unscrapedURLsToFile(outputFolder, null, url, contextCounter);
				} catch (MissingMarkupException e) {
					logger.error("Problem obtaining markup from " + url + ".");
					unscrapedURLsToFile(outputFolder, null, url, contextCounter);
				}
				displayResult(url, result, properties.getOutputFolder(), contextCounter - 1L);
			}

		}
		logger.info("Scraping over.");
		properties.setContextCounter(contextCounter);
		properties.updateConfig();
		shutdown();
	}

	public static void main(String[] args) throws FourZeroFourException, JsonLDInspectionException {
		logger.info("*************************** STARTING SCRAPE: " + formatter.format(new Date(System.currentTimeMillis())));
		logger.info("Default charset: " + Charset.defaultCharset());
		FileScraper core = new FileScraper();

		// to scrape all URLs in the file specified in configuration.properties
		core.scrapeAllUrls();

		logger.info("*************************** ENDING SCRAPE: " + formatter.format(new Date(System.currentTimeMillis())));
		System.exit(0);
	}

}
