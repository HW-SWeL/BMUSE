package hwu.elixir.scrape.scraper;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.exceptions.NTriplesParsingException;
import hwu.elixir.utils.Helpers;

import javax.annotation.Nullable;

/**
 * Extends {@link ScraperCore} by adding methods to extract (bio)schema markup
 * which is then filtered.
 * 
 * Takes a the extracted triples and filters them removing triples with
 * the following predicates:
 * <ol>
 * <li>nofollow</li>
 * <li>{@link https://ogp.me/}</li>
 * <li>xhtml/vocab</li>
 * <li>vocab.sindice</li>
 * </ol>
 * 
 * Also, replaces blank nodes with a URI based on the context counter and the
 * current time.
 */
public class ScraperFilteredCore extends ScraperCore {

	private static Logger logger = LoggerFactory.getLogger(ScraperFilteredCore.class.getName());
	private int countOfJSONLD = 0; // number of JSON-LD blocks found in HTML
	
	/**
	 * Orchestrates the scraping of a given URL and writes the output (as quads) to
	 * a file specified in the arguments. If the outputFileName is not specified, ie null,
	 * the contextCounter will be used to name the file.
	 * 
	 * contextCounter is used a way of keeping track of which URL in a list is being
	 * scraped. This is managed by the calling class.
	 * 
	 * The file will be located in the location specified in application.properties
	 * 
	 * @param url              URL to scrape
	 * @param outputFileName   name of file the output will be written to (may be null)
	 * @param contextCounter   The value of the counter used to record which number
	 *                         of URL is being scraped
	 * @param outputFolderName Folder where output is written to
	 * @return FALSE if failed else TRUE
	 * @throws FourZeroFourException
	 * @throws JsonLDInspectionException
	 * @throws CannotWriteException      Cannot write the markup to the specified
	 *                                   file
	 * @throws MissingMarkupException    Can retrieve HTML from URL, but cannot
	 *                                   obtain triples from that HTML
	 */

	// FIXME possible redundancy of the 2 scrape methods with the different signature using the @Nullable notation instead to make sure that the dynamic boolean value can be null
	/*public boolean scrape(String url, String outputFolderName, String outputFileName, Long contextCounter)
			throws FourZeroFourException, JsonLDInspectionException, CannotWriteException, MissingMarkupException {
		url = fixURL(url);

		String html = "";
		// This dynamic boolean determines if the scraper should start using selenium or use JSOUP to scrape the information (dynamic and static respectively)
		// This method signature does not take the dynamic parameter and always defaults to dynamic approach and it is left for now as it is used by services and web services that are dynamic/static agnostic
		Boolean dynamic = true;


		if (dynamic) {
			logger.info("dynamic scraping setting");
			html = wrapHTMLExtraction(url);
		} else {
			logger.info("static scraping setting");
			html = wrapHTMLExtractionStatic(url);
		}


		if (html == null || html.contentEquals(""))
			return false;

		try {
			html = injectId(html, url);
		} catch (MissingHTMLException e) {
			logger.error(e.toString());
			return false;
		}

		DocumentSource source = new StringDocumentSource(html, url);
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());

		String n3 = getTriplesInNTriples(source);
		if (n3 == null)
			throw new MissingMarkupException(url);

		Model updatedModel = null;
		try {
			updatedModel = processTriples(n3, sourceIRI, contextCounter);
		} catch (NTriplesParsingException e1) {
			logger.error("Failed to process triples into model; the NTriples generated from the URL (" + url
					+ ") could not be parsed into a model.");
			return false;
		}
		if (updatedModel == null)
			return false;

		File directory = new File(outputFolderName);
		if (!directory.exists())
			directory.mkdir();

		if (outputFileName == null) {
			outputFileName = outputFolderName + "/" + contextCounter + ".nq";
		} else {
			outputFileName = outputFolderName + "/" + outputFileName + ".nq";
		}

		try (PrintWriter out = new PrintWriter(new File(outputFileName))) {
			Rio.write(updatedModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			logger.error("Problem writing file for " + url, e);
			throw new CannotWriteException(url);
		}

		if (!new File(outputFileName).exists())
			System.exit(0);

		return true;
	}
	*/

