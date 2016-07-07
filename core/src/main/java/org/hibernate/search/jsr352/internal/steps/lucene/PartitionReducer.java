package org.hibernate.search.jsr352.internal.steps.lucene;

import javax.inject.Named;

import org.jboss.logging.Logger;

@Named
public class PartitionReducer implements javax.batch.api.partition.PartitionReducer {

    private static final Logger logger = Logger.getLogger(PartitionReducer.class);
    
    @Override
    public void beginPartitionedStep() throws Exception {
        logger.info("#beginPartitionedStep() called.");
    }

    @Override
    public void beforePartitionedStepCompletion() throws Exception {
        logger.info("#beforePartitionedStepCompletion() called.");
    }

    @Override
    public void rollbackPartitionedStep() throws Exception {
        logger.info("#rollbackPartitionedStep() called.");
    }

    @Override
    public void afterPartitionedStepCompletion(PartitionStatus status)
            throws Exception {
        logger.info("#afterPartitionedStepCompletion(...) called.");
    }

}
