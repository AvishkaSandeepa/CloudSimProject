package org.cloudbus.cloudsim.projectone.dynamicvmprovisioning;

import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.projectone.comon.CloudletDetails;

import java.util.*;

/**
 * ASSUMPTION :: Allowed to create any number of VMs per instance as much as wanted without interruption (within the datacenter boundaries)
 */
public class DynamicVMProvisioningStrategy {
    // Consider 10000 MIPS per vCpu
    private static final double MIPS_PER_VCPU = 10000.0;

    private static final Map<String, InstanceType> INSTANCE_TYPES = new LinkedHashMap<>();
    private static final Map<Integer, Map<Integer, Double>> vmStartTimes = new HashMap<>();  // <VMID, <CORE , START>
    private static final Map<Integer, Map<Integer, Double>> vmFinishTimes = new HashMap<>(); // <VMID, <CORE , START>
    private static final Map<Integer, Integer> coresOccupied = new HashMap<>();
    private static final Map<String, Integer> numberOfEachInstance = new HashMap<>();
    private static final Map<Integer, Map<Integer, CloudletDetails>> vmToCloudlets = new HashMap<>();
    private static final Map<String, Map<Integer, Vm>> runningVms = new HashMap<>();

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

    public static Map<String, Map<Integer, Vm>> getRunningVms() {
        return runningVms;
    }

    public static Map<String, Integer> getNumberOfEachInstance() {
        return numberOfEachInstance;
    }

    /**
     * Finding best instance considering storage , mips and memory
     * @param cloudlet
     * @return
     */
    private static InstanceType findBestInstance(CloudletDetails cloudlet) {
        InstanceType bestInstance = new InstanceType(0, 0, 0, 0.0, false);
        double minCostHourly = Double.MAX_VALUE;

        for (Map.Entry<String, InstanceType> entry : INSTANCE_TYPES.entrySet()) {
            InstanceType instance = entry.getValue();
            double estimatedCompletionTime = cloudlet.getCloudletSubmissionTime() + (double) cloudlet.getCloudletLength() / (cloudlet.getNumberOfPes() * MIPS_PER_VCPU);
            if (instance.vCpus >= cloudlet.getNumberOfPes() && instance.ram >= cloudlet.getMinMemoryToExecute() && instance.storage >= cloudlet.getMinStorageToExecute()) {
                if (instance.hourlyCost < minCostHourly && estimatedCompletionTime < cloudlet.getDeadline()) {
                    minCostHourly = instance.hourlyCost;
                    bestInstance = instance;
                    cloudlet.setBestInstance(entry.getKey());
                    cloudlet.setHourlyPrice(minCostHourly);
                    bestInstance.setSuitableVm(true);
                }
            }
        }
        return bestInstance;
    }

