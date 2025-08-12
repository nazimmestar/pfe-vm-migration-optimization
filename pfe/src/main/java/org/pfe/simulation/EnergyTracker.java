package org.pfe.simulation;

import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.datacenters.Datacenter;

import java.util.*;

public class EnergyTracker {

    private final Map<Integer, List<Double>> hostEnergyMap = new HashMap<>();

    public void addEnergy(int hostId, double energy) {
        hostEnergyMap.computeIfAbsent(hostId, k -> new ArrayList<>()).add(energy);
    }

    public Map<Integer, List<Double>> getHostEnergyMap() {
        return hostEnergyMap;
    }
}



