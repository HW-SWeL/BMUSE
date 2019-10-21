package hwu.elixir.scrape.scraper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.SeleniumException;
import hwu.elixir.scrape.scraper.examples.FileScraper;
import hwu.elixir.scrape.scraper.examples.SingleURLScraper;
import hwu.elixir.utils.ChromeDriverCreator;
import hwu.elixir.utils.Helpers;

/**
 * Provides core functionality for scraping, but is not an actual scraper. See
 * {@link FileScraper} or {@link SingleURLScraper} for examples of how to use
 * this.
 * 
 * @see FileScraper
 * @see hwu.elixir.scrape.ServiceScrapeDriver
 * 
 */
public abstract class ScraperCore {

	private static Logger logger = LoggerFactory.getLogger(System.class.getName());
	private WebDriver driver = ChromeDriverCreator.getInstance();

	private int countOfJSONLD = 0; // number of JSON-LB blocks found in HTML

	/**
	 * An attempt to close the chromedriver opened by Selenium. Should always be
	 * closed at the end of the scrape.
	 * 
	 * @see ChromeDriverCreator
	 * @see https://github.com/HW-SWeL/Scraper/issues/42
	 */
	public void shutdown() {
		if (driver != null) {
			logger.info("driver is not null... trying to close!");
			driver.quit();
			logger.info("driver closed?!");
		} else {
			logger.info("Driver is null... no need to close.");
		}
	}

	/**
	 * Uses JSoup to pull the HTML of a NON dynamic web page
	 * 
	 * @param url The address of the site to parse
	 * @return The HTML as a string
	 * @throws FourZeroFourException when url is 404
	 */
	protected String getHtml(String url) throws FourZeroFourException {
		try {
			Response response = Jsoup.connect(url).execute();
			return fixAny23WeirdIssues(response.parse().normalise().html());
		} catch (HttpStatusException status) {
			if (status.getStatusCode() == 404) {
				logger.error(url + " produced a 404");
				throw new FourZeroFourException(url);
			}
			logger.error(url + " produced a " + status.getStatusCode());
		} catch (IOException e) {
			logger.error(url + " produced a " + e.getMessage());
			System.out.println(url + " produced a " + e.getMessage());
		} catch (IllegalArgumentException e1) {
			logger.error(url + " produced a " + e1.getMessage());
			System.out.println(url + " produced a " + e1.getMessage());
		}
		return null;
	}

	/**
	 * Uses Selenium to pull the HTML of a dynamic web page (ie, executes the
	 * JavaScript).
	 * 
	 * @param url The address of the page to parse
	 * @return The HTML as a string
	 * @throws FourZeroFourException when page title is 404
	 * @throws SeleniumException
	 */
	protected String getHtmlViaSelenium(String url) throws FourZeroFourException, SeleniumException {
		try {
			if (driver == null) {
				driver = ChromeDriverCreator.getInstance();
			}

			driver.get(url);

			// possibly worthless as Selenium does not support HTTP codes:
			// https://github.com/seleniumhq/selenium-google-code-issue-archive/issues/141
			if (driver.getTitle().contains("404")) {
				logger.error(url + " produced a 404");
				throw new FourZeroFourException(url);
			}

			WebDriverWait wait = new WebDriverWait(driver, 10);
			wait.until(ExpectedConditions
					.presenceOfAllElementsLocatedBy(By.xpath("//script[@type=\"application/ld+json\"]")));

		} catch (TimeoutException to) {
			logger.error("URL timed out: " + url + ". Trying JSoup.");
			return getHtml(url);

		} catch (org.openqa.selenium.WebDriverException crashed) {
			crashed.printStackTrace();
			if (driver == null) {
				driver = ChromeDriverCreator.getInstance();
			}
			throw new SeleniumException(url);
		}

		return fixAny23WeirdIssues(driver.getPageSource());
	}

	/**
	 * Extract schema markup in JSON-LD form from a given URL. Will ignore all other
	 * formats of markup.
	 * 
	 * @param url URL to scrape
	 * @return An array in which each element is a block of JSON-LD containing
	 *         schema.org markup.
	 * @throws FourZeroFourException
	 * @throws SeleniumException
	 */
	protected String[] getOnlyJSONLD(String url) throws FourZeroFourException, SeleniumException {
		String html = getHtmlViaSelenium(url);
		Document doc = Jsoup.parse(html);
		Elements jsonElements = doc.getElementsByTag("script").attr("type", "application/ld+json");

		ArrayList<String> filteredJson = new ArrayList<String>();
		for (Element jsonElement : jsonElements) {
			if (jsonElement.data() != "" && jsonElement.data() != null) {
				if (jsonElement.data().contains("@type") || jsonElement.data().contains("@context")) {
					filteredJson.add(jsonElement.data().trim());
				}
			}
		}
		String[] toReturn = new String[filteredJson.size()];
		filteredJson.toArray(toReturn);
		return toReturn;
	}

