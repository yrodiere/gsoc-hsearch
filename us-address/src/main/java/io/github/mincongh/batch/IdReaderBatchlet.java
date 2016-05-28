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
 * stored then be used for Lucene document production.
 * 
 * @author Mincong HUANG
 */
@Named
public class IdReaderBatchlet implements Batchlet {
    
    @Inject
    private IdProductionContext idProductionContext;
    
    @Override
    public String process() throws Exception {
        Serializable[] ids = idProductionContext.poll();
        while (ids != null) {
            for (Serializable _id : ids) {
                System.out.printf("%d ", _id);
            }
            System.out.printf("%n");
            ids = idProductionContext.poll();
        }
        System.out.println("IdReaderBatchlet#process() completed.");
        return BatchStatus.COMPLETED.toString();
    }
    
    @Override
    public void stop() throws Exception {
        
    }
}
