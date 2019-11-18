package hwu.elixir.scrape.db.crawl;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

public class CrawlRecordTest {

	@Test
	public void test_getterSetter_shouldWork() {
		CrawlRecord cr = new CrawlRecord("http://www.bbc.co.uk");
		assertTrue(cr.getUrl().equalsIgnoreCase("http://www.bbc.co.uk"));
		
		assertTrue(cr.getStatus().equals(StatusOfScrape.UNTRIED));
		cr.setStatus(StatusOfScrape.DOES_NOT_EXIST);
		assertTrue(cr.getStatus().equals(StatusOfScrape.DOES_NOT_EXIST));
		
		assertTrue(cr.getId() == null);
		cr.setId(1L);
		assertTrue(cr.getId().equals(1L));
		
		assertTrue(cr.getDateScraped() == null);
		cr.setDateScraped(new Date());
		assertTrue(cr.getDateScraped().equals(new Date()));
				
		assertTrue(cr.getContext().equalsIgnoreCase(""));
		cr.setContext("giant");
		assertTrue(cr.getContext().equalsIgnoreCase("giant"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void test_constructor_badURL() {
		new CrawlRecord("banana");
	}
	
	@Test
	public void test_equals() {
		CrawlRecord cr = new CrawlRecord("https://www.bbc.co.uk");
		CrawlRecord cr2 = new CrawlRecord("https://www.bbc.co.uk");
		cr2.setStatus(StatusOfScrape.DOES_NOT_EXIST);
		assertTrue(cr.equals(cr2));
		
		cr2.setDateScraped(new Date());
		assertTrue(cr.equals(cr2));
		
		CrawlRecord cr3 = new CrawlRecord("http://www.bbc.co.uk");
		assertFalse(cr.equals(cr3));
		
		assertTrue(cr.equals(cr));
		
		assertFalse(cr.equals("Hello"));
		
	}

}