    /**
     * dynamic provisioning algorithm to execute cloudlets before deadline
     * @param cloudlets
     * @param userId
     * @return
     */
    public static List<Vm> provisionVms(List<CloudletDetails> cloudlets, int userId) {
        List<Vm> vmList = new ArrayList<>();
        Collections.sort(cloudlets, Comparator.comparingDouble(CloudletDetails::getCloudletSubmissionTime));

        // Iterate through given cloudlets for better execution
        for (CloudletDetails cloudlet : cloudlets) {
            InstanceType bestInstance = findBestInstance(cloudlet);
            if (!bestInstance.suitableVm) {
                System.err.println("No suitable VM found for Cloudlet: " + cloudlet.getCloudletId() + " with " + cloudlet.getCloudletLength() + " MIPS before deadline : " + cloudlet.getDeadline());
            }

            String instanceName = getInstanceName(bestInstance);
            if (runningVms.containsKey(instanceName)) {
                boolean cloudletDone = false;
                for (Map.Entry<Integer, Vm> vmEntry : runningVms.get(instanceName).entrySet()) {
                    Vm vm = vmEntry.getValue();
                    int coresAvailable = bestInstance.vCpus - coresOccupied.get(vm.getId());
                    if (coresAvailable > 0 && coresAvailable >= cloudlet.getNumberOfPes()) {
                        vmToCloudlets.computeIfAbsent(vm.getId(), k -> new HashMap<>()).put(coresOccupied.get(vm.getId()), cloudlet); // assign next available core for cloudlet
                        cloudlet.setGuestId(vm.getId()); // set the cloudlet guest ID.
                        updateDetailsForVm(1, cloudlet, vm.getId(), 0, bestInstance);
                        cloudletDone = true;
                        break;
                    } else {
                        List<Integer> checkings = new ArrayList<>();
                        int coreFlag = 0;
                        for (int i = 0; i < vm.getNumberOfPes(); i++) {
                            // check whether the task can be executed on given VM which has higher or equal amount of PEs utilized by current cloudlet
                            if (vmToCloudlets.containsKey(vm.getId()) && vmToCloudlets.get(vm.getId()).containsKey(i)) {
                                boolean isEnoughCoresAvailable = vmToCloudlets.get(vm.getId()).get(i).getNumberOfPes() >= cloudlet.getNumberOfPes();
                                if (!isEnoughCoresAvailable) {
                                    continue;
                                }
                            }
                            int check = checkSuitability(cloudlet, vm, i, bestInstance);
                            if (check == 1) {
                                coreFlag = i;
                            }
                            checkings.add(check);
                        }

                        // create a new VM or use existing one according to the check doing under for loop
                        if (checkings.contains(1)) {
                            int currentCore = 0;
                            for (Map.Entry<Integer, CloudletDetails> cloudletEntry : vmToCloudlets.get(vm.getId()).entrySet()) {
                                if (cloudletEntry.getValue().getCloudletId() == vmToCloudlets.get(vm.getId()).get(coreFlag).getCloudletId())
                                    currentCore = cloudletEntry.getKey();
                            }
                            vmToCloudlets.computeIfAbsent(vm.getId(), k -> new HashMap<>()).put(currentCore, cloudlet);
                            cloudlet.setGuestId(vm.getId()); // set the cloudlet guest ID.
                            updateDetailsForVm(2, cloudlet, vm.getId(), currentCore, bestInstance);
                        } else {
                            // Create another VM for same instance
                            numberOfEachInstance.put(instanceName, numberOfEachInstance.get(instanceName) + 1);
                            Vm newVm = createVm(userId, instanceName, bestInstance);
                            vmList.add(newVm);
                            runningVms.computeIfAbsent(instanceName, k -> new HashMap<>()).put(newVm.getId(), newVm);
                            updateDetailsForVm(0, cloudlet, newVm.getId(), 0, bestInstance); // update the usage of given VM
                            vmToCloudlets.computeIfAbsent(newVm.getId(), k -> new HashMap<>()).put(0, cloudlet);
                            cloudlet.setGuestId(newVm.getId()); // set the cloudlet guest ID.
                        }
                        cloudletDone = true;
                        break;
                    }
                }
                if (!cloudletDone) {
                    // Create another VM for same instance
                    numberOfEachInstance.put(instanceName, numberOfEachInstance.get(instanceName) + 1);
                    Vm newVm = createVm(userId, instanceName, bestInstance);
                    vmList.add(newVm);
                    runningVms.computeIfAbsent(instanceName, k -> new HashMap<>()).put(newVm.getId(), newVm);
                    updateDetailsForVm(0, cloudlet, newVm.getId(), 0, bestInstance); // update the usage of given VM
                    vmToCloudlets.computeIfAbsent(newVm.getId(), k -> new HashMap<>()).put(0, cloudlet);
                    cloudlet.setGuestId(newVm.getId()); // set the cloudlet guest ID.
                }
            } else {
                numberOfEachInstance.put(instanceName, 0);
                Vm vm = createVm(userId, instanceName, bestInstance);
                vmList.add(vm);
                runningVms.computeIfAbsent(instanceName, k -> new HashMap<>()).put(vm.getId(), vm);
                updateDetailsForVm(0, cloudlet, vm.getId(), 0, bestInstance); // update the usage of given VM
                vmToCloudlets.computeIfAbsent(vm.getId(), k -> new HashMap<>()).put(0, cloudlet);
                cloudlet.setGuestId(vm.getId()); // set the cloudlet guest ID.
            }
        }
        return vmList;
    }

