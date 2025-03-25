package com.jobmanagement.worker;

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
    private final ConcurrentHashMap<String, Thread> runningJobs = new ConcurrentHashMap<>();
    private boolean running = true;

    public WorkerServer(int port, String password) {
        this.port = port;
        this.password = password;
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
                        System.out.println("test pass");;
                        break;
//                    case "JOB_STATUS":
//                        handleJobStatus(in, out);
//                        break;
//                    case "CANCEL_JOB":
//                        handleCancelJob(in, out);
//                        break;
//                    case "HEALTH_CHECK":
//                        out.writeObject("OK");
//                        break;
//                    case "SHUTDOWN":
//                        out.writeObject("SHUTTING_DOWN");
//                        stop();
//                        break;
                    default:
                        out.writeObject("UNKNOWN_COMMAND");
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Client handler error: " + e.getMessage());
            }
        }
    }


    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: WorkerServer <port> <password>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String password = args[1];

        WorkerServer server = new WorkerServer(port, password);
        server.start();
//        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

}
