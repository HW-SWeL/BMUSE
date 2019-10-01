package hwu.elixir.scrape.db.crawl;

import java.util.List;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Manages database access through CrawlRecord. Based on Hibernate, but using
 * standard JPA
 * 
 * @see CrawlRecord
 * @author kcm
 */
public class DBAccess {

	private EntityManager em;
	private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate");

	private Logger logger = LoggerFactory.getLogger(DBAccess.class);

	public DBAccess() {
		java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);
		open();
	}

	private void open() {
		em = emf.createEntityManager();
	}

	public void close() {
		if (em.isOpen()) {
			em.clear();
			em.close();
		}
	}

	public void shutdown() {
		close();
		emf.close();
	}

	/**
	 * Given a list of URIs, creates a CrawlRecord for each which is synced to database. 
	 * @param allURIs list of URIs

	 */
	public boolean addAllURIsIntoACrawlRecord(List<String> allURIs) {
		if (!em.isOpen()) {
			open();
		}

		System.out.println("Adding " + allURIs.size() + " records");
		int counter = 0;
		EntityTransaction txn = null;
		try {
			txn = em.getTransaction();
			txn.begin();
			for (String uri : allURIs) {
				if (!crawlRecordExists(uri)) {
					CrawlRecord temp = new CrawlRecord(uri);
					temp.setStatus(StateOfCrawl.UNTRIED);
					em.persist(temp);
					if (counter++ % 20 == 0) {
						em.flush();
						em.clear();
					}
				}
			}
			txn.commit();
		} catch (Exception e) {
			if (txn != null)
				txn.commit();

			logger.error("    ERROR in DBAccess: " + e.getLocalizedMessage());
			return false;
		}
		logger.info("Written " + counter + " records to CrawlRecord table");
		return true;
	}

	/**
	 * Syncs the local copy of the CrawlRecord with the 1 in the database
	 * 
	 * @param allRecords List of CrawlRecord objects to sync
	 * @return true if worked else false
	 * @see CrawlRecord
	 */
	public boolean updateAllCrawlRecords(List<CrawlRecord> allRecords) {
		if (!em.isOpen()) {
			open();
		}

		System.out.println("Updating " + allRecords.size() + " records");
		int counter = 0;
		EntityTransaction txn = null;
		try {
			txn = em.getTransaction();
			txn.begin();

			for (CrawlRecord record : allRecords) {
				CrawlRecord retrievedRecord = findCrawlRecordById(record.getId());
				if (retrievedRecord == null) {
					throw new Exception("Cannot find record with id " + record.getId());
				}
				if (record.getContext() != null) {
					retrievedRecord.setContext(record.getContext());
				}
				if (record.getDateScraped() != null) {
					retrievedRecord.setDateScraped(record.getDateScraped());
				}
				if (record.getStatus() != null) {
					retrievedRecord.setStatus(record.getStatus());
				}
				if(record.isBeingScraped()) {
					retrievedRecord.setBeingScraped(false);
				} else {
					System.out.println("RECORD IS BEING UPDATED AFTER CRAWL YET WAS NOT SET TO BEING CRAWLED!");		
					System.exit(0);
				}
				
				em.persist(retrievedRecord);
				if (counter++ % 20 == 0) {
					em.flush();
					em.clear();
				}
			}
			txn.commit();
		} catch (Exception e) {
			if (txn != null)
				txn.commit();

			logger.error("    ERROR in DBAccess: " + e.getLocalizedMessage());
			return false;
		}
		logger.info("Updated " + counter + " records to CrawlRecord table");

		return true;
	}

	/**
	 * Retrieves all instances of CrawlRecord from DBMS. Each CrawlRecord is a URL that needs to be scraped, with provenance if scraped.
	 * 
	 * @return All CrawlRecords in DBMS. Or null if not possible.
	 * @see CrawlRecord 
	 */
	public List<CrawlRecord> getAllCrawlRecords() {
		if (!em.isOpen()) {
			open();
		}
		try {
			String queryString = "FROM CrawlRecord";
			Query query = em.createQuery(queryString);
			List<CrawlRecord> allRecords = query.getResultList();			
			
			return allRecords;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Retrieves a given number of instances of CrawlRecord from DBMS. Each CrawlRecord is a URL that needs to be scraped, with provenance if scraped.
	 * 
	 * @param number of CrawlRecords to fetch
	 * @return *amount* number of CrawlRecords from DBMS. Or null if not possible.
	 * @see CrawlRecord
	 */
	public List<CrawlRecord> getSomeCrawlRecords(int amount) {
		if (!em.isOpen()) {
			open();
		}
		
		EntityTransaction txn = null;
		try {
			txn = em.getTransaction();
			txn.begin();		
			
			String queryString = "FROM CrawlRecord WHERE status NOT IN ('GIVEN_UP', 'SUCCESS', 'DOES_NOT_EXIST', 'HUMAN_INSPECTION') and beingScraped != 1";
			Query query = em.createQuery(queryString);
			query.setMaxResults(amount);
			List<CrawlRecord> allRecords = query.getResultList();			
			
			for(CrawlRecord record : allRecords) {				
				record.setBeingScraped(true);
				em.persist(record);
			}			
			
			txn.commit();			
			return allRecords;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Retrieves a specific instance of CrawlRecord from DBMS.
	 * 
	 * @param id Unique ID of CrawlRecord to fetch. IDs are auto-generated by DBMS.
	 * @return The CrawlRecord or null if not found/error.
	 * @see CrawlRecord 
	 */
	public CrawlRecord findCrawlRecordById(Long id) {
		if (!em.isOpen()) {
			open();
		}

		try {
			String queryString = "FROM CrawlRecord WHERE id = " + id;
			Query query = em.createQuery(queryString);
			List<CrawlRecord> allRecords = query.getResultList();
			return allRecords.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Retrieves a specific instance of CrawlRecord from DBMS.
	 * 
	 * @param url The URL of the CrawlRecord.
	 * @return The CrawlRecord or null if not found/error.
	 * @see CrawlRecord  
	 */
	public CrawlRecord findCrawlRecordByURL(String url) {
		if (!em.isOpen()) {
			open();
		}

		try {
			String queryString = "FROM CrawlRecord WHERE url LIKE '" + url + "'";
			Query query = em.createQuery(queryString);
			List<CrawlRecord> allRecords = query.getResultList();
			return allRecords.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Determines if a CrawlRecord for a given URL already exists.
	 * 
	 * @param url
	 * @return true if yes, false if no/error
	 */
	public boolean crawlRecordExists(String url) {
		if (!em.isOpen()) {
			open();
		}

		try {
			String queryString = "FROM CrawlRecord WHERE url LIKE '" + url + "'";
			Query query = em.createQuery(queryString);
			List<CrawlRecord> allRecords = query.getResultList();
			if (allRecords.size() > 0)
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
