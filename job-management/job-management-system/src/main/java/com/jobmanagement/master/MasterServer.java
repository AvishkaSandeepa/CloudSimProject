package com.jobmanagement.master;

import com.jobmanagement.shared.Job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterServer {
    private static final int PORT = 8080;
    private static final String WORKER_PASSWORD = "master@123";
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private boolean running = true;
    private final WorkerManager workerManager;
    private final JobScheduler jobScheduler;
    private final HealthMonitor healthMonitor;
    List<Job> jobs = new ArrayList<>();

    public MasterServer() {
        this.workerManager = new WorkerManager();
        this.jobScheduler = new JobScheduler(workerManager);
        this.healthMonitor = new HealthMonitor(workerManager, jobScheduler);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Master server started on port " + PORT);

            new Thread(jobScheduler).start(); // start execution of the jobScheduling in separate thread
            new Thread(healthMonitor).start();

            while (running) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Master server error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                String command = (String) in.readObject();

                switch (command) {
                    case "REGISTER_WORKER":
                        handleWorkerRegistration(in, out);
                        break;
                    case "SUBMIT_JOB":
                        Job job = (Job) in.readObject();
                        fetchAllJobs(job, out);
                        break;
                    case "DONE_BATCH":
                        handleJobSubmission(out);
                        break;
                    case "SUBMIT_JOB_FILE":
                        while (true) {
                            String line = (String) in.readObject();
                            if ("END_OF_FILE".equals(line)) break;
                            Job jobFromFile = parseJobFromLine(line);
                            fetchAllJobs(jobFromFile, out);
                        }
                        out.writeObject("FILE_JOBS_ACCEPTED");
                        break;
                    case "JOB_FAILED":
                        String failedJobPwd = (String) in.readObject();
                        if (WORKER_PASSWORD.equals(failedJobPwd)) {
                            Job failedJob = (Job) in.readObject();
                            workerManager.removeFailedJobs(failedJob.getId());
                            System.out.println("Job failed ... re join the queue : " + failedJob.getId() + "   " + failedJob.getCommand());
                            jobScheduler.addJob(failedJob);
                            out.writeObject("ACK");
                        } else {
                            out.writeObject("AUTH_FAILED");
                        }
                        break;
                    case "JOB_COMPLETE":
                        String completeJobPwd = (String) in.readObject();
                        if (WORKER_PASSWORD.equals(completeJobPwd)) {
                            Job completeJob = (Job) in.readObject();
                            workerManager.updateStatusOfCompleteJobInJObCache(completeJob.getId());
                            System.out.println("Successfully complete the job : " + completeJob.getId() + " : " + completeJob.getCommand() + " by worker at port : " + workerManager.getWorkPort(completeJob.getId()));
                            out.writeObject("ACK");
                        } else {
                            out.writeObject("AUTH_FAILED");
                        }
                        break;
                    case "JOB_STATUS":
                        String jobId = (String) in.readObject();
                        WorkerNode worker = workerManager.getWorkNodeOfRunningJob(jobId);
                        out.writeObject(worker != null ? worker.getJobStatus(jobId) : "UNKNOWN");
                        break;
                    case "CANCEL_JOB":
                        String cancelJobId = (String) in.readObject();
                        out.writeObject(jobScheduler.cancelJob(cancelJobId) ? "CANCELED" : "FAILED");
                        break;
                    case "LIST_WORKERS":
                        out.writeObject(workerManager.getWorkerInfo());
                        break;
//                    case "HEALTH_CHECK":
//                        out.writeObject("OK");
//                        break;
                    default:
                        out.writeObject("UNKNOWN_COMMAND");
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Client handler error: " + e.getMessage());
            }

        }

        private void handleWorkerRegistration(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
            String workerAddress = (String) in.readObject();
            int workerPort = (int) in.readObject();
            String receivedPassword = (String) in.readObject();
            double initialBudget = (double) in.readObject();

            if (WORKER_PASSWORD.equals(receivedPassword)) {
                WorkerNode workerNode = new WorkerNode(workerAddress, workerPort, receivedPassword, initialBudget);
                workerManager.registerNewWorker(workerNode);
                out.writeObject("REGISTERED");
            } else {
                out.writeObject("AUTH_FAIL");
            }

        }

        private void fetchAllJobs(Job job, ObjectOutputStream out) throws IOException {
            try {
                System.out.println(job.getDeadline());
                jobs.add(job);
                out.writeObject("ACCEPTED");
            } catch (IOException e) {
                out.writeObject("REJECTED");
            }
        }

        private void handleJobSubmission(ObjectOutputStream out) throws IOException {
            try {
                if (!jobs.isEmpty()) {
                    jobScheduler.addJobsBatch(jobs);
                    jobs.clear();
                    out.writeObject("ACCEPTED");
                } else {
                    out.writeObject("NO_JOB_FOR_BATCH_PROCESS");
                }
            } catch (IOException e) {
                out.writeObject("REJECTED");
            }
        }
    }

    public static Job parseJobFromLine(String line) throws IllegalArgumentException {
        DateTimeFormatter TIME_FORMATTER =
                DateTimeFormatter.ofPattern("HH:mm:ss");
        // Split the line into components
        String[] parts = line.split("\\s+", 3); // Split on whitespace, max 3 parts

        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Invalid job format. Expected: <command> <deadline(HH:mm:ss)> <budget>");
        }

        try {
            String command = parts[0];
            LocalTime deadline = LocalTime.parse(parts[1], TIME_FORMATTER);
            double budget = Double.parseDouble(parts[2]);

            String jobId = UUID.randomUUID().toString();

            return new Job(jobId, command, deadline, budget);

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid deadline format. Expected HH:mm:ss", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid budget format. Expected numeric value", e);
        }
    }

    public static void main(String[] args) {
        MasterServer server = new MasterServer();
        server.start();
//        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}