	/**
	 * Orchestrates the scraping of a given URL and writes the output (as quads) to
	 * a file specified in the arguments. If the fileName is not specified, ie null,
	 * the contextCounter will be used to name the file.
	 *
	 * contextCounter is used a way of keeping track of which URL in a list is being
	 * scraped. This is managed by the calling class.
	 *
	 * The file will be located in the location specified in application.properties
	 *
	 * @param url              URL to scrape
	 * @param outputFileName   name of file the output will be written to
	 * @param contextCounter   The value of the counter used to record which number
	 *                         of URL is being scraped
	 * @param outputFolderName Folder where output is written to
	 * @param dynamic boolean that determines if JSOUP of Selenium is used to parse the HTML document
	 * @return FALSE if failed else TRUE
	 * @throws FourZeroFourException
	 * @throws JsonLDInspectionException
	 * @throws CannotWriteException      Cannot write the markup to the specified
	 *                                   file
	 * @throws MissingMarkupException    Can retrieve HTML from URL, but cannot
	 *                                   obtain triples from that HTML
	 */
	public boolean scrape(String url, String outputFolderName, String outputFileName, Long contextCounter, @Nullable Boolean dynamic)
			throws FourZeroFourException, JsonLDInspectionException, CannotWriteException, MissingMarkupException {
		url = fixURL(url);

		String html = "";
		// The dynamic boolean determines if the scraper should start using selenium or JSOUP to scrape the information (dynamic and static respectively)

		if (dynamic) {
			logger.info("dynamic scraping setting");
			html = wrapHTMLExtraction(url);
		} else {
			logger.info("static scraping setting");
			html = wrapHTMLExtractionStatic(url);
		}


		if (html == null || html.contentEquals(""))
			return false;
		if (logger.isTraceEnabled() ) {
			logger.trace("Read following html ==============================================================");
			logger.trace(html);
		}
		
		try {
			html = injectId(html, url);
			if (logger.isTraceEnabled() ) {
				logger.trace("Same HTML after injecting ID ==============================================================");
				logger.trace(html);
			}
		} catch (MissingHTMLException e) {
			logger.error(e.toString());
			return false;
		}

		DocumentSource source = new StringDocumentSource(html, url);
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());

		String n3 = getTriplesInNTriples(source);
		if (n3 == null)
			throw new MissingMarkupException(url);

		Model updatedModel = null;
		try {
			updatedModel = processTriples(n3, sourceIRI, contextCounter);
		} catch (NTriplesParsingException e1) {
			logger.error("Failed to process triples into model; the NTriples generated from the URL (" + url
					+ ") could not be parsed into a model.");
			return false;
		}
		if (updatedModel == null)
			return false;

		File directory = new File(outputFolderName);
		if (!directory.exists())
			directory.mkdir();

		if (outputFileName == null) {
			outputFileName = outputFolderName + "/" + contextCounter + ".nq";
		} else {
			outputFileName = outputFolderName + "/" + outputFileName + ".nq";
		}

		try (PrintWriter out = new PrintWriter(new File(outputFileName))) {
			Rio.write(updatedModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			logger.error("Problem writing file for " + url, e);
			throw new CannotWriteException(url);
		}

		if (!new File(outputFileName).exists())
			System.exit(0);

		return true;
	}

