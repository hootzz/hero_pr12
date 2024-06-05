package com.example.hero_pr12;

public class KalmanFilter {
    private double Q = 0.00001;
    private double R = 0.001;
    private double A = 1;
    private double B = 0;
    private double C = 1;
    private double cov = Double.NaN;
    private double x = Double.NaN;

    public double filter(double z) {
        if (Double.isNaN(x)) {
            x = 1.0 / C * z;
            cov = 1.0 / C * R * 1.0 / C;
        } else {
            double predX = A * x + B * 0;
            double predCov = A * cov * A + Q;

            double K = predCov * C * 1.0 / (C * predCov * C + R);
            x = predX + K * (z - C * predX);
            cov = predCov - K * C * predCov;
        }
        return x;
    }
}
