package org.pfe.simulation.Migration;


import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.Comparator;
import java.util.List;
public class VmPlacementPolicy {

    /**
     * Place une VM migrée vers le meilleur hôte selon BFD (Best Fit Decreasing).
     * Le meilleur hôte est celui qui accepte la VM et dont la consommation énergétique estimée est minimale.
     */
    public static Host findBestHostForVm(Datacenter datacenter, Vm vm) {
        List<Host> hosts = datacenter.getHostList();

        return hosts.stream()
                .filter(host -> host.isActive()) // NE PREND QUE LES HÔTES ACTIFS
                .filter(host -> host.isSuitableForVm(vm))
                .filter(host -> host.getId() != vm.getHost().getId())
                .min(Comparator.comparingDouble(host -> estimatePowerAfterPlacement(host, vm)))
                .orElse(null);
    }

    /**
     * Estimation simple de la consommation après placement (basée sur le modèle Pmax × (0.7 + 0.3 × CPU))
     */
    private static double estimatePowerAfterPlacement(Host host, Vm vm) {
        final double Pmax = 250;

        double currentCpu = host.getCpuPercentUtilization();
        double vmCpu = vm.getCpuPercentUtilization();

        double projectedCpu = currentCpu + vmCpu; // sans limiter
        return Pmax * (0.7 + 0.3 * projectedCpu);
    }
}
