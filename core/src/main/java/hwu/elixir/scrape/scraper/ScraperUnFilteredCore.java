package hwu.elixir.scrape.scraper;

import java.io.IOException;
import java.util.Iterator;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.exceptions.NTriplesParsingException;
import hwu.elixir.scrape.exceptions.SeleniumException;


/**
 * Extends {@link ScraperCore} by adding methods to extract raw (bio)schema markup without filtering or
 * amending that markup in anyway.
 *
 */
@Deprecated(since = "0.4", forRemoval = true)
public class ScraperUnFilteredCore extends ScraperCore {
	
	private static Logger logger = LoggerFactory.getLogger(ScraperUnFilteredCore.class.getName());

	/**
	 * Extract schema markup in JSON-LD form from a given URL. Will ignore all other
	 * formats of markup. Some blocks may not be (bio)schema markup. Will not
	 * process/validate JSON-LD, add @id or change @context etc.
	 * 
	 * 
	 * @param url URL to scrape
	 * @return An array in which each element is a block of JSON-LD containing
	 *         schema.org markup.
	 * @throws FourZeroFourException
	 * @throws SeleniumException
	 */
	public String[] getOnlyUnfilteredJSONLDFromUrl(String url) throws FourZeroFourException {
		return getOnlyUnfilteredJSONLDFromHtml(wrapHTMLExtraction(url));
	}

