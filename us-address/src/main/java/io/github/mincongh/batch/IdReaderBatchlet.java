package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.inject.Inject;
import javax.inject.Named;

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
            String msg = "";
            for (Serializable _id : ids) {
                msg += String.format("%5d ", _id);
            }
            System.out.printf("%s%n", msg);
            ids = idProductionContext.poll();
        }
        
        System.out.println("IdReaderBatchlet#process() completed.");
        return BatchStatus.COMPLETED.toString();
    }
    
    @Override
    public void stop() throws Exception {
        
    }
}
