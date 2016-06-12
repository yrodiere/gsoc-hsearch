package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.search.store.IndexShardingStrategy;

@Named
@Singleton
public class IndexingContext {
    
    private ConcurrentHashMap<Class<?>, ConcurrentLinkedQueue<Serializable[]>> idQueues;
    
    private IndexShardingStrategy indexShardingStrategy;
    
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
}
