package com.jobmanagement.master;

import com.jobmanagement.shared.Job;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;

public class MasterCLI {
    private static final int MASTER_PORT = 8080;
    private static final String MASTER_ADDRESS = "localhost";


    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            while (true) {
                System.out.println("master > ");
                String input = reader.readLine().trim();

                if ("exit".equalsIgnoreCase(input)) {
                    System.out.println("Exit from master CLI ... ");
                    break;
                }

                String[] parts = input.split(" ");
                if (parts.length < 1) {
                    System.out.println("invalid number of commands ! try again");
                    continue;
                }

                String command = parts[0].toLowerCase();
                try {
                    switch (command) {
                        case "submit-job":
                            jobSubmission(parts);
                            break;
                        case "job-status":
                            getJobStatus(parts);
                            break;
                        case "cancel-job":
                            cancelRunningJob(parts);
                            break;
                        case "list-workers":
                            getListOfWorkers(parts);
                            break;
                        default:
                            System.out.println("Invalid command .... ");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Error " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Master CLI Error " + e.getMessage());
        }
    }

    private static void getListOfWorkers(String[] parts) throws IOException, ClassNotFoundException {
        if (parts.length < 1) {
            System.out.println("Usage: list-workers");
            return;
        }

        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("LIST_WORKERS");

            String response = (String) in.readObject();
            System.out.println("... Details of registered workers ....");
            System.out.println(response);

        }
    }

    private static void getJobStatus(String[] parts) throws IOException, ClassNotFoundException {
        if (parts.length < 2) {
            System.out.println("Usage: job-status <job id>");
            return;
        }
        String jobId = parts[1];

        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("JOB_STATUS");
            out.writeObject(jobId);

            String response = (String) in.readObject();
            System.out.println("Request for job status : " + jobId + ", Status: " + response);

        }
    }

    private static void cancelRunningJob(String[] parts) throws IOException, ClassNotFoundException {
        if (parts.length < 2) {
            System.out.println("Usage: cancel-job <job id>");
            return;
        }
        String jobId = parts[1];

        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("CANCEL_JOB");
            out.writeObject(jobId);

            String response = (String) in.readObject();
            System.out.println("Job cancellation request submitted. ID: " + jobId + ", Status: " + response);

        }
    }

    private static void jobSubmission(String[] parts) throws IOException, ClassNotFoundException {
        if (parts.length < 2) {
            System.out.println("Usage: submit-job <command>");
            return;
        }
        String jobId = UUID.randomUUID().toString();
        String command = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        Job job = new Job(jobId, command);

        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("SUBMIT_JOB");
            out.writeObject(job);

            String response = (String) in.readObject();
            System.out.println("Job submitted. ID: " + jobId + ", Status: " + response);

        }
    }
}
