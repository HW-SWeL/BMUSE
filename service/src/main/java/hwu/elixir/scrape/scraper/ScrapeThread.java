package hwu.elixir.scrape.scraper;

import java.util.Date;

import hwu.elixir.scrape.db.crawl.CrawlRecord;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;

/**
 * Provides a thread wrapper for the process of scraping. Scraping defined
 * elsewhere. Utilises the Scrape class to maintain the list of sites to scrape
 * and those already scraped.
 * 
 * @author kcm
 *
 * @see Scraper
 * @see ScrapeState
 *
 */
public class ScrapeThread extends Thread {
	private ScrapeState scrapeState;
	private ServiceScraper process;
	private int waitTime;
	private String folderToWriteNQTo;

	/**
	 * 
	 * @param scrapeState Object that maintains state across threads.
	 * @param waitTime    How long (in seconds) thread should wait after scraping
	 *                    page before attempting new page.
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

			record.setContext("https://bioschemas.org/crawl/v1/" + record.getId());			
			record.setDateScraped(new Date());
			
			try {				
				if (process.scrape(record.getUrl(), record.getId(), folderToWriteNQTo, record.getStatus())) {
					scrapeState.addSuccessfulScrapedURL(record);
				} else {
					scrapeState.addFailedToScrapeURL(record);
				}
			} catch (HtmlExtractorServiceException serviceException) {
				scrapeState.addFailedToScrapeURL(record);
			} catch(FourZeroFourException fourZeroFourException) {
				scrapeState.setStatusTo404(record);
			} catch (JsonLDInspectionException je) {
				scrapeState.setStatusToHumanInspection(record);
			}
			try {
				ScrapeThread.sleep(1000 * waitTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}