	/**
	 * Processes a string containing nTriples to obtain a RDF4J {@link Model}.
	 * 
	 * Takes a series of triples as a string and filters them removing triples with
	 * the following predicates:
	 * <ol>
	 * <li>nofollow</li>
	 * <li>{@link https://ogp.me/}</li>
	 * <li>xhtml/vocab</li>
	 * <li>vocab.sindice</li>
	 * </ol>
	 * 
	 * Also, replaces blank nodes with a URI based on the context counter and the
	 * current time.
	 * 
	 * The triples are placed in a context based on the context counter.
	 * 
	 * Ultimately produces an RDF4J Model based on the filtered triples
	 * 
	 * @param nTriples       The triples to be processed
	 * @param sourceIRI      The URL of the page from which the triples were
	 *                       obtained
	 * @param contextCounter The current counter for the context. Assumes the
	 *                       triples will be placed into crawl repo. If not, any
	 *                       number can be used here.
	 * @return An RDF4J model containing the processed triples
	 * @throws NTriplesParsingException Thrown when the nTriples string cannot be
	 *                                  parsed
	 * @see Model
	 */
	protected Model processTriples(String nTriples, IRI sourceIRI, Long contextCounter)
			throws NTriplesParsingException {
		Model model = createModelFromNTriples(nTriples);

		Iterator<Statement> it = model.iterator();

		String nSpace = "https://bioschemas.org/crawl/v1/";

		// Relating to issue #1 COVID-19 repo
		SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMdd");
		Date date = new Date();

		// This block of code does some simple string manipulation to extract domain name and local name
		// Please note that these 2 methods i.e. getNamespace and getLocalName have been deprecated, so
		// at some point they will be removed and must take into account if a later version of rdf4j is used
		String domainLocalName = sourceIRI.getLocalName();
		String IRItoString = sourceIRI.toString();
		// adjust position by 2 to not include "//"
		int tempNSSSoD = IRItoString.indexOf("//") + 2;
		String domainName =  IRItoString.substring(tempNSSSoD, IRItoString.indexOf(".", tempNSSSoD));

		if (domainName.equalsIgnoreCase("www")){
			// adjust position by 4 to not include "www" and "."
			tempNSSSoD = IRItoString.indexOf("www") + 4;
			domainName =  IRItoString.substring(tempNSSSoD, IRItoString.indexOf(".", tempNSSSoD));
		}

		// remove . from local domain name
		if (domainLocalName.indexOf('.') != -1) {
			int lnEnd = domainLocalName.indexOf('.');
			domainLocalName = domainLocalName.substring(0, lnEnd);
		}
		nSpace = nSpace.concat(domainName + "/" + domainLocalName + "/");
		nSpace = nSpace.concat(dateF.format(date) + "/");

		String nGraph = nSpace + contextCounter++;

		ModelBuilder builder = new ModelBuilder();
		builder.setNamespace(" ", nSpace);
		builder.setNamespace("bsc", nSpace);
		// add triples that relate to scraped data on the default graph
		IRI bmuseIRI = SimpleValueFactory.getInstance().createIRI("https://github.com/HW-SWeL/BMUSE/releases/tag/" + properties.getScraperVersion());
		builder.defaultGraph().add(nGraph, "http://purl.org/pav/retrievedFrom", sourceIRI);
		builder.defaultGraph().add(nGraph, "http://purl.org/pav/retrievedOn", Helpers.getFullDateWithTime());
		builder.defaultGraph().add(nGraph, "http://purl.org/pav/createdWith", bmuseIRI);

		HashMap<String, String> replaceBlankNodes = new HashMap<String, String>();

		while (it.hasNext()) {
			Statement temp = it.next();
			Resource subject = temp.getSubject();
			IRI predicate = temp.getPredicate();
			Value object = temp.getObject();

			if (predicate.stringValue().contains("vocab.sindice"))
				continue;
			if (predicate.stringValue().contains("xhtml/vocab"))
				continue;
			if (predicate.stringValue().contains("nofollow"))
				continue;
			if (predicate.stringValue().contains("ogp.me"))
				continue;

			predicate = fixPredicate(predicate);

			object = fixObject(object);

			if (subject instanceof BNode) {
				if (replaceBlankNodes.containsKey(subject.stringValue())) {
					subject = SimpleValueFactory.getInstance().createIRI(replaceBlankNodes.get(subject.stringValue()));
				} else {
					IRI newSubject = iriGenerator(nGraph, sourceIRI);
					replaceBlankNodes.put(subject.stringValue(), newSubject.stringValue());
					subject = newSubject;
				}
			}

			if (object instanceof BNode) {
				if (replaceBlankNodes.containsKey(object.stringValue())) {
					object = SimpleValueFactory.getInstance().createIRI(replaceBlankNodes.get(object.stringValue()));
				} else {
					IRI newObject = iriGenerator(nGraph, sourceIRI);
					replaceBlankNodes.put(object.stringValue(), newObject.stringValue());
					object = newObject;
				}
			}
			builder.namedGraph(nGraph).add(subject, predicate, object);
		}

		return builder.build();
	}

