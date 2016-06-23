package org.hibernate.search.jsr352.internal;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;

/**
 * Read entity IDs from {@code IndexingContext}. Each time, there's one array
 * being read. The number of IDs inside the array depends on the array capacity.
 * This value is defined before the job start. Either the default value defined
 * in the job xml will be applied, or the value overwritten by the user in job
 * parameters. These IDs will be processed in {@code BatchItemProcessor}, then 
 * be used for Lucene document production.
 * <p>
 * The motivation of using an array of IDs over a single ID is to accelerate
 * the entity processing. Use a SELECT statement to obtain only one ID is
 * rather a waste. For more detail about the entity process, please check {@code 
 * BatchItemProcessor}.
 * 
 * @author Mincong HUANG
 */
@Named
public class BatchItemReader implements ItemReader {
    
    @Inject @BatchProperty
    private String entityType;
    
    @Inject
    private IndexingContext indexingContext;
    
    private static final Logger logger = Logger.getLogger(BatchItemReader.class);
    
    /**
     * The checkpointInfo method returns the current checkpoint data for this 
     * reader. It is called before a chunk checkpoint is committed.
     * 
     * @return the checkpoint info
     * @throws Exception thrown for any errors.
     */
    @Override
    public Serializable checkpointInfo() throws Exception {
        logger.info("checkpointInfo");
        return null;
    }

    /**
     * Close operation(s) before the class destruction.
     * 
     * @throws Exception thrown for any errors.
     */
    @Override
    public void close() throws Exception {
        logger.info("close");
    }

    /**
     * Initialize the environment. If checkpoint does not exist, then it should 
     * be the first open. If checkpoint exist, then it isn't the first open,
     * provide another open scenario. This mechanism is not used in this demo.
     * 
     * @param checkpoint The last checkpoint info saved in the batch runtime, 
     *          previously given by checkpointInfo().
     * @throws Exception thrown for any errors.
     */
    @Override
    public void open(Serializable checkpoint) throws Exception {
        logger.debugf("#open(...): entityType = %s", entityType);
    }

    /**
     * Read item from the {@code IndexingContext}. Here, item means an array of
     * IDs previously produced by the {@code IdProducerBatchlet}.
     * 
     * @throws Exception thrown for any errors.
     */
    @Override
    public Object readItem() throws Exception {
        return indexingContext.poll(Class.forName(entityType));
    }
}
