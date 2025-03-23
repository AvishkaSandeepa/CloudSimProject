package org.cloudbus.cloudsim.project.roundrobin;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.project.comon.CloudletDetails;
import org.cloudbus.cloudsim.project.dynamicvmprovisioning.DynamicVMProvisioningStrategy;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;

/**
 * This is a question no.3 of CloudSim assignment

 * main class of simulation of executing cloudlets using a file
 * need a creation of a custom broker class for that
 */
public class RoundRobinApplication {
    /**
     * Define these variables as static variables due to the easy access from main method all across the class
     * If there are multiple brokers, we should avoid using static as it can be override the variables, since it will be used across multiple entities
     */
    public static CustomDatacenterBrokerRoundRobin broker;

    /** The vmlist. */
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
            Datacenter datacenter = createDatacenter("Datacenter_0", 500, 8);

            // ----------- Third Step: Creating a single broker, since these cloudlets are handle by single user/organization
            broker = new CustomDatacenterBrokerRoundRobin("Broker",
                    "F:/Avishka-Sandeepa-PHD/Project/cloudsim/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/project/cloudlet.txt");

            //----------- Fourth Step: Create VMs and Cloudlets and send them to broker
            CloudSim.startSimulation();

            //----------- Fifth Step: Retrieving created VM List
            // You can only get the vm list after starting the simulation.
            // If you re trying to access before initializing there will be an error
            vmlist = CustomDatacenterBrokerRoundRobin.getVmlist();

            //----------- Final step: Print results when simulation is over
            List<CloudletDetails> newList = broker.getCloudletReceivedList();

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
     * @param name of the datacenter
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

            hostList.add(new Host(
                    i,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList)));
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

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.println("Create a datacenter with " + numberOfHosts +" hosts successfully");
        return datacenter;
    }



    /**
     * Prints the Cloudlet objects
     * @param list  list of Cloudlets
     */
    private static void printCloudletList(List<CloudletDetails> list) {
        CloudletDetails cloudlet;
        Map<String, Double> runningVmNames = new HashMap<>();
        Map<Integer, Double> vmExecutionTimes = new HashMap<>();
        Map<Integer, Double> vmCostTimes = new HashMap<>();
        Map<String, Double> totalCostPerVm = new HashMap<>();



        String indent = "    ";
        Log.println("\n \n \n");
        Log.println("========== OUTPUT ==========");
        Log.println("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + indent +
                "SubmissionTime" + indent + indent + "Time" + indent + indent +
                "Start Time" + indent + "Finish Time" + indent + indent +
//                "Deadline" + indent +
                "VM Instance" + indent + indent + "Price Per Hour($)" + indent + indent + "Price for consumption ($)");

        DecimalFormat dft = new DecimalFormat("###.##");
        DecimalFormat dft1 = new DecimalFormat("###.#######");
        List<String> vms = new ArrayList<>();
        int totalCompleted = 0;
        double totalCost = 0.0;
        for (CloudletDetails value : list) {
            cloudlet = value;
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);
            totalCost += cloudlet.getActualCPUTime() * (cloudlet.getHourlyPrice()/3600.0);

//            if (!runningVmNames.containsKey(cloudlet.getBestInstance())) {
//                runningVmNames.put(cloudlet.getBestInstance(), 0.0);
//            }

            int vmId = cloudlet.getGuestId();
            double executionTime = cloudlet.getExecFinishTime() - cloudlet.getExecStartTime();
            vmExecutionTimes.merge(vmId, executionTime, Double::sum);
            vmCostTimes.merge(vmId, cloudlet.getActualCPUTime() * (cloudlet.getHourlyPrice()/3600.0), Double::sum);
            String foundInstanceName = findInstanceNameByVmId(vmId);
            runningVmNames.merge(foundInstanceName, executionTime, Double::sum);
            totalCostPerVm.merge(foundInstanceName, cloudlet.getActualCPUTime() * (cloudlet.getHourlyPrice()/3600.0), Double::sum);

//            if (cloudlet.getDeadline() < cloudlet.getExecFinishTime()) System.err.println("ERRRRRRRR");
            if (cloudlet.getStatus() == CloudletDetails.CloudletStatus.SUCCESS && Objects.nonNull(cloudlet.getBestInstance())) {
                Log.print("SUCCESS");
                totalCompleted++;

                if (!vms.contains(cloudlet.getBestInstance())) vms.add(cloudlet.getBestInstance());
                Log.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + indent + cloudlet.getGuestId() +
                        indent + indent + indent + dft.format(cloudlet.getCloudletSubmissionTime()) +
                        indent + indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + indent + dft.format(cloudlet.getExecFinishTime()) +
//                        indent + indent + indent + cloudlet.getDeadline() +
                        indent + indent + indent + cloudlet.getBestInstance() +
                        indent + indent + indent + cloudlet.getHourlyPrice() +
                        indent + indent + indent + indent + indent + dft1.format(cloudlet.getActualCPUTime() * (cloudlet.getHourlyPrice()/3600.0)));
            }
        }

        Log.println();
        Log.println("No of VMs --> " + " | Total cost for VMs --> $ " + dft1.format(totalCost));
        Log.println();

        Log.println("Total No of cloudlets completed : " + totalCompleted + " out of " + CustomDatacenterBrokerRoundRobin.getTotalCloudletRead());
        Log.println("========== VM ==========");
        Log.println("VM Name" + indent + indent + indent + "Number of Instance" + indent + indent + "Total Execution Time" + indent + indent + indent + "Total Cost ($)");
        for (Map.Entry<String, Double> vm : runningVmNames.entrySet()) {
            Log.println(vm.getKey() +
                    indent + indent + indent + indent + (DynamicVMProvisioningStrategy.getNumberOfEachInstance().get(vm.getKey())+1) +
                    indent + indent + indent +  indent + indent + runningVmNames.get(vm.getKey()) +
                    indent + indent + indent +  indent + indent + dft1.format(totalCostPerVm.get(vm.getKey())));
        }

        Map<Integer, Double> sortedvmExecutionTimes = new TreeMap<>(vmExecutionTimes);
