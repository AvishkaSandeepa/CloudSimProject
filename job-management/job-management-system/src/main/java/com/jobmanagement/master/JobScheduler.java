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
    private final CountDownLatch batchReadySignal = new CountDownLatch(1);

    public JobScheduler(WorkerManager workerManager) {
        this.workerManager = workerManager;
    }

    // ==== Batch Insertion ====
    public void addJobsBatch(List<Job> jobs) {
        jobQueue.addAll(jobs); // Bulk insert (non-blocking)
        jobQueueCache.addAll(jobs); // Track all jobs
        System.out.println("Added " + jobs.size() + " jobs to queue");
        batchReadySignal.countDown(); // Signal workers to start (if waiting)
    }

    // ==== Single Job Insertion (backward-compatible) ====
    public void addJob(Job job) {
        jobQueue.put(job); // Blocking insert (if queue is full)
        jobQueueCache.add(job);
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
        WorkerNode worker = workerManager.getNextAvailableWorker(job);
        if (worker != null) {
            if (Objects.isNull(worker.getAddress()) && worker.getBudget() == -1.0) {
                System.err.println("All workers are out of budget. Job is added to queue ...... ");
                System.err.println("Waiting for another worker to add ... ");
                jobQueue.put(job);
            } else {
                workerManager.assignJob(job.getId(), worker);
                if (worker.executeJob(job)) {
                    System.out.println("Scheduled job " + job.getId() + " on worker " + worker.getAddress() + worker.getPort());
                } else {
                    System.out.println("Failed to schedule job " + job.getId() + ", re-queuing");
                    jobQueue.put(job);
                }
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
        try {
            batchReadySignal.await(); // Wait until batch is loaded (optional)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

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