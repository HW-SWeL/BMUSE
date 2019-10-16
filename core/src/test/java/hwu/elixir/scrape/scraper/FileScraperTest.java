package hwu.elixir.scrape.scraper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.SeleniumException;
import hwu.elixir.utils.CompareNQ;

public class FileScraperTest {

	private static FileScraper fileScraper;
	private static File testHtml;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		fileScraper = new FileScraper();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		fileScraper.shutdown();
	}

	@Before
	public void setUp() throws Exception {
//		simpleCore = Mockito.mock(ScraperCore.class, Mockito.CALLS_REAL_METHODS);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test_fixAny23WeirdIssues() {
		String toFix = "<div>license</div> file format file Format \tFile Format fileFormat<p></p>\n additionalType";
		String fixed = fileScraper.fixAny23WeirdIssues(toFix);
		assertEquals("<div>licensE</div> file format file Format \tFile Format FileFormat<p></p>\n addType", fixed);
	}

	@Test
	public void test_iriGenerator() {
		String nGraph = "https://bioschemas.org/crawl/v1/0";
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI("https://www.macs.hw.ac.uk");

		IRI randomIRI = fileScraper.iriGenerator(nGraph, sourceIRI);

		String uri = randomIRI.stringValue();
		assertTrue(uri.startsWith("https://bioschemas.org/crawl/v1/0/www.macs.hw.ac.uk/"));
		int pos = uri.lastIndexOf("/");
		String randomNumberElement = uri.substring(pos + 1);
		try {
			Integer.parseInt(randomNumberElement);
		} catch (NumberFormatException e) {
			fail();
		}

		nGraph = "https://bioschemas.org/crawl/v1/0";
		sourceIRI = SimpleValueFactory.getInstance().createIRI("http://www.macs.hw.ac.uk#");

		randomIRI = fileScraper.iriGenerator(nGraph, sourceIRI);

		uri = randomIRI.stringValue();
		assertTrue(uri.startsWith("https://bioschemas.org/crawl/v1/0/www.macs.hw.ac.uk#"));
		pos = uri.lastIndexOf("#");
		randomNumberElement = uri.substring(pos + 1);
		try {
			Integer.parseInt(randomNumberElement);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void test_fixPredicate() {
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI("https://www.macs.hw.ac.uk/");
		IRI expectedIRI = SimpleValueFactory.getInstance().createIRI("https://www.macs.hw.ac.uk");
		IRI fixedIRI = fileScraper.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);

		sourceIRI = SimpleValueFactory.getInstance().createIRI("https://schema.org/licensE");
		expectedIRI = SimpleValueFactory.getInstance().createIRI("https://schema.org/license");
		fixedIRI = fileScraper.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);

		sourceIRI = SimpleValueFactory.getInstance().createIRI("https://bioschemas.org/addType");
		expectedIRI = SimpleValueFactory.getInstance().createIRI("https://bioschemas.org/additionalType");
		fixedIRI = fileScraper.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);

		sourceIRI = SimpleValueFactory.getInstance().createIRI("http://bioschemas.org/addType");
		expectedIRI = SimpleValueFactory.getInstance().createIRI("https://bioschemas.org/additionalType");
		fixedIRI = fileScraper.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);

		sourceIRI = SimpleValueFactory.getInstance().createIRI("http://schema.org/licensE");
		expectedIRI = SimpleValueFactory.getInstance().createIRI("https://schema.org/license");
		fixedIRI = fileScraper.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);
	}

	@Test(expected = FourZeroFourException.class)
	public void test_getHtml_404() throws FourZeroFourException {
		fileScraper.getHtml("https://www.macs.hw.ac.uk/~kcm/bannananan.html");
	}

	@Test
	public void test_getHtml() throws FourZeroFourException {
		String html = fileScraper.getHtml("https://www.macs.hw.ac.uk");
		assert (html.contains("CollegeOrUniversity") && html.contains("BreadcrumbList"));
	}

	@Test
	public void test_getHtmlViaSelenium() throws FourZeroFourException, SeleniumException {
		String html = fileScraper.getHtmlViaSelenium("https://www.macs.hw.ac.uk");
		assert (html.contains("CollegeOrUniversity") && html.contains("BreadcrumbList"));
	}

	@Test
	public void test_getOnlyJSONLD() throws FourZeroFourException, SeleniumException {
		String[] allJsonLD = fileScraper.getOnlyJSONLD("http://www.macs.hw.ac.uk");
		assertTrue(allJsonLD.length == 3);
		for (String json : allJsonLD) {
			assertTrue(json.startsWith("{") && json.endsWith("}"));
			assertTrue(json.contains("@context") && json.contains("schema.org"));
		}
	}

	@Test
	public void test_injectId_alreadyGotId() {
		try {
			String resourceName = "testHtml/contextAtEndWithId.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());
			String html = "";
			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}

			String outHtml = fileScraper.injectId(html, "http://test.com");
			assertEquals(html, outHtml);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}

	@Test
	public void test_injectId() {
		try {
			String resourceName = "testHtml/fairsharing.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());
			String html = "";
			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}

			// no trailing /
			String outHtml = fileScraper.injectId(html, "https://myId.com");
			assertTrue(outHtml.contains("\"@id\":\"https://myId.com\","));

			// got a trailing /
			String html2 = html.replaceFirst("\"@context\": \"http://schema.org\"",
					"\"@context\": \"http://schema.org/\"");
			outHtml = fileScraper.injectId(html2, "https://myId.com");
			assertTrue(outHtml.contains("\"@id\":\"https://myId.com\","));

			// context at end of JSON-LD
			resourceName = "testHtml/contextAtEnd.html";
			classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());
			html = "";
			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}
			outHtml = fileScraper.injectId(html, "https://myId.com");
			assertTrue(outHtml.contains("\"@id\":\"https://myId.com\""));

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void test_injectId_hamap() {
		try {
			String resourceName = "testHtml/hamap.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());
			String html = "";
			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}
			String outHtml = fileScraper.injectId(html, "https://myId.com");
			assertTrue(outHtml.contains("\"@id\":\"https://myId.com\","));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void test_injectId_multipleContext() {
		try {
			String resourceName = "testHtml/mutipleContext.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());
			String html = "";
			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}
			String outHtml = fileScraper.injectId(html, "https://myId.com");
			System.out.println(outHtml);
			assertTrue(outHtml.contains("\"@id\":\"https://myId.com\","));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void test_injectId_MissingContext() throws MissingHTMLException, JsonLDInspectionException {
		String resourceName = "testHtml/contextAtEnd.html";
		ClassLoader classLoader = getClass().getClassLoader();
		testHtml = new File(classLoader.getResource(resourceName).getFile());
		String html = "";
		try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
			String line = "";
			while ((line = br.readLine()) != null) {
				html += line;
			}
			html = html.trim();

			html = html.replaceFirst("@context", "@apple");

			html = fileScraper.injectId(html, "https://myId.com");

			assertTrue(html.contains("\"@context\":\"https://myId.com\""));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test(expected = MissingHTMLException.class)
	public void test_injectId_MHException() throws MissingHTMLException, JsonLDInspectionException {
		fileScraper.injectId(null, "https://myId.com");
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_injectId_nullURL() throws MissingHTMLException, JsonLDInspectionException {
		fileScraper.injectId("", null);
	}

	@Test
	public void test_injectId_rdfa() throws MissingHTMLException, JsonLDInspectionException {
		String resourceName = "testHtml/rdfaTestPage.html";
		ClassLoader classLoader = getClass().getClassLoader();
		testHtml = new File(classLoader.getResource(resourceName).getFile());
		String html = "";
		try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
			String line = "";
			while ((line = br.readLine()) != null) {
				html += line;
			}
			html = html.trim();

			String outHtml = fileScraper.injectId(html, "https://myId.com");
			assertEquals(outHtml, html);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void test_fixObject_BNode() {
		BNode bNode = SimpleValueFactory.getInstance().createBNode("a bNode");
		Value outNode = fileScraper.fixObject(bNode);

		assertEquals(outNode, bNode);
	}

	@Test
	public void test_fixObject() {
		IRI inIRI = SimpleValueFactory.getInstance().createIRI("http://bioschemas.org");
		Value outIRI = fileScraper.fixObject(inIRI);
		assertEquals("https://bioschemas.org", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance().createIRI("http://schema.org/DataSet");
		outIRI = fileScraper.fixObject(inIRI);
		assertEquals("https://schema.org/Dataset", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance().createIRI("http://schema.org/dataSet");
		outIRI = fileScraper.fixObject(inIRI);
		assertEquals("https://schema.org/Dataset", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance().createIRI("http://schema.org/dataset/");
		outIRI = fileScraper.fixObject(inIRI);
		assertEquals("https://schema.org/Dataset", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance()
				.createIRI("http://schema.org/dataset/www.bioschemas.org/licensE/FileFormat/addType");
		outIRI = fileScraper.fixObject(inIRI);
		assertEquals("https://www.bioschemas.org/license/fileFormat/additionalType", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance().createIRI("https://schema.org/dataset/www.bioschemas.org/licensE");
		outIRI = fileScraper.fixObject(inIRI);
		assertEquals("https://www.bioschemas.org/license", outIRI.stringValue());

		Literal inLiteral = SimpleValueFactory.getInstance()
				.createLiteral("https://schema.org/dataset/www.bioschemas.org/licensE/FileFormat/addType");
		Value outLiteral = fileScraper.fixObject(inLiteral);
		assertEquals("https://schema.org/dataset/www.bioschemas.org/license/fileFormat/additionalType",
				outLiteral.stringValue());
	}

	@Test
	public void test_fixASingleContext() throws JsonLDInspectionException {
		JSONObject obj = new JSONObject();
		obj.put("key1", "value1");
		String fixedJSON = fileScraper.fixASingleJsonLdBlock(obj.toJSONString(), "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "https://schema.org");
		fixedJSON = fileScraper.fixASingleJsonLdBlock(obj.toJSONString(), "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "http://www.macs.hw.ac.uk");
		fixedJSON = fileScraper.fixASingleJsonLdBlock(obj.toJSONString(), "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "https://schema.org");
		obj.put("@id", "http://www.myId.org");
		fixedJSON = fileScraper.fixASingleJsonLdBlock(obj.toJSONString(), "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));
	}

	@Test
	public void test_getJSONLDMarkup() {
		String html = "";
		try {
			String resourceName = "testHtml/basicWithJSONLD.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());

			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		String[] allJsonMarkup = fileScraper.getJSONLDMarkup(html);
		assertTrue(allJsonMarkup.length == 4);

		for (String json : allJsonMarkup) {
			assertFalse(json.contains("script"));
			assertFalse(json.contains("meta"));
			assertFalse(json.contains("head"));
			assertFalse(json.contains("body"));

			assertTrue(json.contains("{") && json.contains("}"));
			assertTrue((json.contains("@context") && json.contains("https://schema.org"))
					|| (json.contains("@type") && json.contains("Person")));
			assertTrue(json.contains("key1") && json.contains("value1"));
			assertTrue(json.contains("block"));
		}
	}

	@Test
	public void test_swapMarkup() {
		String html = "";
		try {
			String resourceName = "testHtml/basicWithJSONLD.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());

			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		String[] allMarkup = fileScraper.getJSONLDMarkup(html);
		int i = 10;
		for (String oldMarkup : allMarkup) {
			JSONObject obj = new JSONObject();
			obj.put("newBlock", Integer.toString(i++));
			html = fileScraper.swapJsonLdMarkup(html, oldMarkup, obj.toJSONString());
		}

		for (String oldMarkup : allMarkup) {
			assertFalse(html.contains(oldMarkup));
		}

		String[] newAllMarkup = fileScraper.getJSONLDMarkup(html);
		for (String newMarkup : newAllMarkup) {
			assertTrue(html.contains(newMarkup));
		}
	}

	@Test
	public void processTriples_hamap() {
		String html = "";
		try {
			String resourceName = "testHtml/hamap.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());

			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}
			html = fileScraper.injectId(html, "https://hamap.expasy.org");

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		DocumentSource source = new StringDocumentSource(html, "https://hamap.expasy.org");
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());
		String n3 = fileScraper.getTriplesInNTriples(source);
		Model liveModel = fileScraper.processTriples(n3, sourceIRI, 100000L);

		File liveQuads = new File("test_live_hamap.nq");

		try (PrintWriter out = new PrintWriter(liveQuads)) {
			Rio.write(liveModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		String resourceName = "testRDF/hamap.nq";
		ClassLoader classLoader = getClass().getClassLoader();
		File storedQuads = new File(classLoader.getResource(resourceName).getFile());

		CompareNQ compare = new CompareNQ();
		try {
			assertTrue(compare.compare(liveQuads, storedQuads));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		liveQuads.delete();
	}

	@Test
	public void processTriples_biosamples() {
		String html = "";
		try {
			String resourceName = "testHtml/biosamples.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());

			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}
			html = fileScraper.injectId(html, "http://www.macs.hw.ac.uk/shouldNotBeInjected");

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		DocumentSource source = new StringDocumentSource(html, "https://www.ebi.ac.uk/biosamples/samples/SAMEA4999347");
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());
		String n3 = fileScraper.getTriplesInNTriples(source);
		Model liveModel = fileScraper.processTriples(n3, sourceIRI, 100000L);

		File liveQuads = new File("test_live_biosamples.nq");

		try (PrintWriter out = new PrintWriter(liveQuads)) {
			Rio.write(liveModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			e.printStackTrace();
			liveQuads.delete();
			fail();
		}

		String resourceName = "testRDF/biosamples.nq";
		ClassLoader classLoader = getClass().getClassLoader();
		File storedQuads = new File(classLoader.getResource(resourceName).getFile());

		CompareNQ compare = new CompareNQ();
		try {
			assertTrue(compare.compare(liveQuads, storedQuads));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} finally {
			liveQuads.delete();
		}
	}

	@Test
	public void processTriples_chembl() {
		String html = "";
		try {
			String resourceName = "testHtml/chembl.html";
			ClassLoader classLoader = getClass().getClassLoader();
			testHtml = new File(classLoader.getResource(resourceName).getFile());

			try (BufferedReader br = new BufferedReader(new FileReader(testHtml))) {
				String line = "";
				while ((line = br.readLine()) != null) {
					html += line;
				}
				html = html.trim();
			}
			html = fileScraper.injectId(html, "https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/");

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		DocumentSource source = new StringDocumentSource(html,
				"https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/");
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());
		String n3 = fileScraper.getTriplesInNTriples(source);
		Model liveModel = fileScraper.processTriples(n3, sourceIRI, 100000L);

		File liveQuads = new File("test_live_chembl.nq");

		try (PrintWriter out = new PrintWriter(liveQuads)) {
			Rio.write(liveModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		String resourceName = "testRDF/chembl.nq";
		ClassLoader classLoader = getClass().getClassLoader();
		File storedQuads = new File(classLoader.getResource(resourceName).getFile());

		CompareNQ compare = new CompareNQ();
		try {
			assertTrue(compare.compare(liveQuads, storedQuads));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		liveQuads.delete();
	}
}
