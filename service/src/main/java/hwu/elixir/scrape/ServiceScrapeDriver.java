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
 * Runs the scrape. Connects to DBMS to collect a list of URLs (in the forms of CrawlRecords) to scrape. 
 * Scrapes them in turn (using 8 threads), writes the (bio)schema markup extracted to a file (1 file per URL)
 * and adds provenance to the CrawlRecord. Once all URLs extracted, the CrawlRecords are synced to the DBMS and 
 * another batch are fetched.
 * 
 * @author kcm
 *
 */
public class ServiceScrapeDriver {

	private static final String propertiesFile = "application.properties";

	private static int waitTime = 1;
	private static int numberOfPagesToCrawlInALoop;
	private static int totalNumberOfPagesToCrawlInASession;
	private static String outputFolder;
	private static int pagesCounter = 0;

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
	 */
	private void runScrape() {
		processProperties();
		ServiceScraper scrapeOne = new ServiceScraper();		
		logger.info("STARTING CRAWL: " + formatter.format(new Date(System.currentTimeMillis())));
		while (pagesCounter < totalNumberOfPagesToCrawlInASession) {
			System.out.println(pagesCounter + " is less than " + totalNumberOfPagesToCrawlInASession);
			logger.info(pagesCounter + " scraped of " + totalNumberOfPagesToCrawlInASession);
			List<CrawlRecord> pagesToPull = generatePagesToPull();
			if (pagesToPull.isEmpty()) {
				logger.error("Could not find list of URLs to scrape!");
				break;
			}
			ScrapeState scrapeState = new ScrapeState(pagesToPull);
			
			ScrapeThread scrape1 = new ScrapeThread(scrapeOne, scrapeState, waitTime, outputFolder);
			scrape1.setName("S1");

			scrape1.start();
			long startTime = System.nanoTime();
			
			try {
				scrape1.join();
			} catch (InterruptedException e) {
				System.out.println("Exception waiting on thread");
				e.printStackTrace();
			}
			
			if(!scrape1.isWorked()) {
				logger.error("Could not write output file so shutting down!");
				dba.resetBeingScraped(scrapeState.getPagesProcessedAndUnprocessed());
				scrapeOne.shutdown();
				dba.shutdown();
				Date date = new Date(System.currentTimeMillis());
				logger.info("ENDING CRAWL after failure at: " + formatter.format(date));	
				System.out.println("CRAWL OVER!");
				System.exit(0);
			}
			
			logger.debug("Value of isWorked: " + scrape1.isWorked());

			long endTime = System.nanoTime();
			long timeElapsed = endTime - startTime;
			System.out.println("Time in s to complete: " + timeElapsed / 1e+9);
			logger.info("Time in s to complete: " + timeElapsed / 1e+9);

			updateDatabase(scrapeState);
			pagesCounter += numberOfPagesToCrawlInALoop;
			System.out.println("ENDED loop");
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
		logger.info("\n\n\n");
	}

}
