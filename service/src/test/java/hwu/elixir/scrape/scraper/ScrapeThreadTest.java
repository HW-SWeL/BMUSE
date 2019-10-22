package hwu.elixir.scrape.scraper;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hwu.elixir.scrape.db.crawl.CrawlRecord;
import hwu.elixir.scrape.db.crawl.StateOfCrawl;
import hwu.elixir.scrape.exceptions.CannotWriteException;
import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;

public class ScrapeThreadTest {
	
	@Mock
	ScrapeState state;
	
	@Mock
	ServiceScraper scraper;
	

	CrawlRecord record;
	
	private static String outputLoction  = System.getProperty("user.home")+File.separator+"toDelete";
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File outputFolder = new File(outputLoction);
		boolean result = outputFolder.mkdir();
		if(!result) {
			throw new Exception("Cannot create output folder for temporary files used during ScrapeThreadTest!");
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
			throw new Exception("Cannot delete output folder for temporary files used during ScrapeThreadTest!");
		}		
	}
	

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		record = new CrawlRecord("http://www.abc.com");
		record.setId(1L);
	}


	@Test
	public void test_noURLtoCrawl() throws NoSuchFieldException, SecurityException, FourZeroFourException {
		ScrapeThread thread = new ScrapeThread(scraper, state, 1, outputLoction);
		
		when(state.pagesLeftToScrape()).thenReturn(true);
		when(state.getURLToProcess()).thenReturn(null);		

		thread.run();

		verify(state).pagesLeftToScrape();
		verify(state).getURLToProcess();
	}
	
	@Test
	public void test_uRLtoCrawl_butFails() throws NoSuchFieldException, SecurityException, FourZeroFourException, JsonLDInspectionException, CannotWriteException {
		when(state.pagesLeftToScrape()).thenReturn(true, false);
		when(state.getURLToProcess()).thenReturn(record);
		when(scraper.scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED)).thenReturn(false);

		ScrapeThread thread = new ScrapeThread(scraper, state, 1, outputLoction);
		thread.run();
		
		
		verify(state, times(2)).pagesLeftToScrape();
		verify(state).getURLToProcess();		
		verify(scraper).scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED);
		verify(state).addFailedToScrapeURL(record);
	}

	@Test
	public void test_uRLtoCrawl_works() throws FourZeroFourException, JsonLDInspectionException, CannotWriteException {

		when(state.pagesLeftToScrape()).thenReturn(true, false);
		when(state.getURLToProcess()).thenReturn(record);
		when(scraper.scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED)).thenReturn(true);

		ScrapeThread thread = new ScrapeThread(scraper, state, 1, outputLoction);
		thread.run();
		
		
		verify(state, times(2)).pagesLeftToScrape();
		verify(state).getURLToProcess();		
		verify(scraper).scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED);
		verify(state).addSuccessfulScrapedURL(record);
	}
	
	
	@Test
	public void test_uRLtoCrawl_throwsException() throws FourZeroFourException, JsonLDInspectionException, CannotWriteException {
		
		when(state.pagesLeftToScrape()).thenReturn(true, false);
		when(state.getURLToProcess()).thenReturn(record);
		when(scraper.scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED)).thenThrow(FourZeroFourException.class);

		ScrapeThread thread = new ScrapeThread(scraper, state, 1, outputLoction);
		thread.run();
		
		
		verify(state, times(2)).pagesLeftToScrape();
		verify(state).getURLToProcess();		
		verify(scraper).scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED);
	}	
	
	@Test
	public void test_uRLtoCrawl_throws404() throws FourZeroFourException, JsonLDInspectionException, CannotWriteException {

		when(state.pagesLeftToScrape()).thenReturn(true, false);
		when(state.getURLToProcess()).thenReturn(record);
		when(scraper.scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED)).thenThrow(FourZeroFourException.class);

		ScrapeThread thread = new ScrapeThread(scraper, state, 1, outputLoction);
		thread.run();
		
		
		verify(state, times(2)).pagesLeftToScrape();
		verify(state).getURLToProcess();		
		verify(scraper).scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED);
		verify(state).setStatusTo404(record);
	}	
	
	@Test
	public void test_uRLtoCrawl_throwsInsepection() throws FourZeroFourException, JsonLDInspectionException, CannotWriteException {

		when(state.pagesLeftToScrape()).thenReturn(true, false);
		when(state.getURLToProcess()).thenReturn(record);
		when(scraper.scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED)).thenThrow(JsonLDInspectionException.class);

		ScrapeThread thread = new ScrapeThread(scraper, state, 1, outputLoction);
		thread.run();
		
		
		verify(state, times(2)).pagesLeftToScrape();
		verify(state).getURLToProcess();		
		verify(scraper).scrape("http://www.abc.com", 1L, outputLoction, StateOfCrawl.UNTRIED);
		verify(state).setStatusToHumanInspection(record);
	}		
}
