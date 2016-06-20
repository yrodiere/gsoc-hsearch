package org.hibernate.search.jsr352;

import org.hibernate.search.store.IndexShardingStrategy;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class DeploymentIT {

    private final boolean OPTIMIZE_AFTER_PURGE = true;
    private final boolean OPTIMIZE_AT_END = true;
    private final boolean PURGE_AT_START = true;
    private final int ARRAY_CAPACITY = 500;
    private final int FETCH_SIZE = 100000;
    private final int MAX_RESULTS = 1000000;
    private final int PARTITION_CAPACITY = 500;
    private final int PARTITIONS = 4;
    private final int THREADS = 2;
    
    private static final Logger logger = Logger.getLogger(DeploymentIT.class);
    
    @Deployment
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                // deprecated classes is not shipped in the war, add manually
                .addClasses(IndexShardingStrategy.class)
                .addPackages(true, "org.hibernate.search.jsr352")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsResource("META-INF/batch-jobs/mass-index.xml");
        logger.info(war.toString(true));
        return war;
    }
    
    @Test
    public void testJobStart() {
        MassIndexer massIndexer = new MassIndexerImpl()
                .arrayCapacity(ARRAY_CAPACITY)
                .fetchSize(FETCH_SIZE)
                .maxResults(MAX_RESULTS)
                .optimizeAfterPurge(OPTIMIZE_AFTER_PURGE)
                .optimizeAtEnd(OPTIMIZE_AT_END)
                .partitionCapacity(PARTITION_CAPACITY)
                .partitions(PARTITIONS)
                .purgeAtStart(PURGE_AT_START)
                .threads(THREADS);
    }
}
