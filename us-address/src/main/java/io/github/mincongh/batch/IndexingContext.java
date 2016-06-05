package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.search.store.IndexShardingStrategy;

@Named
@Singleton
public class IndexingContext {
    
    private Queue<Serializable[]> idChunkQueue;
    private IndexShardingStrategy indexShardingStrategy;
    
    public IndexingContext() {
        this.idChunkQueue = new LinkedList<Serializable[]>();
    }
    
    public synchronized void add(Serializable[] idArray) {
        idChunkQueue.add(idArray);
    }
    
    public synchronized Serializable[] poll() {
        return idChunkQueue.poll();
    }
    
    public synchronized int size() {
        return idChunkQueue.size();
    }
    
    public IndexShardingStrategy getIndexShardingStrategy() {
        return indexShardingStrategy;
    }
    
    public void setIndexShardingStrategy(IndexShardingStrategy indexShardingStrategy) {
        this.indexShardingStrategy = indexShardingStrategy;
    }
}
