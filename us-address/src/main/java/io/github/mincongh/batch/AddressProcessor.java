package io.github.mincongh.batch;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutor;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.store.IndexShardingStrategy;

@Named
public class AddressProcessor implements ItemProcessor {

    // use address reader to access to importing properties
    // TODO: should have better way to do and avoid this
    @Inject
    private AddressReader addressReader;
    
    // progress monitor
    // In order to simplify the progress, we don't consider the JMX status here.
    // The simple indexing progress monitor is used here
    private MassIndexerProgressMonitor monitor =
            new SimpleIndexingProgressMonitor();
    
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
        
        // use the same mechanism as described in class batch back-end:
        // org.hibernate.search.backend.impl.batch#enqueueAsyncWork(LuceneWork)
        // TODO: implementation here...
        // TODO: an integrator is needed for the task, how to construct an
        // integrator ? -> It is generated using session as parameter, but the 
        // session is closed now. Will it be alright to use a new session ?
        // -> what is the code behind EntendedIntegrator ? -> OK. Integrator is
        // here only for obtaining the entityIndexBinding
        
        @SuppressWarnings("deprecation")
        IndexShardingStrategy shardingStrategy = addressReader
                .getEntityIndexBinding()
                .getSelectionStrategy();
        
        // run in asynchronous mode
        Boolean forceAsync = false;
        StreamingOperationExecutor executor = addWork.acceptIndexWorkVisitor(
                StreamingOperationExecutorSelector.INSTANCE, null);
        executor.performStreamOperation(
                addWork,
                shardingStrategy,
                monitor,
                forceAsync
        );
        return addWork;
    }
}
