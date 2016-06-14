package io.github.mincongh.batch;

import javax.batch.api.BatchProperty;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Decider decides the next step-execution before the start of index chunk. If
 * user requires a index purge, then the next step should be a purge, else, 
 * the next step will be directly the index chunk. Index purge use 
 * IndexPurgerBatchlet. 
 * TODO: modify javadoc 
 * 
 * @author Mincong HUANG
 */
@Named
public class AfterPurgeDecider implements Decider {

    @Inject @BatchProperty
    private Boolean optimizeAfterPurge;
    
    /**
     * Decide the next step using the target batch property.
     * 
     * @param executions step executions.
     */
    @Override
    public String decide(StepExecution[] executions) throws Exception {
        System.out.printf("AfterPurgeDecide#decide: %s%n", String.valueOf(optimizeAfterPurge));
        return String.valueOf(optimizeAfterPurge);
    }
}
