package io.github.mincongh.batch;

import java.util.Properties;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Lucene partition mapper provides a partition plan to the Lucene production 
 * step: "produceLuceneDoc". The partition plan is defined dynamically, 
 * according to the indexing context.
 * <p>
 * Several batch properties are used in this mapper:
 * <ul>
 * <li><b>partitionCapacity</b> defines the capacity of one partition: the 
 * number of id arrays that will be treated in this partition. So the number of 
 * partition is computed by the equation: <br>
 * {@code nbPartition = nbArray / partitionCapacity;}
 * 
 * <li><b>threads</b> defines the number of threads wished by the user. Default
 * value is defined in the job xml file. However, the valued used might be 
 * smaller, depending on the number of partitions.
 * </ul>
 * 
 * @author Mincong HUANG
 */
@Named
public class LucenePartitionMapper implements PartitionMapper {

    @Inject
    private IndexingContext indexingContext;
    
    @Inject @BatchProperty private int partitionCapacity;
    @Inject @BatchProperty private int threads;
    
    @Override
    public PartitionPlan mapPartitions() throws Exception {
        
        int queueSize = indexingContext.size();
        int partitions = Math.max(queueSize / partitionCapacity, 1); // minimum 1 partition
        
        return new PartitionPlanImpl() {

            @Override
            public int getPartitions() {
                System.out.printf("#mapPartitions(): %d partitions.%n", partitions);
                return partitions;
            }

            @Override
            public int getThreads() {
                System.out.printf("#getThreads(): %d threads.%n", Math.min(partitions, threads));
                return Math.min(partitions, threads);
            }

            @Override
            public Properties[] getPartitionProperties() {
                Properties[] props = new Properties[getPartitions()];
//              for (int i = 0; i < getPartitions(); i++) {
//                  props[i] = new Properties();
//                  props[i].setProperty("start", String.valueOf(i * 10 + 1));
//                  props[i].setProperty("end", String.valueOf((i + 1) * 10));
//              }
                return props;
            }
        };
    }
}
