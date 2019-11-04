package hwu.elixir.scrape.scraper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hwu.elixir.scrape.db.crawl.CrawlRecord;
import hwu.elixir.scrape.db.crawl.StatusOfScrape;

/**
 * {@link hwu.elixir.scrape.scraper.ScrapeState} maintains the current state of the crawl. A number of URLs
 * (in the form of {@link hwu.elixir.scrape.db.crawl.CrawlRecord}) are pulled from a DBMS and {@link hwu.elixir.scrape.scraper.ScrapeState}
 * maintains the list of {@link hwu.elixir.scrape.db.crawl.CrawlRecord} and also the status of the scrape before it is 
 * synced back to the DBMS.
 * 
 * Each getter/setter method is sychronized so this *should be* thread safe.
 * 
 * This class does not contact the DBMS.
 */
public class ScrapeState {

	private List<CrawlRecord> urlsToScrape = Collections.synchronizedList(new ArrayList<CrawlRecord>());
	private List<CrawlRecord> urlsProcessed = Collections.synchronizedList(new ArrayList<CrawlRecord>());  // should this be a set?

	/**
	 * 
	 * @param pagesToBeScraped The list of sites to be scraped
	 * @see ScrapeThread
	 * @see CrawlRecord
	 */
	public ScrapeState(List<CrawlRecord> pagesToBeScraped) {
		urlsToScrape.addAll(pagesToBeScraped);
	}

	/**
	 * Any pages/URLs left to scrape?
	 * @return True for yes & false for no
	 * @see CrawlRecord
	 */
	public synchronized boolean pagesLeftToScrape() {
		return !urlsToScrape.isEmpty();
	}

	/**
	 * Returns the next URL/CrawlRecord to be scraped
	 * 
	 * @return First page/URL that needs to be scraped next
	 * @see CrawlRecord
	 */
	public synchronized CrawlRecord getURLToProcess() {
		if (urlsToScrape.isEmpty())
			return null;

		return urlsToScrape.remove(0);
	}

	/**
	 * Adds the given CrawlRecord to the list of CrawlRecords successfully scraped.
	 * Updates the status of the CrawlRecord to SUCCESS.
	 * 
	 * @param url The latest URL/page that has been successfully scraped
	 * @see CrawlRecord
	 */
	public synchronized void addSuccessfulScrapedURL(CrawlRecord record) {
		record.setStatus(StatusOfScrape.SUCCESS);
		urlsProcessed.add(record);
	}

	/**
	 * Adds the given CrawlRecord to the list of CrawlRecords NOT successfully scraped.
	 * Updates the status of the CrawlRecord; if first failure the status is FAILED.
	 * If status is already FAILED it is changed to GIVEN_UP.
	 * 
	 * If the status is FAILED, another try will be made in a future run.
	 *  
	 * 
	 * @param url The latest URL/page that has been unsuccessfully scraped
	 * @see CrawlRecord
	 */
	public synchronized void addFailedToScrapeURL(CrawlRecord record) {
		if (record.getStatus().equals(StatusOfScrape.FAILED)) {
			record.setStatus(StatusOfScrape.GIVEN_UP);
		} else {
			record.setStatus(StatusOfScrape.FAILED);
		}
		urlsProcessed.add(record);
	}

	/**
	 * Changes the status of the CrawlRecord to DOES_NOT_EXIST.
	 * As Selenium does not return the HTTP codes, it is questionable 
	 * how useful this is.
	 * 
	 * 
	 * @param url The latest URL/page that has been 404'd
	 * @see CrawlRecord
	 */
	public synchronized void setStatusTo404(CrawlRecord record) {
		record.setStatus(StatusOfScrape.DOES_NOT_EXIST);
		urlsProcessed.add(record);
	}	
	
	
	/**
	 * 
	 * Changes the status of the CrawlRecord to HUMAN_INSPECTION.
	 * This captures the idea that the URLs may contain unexpected markup that needs a human to 
	 * review and possibly update the scraper. 
	 * 
	 * @param url The latest URL/page that needs human inspection
	 * @see CrawlRecord
	 */
	public synchronized void setStatusToHumanInspection(CrawlRecord record) {
		record.setStatus(StatusOfScrape.HUMAN_INSPECTION);
		urlsProcessed.add(record);
	}	
	
	
	/**
	 * Returns the number of URLs that are still to be scraped in this cycle. 
	 * This does not return the number of URLs left to scrape in the DBMS, just in the current cycle.
	 * 
	 * @return Number of URLs left to scrape in this cycle
	 * @see CrawlRecord
	 */
	public synchronized int getNumberPagesLeftToScrape() {
		return urlsToScrape.size();
	}

	/**
	 * Gets the full list of URLs that have been processed in this cycle.
	 * This does not return the number of URLs that have been scraped in total across all cycles.
	 * 
	 * @return
	 * @see CrawlRecord
	 */
	public synchronized List<CrawlRecord> getPagesProcessed() {
		return urlsProcessed;
	}
	
	/**
	 * Gets the full list of URLs/CrawlRecords regardless of whether scraped or not in the current cycle.
	 * 
	 * @return List of all CrawlRecords in this cycle.
	 * @see CrawlRecord
	 */
	public synchronized List<CrawlRecord> getPagesProcessedAndUnprocessed() {
		List<CrawlRecord> urlsCombined = Collections.synchronizedList(new ArrayList<CrawlRecord>());
		urlsCombined.addAll(urlsProcessed);
		urlsCombined.addAll(urlsToScrape);
		return urlsCombined;
	}	
}