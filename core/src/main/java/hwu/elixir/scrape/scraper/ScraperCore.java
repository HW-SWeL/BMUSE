package hwu.elixir.scrape.scraper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.JSONLDWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.jsonld.JSONLDParser;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.NTriplesParsingException;
import hwu.elixir.scrape.exceptions.SeleniumException;
import hwu.elixir.scrape.scraper.examples.FileScraper;
import hwu.elixir.scrape.scraper.examples.SingleURLScraper;
import hwu.elixir.utils.ChromeDriverCreator;
import hwu.elixir.utils.ScraperProperties;

/**
 * Provides core functionality for scraping, but is not an actual scraper. See
 * {@link FileScraper} or {@link SingleURLScraper} for examples of how to use
 * this.
 * 
 * @see FileScraper
 * @see package.hwu.elixir.scrape.ServiceScrapeDriver
 * 
 */
public abstract class ScraperCore {

	private WebDriver driver;
 
	private static Logger logger = LoggerFactory.getLogger(ScraperCore.class.getName());

	protected ScraperProperties properties;

	public ScraperCore() {
		properties = ScraperProperties.getInstance();
		driver = ChromeDriverCreator.getInstance();
	}
	

	/**
	 * Close the chromedriver opened by Selenium. Should always be closed at the end
	 * of the scrape.
	 * 
	 * @see ChromeDriverCreator
	 * @see <a href="https://github.com/HW-SWeL/Scraper/issues/42">BMUSE issue 42</a>
	 */
	public void shutdown() {
		if (driver != null) {
			logger.info("Closing driver...");
			driver.quit();
			logger.info("Driver closed.");
		} else {
			logger.info("Driver is null... no need to close.");
		}
	}

	/**
	 *
	 * Wraps methods to obtain HTML; can be changed for different types of scraper.
	 *  This is the entry point to the ScraperCore abstract class
	 *  Please note that both these calls are to get HTML in a static way
	 *
	 * @param url
	 * @return
	 * @throws FourZeroFourException
	 */
	protected String wrapHTMLExtractionStatic(String url) throws FourZeroFourException {
		String html = "";

		try {
			html = getHtml(url);
		} catch (FourZeroFourException e) {
			logger.error("404 error " + e);
		}
		return html;
	}

	/**
	 * 
	 * Wraps methods to obtain HTML; can be changed for different types of scraper.
	 *  This is the entry point to the ScraperCore abstract class
	 *  Please note that both these calls are to get HTML in a dynamic way
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
	 * Uses JSoup to pull the HTML of a NON dynamic web page. Much faster than
	 * 	 * Selenium BUT will not execute JS.
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
		} catch (UnknownHostException ehe) {
			logger.error(url + " cannot be found; UnknownHostException thrown!");
			return null;
		} catch (IOException e) {
			logger.error(url + " produced a " + e.getMessage());
		} catch (IllegalArgumentException e1) {
			logger.error(url + " produced a " + e1.getMessage());
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

			try {
				// Try dynamic page
				driver.get(url);
			} catch (NoSuchSessionException e) {
				System.out.println("TRY AGAIN!");
				driver = ChromeDriverCreator.killAndReopen();
				
				driver.get(url);
			}

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
			// Try static page
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
	}

	/**
	 * Removes trailing # or / at the end of a URL
	 * 
	 * @param url
	 * @return Remove with # or / removed (if they existing)
	 */
	protected String fixURL(String url) {
		if (url.endsWith("/") || url.endsWith("#"))
			url = url.substring(0, url.length() - 1);

		return url;
	}

