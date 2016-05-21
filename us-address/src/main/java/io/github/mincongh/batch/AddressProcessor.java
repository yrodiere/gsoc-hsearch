package io.github.mincongh.batch;

import java.io.Serializable;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.impl.HibernateSessionLoadingInitializer;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.spi.InstanceInitializer;

import io.github.mincongh.entity.Address;

@Named
public class AddressProcessor implements ItemProcessor {

    // Entity manager is used for fetching entities from persistence context.
    @PersistenceContext(unitName = "us-address")
    private EntityManager em;
    
    // Hibernate session
    private Session session;
    
    // Interface which gives access to runtime configuration
    // Intended to be used by Search components
    private ExtendedSearchIntegrator searchIntegrator;
    
    // Entity index binding specifies the relation and options from an indexed
    // entity to its index(es).
//  private Map<Class<?>, EntityIndexBinding> entityIndexBindings;
    private EntityIndexBinding entityIndexBinding;
    
    // The document builder for indexed entity (Address)
    private DocumentBuilderIndexedEntity docBuilder;
    
    // TODO: what is a session initializer ?
    private InstanceInitializer sessionInitializer;
    
    // TODO: add description
    private ConversionContext conversionContext;

    /**
     * Builds the Lucene {@code Document} for a given entity instance and its id
     * using +org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity+.
     * 
     */
    @Override
    public Object processItem(Object item) throws Exception {
        
        // TODO: initialization should be placed in a better location
        if (entityIndexBinding == null) {
            init();
        }
        
        Address address = (Address) item;
        
//      // This is a test. At the very beginning, this processor do nothing
//      // but print the entity (address) id:
//      System.out.printf("Processing item %d ...%n", address.getAddressId());
//      return address;

        // Builds the Lucene {@code Document} for a given entity.
        
        // Tenant-aware
        // Hibernate Search supports multi-tenancy on top of Hibernate ORM, 
        // it stores the tenant identifier in the document and automatically 
        // filters the query results. The FullTextSession will be bound to 
        // the specific tenant ("client-A" in the example) and the mass indexer 
        // will only index the entities associated to that tenant identifier.
        // TODO: can it be null for a single-tenancy db ?
        String tenantId = null;
        
        Serializable id = session.getIdentifier(address);
        TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
        conversionContext.pushProperty(docBuilder.getIdKeywordName());
        String idInString = null;
        try {
            idInString = conversionContext
                    .setClass(Address.class)
                    .twoWayConversionContext(idBridge)
                    .objectToString(id);
        } finally {
            conversionContext.popProperty();
        }
        AddLuceneWork addWork = docBuilder.createAddWork(
                tenantId,
                Address.class,
                address,
                id,
                idInString,
                sessionInitializer,
                conversionContext
        );
        return addWork;
    }
    
    /**
     * Initialization for different classes required for the the Lucene
     * document conversion.
     * 
     */
    private void init() {

        // Get Hibernate session from JPA entity manager
        session = em.unwrap(Session.class);
        
        // TODO: construct SessionImplementor before getting searchIntegrator
        // but how should I use it ? This class belongs to org.hibernate.engine.
        // ...
        // get extendedSearchIntegrator via ContextHelper using hibernate
        // session
        searchIntegrator = ContextHelper.getSearchintegrator(session);
        
        // In the previous implementation (currently used in HSERACH), the class
        // IdentifierConsumerDocumentProducer is used to get the index bindings
        // but this time, reader interacts directly with searchIntegrator to
        // these bindings. Once bindings assigned, we need to find out the
        // matched binding for the target class:
        //
        //     io.github.mincongh.entity.Address
        // 
        entityIndexBinding = searchIntegrator
                .getIndexBindings()
                .get(Address.class);
        
        // TODO: There should be optimizations here, but they're omitted at the
        // first time for simply the process
        // ...
        // Get the document builder
        docBuilder = entityIndexBinding.getDocumentBuilder();
        
        // initialize conversion context
        conversionContext = new ContextualExceptionBridgeHelper();
        
        // initialize session initializer
        sessionInitializer = new HibernateSessionLoadingInitializer(
                (SessionImplementor) session
        );
    }
}
