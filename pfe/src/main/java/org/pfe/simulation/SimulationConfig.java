package org.pfe.simulation;

import org.pfe.simulation.Migration.ThresholdStrategy;
import org.pfe.simulation.Migration.ThresholdType;

public class SimulationConfig {

    // Stratégie de seuil : STATIC, DYNAMIC ou HYBRID
    public static ThresholdStrategy THRESHOLD_STRATEGY = ThresholdStrategy.DYNAMIC;

    // Type de seuil : SINGLE ou DOUBLE
    public static ThresholdType THRESHOLD_TYPE = ThresholdType.DOUBLE;

    // Consolider ou non (éteindre les hôtes vides)
    public static boolean CONSOLIDATION_ENABLED = true;

    // Nom automatique basé sur la combinaison
    public static String getPolicyName() {
        if (THRESHOLD_TYPE == ThresholdType.SINGLE) {
            if (THRESHOLD_STRATEGY == ThresholdStrategy.STATIC) return "SSTP";
            if (THRESHOLD_STRATEGY == ThresholdStrategy.DYNAMIC) return "DSTP";
        } else { // DOUBLE
            if (!CONSOLIDATION_ENABLED) return "DTP"; // Sans consolidation

            if (THRESHOLD_STRATEGY == ThresholdStrategy.STATIC) return "SDTP";
            if (THRESHOLD_STRATEGY == ThresholdStrategy.DYNAMIC) return "DDTP";
            if (THRESHOLD_STRATEGY == ThresholdStrategy.HYBRID) return "HDTP";
        }
        return "UNKNOWN";
    }
}
