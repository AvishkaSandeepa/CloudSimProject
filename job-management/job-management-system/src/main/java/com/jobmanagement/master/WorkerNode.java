package com.jobmanagement.master;

import com.jobmanagement.shared.Job;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerNode {
    private final String address;
    private final int port;
    private final String password;
    private final ConcurrentHashMap<String, String> jobStatusCache = new ConcurrentHashMap<>();
    private String status;


    public WorkerNode(String address, int port, String password) {
        this.address = address;
        this.port = port;
        this.password = password;
        this.status = "REGISTERED";
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public ConcurrentHashMap<String, String> getJobStatusCache() {
        return jobStatusCache;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public boolean healthCheck () {
        try (Socket socket = new Socket(address, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("HEALTH_CHECK");

            String response = (String) in.readObject();
            return "OK".equals(response);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in health check " + e.getMessage());
            return false;
        }
    }

    public boolean executeJob (Job job) {
        try (Socket socket = new Socket(address, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("EXECUTE_JOB");
            out.writeObject(password);
            out.writeObject(job);

            String response = (String) in.readObject();
            if ("ACCEPTED".equals(response)) {
                jobStatusCache.put(job.getId(), "RUNNING");
                return true;
            }
            return false;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in job execution " + e.getMessage());
            return false;
        }
    }

    public boolean cancelJob (String jobId) {
        try (Socket socket = new Socket(address, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("CANCEL_JOB");
            out.writeObject(password);
            out.writeObject(jobId);

            String response = (String) in.readObject();
            if ("CANCELED".equals(response)) {
                jobStatusCache.put(jobId, "CANCELED");
                return true;
            }
            return false;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in job cancellation " + e.getMessage());
            return false;
        }
    }

    public String getJobStatus (String jobId) {
        try (Socket socket = new Socket(address, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("JOB_STATUS");
            out.writeObject(password);
            out.writeObject(jobId);

            return (String) in.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in job status check " + e.getMessage());
            return Job.Status.UNKNOWN.name();
        }
    }

    public void updateStatusOfJobCache(String jobId) {
        jobStatusCache.put(jobId, "COMPLETED");
    }
}
