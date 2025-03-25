package com.jobmanagement.shared;

public class Job {
    private final String id;
    private final String command;
    private Status jobStatus;

    public Job(String id, String command) {
        this.id = id;
        this.command = command;
        this.jobStatus = Status.PENDING;
    }

    public String getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    public Status getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(Status jobStatus) {
        this.jobStatus = jobStatus;
    }

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}
