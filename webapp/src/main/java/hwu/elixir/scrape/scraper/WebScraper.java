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

	public JSONArray scrape(String url, ScraperOutput scraperOutputType) throws FourZeroFourException,
			JsonLDInspectionException, MissingHTMLException, SeleniumException, Exception {				
			
		//remove http/s://schema.org from source and replace with the direct link
		String tempSource = getHtmlViaSelenium(fixURL(url));
		tempSource = removeSchemaORG(tempSource);
		DocumentSource source = new StringDocumentSource(tempSource, url);
		String triples = getTriplesInNTriples(source);
		//logger.info("triples from source: " + triples.toString());
		
		if (triples == null) {
			logger.error("n3 is null");
			throw new Exception("Cannot obtain triples from "+url);
		}

		Model model = processTriplesLeaveBlankNodes(triples);
		if (model == null) {
			logger.error("Model is null");
			throw new Exception("Cannot build model for " + url);
		}
		
		JSONArray outputArray = new JSONArray();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		if(scraperOutputType.equals(ScraperOutput.JSONLD)) {
			
			Rio.write(model, stream, RDFFormat.JSONLD);
			String quads = new String(stream.toByteArray());

			//logger.info("JSONLD quads from Rio: " + quads.toString());
		
			JSONParser parser = new JSONParser();
			outputArray = (JSONArray) parser.parse(quads);
		} else if(scraperOutputType.equals(ScraperOutput.TURTLE)) {
			Rio.write(model, stream, RDFFormat.TURTLE);
			String quads = new String(stream.toByteArray());
			//logger.info("TURTLE quads from Rio: " + quads.toString());
			outputArray.add(quads);
		}
		
		return outputArray;
	}

	/**
	 * method that removes all http/s://schema.org instances and replaces them with the direct link
	 * @param source
	 * @return
	 */
	private String removeSchemaORG (String source) {
		String alteredSource = "";
		String tempSchemaContext = properties.getSchemaContext();
		alteredSource = source.replaceAll("htt(p|ps):\\/\\/schema\\.org", tempSchemaContext);
		return alteredSource;
	}

}