	/**
	 * Returns the raw/unprocessed structured data from a given URL in JSON-LD. Will
	 * include rdfa and json-ld (if they exist). Will change the triples so that
	 * Any23 will parse them without throwing an error and then change them back.
	 * 
	 * Will not:
	 * <ol>
	 * <li>remove triples from:
	 * <ol>
	 * <li>{@link https://ogp.me/}</li>
	 * <li>nofollow</li>
	 * <li>xhtml/vocab</li>
	 * <li>vocab.sindice</li>
	 * <li>or anywhere else</li>
	 * <ol>
	 * <li>replace blank nodes
	 * <li>
	 * </ol>
	 * 
	 * 
	 * @param url The URL to be scraped
	 * @return The unfiltered structured data as a JSONLD string
	 * @throws FourZeroFourException
	 * @throws MissingHTMLException     Cannot retrieve HTML from URL
	 * @throws MissingMarkupException   Can retrieve HTML from URL, but cannot
	 *                                  obtain triples from that HTML
	 * @throws NTriplesParsingException Cannot parse the NTriples generated from the
	 *                                  given URL
	 */
	public String scrapeUnfilteredMarkupAsJsonLDFromUrl(String url)
			throws FourZeroFourException, MissingHTMLException, MissingMarkupException, NTriplesParsingException {
		Model model = extractUnfilteredMarkupAsRdf4jModel(url);

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Rio.write(model, out, RDFFormat.JSONLD);
			return out.toString("UTF-8");
		} catch (IOException e) {
			logger.error("IO error whilst writing JSONLD", e);
		}
		return "";
	}

	/**
	 * Returns the raw/unprocessed structured data from a given URL in NTriples.
	 * Will include rdfa and json-ld (if they exist). Will change the triples so
	 * that Any23 will parse them without throwing an error and then change them
	 * back.
	 * 
	 * Will not:
	 * <ol>
	 * <li>remove triples from:
	 * <ol>
	 * <li>{@link https://ogp.me/}</li>
	 * <li>nofollow</li>
	 * <li>xhtml/vocab</li>
	 * <li>vocab.sindice</li>
	 * <li>or anywhere else</li>
	 * <ol>
	 * <li>replace blank nodes
	 * <li>
	 * </ol>
	 * 
	 * @param url The URL to be scraped
	 * @return The unfiltered structured data as a NTriples string
	 * @throws FourZeroFourException
	 * @throws MissingHTMLException     Cannot retrieve HTML from URL
	 * @throws MissingMarkupException   Can retrieve HTML from URL, but cannot
	 *                                  obtain triples from that HTML
	 * @throws NTriplesParsingException Cannot parse the NTriples generated from the
	 *                                  given URL
	 */
	public String scrapeUnfilteredMarkupAsNTriplesFromUrl(String url)
			throws FourZeroFourException, MissingHTMLException, MissingMarkupException, NTriplesParsingException {
		Model model = extractUnfilteredMarkupAsRdf4jModel(url);

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Rio.write(model, out, RDFFormat.NTRIPLES);
			return out.toString("UTF-8");
		} catch (IOException e) {
			logger.error("IO error whilst writing NTriples", e);
		}
		return "";
	}

	/**
	 * Returns the raw/unprocessed structured data from a given URL as an RDF4J
	 * {@link Model}. Will include rdfa and json-ld (if they exist). Will change the
	 * triples so that Any23 will parse them without throwing an error and then
	 * change them back.
	 * 
	 * Will not:
	 * <ol>
	 * <li>remove triples from:
	 * <ol>
	 * <li>{@link https://ogp.me/}</li>
	 * <li>nofollow</li>
	 * <li>xhtml/vocab</li>
	 * <li>vocab.sindice</li>
	 * <li>or anywhere else</li>
	 * <ol>
	 * <li>replace blank nodes
	 * <li>
	 * </ol>
	 * 
	 * @param url The URL to be scraped
	 * @return The unfiltered structured data as a RDF4J Model
	 * @throws FourZeroFourException
	 * @throws MissingHTMLException     Cannot retrieve HTML from URL
	 * @throws MissingMarkupException   Can retrieve HTML from URL, but cannot
	 *                                  obtain triples from that HTML
	 * @throws NTriplesParsingException Cannot parse the NTriples generated from the
	 *                                  given URL
	 * @see {@link Model}
	 */
	private Model extractUnfilteredMarkupAsRdf4jModel(String url)
			throws FourZeroFourException, MissingHTMLException, MissingMarkupException, NTriplesParsingException {

		url = fixURL(url);
		String html = wrapHTMLExtraction(url);

		if (html.contentEquals(""))
			throw new MissingHTMLException(url);

		// not injecting id as leaving blank nodes

		DocumentSource source = new StringDocumentSource(html, url);
		String n3 = getTriplesInNTriples(source);
		if (n3 == null)
			throw new MissingMarkupException(url);

		Model model = processTriplesLeaveBlankNodes(n3);

		return model;
	}

	/**
	 * Processes a string containing nTriples to obtain a RDF4J {@link Model}
	 * 
	 * Does NOT replace the following:
	 * <ol>
	 * <li>nofollow</li>
	 * <li>{@link https://ogp.me/}</li>
	 * <li>xhtml/vocab</li>
	 * <li>vocab.sindice</li>
	 * </ol>
	 * 
	 * DOES NOT replace blank nodes.
	 * 
	 * Triples are NOT placed in a context.
	 * 
	 * DOES rectify the changes made to predicates and objects to ensure Any23 can
	 * parse them (see {@link #fixAny23WeirdIssues(String)}
	 * 
	 * @param nTriples The triples to be processed
	 * @return An RDF4J model containing the UNprocessed triples
	 * @throws NTriplesParsingException Thrown when the nTriples string cannot be
	 *                                  parsed
	 * @see Model
	 */
	protected Model processTriplesLeaveBlankNodes(String nTriples) throws NTriplesParsingException {

		Model model = createModelFromNTriples(nTriples);
		Iterator<Statement> it = model.iterator();
		ModelBuilder builder = new ModelBuilder();

		while (it.hasNext()) {
			Statement temp = it.next();
			Resource subject = temp.getSubject();
			IRI predicate = temp.getPredicate();
			Value object = temp.getObject();

			predicate = fixPredicate(predicate);

			object = fixObject(object);

			builder.add(subject, predicate, object);
		}
		return builder.build();
	}
}
