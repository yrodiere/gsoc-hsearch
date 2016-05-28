package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class IdProductionContext {
    
    private Queue<Serializable[]> idProductionQueue;
    
    public IdProductionContext() {
        this.idProductionQueue = new LinkedList<Serializable[]>();
    }
    
    public void add(Serializable[] idArray) {
        idProductionQueue.add(idArray);
    }
    
    public Serializable[] poll() {
        return idProductionQueue.poll();
    }
}
