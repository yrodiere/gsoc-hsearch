package io.github.mincongh.session;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.ejb.Asynchronous;
import javax.ejb.Stateful;

/**
 * Session bean for different batch processes.
 * 
 * @author Mincong HUANG
 */
@Stateful
public class BatchSession {

    private JobOperator jobOperator;
    
    public BatchSession() {
        this.jobOperator = BatchRuntime.getJobOperator();
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
