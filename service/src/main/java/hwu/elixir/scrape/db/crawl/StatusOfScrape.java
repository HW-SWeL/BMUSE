package hwu.elixir.scrape.db.crawl;

/**
 * 
 * {@link hwu.elixir.scrape.db.crawl.StatusOfScrape} describes the possible status levels the scrape for each URL/CrawlRecord.
 * 
 * Each URL/CrawlRecord can have one of the following:
 * DOES_NOT_EXIST = 404.
 * HUMAN_INSPECTION = cannot parse for some reason; a human should see what is happening.
 * UNTRIED = not scraped yet.
 * FAILED = one failed attempt at scraping; will try again.
 * GIVEN_UP = two failed attempts at scraping. Will not try again.
 * SUCCESS = successfully scraped.
 *
 */

public enum StatusOfScrape {
	DOES_NOT_EXIST, HUMAN_INSPECTION, UNTRIED, FAILED, GIVEN_UP, SUCCESS;
}
