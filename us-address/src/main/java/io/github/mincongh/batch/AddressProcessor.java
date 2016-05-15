package io.github.mincongh.batch;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;

import io.github.mincongh.entity.Address;

@Named
public class AddressProcessor implements ItemProcessor {

    @Override
    public Object processItem(Object item) throws Exception {
        Address address = (Address) item;
        System.out.printf("Processing item %d ...%n", address.getAddressId());
        return address;
    }
}
