package org.hibernate.search.jsr352.internal;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.batch.api.chunk.ItemProcessor;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
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
import org.jboss.logging.Logger;

/**
 * Batch item processor loads entities using entity IDs, provided by the item
 * reader. Please notice: this process is running under multiple partitions,
 * so there're multiple processors running currently. The input IDs are not
 * shared by different processors. And theses IDs are given by the item reader
 * located in the same partition.
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

    private EntityManager em;
    private Session session;
    private ExtendedSearchIntegrator searchIntegrator;
    private EntityIndexBinding entityIndexBinding;
    private StepContext stepContext;

    private static final Logger logger = Logger.getLogger(BatchItemProcessor.class);

    @Inject
    public BatchItemProcessor(StepContext stepContext, IndexingContext indexingContext) {
        this.stepContext = stepContext;
        this.em = indexingContext.getEntityManager();
    }

    /**
     * Process an input item into an output item. Here, the input item is an
     * array of IDs and the output item is a list of Lucene works. During the
     * process, entities are found by an injected entity manager, then they
     * are used for building the correspondent Lucene works.
     *
     * @param item the input item, an array of IDs
     * @return a list of Lucene works
     * @throws Exception thrown for any errors.
     */
    @Override
    public Object processItem(Object item) throws Exception {

        Class<?> entityClazz = ( (EntityIndexingStepData) stepContext.getTransientUserData() ).getEntityClass();
        logger.debugf( "processItem(Object) called. entityType=%s", entityClazz );
        AddLuceneWork addWork = buildAddLuceneWork( item, entityClazz );
        updateWorksCount(1);
        return addWork;
    }

    /**
     * Update the Lucene Works counts using the step context.
     *
     * @param currentCount the works processed during the current
     *          processItem().
     */
    private void updateWorksCount(int currentCount) {
        EntityIndexingStepData userData = (EntityIndexingStepData) stepContext.getTransientUserData();
        userData.incrementProcessedWorkCount( currentCount );
    }

    /**
     * Build addLuceneWork using input entity. This method is inspired by the
     * current mass indexer implementation.
     *
     * @param entity selected entity, obtained from JPA entity manager. It is
     *          used to build Lucene work.
     * @param entityClazz the class type of selected entity
     * @return an addLuceneWork
     */
    private AddLuceneWork buildAddLuceneWork(Object entity, Class<?> entityClazz) {

        // TODO: tenant ID should not be null
        // Or may it be fine to be null? Gunnar's integration test in Hibernate
        // Search: MassIndexingTimeoutIT does not mention the tenant ID neither
        // (The tenant ID is not included mass indexer setup in the ConcertManager)
        String tenantId = null;

        session = em.unwrap(Session.class);
        searchIntegrator = ContextHelper.getSearchintegrator(session);
        entityIndexBinding = searchIntegrator
                .getIndexBindings()
                .get(entityClazz);

        DocumentBuilderIndexedEntity docBuilder = entityIndexBinding.getDocumentBuilder();
        ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
        final InstanceInitializer sessionInitializer = new HibernateSessionLoadingInitializer(
                (SessionImplementor) session
        );

        Serializable id = session.getIdentifier(entity);
        TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
        conversionContext.pushProperty(docBuilder.getIdKeywordName());
        String idInString = null;
        try {
            idInString = conversionContext
                    .setClass(entityClazz)
                    .twoWayConversionContext(idBridge)
                    .objectToString(id);
            logger.infof("idInString=%s", idInString);
        } finally {
            conversionContext.popProperty();
        }
        AddLuceneWork addWork = docBuilder.createAddWork(
                tenantId,
                entityClazz,
                entity,
                id,
                idInString,
                sessionInitializer,
                conversionContext
        );
        return addWork;
    }
}
