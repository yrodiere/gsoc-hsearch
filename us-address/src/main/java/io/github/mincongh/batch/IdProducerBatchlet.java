package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import io.github.mincongh.entity.Address;

/**
 * Read identifiers of entities via entity manager. The result is going to be
 * stored in {@code IndexingContext}, then be used for Lucene document 
 * production in the next step.
 * 
 * @author Mincong HUANG
 */
@Named
public class IdProducerBatchlet implements Batchlet {

    @Inject @BatchProperty private int arrayCapacity;
    @Inject @BatchProperty private int fetchSize;
    @Inject @BatchProperty private int maxResults;
    
    @Inject
    private IndexingContext indexingContext;
    
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
        
        // unwrap session from entity manager
        session = em.unwrap(Session.class);
        
        // get total number of id
        long rowCount = (long) session
            .createCriteria(Address.class)
            .setProjection(Projections.rowCount())
            .setCacheable(false)
            .uniqueResult();
        System.out.printf("Total row = %d%n", rowCount);
        
        // load ids and store in scrollable results
        ScrollableResults scrollableIds = session
            .createCriteria(Address.class)
            .setCacheable(false)
            .setFetchSize(fetchSize)
            .setProjection(Projections.id())
            .setMaxResults(maxResults)
            .scroll(ScrollMode.FORWARD_ONLY);

        Serializable[] ids = new Serializable[arrayCapacity];
        long row = 0;
        int i = 0;
        try {
            while (scrollableIds.next() && row < rowCount) {
                Serializable id = (Serializable) scrollableIds.get(0);
                ids[i++] = id;
                if (i == arrayCapacity) {
                    for (Serializable _id : ids) {
                        System.out.printf("%5d ", _id);
                    }
                    System.out.printf("%n");
                    indexingContext.add(ids);
                    // reset id array and index
                    ids = new Serializable[arrayCapacity];
                    i = 0;
                }
                row++;
            }
        } finally {
            scrollableIds.close();
        }
        return BatchStatus.COMPLETED.toString();
    }
    
    @Override
    public void stop() throws Exception {
        if (session.isOpen()) {
            session.close();
        }
    }
}
