package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutor;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Batch item writer writes a list of items into Lucene documents. Here, items 
 * mean the entities processed by the item processor. These items will be used
 * to create {@code LuceneWork}. 
 * <p>
 * <ul>
 * <li>{@code stepContext} TODO: add description here
 * 
 * <li>{@code stepContext} the JSR 352 specific step context, used for storing
 *      transient data during the step execution.
 *      
 * <li>{@code monitor} mass indexer progress monitor helps to follow the mass
 *      indexing progress and show it in the console.
 * </ul>
 * 
 * @author Mincong HUANG
 */
@Named
public class BatchItemWriter implements ItemWriter {

    @Inject
    private IndexingContext indexingContext;
    private MassIndexerProgressMonitor monitor;
    private final Boolean forceAsync = false;
    
    /**
     * The checkpointInfo method returns the current checkpoint data for this 
     * writer. It is called before a chunk checkpoint is committed.
     * 
     * @return the checkpoint info
     * @throws Exception is thrown for any errors.
     */
    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }
    
    /**
     * The close method marks the end of use of the ItemWriter. The writer
     * is used to do the cleanup.
     * 
     * @throws Exception is thrown for any errors.
     */
    @Override
    public void close() throws Exception {
    
    }

    /**
     * The open method prepares the writer to write items.
     * 
     * @param checkpoint the last checkpoint
     */
    @Override
    public void open(Serializable checkpoint) throws Exception {
        monitor = new SimpleIndexingProgressMonitor();
    }

    /**
     * Produce {@code LuceneWork} using the given entities (items).
     * 
     * @param items a list of entity-array, {@code List<Clazz[]>}. Each item is 
     *          an array of entity.
     * @throw Exception is thrown for any errors.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void writeItems(List<Object> items) throws Exception {
/*
        if (items != null) {
            System.out.printf("#writeItems(...): %d lucene work arrays written.%n", items.size());
        } else {
            System.out.printf("#writeItems(...): null.%n");
        }
*/
        IndexShardingStrategy shardingStrategy = 
                indexingContext.getIndexShardingStrategy();
        for (Object item : items) {
            for(AddLuceneWork addWork : (LinkedList<AddLuceneWork>) item) {
                StreamingOperationExecutor executor = addWork.acceptIndexWorkVisitor(
                        StreamingOperationExecutorSelector.INSTANCE, null);
                executor.performStreamOperation(
                        addWork,
                        shardingStrategy,
//                      monitor,
                        null,
                        forceAsync
                );
            }
        }
    }
}
