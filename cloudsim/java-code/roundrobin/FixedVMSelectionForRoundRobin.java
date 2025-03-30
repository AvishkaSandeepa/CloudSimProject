package org.cloudbus.cloudsim.projectone.roundrobin;

import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.projectone.comon.CloudletDetails;
import org.cloudbus.cloudsim.projectone.dynamicvmprovisioning.DynamicVMProvisioningStrategy;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ASSUMPTION :: Allowed to create only 100 of VMs per instance without interruption (within the datacenter boundaries)
 *               Create next VMs on next suitable instance, if the above bound is exceeded !!
 */
public class FixedVMSelectionForRoundRobin {
    // Consider 10000 MIPS per vCpu
    private static final double MIPS_PER_VCPU = 10000.0;

    private static final Map<String, InstanceType> INSTANCE_TYPES = new LinkedHashMap<>();
    private static final Map<String, Integer> numberOfEachInstance = new HashMap<>();
    private static final Map<String, Map<Integer, Vm>> runningVms = new HashMap<>();
    private static final List<Integer> createdVMIDs = new ArrayList<>();

    /**
     * initializing few of many commercially available VMs
     */
    static {
        // General Purpose Instances (t family)
        INSTANCE_TYPES.put("t3.micro", new InstanceType(2, 1024, 100000, 0.0132, false)); // 2 vCPUs, 1 GiB RAM, $0.0132/hour
        INSTANCE_TYPES.put("t3.small", new InstanceType(2, 2048, 100000, 0.0264, false));
        INSTANCE_TYPES.put("t3.medium", new InstanceType(2, 4096, 100000, 0.0528, false));

        // General Purpose Instances (m family)
        INSTANCE_TYPES.put("m5.large", new InstanceType(2, 8192, 500000, 0.12, false));
        INSTANCE_TYPES.put("m5.xlarge", new InstanceType(4, 16384, 500000, 0.240, false));
        INSTANCE_TYPES.put("m5.2xlarge", new InstanceType(8, 32768, 500000, 0.480, false));

        // Compute Optimized Instances (c family)
        INSTANCE_TYPES.put("c5.large", new InstanceType(2, 4096, 1000000, 0.111, false));
        INSTANCE_TYPES.put("c5.xlarge", new InstanceType(4, 8192, 1000000, 0.222, false));
        INSTANCE_TYPES.put("c5.2xlarge", new InstanceType(8, 16384, 1000000, 0.444, false));

        //Memory Optimized instances (r family)
        INSTANCE_TYPES.put("r5.large", new InstanceType(2, 16384, 500000, 0.151, false));
        INSTANCE_TYPES.put("r5.xlarge", new InstanceType(4, 32768, 500000, 0.302, false));
        INSTANCE_TYPES.put("r5.2xlarge", new InstanceType(8, 65536, 500000, 0.604, false));

        //Graviton Instances(t4g family)
        INSTANCE_TYPES.put("t4g.nano", new InstanceType(2, 512, 100000, 0.0053, false));
        INSTANCE_TYPES.put("t4g.micro", new InstanceType(2, 1024, 100000, 0.0106, false));
        INSTANCE_TYPES.put("t4g.small", new InstanceType(2, 2048, 100000, 0.0212, false));
    }


    public static Map<String, Integer> getNumberOfEachInstance() {
        return numberOfEachInstance;
    }

    public static List<Integer> getCreatedVMIDs() {
        return createdVMIDs;
    }

    public static String getInstanceNameByVmId(int vmId) {
        for (Map.Entry<String, Map<Integer, Vm>> instanceEntry : runningVms.entrySet()) {
            if (instanceEntry.getValue().containsKey(vmId)) {
                return instanceEntry.getKey();
            }
        }
        return null;
    }

    public static double getHourlyPrice(String instanceKey) {
        if (INSTANCE_TYPES.containsKey(instanceKey)) {
            return INSTANCE_TYPES.get(instanceKey).hourlyCost;
        } else {
            return -1.0;
        }
    }

