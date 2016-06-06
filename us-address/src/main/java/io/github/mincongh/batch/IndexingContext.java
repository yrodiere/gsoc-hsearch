package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.search.store.IndexShardingStrategy;

@Named
@Singleton
public class IndexingContext {
    
    private ConcurrentLinkedQueue<Serializable[]> idChunkQueue;
    
    private IndexShardingStrategy indexShardingStrategy;
    
    public IndexingContext() {
        this.idChunkQueue = new ConcurrentLinkedQueue<>();
    }
    
    public void add(Serializable[] idArray) {
        idChunkQueue.add(idArray);
    }
    
    public Serializable[] poll() {
        return idChunkQueue.poll();
    }
    
    public int size() {
        return idChunkQueue.size();
    }
    
    public IndexShardingStrategy getIndexShardingStrategy() {
        return indexShardingStrategy;
    }
    
    public void setIndexShardingStrategy(IndexShardingStrategy indexShardingStrategy) {
        this.indexShardingStrategy = indexShardingStrategy;
    }
}
