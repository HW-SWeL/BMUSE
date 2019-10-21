package hwu.elixir.scrape.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.db.crawl.StateOfCrawl;
import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.SeleniumException;

/**
 * Scrapes a given URL, converts into NQuads and writes to a file (name derived
 * from URL). If the file already exists it will be overwritten.
 * 
 * 
 * @see ScraperCore
 * 
 */
public class ServiceScraper extends ScraperCore {

	private StateOfCrawl status= null;
	
	private static Logger logger = LoggerFactory.getLogger("hwu.elixir.scrape.SimpleScraperImpl");

	/**
	 * Orchestrates the process of scraping a site before converting the extracted
	 * triples to NQuads and writing to a file.
	 * 
	 * @param url               Site to be scraped
	 * @param contextCounter    Number used to generate the named graph/context and
	 *                          the URLs used to replace blank nodes.
	 * @param folderToWriteNQTo Location to which the NQuads will be written
	 * @return True if success; false otherwise
	 * @throws FourZeroFourException 
	 * @throws JsonLDInspectionException
	 * @throws CannotWriteException 
	 * 
	 */
	public boolean scrape(String url, Long contextCounter, String folderToWriteNQTo, StateOfCrawl status) throws FourZeroFourException, JsonLDInspectionException, CannotWriteException {
		this.status = status;
		return scrape(url, null, contextCounter, folderToWriteNQTo);
	}
	
	

	@Override
	protected String wrapHTMLExtraction(String url) throws FourZeroFourException {
		String html = "";
		if (status.equals(StateOfCrawl.UNTRIED) || status.equals(StateOfCrawl.FAILED)) {
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
	

	public static void main(String[] args) {
		try {
			ServiceScraper scraper = new ServiceScraper();
//			scraper.scrape("https://www.bbc.co.uk", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
//			scraper.scrape("https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
//			scraper.scrape("https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/", 100001L, "/Users/kcm/", StateOfCrawl.FAILED);  // works
			scraper.scrape("https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/", 100002L, "/Users/kcm/", StateOfCrawl.GIVEN_UP);  // works
//			scraper.scrape("https://www.ebi.ac.uk/biosamples/samples/SAMEA4999347", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
//			scraper.scrape("https://www.ebi.ac.uk/biosamples/samples/SAMN00025378", 100000L, "/Users/kcm/",StateOfCrawl.UNTRIED); // 404s
//			scraper.scrape("https://www.alliancegenome.org/gene/MGI:2442292", 100000L, "/Users/kcm/",StateOfCrawl.FAILED_TWICE); // AllianceGenome
//			scraper.scrape("https://hamap.expasy.org/", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);
//			scraper.scrape("https://hamap.expasy.org/rule/MF_00191", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);

//			scraper.scrape("https://www.alliancegenome.org/gene/RGD:620474", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED); // BREAKS!
			
//			scraper.scrape("http://biotea.github.io/bioschemas/?pmc=35353", 100000L, "/Users/kcm/", StateOfCrawl.FAILED_TWICE);
//			scraper.scrape("http://biotea.github.io/bioschemas", 100000L, "/Users/kcm/", StateOfCrawl.FAILED_TWICE);
//			scraper.scrape("http://www.macs.hw.ac.uk/~kcm/g2p.html", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
