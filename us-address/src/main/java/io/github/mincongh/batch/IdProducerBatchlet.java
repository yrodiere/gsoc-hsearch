package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
 * stored then be used for Lucene document production.
 * 
 * @author Mincong HUANG
 */
@Named
public class IdProducerBatchlet implements Batchlet {

    @Inject
    @BatchProperty
    private int listCapacity;
    
    @Inject
    @BatchProperty
    private int fetchSize;
    
    @Inject
    @BatchProperty
    private int maxResults;
    
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
        long totalCount = (long) session
            .createCriteria(Address.class)
            .setProjection(Projections.rowCount())
            .setCacheable(false)
            .uniqueResult();
        System.out.printf("Total count = %d%n", totalCount);
        
        // load ids and store in scrollable results
        ScrollableResults scrollableIds = session
            .createCriteria(Address.class)
            .setCacheable(false)
            .setFetchSize(fetchSize)
            .setProjection(Projections.id())
            .setMaxResults(maxResults)
            .scroll(ScrollMode.FORWARD_ONLY);
        
        List<Serializable> ids = new ArrayList<>(listCapacity);
        long count = 0;
        try {
            while (scrollableIds.next()) {
                Serializable id = (Serializable) scrollableIds.get(0);
                ids.add(id);
                if (ids.size() == listCapacity) {
                    for (Serializable i : ids) {
                        System.out.printf("%d ", i);
                    }
                    System.out.printf("%n");
                    ids = new ArrayList<>(listCapacity);
                }
                count++;
                if (count == totalCount) {
                    break;
                }
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
