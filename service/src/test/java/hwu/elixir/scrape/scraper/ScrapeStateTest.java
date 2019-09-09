package hwu.elixir.scrape.scraper;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import hwu.elixir.scrape.db.crawl.CrawlRecord;
import hwu.elixir.scrape.db.crawl.StateOfCrawl;

public class ScrapeStateTest {

	private ScrapeState state;
	private ArrayList<CrawlRecord> pagesToBeScraped;
	
	@Before
	public void setUp() throws Exception {
		CrawlRecord record1 = new CrawlRecord("https://www.macs.hw.ac.uk/~kcm");

		pagesToBeScraped = new ArrayList<CrawlRecord>();
		pagesToBeScraped.add(record1);
		state = new ScrapeState(pagesToBeScraped);
	}
	
	
	@Test
	public void test_gettingCrawlRecord() {
		assertEquals(1, state.getNumberPagesLeftToScrape());
		assertTrue(state.pagesLeftToScrape());
		assertEquals(0, state.getPagesProcessed().size());
		
		CrawlRecord record2 = state.getURLToProcess();
		assertEquals(record2.getUrl(), "https://www.macs.hw.ac.uk/~kcm");
		assertFalse(state.pagesLeftToScrape());
		assertEquals(0, state.getNumberPagesLeftToScrape());
	}
	
	@Test
	public void updatingCR() {
		CrawlRecord record2 = state.getURLToProcess();
		assertFalse(state.pagesLeftToScrape());
		state.addSuccessfulScrapedURL(record2);
		assertEquals(1, state.getPagesProcessed().size());
		assertEquals(StateOfCrawl.SUCCESS, record2.getStatus());
	}		

	
	@Test
	public void updatingCRForFailure() {
		CrawlRecord record2 = state.getURLToProcess();
		assertFalse(state.pagesLeftToScrape());
		state.addFailedToScrapeURL(record2);
		assertEquals(1, state.getPagesProcessed().size());
		assertEquals(StateOfCrawl.FAILED, record2.getStatus());
		
		state.addFailedToScrapeURL(record2);
		assertEquals(2, state.getPagesProcessed().size());
		assertEquals(StateOfCrawl.GIVEN_UP, record2.getStatus());
		
		state.addSuccessfulScrapedURL(record2);
		assertEquals(3, state.getPagesProcessed().size());
		assertEquals(StateOfCrawl.SUCCESS, record2.getStatus());	
		
		state.setStatusTo404(record2);
		assertEquals(4, state.getPagesProcessed().size());
		assertEquals(StateOfCrawl.DOES_NOT_EXIST, record2.getStatus());	
		
		state.setStatusToHumanInspection(record2);
		assertEquals(5, state.getPagesProcessed().size());
		assertEquals(StateOfCrawl.HUMAN_INSPECTION, record2.getStatus());			
	}
	
	@Test
	public void test_isEmpty() {
		state.getURLToProcess();
		assertNull(state.getURLToProcess());		
	}
}
