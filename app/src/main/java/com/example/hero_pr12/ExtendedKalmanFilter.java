package com.example.hero_pr12;

public class ExtendedKalmanFilter {
    private double[][] F; // State transition matrix
    private double[][] H; // Measurement matrix
    private double[][] Q; // Process noise covariance
    private double[][] R; // Measurement noise covariance
    private double[][] P; // Estimate error covariance
    private double[] x; // State
    private double dt; // Time step

    public ExtendedKalmanFilter() {
        this.dt = dt;
        F = new double[][] {
                {1, 0, dt, 0},
                {0, 1, 0, dt},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
        H = new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0}
        };
        Q = new double[][] {
                {0.1, 0, 0, 0},
                {0, 0.1, 0, 0},
                {0, 0, 0.1, 0},
                {0, 0, 0, 0.1}
        };
        R = new double[][] {
                {1, 0},
                {0, 1}
        };
        P = new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
        x = new double[] {0, 0, 0, 0};
    }

    public void predict(double[] u) {
        // Predict state
        x = matrixVectorMultiply(F, x);
        x[2] += u[0] * dt; // Add control input (acceleration in x)
        x[3] += u[1] * dt; // Add control input (acceleration in y)

        // Predict error covariance
        P = matrixAdd(matrixMultiply(matrixMultiply(F, P), transpose(F)), Q);
    }

    public void update(double[] z) {
        // Calculate Kalman gain
        double[][] S = matrixAdd(matrixMultiply(matrixMultiply(H, P), transpose(H)), R);
        double[][] K = matrixMultiply(matrixMultiply(P, transpose(H)), inverse(S));

        // Update estimate
        double[] y = vectorSubtract(z, matrixVectorMultiply(H, x));
        x = vectorAdd(x, matrixVectorMultiply(K, y));

        // Update error covariance
        double[][] I = new double[][] {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
        P = matrixMultiply(matrixSubtract(I, matrixMultiply(K, H)), P);
    }

    public double[] getState() {
        return x;
    }

    // Helper methods for matrix operations
    private double[][] matrixMultiply(double[][] A, double[][] B) {
        int m = A.length;
        int n = B[0].length;
        int o = B.length;
        double[][] result = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < o; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return result;
    }

    private double[] matrixVectorMultiply(double[][] A, double[] v) {
        int m = A.length;
        int n = v.length;
        double[] result = new double[m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[i] += A[i][j] * v[j];
            }
        }
        return result;
    }

    private double[][] matrixAdd(double[][] A, double[][] B) {
        int m = A.length;
        int n = A[0].length;
        double[][] result = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = A[i][j] + B[i][j];
            }
        }
        return result;
    }

    private double[][] matrixSubtract(double[][] A, double[][] B) {
        int m = A.length;
        int n = A[0].length;
        double[][] result = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = A[i][j] - B[i][j];
            }
        }
        return result;
    }

    private double[][] transpose(double[][] A) {
        int m = A.length;
        int n = A[0].length;
        double[][] result = new double[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[j][i] = A[i][j];
            }
        }
        return result;
    }

    private double[] vectorAdd(double[] a, double[] b) {
        int n = a.length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    private double[] vectorSubtract(double[] a, double[] b) {
        int n = a.length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = a[i] - b[i];
        }
        return result;
    }

    private double[][] inverse(double[][] A) {
        // This is a simple implementation for 2x2 matrices
        // For larger matrices, you should use a more robust method
        double det = A[0][0] * A[1][1] - A[0][1] * A[1][0];
        return new double[][] {
                {A[1][1] / det, -A[0][1] / det},
                {-A[1][0] / det, A[0][0] / det}
        };
    }
}