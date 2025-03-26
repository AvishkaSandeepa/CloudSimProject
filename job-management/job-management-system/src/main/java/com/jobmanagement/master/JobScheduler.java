package com.jobmanagement.master;


import com.jobmanagement.shared.Job;

import java.util.*;
import java.util.concurrent.*;

public class JobScheduler implements Runnable {
    private final PriorityBlockingQueue<Job> jobQueue =
            new PriorityBlockingQueue<>(100, Comparator.comparing(Job::getDeadline));
    private final Queue<Job> jobQueueCache = new ConcurrentLinkedQueue<>();
    private final WorkerManager workerManager;
    private volatile boolean running = true;

    public JobScheduler(WorkerManager workerManager) {
        this.workerManager = workerManager;
    }

    public void addJob(Job job) {
        jobQueue.put(job);
        jobQueueCache.add(job); // Keep recording of all jobs executed
        System.out.println("Added job to queue: " + job.getId());
    }

    public void addCorruptedJobs(List<String> jobIds) {
        Set<String> jobIdSet = new HashSet<>(jobIds);
        for (Job job : jobQueueCache) {
            if (jobIdSet.contains(job.getId())) {
                jobQueue.put(job);
                System.out.println("Re-queue the job: " + job.getId() + " from corrupted worker");
            }
        }
    }

    private void scheduleJob(Job job) {
        WorkerNode worker = workerManager.getNextAvailableWorker();
        if (worker != null) {
            workerManager.assignJob(job.getId(), worker);
            if (worker.executeJob(job)) {
                System.out.println("Scheduled job " + job.getId() + " on worker " + worker.getAddress() + worker.getPort());
            } else {
                System.out.println("Failed to schedule job " + job.getId() + ", re-queuing");
                jobQueue.put(job);
            }
        } else {
            System.out.println("No available workers for job " + job.getId() + ", re-queuing");
            jobQueue.put(job);
        }
    }

    public boolean cancelJob(String jobId) {
        WorkerNode worker = workerManager.getWorkNodeOfRunningJob(jobId);
        if (worker != null) {
            if (worker.cancelJob(jobId)) {
                System.out.println("Canceled job " + jobId + " on worker " + worker.getAddress() + worker.getPort());
                return true;
            } else {
                System.out.println("Failed to cancel job " + jobId);
                return false;
            }
        } else {
            System.out.println("Job " + jobId + " is not running on a worker");
            return false;
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Job job = jobQueue.take();
                scheduleJob(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

}