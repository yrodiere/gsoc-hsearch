package io.github.mincongh.session;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.ejb.Asynchronous;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 * Session bean for different batch processes.
 * 
 * @author Mincong HUANG
 */
@Stateful
public class BatchSession {

    // Job operator is used to control different batch jobs
    private JobOperator jobOperator;
    
    // This particular EntityManager is injected as an EXTENDED persistence
    // context, which simply means that the EntityManager is created when the
    // @Stateful bean is created and destroyed when the @Stateful bean is
    // destroyed. Simply put, the data in the EntityManager is cached for the
    // lifetime of the @Stateful bean
    @PersistenceContext(unitName = "us-address", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;
    
    public BatchSession() {
        this.jobOperator = BatchRuntime.getJobOperator();
    }
    
    /**
     * Mass index the Address entity's.
     * <p>Here're some parameters and expected results:
     * <ul>
     * <li><b>array capacity</b> = 1000
     * 
     * <li><b>partition capacity</b> = 125
     * 
     * <li><b>max results</b> = 1 * 1000 * 1000
     * 
     * <li><b>queue size</b>
     *      = max results / array capacity
     *      = 1 * 1000 * 1000 / 1000
     *      = 1000
     * 
     * <li><b>number of partitions</b>
     *      = queue size / partition capacity
     *      = 1000 / 250
     *      = 4
     * 
     * <li><b>minimum checkpoint count (without stop) (not updated)</b>
     *      = (1 close + partition capacity / checkpoint per N items) * nb partitions
     *      = (1 + 50 / 10) * 4
     *      = 6 * 4
     *      = 24
     *      This number will be greater in reality, because some threads process
     *      more items than it capacity expected. And the checkpoint number will
     *      be increased. 
     *      
     * </ul>
     */
    @Asynchronous
    public void massIndex() {
        
        Long start = System.currentTimeMillis();
        
        Properties jobParams = new Properties();
        jobParams.setProperty("fetchSize", "200000");
        jobParams.setProperty("arrayCapacity", "1000");
        jobParams.setProperty("maxResults", "1000000");
        jobParams.setProperty("partitionCapacity", "250");
        jobParams.setProperty("threads", "4");
        jobParams.setProperty("purgeAtStart", String.valueOf(true));
        jobParams.setProperty("optimizeAfterPurge", String.valueOf(true));
        jobParams.setProperty("optimizeAtEnd", String.valueOf(true));
        jobParams.setProperty("rootEntitiesStr", getRootEntities().toString());
        Long executionId = jobOperator.start("mass-index", jobParams);
        
        // calculate the performance
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
        Long end = System.currentTimeMillis();
        System.out.printf("%d rounds, delta T = %d ms.%n", i, end - start);
    }
    
    @Asynchronous
    public void printAddressesTop1000() throws InterruptedException {
        long executionId = jobOperator.start("print-addresses-job", null);
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        int i = 0;
        while (!jobExecution.getBatchStatus().equals(BatchStatus.COMPLETED)
                && i < 10) {
            Thread.sleep(100);
            i++;
        }
        String msg = i <= 10 ? "Finished" : "Failed";
        System.out.println(msg);
    }
    
    private Set<String> getRootEntities() {
        Set<String> rootEntities = new HashSet<>();
        rootEntities.add("io.github.mincongh.entity.Address");
        rootEntities.add("io.github.mincongh.entity.Stock");
        return rootEntities;
    }
}
