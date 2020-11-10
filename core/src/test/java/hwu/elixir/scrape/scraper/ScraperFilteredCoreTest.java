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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.*;

import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.exceptions.NTriplesParsingException;
import hwu.elixir.utils.CompareNQ;

public class ScraperFilteredCoreTest {
	
	private static ScraperFilteredCore scraperCore;
	private static File testHtml;
	private static String outputLoction  = System.getProperty("user.home")+File.separator+"toDelete"+File.separator;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File outputFolder = new File(outputLoction);
		boolean result = outputFolder.mkdir();
		if(!result) {
			throw new Exception("Cannot create output folder for temporary files used during ScraperCoreTest!");
		}		
		
		scraperCore = new ScraperFilteredCore();		
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

	//
	
	@Ignore
	@Test
	public void test_scrape_pass() throws FourZeroFourException, JsonLDInspectionException, CannotWriteException, MissingMarkupException {
		String chemblURLOnGitHub = "https://raw.githubusercontent.com/HW-SWeL/Scraper/master/core/src/test/resources/testHtml/chembl.html";
		String outputFileName = "chemblGitHub";
		
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
		} finally {
			liveQuads.delete();
		}
	}
	
	//
	
	@Ignore
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
		} finally {
//		liveQuads.delete();
		}
	}	

	@Ignore
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
//			liveQuads.delete();
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
//			liveQuads.delete();
		}
	}
	
	@Ignore
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
		} finally {
//			liveQuads.delete();
		}
	}
	
	//
	
	@Test
	public void test_swapJsonLdMarkup() {
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

		String[] allMarkup = scraperCore.getOnlyUnfilteredJSONLDFromHtml(html);
		int i = 10;
		for (String oldMarkup : allMarkup) {
			JSONObject obj = new JSONObject();
			obj.put("newBlock", Integer.toString(i++));
			html = scraperCore.swapJsonLdMarkup(html, oldMarkup, obj.toJSONString());
		}

		for (String oldMarkup : allMarkup) {
			assertFalse(html.contains(oldMarkup));
		}

		String[] newAllMarkup = scraperCore.getOnlyUnfilteredJSONLDFromHtml(html);
		for (String newMarkup : newAllMarkup) {
			assertTrue(html.contains(newMarkup));
		}
	}
	
	//
	
	@Test
	public void test_fixASingleJsonLdBlock_object() throws JsonLDInspectionException {
		JSONObject obj = new JSONObject();
		obj.put("key1", "value1");
		String fixedJSON = scraperCore.fixASingleJsonLdBlock(obj.toJSONString(), "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org/docs/jsonldcontext.jsonld\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));
	}
	
	@Test
	public void test_fixASingleJsonLdBlock_array() throws JsonLDInspectionException {
		JSONObject obj1 = new JSONObject();
		obj1.put("key1", "value1");
		
		JSONObject obj2 = new JSONObject();
		obj2.put("key2", "value2");
		obj2.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");
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
		obj1.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");

		obj2.put("@id", "http://www.myId.org");
		obj2.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");

		obj3.put("@id", "http://www.myId.org");
		obj3.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");

		array = new JSONArray();
		array.add(obj1);
		array.add(obj2);
		array.add(obj3);
		
		String expectedJSON = array.toJSONString().replaceAll("\\\\", "");
		
		assertEquals(expectedJSON, fixedJSON);
	}	
	
	//
	
	@Test
	public void test_fixAJsonLdArray() throws JsonLDInspectionException {
		scraperCore = new ScraperFilteredCore(); // reset contextCounter
		
		JSONObject obj1 = new JSONObject();
		obj1.put("key1", "value1");
		
		JSONObject obj2 = new JSONObject();
		obj2.put("key2", "value2");
		obj2.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");

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
		obj1.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");

		obj2.put("@id", "http://www.myId.org");
		obj2.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");

		obj3.put("@id", "http://www.myId.org");
		obj3.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");

		array = new JSONArray();
		array.add(obj1);
		array.add(obj2);
		array.add(obj3);
		
		String expectedJSON = array.toJSONString().replaceAll("\\\\", "");
		assertEquals(expectedJSON, fixedJSON);
	}
	
	//
	
	@Test
	public void test_fixASingleJsonLdObject() throws JsonLDInspectionException {
		JSONObject obj = new JSONObject();
		obj.put("key1", "value1");
		String fixedJSON = scraperCore.fixASingleJsonLdObject(obj, "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org/docs/jsonldcontext.jsonld\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");
		fixedJSON = scraperCore.fixASingleJsonLdObject(obj, "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org/docs/jsonldcontext.jsonld\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "http://www.macs.hw.ac.uk");
		fixedJSON = scraperCore.fixASingleJsonLdObject(obj, "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org/docs/jsonldcontext.jsonld\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));

		obj = new JSONObject();
		obj.put("key1", "value1");
		obj.put("@context", "https://schema.org/docs/jsonldcontext.jsonld");
		obj.put("@id", "http://www.myId.org");
		fixedJSON = scraperCore.fixASingleJsonLdObject(obj, "http://www.myId.org");
		assertTrue(fixedJSON.contains("\"@context\":\"https://schema.org/docs/jsonldcontext.jsonld\""));
		assertTrue(fixedJSON.contains("\"@id\":\"http://www.myId.org\""));
	}
	
	//
	
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
			String html2 = html.replaceFirst("\"@context\": \"http://schema.org/docs/jsonldcontext.jsonld\"",
					"\"@context\": \"http://schema.org/docs/jsonldcontext.jsonld/\"");
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
		ScraperFilteredCore scraperCore = new ScraperFilteredCore();
		
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

			assertTrue(html.contains("\"@context\":\"https://schema.org/docs/jsonldcontext.jsonld\""));
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
	
	//
	
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
}
