package hwu.elixir.scrape.scraper;

import java.io.File;
import java.io.PrintWriter;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.db.crawl.StateOfCrawl;
import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingContextException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.SeleniumException;

/**
 * Scrapes a given URL, converts into NQuads and writes to a file (name derived
 * from URL). If the file already exists it will be overwritten.
 * 
 * 
 * @author kcm
 * @see Scraper
 * 
 */
public class ServiceScraper extends ScraperCore {

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
	 * @throws HtmlExtractorServiceException
	 * @throws JsonLDInspectionException
	 * @throws CannotWriteException 
	 */
	public boolean scrape(String url, Long contextCounter, String folderToWriteNQTo, StateOfCrawl status)
			throws HtmlExtractorServiceException, FourZeroFourException, JsonLDInspectionException, CannotWriteException {

		if (url.endsWith("/") || url.endsWith("#"))
			url = url.substring(0, url.length() - 1);

		String html = "";
		if (status.equals(StateOfCrawl.UNTRIED) || status.equals(StateOfCrawl.FAILED)) {
//			html = GetHTMLFromNode.getHtml(url);
			try { 
				html = getHtmlViaSelenium(url);
			} catch (SeleniumException e) {
				// try again
				try {
					html = getHtmlViaSelenium(url);
				} catch(SeleniumException e2) {
					return false;
				}				
			}
		} else {
			return false;
		}
		
		try {
			html = injectId(html, url);
		} catch (MissingContextException | MissingHTMLException e) {
			logger.error(e.toString());
			return false;
		}

		DocumentSource source = new StringDocumentSource(html, url);
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());

		String n3 = getTriplesInN3(source);
		if (n3 == null)
			return false;

		Model updatedModel = processTriples(n3, sourceIRI, contextCounter);
		if (updatedModel == null)
			return false;

		File directory = new File(folderToWriteNQTo);
		if(!directory.exists())
			directory.mkdir();

		String fileName = folderToWriteNQTo + "/" + contextCounter + ".nq";

		try (PrintWriter out = new PrintWriter(new File(fileName))) {
			Rio.write(updatedModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			logger.error("Problem writing file for " + url);	
			throw new CannotWriteException(url);			
		}

		if (!new File(fileName).exists())
			System.exit(0);

		return true;
	}


	public static void main(String[] args) {
		try {
			ServiceScraper scraper = new ServiceScraper();
//			scraper.scrape("https://www.bbc.co.uk", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
//			scraper.scrape("https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
//			scraper.scrape("https://www.ebi.ac.uk/biosamples/samples/SAMEA4999347", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
//			scraper.scrape("https://www.ebi.ac.uk/biosamples/samples/SAMN00025378", 100000L, "/Users/kcm/",StateOfCrawl.UNTRIED); // 404s
//			scraper.scrape("https://www.alliancegenome.org/gene/MGI:2442292", 100000L, "/Users/kcm/",StateOfCrawl.FAILED_TWICE); // AllianceGenome
//			scraper.scrape("https://hamap.expasy.org/", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);
			scraper.scrape("https://hamap.expasy.org/rule/MF_00191", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);
			
//			scraper.scrape("http://biotea.github.io/bioschemas/?pmc=35353", 100000L, "/Users/kcm/", StateOfCrawl.FAILED_TWICE);
//			scraper.scrape("http://biotea.github.io/bioschemas", 100000L, "/Users/kcm/", StateOfCrawl.FAILED_TWICE);
//			scraper.scrape("http://www.macs.hw.ac.uk/~kcm/g2p.html", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
