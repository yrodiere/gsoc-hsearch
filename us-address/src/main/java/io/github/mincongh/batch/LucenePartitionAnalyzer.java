package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

@Named
public class LucenePartitionAnalyzer implements PartitionAnalyzer {

    private int workCount = 0;
    
    /**
     * 
     * @param fromCollector the checkpoint obtained from partition collector's 
     *          collectPartitionData
     */
    @Override
    public void analyzeCollectorData(Serializable fromCollector) throws Exception {
        workCount += (int) fromCollector;
        System.out.printf("#analyzeCollectorData(): %d works processed.%n", workCount);
    }

    @Override
    public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
            throws Exception {
        System.out.println("#analyzeStatus(...) called.");
    }

}
