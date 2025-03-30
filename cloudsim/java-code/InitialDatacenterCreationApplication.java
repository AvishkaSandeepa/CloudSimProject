package org.cloudbus.cloudsim.projectone;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a question no.1 of CloudSim assignment
 * <p>
 * Create one data center with 500 hosts. Each host must have:
 * a. 8 cores (10000 MIPS each)
 * b. 65 GB of RAM
 * c. 10 TB of storage
 */
public class InitialDatacenterCreationApplication {
    /**
     * Define these variables as static variables due to the easy access from main method all across the class
     * If there are multiple brokers, we should avoid using static as it can be override the variables, since it will be used across multiple entities
     */
    public static DatacenterBroker broker;
    /**
     * The cloudlet list.
     */
    private static List<Cloudlet> cloudletList;

    /**
     * The vmlist.
     */
    private static List<Vm> vmlist;

    public static void main(String[] args) {
        Log.println("Starting of CloudSim assignment question 1 ...");

        try {
            // ----------- First step: Initialize the CloudSim package. It should be called before creating any entities.
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance(); // Calendar whose fields have been initialized with the current date and time.
            boolean trace_flag = false; // trace events

            CloudSim.init(num_user, calendar, trace_flag);

            // ----------- Second Step: Creating a datacenter with 500 hosts (each has  cores)
            Datacenter datacenter = createDatacenter("Datacenter_0", 1, 2);

            // ----------- Third Step: Creating a single broker, since these cloudlets are handle by single user/organization
            broker = new DatacenterBroker("Broker");
            int brokerId = broker.getId();


            //----------- Fourth Step: Create VMs and Cloudlets and send them to broker
            vmlist = createVM(brokerId, 4); //creating 2000 vms

            cloudletList = createCloudlet(brokerId, 40); // creating 40000 cloudlets

            broker.submitGuestList(vmlist);
            broker.submitCloudletList(cloudletList);

            //----------- Fifth Step: Create VMs and Cloudlets and send them to broker
            CloudSim.startSimulation();

            //----------- Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            //----------- Stop the simulation
            CloudSim.stopSimulation();

            printCloudletList(newList);

            Log.println("CloudSim questionOne is finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.println("Caught an exception while running the code :(");
        }
    }

    /**
     * Creation of a datacenter for a given name with specified characteristics
     *
     * @param name          of the datacenter
     * @param numberOfHosts for the datacenter
     * @param numberOfCores for the datacenter
     * @return Datacenter
     */
    public static Datacenter createDatacenter(String name, int numberOfHosts, int numberOfCores) {
        // Steps for HOST machine creation under given instructions

        // Initialize the list of host machines
        List<Host> hostList = new ArrayList<>();

        // 1. Creation of 500 hosts
        for (int i = 0; i < numberOfHosts; i++) {
            // 2. Define PEs or CPUs/Cores.
            int mips = 10000;  // MIPS for each PE
            List<Pe> peList = new ArrayList<>();

            for (int j = 0; j < numberOfCores; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
            }

            int ram = 66560; //host memory (MB)
            long storage = 10_000_000; //host storage
            int bw = 10000;

            hostList.add(new Host(i, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, new VmSchedulerTimeShared(peList)));
        }

        // 5. Create a DatacenterCharacteristics object that stores the
        // properties of a data center: architecture, OS, list of
        // Machines, allocation policy: time- or space-shared, time zone
        // and its price (G$/Pe time unit).
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this
        // resource
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<>(); // we are not adding SAN
        // devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.println("Create a datacenter with 500 hosts successfully");
        return datacenter;
    }

    /**
     * Creating VMs that sufficient for execution available CPUs
     *
     * @param userId
     * @param numberOfVMs
     * @return List<Vm>
     */
    private static List<Vm> createVM(int userId, final int numberOfVMs) {
        List<Vm> vmList = new ArrayList<>();

        //VM Parameters
        long size = 10000; //image size (MB)
        int ram = 512; //vm memory (MB)
        int mips = 300;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        for (int i = 0; i < numberOfVMs; i++) {
            vmList.add(new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared()));
        }

        return vmList;
    }

    /**
     * @param userId
     * @param cloudlets
     * @return list of cloudlets
     */
    private static List<Cloudlet> createCloudlet(int userId, int cloudlets) {
        List<Cloudlet> list = new ArrayList<>();

        //cloudlet parameters
        long length = 100000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < cloudlets; i++) {
            list.add(new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel));
            list.getLast().setUserId(userId);
        }

        return list;
    }

    /**
     * Prints the Cloudlet objects
     *
     * @param list list of Cloudlets
     */
    private static void printCloudletList(List<Cloudlet> list) {
        Cloudlet cloudlet;

        String indent = "    ";
        Log.println();
        Log.println("========== OUTPUT ==========");
        Log.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (Cloudlet value : list) {
            cloudlet = value;
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.print("SUCCESS");

                Log.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getGuestId() + indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getExecFinishTime()));
            }
        }

    }

}
