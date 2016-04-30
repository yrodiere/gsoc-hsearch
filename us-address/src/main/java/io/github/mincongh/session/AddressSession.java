package io.github.mincongh.session;

import java.util.List;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.jboss.logging.Logger;

import io.github.mincongh.entity.Address;

@Stateful
public class AddressSession {

//    @PersistenceContext(unitName = "us-address")
//    protected EntityManagerFactory entityManagerFactory;
    
    @PersistenceContext(unitName = "us-address", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;
    private Logger logger = Logger.getLogger(this.getClass());
    
    public AddressSession() {
    }

//    public EntityManagerFactory getEntityManagerFactory() {
//        return entityManagerFactory;
//    }
    
//    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
//        this.entityManagerFactory = entityManagerFactory;
//    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Address> getAddresses() {
        List<Address> addresses = null;
        if (entityManager == null)  {
            logger.warn("entityManager is NULL");
            return null;
        }
//        // only for RESOURCE_LOCAL
//        entityManager.getTransaction().begin();
        addresses = entityManager
                .createQuery("SELECT a FROM Address a WHERE a.type = :type")
                .setParameter("type", "Rd")
                .setMaxResults(1000)
                .getResultList();
//        // only for RESOURCE_LOCAL
//        entityManager.getTransaction().commit();
//        entityManager.close();
        if (addresses.isEmpty()) {
            logger.warn("No result found.");
        } else {
            logger.info(addresses.size() + " results found.");
        }
        return addresses;
    }
}
