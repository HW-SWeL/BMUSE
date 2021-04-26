package hwu.elixir.scrape.scraper;

import java.io.ByteArrayOutputStream;

import hwu.elixir.scrape.exceptions.*;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Scrapes a given site and returns n-triples wrapped in a {@link JSONArray}
 * 
 *
 */
public class WebScraper extends ScraperFilteredCore {

	private static final Logger logger = LoggerFactory.getLogger(WebScraper.class.getName());

	public JSONArray scrape(String url, ScraperOutput scraperOutputType) throws FourZeroFourException,
			JsonLDInspectionException, MissingHTMLException, SeleniumException, Exception {

		// parse the html with selenium, dynamic parsing
		String html = wrapHTMLExtraction(url);

		if (html == null || html.contentEquals("")) {
			logger.info("html is null or content equals to cs");
		}

		try {
			html = injectId(html, url);
		} catch (MissingHTMLException e) {
			logger.error(e.toString());
		}

		// write html markup blocks in ntriples form
		DocumentSource source = new StringDocumentSource(html, url);
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());
		String n3 = getTriplesInNTriples(source);
		
		if (n3 == null) {
			throw new MissingMarkupException(url);
		}

		Model updatedModel = null;

		try {
			updatedModel = processTriples(n3, sourceIRI, 0L);
		} catch (NTriplesParsingException e1) {
			logger.error("Failed to process triples into model; the NTriples generated from the URL (" + url
					+ ") could not be parsed into a model.");
		}

		if (updatedModel == null) {
			logger.error("Model is null");
			throw new Exception("Cannot build model for " + url);
		}
		
		JSONArray outputArray = new JSONArray();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		// Check the required output and write the file accordingly, supported JSONLD and TURTLE
		if(scraperOutputType.equals(ScraperOutput.JSONLD)) {
			Rio.write(updatedModel, stream, RDFFormat.JSONLD);
			String quads = new String(stream.toByteArray());
			JSONParser parser = new JSONParser();
			outputArray = (JSONArray) parser.parse(quads);
		} else if(scraperOutputType.equals(ScraperOutput.TURTLE)) {
			Rio.write(updatedModel, stream, RDFFormat.TURTLE);
			String quads = new String(stream.toByteArray());
			outputArray.add(quads);
		} else {
			logger.error("unsupported output format");
		}
		
		return outputArray;
	}
}
