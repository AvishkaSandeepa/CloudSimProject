package com.jobmanagement.master;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class HealthMonitor implements Runnable{
    private final ConcurrentHashMap<Integer, Long> lastAvailableTime = new ConcurrentHashMap<>();
    private final WorkerManager workerManager;
    private final JobScheduler jobScheduler;

    public HealthMonitor(WorkerManager workerManager, JobScheduler jobScheduler) {
        this.workerManager = workerManager;
        this.jobScheduler = jobScheduler;
    }

    public ConcurrentHashMap<Integer, Long> getLastAvailableTime() {
        return lastAvailableTime;
    }

    private void updateWorkerNodeStatus (WorkerNode workerNode) {
        boolean allRunning = workerNode.getJobStatusCache()
                .values()
                .stream()
                .allMatch(status -> "RUNNING".equals(status));

        if (!allRunning) {
            workerNode.setStatus("ACTIVE");
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<WorkerNode> workers = workerManager.getWorkers();
                for (WorkerNode workerNode : workers) {
                    updateWorkerNodeStatus(workerNode);
                    if (workerNode.healthCheck()) {
                        if (lastAvailableTime.containsKey(workerNode.getPort())) {
                            lastAvailableTime.put(workerNode.getPort(), System.currentTimeMillis());
                        } else {
                            lastAvailableTime.put(workerNode.getPort(), 0L);
                        }
                    } else {
                        long lastAvailable = lastAvailableTime.getOrDefault(workerNode.getPort(), 0L);
                        if (System.currentTimeMillis() - lastAvailable > 10000) {
                            System.out.println("No callback from given worker at port : " + workerNode.getPort());
                            workerNode.setStatus("DEAD");
                            List<String> waitingJobs = workerManager.rescheduleJobsForAvailableWorkers(workerNode);
                            jobScheduler.addCorruptedJobs(waitingJobs);
                        }
                    }
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
