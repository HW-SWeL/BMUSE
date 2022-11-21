package hwu.elixir.scrape.scraper.examples;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
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
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
	private static final int maxURLs = 50000;

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
			String sitemapExt = url.substring(urlLength - 3, urlLength);
			// this checks only the extension at the ending
			if (sitemapExt.equalsIgnoreCase(".gz")){
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
	private Elements getSitemapURLs(Document sitemapContent, String sitemapKey) {

			Elements elements = sitemapContent.select(sitemapKey);

		return elements;
	}

	// This method returns only the URL of something to scrape, in case there is a dynamic/static scrape flag
	private String getURLFromTextLine(String url) {
		String URL = url.substring(0, url.indexOf(","));
		return URL;
	}


	/**
	 * Method that returns the dynamic/static setting (per url) from the url file list
	 * @param url
	 * @return flag
	 */
	private String getDynamicORStaticFlag (String url) {
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
	public void scrapeAllUrls(ArrayList<String> urlsToScrape) throws IOException {

		// Load the settings from the application.properties file
		readFileList();

		long contextCounter = properties.getContextCounter();
		String outputFolder = properties.getOutputFolder();
		boolean dynamicScrape = properties.dynamic();

		// Loop through all the URLs in the file
		for (String url : urlsToScrape) {
			int result = 0;

			// If there is a comma on the URL, which indicates a dynamic/static flag
			// or maybe just a mistake, check if static/dynamic is setup as flag and
			// scrape the URL accordingly, if some other text as flag just ignore text and
			// scrape URL with the default approach (dynamic), this is done on a per URL function
			if (url.indexOf(",") != -1){
				String tempFlag = getDynamicORStaticFlag(url);
				if (tempFlag.equalsIgnoreCase("static")){
					dynamicScrape = false;
					logger.info("Static scrape (local setting)");
				} else if (tempFlag.equalsIgnoreCase("dynamic")){
					dynamicScrape = true;
					logger.info("Dynamic scrape (local setting)");
				} else if (tempFlag.equalsIgnoreCase("unknown")) {
					//This case is when the flag in not set to dynamic or static, because it is
					//not known what is the case, the safest option is to set to dynamic
					dynamicScrape = true;
					logger.info("Unknown local setting, scraper set to default (dynamic)");
				}

				url = getURLFromTextLine(url);
			}

			logger.info("Attempting to scrape: " + url);

			// Check if the word sitemap is part of the URL (assumes that the URL is a sitemap if true)
			boolean sitemap = url.toLowerCase().contains("sitemap");


			if (sitemap) {
				/*int sitemapCount = 0, maximumLimit = properties.getMaxLimitScrape();
				boolean scraped, isSitemapIndex = false, isSitemap = false;
				Elements sitemapURLs, sitemapIndexList;
				Document sitemapContent = new Document(url);
				try {
					sitemapContent = getSitemap(url);
				} catch (IOException e){
					logger.error(e.getMessage());
				}*/

				Document sitemapContent = new Document(url);

				// Traverse sitemap index using xpath expression
				W3CDom w3cDom = new W3CDom();
				XPath xPath = XPathFactory.newInstance().newXPath();
				String expression = "/*"; // Select root node
				try {
					Object res = xPath.compile(expression).evaluate(w3cDom.fromJsoup(sitemapContent), XPathConstants.NODESET);
					NodeList nodes = (NodeList) res;
					logger.info("xPath: " + nodes.item(0).getNodeName());
					//if true this is sitemap index
					if (nodes.item(0).getNodeName().equalsIgnoreCase("sitemapindex")){
						logger.info("Sitemapindex found in URL: " + url);
						Elements sitemaps = getSitemapURLs(sitemapContent, "loc");
						for (Element indexSitemap : sitemaps){
							//logger.info(nodes.item(i).getTextContent());
							sitemapContent = getSitemap(indexSitemap.toString());
							scrapeSitemap((String[]) getSitemapURLs(sitemapContent, "loc").toArray(), contextCounter, dynamicScrape, outputFolder);
						}
						//if true this is a sitemap
					} else if (nodes.item(0).getNodeName().equalsIgnoreCase("urlset")){
						logger.info("Sitemap found in URL: " + url);
						/*for (int i=0; i<nodes.getLength(); i++){
							logger.info(nodes.item(i).getTextContent());
						}*/
						sitemapContent = getSitemap(url);
						scrapeSitemap((String[]) getSitemapURLs(sitemapContent, "loc").toArray(), contextCounter, dynamicScrape, outputFolder);
					} else {
						logger.warn("Unknown sitemap type");
					}

				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}


				/*if (isSitemapIndex){
					logger.info("SITEMAPINDEX");
					//get the urls for all the sitemaps and store them
					ArrayList<String> nestedUrlsToScrape = new ArrayList<>();
					sitemapIndexList = getSitemapURLs(sitemapContent, "loc");
					//loop the list of the sitemaps and get all the list of urls
					//store them in an array and make that into an outer nest
					for (Element sitemap : sitemapIndexList) {
						nestedUrlsToScrape.add(getSitemap(sitemap.toString()).toString());
					}
					scrapeAllUrls(nestedUrlsToScrape);
				}
				if (isSitemap){
					sitemapURLs = getSitemapURLs(sitemapContent, "loc");
				}

				sitemapURLs = getSitemapURLs(sitemapContent, "loc");
				sitemapURLs.toArray();*/

				// get sitemap list
				// for url in sitemap URLs list
				//scrapeSitemap(sitemapURLs, contextCounter, dynamicScrape, outputFolder);
				/*for (Element sitemapURL : sitemapURLs) {
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
				}*/
			} else { // else just scrape as a website that has markup
				String[] urls = {url};
				try {
					result = scrape(urls, properties.getOutputFolder(), null, contextCounter++, dynamicScrape);
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


	private void scrapeSitemap(String[] sitemapURLs, long contextCounter, boolean dynamicScrape, String outputFolder){
		boolean scraped = false;
		int result = 0;

		if (maxURLs<sitemapURLs.length){
			logger.info("MAX SITEMAP LIMIT IS: " + maxURLs + ", Please make sure your sitemap does not exceed that limit and adhere to sitemap guidelines");
			logger.info("Scraping over");
			properties.setContextCounter(contextCounter);
			properties.updateConfig();
			shutdown();
		}


			logger.info("Attempting to scrape: " + sitemapURLs[result]);
			try {
				result = scrape(sitemapURLs, properties.getOutputFolder(), null, contextCounter++, dynamicScrape);
				scraped = true;
			} catch (FourZeroFourException e) {
				logger.error(sitemapURLs + "returned a 404.");
				unscrapedURLsToFile(outputFolder, null, sitemapURLs[result], contextCounter - 1L);
				scraped = false;
			} catch (JsonLDInspectionException e) {
				logger.error("The JSON-LD could not be parsed for " + sitemapURLs);
				unscrapedURLsToFile(outputFolder, null, sitemapURLs[result], contextCounter - 1L);
				scraped = false;
			} catch (CannotWriteException e) {
				logger.error("Problem writing file for " + sitemapURLs[result] + " to the " + properties.getOutputFolder() + " directory.");
				unscrapedURLsToFile(outputFolder, null, sitemapURLs[result], contextCounter - 1L);
				scraped = false;
			} catch (MissingMarkupException e) {
				logger.error("Problem obtaining markup from " + sitemapURLs[result] + ".");
				unscrapedURLsToFile(outputFolder, null, sitemapURLs[result], contextCounter - 1L);
				scraped = false;
			}
			if (scraped) {
				displayResult(sitemapURLs[result], result, properties.getOutputFolder(), contextCounter - 1L);
			} else {
				logger.error("URL " + sitemapURLs[result] + " NOT SCRAPED, added to unscraped list");
			}

	}

	public static void main(String[] args) throws FourZeroFourException, JsonLDInspectionException, IOException {
		logger.info("*************************** STARTING SCRAPE: " + formatter.format(new Date(System.currentTimeMillis())));
		logger.info("Default charset: " + Charset.defaultCharset());
		FileScraper core = new FileScraper();

		// to scrape all URLs in the file specified in configuration.properties
		core.scrapeAllUrls(urlsToScrape);

		logger.info("*************************** ENDING SCRAPE: " + formatter.format(new Date(System.currentTimeMillis())));
		System.exit(0);
	}

}
