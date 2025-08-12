package org.pfe.simulation.Cloudlet;

import org.cloudsimplus.cloudlets.CloudletSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CloudletGenerator {

    private static final int NUM_CLOUDLETS = 200;
    private static final long SEED = 12345; // Seed fixe pour reproductibilité

    public static List<CloudletSimple> createCloudlets() {
        List<CloudletSimple> list = new ArrayList<>();
        Random rand = new Random(SEED); // Seed fixée ici

        for (int i = 0; i < NUM_CLOUDLETS; i++) {
            long length = 20000 + rand.nextInt(40000); // 20 000 à 60 000 MI
            int pes = 1 + rand.nextInt(3); // 1 à 3 PEs

            CloudletSimple cloudlet = new CloudletSimple(length, pes);
            cloudlet.setSizes(300 + rand.nextInt(500)); // Taille aléatoire
            list.add(cloudlet);
        }

        return list;
    }
}