    private static void updateDetailsForVm(int flag, CloudletDetails cloudlet, int vmId, int coreToBe, InstanceType instance) {
        double estimatedCompletionTime = cloudlet.getCloudletSubmissionTime() + (double) cloudlet.getCloudletLength() / (cloudlet.getNumberOfPes() * MIPS_PER_VCPU);
        if (flag == 0) { // New Vm creation with all free cores
            coresOccupied.put(vmId, cloudlet.getNumberOfPes());
            vmStartTimes.computeIfAbsent(vmId, k -> new HashMap<>()).put(0, cloudlet.getCloudletSubmissionTime());
            vmFinishTimes.computeIfAbsent(vmId, k -> new HashMap<>()).put(0, estimatedCompletionTime);
        } else if (flag == 1) { // Using existing Vm with free cores
            int core = coresOccupied.get(vmId);
            coresOccupied.put(vmId, coresOccupied.get(vmId) + cloudlet.getNumberOfPes());
            vmStartTimes.computeIfAbsent(vmId, k -> new HashMap<>()).put(core, cloudlet.getCloudletSubmissionTime());
            vmFinishTimes.computeIfAbsent(vmId, k -> new HashMap<>()).put(core, estimatedCompletionTime);
        } else if (flag == 2) { // cases where override the existing data where cloudlet can be started after finishing of the current cloudlet
            vmStartTimes.computeIfAbsent(vmId, k -> new HashMap<>()).put(coreToBe, cloudlet.getCloudletSubmissionTime());
            vmFinishTimes.computeIfAbsent(vmId, k -> new HashMap<>()).put(coreToBe, estimatedCompletionTime);
        }
    }

    private static int checkSuitability(CloudletDetails cloudlet, Vm vm, int core, InstanceType instance) {
        int result;
        double completionTime = (double) cloudlet.getCloudletLength() / (cloudlet.getNumberOfPes() * MIPS_PER_VCPU);
        double startTime = cloudlet.getCloudletSubmissionTime();
        if (vmFinishTimes.containsKey(vm.getId()) && vmFinishTimes.get(vm.getId()).containsKey(core) && vmToCloudlets.containsKey(vm.getId()) && vmToCloudlets.get(vm.getId()).containsKey(core)) {
            double finishTimeOfPrevCloudlet = vmFinishTimes.get(vm.getId()).get(core);
            if (startTime > finishTimeOfPrevCloudlet) {
                if (cloudlet.getDeadline() > startTime + completionTime) {
                    result = 1; // All good (Allow to use the selected running VM for this cloudlet
                } else {
                    result = 2; // cloudlet can be started in given VM but passes the deadline at the end
                }
            } else {
                result = 2; // cloudlet can not be started at given submission time on this VM
            }
        } else {
            result = -1; // cases where cloudlets utilize multiple cores, if cloudlet use 2 cores, core numbering at 0 and 2
        }
        return result;
    }

    private static String getInstanceName(InstanceType instance) {
        for (Map.Entry<String, DynamicVMProvisioningStrategy.InstanceType> entry : INSTANCE_TYPES.entrySet()) {
            if (entry.getValue().equals(instance)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static int createDynamicVmID(int userId, String instanceName, int numberOfInstance) {
        int vmId = userId + INSTANCE_TYPES.keySet().stream().toList().indexOf(instanceName);
        return Integer.parseInt(String.valueOf(vmId) + 0 + 0 + numberOfInstance);
    }

    private static Vm createVm(int userId, String instanceName, InstanceType instance) {
        return new Vm(
                createDynamicVmID(userId, instanceName, numberOfEachInstance.getOrDefault(instanceName, 0)),
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

}