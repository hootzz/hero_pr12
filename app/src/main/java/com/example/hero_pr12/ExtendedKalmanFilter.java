package com.example.hero_pr12;

public class ExtendedKalmanFilter {
    private double[][] Q;  // 프로세스 노이즈 공분산 행렬
    private double[][] R;  // 측정 노이즈 공분산 행렬
    private double[] x;    // 상태 벡터 [x, y]
    private double[][] P;  // 상태 공분산 행렬

    public ExtendedKalmanFilter() {
        Q = new double[][] { { 0.00001, 0 }, { 0, 0.00001 } };
        R = new double[][] { { 0.001, 0 }, { 0, 0.001 } };
        x = new double[] { 0, 0 };
        P = new double[][] { { 1, 0 }, { 0, 1 } };
    }

    // 상태 전이 함수
    private double[] f(double[] state, double[] control) {
        return new double[] { state[0] + control[0], state[1] + control[1] };
    }

    // 상태 전이 함수의 자코비안
    private double[][] F() {
        return new double[][] { { 1, 0 }, { 0, 1 } };
    }

    // 측정 함수
    private double[] h(double[] state) {
        return new double[] { state[0], state[1] };
    }

    // 측정 함수의 자코비안
    private double[][] H() {
        return new double[][] { { 1, 0 }, { 0, 1 } };
    }

    public void predict(double[] control) {
        // 상태 예측
        x = f(x, control);

        // 공분산 예측
        double[][] F = F();
        P = matrixAdd(matrixMultiply(F, matrixMultiply(P, transpose(F))), Q);
    }

    public void update(double[] measurement) {
        // 칼만 이득 계산
        double[][] H = H();
        double[][] S = matrixAdd(matrixMultiply(H, matrixMultiply(P, transpose(H))), R);
        double[][] K = matrixMultiply(P, matrixMultiply(transpose(H), inverse(S)));

        // 상태 갱신
        double[] y = vectorSubtract(measurement, h(x));
        x = vectorAdd(x, matrixVectorMultiply(K, y));

        // 공분산 갱신
        double[][] I = identityMatrix(2);
        P = matrixMultiply(matrixSubtract(I, matrixMultiply(K, H)), P);
    }

    public double[] getState() {
        return x;
    }

    // 행렬 및 벡터 연산 함수들
    private double[][] identityMatrix(double value) {
        return new double[][] { { value, 0 }, { 0, value } };
    }

    private double[][] transpose(double[][] matrix) {
        double[][] result = new double[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                result[j][i] = matrix[i][j];
            }
        }
        return result;
    }

    private double[][] matrixAdd(double[][] A, double[][] B) {
        double[][] result = new double[A.length][A[0].length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++) {
                result[i][j] = A[i][j] + B[i][j];
            }
        }
        return result;
    }

    private double[][] matrixSubtract(double[][] A, double[][] B) {
        double[][] result = new double[A.length][A[0].length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++) {
                result[i][j] = A[i][j] - B[i][j];
            }
        }
        return result;
    }

    private double[][] matrixMultiply(double[][] A, double[][] B) {
        double[][] result = new double[A.length][B[0].length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < B[0].length; j++) {
                for (int k = 0; k < B.length; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return result;
    }

    private double[] matrixVectorMultiply(double[][] A, double[] x) {
        double[] result = new double[A.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < x.length; j++) {
                result[i] += A[i][j] * x[j];
            }
        }
        return result;
    }

    private double[][] inverse(double[][] matrix) {
        double determinant = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
        return new double[][] {
                { matrix[1][1] / determinant, -matrix[0][1] / determinant },
                { -matrix[1][0] / determinant, matrix[0][0] / determinant }
        };
    }

    private double[] vectorAdd(double[] A, double[] B) {
        double[] result = new double[A.length];
        for (int i = 0; i < A.length; i++) {
            result[i] = A[i] + B[i];
        }
        return result;
    }

    private double[] vectorSubtract(double[] A, double[] B) {
        double[] result = new double[A.length];
        for (int i = 0; i < A.length; i++) {
            result[i] = A[i] - B[i];
        }
        return result;
    }
}
