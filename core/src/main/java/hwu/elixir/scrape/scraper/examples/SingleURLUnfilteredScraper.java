package hwu.elixir.scrape.scraper.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.exceptions.NTriplesParsingException;
import hwu.elixir.scrape.scraper.ScraperUnFilteredCore;

public class SingleURLUnfilteredScraper extends ScraperUnFilteredCore {
	
	private static Logger logger = LoggerFactory.getLogger(SingleURLUnfilteredScraper.class.getName());
	
	public void scrapeASingleUnfilteredURL(String url) {
		String jsonLD = "nothing scraped";
		try {
			jsonLD = scrapeUnfilteredMarkupAsJsonLDFromUrl(url);
		} catch (FourZeroFourException e ) {
			logger.error(url + " returned a 404");
		} catch (MissingHTMLException e) {
			logger.error("Trouble reading/parsing HTML from " + url);
		} catch (MissingMarkupException e) {
			logger.error("Cannot find any markup in " + url);
		} catch (NTriplesParsingException e) {
			logger.error("Cannot turn markup from " + url + " into valid nTriples");
		} finally {
			shutdown();
		}
		System.out.println(jsonLD);	
	}
	
	
	public static void main(String[] args) {
		String url = "http://biotea.github.io/bioschemas/?pmc=35353";
		SingleURLUnfilteredScraper scraper = new SingleURLUnfilteredScraper();
		scraper.scrapeASingleUnfilteredURL(url);
	}

}
