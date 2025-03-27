package com.jobmanagement.worker;

import com.jobmanagement.shared.Job;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerServer {

    private final int port;
    private final String password;
    private final double initialBudget;
    private final JobExecutor jobExecutor;
    private final ConcurrentHashMap<String, Thread> runningJobs = new ConcurrentHashMap<>();
    private boolean running = true;

    public WorkerServer(int port, String password, double initialBudget) {
        this.port = port;
        this.password = password;
        this.initialBudget = initialBudget;
        this.jobExecutor = new JobExecutor();
    }

    public void start() {
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker server started on port " + port);

            registerWithMaster();

            while (running) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Worker server error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private void registerWithMaster() {
        try (Socket socket = new Socket("localhost", 8080);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("REGISTER_WORKER");
            out.writeObject("localhost");
            out.writeObject(port);
            out.writeObject(password);
            out.writeObject(initialBudget);

            String response = (String) in.readObject();
            if (!"REGISTERED".equals(response)) {
                System.err.println("Failed to register with master: " + response);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Registration error: " + e.getMessage());
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
                    case "EXECUTE_JOB":
                        System.out.println("test pass");
                        handleExecuteJob(in,out);
                        break;
                    case "JOB_STATUS":
                        handleJobStatus(in, out);
                        break;
                    case "CANCEL_JOB":
                        handleCancelJob(in, out);
                        break;
                    case "HEALTH_CHECK":
                        out.writeObject("OK");
                        break;
                    case "SHUTDOWN":
                        out.writeObject("SHUTTING_DOWN");
                        running = false;
                        break;
                    default:
                        out.writeObject("UNKNOWN_COMMAND");
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Client handler error: " + e.getMessage());
            }
        }
    }

    private void handleCancelJob(ObjectInputStream in, ObjectOutputStream out)
            throws IOException, ClassNotFoundException {
        String receivedPassword = (String) in.readObject();
        if (!password.equals(receivedPassword)) {
            out.writeObject("AUTH_FAILED");
            return;
        }

        String jobId = (String) in.readObject();
        if (runningJobs.containsKey(jobId)) {
            Thread runningThread = runningJobs.get(jobId);
            runningThread.interrupt();
            System.out.println("Job is canceled according to the user request. job id : " + jobId);
            runningJobs.remove(jobId);
            out.writeObject("CANCELED");
        } else {
            System.out.println("There is no running job for given job ID : " + jobId + "    job may be finished!");
            out.writeObject("FAILED");
        }
    }

    private void handleExecuteJob(ObjectInputStream in, ObjectOutputStream out)
            throws IOException, ClassNotFoundException {
        String receivedPassword = (String) in.readObject();
        if (!password.equals(receivedPassword)) {
            out.writeObject("AUTH_FAILED");
            return;
        }

        Job job = (Job) in.readObject();
        Thread jobThread = new Thread(() -> {
            jobExecutor.execute(job);
            runningJobs.remove(job.getId());

            if (jobExecutor.getStatus(job.getId()) == Job.Status.FAILED) {
                notifyMasterOfFailure(job);
            } else if (jobExecutor.getStatus(job.getId()) == Job.Status.COMPLETED) {
                notifyMasterOfCompleteness(job);
            }
        });

        runningJobs.put(job.getId(), jobThread);
        jobThread.start();
        System.out.println("Job execution starts " + job.getId());
        out.writeObject("ACCEPTED");
    }

    private void handleJobStatus(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String receivedPassword = (String) in.readObject();
        if (!password.equals(receivedPassword)) {
            out.writeObject("AUTH_FAILED");
            return;
        }

        String jobId = (String) in.readObject();
        Job.Status status = jobExecutor.getStatus(jobId);
        out.writeObject(status.name());
    }

    private void notifyMasterOfFailure(Job job) {
        try (Socket socket = new Socket("localhost", 8080);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("JOB_FAILED");
            out.writeObject(password);
            out.writeObject(job);

            String response = (String) in.readObject();
            if (!"ACK".equals(response)) {
                System.err.println("Failed to notify master of job failure");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error notifying master of failure: " + e.getMessage());
        }
    }

    private void notifyMasterOfCompleteness(Job job) {
        try (Socket socket = new Socket("localhost", 8080);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("JOB_COMPLETE");
            out.writeObject(password);
            out.writeObject(job);

            String response = (String) in.readObject();
            if (!"ACK".equals(response)) {
                System.err.println("Failed to notify master of job completion");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error notifying master of completion: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: WorkerServer <port> <password> <budget>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String password = args[1];
        double initialBudget = Double.parseDouble(args[2]);

        WorkerServer server = new WorkerServer(port, password, initialBudget);
        server.start();
//        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

}
