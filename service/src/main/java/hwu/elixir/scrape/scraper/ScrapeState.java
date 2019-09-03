package hwu.elixir.scrape.scraper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hwu.elixir.scrape.db.crawl.CrawlRecord;
import hwu.elixir.scrape.db.crawl.StateOfCrawl;

/**
 * Stores the list of pages to be scraped, which are accessed by each
 * ScrapeThread.
 * 
 * Each getter/setter method is sychronized so this *should be* thread safe.
 */
public class ScrapeState {

	private List<CrawlRecord> urlsToScrape = Collections.synchronizedList(new ArrayList<CrawlRecord>());
	private List<CrawlRecord> urlsProcessed = Collections.synchronizedList(new ArrayList<CrawlRecord>());  // should this be a set?

	/**
	 * 
	 * @param pagesToBeScraped The list of sites to be scraped
	 * @see ScrapeThread
	 */
	public ScrapeState(List<CrawlRecord> pagesToBeScraped) {
		urlsToScrape.addAll(pagesToBeScraped);
	}

	/**
	 * 
	 * @return Any pages/URLs left to scrape?
	 */
	public synchronized boolean pagesLeftToScrape() {
		return !urlsToScrape.isEmpty();
	}

	/**
	 * 
	 * @return First page/URL that needs to be scraped next
	 */
	public synchronized CrawlRecord getURLToProcess() {
		if (urlsToScrape.isEmpty())
			return null;

		return urlsToScrape.remove(0);
	}

	/**
	 * 
	 * @param url The latest URL/page that has been successfully scraped
	 */
	public synchronized void addSuccessfulScrapedURL(CrawlRecord record) {
		record.setStatus(StateOfCrawl.SUCCESS);
		urlsProcessed.add(record);
	}

	/**
	 * 
	 * @param url The latest URL/page that has been successfully scraped
	 */
	public synchronized void addFailedToScrapeURL(CrawlRecord record) {
		if (record.getStatus().equals(StateOfCrawl.FAILED)) {
			record.setStatus(StateOfCrawl.GIVEN_UP);
		} else {
			record.setStatus(StateOfCrawl.FAILED);
		}
		urlsProcessed.add(record);
	}

	/**
	 * 
	 * @param url The latest URL/page that has been 404d
	 */
	public synchronized void setStatusTo404(CrawlRecord record) {
		record.setStatus(StateOfCrawl.DOES_NOT_EXIST);
		urlsProcessed.add(record);
	}	
	
	
	/**
	 * 
	 * @param url The latest URL/page that needs human inspection
	 */
	public synchronized void setStatusToHumanInspection(CrawlRecord record) {
		record.setStatus(StateOfCrawl.HUMAN_INSPECTION);
		urlsProcessed.add(record);
	}	
	
	
	/**
	 * 
	 * @return Number of pages left to scrape
	 */
	public synchronized int getNumberPagesLeftToScrape() {
		return urlsToScrape.size();
	}

	/**
	 * Gets the full list of pages that have been processed
	 * 
	 * @return
	 */
	public synchronized List<CrawlRecord> getPagesProcessed() {
		return urlsProcessed;
	}
}