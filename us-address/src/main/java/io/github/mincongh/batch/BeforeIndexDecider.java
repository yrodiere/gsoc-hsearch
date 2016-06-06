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
 * 
 * @author Mincong HUANG
 */
@Named
public class BeforeIndexDecider implements Decider {

    @Inject @BatchProperty
    private Boolean purgeAtStart;
    private final String PURGE_IDX = "purgeIndex";
    private final String PRODUCE_DOC = "produceLuceneDoc";
    
    /**
     * Decide the next step
     * 
     * @param executions not used for the moment.
     */
    @Override
    public String decide(StepExecution[] executions) throws Exception {
        
        String nextStep = purgeAtStart ? PURGE_IDX : PRODUCE_DOC;
        
        for (StepExecution se : executions) {
            System.out.println(se.getStepName() + " "
                    + se.getBatchStatus().toString() + " "
                    + se.getExitStatus()
            );
        }
        
        return nextStep;
    }
}
