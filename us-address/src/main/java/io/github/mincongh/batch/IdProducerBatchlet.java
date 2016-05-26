package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.batch.api.Batchlet;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import io.github.mincongh.entity.Address;

/**
 * Read identifiers of entities via entity manager. The result is going to be
 * stored then be used for Lucene document production.
 * 
 * @author Mincong HUANG
 */
@Named
public class IdProducerBatchlet implements Batchlet {

    private final int BATCH_SIZE = 500;
    private final int FETCH_SIZE = 1000;
    private final int MAX_RESULTS = 10000;
    
    @PersistenceContext(unitName = "us-address")
    private EntityManager em;
    private Session session;
    
    /**
     * Load id of all target entities using Hibernate Session. In order to 
     * follow the id loading progress, the total number will be additionally 
     * computed as well.
     */
    @Override
    public String process() throws Exception {
        
        session = em.unwrap(Session.class);
        
        // get total number of id
        long totalCount = (long) session
            .createCriteria(Address.class)
            .setProjection(Projections.rowCount())
            .setCacheable(false)
            .uniqueResult();
        System.out.printf("Total count = %d%n", totalCount);
        
        // load ids
        Criteria criteria = session
            .createCriteria(Address.class)
            .setCacheable(false)
            .setFetchSize(FETCH_SIZE)
            .setMaxResults(MAX_RESULTS);
        ScrollableResults results = criteria.scroll(ScrollMode.FORWARD_ONLY);
        List<Serializable> ids = new ArrayList<>(BATCH_SIZE);
        long count = 0;
        try {
            while (results.next()) {
                Serializable id = (Serializable) results.get(0);
                ids.add(id);
                count++;
                if (ids.size() == BATCH_SIZE) {
                    System.out.printf("Batch size reached. Printing ids ...%n");
                    for (Serializable i : ids) {
                        System.out.println(i);
                    }
                    System.out.printf("%n");
                }
//                if (count == totalCount) {
//                    break;
//                }
            }
        } finally {
            results.close();
        }
        return null;
    }

    @Override
    public void stop() throws Exception {
        if (session.isOpen()) {
            session.close();
        }
    }
}
