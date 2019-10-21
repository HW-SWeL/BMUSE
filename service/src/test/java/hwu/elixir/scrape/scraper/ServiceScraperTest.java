package hwu.elixir.scrape.scraper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hwu.elixir.scrape.db.crawl.StateOfCrawl;
import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;

public class ServiceScraperTest {

	private ServiceScraper scraper;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		scraper = new ServiceScraper();
	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void test_wscrape_doSomething() throws FourZeroFourException, JsonLDInspectionException, CannotWriteException {
		assertNotEquals("", scraper.scrape("http://www.hw.ac.uk", 100L, "/Users/kcm/", StateOfCrawl.FAILED));
		assertNotEquals("", scraper.scrape("http://www.hw.ac.uk", 100L, "/Users/kcm/", StateOfCrawl.UNTRIED));
	}
	
	@Test
	public void test_scrape_NothingToDo()
			throws FourZeroFourException, JsonLDInspectionException, CannotWriteException {
		assertEquals("", scraper.scrape("http://www.hw.ac.uk", 100L, "/Users/kcm/", StateOfCrawl.SUCCESS));

		assertEquals("", scraper.scrape("http://www.hw.ac.uk/", 100L, "/Users/kcm/", StateOfCrawl.GIVEN_UP));

		assertEquals("", scraper.scrape("http://www.hw.ac.uk#", 100L, "/Users/kcm/", StateOfCrawl.DOES_NOT_EXIST));
		
		assertEquals("", scraper.scrape("http://www.hw.ac.uk#", 100L, "/Users/kcm/", StateOfCrawl.HUMAN_INSPECTION));
	}

}
