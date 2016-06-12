package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

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
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Batch item processor loads entities using entity IDs, provided by batch item
 * reader. Please notice that the process runs under multiple partitions, which
 * means the input IDs are provided by the item reader in the same partition.
 * 
 * <p>
 * Several attributes are used in this class :
 * <ul>
 * <li>{@code session} is the Hibernate session unwrapped from JPA entity. It
 *      will be used to construct the Lucene work.
 *  
 * <li>{@code searchIntegrator} is an interface which gives access to runtime
 *      configuration, it is intended to be used by Search components.
 * 
 * <li>{@code entityIndexBinding} Entity index binding specifies the relation
 *      and options from an indexed entity to its index(es).
 * 
 * <li>{@code docBuilder} is the document builder for indexed entity (Address).
 * 
 * <li>{@code sessionInitializer} TODO: don't know what it is.
 * 
 * <li>{@code conversionContext} TODO: don't know what it is.
 * 
 * <li>{@code shardingStrategy} TODO: add description
 * 
 * <li>{@code indexingContext} TODO: add description
 * </ul>
 * 
 * @author Mincong HUANG
 */
@Named
public class BatchItemProcessor implements ItemProcessor {
    
    @PersistenceContext(unitName = "us-address")
    private EntityManager em;
    private Session session;
    private ExtendedSearchIntegrator searchIntegrator;
    private DocumentBuilderIndexedEntity docBuilder;
    private EntityIndexBinding entityIndexBinding;
    private InstanceInitializer sessionInitializer;
    private ConversionContext conversionContext;
    private IndexShardingStrategy shardingStrategy;
    @Inject
    private IndexingContext indexingContext;
    @Inject
    private StepContext stepContext;
    @Inject @BatchProperty
    private String entityType;
    
    /**
     * Process an input item into an output item. Here, the input item is an 
     * array of IDs and the output item is a list of entities mapped to these 
     * IDs. During the process, an injected entity manager is used to find out 
     * the entities in the database.
     * 
     * @param item the input item, an array of IDs
     * @return a list of entities mapped to the input IDs
     * @throws Exception thrown for any errors.
     */
    @Override
    public Object processItem(Object item) throws Exception {
        
        Class<?> entityClazz = Class.forName(entityType);
        
        // TODO: change the id to generic type
        // TODO: accept all entity type. For instance, only Address.class works
        if (entityType.equals("io.github.mincongh.entity.Stock")) {
            return null;
        }
        
        int[] ids = toIntArray((Serializable[]) item);
        List<?> entities = null;
        List<AddLuceneWork> addWorks = null;
        
        CriteriaQuery<?> q = buildCriteriaQuery(entityClazz, ids);
        entities = em
                .createQuery(q)
                // don't insert into cache.
                .setHint("javax.persistence.cache.storeMode", "BYPASS")
                // get data directly from the database.
                .setHint("javax.persistence.cache.retrieveMode", "BYPASS")
                .getResultList();
        addWorks = buildAddLuceneWorks(entities, entityClazz);
        updateWorksCount(addWorks.size());
  
        return addWorks;
    }
    
    /**
     * Update the Lucene Works counts using the step context.
     * 
     * @param currentCount the works processed during the current 
     *          processItem().
     */
    private void updateWorksCount(int currentCount) {
        Object userData = stepContext.getTransientUserData();
        int previousCount = userData != null ? (int) userData : 0;
        int totalCount = previousCount + currentCount;
        stepContext.setTransientUserData(totalCount);
    }
    
    /**
     * Build addLuceneWorks using entities. This method is inspired by the
     * current mass indexer implementation.
     * 
     * @param addresses entities obtained from JPA entity manager
     * @param entityClazz the class type of selected entities
     * @return a list of addLuceneWorks
     */
    private List<AddLuceneWork> buildAddLuceneWorks(List<?> addresses, 
            Class<?> entityClazz) {
        
        List<AddLuceneWork> addWorks = new LinkedList<>();
        String tenantId = null;
        
        session = em.unwrap(Session.class);
        searchIntegrator = ContextHelper.getSearchintegrator(session);
        entityIndexBinding = searchIntegrator
                .getIndexBindings()
                .get(entityClazz);
        shardingStrategy = entityIndexBinding.getSelectionStrategy();
        indexingContext.setIndexShardingStrategy(shardingStrategy);
        docBuilder = entityIndexBinding.getDocumentBuilder();
        conversionContext = new ContextualExceptionBridgeHelper();
        sessionInitializer = new HibernateSessionLoadingInitializer(
                (SessionImplementor) session
        );
        
        for (Object entity: addresses) {
            Serializable id = session.getIdentifier(entity);
            TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
            conversionContext.pushProperty(docBuilder.getIdKeywordName());
            String idInString = null;
            try {
                idInString = conversionContext
                        .setClass(entityClazz)
                        .twoWayConversionContext(idBridge)
                        .objectToString(id);
            } finally {
                conversionContext.popProperty();
            }
            AddLuceneWork addWork = docBuilder.createAddWork(
                    tenantId,
                    entity.getClass(),
                    entity,
                    id,
                    idInString,
                    sessionInitializer,
                    conversionContext
            );
            addWorks.add(addWork);
        }
        
        return addWorks;
    }
    
    /**
     * Build criteria query using JPA criteria builder.
     * 
     * TODO: the type of entry array ids should be generic.
     * 
     * @param clazz the target class
     * @param ids the identifiers, of which the correspondent entities should be
     *          selected. 
     * @return the criteria query built
     */
    private <T> CriteriaQuery<T> buildCriteriaQuery(Class<T> clazz, int[] ids) {
        CriteriaQuery<T> q = em.getCriteriaBuilder().createQuery(clazz);
        Root<T> root = q.from(clazz);
        // TODO: get attribute id in generic type
        Path<Integer> attrId = root.get("addressId");
        In<Integer> inIds = em.getCriteriaBuilder().in(attrId);
        for (int id : ids) {
            inIds.value(id);
        }
        q.where(inIds);
        return q;
    }
    
    /**
     * Cast the serializable array into primitive integer array.
     * 
     * @param s serializable array
     * @return the primitive integer array
     */
    private int[] toIntArray(Serializable[] s){
        int[] array = new int[s.length];
        for(int i = 0; i < s.length; i++) {
            array[i] = (int) s[i];
        }
        return array;
    }
}
