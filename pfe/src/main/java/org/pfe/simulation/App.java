package org.pfe.simulation;

import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.HostHistoryTableBuilder;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostStateHistoryEntry;
import org.cloudsimplus.schedulers.MipsShare;
import org.cloudsimplus.vms.Vm;
import org.pfe.simulation.Cloudlet.CloudletGenerator;
import org.pfe.simulation.Datacenter.DatacenterCreator;
import org.pfe.simulation.Migration.ThresholdCalculator;
import org.pfe.simulation.Migration.ThresholdStrategy;
import org.pfe.simulation.Migration.ThresholdType;
import org.pfe.simulation.Migration.MigrationPolicyIdentifier;


import org.pfe.simulation.Migration.VmPlacementPolicy;
import org.pfe.simulation.Migration.VmSelectionPolicy;
import org.pfe.simulation.Simulation.SimulationManager;
import org.pfe.simulation.Vm.VmCreator;

import java.io.*;
import java.util.*;


public class App {

    private static final EnergyTracker energyTracker = new EnergyTracker();
    private static final Map<Integer, List<Double>> hostAvgCpuMap = new HashMap<>();
    private static final List<Double> migrationTimes = new ArrayList<>();
    private static final List<Double> migrationTimeline = new ArrayList<>();
    private static final List<Double> cumulativeEnergy = new ArrayList<>();
    private static final Map<Integer, List<Double>> tUpperMap = new HashMap<>();
    private static final Map<Integer, List<Double>> tLowerMap = new HashMap<>();
    private static double currentEnergyTotal = 0;
    private static final double Pmax = 250; // Puissance max d’un hôte en watts
    private static final double timeStepSeconds = 1.0; // chaque tick = 1 seconde
    private static final List<Integer> activeHostCounts = new ArrayList<>();


    private static int migrationCount = 0;
    private static double totalMigrationEnergy = 0;



    public static void runSimulation(List<CloudletSimple> cloudletList) {

        // Réinitialisation des variables globales
        cumulativeEnergy.clear();
        migrationCount = 0;
        migrationTimes.clear();
        migrationTimeline.clear();
        currentEnergyTotal = 0;
        totalMigrationEnergy = 0;
        hostAvgCpuMap.clear();

        CloudSimPlus simulation = SimulationManager.createNewSimulation();


        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);
        Datacenter datacenter0 = DatacenterCreator.createDatacenter(simulation);

        List<Vm> vmList = VmCreator.CreateVmList();
        broker.submitCloudletList(cloudletList);



        broker.submitCloudletList(cloudletList);

        for (Vm vm : vmList) {
            broker.submitVm(vm);
            bindCloudletsToVm(broker, cloudletList, vm);

        }

        simulation.addOnClockTickListener(evt -> {
            if ((int) simulation.clock() % 1 == 0) {
                System.out.printf("⏱️ Enregistrement énergie à %.0f sec%n", simulation.clock());
                printEnergyConsumption(datacenter0);
//                printHostsStatus(datacenter0);
                monitorSystem(datacenter0);
                int activeHosts = (int) datacenter0.getHostList().stream()
                        .filter(Host::isActive)
                        .count();

                activeHostCounts.add(activeHosts);

                System.out.printf("🟢 Hôtes actifs à %.0f sec : %d%n", simulation.clock(), activeHosts);

                for (Host host : datacenter0.getHostList()) {
                    List<Vm> vms = host.getVmList();
                    double avgCpu = vms.isEmpty() ? 0 :
                            vms.stream()
                                    .mapToDouble(vm -> vm.getCpuPercentUtilization() * 100)
                                    .average()
                                    .orElse(0);

                    hostAvgCpuMap
                            .computeIfAbsent((int) host.getId(), k -> new ArrayList<>())
                            .add(avgCpu);
                }
            }
        });


        simulation.addOnClockTickListener(evt -> {
            double time = simulation.clock();

            System.out.printf("🔎 Suivi des VMs actives à %.2f sec%n", time);

            for (Vm vm : broker.getVmCreatedList()) {
                boolean isExecuting = !vm.getCloudletScheduler().getCloudletExecList().isEmpty();

                if (isExecuting) {
                    Host host = vm.getHost();
                    double cpuUtil = vm.getCpuPercentUtilization() * 100;

                    System.out.printf("➡️ VM %d (Host %d) - CPU: %.2f%%%n", vm.getId(), host.getId(), cpuUtil);
                }
            }

            System.out.println("----------------------------------");
        });




