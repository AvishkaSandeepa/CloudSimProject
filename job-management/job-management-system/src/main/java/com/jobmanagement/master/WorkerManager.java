package com.jobmanagement.master;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerManager {
    private static final List<WorkerNode> workers = new ArrayList<>();
    private static final ConcurrentHashMap<String, WorkerNode> assignJobs = new ConcurrentHashMap<>();

    public static synchronized void registerNewWorker(WorkerNode workerNode) {
        workers.add(workerNode);
        System.out.println("Registered worker: " + workerNode.getAddress() + ":" + workerNode.getPort());
    }
}
