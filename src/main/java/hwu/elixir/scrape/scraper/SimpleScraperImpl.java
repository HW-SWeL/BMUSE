package hwu.elixir.scrape.scraper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.rdf4j.model.impl.SimpleBNode;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.db.crawl.StateOfCrawl;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingContextException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.utils.GetHTMLFromNode;
import hwu.elixir.utils.Helpers;

/**
 * Scrapes a given URL, converts into NQuads and writes to a file (name derived
 * from URL). If the file already exists it will be overwritten.
 * 
 * 
 * @author kcm
 * @see Scraper
 * 
 */
public class SimpleScraperImpl implements Scraper {

	private static Logger logger = LoggerFactory.getLogger("hwu.elixir.scrape.SimpleScraperImpl");

	private GetHTMLFromNode getHtmlFromNode = new GetHTMLFromNode();

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
	 */
	public boolean scrape(String url, Long contextCounter, String folderToWriteNQTo, StateOfCrawl status)
			throws HtmlExtractorServiceException, FourZeroFourException, JsonLDInspectionException {

		if (url.endsWith("/") || url.endsWith("#"))
			url = url.substring(0, url.length() - 1);

		String html = "";
		if (status.equals(StateOfCrawl.UNTRIED) || status.equals(StateOfCrawl.FAILED)) {
			html = getHtml(url);
		} else if (status.equals(StateOfCrawl.FAILED_TWICE) || status.equals(StateOfCrawl.NODE_FAILED)) {
			System.out.println("using puppetter");
			html = getHtmlFromNode.getHtml(url);
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
			System.exit(0);
		}

		if (!new File(fileName).exists())
			System.exit(0);

		return true;
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
	 * Takes a series of N3 triples as a string and filters them removing triples
	 * with the following predicates:
	 * <ol>
	 * <li>nofollow</li>
	 * <li>ogp.me/...</li>
	 * <li>xhtml/vocab</li>
	 * <li>vocab.sindice</li>
	 * </ol>
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
	 * @param contextCounter The current counter for the context.
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
	 * Removes changes made to allow Any23 to parse the html
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
//				if (tempIRI.startsWith("https")) {
					tempIRI = "https://" + tempIRI.substring(tempIRI.lastIndexOf("www.")).trim();
//				} else {
//					tempIRI = "https://" + tempIRI.substring(tempIRI.lastIndexOf("www.")).trim(); 
//				}
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
	 * Injects an @ id attribute into the given html source. Thus prevents Any23
	 * creating blank nodes at the top of the graph
	 * 
	 * Problems: only works with json-ld not rdfa etc AND only deals with 1st block
	 * 
	 * Hard to deal with multiple @ contexts as they may be nested in a single
	 * block, or there may be multiple blocks (or a hybrid).
	 * 
	 * Handles contexts that declare prefixes by looking to see if @ context is
	 * followed by a {.
	 * 
	 * Looks 100 characters before & after the context to see if an @ id is defined.
	 * If not, it is injected using the url given.
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
	public String injectId(String html, String url)
			throws MissingContextException, MissingHTMLException, JsonLDInspectionException {

		if (url == null)
			throw new IllegalArgumentException("url cannot be null");

		if (html == null)
			throw new MissingHTMLException(url);

		if (html.indexOf("vocab=\"http://schema.org") != -1 || html.indexOf("vocab=\"https://schema.org") != -1) {
			logger.info("Appears to be RDFa; no injection for: " + url);
			return html;
		}

		int posContext = html.indexOf("@context");
		if (posContext == -1) {
			logger.info("Missing context in " + url);
			throw new MissingContextException(url);
		} else {
			if (html.indexOf("@id") != -1) {
				logger.info("Not injecting id into: " + url);
			} else {

				String toInject = "\"@id\": \"" + url + "\"";
				
				Pattern pattern = Pattern.compile("\"?@context\"?\\s*:\\s*\"?");
				Matcher matcher = pattern.matcher(html);
				if (matcher.find()) {
					String tempStart = html.substring(0, matcher.start());					
					tempStart += " " + toInject + ", ";
					tempStart += html.substring(matcher.start());
					return tempStart;					
				}				
				
				// should never happen
				logger.warn(url + " did not receive any injection! posContext is " + posContext);
				throw new JsonLDInspectionException(url);
			}
		}

		return html;

	}

	public String fixAny23WeirdIssues(String html) {
		// any23 has problems with license & fileFormat
		return html.replaceAll("license", "licensE").replaceAll("fileFormat", "FileFormat").replaceAll("additionalType",
				"addType");
	}

	// move to helper?
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

	public static void main(String[] args) {
		try {
			SimpleScraperImpl scraper = new SimpleScraperImpl();
//			scraper.scrape("https://www.bbc.co.uk", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
			scraper.scrape("https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
//			scraper.scrape("https://www.ebi.ac.uk/biosamples/samples/SAMEA4999347", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);  // works
//			scraper.scrape("https://www.ebi.ac.uk/biosamples/samples/SAMN00025378", 100000L, "/Users/kcm/",StateOfCrawl.UNTRIED); // 404s
//			scraper.scrape("https://www.alliancegenome.org/gene/MGI:2442292", 100000L, "/Users/kcm/",StateOfCrawl.FAILED_TWICE); // AllianceGenome
//			scraper.scrape("https://hamap.expasy.org/", 100000L, "/Users/kcm/", StateOfCrawl.UNTRIED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
