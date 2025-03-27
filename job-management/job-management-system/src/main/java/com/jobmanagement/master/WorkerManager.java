package com.jobmanagement.master;

import com.jobmanagement.shared.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerManager {
    private final List<WorkerNode> workers = new ArrayList<>();
    private final List<WorkerNode> workersCache = new ArrayList<>();
    private final ConcurrentHashMap<String, WorkerNode> assignJobs = new ConcurrentHashMap<>();
    private int currentIndex = 0;

    public List<WorkerNode> getWorkers() {
        return workers;
    }

    public synchronized void registerNewWorker(WorkerNode workerNode) {
        workers.add(workerNode);
        workersCache.add(workerNode); // add all registered workers for a given session as reference
        System.out.println("Registered worker: " + workerNode.getAddress() + ":" + workerNode.getPort());
    }

    public synchronized WorkerNode getNextAvailableWorker(Job job) {
        if (workers.isEmpty()) return null;
        List<WorkerNode> eligibleWorkers = workers.stream()
                .filter(worker -> worker.getBudget() >= job.getBudget())
                .toList();
        if (eligibleWorkers.isEmpty()) return new WorkerNode(null, 0, null, -1.0);
        currentIndex = (currentIndex + 1) % eligibleWorkers.size(); // round-robin algorithm for worker selection
        System.out.println("current available budget for worker : " + eligibleWorkers.get(currentIndex).getBudget());
        return eligibleWorkers.get(currentIndex);
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

    public synchronized List<String> rescheduleJobsForAvailableWorkers(WorkerNode workerNode) {
        workers.remove(workerNode);
        List<String> keysToRemove = assignJobs.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getPort() == workerNode.getPort())
                .map(ConcurrentHashMap.Entry::getKey)
                .toList();

        assignJobs.keySet().removeAll(keysToRemove);
        return keysToRemove;
    }

    public String getWorkerInfo() {
        StringBuilder report = new StringBuilder();
        report.append("Worker Address | Port | Status | # Running Jobs | # Completed Jobs | # Canceled Jobs\n");
        report.append("------------------------------------------------------------------------------------\n");

        for (WorkerNode worker : workersCache) {
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

            String status = worker.getStatus();
            if (status.equals("DEAD")) runningJobs = 0;

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
