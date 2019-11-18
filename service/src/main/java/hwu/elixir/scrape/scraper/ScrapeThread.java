package hwu.elixir.scrape.scraper;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.db.crawl.CrawlRecord;
import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;

/**
 * {@link hwu.elixir.scrape.scraper.ScrapeThread} manages the process of scraping. Obtaining a URL from {@link hwu.elixir.scrape.scraper.ScrapeState}
 * and giving it to {@link hwu.elixir.scrape.scraper.ServiceScraper} for actual scraping.
 * 
 *
 * @see ServiceScraper
 * @see ScrapeState
 *
 */
public class ScrapeThread extends Thread {
	private ScrapeState scrapeState;
	private ServiceScraper process;
	private int waitTime;
	private String folderToWriteNQTo;
	private boolean fileWritten = true;
	private int scrapeVersion = 1;
	
	
	private static Logger logger = LoggerFactory.getLogger(System.class.getName());
	

	/**
	 * Sets up a thread for actually scrapping. 
	 * 
	 * @param scraper Scraper that will actually do the scraping.
	 * @param scrapeState Object that maintains state across threads.
	 * @param waitTime    How long (in seconds) thread should wait after scraping
	 *                    page before attempting new page.
	 * @param folderToWriteNQTo Output folder for the NQuads files generated in this process.
	 * @param contextVersion The context URL used is 'https://bioschemas.org/crawl/CONTEXTVERSION/ID' Where ID is the id of the CrawlRecord pulled from the DBMS.
	 * 
	 * @see ScrapeState
	 * @see Scraper
	 */
	public ScrapeThread(ServiceScraper scraper, ScrapeState scrapeState, int waitTime, String folderToWriteNQTo, int contextVersion) {
		this.scrapeState = scrapeState;
		process = scraper;
		this.waitTime = waitTime;
		this.folderToWriteNQTo = folderToWriteNQTo;
		this.scrapeVersion = contextVersion;

	}
	
	/**
	 * 
	 * Sets up a thread for actually scrapping. contextVersion is set to 1.
	 * 
	 * @param scrapeState Object that maintains state across threads.
	 * @param waitTime    How long (in seconds) thread should wait after scraping
	 *                    page before attempting new page.
	 * @param waitTime    How long (in seconds) thread should wait after scraping
	 *                    page before attempting new page.
	 * @param folderToWriteNQTo Output folder for the NQuads files generated in this process.                    
	 * 
	 * @see ScrapeState
	 * @see Scraper
	 */
	public ScrapeThread(ServiceScraper scraper, ScrapeState scrapeState, int waitTime, String folderToWriteNQTo) {
		this.scrapeState = scrapeState;
		process = scraper;
		this.waitTime = waitTime;
		this.folderToWriteNQTo = folderToWriteNQTo;
	}	

	@Override
	/**
	 * Defines high-level process of scraping. Actual scraping done by an
	 * implementation of Scraper. If page scrape successful will add url to
	 * Scrape.sitesScraped
	 * 
	 * @see Scraper
	 * @see SimpleScraper
	 */
	public void run() {
		while (scrapeState.pagesLeftToScrape()) {
			CrawlRecord record = scrapeState.getURLToProcess();

			if (record == null)
				break;

			record.setContext("https://bioschemas.org/crawl/" + scrapeVersion +"/" + record.getId());			
			record.setDateScraped(new Date());
			
			try {				
				if (process.scrape(record.getUrl(), record.getId(), folderToWriteNQTo, record.getStatus())) {
					scrapeState.addSuccessfulScrapedURL(record);
				} else {
					scrapeState.addFailedToScrapeURL(record);
				}
			} catch(FourZeroFourException fourZeroFourException) {
				scrapeState.setStatusTo404(record);
				fileWritten = false;
			} catch (JsonLDInspectionException je) {
				scrapeState.setStatusToHumanInspection(record);
				fileWritten = false;
			} catch (CannotWriteException cannotWrite) {		
				logger.error("Caught cannot read file, setting worked to false!");
				fileWritten = false;
				scrapeState.addFailedToScrapeURL(record);
				return; // no point in continuing
			} catch (MissingMarkupException e) {
				logger.error("Cannot obtain markup from " + record.getUrl() +".");
				fileWritten = false;
				scrapeState.addFailedToScrapeURL(record);
			}
			try {
				ScrapeThread.sleep(100 * waitTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isFileWritten() {
		return fileWritten;
	}
}