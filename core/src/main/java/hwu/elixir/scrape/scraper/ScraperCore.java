package hwu.elixir.scrape.scraper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.apache.any23.writer.NQuadsWriter;
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

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.SeleniumException;
import hwu.elixir.utils.ChromeDriverCreator;
import hwu.elixir.utils.Helpers;

/**
 * Provides core functionality for scraping, but is not an actual scraper
 * 
 * 
 * @author kcm
 * @see FileScraper
 * @see hwu.elixir.scrape.ServiceScrapeDriver
 * 
 */
public abstract class ScraperCore {

	private static Logger logger = LoggerFactory.getLogger(System.class.getName());
	private WebDriver driver = ChromeDriverCreator.getInstance();

	private int countOfJSONLD = 0; // number of JSON-LB blocks found in HTML

	/**
	 * An attempt to close the chromedriver opened by Selenium. 
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
			logger.info("Driver is null");
		}
	}

	/**
	 * Uses JSoup to pull the HTML of a NON dynamic web page
	 * 
	 * @param url The address of the site to parse
	 * @return The HTML as a string
	 * @throws FourZeroFourException when url is 404
	 */
	public String getHtml(String url) throws FourZeroFourException {
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
	 * Uses Selenium to pull the HTML of a dynamic web page (ie, executes the JavaScript).
	 * 
	 * @param url The address of the page to parse
	 * @return The HTML as a string
	 * @throws FourZeroFourException when page title is 404
	 * @throws SeleniumException 
	 */
	public String getHtmlViaSelenium(String url) throws FourZeroFourException, SeleniumException {
		try {
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
			logger.error("URL timed out: " + url);
			return null;

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
	 * Extract schema markup in JSON-LD form from a given URL. Will
	 * ignore all other formats of markup.
	 * 
	 * @param url URL to scrape
	 * @return An array in which each element is a block of JSON-LD containing
	 *         schema.org markup.
	 * @throws FourZeroFourException
	 * @throws SeleniumException
	 */
	public String[] getOnlyJSONLD(String url) throws FourZeroFourException, SeleniumException {
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
	 * Extract schema markup in JSON-LD form from given HTML. Will
	 * ignore all other formats of markup.
	 * 
	 * @param html to find JSON-LD in
	 * @return An array in which each element is a block of JSON-LD containing
	 *         schema.org markup.
	 * @throws FourZeroFourException
	 * @throws SeleniumException
	 */
	public String[] getJSONLDMarkup(String html) {
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
	 * Takes an Any23 DocumentSource and converts into triples in N3 form.
	 * 
	 * @param source The HTML as an Any23 DocumentSource
	 * @return Triples in N3 form as a long String
	 * @see DocumentSource
	 */
	public String getTriplesInN3(DocumentSource source) {

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
	 * Takes an Any23 DocumentSource and converts into quads in NQ form.
	 * 
	 * @param source The HTML as an Any23 DocumentSource
	 * @return Triples in quads (NQ) form as a long String
	 * @see DocumentSource
	 */
	public String getTriplesInNQ(DocumentSource source) {

		Any23 runner = new Any23();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(); TripleHandler handler = new NQuadsWriter(out);) {

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
	 * Takes a series of N3 triples as a string and filters them removing triples
	 * with the following predicates:
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
	 * @param n3             The triples to be processed
	 * @param sourceIRI      The URL of the page from which the triples were
	 *                       obtained
	 * @param contextCounter The current counter for the context. Assumes the
	 *                       triples will be placed into crawl repo. If not, any
	 *                       number can be used here.
	 * @return An RDF4J model containing the processed triples
	 * @see Model
	 */
	public Model processTriples(String n3, IRI sourceIRI, Long contextCounter) {
		InputStream input = new ByteArrayInputStream(n3.getBytes(StandardCharsets.UTF_8));

		Model model;
		try {
			model = Rio.parse(input, "", RDFFormat.N3);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			System.out.println("Cannot parse N3 into model");
			logger.error("Cannot parse n3 into a model", e);
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
	 * Takes a series of N3 triples as a string and filters them removing triples
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
	 * @param n3 The triples to be processed
	 * @return An RDF4J model containing the processed triples
	 * @see Model
	 */
	public Model processTriplesLeaveBlankNodes(String n3) {
		InputStream input = new ByteArrayInputStream(n3.getBytes(StandardCharsets.UTF_8));

		Model model;
		try {
			model = Rio.parse(input, "", RDFFormat.N3);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			System.out.println("Cannot parse N3 into model");
			logger.error("Cannot parse n3 into a model", e);
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
	 * Removes changes made to allow Any23 to parse the html & standardises on httpS://schema.org
	 * 
	 * @param predicate
	 * @return Corrected IRI
	 * @see #fixAny23WeirdIssues(String)
	 */
	public IRI fixPredicate(IRI predicate) {
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
	public Value fixObject(Value object) {
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
	 * Problems: only works with json-ld not rdfa etc AND only deals with 1st block
	 * 
	 * Hard to deal with multiple @ contexts as they may be nested in a single
	 * block, or there may be multiple blocks (or a hybrid).
	 * 
	 * Handles contexts that declare prefixes by looking to see if @ context is
	 * followed by a {.
	 * 
	 * 
	 * Adding the @ id in the wrong location can completely break the parsers
	 * ability to generate triples. The location of the injection should be checked
	 * if fewer triples are generated than expected.
	 * 
	 * @param html the source to be changed
	 * @param url  the url to be used for @ id
	 * @return the original source with @ id added in however many blocks there are
	 * @throws JsonLDInspectionException
	 */
	public String injectId(String html, String url) throws MissingHTMLException, JsonLDInspectionException {

		countOfJSONLD = 0;

		if (url == null)
			throw new IllegalArgumentException("url cannot be null");

		if (html == null)
			throw new MissingHTMLException(url);

		int posContext = html.indexOf("@context");
		if (posContext == -1) {
			if (html.indexOf("vocab=\"http://schema.org") != -1 || html.indexOf("vocab=\"https://schema.org") != -1) {
				logger.info("No @context, but a vocab; appears to be RDFa with no JSON-LD: " + url);
			}
		}
		return fixAllJsonLdBlocks(html, url);
	}

	/**
	 * Given HTML source, gets all the JSON-LD blocks and orchestrates the amendment
	 * of them
	 * 
	 * @param html
	 * @param url
	 * @return HTML in which JSON-LD has been corrected
	 * @throws JsonLDInspectionException
	 * @see {@link #fixASingleJsonLdBlock(String, String)}
	 */
	public String fixAllJsonLdBlocks(String html, String url) throws JsonLDInspectionException {
		String[] allMarkup = null;
		if (html.startsWith("{")) {
			logger.info("Just JSON no HTML from: " + url);
			allMarkup = new String[1];
			allMarkup[0] = html;

		} else {
			allMarkup = getJSONLDMarkup(html);
		}

		logger.info("Number of JSONLD sections: " + allMarkup.length);

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
	 * Replaces the old JSON-LD markup with the new markup
	 * 
	 * @param html      Current HTML
	 * @param oldMarkup The markup to be replaced
	 * @param newMarkup The new markup to be added
	 * @return HTML with the newMarkup replacing the oldMarkup
	 */
	public String swapJsonLdMarkup(String html, String oldMarkup, String newMarkup) {

		int oldPosition = html.indexOf(oldMarkup);
		String newHtml = html.substring(0, oldPosition) + newMarkup + html.substring(oldPosition + oldMarkup.length());

		return newHtml;
	}

	/**
	 * 
	 * Corrects/amends a single block of JSON-LD markup extracted from the HTML
	 * source
	 * <ol>
	 * <li>Changes context to https://schema.org</li>
	 * <li>adds @id based on url</li>
	 * </ol>
	 * 
	 * @param markup A single block of JSON-LD (bio)schema markup
	 * @param url    The URL of the site the markup was scraped from
	 * @return Amended JSON-LD markup
	 * @throws JsonLDInspectionException
	 */
	public String fixASingleJsonLdBlock(String markup, String url) throws JsonLDInspectionException {
		JSONParser parser = new JSONParser();
		JSONObject parsedJSON = null;

		try {
			parsedJSON = (JSONObject) parser.parse(markup);
		} catch (ParseException e) {
			logger.error("Failed to parse JSON from :" + url);
			throw new JsonLDInspectionException("Failed to parse JSON from :" + url);
		}

		if (parsedJSON.containsKey("@context")) {
			String contextValue = parsedJSON.get("@context").toString();
			if (!(contextValue.equalsIgnoreCase("https://schema.org"))) {
				parsedJSON.remove("@context");
				parsedJSON.put("@context", "https://schema.org");
			}
			contextValue = parsedJSON.get("@context").toString();

		} else {
			parsedJSON.put("@context", "https://schema.org");
		}

		if (!parsedJSON.containsKey("@id")) {
			if (countOfJSONLD > 0) {
				parsedJSON.put("@id", url + "#" + countOfJSONLD);
			} else {
				parsedJSON.put("@id", url);
			}
		}

		return parsedJSON.toJSONString().replaceAll("\\\\", "");
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
	public String fixAny23WeirdIssues(String html) {
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
	public IRI iriGenerator(String ngraph, IRI sourceIRI) {
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

}
