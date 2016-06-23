package org.hibernate.search.jsr352;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.hibernate.search.jsr352.MassIndexer;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.hibernate.search.jsr352.test.entity.Address;
import org.hibernate.search.jsr352.test.entity.Stock;
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
    
    private static final Logger logger = Logger.getLogger(DeploymentIT.class);
    
    @Deployment
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addPackages(true, "org.hibernate.search.jsr352")
                .addPackages(true, "javax.persistence")
                .addPackages(true, "org.hibernate.search.annotations")
//              .addClasses(Address.class, Stock.class)
                .addClass(Serializable.class)
                .addClass(Date.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml")
                .addAsResource("META-INF/batch-jobs/mass-index.xml");
        return war;
    }
    
    @Test
    public void testJobStart() {
        // start job
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
        long executionId = massIndexer.start();
        
        // calculate the performance
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        JobExecution execution = jobOperator.getJobExecution(executionId);
        int i = 0; 
        while (!execution.getBatchStatus().equals(BatchStatus.COMPLETED) && i < 200) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            i++;
        }
        logger.info("Finished");
    }
    
    private Set<Class<?>> getRootEntities() {
        Set<Class<?>> rootEntities = new HashSet<>();
        rootEntities.add(Address.class);
        rootEntities.add(Stock.class);
        return rootEntities;
    }
    
    
}
