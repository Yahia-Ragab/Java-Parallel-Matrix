package algorithms;

import matrix.Matrix;
import java.util.concurrent.*;

/**
 * Fork/Join Matrix Multiplier using Row-based Decomposition with RecursiveTask.
 * 
 * This implementation uses RecursiveTask<Matrix> where each recursive call
 * returns a Matrix containing the computed rows, rather than modifying
 * a shared matrix in-place. The final result is built by combining
 * the partial matrices returned from recursive subtasks.
 * 
 * Strategy:
 * - Divide the rows of matrix A into two halves
 * - Recursively compute multiplication for each half
 * - Combine the two partial result matrices into the final result
 */
public class ForkJoinRowMultiplier implements MatrixMultiplier {

    private final int threshold;

    public ForkJoinRowMultiplier(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public Matrix multiply(Matrix A, Matrix B) {
        if (A.cols != B.rows) {
            throw new IllegalArgumentException(
                "Matrix dimensions incompatible: A(" + A.rows + "x" + A.cols + 
                ") cannot be multiplied by B(" + B.rows + "x" + B.cols + ")");
        }

        // Create ForkJoinPool and invoke the root task
        ForkJoinPool pool = ForkJoinPool.commonPool();
        RowTask rootTask = new RowTask(A, B, 0, A.rows);
        
        // The root task returns the complete result matrix
        return pool.invoke(rootTask);
    }

    /**
     * RecursiveTask that computes a portion of the matrix multiplication.
     * 
     * This task is responsible for computing rows [start, end) of the result matrix.
     * It returns a Matrix containing only those rows, which will be combined
     * with results from other tasks to form the final result.
     */
    private class RowTask extends RecursiveTask<Matrix> {
        private final Matrix A, B;
        private final int start, end;  // Row range [start, end) to compute

        RowTask(Matrix A, Matrix B, int start, int end) {
            this.A = A; 
            this.B = B;
            this.start = start; 
            this.end = end;
        }

        @Override
        protected Matrix compute() {
            int rowsToCompute = end - start;
            
            // BASE CASE: If the number of rows is small enough, compute directly
            // This avoids the overhead of task creation for small subproblems
            if (rowsToCompute <= threshold) {
                // Create a result matrix for just these rows
                Matrix result = new Matrix(rowsToCompute, B.cols);
                
                // Compute each row in this range
                for (int i = 0; i < rowsToCompute; i++) {
                    int actualRow = start + i;  // Actual row index in matrix A
                    
                    // For each column in the result matrix
                    for (int j = 0; j < B.cols; j++) {
                        double sum = 0;
                        // Standard matrix multiplication: dot product of row and column
                        for (int k = 0; k < A.cols; k++) {
                            sum += A.data[actualRow][k] * B.data[k][j];
                        }
                        // Store result in local matrix (indexed from 0)
                        result.data[i][j] = sum;
                    }
                }
                return result;  // Return the partial result matrix
            }
            
            // RECURSIVE CASE: Split the work in half
            // Divide the row range into two approximately equal parts
                int mid = (start + end) / 2;
            
            // Create two subtasks:
            // - Left task computes rows [start, mid)
            // - Right task computes rows [mid, end)
            RowTask leftTask = new RowTask(A, B, start, mid);
            RowTask rightTask = new RowTask(A, B, mid, end);
            
            // Fork the left task (submit it to the pool for parallel execution)
            // Compute the right task in the current thread
            leftTask.fork();
            Matrix rightResult = rightTask.compute();  // Compute right part
            Matrix leftResult = leftTask.join();      // Wait for left part to complete
            
            // COMBINE PHASE: Merge the two partial results into one matrix
            // The left result contains rows 0 to (mid-start-1)
            // The right result contains rows 0 to (end-mid-1)
            // We need to combine them so left comes first, then right
            return combineMatrices(leftResult, rightResult, start, mid, end);
        }
        
        /**
         * Combines two partial result matrices into a single matrix.
         * 
         * @param left The result matrix for rows [start, mid)
         * @param right The result matrix for rows [mid, end)
         * @param start Original start row index
         * @param mid Middle row index (split point)
         * @param end Original end row index
         * @return Combined matrix containing all rows from start to end
         */
        private Matrix combineMatrices(Matrix left, Matrix right, int start, int mid, int end) {
            int totalRows = end - start;
            int cols = B.cols;
            Matrix combined = new Matrix(totalRows, cols);
            
            // Copy left part: rows 0 to (mid-start-1) from left matrix
            int leftRows = mid - start;
            for (int i = 0; i < leftRows; i++) {
                for (int j = 0; j < cols; j++) {
                    combined.data[i][j] = left.data[i][j];
            }
            }
            
            // Copy right part: rows (mid-start) to (end-start-1) from right matrix
            // The right matrix's rows are placed after the left matrix's rows
            int rightRows = end - mid;
            for (int i = 0; i < rightRows; i++) {
                for (int j = 0; j < cols; j++) {
                    combined.data[leftRows + i][j] = right.data[i][j];
                }
            }
            
            return combined;
        }
    }
}
