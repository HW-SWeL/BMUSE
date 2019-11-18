package hwu.elixir.scrape.db.crawl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DBAccessTest {
	
	private static DBAccess dba;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		dba = new DBAccess();
		
		ArrayList<String> allURIs = new ArrayList<String>();
		allURIs.add("http://www.hw.ac.uk");
		allURIs.add("http://www.macs.hw.ac.uk");
		dba.addAllURIsIntoACrawlRecord(allURIs);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		dba.shutdown();
		System.out.println("FINISHED!");
	}

	@Test
	public void test_addAllURIsIntoACrawlRecord() {
		assertEquals(2, dba.getAllCrawlRecords().size());
	}
	
	@Test
	public void test_crawlRecordExists() {
		assertFalse(dba.crawlRecordExists("http://www.12345.com"));
	}
	
	@Test
	public void test_addURLAlreadyThere() {
		ArrayList<String> allURIs = new ArrayList<String>();
		allURIs.add("http://www.hw.ac.uk");
		dba.addAllURIsIntoACrawlRecord(allURIs);
		assertEquals(2, dba.getAllCrawlRecords().size());
	}

	@Test
	public void test_getSomeCrawlRecords_plus_beingScraped() {
		List<CrawlRecord> list = dba.getSomeCrawlRecords(1);
		assertTrue(list.size() == 1);		
		assertEquals(StatusOfScrape.UNTRIED, list.get(0).getStatus());
		assertEquals(true, list.get(0).isBeingScraped());
		
		dba.resetBeingScraped(list);
		CrawlRecord one = dba.findCrawlRecordById(1L);
		assertEquals("http://www.hw.ac.uk", one.getUrl());
		assertEquals(false, one.isBeingScraped());
	}
	
	@Test
	public void test_getAllCrawlRecords() {
		List<CrawlRecord> list = dba.getAllCrawlRecords();
		assertTrue(list.size() == 2);		
		assertEquals(StatusOfScrape.UNTRIED, list.get(0).getStatus());		
	}	
	
	@Test
	public void test_findCrawlRecordById_works() {
		CrawlRecord one = dba.findCrawlRecordById(1L);
		assertTrue(one.getId() == 1L);
		assertEquals("http://www.hw.ac.uk", one.getUrl());
		assertEquals(StatusOfScrape.UNTRIED, one.getStatus());
	}
	
	@Test
	public void test_findCrawlRecordById_fails() {
		assertNull(dba.findCrawlRecordById(10L));
	
	}	
	
	@Test
	public void test_findCrawlRecordByURL_works() {
		CrawlRecord one = dba.findCrawlRecordByURL("http://www.macs.hw.ac.uk");
		assertTrue(one.getId() == 2L);
		assertEquals("http://www.macs.hw.ac.uk", one.getUrl());
		assertEquals(StatusOfScrape.UNTRIED, one.getStatus());
	}
	
	@Test
	public void test_findCrawlRecordByURL_fails() {
		assertNull(dba.findCrawlRecordByURL("http://www.bbc.co.uk"));
	
	}		
	
	@Test 
	public void test_updateAllRecords() {
		List<CrawlRecord> list = dba.getAllCrawlRecords();
		for(CrawlRecord record : list) {
			record.setDateScraped(new Date());
			record.setStatus(StatusOfScrape.SUCCESS);
			record.setContext("the context");
		}
		dba.updateAllCrawlRecords(list);
		
		CrawlRecord one = dba.findCrawlRecordById(1L);
		assertTrue(one.getId() == 1L);
		assertEquals("http://www.hw.ac.uk", one.getUrl());
		assertEquals(StatusOfScrape.SUCCESS, one.getStatus());
		
		list = dba.getAllCrawlRecords();
		for(CrawlRecord record : list) {
			record.setDateScraped(null);
			record.setStatus(StatusOfScrape.UNTRIED);
			record.setContext("");
		}
		dba.updateAllCrawlRecords(list);
		
	}
	
	@Test
	public void test_updateAllRecords_fail() {
		CrawlRecord one = new CrawlRecord("http://www.bbc.co.uk");
		one.setStatus(StatusOfScrape.DOES_NOT_EXIST);
		List<CrawlRecord> list = new ArrayList<CrawlRecord>();
		list.add(one);
		try {
			dba.updateAllCrawlRecords(list);
		} catch(Exception e) {
			assertEquals("Cannot find record with id null", e.getMessage());			
		}
	}
}
