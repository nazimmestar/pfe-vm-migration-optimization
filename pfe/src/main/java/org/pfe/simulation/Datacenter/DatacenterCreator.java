package org.pfe.simulation.Datacenter;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.pfe.simulation.Simulation.SimulationManager;

import java.util.ArrayList;
import java.util.List;


public class DatacenterCreator {

    private static final int  HOSTS = 16;
    private static final int  HOST_PES = 32;
    private static final int  HOST_MIPS = 2500; // Milion Instructions per Second (MIPS)
    private static final int  HOST_RAM = 131072; //in Megabytes
    private static final long HOST_BW = 80000; //in Megabits/s
    private static final long HOST_STORAGE = 800000; //in Megabytes

    private static List<Host> hostList;

    public static Datacenter createDatacenter(CloudSimPlus simulation) {
        hostList = new ArrayList<>(HOSTS);
        for (int i = 0; i < HOSTS; i++) {
            hostList.add(createHost());
        }
        return new DatacenterSimple(simulation, hostList, new VmAllocationPolicyFirstFit());
    }


    public static List<Host> getHostList() {
        return hostList;
    }

    private static Host createHost() {
        final var peList = new ArrayList<Pe>(HOST_PES);
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(HOST_MIPS));
        }
        HostSimple host = new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
        host.setVmScheduler(new VmSchedulerTimeShared());
        host.setStateHistoryEnabled(true);
        return host;
    }

}

