package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Read entity IDs from {@code IndexingContext}. Each time, there's an array of 
 * IDs being read. The number depend on array capacity, defined before the job
 * start. The default value is __. These IDs is going to be processed in 
 * {@code BatchItemProcessor}, then be used for Lucene document production.
 * 
 * @author Mincong HUANG
 */
@Named
public class BatchItemReader implements ItemReader {
    
    @Inject
    private IndexingContext indexingContext;
    
    @Override
    public Serializable checkpointInfo() throws Exception {
        System.out.println("BatchItemReader#checkpointInfo()");
        return null;
    }

    @Override
    public void close() throws Exception {
        System.out.println("BatchItemReader#close()");
    }

    /**
     * Initialize the environment. If checkpoint does not exist, then it should 
     * be the first open. If checkpoint exist, then it isn't the first open,
     * provide another open scenario. This mechanism is not used in this demo.
     * 
     * @param checkpoint The checkpoint info were saved in the batch runtime, 
     *          previously provided by checkpointInfo().
     * @throws Exception Any exception occurs during the read.
     */
    @Override
    public void open(Serializable checkpoint) throws Exception {
        System.out.println("BatchItemReader#open(...)");
    }

    /**
     * Read item from the {@code IndexingContext}. Here, item means an array of
     * IDs previously produced by the {@code IdProducerBatchlet}.
     */
    @Override
    public Object readItem() throws Exception {
        return indexingContext.poll();
    }
}
