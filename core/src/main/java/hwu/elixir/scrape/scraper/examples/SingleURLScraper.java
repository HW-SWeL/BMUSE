package hwu.elixir.scrape.scraper.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.scraper.ScraperFilteredCore;

/**
 * 
 * Scrapes (bio)schema markup from a given URL and writes output (in NQuads) to home directory.
 * 
 * 
 */
public class SingleURLScraper extends ScraperFilteredCore {

	private static String outputFolder = System.getProperty("user.home");
	private static Logger logger = LoggerFactory.getLogger(System.class.getName());

	/**
	 * Scrape a given URL and write to file in the home directory. Output will be in NQuads format.
	 * 
	 * @param url The URL to scrape
	 * @throws  
	 * @throws FourZeroFourException
	 * @throws JsonLDInspectionException
	 * @throws CannotWriteException
	 */
	public void scrapeASingleURL(String url, String outputFileName) {
		try {
			displayResult(url, scrape(url, outputFolder, outputFileName, 0L), outputFolder);
		} catch (FourZeroFourException | JsonLDInspectionException e) {
			logger.error("Cannot scrape site; error thrown", e);
		} catch (CannotWriteException e) {
			logger.error("Problem writing file for to the " + outputFolder + " directory.");	
		} catch (MissingMarkupException e) {
			logger.error("Problem obtaining markup from " + url + ".");
		} finally {
			shutdown();
		}
	}

	public static void main(String[] args) {
		SingleURLScraper scraper = new SingleURLScraper();

		scraper.scrapeASingleURL("https://www.uniprot.org/uniprot/P46736", "uniprot");
	}
}
