package org.hibernate.search.jsr352.internal;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.jboss.logging.Logger;

/**
 * TODO: update description.
 *
 * Read entity IDs from {@code IndexingContext}. Each time, there's one array
 * being read. The number of IDs inside the array depends on the array capacity.
 * This value is defined before the job start. Either the default value defined
 * in the job xml will be applied, or the value overwritten by the user in job
 * parameters. These IDs will be processed in {@code BatchItemProcessor}, then
 * be used for Lucene document production.
 * <p>
 * The motivation of using an array of IDs over a single ID is to accelerate
 * the entity processing. Use a SELECT statement to obtain only one ID is
 * rather a waste. For more detail about the entity process, please check {@code
 * BatchItemProcessor}.
 *
 * @author Mincong HUANG
 */
@Named
public class BatchItemReader implements ItemReader {

    private static final Logger logger = Logger.getLogger(BatchItemReader.class);

    @Inject @BatchProperty(name="entityType")
    private String entityName;
    @Inject @BatchProperty
    private int maxResults;
    private Class<?> entityClazz;
    private JobContext jobContext;
    private IndexingContext indexingContext;
    private Serializable checkpointId;

    private EntityManager em;
    private Session session;
    private StatelessSession ss;
    private ScrollableResults scroll;

	@Inject
	public BatchItemReader(JobContext jobContext, IndexingContext indexingContext) {
	    this.jobContext = jobContext;
		this.indexingContext = indexingContext;
		this.em = indexingContext.getEntityManager();
	}

	/**
     * The checkpointInfo method returns the current checkpoint data for this
     * reader. It is called before a chunk checkpoint is committed.
     *
     * @return the checkpoint info
     * @throws Exception thrown for any errors.
     */
    @Override
    public Serializable checkpointInfo() throws Exception {
        logger.info("checkpointInfo() called. "
                + "Saving last read ID to batch runtime...");
        return checkpointId;
    }

    /**
     * Close operation(s) before the class destruction.
     *
     * @throws Exception thrown for any errors.
     */
    @Override
    public void close() throws Exception {
        logger.info( "closing everything..." );
        try {
            scroll.close();
            logger.info( "Scrollable results closed." );
        } catch (Exception e) {
            logger.error(e);
        }
        try {
            ss.close();
            logger.info( "Stateless session closed." );
        } catch (Exception e) {
            logger.error(e);
        }
        try {
            session.close();
            logger.info( "Session closed" );
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Initialize the environment. If checkpoint does not exist, then it should
     * be the first open. If checkpoint exist, then it isn't the first open,
     * save the input object "checkpoint" into "tempIDs".
     *
     * @param checkpoint The last checkpoint info saved in the batch runtime,
     *          previously given by checkpointInfo(). If this is the first
     *          start, then the checkpoint will be null, so does lastId.
     * @throws Exception thrown for any errors.
     */
    @Override
    public void open(Serializable checkpoint) throws Exception {
        logger.infof( "open reader for entityName=%s", entityName );
        checkpointId = checkpoint;
        session = indexingContext.getEntityManager().unwrap( Session.class );
        ss = session.getSessionFactory().openStatelessSession();

        entityClazz = ( (BatchContextData) jobContext.getTransientUserData() ).getIndexedType( entityName );

//        // Idea 1
//        // I can't use the below line because I don't have the "instance" :
//        em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(instance);

//        // Idea 2
//        // I've tried the below line too
//        // But I can't use it because sessionFactory is null
//        ClassMetadata m = sessionFactory.getClassMetadata( entityType );
        
        scroll = ss.createCriteria( entityClazz )
//              .add( Restrictions.gt( m.getIdentifierPropertyName(), checkpointId ))
                .setReadOnly( true )
                .setCacheable( true )
                .setFetchSize( 1 )
                .setMaxResults( maxResults )
                .scroll( ScrollMode.FORWARD_ONLY );
        
//        while ( scrollableResults.next() ) {
//            logger.info(entityClazz.cast(scrollableResults.get(0)));
//        }
    }

    /**
     * Read item from database using JPA. Each read, there will be only one
     * entity fetched.
     *
     * @throws Exception thrown for any errors.
     */
    @Override
    public Object readItem() throws Exception {
        logger.info( "Reading item ..." );
        Object entity = null;
        if ( scroll.next() ) {
            entity = scroll.get(0);
            checkpointId = (Serializable) em.getEntityManagerFactory()
                .getPersistenceUnitUtil()
                .getIdentifier( entity );
        } else {
            logger.info( "no more result. read ends.");
        }
        return entity;
    }
}
