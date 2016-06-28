package org.hibernate.search.jsr352.se;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.MassIndexer;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.hibernate.search.jsr352.se.test.Company;
import org.jberet.operations.JobOperatorImpl;
import org.jboss.logging.Logger;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MassIndexerIT {

    private EntityManagerFactory emf;
    private EntityManager em;
    
    private JobOperator jobOperator = null;
    
    private final Company COMPANY_1 = new Company("Google");
    private final Company COMPANY_2 = new Company("Red Hat");
    private final Company COMPANY_3 = new Company("Microsoft");
    
    private static final Logger logger = Logger.getLogger(MassIndexerIT.class);
    
    @Before
    public void setupJPA() {
        
//      if (jobOperator == null) {
//          jobOperator = (JobOperatorImpl) BatchRuntime.getJobOperator();
//      }
        
        emf = Persistence.createEntityManagerFactory("h2");
        em = emf.createEntityManager();
        
        em.getTransaction().begin();
        em.persist(COMPANY_1);
        em.persist(COMPANY_2);
        em.persist(COMPANY_3);
        em.getTransaction().commit();
    }

    @Test
    public void testJPA() {
        Company google = em.find(Company.class, 1L);
        Company redhat = em.find(Company.class, 2L);
        Company microsoft = em.find(Company.class, 3L);
        assertEquals(COMPANY_1.getName(), google.getName());
        assertEquals(COMPANY_2.getName(), redhat.getName());
        assertEquals(COMPANY_3.getName(), microsoft.getName());
    }
    
    /**
     * Index strategy should be set to "manual" in JPA configuration file
     * persistence.xml
     * 
     */
    @Test
    public void testSearch() {
        
        logger.infof("finding company called %s ...", "google");
        List<Company> companies = findCompanyByName("google");
        assertEquals(0, companies.size());
        
        indexCompany();
        
        companies = findCompanyByName("google");
        assertEquals(1, companies.size());
    }
    
    private List<Company> findCompanyByName(String name) {
        FullTextEntityManager ftem = Search.getFullTextEntityManager(em);
        Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
                .forEntity(Company.class).get()
                    .keyword().onField("name").matching(name)
                .createQuery();
        @SuppressWarnings("unchecked")
        List<Company> result = ftem.createFullTextQuery(luceneQuery).getResultList();
        return result;
    }
    
    private void indexCompany() {
//        issue #63
//        java.util.ServiceConfigurationError: javax.batch.operations.JobOperator: Provider org.jberet.operations.JobOperatorImpl could not be instantiated
//                at org.jboss.weld.environment.se.WeldContainer.initialize(WeldContainer.java:136)
//                at org.jboss.weld.environment.se.Weld.initialize(Weld.java:589)
//                ...
//        Set<Class<?>> rootEntities = new HashSet<>();
//        rootEntities.add(Company.class);
//        // org.hibernate.search.jsr352.MassIndexer
//        MassIndexer massIndexer = new MassIndexerImpl().rootEntities(rootEntities);
//        long executionId = massIndexer.start();
//        logger.infof("job execution id = %d", executionId);
        try {
            Search.getFullTextEntityManager( em )
                .createIndexer()
                .batchSizeToLoadObjects( 1 )
                .threadsToLoadObjects( 1 )
                .transactionTimeout( 10 )
                .cacheMode( CacheMode.IGNORE )
                .startAndWait();
        }
        catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
    }
    
    @After
    public void shutdownJPA() {
        em.close();
        emf.close();
    }
}