	/**
	 * Generates a new IRI based on the named graph and the source's IRI. Includes a
	 * random element based on time to ensure no collisions.
	 * 
	 * @param ngraph
	 * @param sourceIRI
	 * @return New IRI
	 */
	protected IRI iriGenerator(String ngraph, IRI sourceIRI) {
		String source = "";
		if (sourceIRI.toString().indexOf("https://") != -1) {
			source = sourceIRI.toString().replaceAll("https://", "");
		} else {
			source = sourceIRI.toString().replaceAll("http://", "");
		}

		if (!(source.endsWith("/") || source.endsWith("#"))) {
			source += "/";
		}

		Random rand = new Random();
		int randomInt = Math.abs(rand.nextInt());
		return SimpleValueFactory.getInstance().createIRI(ngraph + "/" + source + randomInt);
	}

	/**
	 * Injects an @ id attribute into the given html source; prevents Any23 creating
	 * blank nodes at the top of the graph
	 * 
	 * Problems:
	 * <ol>
	 * <li>only works with json-ld; won't do anything with rdfa.</li>
	 * <li>ignoring nesting; only outer layer has id injected thus lower levels will
	 * use id based on outer.</li>
	 * </ol>
	 * 
	 * Adding the @ id in the wrong location can completely break the parsers
	 * ability to generate triples. The location of the injection should be checked
	 * if fewer triples are generated than expected.
	 * 
	 * @param html the source to be changed
	 * @param url  the url to be used for @ id
	 * @return the original source with @ id added in if missing from a block of
	 *         JSON-LD. if not, unchanged source
	 * @throws JsonLDInspectionException
	 */
	protected String injectId(String html, String url) throws MissingHTMLException, JsonLDInspectionException {
		countOfJSONLD = 0;

		if (url == null)
			throw new IllegalArgumentException("url cannot be null");

		if (html == null)
			throw new MissingHTMLException(url);

		int posContext = html.indexOf("@context");
		if (posContext == -1) {
			if (html.indexOf("vocab=\"http://schema.org") != -1 || html.indexOf("vocab=\"https://schema.org") != -1) {
				logger.info("No @context, but a vocab; appears to be RDFa with no JSON-LD: " + url);
				return html;
			}
		}
		return fixAllJsonLdBlocks(html, url);
	}

	/**
	 * Given HTML source, gets all the JSON-LD blocks and orchestrates the amendment
	 * of them using {@link #fixASingleJsonLdBlock(String, String)}
	 * 
	 * @param html The HTML source
	 * @param url  The URL from which the source was obtained
	 * @return HTML in which JSON-LD has been corrected
	 * @throws JsonLDInspectionException when JSON cannot be parsed
	 * @see {@link #fixASingleJsonLdBlock(String, String)}
	 */
	protected String fixAllJsonLdBlocks(String html, String url) throws JsonLDInspectionException {
		String[] allMarkup = null;
		if (html.startsWith("{")) {
			logger.info("Just JSON no HTML from: " + url);
			allMarkup = new String[1];
			allMarkup[0] = html;

		} else {
			allMarkup = getOnlyUnfilteredJSONLDFromHtml(html);
		}

		logger.debug("Number of JSONLD sections: " + allMarkup.length);

		for (String markup : allMarkup) {
			String newMarkup = fixASingleJsonLdBlock(markup, url);

			if (newMarkup.equalsIgnoreCase(markup)) {
				continue;
			}

			html = swapJsonLdMarkup(html, markup, newMarkup);
			countOfJSONLD++;
		}

		return html;
	}

	/**
	 * 
	 * Corrects/amends a single JSON-LD block markup extracted from the HTML source.
	 * A block of JSON can either contain a single JSON description or an array of
	 * several JSON descriptions. For each description this will:
	 * <ol>
	 * <li>Changes context to https://schema.org</li>
	 * <li>adds @id based on url</li>
	 * </ol>
	 * 
	 * 
	 * @param markup A single block of JSON-LD (bio)schema markup as a String
	 * @param url    The URL of the site the markup was scraped from
	 * @return mended JSON-LD markup as a String
	 * @throws JsonLDInspectionException when JSON cannot be parsed
	 */
	protected String fixASingleJsonLdBlock(String markup, String url) throws JsonLDInspectionException {
		JSONParser parser = new JSONParser();
		JSONArray jsonArray = null;
		JSONObject jsonObj = null;
		try {
			Object obj = parser.parse(markup);

			if (obj instanceof JSONArray) {
				jsonArray = (JSONArray) obj;
				return fixAJsonLdArray(jsonArray, url);
			} else if (obj instanceof JSONObject) {
				jsonObj = (JSONObject) obj;
				return fixASingleJsonLdObject(jsonObj, url);
			}

			throw new JsonLDInspectionException("Unkown object obtained from JSON parser :" + url);

		} catch (ParseException e) {
			throw new JsonLDInspectionException("JSON ParseException. Failed to parse JSON from :" + url + " Parser Error: " + e);
		}
	}

