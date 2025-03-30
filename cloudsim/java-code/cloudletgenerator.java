package org.cloudbus.cloudsim.projectone;

import java.io.*;
import java.util.Random;

public class cloudletgenerator {
    public static void main(String[] args) {
        String inputFile = "F:\\Avishka-Sandeepa-PHD\\Project\\cloudsim\\modules\\cloudsim-examples\\src\\main\\java\\org\\cloudbus\\cloudsim\\projectone\\dataset\\cloudlet.txt";    // Input file with original data
        String outputFile = "F:\\Avishka-Sandeepa-PHD\\Project\\cloudsim\\modules\\cloudsim-examples\\src\\main\\java\\org\\cloudbus\\cloudsim\\projectone\\dataset\\cloudlets_updated.txt"; // Output file with updated deadlines

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            Random random = new Random();
            String line;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    writer.newLine();
                    continue;
                }

                // Split the line into columns
                String[] parts = line.split("\\s+");
                if (parts.length < 5) {
                    // If line doesn't have enough columns, write it as-is
                    writer.write(line);
                    writer.newLine();
                    continue;
                }

                try {
                    // Parse the values
                    int submissionTime = Integer.parseInt(parts[0]);
                    long mi = Long.parseLong(parts[1]);
                    int memory = Integer.parseInt(parts[2]);
                    int storage = Integer.parseInt(parts[3]);

                    // Calculate new deadline
                    double executionTime = mi / 10000.0;
                    int buffer = 5 + random.nextInt(16); // Random between 5-20
                    int newDeadline = submissionTime + (int)executionTime + 1;

                    // Write the updated line
                    writer.write(String.format("%d %d %d %d %d",
                            submissionTime, mi, memory, storage, newDeadline));
                    writer.newLine();

                } catch (NumberFormatException e) {
                    // If parsing fails, write the line as-is
                    writer.write(line);
                    writer.newLine();
                }
            }

            System.out.println("Successfully updated deadlines and saved to: " + outputFile);

        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
