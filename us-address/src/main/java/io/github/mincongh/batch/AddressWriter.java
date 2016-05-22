package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;
import javax.inject.Named;

import org.hibernate.search.backend.AddLuceneWork;

@Named
public class AddressWriter implements ItemWriter {

    @Override
    public Serializable checkpointInfo() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        // TODO: shutdown executor
    }

    @Override
    public void open(Serializable arg0) throws Exception {
        // TODO Auto-generated method stub
    }

    /**
     * Write items to destination :
     * TODO: add more description here
     * For instance, they do nothing but print to the console.
     * 
     * @param items items to write. This is a list of AddLuceneWorks processed
     *         previously by the AddressProcessor.
     */
    @Override
    public void writeItems(List<Object> items) throws Exception {
        System.out.println("----------wirteItems(List<Object>) start----------");
        for (Object item: items) {
//          System.out.printf("%d,", ((Address) item).getAddressId());
            System.out.println((AddLuceneWork) item);
        }
        System.out.println("----------wirteItems(List<Object>) end----------");
    }
}
