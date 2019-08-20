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

public class DBAccess {

	private EntityManager em;
	private static EntityManagerFactory emf  = Persistence.createEntityManagerFactory("hibernate");

	private Logger logger = LoggerFactory.getLogger(DBAccess.class);

	public DBAccess() {
		java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);
		open();
	}
	
	private void open() {		
		em = emf.createEntityManager();		
	}

	public void close() {
		if(em.isOpen()) {
			em.clear();
			em.close();
		}
	}
	
	public void shutdown() {
		close();
		emf.close();
	}

	public boolean addAllURIsIntoACrawlRecord(List<String> allURIs) {
		if(!em.isOpen()) {
			open(); 
		}
		
		System.out.println("Adding "+allURIs.size()+" records");
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
			if(txn != null)
				txn.commit();
			
			logger.error("    ERROR in DBAccess: " + e.getLocalizedMessage());
			return false;
		}
		logger.info("Written " + counter + " records to CrawlRecord table");
		return true;
	}
	
	public boolean updateAllCrawlRecords(List<CrawlRecord> allRecords) {
		if(!em.isOpen()) {
			open(); 
		}		
		
		System.out.println("Updating "+allRecords.size()+" records");
		int counter = 0;
		EntityTransaction txn = null;
		try {
			txn = em.getTransaction();
			txn.begin();
			
			for(CrawlRecord record : allRecords) {
				CrawlRecord retrievedRecord = findCrawlRecordById(record.getId());
				if(retrievedRecord == null) {
					throw new Exception("Cannot find record with id " + record.getId());
				}
				if(record.getContext() != null) {
					retrievedRecord.setContext(record.getContext());
				}
				if(record.getDateScraped() != null) {
					retrievedRecord.setDateScraped(record.getDateScraped());
				}
				if(record.getStatus() != null) {
					retrievedRecord.setStatus(record.getStatus());
				}
				em.persist(retrievedRecord);
				if (counter++ % 20 == 0) {
					em.flush();
					em.clear();
				}
			}
			txn.commit();
		} catch (Exception e) {
			if(txn != null)
				txn.commit();	
			
			logger.error("    ERROR in DBAccess: " + e.getLocalizedMessage());
			return false;
		}
		logger.info("Updated " + counter + " records to CrawlRecord table");
		
		return true;
	}
	

	public List<CrawlRecord> getAllCrawlRecords() {
		if(!em.isOpen()) {
			open(); 
		}
		
		try {
			String queryString = "FROM CrawlRecord";
//			logger.info(queryString);
			Query query = em.createQuery(queryString);
			List<CrawlRecord> allRecords = query.getResultList();
			return allRecords;
		} catch (Exception e) {
			return null;
		}
	}
	
	public List<CrawlRecord> getSomeCrawlRecords(int amount) {
		if(!em.isOpen()) {
			open(); 
		}
		
		try {
			String queryString = "FROM CrawlRecord WHERE status NOT IN ('GIVEN_UP', 'SUCCESS', 'DOES_NOT_EXIST') ";
//			logger.info(queryString);			
			Query query = em.createQuery(queryString);
			query.setMaxResults(amount);
			List<CrawlRecord> allRecords = query.getResultList();
			return allRecords;
		} catch (Exception e) {
			return null;
		}
	}	
	

	public CrawlRecord findCrawlRecordById(Long id) {
		if(!em.isOpen()) {
			open(); 
		}		
		
		try {
			String queryString = "FROM CrawlRecord WHERE id = " + id;
//			logger.info(queryString);
			Query query = em.createQuery(queryString);
			List<CrawlRecord> allRecords = query.getResultList();
			return allRecords.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public CrawlRecord findCrawlRecordByURL(String url) {
		if(!em.isOpen()) {
			open(); 
		}
		
		try {
			String queryString = "FROM CrawlRecord WHERE url LIKE '" + url + "'";
//			logger.info(queryString);
			Query query = em.createQuery(queryString);
			List<CrawlRecord> allRecords = query.getResultList();
			return allRecords.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public boolean crawlRecordExists(String url) {
		if(!em.isOpen()) {
			open(); 
		}		
		
		try {
			String queryString = "FROM CrawlRecord WHERE url LIKE '" + url + "'";
//			logger.info(queryString);
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
