package io.github.mincongh.batch;

import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;

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
    @Inject @BatchProperty(name="rootEntities") private String rootEntitiesStr;
    
    @Override
    public PartitionPlan mapPartitions() throws Exception {

        String[] rootEntities = parse(rootEntitiesStr);
        Queue<String> classQueue = new LinkedList<>();
        
        int partSum = 0;
        for (String rootEntity: rootEntities) {
            int queueSize = indexingContext.sizeOf(Class.forName(rootEntity));
            // TODO: handle queueSize is 0
            int partCount = queueSize / partitionCapacity;
            if (queueSize % partitionCapacity != 0) {
                partCount++;
            }
            partSum += partCount;
            // enqueue entity type into classQueue, as much as the number of
            // the partitions
            for (int i = 0; i < partCount; i++) {
                classQueue.add(rootEntity);
            }
        }
        final int partCountFinal = partSum;
        
        return new PartitionPlanImpl() {

            @Override
            public int getPartitions() {
                System.out.printf("#mapPartitions(): %d partitions.%n", partCountFinal);
                return partCountFinal;
            }

            @Override
            public int getThreads() {
                System.out.printf("#getThreads(): %d threads.%n", Math.min(partCountFinal, threads));
                return Math.min(partCountFinal, threads);
            }

            @Override
            public Properties[] getPartitionProperties() {
                
                Properties[] props = new Properties[getPartitions()];
                for (int i = 0; i < props.length; i++) {
                    String entityType = classQueue.poll();
                    props[i] = new Properties();
                    props[i].setProperty("entityType", entityType);
                }
                return props;
            }
        };
    }
    
    /**
     * Parse a set of entities in string into a set of entity-types.
     * 
     * @param raw a set of entities concatenated in string, separated by ","
     *          and surrounded by "[]", e.g. "[com.xx.foo, com.xx.bar]".
     * @return a set of entity-types
     * @throws NullPointerException thrown if the entity-token is not found.
     */
    private String[] parse(String raw) throws NullPointerException {
        if (raw == null) {
            throw new NullPointerException("Not any target entity to index");
        }
        String[] rootEntities = raw
                .substring(1, raw.length() - 1)
                .split(", ");
        return rootEntities;
    }
}
