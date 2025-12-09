package algorithms;

import matrix.Matrix;

public class SequentialMultiplier implements MatrixMultiplier {

    @Override
    public Matrix multiply(Matrix A, Matrix B) {
        if (A.cols != B.rows) {
            throw new IllegalArgumentException(
                "Matrix dimensions incompatible: A(" + A.rows + "x" + A.cols + 
                ") cannot be multiplied by B(" + B.rows + "x" + B.cols + ")");
        }
        Matrix C = new Matrix(A.rows, B.cols);

        for (int i = 0; i < A.rows; i++) {
            for (int j = 0; j < B.cols; j++) {
                double sum = 0;
                for (int k = 0; k < A.cols; k++)
                    sum += A.data[i][k] * B.data[k][j];
                C.data[i][j] = sum;
            }
        }
        return C;
    }
}
