package org.hibernate.search.jsr352.internal;

import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

@Named
public class IndexPurgerBatchlet implements Batchlet {

    @Override
    public String process() throws Exception {
        
        System.out.println("purging entities ...");
        
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() throws Exception {
        // TODO Auto-generated method stub
    }
}
