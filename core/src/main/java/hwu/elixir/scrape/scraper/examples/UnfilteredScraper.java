package hwu.elixir.scrape.scraper.examples;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.exceptions.NTriplesParsingException;
import hwu.elixir.scrape.scraper.ScraperUnFilteredCore;

public class UnfilteredScraper extends ScraperUnFilteredCore {
	
	public static void main(String[] args) {
		
		String url = "http://biotea.github.io/bioschemas/?pmc=35353";
		UnfilteredScraper scraper = new UnfilteredScraper();
		
		String jsonLD = "nothing scraped";
		try {
//			jsonLD = scraper.scrapeUnfilteredMarkupAsNTriplesFromUrl(url);
			jsonLD = scraper.scrapeUnfilteredMarkupAsJsonLDFromUrl(url);
		} catch (FourZeroFourException | MissingHTMLException | MissingMarkupException | NTriplesParsingException e) {
			e.printStackTrace();
		} finally {
			scraper.shutdown();
		}
		
		System.out.println(jsonLD);		
	}

}
