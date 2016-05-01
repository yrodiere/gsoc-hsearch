package io.github.mincongh.session;

import java.util.List;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.jboss.logging.Logger;

import io.github.mincongh.entity.Address;

@Stateful
public class AddressSession {
    
    // This particular EntityManager is injected as an EXTENDED persistence
    // context, which simply means that the EntityManager is created when the
    // @Stateful bean is created and destroyed when the @Stateful bean is
    // destroyed. Simply put, the data in the EntityManager is cached for the
    // lifetime of the @Stateful bean
    @PersistenceContext(unitName = "us-address", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;
    
    private Logger logger = Logger.getLogger(this.getClass());
    
    private FullTextEntityManager fullTextEntityManager;
    
    public AddressSession() {
    }

    @SuppressWarnings("unchecked")
    public List<Address> getAddresses() {
        List<Address> addresses = null;
        if (entityManager == null)  {
            logger.warn("entityManager is NULL");
            return null;
        }
        addresses = entityManager
                .createQuery("SELECT a FROM Address a WHERE a.type = :type")
                .setParameter("type", "Rd")
                .setMaxResults(1000)
                .getResultList();
        if (addresses.isEmpty()) {
            logger.warn("No result found.");
        }
        return addresses;
    }
    
    public void index() throws InterruptedException {
        fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        fullTextEntityManager
                .createIndexer(Address.class)
                .batchSizeToLoadObjects(10000)
                .cacheMode(CacheMode.NORMAL)
                .threadsToLoadObjects(4)        // 4 threads
                .transactionTimeout(3600)       // 3600 seconds
                .startAndWait();
    }
}
