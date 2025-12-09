# Parallel Matrix Multiplication Project

A Java implementation of parallel matrix multiplication using the Fork/Join framework, with both console and GUI interfaces.

## Features

- **Sequential Matrix Multiplication**: Baseline implementation
- **Fork/Join Row-based Multiplication**: Parallel implementation using row-range decomposition
- **Fork/Join Block-based Multiplication**: Parallel implementation using block decomposition
- **Comprehensive Benchmarking**: Automated performance testing with speedup calculations
- **JavaFX GUI**: Interactive graphical interface for running benchmarks
- **Input Validation**: Dimension checking and error handling

## Project Structure

```
parallel_processing_project/
├── matrix/
│   └── Matrix.java                    # Matrix data structure
├── algorithms/
│   ├── MatrixMultiplier.java          # Interface for multipliers
│   ├── SequentialMultiplier.java     # Sequential implementation
│   ├── ForkJoinRowMultiplier.java    # Fork/Join row-based
│   └── ForkJoinBlockMultiplier.java  # Fork/Join block-based
├── MatrixBenchmark.java               # Console benchmark runner
├── MatrixGUI.java                     # JavaFX GUI application
├── Main.java                          # Application entry point
├── build.sh                           # Build script
└── run.sh                             # Run script
```

## Requirements

- Java 11 or higher
- JavaFX (for GUI mode) - see installation instructions below

## Installation

### Install JavaFX (for GUI)

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install openjfx libopenjfx-java
```

**Manual Installation:**
1. Download JavaFX SDK from [https://openjfx.io/](https://openjfx.io/)
2. Extract to a directory (e.g., `/opt/javafx-sdk-17`)
3. Set environment variable:
   ```bash
   export JAVAFX_HOME=/opt/javafx-sdk-17
   ```

## Building and Running

### Quick Start

```bash
# Make scripts executable
chmod +x build.sh run.sh

# Build the project
./build.sh

# Run GUI (if JavaFX is installed)
./run.sh

# Or run console benchmark
java MatrixBenchmark
```

### Manual Compilation

**With JavaFX (for GUI):**
```bash
javac -d . --module-path /usr/share/openjfx/lib --add-modules javafx.controls \
    matrix/*.java algorithms/*.java MatrixBenchmark.java MatrixGUI.java Main.java
```

**Without JavaFX (console only):**
```bash
javac -d . matrix/*.java algorithms/*.java MatrixBenchmark.java
```

### Running

**GUI Mode:**
```bash
java --module-path /usr/share/openjfx/lib --add-modules javafx.controls Main
```

**Console Mode:**
```bash
java MatrixBenchmark
```

**Console Mode (alternative):**
```bash
java Main --console
```

## Usage

### GUI Application

The JavaFX GUI provides:
- **Configuration Panel**: Set matrix size, threshold, algorithm selection
- **Preset Buttons**: Quick selection of common matrix sizes (256, 512, 1024)
- **Compare All**: Option to run all algorithms and compare results
- **Single Test**: Run one algorithm with specified parameters
- **Full Benchmark**: Run comprehensive benchmark across multiple sizes
- **Progress Bar**: Visual feedback during long-running operations
- **Results Display**: Formatted output with speedup calculations

### Console Benchmark

The console benchmark automatically:
- Tests matrices of sizes 256x256, 512x512, and 1024x1024
- Runs each algorithm multiple times for accurate averages
- Calculates and displays speedup ratios
- Performs threshold sensitivity analysis

## Example Output

```
=== Matrix Multiplication Benchmark ===

Testing with 512x512 matrices:
----------------------------------------
Sequential: 316.38 ms (avg over 5 runs)
ForkJoin Row-based: 145.84 ms (avg over 5 runs)
ForkJoin Block-based: 120.16 ms (avg over 5 runs)

Results Summary:
  Sequential:        316.38 ms
  ForkJoin Row:      145.84 ms (Speedup: 2.17x)
  ForkJoin Block:    120.16 ms (Speedup: 2.63x)
```

## Design Decisions

1. **Object-Oriented Architecture**: Clear separation between data (Matrix), algorithms (MatrixMultiplier implementations), and benchmarking (MatrixBenchmark, MatrixGUI)

2. **Decomposition Strategies**: 
   - **Row-based**: Divides work by row ranges, good for load balancing
   - **Block-based**: Divides work into 2D blocks, better cache locality

3. **Configurable Threshold**: Allows tuning the granularity of parallel tasks

4. **Input Validation**: All multipliers validate matrix dimensions before computation

## Performance Notes

- Parallel speedup depends on matrix size, threshold, and number of CPU cores
- Block-based decomposition typically performs better for larger matrices
- Optimal threshold varies by system and matrix size

## Troubleshooting

**JavaFX not found:**
- Install JavaFX using the instructions above
- Or use console mode: `java MatrixBenchmark`

**Compilation errors:**
- Ensure Java 11+ is installed: `java -version`
- Check JavaFX installation: `ls /usr/share/openjfx/lib`

**GUI doesn't launch:**
- Verify JavaFX modules are available
- Try console mode as fallback

## License

This project is for educational purposes.

