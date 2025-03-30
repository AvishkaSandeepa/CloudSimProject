package org.cloudbus.cloudsim.projectone.roundrobin;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.projectone.comon.CloudletDetails;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CustomDatacenterBrokerRoundRobin extends DatacenterBroker {

    private static List<Vm> vmlist;
    private int lastVmIndex = 0;
    private static int totalCloudletRead = 0;
    private final String cloudletFilePath;
    private List<CloudletDetails> cloudletDetailsList;
    private int guestIndex = 0;
    private final Map<Integer, Queue<CloudletDetails>> vmTaskQueues = new HashMap<>(); // VM ID -> Task Queue
    private final Map<Integer, Integer> vmCoreUsage = new HashMap<>(); // VM ID -> Core Usage

    /**
     * Created a new CustomDatacenterBroker object for round robin algorithm.
     *
     * @param name name to be associated with this entity (as required by {@link org.cloudbus.cloudsim.core.SimEntity} class)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public CustomDatacenterBrokerRoundRobin(String name, String cloudletFilePath) throws Exception {
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

    /**
     * override the startEntity to get the created fixed VM list
     */
    @Override
    public void startEntity() {
        super.startEntity();
        vmlist = FixedVMSelectionForRoundRobin.findBestInstance(getCloudletList(), getId());
        submitGuestList(vmlist);
    }


    /**
     * Implement round robin algorithm to assign VMs to cloudlet
     * Ensure Cloudlets Are Assigned in a Cyclic Manner
     */
    @Override
    protected void submitCloudlets() {
        List<CloudletDetails> successfullySubmitted = new ArrayList<>();
        List<CloudletDetails> cloudlets = getCloudletList();
        List<GuestEntity> createdVmList = getGuestsCreatedList();
        int numCreatedVms = createdVmList.size();

        if (numCreatedVms == 0) {
            System.err.println(CloudSim.clock() + ": " + getName() + ": Error: No created VMs available for scheduling.");
            return;
        }

        for (CloudletDetails cloudlet : cloudlets) {
            GuestEntity vm;
            vm = createdVmList.get(lastVmIndex % numCreatedVms);
            if (cloudlet.getGuestId() != -1) {
                GuestEntity specificVm = VmList.getById(createdVmList, cloudlet.getGuestId());
                if (specificVm != null) {
                    vm = specificVm; // Override round-robin if a specific vm is designated.
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
            }
            if (!Log.isDisabled()) {
                Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Sending ", cloudlet.getClass().getSimpleName(),
                        " #", cloudlet.getCloudletId(), " to " + vm.getClassName() + " #", vm.getId());
            }

            double currentTime = CloudSim.clock();
            double delay = cloudlet.getCloudletSubmissionTime() - currentTime;
            if (delay > 0) {
                cloudlet.setGuestId(vm.getId());
                send(getVmsToDatacentersMap().get(vm.getId()), delay, CloudActionTags.CLOUDLET_SUBMIT, cloudlet);
                cloudletsSubmitted++;
                lastVmIndex++;
                guestIndex = (guestIndex + 1) % numCreatedVms; // Round-robin increment
                getCloudletSubmittedList().add(cloudlet);
                successfullySubmitted.add(cloudlet);
            } else {
                System.err.println("Task cannot be executed. current simulation time passes the task submission time. cloudletID : " + cloudlet.getCloudletId());
            }

        }

        // remove submitted cloudlets from waiting list
        getCloudletList().removeAll(successfullySubmitted);
    }

}
