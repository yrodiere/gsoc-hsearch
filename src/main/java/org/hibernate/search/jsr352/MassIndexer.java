package org.hibernate.search.jsr352;

import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

// TODO: separate this class into interface and implementation
public class MassIndexer {

    private boolean optimizeAfterPurge = false;
    private boolean optimizeAtEnd = false;
    private boolean purgeAtStart = false;
    
    private int arrayCapacity = 1000;
    private int fetchSize = 200000;
    private int maxResults = 1000000;
    private int partitionCapacity = 250;
    private int partitions = 4;
    private int threads = 2;
    
    private final String JOB_NAME = "mass-index";

    private Set<Class<?>> rootEntities;
    
    MassIndexer() {
        
    }
    
    public long start() {
        Properties jobParams = new Properties();
        jobParams.setProperty("fetchSize", String.valueOf(fetchSize));
        jobParams.setProperty("arrayCapacity", String.valueOf(arrayCapacity));
        jobParams.setProperty("maxResults", String.valueOf(maxResults));
        jobParams.setProperty("partitionCapacity", String.valueOf(partitionCapacity));
        jobParams.setProperty("partitions", String.valueOf(partitions));
        jobParams.setProperty("threads", String.valueOf(threads));
        jobParams.setProperty("purgeAtStart", String.valueOf(purgeAtStart));
        jobParams.setProperty("optimizeAfterPurge", String.valueOf(optimizeAfterPurge));
        jobParams.setProperty("optimizeAtEnd", String.valueOf(optimizeAtEnd));
        jobParams.setProperty("rootEntities", String.valueOf(rootEntities));
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Long executionId = jobOperator.start(JOB_NAME, jobParams);
        return executionId;
    }
    
    public void stop(long executionId) {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        jobOperator.stop(executionId);
    }
    
    public MassIndexer arrayCapacity(int arrayCapacity) {
        if (arrayCapacity < 1) {
            throw new IllegalArgumentException("arrayCapacity must be at least 1");
        }
        this.arrayCapacity = arrayCapacity;
        return this;
    }
    
    public MassIndexer fetchSize(int fetchSize) {
        if (fetchSize < 1) {
            throw new IllegalArgumentException("fetchSize must be at least 1");
        }
        this.fetchSize = fetchSize;
        return this;
    }
    
    public MassIndexer maxResults(int maxResults) {
        if (maxResults < 1) {
            throw new IllegalArgumentException("maxResults must be at least 1");
        }
        this.maxResults = maxResults;
        return this;
    }
    
    public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge) {
        this.optimizeAfterPurge = optimizeAfterPurge;
        return this;
    }
    
    public MassIndexer optimizeAtEnd(boolean optimizeAtEnd) {
        this.optimizeAtEnd = optimizeAtEnd;
        return this;
    }
    
    public MassIndexer partitionCapacity(int partitionCapacity) {
        if (partitionCapacity < 1) {
            throw new IllegalArgumentException("partitionCapacity must be at least 1");
        }
        this.partitionCapacity = partitionCapacity;
        return this;
    }
    
    public MassIndexer partitions(int partitions) {
        if (partitions < 1) {
            throw new IllegalArgumentException("partitions must be at least 1");
        }
        this.partitions = partitions;
        return this;
    }
    
    public MassIndexer purgeAtStart(boolean purgeAtStart) {
        this.purgeAtStart = purgeAtStart;
        return this;
    }
    
    public MassIndexer threads(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1");
        }
        this.threads = threads;
        return this;
    }
}
