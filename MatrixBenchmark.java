import matrix.Matrix;
import algorithms.*;

public class MatrixBenchmark {
    
    private static final int WARMUP_RUNS = 2;
    private static final int BENCHMARK_RUNS = 5;
    
    public static void main(String[] args) {
        System.out.println("=== Matrix Multiplication Benchmark ===\n");
        
        // Matrix sizes to test
        int[] sizes = {256, 512, 1024};
        int threshold = 64; // Default threshold for Fork/Join
        
        // Run benchmarks for each size
        for (int size : sizes) {
            System.out.println("Testing with " + size + "x" + size + " matrices:");
            System.out.println("----------------------------------------");
            
            // Create multipliers
            MatrixMultiplier sequential = new SequentialMultiplier();
            MatrixMultiplier forkJoinRow = new ForkJoinRowMultiplier(threshold);
            MatrixMultiplier forkJoinBlock = new ForkJoinBlockMultiplier(threshold);
            
            // Generate test matrices
            Matrix A = Matrix.random(size, size);
            Matrix B = Matrix.random(size, size);
            
            // Warmup runs
            System.out.println("Warming up...");
            for (int i = 0; i < WARMUP_RUNS; i++) {
                sequential.multiply(A, B);
                forkJoinRow.multiply(A, B);
                forkJoinBlock.multiply(A, B);
            }
            
            // Benchmark Sequential
            double seqTime = benchmark(sequential, A, B, "Sequential");
            
            // Benchmark Fork/Join Row-based
            double rowTime = benchmark(forkJoinRow, A, B, "Fork/Join Row-based");
            
            // Benchmark Fork/Join Block-based
            double blockTime = benchmark(forkJoinBlock, A, B, "Fork/Join Block-based");
            
            // Calculate speedups
            double rowSpeedup = seqTime / rowTime;
            double blockSpeedup = seqTime / blockTime;
            
            System.out.println("\nResults Summary:");
            System.out.printf("  Sequential:        %.2f ms\n", seqTime);
            System.out.printf("  Fork/Join Row:     %.2f ms (Speedup: %.2fx)\n", rowTime, rowSpeedup);
            System.out.printf("  Fork/Join Block:   %.2f ms (Speedup: %.2fx)\n", blockTime, blockSpeedup);
            System.out.println();
        }
        
        // Test with different thresholds
        System.out.println("\n=== Threshold Sensitivity Analysis ===");
        testThresholdSensitivity(512);
    }
    
    private static double benchmark(MatrixMultiplier multiplier, Matrix A, Matrix B, String name) {
        long totalTime = 0;
        
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            // Generate fresh matrices for each run
            Matrix testA = Matrix.random(A.rows, A.cols);
            Matrix testB = Matrix.random(B.rows, B.cols);
            
            long start = System.nanoTime();
            multiplier.multiply(testA, testB);
            long end = System.nanoTime();
            
            totalTime += (end - start);
        }
        
        double avgTimeMs = (totalTime / BENCHMARK_RUNS) / 1_000_000.0;
        System.out.printf("%s: %.2f ms (avg over %d runs)\n", name, avgTimeMs, BENCHMARK_RUNS);
        
        return avgTimeMs;
    }
    
    private static void testThresholdSensitivity(int matrixSize) {
        System.out.println("\nTesting different thresholds with " + matrixSize + "x" + matrixSize + " matrices:");
        int[] thresholds = {32, 64, 128, 256};
        
        Matrix A = Matrix.random(matrixSize, matrixSize);
        Matrix B = Matrix.random(matrixSize, matrixSize);
        
        MatrixMultiplier sequential = new SequentialMultiplier();
        double seqTime = benchmark(sequential, A, B, "Sequential (baseline)");
        
        System.out.println("\nThreshold | Row-based Time | Block-based Time | Row Speedup | Block Speedup");
        System.out.println("----------|----------------|------------------|-------------|---------------");
        
        for (int threshold : thresholds) {
            MatrixMultiplier row = new ForkJoinRowMultiplier(threshold);
            MatrixMultiplier block = new ForkJoinBlockMultiplier(threshold);
            
            double rowTime = benchmark(row, A, B, "");
            double blockTime = benchmark(block, A, B, "");
            
            double rowSpeedup = seqTime / rowTime;
            double blockSpeedup = seqTime / blockTime;
            
            System.out.printf("%9d | %14.2f | %16.2f | %11.2fx | %13.2fx\n", 
                threshold, rowTime, blockTime, rowSpeedup, blockSpeedup);
        }
    }
}

