package hwu.elixir.scrape.scraper;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hwu.elixir.scrape.db.crawl.StateOfCrawl;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;

public class SimpleScraperImplTest {

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
	public void test_scrape_NothingToDo()
			throws HtmlExtractorServiceException, FourZeroFourException, JsonLDInspectionException {
		assertFalse(scraper.scrape("http://www.hw.ac.uk", 100L, "/Volumes/kcm/", StateOfCrawl.SUCCESS));

		assertFalse(scraper.scrape("http://www.hw.ac.uk/", 100L, "/Volumes/kcm/", StateOfCrawl.SUCCESS));

		assertFalse(scraper.scrape("http://www.hw.ac.uk#", 100L, "/Volumes/kcm/", StateOfCrawl.SUCCESS));
	}

}
