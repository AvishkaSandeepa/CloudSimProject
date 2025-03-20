package org.cloudbus.cloudsim.project;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CustomDatacenterBroker extends DatacenterBroker {

    private static List<Vm> vmlist;
    private static int totalCloudletRead = 0;
    private final String cloudletFilePath;
    private List<CloudletDetails> cloudletDetailsList;
    /**
     * Next guest to which send the cloudlet
     */
    private int guestIndex = 0;
    private String workloadFilePath;
    private final Map<Integer, Queue<CloudletDetails>> vmTaskQueues = new HashMap<>(); // VM ID -> Task Queue
    private final Map<Integer, Integer> vmCoreUsage = new HashMap<>(); // VM ID -> Core Usage

    /**
     * Created a new CustomDatacenterBroker object.
     *
     * @param name name to be associated with this entity (as required by {@link org.cloudbus.cloudsim.core.SimEntity} class)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public CustomDatacenterBroker(String name, String cloudletFilePath) throws Exception {
        super(name);
        this.cloudletFilePath = cloudletFilePath;
        readWorkloadFile(super.getId(), cloudletFilePath);
    }

    public static List<Vm> getVmlist() {
        return vmlist;
    }

    public static int getTotalCloudletRead() {
        return totalCloudletRead;
    }

    /**
     * Read from text file
     * Get the cloudlets
     *
     * @param workloadFile
     * @return
     */
    private void readWorkloadFile(int userId, String workloadFile) {
        List<CloudletDetails> cloudletList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(workloadFile))) {
            String line;
            int cloudletId = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");
                if (values.length < 5) continue;

                double submissionTime = Double.parseDouble(values[0]);
                long length = Long.parseLong(values[1]);
                long minMemoryToExecute = Long.parseLong(values[2]);
                long minStorageToExecute = Long.parseLong(values[3]);
                double deadline = Double.parseDouble(values[4]);
                int pesNumber = 1;
                UtilizationModel utilizationModel = new UtilizationModelFull();

                cloudletList.add(new CloudletDetails(cloudletId, length, pesNumber, minStorageToExecute, minStorageToExecute, utilizationModel, utilizationModel, utilizationModel, submissionTime, minMemoryToExecute, minStorageToExecute, deadline));
                cloudletList.getLast().setUserId(userId);

                cloudletId++;
                totalCloudletRead++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.submitCloudletList(cloudletList);
        cloudletDetailsList = cloudletList;
    }

    public List<CloudletDetails> getCloudletDetailsList() {
        return cloudletDetailsList;
    }

    @Override
    public void startEntity() {
        super.startEntity();
        vmlist = VmProvisioningStrategy.provisionVms(getCloudletList(), getId());
        submitGuestList(vmlist);
    }


    // ==============================================
    @Override
    protected void submitCloudlets() {
        List<CloudletDetails> successfullySubmitted = new ArrayList<>();
        List<CloudletDetails> cloudlets = getCloudletList();
        Collections.sort(cloudlets, Comparator.comparingDouble(CloudletDetails::getCloudletSubmissionTime));
        for (CloudletDetails cloudlet : cloudlets) {
            GuestEntity vm;
            // if user didn't bind this cloudlet and it has not been executed yet
            if (cloudlet.getGuestId() == -1) {
                vm = getGuestsCreatedList().get(guestIndex);
            } else { // submit to the specific vm
                vm = VmList.getById(getGuestsCreatedList(), cloudlet.getGuestId());
                if (vm == null) { // vm was not created
                    vm = VmList.getById(getGuestList(), cloudlet.getGuestId()); // check if exists in the submitted list

                    if (!Log.isDisabled()) {
                        if (vm != null) {
                            Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Postponing execution of cloudlet ", cloudlet.getCloudletId(), ": bount ", vm.getClassName(), " #", vm.getId(), " not available");
                        } else {
                            Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Postponing execution of cloudlet ", cloudlet.getCloudletId(), ": bount guest entity of id ", cloudlet.getGuestId(), " doesn't exist");
                        }
                    }
                    continue;
                }
            }

            if (!Log.isDisabled()) {
                Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Sending ", cloudlet.getClass().getSimpleName(), " #", cloudlet.getCloudletId(), " to " + vm.getClassName() + " #", vm.getId());
            }

            cloudlet.setGuestId(vm.getId());
            vmTaskQueues.computeIfAbsent(vm.getId(), k -> new LinkedList<>());
            vmCoreUsage.putIfAbsent(vm.getId(), 0);

            if (vmCoreUsage.get(vm.getId()) < vm.getNumberOfPes()) {
                double currentTime = CloudSim.clock();
                double delay = cloudlet.getCloudletSubmissionTime() - currentTime;

                if (delay > 0) {
                    send(getVmsToDatacentersMap().get(vm.getId()), delay, CloudActionTags.CLOUDLET_SUBMIT, cloudlet);
                    cloudletsSubmitted++;
                    guestIndex = (guestIndex + 1) % getGuestsCreatedList().size();
                    getCloudletSubmittedList().add(cloudlet);
                    successfullySubmitted.add(cloudlet);
                    vmCoreUsage.put(vm.getId(), vmCoreUsage.get(vm.getId()) + 1);
                } else {
                    System.err.println("Task cannot be executed. current simulation time passes the task submission time. cloudletID : " + cloudlet.getCloudletId());
                }

            } else {
                vmTaskQueues.get(vm.getId()).add(cloudlet);
            }

        }

        // remove submitted cloudlets from waiting list
        getCloudletList().removeAll(successfullySubmitted);
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
        CloudletDetails cloudlet = (CloudletDetails) ev.getData();
        super.processCloudletReturn(ev);

        vmCoreUsage.put(cloudlet.getGuestId(), vmCoreUsage.get(cloudlet.getGuestId()) - 1);
        dispatchNextTask(cloudlet.getGuestId());
    }

    private void dispatchNextTask(int vmId) {
        if (!vmTaskQueues.get(vmId).isEmpty()) {
            Queue<CloudletDetails> cloudletDetails = vmTaskQueues.get(vmId);
            int cloudletId = -1;
            CloudletDetails nextCloudlet = cloudletDetails.poll();
            if (Objects.nonNull(nextCloudlet)) {
                cloudletId = nextCloudlet.getCloudletId();
            }
            GuestEntity vm = VmList.getById(getGuestsCreatedList(), vmId);
            if (vm == null) {
                vm = VmList.getById(getGuestList(), vmId); // check if exists in the submitted list

                if (!Log.isDisabled()) {
                    if (vm != null) {
                        Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Postponing execution of cloudlet ", cloudletId, ": bount ", vm.getClassName(), " #", vm.getId(), " not available");
                    } else {
                        Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Postponing execution of cloudlet ", cloudletId, ": bount guest entity of id ", vmId, " doesn't exist");
                    }
                }
            } else {
                if ((vmCoreUsage.get(vm.getId()) < vm.getNumberOfPes()) && Objects.nonNull(nextCloudlet)) {
                    double currentTime = CloudSim.clock();
                    double delay = nextCloudlet.getCloudletSubmissionTime() - currentTime;
                    if (delay > 0) {
                        send(getVmsToDatacentersMap().get(vm.getId()), delay, CloudActionTags.CLOUDLET_SUBMIT, nextCloudlet);
                        cloudletsSubmitted++;
                        guestIndex = (guestIndex + 1) % getGuestsCreatedList().size();
                        getCloudletSubmittedList().add(nextCloudlet);
                        vmCoreUsage.put(vm.getId(), vmCoreUsage.get(vm.getId()) + 1);
                    } else {
                        System.err.println("Task cannot be executed. current simulation time passes the task submission time. cloudletID : " + cloudletId);
                    }
                } else {
                    vmTaskQueues.get(vm.getId()).add(nextCloudlet);
                }
            }
        }
    }
}
