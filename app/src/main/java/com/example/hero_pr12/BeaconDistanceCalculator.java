package com.example.hero_pr12;

public abstract class BeaconDistanceCalculator {

    /**
     * Different Path Loss Exponent parameters for different environments.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Log-distance_path_loss_model"></a>
     */
    public static final float PATH_LOSS_PARAMETER_OPEN_SPACE = 2;
    public static final float PATH_LOSS_PARAMETER_INDOOR = 1.7f;
    public static final float PATH_LOSS_PARAMETER_OFFICE_HARD_PARTITION = 3f;

    public static final int CALIBRATED_RSSI_AT_ONE_METER = -62;
    public static final int SIGNAL_LOSS_AT_ONE_METER = -41;

    private static float pathLossParameter = PATH_LOSS_PARAMETER_OFFICE_HARD_PARTITION;

    /**
     * Calculates the distance to the specified beacon using the log-distance path loss model.
     */
    public static float calculateDistance(float rssi, float calibratedRssi, float pathLossParameter) {
        return (float) Math.pow(10, (calibratedRssi - rssi) / (10 * pathLossParameter));
    }

    public static float getCalibratedRssiAtOneMeter(float calibratedRssi, float calibratedDistance) {
        float calibratedRssiAtOneMeter;
        if (calibratedDistance == 1.0) {
            calibratedRssiAtOneMeter = calibratedRssi;
        } else {
            calibratedRssiAtOneMeter = (float) (calibratedRssi + 10 * pathLossParameter * Math.log10(calibratedDistance));
        }
        return calibratedRssiAtOneMeter;
    }

    public static void setPathLossParameter(float pathLossParameter) {
        BeaconDistanceCalculator.pathLossParameter = pathLossParameter;
    }

    public static float getPathLossParameter() {
        return BeaconDistanceCalculator.pathLossParameter;
    }
}
