package org.pfe.simulation.Vm;

import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VmCreator {

    private static final int VMS = 64;
//    private static final int VM_PES = 2;
//    private static final int VM_MIPS = 2500;




    public static List<Vm> CreateVmList() {
        List<Vm> vmList = new ArrayList<>(64);
        Random rand = new Random();

        for (int i = 0; i < 64; i++) {
            int mips = 1000 + rand.nextInt(2000); // entre 1000 et 3000 MIPS
            int pes = 1 + rand.nextInt(4); // 1 à 4 PEs
            Vm vm = new VmSimple(mips, pes);
            vm.setRam(2048 + rand.nextInt(8192))     // 2GB à 10GB
                    .setBw(1000 + rand.nextInt(5000))      // BW variable
                    .setSize(10000 + rand.nextInt(100000)); // 10GB à 110GB
            vmList.add(vm);
        }

        return vmList;
    }

}
