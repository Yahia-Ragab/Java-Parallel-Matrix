package matrix;

import java.util.Random;

public class Matrix {
    public final int rows;
    public final int cols;
    public final double[][] data;

    public Matrix(int r, int c) {
        rows = r;
        cols = c;
        data = new double[r][c];
    }

    public static Matrix random(int r, int c) {
        Matrix m = new Matrix(r, c);
        Random rand = new Random();

        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                m.data[i][j] = rand.nextDouble() * 10;

        return m;
    }

    public int getRowCount() {
        return rows;
    }

    public int getColCount() {
        return cols;
    }
}
