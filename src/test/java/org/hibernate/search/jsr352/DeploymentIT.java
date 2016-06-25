package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.inject.Inject;

import org.hibernate.search.jsr352.MassIndexer;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.hibernate.search.jsr352.internal.IndexingContext;
import org.hibernate.search.jsr352.test.entity.Address;
import org.hibernate.search.jsr352.test.entity.Stock;
import org.hibernate.search.jsr352.test.util.BatchTestHelper;
import org.hibernate.search.store.IndexShardingStrategy;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
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
    
    private final long DB_ADDRESS_ROWS = 3221316;
    private final long DB_ADDRESS_ROWS_LOADED = 1000000;
    private final long DB_STOCK_ROWS = 4194;
    
    @Inject private IndexingContext indexingContext;

    private static final Logger logger = Logger.getLogger(DeploymentIT.class);
    
    @Deployment
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addPackages(true, "org.hibernate.search.jsr352")
                .addPackages(true, "javax.persistence")
                .addPackages(true, "org.hibernate.search.annotations")
                .addClass(Serializable.class)
                .addClass(Date.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
                .addAsResource("META-INF/batch-jobs/mass-index.xml");
        return war;
    }
    
    @Test
    public void testJobStart() throws InterruptedException {
        
        // start job
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        MassIndexer massIndexer = createAndInitJob();
        long executionId = massIndexer.start();
        
        // wait until job finishes
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        jobExecution = BatchTestHelper.keepTestAlive(jobExecution);
        
        // tests
        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);
        for (StepExecution stepExecution: stepExecutions) {
            switch (stepExecution.getStepName()) {
                
                case "loadId":
                    assertEquals(DB_ADDRESS_ROWS + DB_STOCK_ROWS, indexingContext.getEntityCount());
                    break;
                    
                case "produceLuceneDoc":
                    Metric[] metrics = stepExecution.getMetrics();
                    testChunk(BatchTestHelper.getMetricsMap(metrics));
                    break;
                    
                default:
                    break;
            }
        }
        assertEquals(jobExecution.getBatchStatus(), BatchStatus.COMPLETED);
        logger.info("Finished");
    }
    
    private void testChunk(Map<Metric.MetricType, Long> metricsMap) {
        long addressCount = (long) Math.ceil((double) DB_ADDRESS_ROWS_LOADED / ARRAY_CAPACITY);
        long stockCount = (long) Math.ceil((double) DB_STOCK_ROWS / ARRAY_CAPACITY);
        // The read count.
        long expectedReadCount = addressCount + stockCount;
        long actualReadCount = metricsMap.get(Metric.MetricType.READ_COUNT);
        assertEquals(expectedReadCount, actualReadCount);
        // The write count
        // TODO: make BatchItemProcessor generic in order to process the 
        // entity `stock` in the current implementation
        long expectedWriteCount = addressCount + 0;
        long actualWriteCount = metricsMap.get(Metric.MetricType.WRITE_COUNT);
        assertEquals(expectedWriteCount, actualWriteCount);
    }
    
    private MassIndexer createAndInitJob() {
        MassIndexer massIndexer = new MassIndexerImpl()
                .arrayCapacity(ARRAY_CAPACITY)
                .fetchSize(FETCH_SIZE)
                .maxResults(MAX_RESULTS)
                .optimizeAfterPurge(OPTIMIZE_AFTER_PURGE)
                .optimizeAtEnd(OPTIMIZE_AT_END)
                .partitionCapacity(PARTITION_CAPACITY)
                .partitions(PARTITIONS)
                .purgeAtStart(PURGE_AT_START)
                .threads(THREADS)
                .rootEntities(getRootEntities());
        return massIndexer;
    }
    
    private Set<Class<?>> getRootEntities() {
        Set<Class<?>> rootEntities = new HashSet<>();
        rootEntities.add(Address.class);
        rootEntities.add(Stock.class);
        return rootEntities;
    }
}
