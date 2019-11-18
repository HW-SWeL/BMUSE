package hwu.elixir.scrape.scraper;

import java.io.ByteArrayOutputStream;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.SeleniumException;

/** 
 * Scrapes a given site and returns the raw (unfiltered/unprocessed) n-triples wrapped in a {@link JSONArray}
 * 
 *
 */
public class WebScraper extends ScraperUnFilteredCore {

	private static final Logger logger = LoggerFactory.getLogger(System.class.getName());

	public JSONArray scrape(String url) throws FourZeroFourException,
			JsonLDInspectionException, MissingHTMLException, SeleniumException, Exception {				
			
		DocumentSource source = new StringDocumentSource(getHtmlViaSelenium(fixURL(url)), url);
		String triples = getTriplesInNTriples(source);
		
		if (triples == null) {
			logger.error("n3 is null");
			throw new Exception("Cannot obtain triples from "+url);
		}

		Model model = processTriplesLeaveBlankNodes(triples);
		if (model == null) {
			logger.error("Model is null");
			throw new Exception("Cannot build model for " + url);
		}
			
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Rio.write(model, stream, RDFFormat.JSONLD);
		String quads = new String(stream.toByteArray());
		
		JSONParser parser = new JSONParser();
		JSONArray outputArray= (JSONArray) parser.parse(quads);
		
		return outputArray;
	}

}
