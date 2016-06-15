package org.hibernate.search.jsr352.internal;

import javax.batch.api.partition.PartitionReducer;
import javax.inject.Named;

@Named
public class LucenePartitionReducer implements PartitionReducer {

    @Override
    public void beginPartitionedStep() throws Exception {
        System.out.println("#beginPartitionedStep() called.");
    }

    @Override
    public void beforePartitionedStepCompletion() throws Exception {
        System.out.println("#beforePartitionedStepCompletion() called.");
    }

    @Override
    public void rollbackPartitionedStep() throws Exception {
        System.out.println("#rollbackPartitionedStep() called.");
    }

    @Override
    public void afterPartitionedStepCompletion(PartitionStatus status)
            throws Exception {
        System.out.println("#afterPartitionedStepCompletion(...) called.");
    }

}