	/**
	 * Corrects an {@link JSONArray} of (bio)schemas markup; each element is a
	 * {@link JSONObject}. Uses {@link #fixASingleJSONLdObject(JSONObject, String)}
	 * 
	 * @param array An {@link JSONArray} of (bio)schemas markup
	 * @param url   The URL from which the markup was scraped
	 * @return The corrected markup stringified; will still be in array
	 */
	@SuppressWarnings("unchecked")
	protected String fixAJsonLdArray(JSONArray array, String url) {
		for (int i = 0; i < array.size(); i++) {
			JSONObject correctedObj = fixASingleJSONLdObject((JSONObject) array.get(i), url);

			array.remove(i);
			array.add(i, correctedObj);
		}

		return array.toJSONString().replaceAll("\\\\", "");
	}

	/**
	 * Wrapper that converts {@link #fixASingleJSONLdObject(JSONObject, String)}
	 * such that it returns a String rather than a {@link JSONObject}
	 * 
	 * @param jsonObj A {@link JSONObject} containing the (bio)schema markup to be
	 *                corrected
	 * @param url     The URL from where the jsonObj was obtained
	 * @return A stringified version of the corrected {@link JSONObject}
	 */
	protected String fixASingleJsonLdObject(JSONObject jsonObj, String url) {
		//FIXME fix issue that removes escape character \" in json strings used replace rather than replaceAll but still to do more tests for the solution
		return fixASingleJSONLdObject(jsonObj, url).toJSONString().replace("\\\\", "");
	}

	/**
	 * Corrects/amends a single JSON-LD object markup extracted from the HTML source
	 * <ol>
	 * <li>Changes context to https://schema.org</li>
	 * <li>adds @id based on url</li>
	 * </ol>
	 * 
	 * @param jsonObj A single block of JSON-LD (bio)schema markup as a
	 *                {@link JSONObject}
	 * @param url     The URL of the site the markup was scraped from
	 * @return Amended JSON-LD markup as a {@link JSONObject}
	 */
	@SuppressWarnings("unchecked")
	protected JSONObject fixASingleJSONLdObject(JSONObject jsonObj, String url) {
		if (jsonObj.containsKey("@context")) {
			String contextValue = jsonObj.get("@context").toString();
			
			if (!(contextValue.equalsIgnoreCase("https://schema.org"))) {
				jsonObj.remove("@context");
				jsonObj.put("@context", properties.getSchemaContext());
			}
			//FIXME This was added to replace https://schema.org temporary fix only
			if ((contextValue.equalsIgnoreCase("https://schema.org"))) {
				jsonObj.remove("@context");
				jsonObj.put("@context", properties.getSchemaContext());
			}

			contextValue = jsonObj.get("@context").toString();

		} else {
			jsonObj.put("@context", properties.getSchemaContext());
		}

		if (!jsonObj.containsKey("@id")) {
			if (countOfJSONLD > 0) {
				jsonObj.put("@id", url + "-" + countOfJSONLD);
			} else {
				jsonObj.put("@id", url);
			}
		}
		return jsonObj;
	}

	/**
	 * Replaces the old JSON-LD markup with the new markup
	 * 
	 * @param html      Current HTML
	 * @param oldMarkup The markup to be replaced
	 * @param newMarkup The new markup to be added
	 * @return HTML with the newMarkup replacing the oldMarkup
	 */
	protected String swapJsonLdMarkup(String html, String oldMarkup, String newMarkup) {

		int oldPosition = html.indexOf(oldMarkup);
		String newHtml = html.substring(0, oldPosition) + newMarkup + html.substring(oldPosition + oldMarkup.length());

		return newHtml;
	}
}
