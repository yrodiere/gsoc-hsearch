package io.github.mincongh.batch;

import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

@Named
public class OptimizerBatchlet implements Batchlet {

    @Override
    public String process() throws Exception {
       System.out.println("Optimizing ...");
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() throws Exception {
        
    }
}
