package algorithms;

import matrix.Matrix;
import java.util.concurrent.*;

/**
 * Fork/Join Matrix Multiplier using Block-based Decomposition with RecursiveTask.
 * 
 * This implementation uses RecursiveTask<Matrix> where each recursive call
 * returns a Matrix block containing the computed result, rather than modifying
 * a shared matrix in-place. The final result is built by combining
 * the partial block matrices returned from recursive subtasks.
 * 
 * Strategy:
 * - Divide the computation into 2D blocks (sub-matrices)
 * - For each block, recursively compute the multiplication
 * - Combine the 4 quadrant blocks into the final result
 * 
 * The algorithm uses two decomposition strategies:
 * 1. Split along k-dimension: For accumulating partial products (C = A*B)
 * 2. Split along row/column: For dividing the result matrix into quadrants
 */
public class ForkJoinBlockMultiplier implements MatrixMultiplier {

    private final int threshold;

    public ForkJoinBlockMultiplier(int threshold) {
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
        // The root task computes the entire result matrix [0, A.rows) x [0, B.cols)
        // using the full k-range [0, A.cols) for the dot product
        ForkJoinPool pool = ForkJoinPool.commonPool();
        BlockTask rootTask = new BlockTask(A, B, 0, A.rows, 0, B.cols, 0, A.cols);
        
        // The root task returns the complete result matrix
        return pool.invoke(rootTask);
    }

    /**
     * RecursiveTask that computes a block of the matrix multiplication.
     * 
     * This task is responsible for computing a rectangular block of the result matrix:
     * - Rows: [rStart, rEnd)
     * - Columns: [cStart, cEnd)
     * - Using k-range: [kStart, kEnd) for the dot product computation
     * 
     * The task returns a Matrix containing only the computed block, which will be
     * combined with results from other tasks to form the final result.
     */
    private class BlockTask extends RecursiveTask<Matrix> {
        private final Matrix A, B;
        private final int rStart, rEnd;  // Row range [rStart, rEnd) in result matrix
        private final int cStart, cEnd;  // Column range [cStart, cEnd) in result matrix
        private final int kStart, kEnd;  // K range [kStart, kEnd) for dot product

        BlockTask(Matrix A, Matrix B,
                  int rStart, int rEnd,
                  int cStart, int cEnd,
                  int kStart, int kEnd) {
            this.A = A; 
            this.B = B;
            this.rStart = rStart; 
            this.rEnd = rEnd;
            this.cStart = cStart; 
            this.cEnd = cEnd;
            this.kStart = kStart;
            this.kEnd = kEnd;
        }

        @Override
        protected Matrix compute() {
            int rows = rEnd - rStart;
            int cols = cEnd - cStart;
            int kSize = kEnd - kStart;

            // BASE CASE: If the block is small enough, compute directly
            // We check both the block size (rows * cols) and the k-dimension size
            // This avoids the overhead of task creation for small subproblems
            if (rows * cols <= threshold || kSize <= threshold) {
                // Create a result matrix for just this block
                Matrix result = new Matrix(rows, cols);
                
                // Compute each cell in this block
                for (int i = 0; i < rows; i++) {
                    int actualRow = rStart + i;  // Actual row index in matrix A
                    for (int j = 0; j < cols; j++) {
                        int actualCol = cStart + j;  // Actual column index in matrix B
                        
                        // Compute dot product: sum of A[actualRow][k] * B[k][actualCol]
                        // Only use the k-range [kStart, kEnd)
                        double sum = 0;
                        for (int k = kStart; k < kEnd; k++) {
                            sum += A.data[actualRow][k] * B.data[k][actualCol];
                        }
                        // Store result in local matrix (indexed from 0)
                        result.data[i][j] = sum;
                    }
                }
                return result;  // Return the partial result block
            }

            // RECURSIVE CASE: Split the computation
            // We have two splitting strategies:
            
            // STRATEGY 1: Split along k-dimension (for accumulating partial products)
            // This is useful when the k-range is large but the block is small
            // We split the dot product computation: C = A*B = A*B_left + A*B_right
            if (kSize > threshold) {
                int kMid = (kStart + kEnd) / 2;
                
                // Create two subtasks that compute the same block but with different k-ranges
                BlockTask leftTask = new BlockTask(A, B, rStart, rEnd, cStart, cEnd, kStart, kMid);
                BlockTask rightTask = new BlockTask(A, B, rStart, rEnd, cStart, cEnd, kMid, kEnd);
                
                // Fork the left task, compute the right task in current thread
                leftTask.fork();
                Matrix rightResult = rightTask.compute();
                Matrix leftResult = leftTask.join();
                
                // COMBINE: Add the two partial results
                // Since both compute the same block with different k-ranges,
                // we add them together to get the complete dot product
                return addMatrices(leftResult, rightResult);
            } 
            // STRATEGY 2: Split along row and column dimensions (block decomposition)
            // This divides the result matrix into 4 quadrants
            else {
                // Split the block into 4 quadrants
            int rMid = (rStart + rEnd) / 2;
            int cMid = (cStart + cEnd) / 2;

                // Create 4 subtasks for the 4 quadrants:
                // Top-left:    [rStart, rMid) x [cStart, cMid)
                // Top-right:   [rStart, rMid) x [cMid, cEnd)
                // Bottom-left: [rMid, rEnd) x [cStart, cMid)
                // Bottom-right: [rMid, rEnd) x [cMid, cEnd)
                BlockTask topLeft = new BlockTask(A, B, rStart, rMid, cStart, cMid, kStart, kEnd);
                BlockTask topRight = new BlockTask(A, B, rStart, rMid, cMid, cEnd, kStart, kEnd);
                BlockTask bottomLeft = new BlockTask(A, B, rMid, rEnd, cStart, cMid, kStart, kEnd);
                BlockTask bottomRight = new BlockTask(A, B, rMid, rEnd, cMid, cEnd, kStart, kEnd);
                
                // Fork all tasks for parallel execution
                topLeft.fork();
                topRight.fork();
                bottomLeft.fork();
                Matrix bottomRightResult = bottomRight.compute();  // Compute in current thread
                
                // Join all forked tasks
                Matrix topLeftResult = topLeft.join();
                Matrix topRightResult = topRight.join();
                Matrix bottomLeftResult = bottomLeft.join();
                
                // COMBINE: Arrange the 4 blocks into a single matrix
                // The blocks are arranged as:
                // [topLeft    topRight   ]
                // [bottomLeft bottomRight]
                return combineBlocks(topLeftResult, topRightResult, bottomLeftResult, bottomRightResult);
            }
        }
        
