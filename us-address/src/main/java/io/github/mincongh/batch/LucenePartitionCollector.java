package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.partition.PartitionCollector;
import javax.inject.Named;

@Named
public class LucenePartitionCollector implements PartitionCollector {

    /**
     * The collectPartitionData method receives control periodically during 
     * partition processing. This method receives control on each thread 
     * processing a partition as IdProducerBatchlet, once at the end of the 
     * process.
     */
    @Override
    public Serializable collectPartitionData() throws Exception {
        System.out.println("#collectPartitionData() called.");
        return 1;
    }
}
