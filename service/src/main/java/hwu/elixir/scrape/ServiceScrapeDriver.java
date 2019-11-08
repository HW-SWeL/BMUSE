package hwu.elixir.scrape;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.db.crawl.CrawlRecord;
import hwu.elixir.scrape.db.crawl.DBAccess;
import hwu.elixir.scrape.scraper.ScrapeState;
import hwu.elixir.scrape.scraper.ScrapeThread;
import hwu.elixir.scrape.scraper.ServiceScraper;
import hwu.elixir.utils.Helpers;


/** 
 * Runs the scrape. Connects to a DBMS to collect a list of URLs (in the form of CrawlRecords) to scrape. 
 * Scrapes them in turn, writes the (bio)schema markup extracted to a file (1 file per URL)
 * and adds provenance to the CrawlRecord. Once all URLs extracted, the CrawlRecords are synced to the DBMS and 
 * another batch are fetched.
 * 
 *
 */
public class ServiceScrapeDriver {

	private static final String propertiesFile = "application.properties";

	private int waitTime = 1;
	private int numberOfPagesToCrawlInALoop;
	private int totalNumberOfPagesToCrawlInASession;
	private String outputFolder;
	private int pagesCounter = 0;
	private int scrapeVersion = 1;

	private DBAccess dba;
	
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

	private static Logger logger = LoggerFactory.getLogger(System.class.getName());


	/**
	 * Runs the scrape process
	 * 
	 * @param args Not used
	 */
	public static void main(String[] args) {
		ServiceScrapeDriver driver = new ServiceScrapeDriver();
		driver.runScrape();
		System.exit(0);
	}
	
	/** 
	 * Fires off threads and organises update of DBMS at end before starting another loop
	 * 
	 * Originally designed as a multi-threaded process; now reduced to a single thread as 
	 * the selenium webdriver is too expensive to run multi-threaded. However, the threading
	 * as been left in situ in case it is useful in the future.
	 * 
	 */
	private void runScrape() {
		processProperties();
		ServiceScraper scrapeOne = new ServiceScraper();		
		logger.info("STARTING CRAWL: " + formatter.format(new Date(System.currentTimeMillis())));
		while (pagesCounter < totalNumberOfPagesToCrawlInASession) {
			logger.info(pagesCounter + " scraped of " + totalNumberOfPagesToCrawlInASession);
			List<CrawlRecord> pagesToPull = generatePagesToPull();
			if (pagesToPull.isEmpty()) {
				logger.error("Could not find list of URLs to scrape!");
				break;
			}
			ScrapeState scrapeState = new ScrapeState(pagesToPull);
			
			ScrapeThread scrape1 = new ScrapeThread(scrapeOne, scrapeState, waitTime, outputFolder, scrapeVersion);
			scrape1.setName("S1");

			scrape1.start();
			long startTime = System.nanoTime();
			
			try {
				scrape1.join();
			} catch (InterruptedException e) {
				logger.error("Exception waiting on thread");
				e.printStackTrace();
				dba.resetBeingScraped(scrapeState.getPagesProcessedAndUnprocessed());
				dba.shutdown();
				scrapeOne.shutdown();
				return;				
			}
			
			if(!scrape1.isFileWritten()) {
				logger.error("Could not write output file so shutting down!");
				dba.resetBeingScraped(scrapeState.getPagesProcessedAndUnprocessed());
				scrapeOne.shutdown();
				dba.shutdown();
				Date date = new Date(System.currentTimeMillis());
				logger.info("ENDING CRAWL after failure at: " + formatter.format(date));					
				return;
			}
			
			logger.debug("Value of isFileWritten: " + scrape1.isFileWritten());

			long endTime = System.nanoTime();
			long timeElapsed = endTime - startTime;
			logger.info("Time in s to complete: " + timeElapsed / 1e+9);

			updateDatabase(scrapeState);
			pagesCounter += numberOfPagesToCrawlInALoop;
			logger.info("ENDED loop");
		}
		scrapeOne.shutdown();
		dba.shutdown();
		logger.info("ENDING CRAWL: " + formatter.format(new Date(System.currentTimeMillis())));
	}

	/**
	 * Updates the DBMS by syncing CrawlRecords at end
	 * 
	 * @param scrapeState State of scrape at end
	 * @return true if success / false otherwise
	 * @see ScrapeState
	 * @see CrawlRecord
	 */
	private boolean updateDatabase(ScrapeState scrapeState) {
		boolean result = false;
		try {
			if(dba == null)
				dba = new DBAccess();
			result = dba.updateAllCrawlRecords(scrapeState.getPagesProcessed());
		} catch (Exception e) {
			System.out.println("Exception thrown by updated database");
			e.printStackTrace();
			return false;
		} finally {
			dba.close();
		}
		return result;
	}

	/**
	 * Get a list of URLs (in the form of CrawlRecords) that need to be scraped 
	 * from DBMS.
	 * 
	 * @return List of URLs to be scraped
	 * @see CrawlRecord
	 */
	private List<CrawlRecord> generatePagesToPull() {	
		try {
			if(dba == null)
				dba = new DBAccess();
			return dba.getSomeCrawlRecords(numberOfPagesToCrawlInALoop);
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			dba.close();
		}
		return null;
	}

	/**
	 * Updates properties based on properties file in src > main > resources
	 * 
	 */
	private void processProperties() {
		ClassLoader classLoader = ServiceScrapeDriver.class.getClassLoader();

		InputStream is = classLoader.getResourceAsStream(propertiesFile);
		if(is == null) {
			logger.error("     Cannot find " + propertiesFile + " file");
			throw new IllegalArgumentException(propertiesFile + "file is not found!");
		}

		Properties prop = new Properties();

		try {
			prop.load(is);
		} catch (IOException e) {
			logger.error("     Cannot load application.properties", e);
			System.exit(0);
		}

		waitTime = Integer.parseInt(prop.getProperty("waitTime").trim());
		logger.info("     waitTime: " + waitTime);
		outputFolder = prop.getProperty("outputFolder").trim();
		outputFolder += "_"+Helpers.getDateForName()+"/";
		logger.info("     outputFolder: " + outputFolder);		
		numberOfPagesToCrawlInALoop = Integer.parseInt(prop.getProperty("numberOfPagesToCrawlInALoop").trim());
		logger.info("     numberOfPagesToCrawl: " + numberOfPagesToCrawlInALoop);
		totalNumberOfPagesToCrawlInASession = Integer.parseInt(prop.getProperty("totalNumberOfPagesToCrawlInASession").trim());
		logger.info("     totalNumberOfPagesToCrawlInASession: " + totalNumberOfPagesToCrawlInASession);
		scrapeVersion = Integer.parseInt(prop.getProperty("scrapeVersion").trim());
		logger.info("     scrapeVersion: " + scrapeVersion);		
		logger.info("\n\n\n");
	}

}