        /**
         * Adds two matrices of the same size element-wise.
         * 
         * This is used when combining results from k-dimension splitting,
         * where both matrices represent the same block computed with different k-ranges.
         * 
         * @param left First matrix (partial result from k-range [kStart, kMid))
         * @param right Second matrix (partial result from k-range [kMid, kEnd))
         * @return Sum of the two matrices
         */
        private Matrix addMatrices(Matrix left, Matrix right) {
            // Both matrices have the same dimensions
            Matrix result = new Matrix(left.rows, left.cols);
            for (int i = 0; i < left.rows; i++) {
                for (int j = 0; j < left.cols; j++) {
                    // Add corresponding elements
                    result.data[i][j] = left.data[i][j] + right.data[i][j];
                }
            }
            return result;
        }
        
        /**
         * Combines four quadrant blocks into a single matrix.
         * 
         * Arranges the blocks in a 2x2 grid:
         * [topLeft    topRight   ]
         * [bottomLeft bottomRight]
         * 
         * @param topLeft Top-left quadrant block
         * @param topRight Top-right quadrant block
         * @param bottomLeft Bottom-left quadrant block
         * @param bottomRight Bottom-right quadrant block
         * @return Combined matrix containing all four blocks
         */
        private Matrix combineBlocks(Matrix topLeft, Matrix topRight, 
                                     Matrix bottomLeft, Matrix bottomRight) {
            // Calculate total dimensions
            int totalRows = topLeft.rows + bottomLeft.rows;
            int totalCols = topLeft.cols + topRight.cols;
            Matrix result = new Matrix(totalRows, totalCols);
            
            // Copy top-left block to position (0, 0)
            for (int i = 0; i < topLeft.rows; i++) {
                for (int j = 0; j < topLeft.cols; j++) {
                    result.data[i][j] = topLeft.data[i][j];
                }
            }
            
            // Copy top-right block to position (0, topLeft.cols)
            for (int i = 0; i < topRight.rows; i++) {
                for (int j = 0; j < topRight.cols; j++) {
                    result.data[i][topLeft.cols + j] = topRight.data[i][j];
                }
            }
            
            // Copy bottom-left block to position (topLeft.rows, 0)
            for (int i = 0; i < bottomLeft.rows; i++) {
                for (int j = 0; j < bottomLeft.cols; j++) {
                    result.data[topLeft.rows + i][j] = bottomLeft.data[i][j];
                }
            }
            
            // Copy bottom-right block to position (topLeft.rows, topLeft.cols)
            for (int i = 0; i < bottomRight.rows; i++) {
                for (int j = 0; j < bottomRight.cols; j++) {
                    result.data[topLeft.rows + i][topLeft.cols + j] = bottomRight.data[i][j];
                }
            }
            
            return result;
        }
    }
}
