package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Specific indexing context for mass indexer. Several attributes are used :
 * <p>
 * <ul>
 * <li>entityCount: the total number of entities to be indexed in the job. The
 *      number is summarized by partitioned step "loadId". Each 
 *      IdProducerBatchlet (partiton) produces the number of entities linked to
 *      its own target entity, then call the method #addEntityCount(long) to
 *      summarize it with other partition(s).</li>
 * </ul>
 * @author Mincong HUANG
 */
@Named
@Singleton
public class IndexingContext {
    
    private ConcurrentHashMap<Class<?>, ConcurrentLinkedQueue<Serializable[]>> idQueues;
    private IndexShardingStrategy indexShardingStrategy;
    private long entityCount = 0;
    
    public void add(Serializable[] clazzIDs, Class<?> clazz) {
        idQueues.get(clazz).add(clazzIDs);
    }
    
    public Serializable[] poll(Class<?> clazz) {
        return idQueues.get(clazz).poll();
    }
    
    public int sizeOf(Class<?> clazz) {
        return idQueues.get(clazz).size();
    }
    
    public void createQueue(Class<?> clazz) {
        idQueues.put(clazz, new ConcurrentLinkedQueue<>());
    }
    
    public IndexingContext() {
        this.idQueues = new ConcurrentHashMap<>();
    }
    
    public ConcurrentHashMap<Class<?>, ConcurrentLinkedQueue<Serializable[]>> getIdQueues() {
        return idQueues;
    }
    
    // I don't think we need this method.
    public void setIdQueues(ConcurrentHashMap<Class<?>, ConcurrentLinkedQueue<Serializable[]>> idQueues) {
        this.idQueues = idQueues;
    }
    
    public IndexShardingStrategy getIndexShardingStrategy() {
        return indexShardingStrategy;
    }
    
    public void setIndexShardingStrategy(IndexShardingStrategy indexShardingStrategy) {
        this.indexShardingStrategy = indexShardingStrategy;
    }
    
    public void addEntityCount(long entityCount) {
        this.entityCount += entityCount;
    }
    
    public long getEntityCount() {
        return entityCount;
    }
}
