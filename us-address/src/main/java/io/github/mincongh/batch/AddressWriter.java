package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;
import javax.inject.Named;

import io.github.mincongh.entity.Address;

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
    }

    @Override
    public void open(Serializable arg0) throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeItems(List<Object> items) throws Exception {
        for (Object item: items) {
            System.out.printf("%d,", ((Address) item).getAddressId());
        }
        System.out.printf("%n");
    }
}