	private String getJSONLD(InputStream is, String URI) throws IOException, RDFParseException, RDFHandlerException {
		JSONLDParser LDParser = new JSONLDParser();

		try {
			LDParser.parse(is, URI);
		} catch (RDFParseException e) {
			logger.error(e.getMessage());
		} catch (RDFHandlerException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		;



		return LDParser.toString();
	}

	protected String getTripelsInNTriplesRDF4J(DocumentSource source, RDFFormat inFormat, RDFFormat outFormat) throws IOException, RDFParseException, RDFHandlerException {

		OutputStream os = new OutputStream() {
			@Override
			public void write(int i) throws IOException {

			}
		};
		RDFParser rdfParser = Rio.createParser(inFormat);
		RDFWriter rdfWriter = Rio.createWriter(outFormat, os);
		String baseURI = "";
		InputStream is = new InputStream() {
			@Override
			public int read() throws IOException {
				return 0;
			}
		};


		rdfParser.setRDFHandler(rdfWriter);

		try {

			rdfParser.parse(is, source.toString());

		} catch (RDFParseException e) {
			System.out.println(e);
		} catch (RDFHandlerException e) {
			System.out.println(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
		;


		return os.toString();
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
			logger.error("Cannot extract triples", e);
		} catch (IOException e1) {
			logger.error(" IO error whilst extracting triples", e1);
		} catch (TripleHandlerException e2) {
			logger.error("TripleHanderException", e2);
		}

		return null;
	}
	
	/**
	 * SHOULD take an Any23 DocumentSource and converts into triples in JSON-LD form.
	 * SHOULD being the keyword; actually returns an empty string because the JSONLDWriter
	 * doesn't work :( 
	 * 
	 * @param source
	 * @return
	 * @deprecated
	 */
	protected String getTriplesInJSONLD(DocumentSource source) {

		Any23 runner = new Any23();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				TripleHandler handler = new JSONLDWriter(out);) {

			runner.extract(source, handler);
			return out.toString("UTF-8");
		} catch (ExtractionException e) {
			logger.error("Cannot extract triples", e);
		} catch (IOException e) {
			logger.error(" IO error whilst extracting triples", e);
		} catch (TripleHandlerException e1) {
			logger.error("TripleHanderException", e1);
		}

		return null;
	}	

	/**
	 * Loads nTriples into a InputStream and passes that to RDF4J NTriples parser to
	 * generate a {@link Model}
	 * 
	 * @param nTriples String of nTriples
	 * @return RDF4J Model with the triples loaded inside it
	 * @throws RDFParseException            Cannot parse the nTriples for some
	 *                                      reason. Common problem is a URL with an
	 *                                      inappropriate character, e.g., |
	 * @throws UnsupportedRDFormatException
	 * @throws IOException
	 */
	private Model createModelFromNTriples2(String nTriples)
			throws RDFParseException, UnsupportedRDFormatException, IOException {
		InputStream input = new ByteArrayInputStream(nTriples.getBytes(StandardCharsets.UTF_8));

		return Rio.parse(input, "", RDFFormat.NTRIPLES);
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
	 * Generates a RDF4J {@link Model} from a string of NTriples.
	 * 
	 * @param nTriples The string containing the N-Triples to be turned into a Model
	 * @return The Model containing the triples from nTriples
	 * @throws NTriplesParsingException Thrown when the input param cannot be parsed
	 *                                  as NTriples
	 */
	protected Model createModelFromNTriples(String nTriples) throws NTriplesParsingException {

		try {
			return createModelFromNTriples2(nTriples);
		} catch (RDFParseException e) {
			logger.error("RDFParseException, the exception is dealt with by removing some characters and trying again: " + e);
			// RDF4J doesn't like | (ie character U+7C) inside URLs.
			// this removes | from everywhere in the doc...
			//TODO monitor any possible issues by character # inside a URL, relevant issue number 43
			nTriples = nTriples.replaceAll("\\|", "");
			try {
				return createModelFromNTriples2(nTriples);
			} catch (RDFParseException | UnsupportedRDFormatException | IOException e2) {
				logger.error("Cannot parse triples into a model", e2);
				throw new NTriplesParsingException("Cannot parse triples into a model");
			}
		} catch (UnsupportedRDFormatException | IOException e2) {
			logger.error("Cannot parse triples into a model. Already tried removing |", e2);
			throw new NTriplesParsingException("Cannot parse triples into a model");
		}
	}

	/**
	 * Extract schema markup in JSON-LD form from a given HTML. Will ignore all
	 * other formats of markup. Some blocks may not be (bio)schema markup. Will not
	 * process/validate JSON-LD, remove content or add/change @id or @context etc.
	 * 
	 * @param html to find JSON-LD in
	 * @return An array in which each element is a block of JSON-LD containing
	 *         schema.org markup.
	 * @throws FourZeroFourException
	 * @throws SeleniumException
	 */
	protected String[] getOnlyUnfilteredJSONLDFromHtml(String html) {
		Document doc = Jsoup.parse(html);
		Elements jsonElements = doc.getElementsByTag("script").select("[type=application/ld+json]");

		ArrayList<String> jsonMarkup = new ArrayList<String>();
		for (Element jsonElement : jsonElements) {
			if (jsonElement.data() != "" && jsonElement.data() != null) {
				if (jsonElement.data().contains("\"@type") || jsonElement.data().contains("\"@context")) {
					int positionOfClosingTag = jsonElement.data().indexOf("</script");
					if (positionOfClosingTag == -1) {
						jsonMarkup.add(jsonElement.data());
					} else {
						jsonMarkup.add(jsonElement.data().substring(0, positionOfClosingTag));
					}
				}
			}
		}

		String[] toReturn = new String[jsonMarkup.size()];
		jsonMarkup.toArray(toReturn);
		return toReturn;
	}
}
