/**
 * 
 * Mimics the *crawl*. Provides a hibernate class ({@link hwu.elixir.scrape.db.crawl.CrawlRecord}) that is synced to the DBMS.
 * {@link hwu.elixir.scrape.db.crawl.DBAccess} controls communication with the DBMS. 
 * {@link hwu.elixir.scrape.db.crawl.StatusOfScrape} describes the possible status levels of each URL/CrawlRecord.
 * 
 */
package hwu.elixir.scrape.db.crawl;