package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

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
    @Inject @BatchProperty(name = "entityType") private String entityTypeStr;
    
    @Inject
    private JobContext jobContext;
    
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
        
        // get entity class type
        Class<?> entityClazz = Class.forName(entityTypeStr);
        
        // unwrap session from entity manager
        session = em.unwrap(Session.class);
        
        // get total number of id
        long rowCount = (long) session
            .createCriteria(entityClazz)
            .setProjection(Projections.rowCount())
            .setCacheable(false)
            .uniqueResult();
        System.out.printf("entityType = %s (%d rows).%n", entityTypeStr, rowCount);
        jobContext.setTransientUserData(rowCount);
        
        // load ids and store in scrollable results
        ScrollableResults scrollableIds = session
            .createCriteria(entityClazz)
            .setCacheable(false)
            .setFetchSize(fetchSize)
            .setProjection(Projections.id())
            .setMaxResults(maxResults)
            .scroll(ScrollMode.FORWARD_ONLY);

        Serializable[] entityIDs = new Serializable[arrayCapacity];
        long row = 0;
        int i = 0;
        try {
            // create (K, V) pair in the hash-map embedded
            // indexing context
            indexingContext.createQueue(entityClazz);
            
            while (scrollableIds.next() && row < rowCount) {
                Serializable id = (Serializable) scrollableIds.get(0);
                entityIDs[i++] = id;
                if (i == arrayCapacity) {
                    // add array entityIDs into indexing context
                    // (mapped to key K=entityClazz
                    indexingContext.add(entityIDs, entityClazz);
                    // reset id array and index
                    entityIDs = new Serializable[arrayCapacity];
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
