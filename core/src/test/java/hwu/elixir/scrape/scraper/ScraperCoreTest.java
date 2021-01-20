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
import java.io.FileWriter;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
		
		scraperCore = Mockito.mock(ScraperCore.class, Mockito.CALLS_REAL_METHODS);
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
	
	@Test
	public void test_fixAny23WeirdIssues() {
		String toFix = "<div>license</div> file format file Format \tFile Format fileFormat<p></p>\n additionalType";
		String fixed = scraperCore.fixAny23WeirdIssues(toFix);
		assertEquals("<div>licensE</div> file format file Format \tFile Format FileFormat<p></p>\n addType", fixed);
	}

	//

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

	//
	
	@Test(expected = FourZeroFourException.class)
	public void test_getHtml_404() throws FourZeroFourException {
		scraperCore.getHtml("https://www.macs.hw.ac.uk/~kcm/bannananan.html");
	}

	@Test
	public void test_getHtml() throws FourZeroFourException {
		String html = scraperCore.getHtml("https://www.macs.hw.ac.uk/SWeL/BMUSE/tests/20191024203151_MACS.htm");
		assert (html.contains("CollegeOrUniversity") && html.contains("BreadcrumbList"));
	}

	//
	
	@Test
	public void test_getHtmlViaSelenium() throws FourZeroFourException, SeleniumException {
		String html = scraperCore.getHtmlViaSelenium("https://www.macs.hw.ac.uk/SWeL/BMUSE/tests/20191024203151_MACS.htm");
		assert (html.contains("CollegeOrUniversity") && html.contains("BreadcrumbList"));
	}

	//
	
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

	//

	@Test
	public void test_getOnlyUnfilteredJSONLDFromHtml() {
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
		String[] allJsonMarkup = scraperCore.getOnlyUnfilteredJSONLDFromHtml(html);
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

	//
	
	@Test
	public void test_wrapHTMLExtraction_basic() throws FourZeroFourException, SeleniumException {
		String url = "https://www.macs.hw.ac.uk/SWeL/BMUSE/tests/20191024203151_MACS.htm";
		String html1 = scraperCore.getHtmlViaSelenium(url);
		String html2 = scraperCore.wrapHTMLExtraction(url);
		
		assertEquals(html2, html1);
	}

	//

	@Ignore
	@Test
	public void test_wrapHTMLExtraction_fail() throws FourZeroFourException, SeleniumException {
		String url = "https://www.apJ7G2m!.com";
		String html1 = scraperCore.wrapHTMLExtraction(url);	
		assertNull(html1);
	}
	
	//
	
	@Test
	public void test_fixURL_pass() {
		String url = "https://www.macs.hw.ac.uk";
		assertEquals(url, scraperCore.fixURL(url));
		
		String url2 = "https://www.macs.hw.ac.uk/";
		assertEquals(url, scraperCore.fixURL(url2));
		
		url2 = "https://www.macs.hw.ac.uk#";
		assertEquals(url, scraperCore.fixURL(url2));	
	}		
}
