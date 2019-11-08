package hwu.elixir.scrape.scraper;

import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.json.simple.JSONArray;
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
		Rio.write(model, stream, RDFFormat.NTRIPLES);
		String quads = new String(stream.toByteArray());

		return writeN3ToJSONArray(quads);
	}

	/** Wraps NTriples in {@link JSONArray}
	 * 
	 * 
	 * @param n3 NTriples as a String
	 * @return Triples as a {@link JSONArray}
	 */
	private JSONArray writeN3ToJSONArray(String n3) {
		JSONArray array = new JSONArray();
		StringTokenizer st = new StringTokenizer(n3, "\n");
		while (st.hasMoreTokens()) {
			array.add(st.nextToken());
		}
		return array;
	}

}
