package org.hibernate.search.jsr352.internal;

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
 * mean the luceneWorks, given by the processor. These items will be executed
 * using StreamingOperationExecutor.
 * <p>
 * <ul>
 * <li>{@code indexingContext} is used to store the shardingStrategy
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
    
    private final Boolean forceAsync = false;
    
    // TODO: The monitor is not used for instance. It should be used later.
    private MassIndexerProgressMonitor monitor;
    
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
     * Execute {@code LuceneWork}
     * 
     * @param items a list of items, where each item is a list of Lucene works.
     * @throw Exception is thrown for any errors.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void writeItems(List<Object> items) throws Exception {
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
