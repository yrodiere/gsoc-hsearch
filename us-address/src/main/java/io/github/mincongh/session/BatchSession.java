package io.github.mincongh.session;

import java.util.Properties;

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
     * <li><b>array capacity</b> = 500
     * 
     * <li><b>partition capacity</b> = 50
     * 
     * <li><b>max results</b> = 100 * 1000
     * 
     * <li><b>queue size</b>
     *      = max results / array capacity
     *      = 100 * 1000 / 500
     *      = 200
     * 
     * <li><b>number of partitions</b>
     *      = queue size / partition capacity
     *      = 200 / 50
     *      = 4
     * 
     * <li><b>minimum checkpoint count (without stop)</b>
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
        Properties jobParams = new Properties();
        jobParams.setProperty("fetchSize", "1000");
        jobParams.setProperty("arrayCapacity", "500");
        jobParams.setProperty("maxResults", "100000");
        jobParams.setProperty("partitionCapacity", "50");
        jobParams.setProperty("threads", "4");
        jobOperator.start("mass-index", jobParams);
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
}
