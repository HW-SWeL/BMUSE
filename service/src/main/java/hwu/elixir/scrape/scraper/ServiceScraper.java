package hwu.elixir.scrape.scraper;

import hwu.elixir.scrape.db.crawl.StatusOfScrape;
import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.exceptions.SeleniumException;

/**
 * {@link hwu.elixir.scrape.scraper.ServiceScraper} extends {@link hwu.elixir.scrape.scraper.ScraperFilteredCore} and provides the 
 * actual scraping functionality.
 * 
 * Scrapes a given URL, converts into NQuads and writes to a file (name derived
 * from URL). If the file already exists it will be overwritten.
 * 
 * 
 * @see ScraperFilteredCore
 * 
 */
public class ServiceScraper extends ScraperFilteredCore {

	private StatusOfScrape status= null;

	/**
	 * Orchestrates the process of scraping a site before converting the extracted
	 * triples to NQuads and writing to a file.
	 * 
	 * @param url               Site to be scraped
	 * @param contextCounter    Number used to generate the named graph/context and
	 *                          the URLs used to replace blank nodes.
	 * @param outputFolderName Location to which the NQuads will be written
	 * @return True if success; false otherwise
	 * @throws FourZeroFourException 
	 * @throws JsonLDInspectionException
	 * @throws CannotWriteException 
	 * @throws MissingMarkupException 
	 * 
	 */
	public boolean scrape(String url, Long contextCounter, String outputFolderName, StatusOfScrape status) throws FourZeroFourException, JsonLDInspectionException, CannotWriteException, MissingMarkupException {
		this.status = status;
		return scrape(url, outputFolderName, null, contextCounter);
	}
	
	

	@Override	
	/* Now takes account of StateOfCrawl
	 */
	protected String wrapHTMLExtraction(String url) throws FourZeroFourException {
		String html = "";
		if (status.equals(StatusOfScrape.UNTRIED) || status.equals(StatusOfScrape.FAILED)) {
			try {
				html = getHtmlViaSelenium(url);
			} catch (SeleniumException e) {
				// try again
				try {
					html = getHtmlViaSelenium(url);
				} catch (SeleniumException e2) {
					return "";
				}
			}
		} else {
			return "";
		}
		return html;
	}
}
