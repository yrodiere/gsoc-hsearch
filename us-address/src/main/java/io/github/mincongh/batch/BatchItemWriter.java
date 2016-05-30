package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;
import javax.inject.Named;

/**
 * Batch item writer writes a list of items into Lucene documents. Here, items 
 * mean the entities processed by the item processor. These items will be used
 * to create {@code LuceneWork}. 
 * 
 * @author Mincong HUANG
 */
@Named
public class BatchItemWriter implements ItemWriter {

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
    
    }

    /**
     * Produce {@code LuceneWork} using the given entities (items).
     * 
     * @param items a list of entity-array, {@code List<Clazz[]>}. Each item is 
     *          an array of entity.
     * @throw Exception is thrown for any errors.
     */
    @Override
    public void writeItems(List<Object> items) throws Exception {
        if (items != null) {
            System.out.printf("#writeItems(...): %d arrays written.%n", items.size());
        } else {
            System.out.printf("#writeItems(...): null.%n");
        }
    }
}
