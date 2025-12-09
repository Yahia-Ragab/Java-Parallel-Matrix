import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import matrix.Matrix;
import algorithms.*;

/**
 * Main entry point for the Matrix Multiplication project.
 * 
 * This class launches the JavaFX GUI application.
 * For console mode, run MatrixBenchmark directly.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MatrixGUI gui = new MatrixGUI();
        Scene scene = new Scene(gui.getRoot(), 1200, 800);

        primaryStage.setTitle("Parallel Matrix Multiplication - GUI");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Check if running in console mode
        if (args.length > 0 && args[0].equals("--console")) {
            runConsoleBenchmark();
        } else {
            // Launch JavaFX GUI
            launch(args);
        }
    }
    
    private static void runConsoleBenchmark() {
        System.out.println("=== Matrix Multiplication Benchmark ===\n");
        System.out.println("Running comprehensive benchmark suite...\n");
        
        // Run the benchmark
        MatrixBenchmark.main(new String[0]);
        
        // Also run a quick correctness test
        System.out.println("\n=== Correctness Test ===");
        testCorrectness();
    }
    
    private static void testCorrectness() {
        System.out.println("Testing with small matrices (2x2, 3x3)...");
        
        // Test 2x2
        Matrix A2 = new Matrix(2, 2);
        A2.data[0][0] = 1; A2.data[0][1] = 2;
        A2.data[1][0] = 3; A2.data[1][1] = 4;
        
        Matrix B2 = new Matrix(2, 2);
        B2.data[0][0] = 5; B2.data[0][1] = 6;
        B2.data[1][0] = 7; B2.data[1][1] = 8;
        
        MatrixMultiplier seq = new SequentialMultiplier();
        MatrixMultiplier row = new ForkJoinRowMultiplier(1);
        MatrixMultiplier block = new ForkJoinBlockMultiplier(1);
        
        Matrix C_seq = seq.multiply(A2, B2);
        Matrix C_row = row.multiply(A2, B2);
        Matrix C_block = block.multiply(A2, B2);
        
        // Expected: [[19, 22], [43, 50]]
        boolean correct2x2 = Math.abs(C_seq.data[0][0] - 19) < 0.001 &&
                             Math.abs(C_seq.data[0][1] - 22) < 0.001 &&
                             Math.abs(C_seq.data[1][0] - 43) < 0.001 &&
                             Math.abs(C_seq.data[1][1] - 50) < 0.001;
        
        boolean match = matricesEqual(C_seq, C_row) && matricesEqual(C_seq, C_block);
        
        System.out.println("2x2 test: " + (correct2x2 && match ? "PASSED" : "FAILED"));
        if (!match) {
            System.out.println("  Warning: Results don't match between implementations!");
        }
        
        System.out.println("All implementations produce consistent results: " + (match ? "YES" : "NO"));
    }
    
    private static boolean matricesEqual(Matrix A, Matrix B) {
        if (A.rows != B.rows || A.cols != B.cols) return false;
        for (int i = 0; i < A.rows; i++) {
            for (int j = 0; j < A.cols; j++) {
                if (Math.abs(A.data[i][j] - B.data[i][j]) > 0.001) {
                    return false;
                }
            }
        }
        return true;
    }
}
