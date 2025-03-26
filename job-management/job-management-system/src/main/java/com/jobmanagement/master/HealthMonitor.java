package com.jobmanagement.master;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class HealthMonitor implements Runnable{
    private final ConcurrentHashMap<Integer, Long> lastAvailableTime = new ConcurrentHashMap<>();
    private final WorkerManager workerManager;

    public HealthMonitor(WorkerManager workerManager) {
        this.workerManager = workerManager;
    }

    public ConcurrentHashMap<Integer, Long> getLastAvailableTime() {
        return lastAvailableTime;
    }

    private void updateWorkerNodeStatus (WorkerNode workerNode) {
        for (ConcurrentHashMap.Entry<String, String> entry : workerNode.getJobStatusCache().entrySet()) {
            if (Objects.equals(entry.getValue(), "RUNNING")) {
                continue;
            } else {
                workerNode.setStatus("ACTIVE");
            }
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
