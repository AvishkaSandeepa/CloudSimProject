package org.cloudbus.cloudsim.projectone.dynamicvmprovisioning;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomDatacenter extends Datacenter {

    private final Map<Integer, Double> vmStartTimes = new HashMap<>();
    private final Map<Integer, Double> vmFinishTimes = new HashMap<>();

    /**
     * Allocates a new Datacenter object.
     *
     * @param name               the name to be associated with this entity (as required by the super class)
     * @param characteristics    the characteristics of the datacenter to be created
     * @param vmAllocationPolicy the policy to be used to allocate VMs into hosts
     * @param storageList        a List of storage elements, for data simulation
     * @param schedulingInterval the scheduling delay to process each datacenter received event
     * @throws Exception when one of the following scenarios occur:
     *                   <ul>
     *                     <li>creating this entity before initializing CloudSim package
     *                     <li>this entity name is <tt>null</tt> or empty
     *                     <li>this entity has <tt>zero</tt> number of PEs (Processing Elements). <br/>
     *                     No PEs mean the Cloudlets can't be processed. A CloudResource must contain
     *                     one or more Machines. A Machine must contain one or more PEs.
     *                   </ul>
     * @pre name != null
     * @pre resource != null
     * @post $none
     */
    public CustomDatacenter(String name, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
    }


    /**
     * override the processVmCreate method to capture the start time of the vm
     *
     * @param ev information about the event just happened
     * @param ack indicates if the event's sender expects to receive
     * an acknowledge message when the event finishes to be processed
     *
     */
    @Override
    protected void processVmCreate(SimEvent ev, boolean ack) {
        GuestEntity guest = (GuestEntity) ev.getData();

        boolean result;
        HostEntity userPreferredHost = guest.getHost();
        if (userPreferredHost != null && getVmAllocationPolicy().getHostList().contains(userPreferredHost)) {
            result = getVmAllocationPolicy().allocateHostForGuest(guest, userPreferredHost);
        } else {
            result = getVmAllocationPolicy().allocateHostForGuest(guest);
        }

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = guest.getId();
            data[2] = result ? CloudSimTags.TRUE : CloudSimTags.FALSE;
            send(guest.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudActionTags.VM_CREATE_ACK, data);
        }

        if (result) {
            getVmList().add(guest);

            vmStartTimes.put(guest.getId(), CloudSim.clock());

            if (guest.isBeingInstantiated()) {
                guest.setBeingInstantiated(false);
            }

            guest.updateCloudletsProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(guest).getGuestScheduler()
                    .getAllocatedMipsForGuest(guest));
        } else {
            Log.printlnConcat(CloudSim.clock(), ": Datacenter.guestAllocator: Couldn't find a host for ", guest.getClassName(), " #", guest.getId());
        }
    }

    /**
     * override the processVmDestroy method to capture the end time of the vm
     *
     * @param ev information about the event just happened
     * @param ack indicates if the event's sender expects to receive
     * an acknowledge message when the event finishes to be processed
     *
     */
    @Override
    protected void processVmDestroy(SimEvent ev, boolean ack) {
        GuestEntity vm = (GuestEntity) ev.getData();
        getVmAllocationPolicy().deallocateHostForGuest(vm);

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = vm.getId();
            data[2] = CloudSimTags.TRUE;

            sendNow(vm.getUserId(), CloudActionTags.VM_DESTROY_ACK, data);
        }

        vmFinishTimes.put(vm.getId(), CloudSim.clock());

        getVmList().remove(vm);
    }

    public Map<Integer, Double> getVmStartTimes() {
        return vmStartTimes;
    }

    public Map<Integer, Double> getVmFinishTimes() {
        return vmFinishTimes;
    }
}
