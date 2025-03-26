package com.jobmanagement.shared;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalTime;

public class Job implements Serializable {
    private final String id;
    private final String command;
    private Status jobStatus;
    private LocalTime deadline;
    private double budget;

    public Job(String id, String command, LocalTime deadline, double budget) {
        this.id = id;
        this.command = command;
        this.jobStatus = Status.PENDING;
        this.deadline = deadline;
        this.budget = budget;
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

    public LocalTime getDeadline() {
        return deadline;
    }

    public double getBudget() {
        return budget;
    }

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, UNKNOWN
    }
}