	/**
	 * Extract schema markup in JSON-LD form from a given HTML. Will ignore all other
	 * formats of markup.
	 * 
	 * @param html to find JSON-LD in
	 * @return An array in which each element is a block of JSON-LD containing
	 *         schema.org markup.
	 * @throws FourZeroFourException
	 * @throws SeleniumException
	 */
	protected String[] getOnlyJSONLDMarkup(String html) {
		Document doc = Jsoup.parse(html);
		Elements jsonElements = doc.getElementsByTag("script").attr("type", "application/ld+json");

		ArrayList<String> filteredJson = new ArrayList<String>();
		for (Element jsonElement : jsonElements) {
			if (jsonElement.data() != "" && jsonElement.data() != null) {
				if (jsonElement.data().contains("\"@type") || jsonElement.data().contains("\"@context")) {
					int positionOfClosingTag = jsonElement.data().indexOf("</script");
					if (positionOfClosingTag == -1) {
						filteredJson.add(jsonElement.data());
					} else {
						filteredJson.add(jsonElement.data().substring(0, positionOfClosingTag));
					}
				}
			}
		}

		String[] toReturn = new String[filteredJson.size()];
		filteredJson.toArray(toReturn);
		return toReturn;
	}

	/**
	 * Takes an Any23 DocumentSource and converts into triples in NTriples form.
	 * 
	 * @param source The HTML as an Any23 DocumentSource
	 * @return Triples in NTriples form as a long String
	 * @see DocumentSource
	 */
	protected String getTriplesInNTriples(DocumentSource source) {

		Any23 runner = new Any23();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				TripleHandler handler = new NTriplesWriter(out);) {

			runner.extract(source, handler);

			return out.toString("UTF-8");
		} catch (ExtractionException e) {
			System.out.println("Cannot extract triples!");
			logger.error("Cannot extract triples", e);
		} catch (IOException e) {
			System.out.println("IO error whilst extracting triples!");
			logger.error(" IO error whilst extracting triples", e);
		} catch (TripleHandlerException e1) {
			System.out.println("TripleHanderException!");
			logger.error("TripleHanderException", e1);
		}

