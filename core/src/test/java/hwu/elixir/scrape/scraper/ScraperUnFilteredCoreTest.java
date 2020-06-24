package hwu.elixir.scrape.scraper;

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
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.MissingMarkupException;
import hwu.elixir.scrape.exceptions.NTriplesParsingException;
import hwu.elixir.scrape.exceptions.SeleniumException;
import hwu.elixir.utils.CompareNQ;

public class ScraperUnFilteredCoreTest {
	
	private static ScraperUnFilteredCore scraperCore;
	private static File testHtml;
	private static String outputLoction  = System.getProperty("user.home")+File.separator+"toDelete"+File.separator;	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File outputFolder = new File(outputLoction);
		boolean result = outputFolder.mkdir();
		if(!result) {
			throw new Exception("Cannot create output folder for temporary files used during ScraperCoreTest!");
		}		
		
		scraperCore = new ScraperUnFilteredCore();		
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
	public void test_getOnlyJSONLD() throws FourZeroFourException, SeleniumException {
		ScraperUnFilteredCore scraperCore = new ScraperUnFilteredCore();
		
		String[] allJsonLD = scraperCore.getOnlyUnfilteredJSONLDFromUrl("https://www.macs.hw.ac.uk/SWeL/BMUSE/tests/20191024203151_MACS.htm");
		assertTrue(allJsonLD.length == 3);
		for (String json : allJsonLD) {
			json = json.trim();
			assertTrue(json.startsWith("{"));
			assertTrue(json.endsWith("}"));
			assertTrue(json.contains("@context"));
			assertTrue(json.contains("schema.org"));
		}
	}
	
	//
	
	@Test
	public void test_processTriplesLeaveBlankNodes_chembl() throws NTriplesParsingException {
		ScraperUnFilteredCore scraperCore = new ScraperUnFilteredCore();
		
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
		
		String resourceName = "testRDF/chemblUnfiltered.nq";
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
	public void test_scrapeUnfilteredMarkupAsNTriplesFromUrl() throws FourZeroFourException, MissingHTMLException, MissingMarkupException, NTriplesParsingException {
		ScraperUnFilteredCore scraperCore = new ScraperUnFilteredCore();
		
		String outputNQ = scraperCore.scrapeUnfilteredMarkupAsNTriplesFromUrl("https://raw.githubusercontent.com/HW-SWeL/Scraper/master/core/src/test/resources/testHtml/chembl.html");
		try (PrintWriter out = new PrintWriter(new FileWriter(new File(outputLoction+"rawChembl.nq")))) {
			out.print(outputNQ);
		} catch (IOException e1) {
			e1.printStackTrace();
			fail();
		}
		
		File liveQuads = new File(outputLoction+"rawChembl.nq");
		
		String resourceName = "testRDF/chemblUnFilteredFromGitHub.nq";
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
	
	@Ignore
	@Test 
	public void test_scrapeUnfilteredMarkupAsJsonLdsFromUrl() throws FourZeroFourException, MissingHTMLException, MissingMarkupException, NTriplesParsingException {
		ScraperUnFilteredCore scraperCore = new ScraperUnFilteredCore();
		
		String outputNQ = scraperCore.scrapeUnfilteredMarkupAsJsonLDFromUrl("https://raw.githubusercontent.com/HW-SWeL/Scraper/master/core/src/test/resources/testHtml/chembl.html");
		try (PrintWriter out = new PrintWriter(new FileWriter(new File(outputLoction+"rawChembl.jsonld")))) {
			out.print(outputNQ);
		} catch (IOException e1) {
			e1.printStackTrace();
			fail();
		}
		
		// how to compare jsonld?
	}	
	
}
