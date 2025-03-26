package com.jobmanagement.worker;

import com.jobmanagement.shared.Job;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JobExecutor {
    private final ConcurrentMap<String, Job.Status> statusMap = new ConcurrentHashMap<>();

    public void execute(Job job) {
        statusMap.put(job.getId(), Job.Status.RUNNING);
        job.setJobStatus(Job.Status.RUNNING);

        try {
            // simulation only
            System.out.println("Executing job: " + job.getId() + " - " + job.getCommand());
            Thread.sleep(50000); // Simulate 10 seconds of work

            if (Thread.currentThread().isInterrupted()) {
                statusMap.put(job.getId(), Job.Status.CANCELLED);
                job.setJobStatus(Job.Status.CANCELLED);
                return;
            }

            statusMap.put(job.getId(), Job.Status.COMPLETED);
            job.setJobStatus(Job.Status.COMPLETED);
        } catch (InterruptedException e) {
            statusMap.put(job.getId(), Job.Status.CANCELLED);
            job.setJobStatus(Job.Status.CANCELLED);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            statusMap.put(job.getId(), Job.Status.FAILED);
            job.setJobStatus(Job.Status.FAILED);
        }
    }

    public Job.Status getStatus(String jobId) {
        return statusMap.getOrDefault(jobId, Job.Status.UNKNOWN);
    }
}
