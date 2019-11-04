/**
 * 
 * Actual scraper. Extends {@link hwu.elixir.scrape.scraper.ScraperFilteredCore}.
 * Originally, intended to be multi-threaded; however, this was too expensive when using Selenium.
 * Nevertheless the threading was left for possible future use.
 * 
 * {@link hwu.elixir.scrape.scraper.ScrapeState} maintains the current state of the crawl. A number of URLs
 * (in the form of {@link hwu.elixir.scrape.db.crawl.CrawlRecord}) are pulled from a DBMS and {@link hwu.elixir.scrape.scraper.ScrapeState}
 * maintains the list of {@link hwu.elixir.scrape.db.crawl.CrawlRecord} and also the status of the scrape before it is 
 * synced back to the DBMS.
 * 
 * {@link hwu.elixir.scrape.scraper.ServiceScraper} extends {@link hwu.elixir.scrape.scraper.ScraperFilteredCore} and provides the 
 * actual scraping functionality.
 * 
 * {@link hwu.elixir.scrape.scraper.ScrapeThread} manages the process of scraping. Obtaining a URL from {@link hwu.elixir.scrape.scraper.ScrapeState}
 * and giving it to {@link hwu.elixir.scrape.scraper.ServiceScraper} for actual scraping.
 * 
 */
package hwu.elixir.scrape.scraper;