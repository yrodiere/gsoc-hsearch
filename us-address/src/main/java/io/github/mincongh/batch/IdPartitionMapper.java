package io.github.mincongh.batch;

import java.util.Properties;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Id partition mapper provides a map partitions plan to the step "printId".
 * The partition plan is defined dynamically, according to the id production 
 * context.
 * 
 * @author Mincong HUANG
 */
@Named
public class IdPartitionMapper implements PartitionMapper {

    @Inject
    private IdProductionContext idProductionContext;
    
    @Inject @BatchProperty private int partitionCapacity;
    @Inject @BatchProperty private int threads;
    
    @Override
    public PartitionPlan mapPartitions() throws Exception {
        
        int queueSize = idProductionContext.size();
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
