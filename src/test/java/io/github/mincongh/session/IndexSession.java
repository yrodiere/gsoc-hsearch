package io.github.mincongh.session;

import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

import io.github.mincongh.entity.Address;

/**
 * Index Session bean is used for indexing all entities in the databases for
 * hibernate search. This session bean is run in an asynchronous way, which
 * guarantee the main thread is not crashed. 
 * 
 * @author Mincong HUANG
 */
@Stateful
public class IndexSession {
    
    // This particular EntityManager is injected as an EXTENDED persistence
    // context, which simply means that the EntityManager is created when the
    // @Stateful bean is created and destroyed when the @Stateful bean is
    // destroyed. Simply put, the data in the EntityManager is cached for the
    // lifetime of the @Stateful bean
    @PersistenceContext(unitName = "jsr352", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;
    
    private FullTextEntityManager fullTextEntityManager;
    
    public IndexSession() {
    }

    @SuppressWarnings("unchecked")
    public List<Address> getAddresses() {
        return entityManager
            .createQuery("SELECT a FROM Address a WHERE a.type = :type")
            .setParameter("type", "Rd")
            .setMaxResults(1000)
            .getResultList();
    }
    
    // @Asynchronous Used to mark a session bean method as an asynchronous 
    //     method or to designate all business methods of a session bean class 
    //     as asynchronous.
    @Asynchronous
    public void index() throws InterruptedException {
        fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        fullTextEntityManager
            // Delete existing index and rebuild, for all Address instances
            .createIndexer(Address.class)
            // Batch of 10000 objects per query
            .batchSizeToLoadObjects(10000)
            // CacheMode.IGNORE:
            // The session will never interact with the cache, except to 
            // invalidate cache items when updates occur
            .cacheMode(CacheMode.IGNORE)
            // 4 parallel threads to load the Address instances
            // They will also need to process indexed embedded relations and
            // custom FieldBridges to finally output a Lucene document
            .threadsToLoadObjects(4)
            // Time out in 3600 seconds for its transactions
            // these transactions are read-only
            .transactionTimeout(3600)
            // The MassIndexer uses a forward-only scrollable result to iterate
            // on the primary keys to be loaded, but MySQL’s JDBC driver will
            // load all values in memory. To avoid this "optimization", set 
            // idFetchSize to Integer.MIN_VALUE.
            .idFetchSize(Integer.MIN_VALUE)
            // run in asynchronous way
            .start();
    }
}
