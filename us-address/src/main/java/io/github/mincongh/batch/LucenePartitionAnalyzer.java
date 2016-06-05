package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LucenePartitionAnalyzer implements PartitionAnalyzer {

    @Inject
    private JobContext jobContext;
    private int workCount = 0;
    private float percentage = 0;
    @Inject @BatchProperty
    private int maxResults;
    
    /**
     * Analyze data obtained from different partition plans via partition data
     * collectors. The current analyze is to summarize to their progresses : 
     * 
     *     workCount = workCount1 + workCount2 + ... + workCountN
     * 
     * Then it shows the total mass index progress in percentage. This method is
     * very similar to the current simple progress monitor. Note: concerning
     * the "total" number of entities to process, it depends on 2 values : the
     * number of row in the db table and the max results to process, defined by
     * user before the job start. So the minimum between them will be used.
     * 
     * @param fromCollector the checkpoint obtained from partition collector's 
     *          collectPartitionData
     */
    @Override
    public void analyzeCollectorData(Serializable fromCollector) throws Exception {
        
        long rowCount = (long) jobContext.getTransientUserData();
        int total = Math.min((int) rowCount, maxResults);

        workCount += (int) fromCollector;
        if (total != 0) {
            percentage = 100 * (float) workCount / total;
        }
        System.out.printf("#analyzeCollectorData(): %d works processed (%.1f%%).%n",
                workCount, percentage);
    }

    @Override
    public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
            throws Exception {
        System.out.println("#analyzeStatus(...) called.");
    }

}
