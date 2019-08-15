package hwu.elixir.scrape.scraper;

import hwu.elixir.scrape.db.crawl.StateOfCrawl;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;

public interface Scraper {
	
	public boolean scrape(String url, Long contextCounter, String folderToWriteNQTo, StateOfCrawl status) throws HtmlExtractorServiceException, FourZeroFourException, JsonLDInspectionException;

}
