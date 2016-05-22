package io.github.mincongh.batch;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;

import org.hibernate.search.backend.AddLuceneWork;

@Named
public class AddressProcessor implements ItemProcessor {

    /**
     * Builds the Lucene {@code Document} for a given entity instance and its id
     * using +org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity+.
     * 
     */
    @Override
    public Object processItem(Object item) throws Exception {
        
//      // This is a test. At the very beginning, this processor do nothing
//      // but print the entity (address) id:        
//      Address address = (Address) item;        
//      System.out.printf("Processing item %d ...%n", address.getAddressId());
//      return address;

        AddLuceneWork addWork = (AddLuceneWork) item;
        System.out.println(addWork);
        return addWork;
    }
}