    /**
     * Finding best instance considering storage , mips and memory
     * @param cloudletDetails
     * @param userId
     * @return
     */
    public static List<Vm> findBestInstance(List<CloudletDetails> cloudletDetails, int userId) {
        List<Vm> vmList = new ArrayList<>();

        for (CloudletDetails cloudlet : cloudletDetails) {
            InstanceType bestInstance = new InstanceType(0, 0, 0, 0.0, false);
            Map<String, Double> selectedVmInstances = new HashMap<>();

            for (Map.Entry<String, InstanceType> entry : INSTANCE_TYPES.entrySet()) {
                InstanceType instance = entry.getValue();
                if (instance.vCpus >= cloudlet.getNumberOfPes() && instance.ram >= cloudlet.getMinMemoryToExecute() && instance.storage >= cloudlet.getMinStorageToExecute()) {
                    selectedVmInstances.put(entry.getKey(), instance.hourlyCost);
                }
            }

            Map<String, Double> sortedSelectedVmInstances = selectedVmInstances.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new));

            for (Map.Entry<String, Double> sortedEntry : sortedSelectedVmInstances.entrySet()) {
                String instanceName = sortedEntry.getKey();
                System.out.println("Checking instance: " + instanceName); // Debugging

                if (numberOfEachInstance.containsKey(instanceName)) {
                    int currentCount = numberOfEachInstance.get(instanceName);
                    System.out.println("Current count for " + instanceName + ": " + currentCount); // Debugging

                    if (currentCount < 80) { // Changed to < from <=
                        bestInstance = INSTANCE_TYPES.get(instanceName);
                        Vm vm = createVm(userId, instanceName, bestInstance);
                        vmList.add(vm);
                        createdVMIDs.add(vm.getId());
                        runningVms.computeIfAbsent(instanceName, k -> new HashMap<>()).put(vm.getId(), vm);
                        numberOfEachInstance.put(instanceName, currentCount + 1);
                        System.out.println("Updated count for " + instanceName + ": " + (currentCount + 1)); // Debugging
                    }
                    break;
                } else {
                    bestInstance = INSTANCE_TYPES.get(instanceName);
                    Vm vm = createVm(userId, instanceName, bestInstance);
                    vmList.add(vm);
                    createdVMIDs.add(vm.getId());
                    numberOfEachInstance.put(instanceName, 1);
                    runningVms.computeIfAbsent(instanceName, k -> new HashMap<>()).put(vm.getId(), vm);
                    System.out.println("First instance of " + instanceName + " created."); // Debugging
                    break;
                }
            }
        }
        return vmList;
    }

    private static int createDynamicVmID(int userId, String instanceName, int numberOfInstance) {
        int vmId = userId + INSTANCE_TYPES.keySet().stream().toList().indexOf(instanceName);
        return Integer.parseInt(String.valueOf(vmId) + 0 + 0 + numberOfInstance);
    }

    private static Vm createVm(int userId, String instanceName, InstanceType instance) {
        return new Vm(
                createDynamicVmID(userId, instanceName, numberOfEachInstance.getOrDefault(instanceName, 1)),
                userId,
                MIPS_PER_VCPU,
                instance.vCpus,
                instance.ram,
                10000,
                instance.storage,
                "Xen",
                new CloudletSchedulerSpaceShared());
    }

    public static double getHourlyCostByVmId(int vmId) {
        for (Map.Entry<String, Map<Integer, Vm>> instanceEntry : runningVms.entrySet()) {
            String instanceType = instanceEntry.getKey();
            Map<Integer, Vm> vmsOfThisType = instanceEntry.getValue();

            if (vmsOfThisType.containsKey(vmId)) {
                InstanceType instanceSpecs = INSTANCE_TYPES.get(instanceType);
                if (instanceSpecs != null) {
                    return instanceSpecs.getHourlyCost();
                }
            }
        }
        return -1;
    }

    private static class InstanceType {
        int vCpus;
        int ram;
        int storage;
        double hourlyCost;
        boolean suitableVm;

        public InstanceType(int vCpus, int ram, int storage, double hourlyCost, boolean suitableVm) {
            this.vCpus = vCpus;
            this.ram = ram;
            this.storage = storage;
            this.hourlyCost = hourlyCost;
            this.suitableVm = suitableVm;
        }

        public int getvCpus() {
            return vCpus;
        }

        public int getRam() {
            return ram;
        }

        public int getStorage() {
            return storage;
        }

        public double getHourlyCost() {
            return hourlyCost;
        }

        public boolean isSuitableVm() {
            return suitableVm;
        }

        public void setSuitableVm(boolean suitableVm) {
            this.suitableVm = suitableVm;
        }
    }

    public static Map<String, Map<Integer, Vm>> getRunningVms() {
        return runningVms;
    }

}