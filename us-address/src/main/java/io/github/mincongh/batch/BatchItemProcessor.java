package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;

@Named
public class BatchItemProcessor implements ItemProcessor {

    @Override
    public Object processItem(Object item) throws Exception {
        
        Serializable[] ids = (Serializable[]) item;
        String msg = "";
        
        for (Serializable id : ids) {
            msg += String.format("%7d", (int) id);
        }
        System.out.printf("#processItem(): %s%n", msg);
        
        return ids;
    }
}
