package org.pfe.simulation;

import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.HostHistoryTableBuilder;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostStateHistoryEntry;
import org.cloudsimplus.vms.Vm;
import org.pfe.simulation.Cloudlet.CloudletGenerator;
import org.pfe.simulation.Datacenter.DatacenterCreator;
import org.pfe.simulation.Migration.ThresholdCalculator;
import org.pfe.simulation.Migration.VmPlacementPolicy;
import org.pfe.simulation.Migration.VmSelectionPolicy;
import org.pfe.simulation.Simulation.SimulationManager;
import org.pfe.simulation.Vm.VmCreator;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Appbase {

    private static final EnergyTracker energyTracker = new EnergyTracker();
    private static final Map<Integer, List<Double>> hostAvgCpuMap = new HashMap<>();
    private static final double Pmax = 250; // Puissance max des hôtes
    private static final double timeStepSeconds = 1; // pas de simulation (ex: 10s)
    private static double currentEnergyTotal = 0; // cumulée
    private static final List<Double> cumulativeEnergy = new ArrayList<>();



    public static void main(String[] args) {
        runSimulation();
    }

    public static void runSimulation() {
        CloudSimPlus simulation = SimulationManager.createNewSimulation();



        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);
        Datacenter datacenter0 = DatacenterCreator.createDatacenter(simulation);
        List<Vm> vmList = VmCreator.CreateVmList();
        List<CloudletSimple> cloudletList = CloudletGenerator.createCloudlets();



        broker.submitCloudletList(cloudletList);

        for (Vm vm : vmList) {
            broker.submitVm(vm);
            bindCloudletsToVm(broker, cloudletList, vm);

        }

        simulation.addOnClockTickListener(evt -> {
            if ((int) simulation.clock() % 1 == 0) {
                System.out.printf("⏱️ Enregistrement énergie à %.0f sec%n", simulation.clock());
                printEnergyConsumption(datacenter0);
                monitorSystem();
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




        simulation.start();

        System.out.println("========== Résultats des Cloudlets ==========");
        new CloudletsTableBuilder(broker.getCloudletFinishedList()).build();

        System.out.println("========== Utilisation des Hôtes ==========");
        datacenter0.getHostList().forEach(Appbase::printHostHistory);

        saveCumulativeEnergyCSV("base");
        saveHostAvgCpuData();




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
            double cpuUtil = host.getCpuPercentUtilization() * 100;

            long totalRam = host.getRam().getCapacity();
            long freeRam = host.getRamProvisioner().getAvailableResource();
            long allocatedRam = totalRam - freeRam;
            double ramUtil = (double) allocatedRam * 100 / totalRam;

            System.out.printf("Host %d - CPU: %.2f%%, RAM: %.2f%%%n", host.getId(), cpuUtil, ramUtil);
        }
    }


    private static void printEnergyConsumption(Datacenter datacenter) {
        double totalPower = 0;

        for (Host host : datacenter.getHostList()) {
            double cpuUtil = host.getCpuPercentUtilization();
            double power = Pmax * (0.7 + 0.3 * cpuUtil); // modèle de puissance linéaire
            totalPower += power;

            energyTracker.addEnergy((int) host.getId(), power); // enregistrement par hôte

            System.out.printf("Host %d power consumption: %.2f W%n", host.getId(), power);
        }

        // Convertir puissance (W) en énergie (Wh) pour 1 tick
        double energyThisTick = totalPower * (timeStepSeconds / 3600.0);
        currentEnergyTotal += energyThisTick;
        cumulativeEnergy.add(currentEnergyTotal);

        System.out.printf("⚡ Énergie cumulée : %.4f Wh%n", currentEnergyTotal);
    }



    private static void monitorSystem() {
        System.out.println("------ Monitoring System ------");
        for (Host host : DatacenterCreator.getHostList()) {
            double cpuUtil = host.getCpuPercentUtilization() * 100;

            long allocatedRam = host.getRam().getCapacity() - host.getRamProvisioner().getAvailableResource();
            double ramUtil = (double) allocatedRam * 100 / host.getRam().getCapacity();

            System.out.printf("Host %d - CPU: %.2f%% | RAM: %.2f%%%n", host.getId(), cpuUtil, ramUtil);

            // Add migration check here
            if (cpuUtil > 80.0) {
                System.out.println("⚠️ Overloaded Host detected. Starting migration...");
                // Call your migration method here
            }
        }


    }






    private static void saveCumulativeEnergyCSV(String strategyName) {
        String energyFile = "energy_" + strategyName + ".csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(energyFile))) {
            writer.println("Time(s),Energy_Wh");
            for (int i = 0; i < cumulativeEnergy.size(); i++) {
                writer.printf("%d,%.4f%n", i * (int) timeStepSeconds, cumulativeEnergy.get(i));
            }
            System.out.printf("✅ Fichier '%s' généré.%n", energyFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveHostAvgCpuData() {
        String policyName = "base"; // STATIC, DYNAMIC, HYBRID
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




}

