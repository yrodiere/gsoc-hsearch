package org.hibernate.search.jsr352.internal;

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

        Class<?>[] rootEntities = parse(rootEntitiesStr);
        Queue<String> classQueue = new LinkedList<>();
        
        int totalPartitions = 0;
        for (Class<?> rootEntity: rootEntities) {
            
            int queueSize = indexingContext.sizeOf(rootEntity);
            int classPartitions = queueSize / partitionCapacity;  // TODO: handle queueSize is 0
            if (queueSize % partitionCapacity != 0) {
                classPartitions++;
            }
            
            // enqueue entity type into classQueue, as much as the number of
            // the class partitions
            for (int i = 0; i < classPartitions; i++) {
                classQueue.add(rootEntity.getName());
            }
            System.out.printf("%d partitions added to root entity \"%s\".%n",
                    classPartitions, rootEntity);
            
            totalPartitions += classPartitions;
        }
        final int TOTAL_PARTITIONS = totalPartitions;
        
        return new PartitionPlanImpl() {

            @Override
            public int getPartitions() {
                System.out.printf("#mapPartitions(): %d partitions.%n", TOTAL_PARTITIONS);
                return TOTAL_PARTITIONS;
            }

            @Override
            public int getThreads() {
                System.out.printf("#getThreads(): %d threads.%n", Math.min(TOTAL_PARTITIONS, threads));
                return Math.min(TOTAL_PARTITIONS, threads);
            }

            @Override
            public Properties[] getPartitionProperties() {
                Properties[] props = new Properties[TOTAL_PARTITIONS];
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
     * @throws ClassNotFoundException thrown if the target string name is not
     *          a valid class name.
     */
    private Class<?>[] parse(String raw) throws NullPointerException, 
            ClassNotFoundException {
        if (raw == null) {
            throw new NullPointerException("Not any target entity to index");
        }
        String[] names = raw
                .substring(1, raw.length() - 1)  // removes '[' and ']'
                .split(", ");
        Class<?>[] classes = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) {
            classes[i] = Class.forName(names[i]);
        }
        return classes;
    }
}
