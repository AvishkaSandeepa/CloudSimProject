package com.jobmanagement.master;

import com.jobmanagement.shared.Job;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

public class MasterCLI {
    private static final int MASTER_PORT = 8080;
    private static final String MASTER_ADDRESS = "localhost";


    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Welcome to the job scheduling CLI. To get all available commands, please type 'help' and press enter");
            System.out.println("----------------------------------------------------------------------------------------------------");

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
                        case "done":
                            doneBatch(parts);
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
                        case "help":
                            printHelp();
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
            System.out.println("Usage: submit-job <command> <deadline> <budget>");
            return;
        }
        String jobId = UUID.randomUUID().toString();
        String command = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 2));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime deadline = LocalTime.parse(parts[2], formatter);
        double budget = Double.parseDouble(parts[3]);
        Job job = new Job(jobId, command, deadline, budget);

        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("SUBMIT_JOB");
            out.writeObject(job);

            String response = (String) in.readObject();
            System.out.println("Job submitted. ID: " + jobId + ", Status: " + response);

        }
    }

    private static void doneBatch(String[] parts) throws IOException, ClassNotFoundException {
        if (parts.length < 1) {
            System.out.println("Usage: done");
            return;
        }

        try (Socket socket = new Socket(MASTER_ADDRESS, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("DONE_BATCH");

            String response = (String) in.readObject();
            System.out.println("Submit for batch process. STATUS :  " + response);

        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  submit-job <command> <deadline> <budget>  - Submit a new job. Deadline should be in HH:mm:ss format");
        System.out.println("  done - Start scheduling and execution of initially submitted jobs");
        System.out.println("  job-status <jobId>    - Check job status");
        System.out.println("  cancel-job <jobId>    - Cancel a running job");
        System.out.println("  list-workers      - List all registered workers");
        System.out.println("  exit              - Exit the CLI");
    }
}