        ThresholdCalculator.setStrategy(SimulationConfig.THRESHOLD_STRATEGY);
        ThresholdCalculator.setThresholdType(SimulationConfig.THRESHOLD_TYPE);


        simulation.start();

        System.out.println("========== Résultats des Cloudlets ==========");
        new CloudletsTableBuilder(broker.getCloudletFinishedList()).build();

        System.out.println("========== Utilisation des Hôtes ==========");
        datacenter0.getHostList().forEach(App::printHostHistory);
        System.out.println("========== Statistiques des migrations ==========");
        System.out.printf("🔁 Nombre total de migrations : %d%n", migrationCount);
        System.out.printf("⚡ Énergie totale des migrations : %.4f Wh%n", totalMigrationEnergy);
        double totalMigrationTime = migrationTimes.stream().mapToDouble(Double::doubleValue).sum();
        System.out.printf("⏱ Temps total de migration : %.2f sec%n", totalMigrationTime);

        saveEnergyGraphData();
        saveHostAvgCpuData();
        saveMigrationTimeline();
        saveCumulativeEnergyCSV(SimulationConfig.getPolicyName());
        saveTupperTlowerData();
        saveActiveHostsCSV();


        System.out.println("Simulation terminée.");
    }

    private static void printHostHistory(Host host) {
        boolean cpuUsed =
                host.getStateHistory().stream()
                        .map(HostStateHistoryEntry::percentUsage)
                        .anyMatch(cpuUtilization -> cpuUtilization > 0);

        if (cpuUsed) {
            new HostHistoryTableBuilder(host).setTitle("Host " + host.getId() + " History").build();
        } else {
            System.out.printf("\tHost %d : CPU was zero all the time%n", host.getId());
        }
    }

    private static void bindCloudletsToVm(DatacenterBrokerSimple broker, List<CloudletSimple> cloudlets, Vm vm) {
        for (CloudletSimple cloudlet : cloudlets) {
            if (cloudlet.getVm() == null) {
                broker.bindCloudletToVm(cloudlet, vm);
            }
        }
    }


    private static void printHostsStatus(Datacenter datacenter) {
        System.out.println("=== État des hôtes ===");
        for (Host host : datacenter.getHostList()) {
            double cpuUtil = host.getCpuPercentUtilization() * 100; // because the unit is percent % so we mutiply by 100

            long totalRam = host.getRam().getCapacity();
            long freeRam = host.getRamProvisioner().getAvailableResource();
            long allocatedRam = totalRam - freeRam;
            double ramUtil = (double) allocatedRam * 100 / totalRam;

            System.out.printf("Host %d - CPU: %.2f%%, RAM: %.2f%%%n", host.getId(), cpuUtil, ramUtil);
        }
    }


    private static void printEnergyConsumption(Datacenter datacenter) {
        double totalPower = 0;

        boolean consolidation = SimulationConfig.CONSOLIDATION_ENABLED;

        for (Host host : datacenter.getHostList()) {
            double cpuUtil = host.getCpuPercentUtilization();

            if (consolidation && cpuUtil == 0.0) {
                continue; // ignorer les hôtes éteints
            }

            // TOUJOURS calculer l’énergie sinon
            double power = Pmax * (0.7 + 0.3 * cpuUtil); // ou modèle plus complexe
            totalPower += power;
            energyTracker.addEnergy((int) host.getId(), power);


        }

        // Convertir puissance (W) en énergie (Wh) pour 1 seconde = (1/3600) heure
        double energyThisTick = totalPower * (timeStepSeconds / 3600.0);
        currentEnergyTotal += energyThisTick;
        cumulativeEnergy.add(currentEnergyTotal);

        System.out.printf("⚡ Énergie cumulée : %.4f Wh%n", currentEnergyTotal);


    }





    private static void monitorSystem(Datacenter datacenter) {
        System.out.println("------ Monitoring System (avec migration) ------");

        // Étape 1 : faire toutes les migrations
        for (Host host : datacenter.getHostList()) {
            double cpuUtil = host.getCpuPercentUtilization();

            // Calcul des seuils dynamiques
            double tUpper = ThresholdCalculator.calculateTupper(host);
            double tLower = ThresholdCalculator.calculateTlower(host);
            tUpperMap.computeIfAbsent((int) host.getId(), k -> new ArrayList<>()).add(tUpper);
            tLowerMap.computeIfAbsent((int) host.getId(), k -> new ArrayList<>()).add(tLower);


            System.out.printf("Host %d - CPU: %.2f%% | Tupper: %.2f | Tlower: %.2f%n",
                    host.getId(), cpuUtil * 100, tUpper, tLower);

            if (cpuUtil > tUpper) {
                System.out.println("⚠️ Surcharge détectée ! Migration d'une VM en cours...");
                Vm vmToMigrate = VmSelectionPolicy.selectVmToMigrate(host);

                if (vmToMigrate != null) {
                    Host targetHost = VmPlacementPolicy.findBestHostForVm(datacenter, vmToMigrate);
                    if (targetHost != null) {
                        migrateVm(vmToMigrate, targetHost);
                    } else {
                        System.out.println("❌ Aucun hôte disponible pour la migration.");
                    }
                }
            } else if (SimulationConfig.CONSOLIDATION_ENABLED && cpuUtil < tLower) {
                System.out.println("ℹ️ Hôte sous-utilisé. Migration de toutes les VMs...");
                List<Vm> vmsToMigrate = VmSelectionPolicy.selectAllVmsToMigrate(host);

                for (Vm vm : vmsToMigrate) {
                    Host targetHost = VmPlacementPolicy.findBestHostForVm(datacenter, vm);
                    if (targetHost != null) {
                        migrateVm(vm, targetHost);
                    } else {
                        System.out.printf("❌ Pas de cible trouvée pour VM %d%n", vm.getId());
                    }
                }
            }

        }

        // Étape 2 : maintenant que toutes les VMs ont été migrées, éteindre les hôtes vides

            if (SimulationConfig.CONSOLIDATION_ENABLED) {
                for (Host host : datacenter.getHostList()) {
                    if (host.getVmList().isEmpty() && host.isActive()) {
                        host.setActive(false);
                        System.out.printf("🔌 Hôte %d est vide et a été éteint pour économiser l'énergie.%n", host.getId());
                    }
                }
            }



        for (Host h : datacenter.getHostList()) {
            if (h.isActive()) {
                System.out.printf("🖥️ Host %d (active): CPU util %.2f, RAM libre: %d%n",
                        h.getId(),
                        h.getCpuPercentUtilization(),
                        h.getRamProvisioner().getAvailableResource());
            }
        }

    }





    private static void saveEnergyGraphData() {
        Map<Integer, List<Double>> data = energyTracker.getHostEnergyMap();

        try (PrintWriter writer = new PrintWriter("host_energy.csv")) {
            // En-tête
            StringBuilder header = new StringBuilder("Time");
            for (Integer hostId : data.keySet()) {
                header.append(",Host_").append(hostId);
            }
            writer.println(header);

            int maxLength = data.values().stream().mapToInt(List::size).max().orElse(0);
            for (int i = 0; i < maxLength; i++) {
                StringBuilder row = new StringBuilder(String.valueOf(i * 10)); // ex : pas de 10s
                for (Integer hostId : data.keySet()) {
                    List<Double> values = data.get(hostId);
                    row.append(",").append(i < values.size() ? values.get(i) : "");
                }
                writer.println(row);
            }

            System.out.println("✅ Fichier 'host_energy.csv' généré.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private static void migrateVm(Vm vm, Host targetHost) {
        Host oldHost = vm.getHost();

        System.out.printf("🔄 Migration de VM %d de Host %d vers Host %d%n",
                vm.getId(), oldHost.getId(), targetHost.getId());

        // 🔢 Estimer le temps de migration : (taille mémoire en Mbits) / bande passante (Mbps)
        long ramMb = vm.getRam().getCapacity();
        double bwMbps = Math.min(oldHost.getBw().getCapacity(), targetHost.getBw().getCapacity());
        double migrationTime = (ramMb * 8.0) / bwMbps;

        System.out.printf("⏱ Temps estimé de migration : %.2f sec%n", migrationTime);

        // 🔋 Consommation énergétique (simplifiée) : Pmax * temps
        final double Pmax = 250; // en watts
        double energy = Pmax * (migrationTime / 3600.0); // en Wh

        System.out.printf("⚡ Énergie estimée pour la migration : %.4f Wh%n", energy);

        // 🔁 Migration simulée
        oldHost.getVmList().remove(vm);
        targetHost.createVm(vm);

        // 🧮 Mise à jour des stats
        migrationCount++;
        totalMigrationEnergy += energy;
        migrationTimes.add(migrationTime);
//        migrationTimeline.add(SimulationManager.getSimulation().clock());
        migrationTimes.add(migrationTime);



    }



    private static void saveMigrationTimeline() {
        try (PrintWriter writer = new PrintWriter("migration_timeline.csv")) {
            writer.println("Time(s),CumulativeMigrations");

            int count = 0;
            for (double time : migrationTimeline) {
                count++;
                writer.printf("%.2f,%d%n", time, count);
            }

            System.out.println("✅ Fichier 'migration_timeline.csv' généré.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveCumulativeEnergyCSV(String strategyName) {
        String policyName = SimulationConfig.getPolicyName();
        String energyFile = "results/energy_" + policyName + ".csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(energyFile))) {
            writer.println("Time(s),Energy_Wh");
            for (int i = 0; i < cumulativeEnergy.size(); i++) {
                writer.printf("%d,%.4f%n", i * 1, cumulativeEnergy.get(i)); // ici 1 = intervalle entre ticks
            }
            System.out.printf("✅ Fichier 'energy_%s.csv' généré.%n", strategyName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static void saveHostAvgCpuData() {
        String policyName = SimulationConfig.getPolicyName(); // STATIC, DYNAMIC, HYBRID
        String cpuFile = "results/host_avg_cpu_" + policyName + ".csv";

        try (PrintWriter writer = new PrintWriter(cpuFile)) {
            StringBuilder header = new StringBuilder("Time");
            for (Integer hostId : hostAvgCpuMap.keySet()) {
                header.append(",Host_").append(hostId);
            }
            writer.println(header);

            int maxTicks = hostAvgCpuMap.values().stream().mapToInt(List::size).max().orElse(0);
            for (int i = 0; i < maxTicks; i++) {
                StringBuilder row = new StringBuilder(String.valueOf(i * 10)); // temps en secondes
                for (Integer hostId : hostAvgCpuMap.keySet()) {
                    List<Double> values = hostAvgCpuMap.get(hostId);
                    row.append(",").append(i < values.size() ? values.get(i) : "");
                }
                writer.println(row);
            }

            System.out.printf("✅ Fichier 'host_avg_cpu_%s.csv' généré.%n", policyName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void saveTupperTlowerData() {
        String policyName = SimulationConfig.getPolicyName();
        String filename = "results/tupper_tlower_" + policyName + ".csv";

        try (PrintWriter writer = new PrintWriter(filename)) {
            StringBuilder header = new StringBuilder("Time");
            for (Integer hostId : tUpperMap.keySet()) {
                header.append(",Tupper_H").append(hostId).append(",Tlower_H").append(hostId);
            }
            writer.println(header);

            int maxTicks = tUpperMap.values().stream().mapToInt(List::size).max().orElse(0);
            for (int i = 0; i < maxTicks; i++) {
                StringBuilder row = new StringBuilder(String.valueOf(i));
                for (Integer hostId : tUpperMap.keySet()) {
                    List<Double> tUppers = tUpperMap.getOrDefault(hostId, new ArrayList<>());
                    List<Double> tLowers = tLowerMap.getOrDefault(hostId, new ArrayList<>());

                    row.append(",")
                            .append(i < tUppers.size() ? String.format("%.2f", tUppers.get(i)) : "")
                            .append(",")
                            .append(i < tLowers.size() ? String.format("%.2f", tLowers.get(i)) : "");
                }
                writer.println(row);
            }

            System.out.printf("✅ Fichier 'tupper_tlower_%s.csv' généré.%n", policyName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveActiveHostsCSV() {
        String policyName = SimulationConfig.getPolicyName(); // STATIC, DYNAMIC, HYBRID
        String filePath = "results/active_hosts_" + policyName + ".csv";

        try (PrintWriter writer = new PrintWriter(filePath)) {
            writer.println("Time(s),Active_Hosts");

            for (int i = 0; i < activeHostCounts.size(); i++) {
                writer.printf("%d,%d%n", i, activeHostCounts.get(i));
            }

            System.out.printf("✅ Fichier 'active_hosts_%s.csv' généré.%n", policyName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}

