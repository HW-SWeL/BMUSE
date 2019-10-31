package hwu.elixir.scrape.scraper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
import org.json.simple.JSONArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.exceptions.NTriplesParsingException;
import hwu.elixir.scrape.exceptions.SeleniumException;
import hwu.elixir.utils.CompareNQ;

public class ScraperCoreTest {
	
	private static ScraperCore scraperCore;
	private static File testHtml;
	private static String outputLoction  = System.getProperty("user.home")+File.separator+"toDelete"+File.separator;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File outputFolder = new File(outputLoction);
		boolean result = outputFolder.mkdir();
		if(!result) {
			throw new Exception("Cannot create output folder for temporary files used during ScraperCoreTest!");
		}		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		File outputFolder = new File(outputLoction);
		String[] listOfFiles = outputFolder.list();
		for(String fileName : listOfFiles) {
			File currentFile = new File(outputFolder.getPath(), fileName);
			currentFile.delete();
		}			
		
		boolean result = outputFolder.delete();
		if(!result) {
			throw new Exception("Cannot delete output folder for temporary files used during ScraperCoreTest!");
		}		
		
		scraperCore.shutdown();
	}

	@Before
	public void setUp() throws Exception {		
		scraperCore = Mockito.mock(ScraperCore.class, Mockito.CALLS_REAL_METHODS);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/*
	 * 
	 */
	
	@Test
	public void test_fixAny23WeirdIssues() {
		String toFix = "<div>license</div> file format file Format \tFile Format fileFormat<p></p>\n additionalType";
		String fixed = scraperCore.fixAny23WeirdIssues(toFix);
		assertEquals("<div>licensE</div> file format file Format \tFile Format FileFormat<p></p>\n addType", fixed);
	}

	@Test
	public void test_iriGenerator() {
		String nGraph = "https://bioschemas.org/crawl/v1/0";
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI("https://www.macs.hw.ac.uk");

		IRI randomIRI = scraperCore.iriGenerator(nGraph, sourceIRI);

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

		randomIRI = scraperCore.iriGenerator(nGraph, sourceIRI);

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
		IRI fixedIRI = scraperCore.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);

		sourceIRI = SimpleValueFactory.getInstance().createIRI("https://schema.org/licensE");
		expectedIRI = SimpleValueFactory.getInstance().createIRI("https://schema.org/license");
		fixedIRI = scraperCore.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);

		sourceIRI = SimpleValueFactory.getInstance().createIRI("https://bioschemas.org/addType");
		expectedIRI = SimpleValueFactory.getInstance().createIRI("https://bioschemas.org/additionalType");
		fixedIRI = scraperCore.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);

		sourceIRI = SimpleValueFactory.getInstance().createIRI("http://bioschemas.org/addType");
		expectedIRI = SimpleValueFactory.getInstance().createIRI("https://bioschemas.org/additionalType");
		fixedIRI = scraperCore.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);

		sourceIRI = SimpleValueFactory.getInstance().createIRI("http://schema.org/licensE");
		expectedIRI = SimpleValueFactory.getInstance().createIRI("https://schema.org/license");
		fixedIRI = scraperCore.fixPredicate(sourceIRI);
		assertEquals(expectedIRI, fixedIRI);
	}

	@Test(expected = FourZeroFourException.class)
	public void test_getHtml_404() throws FourZeroFourException {
		scraperCore.getHtml("https://www.macs.hw.ac.uk/~kcm/bannananan.html");
	}

	@Test
	public void test_getHtml() throws FourZeroFourException {
		String html = scraperCore.getHtml("https://www.macs.hw.ac.uk");
		assert (html.contains("CollegeOrUniversity") && html.contains("BreadcrumbList"));
	}

	@Test
	public void test_getHtmlViaSelenium() throws FourZeroFourException, SeleniumException {
		String html = scraperCore.getHtmlViaSelenium("https://www.macs.hw.ac.uk");
		assert (html.contains("CollegeOrUniversity") && html.contains("BreadcrumbList"));
	}

	@Test
	public void test_getOnlyJSONLD() throws FourZeroFourException, SeleniumException {
		String[] allJsonLD = scraperCore.getOnlyJSONLDFromUrl("http://www.macs.hw.ac.uk");
		assertTrue(allJsonLD.length == 3);
		for (String json : allJsonLD) {
			json = json.trim();
			assertTrue(json.startsWith("{"));
			assertTrue(json.endsWith("}"));
			assertTrue(json.contains("@context"));
			assertTrue(json.contains("schema.org"));
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

			String outHtml = scraperCore.injectId(html, "http://test.com");
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
			String outHtml = scraperCore.injectId(html, "https://myId.com");
			assertTrue(outHtml.contains("\"@id\":\"https://myId.com\","));

			// got a trailing /
			String html2 = html.replaceFirst("\"@context\": \"http://schema.org\"",
					"\"@context\": \"http://schema.org/\"");
			outHtml = scraperCore.injectId(html2, "https://myId.com");
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
			outHtml = scraperCore.injectId(html, "https://myId.com");
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
			String outHtml = scraperCore.injectId(html, "https://myId.com");
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
			String outHtml = scraperCore.injectId(html, "https://myId.com");			
			assertTrue(outHtml.contains("\"@id\":\"https://myId.com\","));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
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

			html = scraperCore.injectId(html, "https://myId.com");

			assertTrue(html.contains("\"@context\":\"https://schema.org\""));
			assertTrue(html.contains("\"@id\":\"https://myId.com\""));

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
		scraperCore.injectId(null, "https://myId.com");
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_injectId_nullURL() throws MissingHTMLException, JsonLDInspectionException {
		scraperCore.injectId("", null);
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

			String outHtml = scraperCore.injectId(html, "https://myId.com");
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
		Value outNode = scraperCore.fixObject(bNode);

		assertEquals(outNode, bNode);
	}

	@Test
	public void test_fixObject() {
		IRI inIRI = SimpleValueFactory.getInstance().createIRI("http://bioschemas.org");
		Value outIRI = scraperCore.fixObject(inIRI);
		assertEquals("https://bioschemas.org", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance().createIRI("http://schema.org/DataSet");
		outIRI = scraperCore.fixObject(inIRI);
		assertEquals("https://schema.org/Dataset", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance().createIRI("http://schema.org/dataSet");
		outIRI = scraperCore.fixObject(inIRI);
		assertEquals("https://schema.org/Dataset", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance().createIRI("http://schema.org/dataset/");
		outIRI = scraperCore.fixObject(inIRI);
		assertEquals("https://schema.org/Dataset", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance()
				.createIRI("http://schema.org/dataset/www.bioschemas.org/licensE/FileFormat/addType");
		outIRI = scraperCore.fixObject(inIRI);
		assertEquals("https://www.bioschemas.org/license/fileFormat/additionalType", outIRI.stringValue());

		inIRI = SimpleValueFactory.getInstance().createIRI("https://schema.org/dataset/www.bioschemas.org/licensE");
		outIRI = scraperCore.fixObject(inIRI);
		assertEquals("https://www.bioschemas.org/license", outIRI.stringValue());

		Literal inLiteral = SimpleValueFactory.getInstance()
				.createLiteral("https://schema.org/dataset/www.bioschemas.org/licensE/FileFormat/addType");
		Value outLiteral = scraperCore.fixObject(inLiteral);
		assertEquals("https://schema.org/dataset/www.bioschemas.org/license/fileFormat/additionalType",
				outLiteral.stringValue());
	}

	@Test
	public void test_fixASingleJsonLdObject() throws JsonLDInspectionException {
		JSONObject obj = new JSONObject();
		obj.put("key1", "value1");
		String fixedJSON = scraperCore.fixASingleJsonLdObject(obj, "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "https://schema.org");
		fixedJSON = scraperCore.fixASingleJsonLdObject(obj, "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "http://www.macs.hw.ac.uk");
		fixedJSON = scraperCore.fixASingleJsonLdObject(obj, "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "https://schema.org");
		obj.put("@id", "http://www.myId.org");
		fixedJSON = scraperCore.fixASingleJsonLdObject(obj, "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));
	}
	
	@Test
	public void test_fixAJsonLdArray() throws JsonLDInspectionException {
		JSONObject obj1 = new JSONObject();
		obj1.put("key1", "value1");
		
		JSONObject obj2 = new JSONObject();
		obj2.put("key2", "value2");
		obj2.put("@context", "https://schema.org");
		
		JSONObject obj3 = new JSONObject();
		obj3.put("key3", "value3");
		obj3.put("@context", "http://www.macs.hw.ac.uk");
		
		JSONArray array = new JSONArray();
		array.add(obj1);
		array.add(obj2);
		array.add(obj3);
		
		String fixedJSON = scraperCore.fixAJsonLdArray(array, "http://www.myId.org");
		//
		obj1.put("@id", "http://www.myId.org");
		obj1.put("@context", "https://schema.org");
		
		obj2.put("@id", "http://www.myId.org");
		obj2.put("@context", "https://schema.org");
		
		obj3.put("@id", "http://www.myId.org");
		obj3.put("@context", "https://schema.org");
		
		array = new JSONArray();
		array.add(obj1);
		array.add(obj2);
		array.add(obj3);
		
		String expectedJSON = array.toJSONString().replaceAll("\\\\", "");
		
		assertEquals(expectedJSON, fixedJSON);
		
	}

	@Test
	public void test_fixASingleJsonLdBlock_array() throws JsonLDInspectionException {
		JSONObject obj1 = new JSONObject();
		obj1.put("key1", "value1");
		
		JSONObject obj2 = new JSONObject();
		obj2.put("key2", "value2");
		obj2.put("@context", "https://schema.org");
		
		JSONObject obj3 = new JSONObject();
		obj3.put("key3", "value3");
		obj3.put("@context", "http://www.macs.hw.ac.uk");
		
		JSONArray array = new JSONArray();
		array.add(obj1);
		array.add(obj2);
		array.add(obj3);
		
		String fixedJSON = scraperCore.fixASingleJsonLdBlock(array.toJSONString(), "http://www.myId.org");
		//
		obj1.put("@id", "http://www.myId.org");
		obj1.put("@context", "https://schema.org");
		
		obj2.put("@id", "http://www.myId.org");
		obj2.put("@context", "https://schema.org");
		
		obj3.put("@id", "http://www.myId.org");
		obj3.put("@context", "https://schema.org");
		
		array = new JSONArray();
		array.add(obj1);
		array.add(obj2);
		array.add(obj3);
		
		String expectedJSON = array.toJSONString().replaceAll("\\\\", "");
		
		assertEquals(expectedJSON, fixedJSON);
	}
	
	@Test
	public void test_fixASingleJsonLdBlock_object() throws JsonLDInspectionException {
		JSONObject obj = new JSONObject();
		obj.put("key1", "value1");
		String fixedJSON = scraperCore.fixASingleJsonLdBlock(obj.toJSONString(), "http://www.myId.org");
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
		String[] allJsonMarkup = scraperCore.getOnlyJSONLDFromHtml(html);
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

		String[] allMarkup = scraperCore.getOnlyJSONLDFromHtml(html);
		int i = 10;
		for (String oldMarkup : allMarkup) {
			JSONObject obj = new JSONObject();
			obj.put("newBlock", Integer.toString(i++));
			html = scraperCore.swapJsonLdMarkup(html, oldMarkup, obj.toJSONString());
		}

		for (String oldMarkup : allMarkup) {
			assertFalse(html.contains(oldMarkup));
		}

		String[] newAllMarkup = scraperCore.getOnlyJSONLDFromHtml(html);
		for (String newMarkup : newAllMarkup) {
			assertTrue(html.contains(newMarkup));
		}
	}

	@Test
	public void processTriples_hamap() throws NTriplesParsingException {
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
			html = scraperCore.injectId(html, "https://hamap.expasy.org");

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		DocumentSource source = new StringDocumentSource(html, "https://hamap.expasy.org");
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());
		String n3 = scraperCore.getTriplesInNTriples(source);
		Model liveModel = scraperCore.processTriples(n3, sourceIRI, 100000L);

		File liveQuads = new File(outputLoction+"test_live_hamap.nq");

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
	public void processTriples_biosamples() throws NTriplesParsingException {
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
			html = scraperCore.injectId(html, "http://www.macs.hw.ac.uk/shouldNotBeInjected");

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		DocumentSource source = new StringDocumentSource(html, "https://www.ebi.ac.uk/biosamples/samples/SAMEA4999347");
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());
		String n3 = scraperCore.getTriplesInNTriples(source);
		Model liveModel = scraperCore.processTriples(n3, sourceIRI, 100000L);

		File liveQuads = new File(outputLoction+"test_live_biosamples.nq");

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
	public void processTriples_chembl() throws NTriplesParsingException {
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
			html = scraperCore.injectId(html, "https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/");

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		DocumentSource source = new StringDocumentSource(html,
				"https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/");
		IRI sourceIRI = SimpleValueFactory.getInstance().createIRI(source.getDocumentIRI());
		String n3 = scraperCore.getTriplesInNTriples(source);
		Model liveModel = scraperCore.processTriples(n3, sourceIRI, 100000L);

		File liveQuads = new File(outputLoction+"test_live_chembl.nq");

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
	
	
	@Test
	public void test_processTriplesLeaveBlankNodes_chembl() throws NTriplesParsingException {
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
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}	
		
		DocumentSource source = new StringDocumentSource(html,
				"https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL59/");
		String n3 = scraperCore.getTriplesInNTriples(source);
		Model liveModel = scraperCore.processTriplesLeaveBlankNodes(n3);		
	
		File liveQuads = new File(outputLoction+"test_live_chembl.nq");

		try (PrintWriter out = new PrintWriter(liveQuads)) {
			Rio.write(liveModel, out, RDFFormat.NQUADS);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
		String resourceName = "testRDF/unprocessedChembl.nq";
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
	public void test_wrapHTMLExtraction_basic() throws FourZeroFourException, SeleniumException {
		String url = "https://www.macs.hw.ac.uk";
		String html1 = scraperCore.getHtmlViaSelenium(url);
		String html2 = scraperCore.wrapHTMLExtraction(url);
		
		assertEquals(html2, html1);
	}

	
	@Test
	public void test_wrapHTMLExtraction_fail() throws FourZeroFourException, SeleniumException {
		String url = "https://www.apJ7G2m!.com";
		String html1 = scraperCore.wrapHTMLExtraction(url);	
		assertNull(html1);
	}
	
	@Test
	public void test_fixURL_pass() {
		String url = "https://www.macs.hw.ac.uk";
		assertEquals(url, scraperCore.fixURL(url));
		
		String url2 = "https://www.macs.hw.ac.uk/";
		assertEquals(url, scraperCore.fixURL(url2));
		
		url2 = "https://www.macs.hw.ac.uk#";
		assertEquals(url, scraperCore.fixURL(url2));	
	}
	
	
	@Test
	public void test_scrape_pass() throws FourZeroFourException, JsonLDInspectionException, CannotWriteException, MissingMarkupException {
		String chemblURLOnGitHub = "https://raw.githubusercontent.com/HW-SWeL/Scraper/master/core/src/test/resources/testHtml/chembl.html";
		String outputFileName = "chembl";
		
		scraperCore.scrape(chemblURLOnGitHub, outputLoction, outputFileName, 100000L);
		String fileName = outputLoction+outputFileName+".nq";		
		File liveQuads = new File(fileName);
		
		String resourceName = "testRDF/chemblFromGitHub.nq";
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
	}
	
}
