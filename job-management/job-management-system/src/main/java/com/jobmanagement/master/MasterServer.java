package com.jobmanagement.master;

import com.jobmanagement.shared.Job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterServer {
    private static final int PORT = 8080;
    private static final String WORKER_PASSWORD = "master@123";
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private boolean running = true;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Master server started on port " + PORT);

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
//                    case "SUBMIT_JOB":
//                        Job job = (Job) in.readObject();
//                        jobScheduler.addJob(job);
//                        out.writeObject("ACCEPTED");
//                        break;
//                    case "JOB_STATUS":
//                        String jobId = (String) in.readObject();
//                        WorkerNode worker = workerManager.getWorkerForJob(jobId);
//                        out.writeObject(worker != null ? worker.getJobStatus(jobId) : "UNKNOWN");
//                        break;
//                    case "CANCEL_JOB":
//                        String cancelJobId = (String) in.readObject();
//                        out.writeObject(jobScheduler.cancelJob(cancelJobId) ? "CANCELED" : "FAILED");
//                        break;
//                    case "LIST_WORKERS":
//                        out.writeObject(workerManager.getWorkerInfo());
//                        break;
                    case "HEALTH_CHECK":
                        out.writeObject("OK");
                        break;
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

            if (WORKER_PASSWORD.equals(receivedPassword)) {
                WorkerNode workerNode = new WorkerNode(workerAddress, workerPort, receivedPassword);
                WorkerManager.registerNewWorker(workerNode);
                out.writeObject("REGISTERED");
            } else {
                out.writeObject("AUTH_FAIL");
            }

        }
    }

    public static void main(String[] args) {
        MasterServer server = new MasterServer();
        server.start();
//        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}