		return null;
	}

	/**
	 * Takes a series of triples as a string and filters them removing triples with
	 * the following predicates:
	 * <ol>
	 * <li>nofollow</li>
	 * <li>ogp.me/...</li>
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
	 * @see Model
	 */
	protected Model processTriples(String nTriples, IRI sourceIRI, Long contextCounter) {
		InputStream input = new ByteArrayInputStream(nTriples.getBytes(StandardCharsets.UTF_8));

		Model model;
		try {
			model = Rio.parse(input, "", RDFFormat.NTRIPLES);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			logger.error("Cannot parse triples into a model", e);
			return null;
		}
		Iterator<Statement> it = model.iterator();

		String nSpace = "https://bioschemas.org/crawl/v1/";
		String nGraph = nSpace + contextCounter++;

		ModelBuilder builder = new ModelBuilder();
		builder.setNamespace(" ", nSpace);
		builder.setNamespace("bsc", nSpace);
		builder.namedGraph(nGraph).add(nGraph, "http://purl.org/pav/retrievedFrom", sourceIRI);
		builder.namedGraph(nGraph).add(nGraph, "http://purl.org/pav/retrievedOn", Helpers.getFullDateWithTime());

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
	 * Takes a series of n-triples as a string and filters them removing triples
	 * with the following predicates:
	 * <ol>
	 * <li>nofollow</li>
	 * <li>ogp.me/...</li>
	 * <li>xhtml/vocab</li>
	 * <li>vocab.sindice</li>
	 * </ol>
	 * 
	 * DOES NOT replace blank nodes.
	 * 
	 * Triples are NOT placed in a context.
	 * 
	 * Ultimately produces an RDF4J Model based on the filtered triples
	 * 
	 * @param nTriples The triples to be processed
	 * @return An RDF4J model containing the processed triples
	 * @see Model
	 */
	protected Model processTriplesLeaveBlankNodes(String nTriples) {
		InputStream input = new ByteArrayInputStream(nTriples.getBytes(StandardCharsets.UTF_8));

		Model model;
		try {
			model = Rio.parse(input, "", RDFFormat.NTRIPLES);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			logger.error("Cannot parse triples into a model", e);
			return null;
		}
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

	/**
	 * Removes changes made to allow Any23 to parse the html & standardises on
	 * httpS://schema.org
	 * 
	 * @param predicate
	 * @return Corrected IRI
	 * @see #fixAny23WeirdIssues(String)
	 */
	protected IRI fixPredicate(IRI predicate) {
		String tempPred = predicate.stringValue().trim().replaceAll("licensE", "license")
				.replaceAll("FileFormat", "fileFormat").replaceAll("addType", "additionalType");
		if (tempPred.endsWith("/")) {
			tempPred = tempPred.substring(0, tempPred.length() - 1);
		}
		if (tempPred.startsWith("http://bioschemas")) {
			tempPred = tempPred.replaceFirst("http://bioschemas", "https://bioschemas");
		}
		if (tempPred.startsWith("http://schema")) {
			tempPred = tempPred.replaceFirst("http://schema", "https://schema");
		}
		return SimpleValueFactory.getInstance().createIRI(tempPred);
	}

	/**
	 * Removes changes made to allow Any23 to parse the html & corrects common
	 * spelling mistakes
	 * 
	 * @param object
	 * @return Corrected IRI
	 * @see #fixAny23WeirdIssues(String)
	 */
	protected Value fixObject(Value object) {
		if (object instanceof BNode)
			return object;
		if (object instanceof IRI) {
			String tempIRI = object.stringValue();

			if (tempIRI.startsWith("http://bioschemas")) {
				tempIRI = tempIRI.replaceFirst("http://bioschemas", "https://bioschemas");
			}
			if (tempIRI.startsWith("http://schema")) {
				tempIRI = tempIRI.replaceFirst("http://schema", "https://schema");
			}

			if (tempIRI.contains("schema.org/DataSet")) {
				tempIRI = tempIRI.replaceFirst("schema.org/DataSet", "schema.org/Dataset");
			}

			if (tempIRI.contains("schema.org/dataSet")) {
				tempIRI = tempIRI.replaceFirst("schema.org/dataSet", "schema.org/Dataset");
			}

			if (tempIRI.contains("schema.org/dataset")) {
				tempIRI = tempIRI.replaceFirst("schema.org/dataset", "schema.org/Dataset");
			}

			if (tempIRI.lastIndexOf("www.") > 20) {
				tempIRI = "https://" + tempIRI.substring(tempIRI.lastIndexOf("www.")).trim();
			}

			if (tempIRI.endsWith("/")) {
				tempIRI = tempIRI.substring(0, tempIRI.length() - 1);
			}

			return SimpleValueFactory.getInstance().createIRI(tempIRI.replaceAll("licensE", "license")
					.replaceAll("FileFormat", "fileFormat").replaceAll("addType", "additionalType"));
		}
		return SimpleValueFactory.getInstance().createLiteral(object.stringValue().replaceAll("licensE", "license")
				.replaceAll("FileFormat", "fileFormat").replaceAll("addType", "additionalType"));
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
	 * @param url The URL from which the source was obtained
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
			allMarkup = getOnlyJSONLDMarkup(html);
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
	 * A block of JSON can either contain a single JSON description or an array of several JSON descriptions.
	 * For each description this will:
	 * <ol>
	 * <li>Changes context to https://schema.org</li>
	 * <li>adds @id based on url</li>
	 * </ol>
	 * 
	 * 
	 * @param markup A single block of JSON-LD (bio)schema markup as a String
	 * @param url The URL of the site the markup was scraped from
	 * @return mended JSON-LD markup as a String
	 * @throws JsonLDInspectionException when JSON cannot be parsed
	 */
	protected String fixASingleJsonLdBlock(String markup, String url) throws JsonLDInspectionException {
		JSONParser parser = new JSONParser();
		JSONArray jsonArray = null;
		JSONObject jsonObj = null;
		try {
			Object obj  = parser.parse(markup);
			
			if(obj instanceof JSONArray) {
				jsonArray = (JSONArray) obj;
				return fixAJsonLdArray(jsonArray, url);
			} else if(obj instanceof JSONObject) {
				jsonObj = (JSONObject) obj;
				return fixASingleJsonLdObject(jsonObj, url);
			}
			
			throw new JsonLDInspectionException("Unkown object obtained from JSON parser :" + url);
					
		} catch (ParseException e) {
			logger.error("Failed to parse JSONArray from :" + url);
			throw new JsonLDInspectionException("Failed to parse JSON from :" + url);
		}		
	}

	/**
	 * Corrects an {@link JSONArray} of (bio)schemas markup; each element is a {@link JSONObject}. 
	 * Uses {@link #fixASingleJSONLdObject(JSONObject, String)}
	 * 
	 * @param array An {@link JSONArray} of (bio)schemas markup
	 * @param url The URL from which the markup was scraped
	 * @return The corrected markup stringified; will still be in array
	 */
	protected String fixAJsonLdArray(JSONArray array, String url) {
		for (int i = 0; i < array.size(); i++) {
			JSONObject jsonObj = (JSONObject) array.get(i);

			JSONObject correctedObj = fixASingleJSONLdObject(jsonObj, url);

			array.remove(i);
			array.add(i, correctedObj);
		}

		return array.toJSONString().replaceAll("\\\\", "");
	}

	/**
	 * Wrapper that converts {@link #fixASingleJSONLdObject(JSONObject, String)} such that it returns 
	 * a String rather than a {@link JSONObject}
	 * 
	 * @param jsonObj A {@link JSONObject} containing the (bio)schema markup to be corrected
	 * @param url The URL from where the jsonObj was obtained
	 * @return A stringified version of the corrected {@link JSONObject} 
	 */
	protected String fixASingleJsonLdObject(JSONObject jsonObj, String url) {

		JSONObject correctedObj = fixASingleJSONLdObject(jsonObj, url);

		return correctedObj.toJSONString().replaceAll("\\\\", "");
	}
	
	
	/**
	 * 
	 * Corrects/amends a single JSON-LD object markup extracted from the HTML
	 * source
	 * <ol>
	 * <li>Changes context to https://schema.org</li>
	 * <li>adds @id based on url</li>
	 * </ol>
	 *  
	 * @param jsonObj A single block of JSON-LD (bio)schema markup as a {@link JSONObject}
	 * @param url    The URL of the site the markup was scraped from
	 * @return Amended JSON-LD markup as a {@link JSONObject}
	 */
	protected JSONObject fixASingleJSONLdObject(JSONObject jsonObj, String url) {
		if (jsonObj.containsKey("@context")) {
			String contextValue = jsonObj.get("@context").toString();
			if (!(contextValue.equalsIgnoreCase("https://schema.org"))) {
				jsonObj.remove("@context");
				jsonObj.put("@context", "https://schema.org");
			}
			contextValue = jsonObj.get("@context").toString();

		} else {
			jsonObj.put("@context", "https://schema.org");
		}

		if (!jsonObj.containsKey("@id")) {
			if (countOfJSONLD > 0) {
				jsonObj.put("@id", url + "#" + countOfJSONLD);
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

	/**
	 * Changes the HTML such that Any23 can parse it. Bugs in Any23 mean that some
	 * predicates (that should work) break the parser.
	 * 
	 * 
	 * @param html HTML to be corrected
	 * @return Corrected HTML
	 * @see #fixAny23WeirdIssues(String)
	 */
	protected String fixAny23WeirdIssues(String html) {
		return html.replaceAll("license", "licensE").replaceAll("fileFormat", "FileFormat").replaceAll("additionalType",
				"addType");
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
	 * 
	 * Wraps methods to obtain HTML; can be changed for different types of scraper.
	 * 
	 * @param url
	 * @return
	 * @throws FourZeroFourException
	 */
	protected String wrapHTMLExtraction(String url) throws FourZeroFourException {
		String html = "";
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
		return html;
	}

	/**
	 * Orchestrates the scraping of a given URL and writes the output (as quads) to a file
	 * specified in the arguments. If the fileName is not specified, ie null, the contextCounter
	 * will be used to name the file.
	 * 
	 * contextCounter is used a way of keeping track of which URL in a list is being scraped.
	 * This is managed by the calling class. 
	 * 
	 * The file will be located in the location specified in application.properties
	 * 
	 * @param url URL to scrape
	 * @param outputFileName name of file the output will be written to
	 * @param contextCounter The value of the counter used to record which number of URL is being scraped
	 * @param outputFolderName Folder where output is written to
	 * @return FALSE if failed else TRUE
	 * @throws FourZeroFourException
	 * @throws JsonLDInspectionException
	 * @throws CannotWriteException
	 */
	public boolean scrape(String url, String outputFolderName, String outputFileName, Long contextCounter)
			throws FourZeroFourException, JsonLDInspectionException, CannotWriteException {
		if (url.endsWith("/") || url.endsWith("#"))
			url = url.substring(0, url.length() - 1);

		String html = wrapHTMLExtraction(url);

		if (html.contentEquals(""))
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
			return false;

		Model updatedModel = processTriples(n3, sourceIRI, contextCounter);
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
	 * Writes the success or failure of a scrape to the log.
	 * 
	 * @param url          URL scraped
	 * @param result       TRUE for success; FALSE for fail.
	 * @param outputFolder Where the output was written.
	 */
	protected void displayResult(String url, boolean result, String outputFolder) {
		if (result) {
			logger.info(url + " was successfully scraped and written to " + outputFolder);
		} else {
			logger.error(url + " was NOT successfully scraped.");
		}
		logger.info("\n\n");
	}
}
