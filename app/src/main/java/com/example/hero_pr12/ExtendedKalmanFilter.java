package com.example.hero_pr12;

public class ExtendedKalmanFilter {
    private double Q = 0.00001;  // 프로세스 노이즈 공분산
    private double R = 0.001;    // 측정 노이즈 공분산
    private double x = 0;        // 상태 변수
    private double P = 1;        // 상태 공분산

    public double filter(double z) {
        double y = z - x;        // 혁신
        double S = P + R;        // 혁신 공분산
        double K = P / S;        // 칼만 이득
        x = x + K * y;           // 상태 변수 갱신
        P = (1 - K) * P + Q;     // 상태 공분산 갱신
        return x;
    }
}