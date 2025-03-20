package org.cloudbus.cloudsim.project;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import java.util.List;

public class CloudletDetails extends Cloudlet {
    private final double submissionTime;
    private final long minMemoryToExecute;
    private final long minStorageToExecute;
    private final double deadline;
    private String bestInstance;
    private double hourlyPrice;

    public CloudletDetails(
            int cloudletId,
            long cloudletLength,
            int pesNumber,
            long cloudletFileSize,
            long cloudletOutputSize,
            UtilizationModel utilizationModelCpu,
            UtilizationModel utilizationModelRam,
            UtilizationModel utilizationModelBw,
            double submissionTime,
            long minMemoryToExecute,
            long minStorageToExecute,
            double deadline) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.deadline = deadline;
        this.submissionTime = submissionTime;
        this.minMemoryToExecute = minMemoryToExecute;
        this.minStorageToExecute = minStorageToExecute;

    }

    public double getHourlyPrice() {
        return hourlyPrice;
    }

    public void setHourlyPrice(double hourlyPrice) {
        this.hourlyPrice = hourlyPrice;
    }

    public void setBestInstance(String bestInstance) {
        this.bestInstance = bestInstance;
    }

    public String getBestInstance() {
        return bestInstance;
    }

    public double getCloudletSubmissionTime() {
        return submissionTime;
    }

    public long getMinMemoryToExecute() {
        return minMemoryToExecute;
    }

    public long getMinStorageToExecute() {
        return minStorageToExecute;
    }

    public double getDeadline() {
        return deadline;
    }

}
