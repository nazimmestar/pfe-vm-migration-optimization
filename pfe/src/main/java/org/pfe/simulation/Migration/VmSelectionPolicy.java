package org.pfe.simulation.Migration;

import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.hosts.Host;

import java.util.Comparator;
import java.util.List;

public class VmSelectionPolicy {
    /**
     * Sélectionne la VM à migrer depuis un hôte surchargé.
     * Politique : la VM avec la plus faible consommation CPU (minimize impact).
     */
    public static Vm selectVmToMigrate(Host host) {
        List<Vm> vmList = host.getVmList();

        if (vmList.isEmpty()) {
            return null;
        }

        // Choisir la VM avec la plus petite utilisation CPU (meilleure candidate à migrer)
        return vmList.stream()
                .filter(vm -> !vm.isInMigration()) // éviter les VMs déjà en migration
                .min(Comparator.comparingDouble(Vm::getCpuPercentUtilization))
                .orElse(null);
    }

    /**
     * Sélectionne toutes les VMs d’un hôte sous-utilisé (pour l’éteindre).
     */
    public static List<Vm> selectAllVmsToMigrate(Host host) {
        return host.getVmList().stream()
                .filter(vm -> !vm.isInMigration())
                .toList();
    }
}
