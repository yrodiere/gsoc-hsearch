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
     * Load Address entity's ids and print them in the console. 
     */
    @Asynchronous
    public void printId() {
        Properties jobParams = new Properties();
        jobParams.setProperty("fetchSize", "10000");
        jobParams.setProperty("listCapacity", "5000");
        jobOperator.start("print-address-id", jobParams);
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
