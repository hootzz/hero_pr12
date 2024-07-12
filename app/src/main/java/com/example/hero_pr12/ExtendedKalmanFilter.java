package com.example.hero_pr12;

public class ExtendedKalmanFilter {
    private double[][] Q;  // 프로세스 노이즈 공분산 행렬
    private double[][] R;  // 측정 노이즈 공분산 행렬
    private double[] x;    // 상태 벡터 [x, y]
    private double[][] P;  // 상태 공분산 행렬
    private int age;       // 재귀 값 (Age)
    private double Ts = 1.0; // 시간 곱 (Update period)

    public ExtendedKalmanFilter() {
        Q = new double[][] { { 0.00001, 0 }, { 0, 0.00001 } };
        R = new double[][] { { 0.001, 0 }, { 0, 0.001 } };
        x = new double[] { 0, 0 };
        P = new double[][] { { 1, 0 }, { 0, 1 } };
        age = 0;
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

    public void process(double[] control, double[] measurement) {
        if (age == 0) {
            initialize(control, measurement);
        } else {
            predict(control);
            update(measurement);
        }
        age++;
    }

    private void initialize(double[] control, double[] measurement) {
        // 초기화 과정
        x = new double[] { 0, 0 }; // 초기 위치 설정
        P = new double[][] { { 1, 0 }, { 0, 1 } }; // 초기 공분산 행렬 설정

        // 역방향 범위의 합을 구하고 무게 중심 방법을 사용하여 초기 위치를 계산
        double rangeSum = 0;
        for (double value : measurement) {
            rangeSum += 1.0 / value;
        }
        double initialX = 0;
        double initialY = 0;
        for (int i = 0; i < measurement.length; i++) {
            initialX += (1.0 / measurement[i]) / rangeSum * control[0];
            initialY += (1.0 / measurement[i]) / rangeSum * control[1];
        }
        x = new double[] { initialX, initialY };

        // 측정 오차 공분산 행렬 연산을 통해 초기 공분산 행렬 설정
        double[][] H = H();
        double[][] HT = transpose(H);
        double[][] HPHT = matrixMultiply(H, matrixMultiply(P, HT));
        P = matrixAdd(HPHT, R);

        age++; // 재귀횟수 증가
    }

    public void predict(double[] control) {
        // 예측 과정 시작
        // 1. 상태 벡터 예측
        x = f(x, control);

        // 2. 상태 변이 행렬 설정
        double[][] F = F();

        // 3. 공분산 행렬 과정의 잡음 설정
        double[][] FT = transpose(F);
        double[][] FPFT = matrixMultiply(F, matrixMultiply(P, FT));

        // 4. 상태 오차 공분산 예측 값 산출
        P = matrixAdd(FPFT, Q);
    }

    public void update(double[] measurement) {
        // 추정 과정
        // 1. 지역 변수 초기화
        double[][] H = H();

        // 2. 상태 벡터 전송
        double[] hx = h(x);

        // 3. 관측 행렬과 관찰 잔여 값 계산
        double[][] HT = transpose(H);
        double[][] S = matrixAdd(matrixMultiply(H, matrixMultiply(P, HT)), R);
        double[][] K = matrixMultiply(P, matrixMultiply(HT, inverse(S)));

        // 4. 칼만 이득 계산
        double[] y = vectorSubtract(measurement, hx);

        // 5. 추정 값 계산
        x = vectorAdd(x, matrixVectorMultiply(K, y));

        // 6. 오차 공분산 계산
        double[][] I = identityMatrix(2);
        P = matrixMultiply(matrixSubtract(I, matrixMultiply(K, H)), P);

        // 7. 재귀 값 증가
        age++;

        // 8. 지역 변수 초기화 (필요 시)
    }

    public double[] getState() {
        return x;
    }

    // 행렬 및 벡터 연산 함수들
    private double[][] identityMatrix(int size) {
        double[][] result = new double[size][size];
        for (int i = 0; i < size; i++) {
            result[i][i] = 1;
        }
        return result;
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
