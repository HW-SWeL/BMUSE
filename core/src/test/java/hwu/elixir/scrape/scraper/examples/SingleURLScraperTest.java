package hwu.elixir.scrape.scraper.examples;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SingleURLScraperTest {
	
	private static String outputFolder = System.getProperty("user.home");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void test_scrapeASingleURL() {
		String fileName = "resultOfScrape";
		SingleURLScraper scraper = new SingleURLScraper();
		scraper.scrapeASingleURL("https://www.macs.hw.ac.uk", fileName);
		
		File outputFile = new File(outputFolder + File.separator + fileName+".nq");
		assertTrue(outputFile.exists());
		
		String fileContent = "";
		try(BufferedReader in = new BufferedReader(new FileReader(outputFile))) {
			String line = "";
			while((line = in.readLine())!= null) {
				fileContent += line;
			}
		} catch (FileNotFoundException e) {		
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		
		assertTrue(fileContent.contains("<https://www.hw.ac.uk/schools/mathematical-computer-sciences.htm> <https://schema.org/name> \"Mathematical and Computer Sciences\""));
		assertTrue(fileContent.contains("<https://www.macs.hw.ac.uk> <http://purl.org/dc/terms/title> \"Mathematical and Computer Sciences | Heriot-Watt University\""));
		
	}
}
