//package org.cloudbus.cloudsim.project;
//
//import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
//import org.cloudbus.cloudsim.Vm;
//import org.cloudbus.cloudsim.core.CloudSim;
//
//import java.util.*;
//
//public class ProvisioningStrategy {
//
//    // Consider 1000 MIPS per vCpu
//    private static final double MIPS_PER_VCPU = 1000.0;
//
//    private static final Map<String, InstanceType> INSTANCE_TYPES = new LinkedHashMap<>();
//    private static final Map<Integer, Double> vmStartTimes = new HashMap<>();
//    private static final Map<Integer, Double> vmFinishTimes = new HashMap<>();
//    private static final Map<String, Double> timeOccupied = new HashMap<>();
//    private static final Map<Integer, Integer> coresOccupied = new HashMap<>();
//    private static final Map<String, Integer> numberOfEachInstance = new HashMap<>();
//
//    // initializing few of many commercially available VMs
//    static {
//        // General Purpose Instances (t family)
//        INSTANCE_TYPES.put("t3.micro", new InstanceType(2, 1024, 100000, 0.0132)); // 2 vCPUs, 1 GiB RAM, $0.0132/hour
//        INSTANCE_TYPES.put("t3.small", new InstanceType(2, 2048, 100000, 0.0264));
//        INSTANCE_TYPES.put("t3.medium", new InstanceType(2, 4096, 100000, 0.0528));
//
//        // General Purpose Instances (m family)
//        INSTANCE_TYPES.put("m5.large", new InstanceType(2, 8192, 500000, 0.12));
//        INSTANCE_TYPES.put("m5.xlarge", new InstanceType(4, 16384, 500000, 0.240));
//        INSTANCE_TYPES.put("m5.2xlarge", new InstanceType(8, 32768, 500000, 0.480));
//
//        // Compute Optimized Instances (c family)
//        INSTANCE_TYPES.put("c5.large", new InstanceType(2, 4096, 1000000, 0.111));
//        INSTANCE_TYPES.put("c5.xlarge", new InstanceType(4, 8192, 1000000, 0.222));
//        INSTANCE_TYPES.put("c5.2xlarge", new InstanceType(8, 16384, 1000000, 0.444));
//
//        //Memory Optimized instances (r family)
//        INSTANCE_TYPES.put("r5.large", new InstanceType(2, 16384, 500000, 0.151));
//        INSTANCE_TYPES.put("r5.xlarge", new InstanceType(4, 32768, 500000, 0.302));
//        INSTANCE_TYPES.put("r5.2xlarge", new InstanceType(8, 65536, 500000, 0.604));
//
//        //Graviton Instances(t4g family)
//        INSTANCE_TYPES.put("t4g.nano", new InstanceType(2, 512, 100000, 0.0053));
//        INSTANCE_TYPES.put("t4g.micro", new InstanceType(2, 1024, 100000, 0.0106));
//        INSTANCE_TYPES.put("t4g.small", new InstanceType(2, 2048, 100000, 0.0212));
//    }
//
//    private static Map<String, Integer> availableCores = new HashMap<>();
//    public static List<Vm> provisionVms(List<CloudletDetails> cloudlets, int userId) {
//        List<Vm> vmList = new ArrayList<>();
//        Map<String, List<CloudletDetails>> vmToCloudlets = new HashMap<>();
//        Map<String, Vm> runningVms = new HashMap<>();
//        int occupiedCores = 1;
//        Collections.sort(cloudlets, Comparator.comparingDouble(CloudletDetails::getCloudletSubmissionTime));
//
//        // Iterate through given cloudlets for better execution
//        for (CloudletDetails cloudlet : cloudlets) {
//            InstanceType bestInstance = findBestInstance(cloudlet);
//            if (bestInstance == null) {
//                System.err.println("No suitable VM found for Cloudlet: " + cloudlet.getCloudletId() + " with " + cloudlet.getCloudletLength() + " MIPS before deadline : " + cloudlet.getDeadline());
//                continue;
//            }
//
//            double estimatedCompletionTime = cloudlet.getSubmissionTime() + (double) cloudlet.getCloudletLength() / (bestInstance.vCpus * MIPS_PER_VCPU);
//            double taskDuration = (double) cloudlet.getCloudletLength() / (bestInstance.vCpus * MIPS_PER_VCPU);
//            String instanceName = getInstanceName(bestInstance);
//            double currentTime = CloudSim.clock();
//            if (runningVms.containsKey(instanceName)) {
//                Vm vm = runningVms.get(instanceName);
//                if (vm.getNumberOfPes() > 0) {
//
//                }
//                vmToCloudlets.computeIfAbsent(instanceName, k -> new ArrayList<>()).add(cloudlet);
//                vmFinishTimes.put(instanceName, vmFinishTimes.get(instanceName) + estimatedCompletionTime);
//                cloudlet.setGuestId(vm.getId()); // set the cloudlet guest ID.
//            } else {
//                Vm vm = createVm(userId, instanceName, bestInstance);
//                vmList.add(vm);
//                numberOfEachInstance.put(getInstanceName(bestInstance), 1);
//                runningVms.put(instanceName, vm);
//                vmToCloudlets.computeIfAbsent(instanceName, k -> new ArrayList<>()).add(cloudlet);
//                vmStartTimes.put(createDynamicVmID(userId,getInstanceName(bestInstance), bestInstance), cloudlet.getCloudletSubmissionTime()); // Record VM start time
//                vmFinishTimes.put(createDynamicVmID(userId,getInstanceName(bestInstance), bestInstance), estimatedCompletionTime);
////                coresOccupied.put(createDynamicVmID(userId,getInstanceName(bestInstance), bestInstance), occupiedCores);
////                occupiedCores++;
//                vm.setNumberOfPes(vm.getNumberOfPes() - 1);
//                cloudlet.setGuestId(vm.getId()); // set the cloudlet guest ID.
//            }
//        }
//        return vmList;
//    }
//
//    private static InstanceType findBestInstance(CloudletDetails cloudlet) {
//        InstanceType bestInstance = null;
//        double minCostHourly = Double.MAX_VALUE;
//
//        for (Map.Entry<String, InstanceType> entry : INSTANCE_TYPES.entrySet()) {
//            InstanceType instance = entry.getValue();
//            double estimatedCompletionTime = cloudlet.getSubmissionTime() + (double) cloudlet.getCloudletLength() / (instance.vCpus * MIPS_PER_VCPU);
//            if (instance.vCpus >= cloudlet.getNumberOfPes() && instance.ram >= cloudlet.getMinMemoryToExecute() && instance.storage >= cloudlet.getMinStorageToExecute()) {
//                if (instance.hourlyCost < minCostHourly && estimatedCompletionTime < cloudlet.getDeadline()) {
//                    minCostHourly = instance.hourlyCost;
//                    bestInstance = instance;
//                    cloudlet.setBestInstance(entry.getKey());
//                    cloudlet.setHourlyPrice(minCostHourly);
//                    timeOccupied.put(getInstanceName(bestInstance),
//                            (timeOccupied.containsKey(getInstanceName(bestInstance)) ?
//                                    timeOccupied.get(getInstanceName(bestInstance)) + (double) cloudlet.getCloudletLength() / (bestInstance.vCpus * MIPS_PER_VCPU) :
//                                    (double) cloudlet.getCloudletLength() / (bestInstance.vCpus * MIPS_PER_VCPU)));
//                    coresOccupied.put(getInstanceName(bestInstance), 0);
//                }
//            }
//        }
//        return bestInstance;
//    }
//
//    private static String getInstanceName(InstanceType instance) {
//        for (Map.Entry<String, InstanceType> entry : INSTANCE_TYPES.entrySet()) {
//            if (entry.getValue().equals(instance)) {
//                return entry.getKey();
//            }
//        }
//        return null;
//    }
//
//    private static int createDynamicVmID (int userId, String instanceName, InstanceType instance) {
//        int vmId = userId + INSTANCE_TYPES.keySet().stream().toList().indexOf(instanceName);
//        return Integer.parseInt(String.valueOf(vmId) + String.valueOf(numberOfEachInstance.get(instanceName)));
//    }
//
//    private static Vm createVm(int userId, String instanceName, InstanceType instance) {
////        int vmId = userId + INSTANCE_TYPES.keySet().stream().toList().indexOf(instanceName);
//        int mips = (int) (instance.vCpus * MIPS_PER_VCPU);
////        vmId = Integer.parseInt(String.valueOf(vmId) + String.valueOf(numberOfEachInstance.get(instanceName)));
//        return new Vm(createDynamicVmID(userId, instanceName, instance),
//                userId,
//                mips,
//                instance.vCpus,
//                instance.ram,
//                10000,
//                instance.storage,
//                "Xen",
//                new CloudletSchedulerSpaceShared());
//    }
//
//    public static Map<Integer, Double> getVmFinishTimes() {
//        return vmFinishTimes;
//    }
//
//    public static Map<Integer, Double> getVmStartTimes() {
//        return vmStartTimes;
//    }
//
//    private static class InstanceType {
//        int vCpus;
//        int ram;
//        int storage;
//        double hourlyCost;
//
//        public InstanceType(int vCpus, int ram, int storage, double hourlyCost) {
//            this.vCpus = vCpus;
//            this.ram = ram;
//            this.storage = storage;
//            this.hourlyCost = hourlyCost;
//        }
//    }
//
//}