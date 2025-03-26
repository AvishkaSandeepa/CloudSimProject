package com.jobmanagement.master;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerManager {
    private final List<WorkerNode> workers = new ArrayList<>();
    private final ConcurrentHashMap<String, WorkerNode> assignJobs = new ConcurrentHashMap<>();
    private int currentIndex = 0;

    public List<WorkerNode> getWorkers() {
        return workers;
    }

    public synchronized void registerNewWorker(WorkerNode workerNode) {
        workers.add(workerNode);
        System.out.println("Registered worker: " + workerNode.getAddress() + ":" + workerNode.getPort());
    }

    public synchronized WorkerNode getNextAvailableWorker() {
        if (workers.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % workers.size(); // round-robin algorithm for worker selection
        return workers.get(currentIndex);
    }

    public synchronized void assignJob(String jobId, WorkerNode workerNode) {
        assignJobs.put(jobId, workerNode);
        workerNode.setStatus("RUNNING");
        System.out.println("Job : " + jobId + " is assigned to a worker");
    }

    public synchronized void removeFailedJobs(String jobId) {
        assignJobs.remove(jobId);
        System.out.println("Job : " + jobId + " is removed from a worker due to job failure detection");
    }

    public synchronized void removeCanceledJobs(String jobId) {
        assignJobs.remove(jobId);
        System.out.println("Job : " + jobId + " is removed from a worker due to job cancellation");
    }

    public synchronized int getWorkPort(String jobId) {
        return assignJobs.get(jobId).getPort();
    }

    public synchronized WorkerNode getWorkNodeOfRunningJob(String jobId) {
        return assignJobs.getOrDefault(jobId, null);
    }

    public void updateStatusOfCompleteJobInJObCache(String jobId) {
        WorkerNode worker = getWorkNodeOfRunningJob(jobId);
        worker.updateStatusOfJobCache(jobId);
    }

    public String getWorkerInfo() {
        StringBuilder report = new StringBuilder();
        report.append("Worker Address | Port | Status | # Running Jobs | # Completed Jobs | # Canceled Jobs\n");
        report.append("------------------------------------------------------------------------------------\n");

        for (WorkerNode worker : workers) {
            int runningJobs = 0;
            int completedJobs = 0;
            int canceledJobs = 0;

            for (ConcurrentHashMap.Entry<String, String> entry : worker.getJobStatusCache().entrySet()) {
                if ("RUNNING".equalsIgnoreCase(entry.getValue())) {
                    runningJobs++;
                } else if ("COMPLETED".equalsIgnoreCase(entry.getValue())) {
                    completedJobs++;
                }  else if ("CANCELED".equalsIgnoreCase(entry.getValue())) {
                    canceledJobs++;
                }
            }

            // Get worker status (assuming simple logic - you can enhance this)
            String status = worker.getStatus();

            report.append(String.format("%-15s| %-5d| %-8s| %-15d| %-15d| %-15d%n",
                    worker.getAddress(),
                    worker.getPort(),
                    status,
                    runningJobs,
                    completedJobs,
                    canceledJobs));
        }

        return report.toString();
    }
}
