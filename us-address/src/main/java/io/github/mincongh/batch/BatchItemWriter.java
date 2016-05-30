package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;
import javax.inject.Named;

@Named
public class BatchItemWriter implements ItemWriter {

    @Override
    public Serializable checkpointInfo() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void open(Serializable arg0) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void writeItems(List<Object> items) throws Exception {
        if (items != null) {
            System.out.printf("#writeItems(...): %d arrays written.%n", items.size());
        } else {
            System.out.printf("#writeItems(...): null.%n");
        }
    }
}
