package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

@Named
public class IdPartitionAnalyzer implements PartitionAnalyzer {

    private int finishedPartitions = 0;
    
    @Override
    public void analyzeCollectorData(Serializable partition) throws Exception {
        finishedPartitions += (int) partition;
        System.out.printf("#analyzeCollectorData(): "
                + "%d partitions finished.%n", finishedPartitions);
    }

    @Override
    public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
            throws Exception {
        System.out.println("#analyzeStatus(...) called.");
    }

}
