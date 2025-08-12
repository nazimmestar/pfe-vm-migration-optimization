package org.pfe.simulation.Migration;

import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.List;

public class ThresholdCalculator {

    private static ThresholdStrategy strategy = ThresholdStrategy.DYNAMIC; // default is dynamic

    public static void setStrategy( ThresholdStrategy s) {
        strategy = s;
    }

    private static ThresholdType type = ThresholdType.DOUBLE; // par défaut DOUBLE

    public static void setThresholdType(ThresholdType t) {
        type = t;
    }

    public static double calculateTupper(Host host) {
        return switch (strategy) {
            case STATIC -> 0.9;
            case DYNAMIC -> calculateDynamicTupper(host);
            case HYBRID -> 0.8;
        };
    }

    public static double calculateTlower(Host host) {
        if (type == ThresholdType.SINGLE) {
            return 0.0; // pas de seuil inférieur pour le mode SINGLE
        }

        return switch (strategy) {
            case STATIC -> 0.3;
            case DYNAMIC -> calculateDynamicTlower(host);
            case HYBRID -> (0.3 + calculateDynamicTlower(host)) / 2.0;
        };
    }


    private static double calculateDynamicTupper(Host host) {
        List<Vm> vmList = host.getVmList();

        double sumCpu = vmList.stream().mapToDouble(Vm::getCpuPercentUtilization).sum();
        double totalBw = host.getBw().getCapacity();
        double allocatedBw = vmList.stream().mapToLong(vm -> vm.getBw().getCapacity()).sum();
        double totalRam = host.getRam().getCapacity();
        double allocatedRam = vmList.stream().mapToLong(vm -> vm.getRam().getCapacity()).sum();

        double temp = sumCpu + ((double) allocatedBw / totalBw) + ((double) allocatedRam / totalRam);
        double tupper = 1 - (((0.95 * temp) + sumCpu) - ((0.90 * temp) + sumCpu));

        return Math.min(1.0, Math.max(0.0, tupper));
    }

    private static double calculateDynamicTlower(Host host) {
        double cpuUtil = host.getCpuPercentUtilization();
        List<Vm> vmList = host.getVmList();
        if (vmList.isEmpty()) return 0;

        double sumCpu = vmList.stream().mapToDouble(Vm::getCpuPercentUtilization).sum();
        long totalBw = host.getBw().getCapacity();
        long totalRam = host.getRam().getCapacity();
        long allocatedBw = vmList.stream().mapToLong(vm -> vm.getBw().getCapacity()).sum();
        long allocatedRam = vmList.stream().mapToLong(vm -> vm.getRam().getCapacity()).sum();

        if (cpuUtil < 0.3) {
            double n = vmList.size();
            double sum1 = (sumCpu + allocatedBw + allocatedRam) / (n * (totalBw + totalRam));
            double tlower = Math.sqrt(Math.pow((sumCpu - sum1), 2)) - (0.3 * sum1);
            return Math.min(1.0, Math.max(0.0, tlower));
        }

        return 0.3;
    }


}
