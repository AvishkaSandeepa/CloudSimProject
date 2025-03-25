package com.jobmanagement.master;

public class WorkerNode {
    private final String address;
    private final int port;
    private final String password;

    public WorkerNode(String address, int port, String password) {
        this.address = address;
        this.port = port;
        this.password = password;
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
}
