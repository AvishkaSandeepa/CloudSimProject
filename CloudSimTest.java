//package org.cloudbus.cloudsim.project;
//
//import org.cloudbus.cloudsim.*;
//import org.cloudbus.cloudsim.core.CloudActionTags;
//import org.cloudbus.cloudsim.core.CloudSim;
//import org.cloudbus.cloudsim.core.CloudSimTags;
//import org.cloudbus.cloudsim.core.SimEvent;
//import org.cloudbus.cloudsim.lists.VmList;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.*;
//
//public class CloudSimTest extends DatacenterBroker {
//
//    private String workloadFilePath;
//    private Map<Integer, Queue<Cloudlet>> vmTaskQueues = new HashMap<>(); // VM ID -> Task Queue
//    private Map<Integer, Integer> vmCoreUsage = new HashMap<>(); // VM ID -> Core Usage
//
//    public CloudSimTest(String name, String workloadFilePath) throws Exception {
//        super(name);
//        this.workloadFilePath = workloadFilePath;
//        readWorkloadFile();
//    }
//
//    private void readWorkloadFile() {
//        List<CloudletDetails> cloudlets = new ArrayList<>();
//        UtilizationModel cpuUtil = new UtilizationModelFull();
//        UtilizationModel ramUtil = new UtilizationModelFull();
//        UtilizationModel bwUtil = new UtilizationModelFull();
//
//        try (BufferedReader br = new BufferedReader(new FileReader(workloadFilePath))) {
//            String line;
//            int cloudletId = 1;
//            while ((line = br.readLine()) != null) {
//                String[] parts = line.split("\\s+");
//                if (parts.length == 5) {
//                    double submissionTime = Double.parseDouble(parts[0]);
//                    long mis = Long.parseLong(parts[1]);
//                    long memory = Long.parseLong(parts[2]);
//                    long storage = Long.parseLong(parts[3]);
//                    double deadline = Double.parseDouble(parts[4]);
//
//                    cloudlets.add(new CloudletDetails(cloudletId++, mis, 1, 1024, 2048, cpuUtil, ramUtil, bwUtil, submissionTime, memory, storage, deadline));
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        List<Cloudlet> cloudletList = new ArrayList<>(cloudlets);
//        super.submitCloudletList(cloudletList);
//    }
//
//    @Override
//    public void startEntity() {
//        super.startEntity();
//        provisionVms();
//    }
//
//    private void provisionVms() {
//        List<Vm> vmList = ProvisioningStrategy.provisionVms(getCloudletList(), getId());
//        submitGuestList(vmList);
//    }
//
//    @Override
//    protected void submitCloudlets() {
//        List<Cloudlet> cloudlets = getCloudletList();
//        cloudlets.sort(Comparator.comparingDouble(Cloudlet::getSubmissionTime));
//
//        for (Cloudlet cloudlet : cloudlets) {
//            scheduleCloudlet(cloudlet);
//        }
//    }
//
//    private void scheduleCloudlet(Cloudlet cloudlet) {
//        double currentTime = CloudSim.clock();
//        if (Math.abs(currentTime - cloudlet.getSubmissionTime()) < 0.001) { // Task arrival
//            Vm vm = VmList.getById(getGuestsCreatedList(), cloudlet.getGuestId());
//            int vmId = vm.getId();
//            int vmCores = vm.getNumberOfPes();
//
//            vmTaskQueues.computeIfAbsent(vmId, k -> new LinkedList<>());
//            vmCoreUsage.putIfAbsent(vmId, 0);
//
//            if (vmCoreUsage.get(vmId) < vmCores) {
//                submitCloudletToVm(cloudlet, vmId);
//            } else {
//                vmTaskQueues.get(vmId).add(cloudlet);
//            }
//        }
//    }
//
//    private void submitCloudletToVm(Cloudlet cloudlet, int vmId) {
//        vmCoreUsage.put(vmId, vmCoreUsage.get(vmId) + 1);
//        sendNow(getVmsToDatacentersMap().get(vmId), CloudActionTags.CLOUDLET_SUBMIT, cloudlet);
//    }
//
//    @Override
//    protected void processCloudletReturn(SimEvent ev) {
//        Cloudlet cloudlet = (Cloudlet) ev.getData();
//        super.processCloudletReturn(ev);
//
//        int vmId = cloudlet.getGuestId();
//        vmCoreUsage.put(vmId, vmCoreUsage.get(vmId) - 1);
//
//        dispatchNextTask(vmId);
//    }
//
//    private void dispatchNextTask(int vmId) {
//        if (!vmTaskQueues.get(vmId).isEmpty() && vmCoreUsage.get(vmId) < VmList.getById(getGuestsCreatedList(), vmId).getNumberOfPes()) {
//            Cloudlet nextCloudlet = vmTaskQueues.get(vmId).poll();
//            submitCloudletToVm(nextCloudlet, vmId);
//        }
//    }
//
//    @Override
//    public void processEvent(SimEvent ev){
//        super.processEvent(ev);
//        if (ev.getTag() == CloudActionTags.VM_CREATE_ACK){
//            submitCloudlets();
//        }
//    }
//}