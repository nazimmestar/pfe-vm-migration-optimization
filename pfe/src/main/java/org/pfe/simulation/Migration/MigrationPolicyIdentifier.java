package org.pfe.simulation.Migration;

public class MigrationPolicyIdentifier {
    public static String getPolicyCode(ThresholdStrategy strategy, ThresholdType type) {
        return switch (strategy) {
            case STATIC -> (type == ThresholdType.SINGLE) ? "SSTP" : "SDTP";
            case DYNAMIC -> (type == ThresholdType.SINGLE) ? "DSTP" : "DDTP";
            case HYBRID -> {
                if (type == ThresholdType.DOUBLE) yield "HDTP";
                else throw new IllegalArgumentException("HYBRID strategy with SINGLE threshold (HSTP) is not supported.");
            }
        };
    }
}