//        Log.println();
//        Log.println();
//        Log.println("======== VM Times ========");
//        Log.println("VM ID" + indent + indent + "ExecutionTime" + indent + indent + "Cost per Instance");
//        for (Map.Entry<Integer, Double> entry : sortedvmExecutionTimes.entrySet()) {
//            Log.println(entry.getKey() +
//                    indent + indent + entry.getValue() +
//                    indent + indent + indent + indent + indent + dft1.format(vmCostTimes.get(entry.getKey())));
//        }

        // ==================== testing only ====================================
        Log.println();
        Log.println();
        for (Map.Entry<Integer, Double> entry : sortedvmExecutionTimes.entrySet()) {
            Log.print(entry.getKey() + ", ");
        }

        Log.println();
        for (Map.Entry<Integer, Double> entry : sortedvmExecutionTimes.entrySet()) {
            Log.print(entry.getValue() + ", ");
        }

        Log.println();
        for (Map.Entry<Integer, Double> entry : sortedvmExecutionTimes.entrySet()) {
            Log.print(dft1.format(vmCostTimes.get(entry.getKey())) + ", ");
        }
        // ==========================================================================



        Log.println();
        Log.println();
        Log.println("======== VM Instance Name to VMID mapping ========");
        for (Map.Entry<String, Map<Integer, Vm>> runingVms : DynamicVMProvisioningStrategy.getRunningVms().entrySet()) {
            Log.print("VM Instance Name : " + runingVms.getKey() + " ---> ");
            for (Map.Entry<Integer, Vm> entry : runingVms.getValue().entrySet()) {
                Log.print(entry.getKey() + ", ");
            }
            Log.println();
        }


    }

    private static String findInstanceNameByVmId(int vmId) {
        for (Map.Entry<String, Map<Integer, Vm>> outerEntry : DynamicVMProvisioningStrategy.getRunningVms().entrySet()) {
            for (Map.Entry<Integer, Vm> innerEntry : outerEntry.getValue().entrySet()) {
                if (innerEntry.getKey() == vmId) {
                    return outerEntry.getKey();
                }
            }
        }
        return null;
    }

}
