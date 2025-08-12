package org.pfe.simulation;

import org.cloudsimplus.cloudlets.CloudletSimple;
import org.pfe.simulation.Cloudlet.CloudletGenerator;
import org.pfe.simulation.Migration.ThresholdStrategy;
import org.pfe.simulation.Migration.ThresholdType;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

public class MainSimulationLauncher {

    public static void main(String[] args) {


        Object[][] configurations = {
                // { Strategy, Type, Consolidation }
                {ThresholdStrategy.STATIC, ThresholdType.SINGLE, false},  // SSTP
                {ThresholdStrategy.DYNAMIC, ThresholdType.SINGLE, false}, // DSTP
                {ThresholdStrategy.STATIC, ThresholdType.DOUBLE, true},   // SDTP
                {ThresholdStrategy.DYNAMIC, ThresholdType.DOUBLE, true},  // DDTP
                {ThresholdStrategy.HYBRID, ThresholdType.DOUBLE, true}    // HDTP
        };

        for (Object[] config : configurations) {
            // Appliquer la configuration
            SimulationConfig.THRESHOLD_STRATEGY = (ThresholdStrategy) config[0];
            SimulationConfig.THRESHOLD_TYPE = (ThresholdType) config[1];
            SimulationConfig.CONSOLIDATION_ENABLED = (boolean) config[2];

            String policyName = SimulationConfig.getPolicyName();

            System.out.println("🔧 Lancement de la stratégie : " + policyName);

            try {

                // Rediriger la sortie AVANT de lancer la simulation
                PrintStream logFile = new PrintStream("results/log_" + policyName + ".txt");
                System.setOut(logFile);
                List<CloudletSimple> sharedCloudlets = CloudletGenerator.createCloudlets();

                App.runSimulation(sharedCloudlets );

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
