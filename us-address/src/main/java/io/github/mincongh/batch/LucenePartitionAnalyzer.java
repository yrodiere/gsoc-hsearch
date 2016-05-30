package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

@Named
public class LucenePartitionAnalyzer implements PartitionAnalyzer {

    private int checkpoints = 0;
    
    @Override
    public void analyzeCollectorData(Serializable checkpoint) throws Exception {
        checkpoints += (int) checkpoint;
        System.out.printf("#analyzeCollectorData(): %d checkpoints.%n", checkpoints);
    }

    @Override
    public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
            throws Exception {
        System.out.println("#analyzeStatus(...) called.");
    }

}
