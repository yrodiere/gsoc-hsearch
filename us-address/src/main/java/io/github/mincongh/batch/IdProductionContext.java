package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class IdProductionContext {
    
    private Queue<Serializable[]> idChunkQueue;
    
    public IdProductionContext() {
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
}
