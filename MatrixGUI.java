import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.concurrent.Task;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import matrix.Matrix;
import algorithms.*;

// Data model for benchmark results table
class BenchmarkResult {
    private final SimpleStringProperty algorithm;
    private final SimpleIntegerProperty matrixSize;
    private final SimpleIntegerProperty threshold;
    private final SimpleIntegerProperty runs;
    private final SimpleDoubleProperty timeMs;
    private final SimpleDoubleProperty speedup;

    public BenchmarkResult(String algorithm, int matrixSize, int threshold, int runs, double timeMs, double speedup) {
        this.algorithm = new SimpleStringProperty(algorithm);
        this.matrixSize = new SimpleIntegerProperty(matrixSize);
        this.threshold = new SimpleIntegerProperty(threshold);
        this.runs = new SimpleIntegerProperty(runs);
        this.timeMs = new SimpleDoubleProperty(timeMs);
        this.speedup = new SimpleDoubleProperty(speedup);
    }

    // Getters for properties
    public String getAlgorithm() { return algorithm.get(); }
    public int getMatrixSize() { return matrixSize.get(); }
    public int getThreshold() { return threshold.get(); }
    public int getRuns() { return runs.get(); }
    public double getTimeMs() { return timeMs.get(); }
    public double getSpeedup() { return speedup.get(); }

    // Property getters for TableView
    public SimpleStringProperty algorithmProperty() { return algorithm; }
    public SimpleIntegerProperty matrixSizeProperty() { return matrixSize; }
    public SimpleIntegerProperty thresholdProperty() { return threshold; }
    public SimpleIntegerProperty runsProperty() { return runs; }
    public SimpleDoubleProperty timeMsProperty() { return timeMs; }
    public SimpleDoubleProperty speedupProperty() { return speedup; }
}

public class MatrixGUI {

    private VBox root;
    private TabPane tabPane;

    // Matrix Input Tab Components
    private TextField matrixARowsField, matrixAColsField;
    private TextField matrixBRowsField, matrixBColsField;
    private GridPane matrixAGrid, matrixBGrid, resultGrid;
    private ScrollPane matrixAScroll, matrixBScroll, resultScroll;
    private ComboBox<String> algorithmComboBox;
    private TextField thresholdField;
    private Button createMatricesBtn, generateRandomBtn, multiplyBtn, clearMatricesBtn;
    private Label matrixALabel, matrixBLabel, resultLabel;
    private TextArea summaryArea;

    // Benchmark Tab Components
    private TextField benchmarkSizeField;
    private TextField benchmarkThresholdField;
    private ComboBox<String> benchmarkAlgorithmBox;
    private CheckBox compareAllCheckBox;
    private Spinner<Integer> runsSpinner;
    private TableView<BenchmarkResult> benchmarkTable;
    private ObservableList<BenchmarkResult> benchmarkData;
    private BarChart<String, Number> performanceChart;
    private BarChart<String, Number> speedupChart;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button runBtn, clearBtn, benchmarkBtn;

    // Current matrices
    private Matrix matrixA, matrixB, resultMatrix;
    private TextField[][] matrixACells, matrixBCells, resultCells;

    public MatrixGUI() {
        buildUI();
    }

    public VBox getRoot() {
        return root;
    }

    private void buildUI() {
        root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Title
        Label title = new Label("Parallel Matrix Multiplication");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setPadding(new Insets(0, 0, 10, 0));

        // Create tab pane
        tabPane = new TabPane();

        // Matrix Input Tab
        Tab matrixTab = new Tab("Matrix Input & Multiplication");
        matrixTab.setClosable(false);
        matrixTab.setContent(createMatrixInputTab());

        // Benchmark Tab
        Tab benchmarkTab = new Tab("Performance Benchmark");
        benchmarkTab.setClosable(false);
        benchmarkTab.setContent(createBenchmarkTab());

        tabPane.getTabs().addAll(matrixTab, benchmarkTab);

        root.getChildren().addAll(title, tabPane);
    }

    private ScrollPane createMatrixInputTab() {
        VBox mainContainer = new VBox(12);
        mainContainer.setPadding(new Insets(15));

        // Configuration Section
        VBox configSection = createMatrixConfigSection();

        // Matrices Display Section (Now includes Summary)
        HBox matricesSection = createMatricesDisplaySection();

        // Control Buttons
        HBox controlButtons = createMatrixControlButtons();

        // Note: Summary section removed from here as it is now inside matricesSection

        mainContainer.getChildren().addAll(
                configSection,
                matricesSection,
                controlButtons
        );

        ScrollPane scroll = new ScrollPane(mainContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPadding(new Insets(5));
        return scroll;
    }

    private VBox createMatrixConfigSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");

        Label sectionTitle = new Label("Matrix Configuration");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Matrix A dimensions
        HBox matrixADims = new HBox(10);
        matrixADims.setAlignment(Pos.CENTER_LEFT);
        Label labelA = new Label("Matrix A Dimensions:");
        labelA.setMinWidth(150);
        matrixARowsField = new TextField("3");
        matrixARowsField.setPrefWidth(60);
        Label xLabel1 = new Label("×");
        matrixAColsField = new TextField("3");
        matrixAColsField.setPrefWidth(60);
        matrixADims.getChildren().addAll(labelA, matrixARowsField, xLabel1, matrixAColsField);

        // Matrix B dimensions
        HBox matrixBDims = new HBox(10);
        matrixBDims.setAlignment(Pos.CENTER_LEFT);
        Label labelB = new Label("Matrix B Dimensions:");
        labelB.setMinWidth(150);
        matrixBRowsField = new TextField("3");
        matrixBRowsField.setPrefWidth(60);
        Label xLabel2 = new Label("×");
        matrixBColsField = new TextField("3");
        matrixBColsField.setPrefWidth(60);
        matrixBDims.getChildren().addAll(labelB, matrixBRowsField, xLabel2, matrixBColsField);

        // Algorithm selection
        HBox algoBox = new HBox(10);
        algoBox.setAlignment(Pos.CENTER_LEFT);
        Label algoLabel = new Label("Algorithm:");
        algoLabel.setMinWidth(150);
        algorithmComboBox = new ComboBox<>();
        algorithmComboBox.getItems().addAll(
                "Sequential",
                "ForkJoin Row-based",
                "ForkJoin Block-based"
        );
        algorithmComboBox.getSelectionModel().select(0);
        algorithmComboBox.setPrefWidth(200);

        // Threshold
        Label thresholdLabel = new Label("Threshold:");
        thresholdLabel.setMinWidth(80);
        thresholdField = new TextField("64");
        thresholdField.setPrefWidth(60);
        HBox thresholdBox = new HBox(10, thresholdLabel, thresholdField);

        // Disable threshold when Sequential is selected
        algorithmComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean disable = "Sequential".equals(newVal);
            thresholdField.setDisable(disable);
        });
        thresholdField.setDisable("Sequential".equals(algorithmComboBox.getValue()));

        algoBox.getChildren().addAll(algoLabel, algorithmComboBox, thresholdBox);

        section.getChildren().addAll(
                sectionTitle,
                matrixADims,
                matrixBDims,
                algoBox
        );

        return section;
    }

    private HBox createMatricesDisplaySection() {
        HBox container = new HBox(15);
        container.setPadding(new Insets(10, 0, 10, 0));

        // Matrix A
        VBox matrixAContainer = new VBox(5);
        matrixALabel = new Label("Matrix A");
        matrixALabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        matrixAGrid = new GridPane();
        matrixAGrid.setHgap(5);
        matrixAGrid.setVgap(5);
        matrixAGrid.setPadding(new Insets(5));
        matrixAScroll = new ScrollPane(matrixAGrid);
        matrixAScroll.setPrefSize(250, 300);
        matrixAScroll.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        matrixAContainer.getChildren().addAll(matrixALabel, matrixAScroll);

        // Matrix B
        VBox matrixBContainer = new VBox(5);
        matrixBLabel = new Label("Matrix B");
        matrixBLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        matrixBGrid = new GridPane();
        matrixBGrid.setHgap(5);
        matrixBGrid.setVgap(5);
        matrixBGrid.setPadding(new Insets(5));
        matrixBScroll = new ScrollPane(matrixBGrid);
        matrixBScroll.setPrefSize(250, 300);
        matrixBScroll.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        matrixBContainer.getChildren().addAll(matrixBLabel, matrixBScroll);

        // Result Matrix
        VBox resultContainer = new VBox(5);
        resultLabel = new Label("Result Matrix (A × B)");
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        resultGrid = new GridPane();
        resultGrid.setHgap(5);
        resultGrid.setVgap(5);
        resultGrid.setPadding(new Insets(5));
        resultScroll = new ScrollPane(resultGrid);
        resultScroll.setPrefSize(250, 300);
        resultScroll.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #4caf50;");
        resultContainer.getChildren().addAll(resultLabel, resultScroll);

        // Summary Section (Added here to be beside Result Matrix)
        VBox summaryContainer = createSummarySection();

        container.getChildren().addAll(matrixAContainer, matrixBContainer, resultContainer, summaryContainer);

        return container;
    }

    private HBox createMatrixControlButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        createMatricesBtn = new Button("Create Matrix Grids");
        createMatricesBtn.setPrefWidth(160);
        createMatricesBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        createMatricesBtn.setOnAction(e -> createMatrixGrids());

        generateRandomBtn = new Button("Generate Random");
        generateRandomBtn.setPrefWidth(160);
        generateRandomBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        generateRandomBtn.setOnAction(e -> generateRandomMatrices());

        multiplyBtn = new Button("Multiply Matrices");
        multiplyBtn.setPrefWidth(160);
        multiplyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        multiplyBtn.setOnAction(e -> multiplyMatrices());

        clearMatricesBtn = new Button("Clear All");
        clearMatricesBtn.setPrefWidth(160);
        clearMatricesBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        clearMatricesBtn.setOnAction(e -> clearAllMatrices());

        buttonBox.getChildren().addAll(createMatricesBtn, generateRandomBtn, multiplyBtn, clearMatricesBtn);

        return buttonBox;
    }

    private VBox createSummarySection() {
        VBox section = new VBox(5);

        Label summaryLabel = new Label("Summary & Performance");
        summaryLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        summaryArea = new TextArea();
        // Updated Dimensions to match the Matrix ScrollPanes (250x300)
        summaryArea.setPrefSize(250, 300);
        summaryArea.setEditable(false);
        summaryArea.setFont(Font.font("Consolas", 12));
        summaryArea.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-control-inner-background: #fff8e1;");

        section.getChildren().addAll(summaryLabel, summaryArea);

        return section;
    }

    private void createMatrixGrids() {
        try {
            int rowsA = Integer.parseInt(matrixARowsField.getText());
            int colsA = Integer.parseInt(matrixAColsField.getText());
            int rowsB = Integer.parseInt(matrixBRowsField.getText());
            int colsB = Integer.parseInt(matrixBColsField.getText());

            if (rowsA <= 0 || colsA <= 0 || rowsB <= 0 || colsB <= 0) {
                showError("Dimensions must be positive integers.");
                return;
            }

            if (colsA != rowsB) {
                showError("Matrix A columns (" + colsA + ") must equal Matrix B rows (" + rowsB + ") for multiplication.");
                return;
            }

            // Create Matrix A grid
            matrixAGrid.getChildren().clear();
            matrixACells = new TextField[rowsA][colsA];
            for (int i = 0; i < rowsA; i++) {
                for (int j = 0; j < colsA; j++) {
                    TextField cell = new TextField("0");
                    cell.setPrefWidth(50);
                    cell.setPrefHeight(30);
                    matrixACells[i][j] = cell;
                    matrixAGrid.add(cell, j, i);
                }
            }
            matrixALabel.setText("Matrix A (" + rowsA + "×" + colsA + ")");

            // Create Matrix B grid
            matrixBGrid.getChildren().clear();
            matrixBCells = new TextField[rowsB][colsB];
            for (int i = 0; i < rowsB; i++) {
                for (int j = 0; j < colsB; j++) {
                    TextField cell = new TextField("0");
                    cell.setPrefWidth(50);
                    cell.setPrefHeight(30);
                    matrixBCells[i][j] = cell;
                    matrixBGrid.add(cell, j, i);
                }
            }
            matrixBLabel.setText("Matrix B (" + rowsB + "×" + colsB + ")");

            // Clear result grid
            resultGrid.getChildren().clear();
            resultLabel.setText("Result Matrix (A × B)");
            summaryArea.clear();

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for matrix dimensions.");
        }
    }

    private void generateRandomMatrices() {
        try {
            int rowsA = Integer.parseInt(matrixARowsField.getText());
            int colsA = Integer.parseInt(matrixAColsField.getText());
            int rowsB = Integer.parseInt(matrixBRowsField.getText());
            int colsB = Integer.parseInt(matrixBColsField.getText());

            if (rowsA <= 0 || colsA <= 0 || rowsB <= 0 || colsB <= 0) {
                showError("Dimensions must be positive integers.");
                return;
            }

            if (colsA != rowsB) {
                showError("Matrix A columns (" + colsA + ") must equal Matrix B rows (" + rowsB + ") for multiplication.");
                return;
            }

            // Create grids first if not created
            if (matrixACells == null || matrixACells.length != rowsA || matrixACells[0].length != colsA) {
                createMatrixGrids();
            }

            // Generate random values for Matrix A
            for (int i = 0; i < rowsA; i++) {
                for (int j = 0; j < colsA; j++) {
                    double value = Math.round((Math.random() * 10) * 100.0) / 100.0;
                    matrixACells[i][j].setText(String.valueOf(value));
                }
            }

            // Generate random values for Matrix B
            for (int i = 0; i < rowsB; i++) {
                for (int j = 0; j < colsB; j++) {
                    double value = Math.round((Math.random() * 10) * 100.0) / 100.0;
                    matrixBCells[i][j].setText(String.valueOf(value));
                }
            }

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for matrix dimensions.");
        }
    }

    private void multiplyMatrices() {
        try {
            if (matrixACells == null || matrixBCells == null) {
                showError("Please create matrix grids first.");
                return;
            }

            int rowsA = matrixACells.length;
            int colsA = matrixACells[0].length;
            int rowsB = matrixBCells.length;
            int colsB = matrixBCells[0].length;

            // Read Matrix A
            matrixA = new Matrix(rowsA, colsA);
            for (int i = 0; i < rowsA; i++) {
                for (int j = 0; j < colsA; j++) {
                    try {
                        matrixA.data[i][j] = Double.parseDouble(matrixACells[i][j].getText());
                    } catch (NumberFormatException e) {
                        showError("Invalid number in Matrix A at position (" + (i+1) + "," + (j+1) + ")");
                        return;
                    }
                }
            }

            // Read Matrix B
            matrixB = new Matrix(rowsB, colsB);
            for (int i = 0; i < rowsB; i++) {
                for (int j = 0; j < colsB; j++) {
                    try {
                        matrixB.data[i][j] = Double.parseDouble(matrixBCells[i][j].getText());
                    } catch (NumberFormatException e) {
                        showError("Invalid number in Matrix B at position (" + (i+1) + "," + (j+1) + ")");
                        return;
                    }
                }
            }

            // Get algorithm
            String algorithm = algorithmComboBox.getValue();
            int threshold = Integer.parseInt(thresholdField.getText());

            MatrixMultiplier multiplier;
            switch (algorithm) {
                case "ForkJoin Row-based" -> multiplier = new ForkJoinRowMultiplier(threshold);
                case "ForkJoin Block-based" -> multiplier = new ForkJoinBlockMultiplier(threshold);
                default -> multiplier = new SequentialMultiplier();
            }

            // Perform multiplication
            long startTime = System.nanoTime();
            resultMatrix = multiplier.multiply(matrixA, matrixB);
            long endTime = System.nanoTime();
            double executionTime = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds

            // Display result matrix
            displayResultMatrix();

            // Update summary
            int resultRows = resultMatrix.rows;
            int resultCols = resultMatrix.cols;
            summaryArea.clear();
            summaryArea.appendText("=== Multiplication Summary ===\n");
            summaryArea.appendText("Algorithm: " + algorithm + "\n");
            summaryArea.appendText("Matrix A: " + rowsA + "×" + colsA + "\n");
            summaryArea.appendText("Matrix B: " + rowsB + "×" + colsB + "\n");
            summaryArea.appendText("Result: " + resultRows + "×" + resultCols + "\n");
            summaryArea.appendText(String.format("Execution Time: %.4f ms\n", executionTime));
            if (threshold > 0 && algorithm!="Sequential") {
                summaryArea.appendText("Threshold: " + threshold + "\n");
            }

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers. Error: " + e.getMessage());
        } catch (Exception e) {
            showError("Error during multiplication: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayResultMatrix() {
        if (resultMatrix == null) return;

        resultGrid.getChildren().clear();
        resultCells = new TextField[resultMatrix.rows][resultMatrix.cols];

        for (int i = 0; i < resultMatrix.rows; i++) {
            for (int j = 0; j < resultMatrix.cols; j++) {
                TextField cell = new TextField(String.format("%.2f", resultMatrix.data[i][j]));
                cell.setPrefWidth(60);
                cell.setPrefHeight(30);
                cell.setEditable(false);
                cell.setStyle("-fx-background-color: #c8e6c9;");
                resultCells[i][j] = cell;
                resultGrid.add(cell, j, i);
            }
        }

        resultLabel.setText("Result Matrix (" + resultMatrix.rows + "×" + resultMatrix.cols + ")");
    }

    private void clearAllMatrices() {
        matrixAGrid.getChildren().clear();
        matrixBGrid.getChildren().clear();
        resultGrid.getChildren().clear();
        matrixACells = null;
        matrixBCells = null;
        resultCells = null;
        matrixA = null;
        matrixB = null;
        resultMatrix = null;
        matrixALabel.setText("Matrix A");
        matrixBLabel.setText("Matrix B");
        resultLabel.setText("Result Matrix (A × B)");
        summaryArea.clear();
    }

    // Benchmark Tab (keeping existing functionality)
    private ScrollPane createBenchmarkTab() {
        VBox mainContainer = new VBox(12);
        mainContainer.setPadding(new Insets(15));

        // Input section
        VBox inputSection = createBenchmarkInputSection();

        // Control buttons
        HBox buttonBox = createBenchmarkButtonBox();

        // Progress section
        VBox progressSection = createProgressSection();

        // Output section
        VBox outputSection = createBenchmarkOutputSection();

        mainContainer.getChildren().addAll(
                inputSection,
                buttonBox,
                progressSection,
                outputSection
        );

        ScrollPane scroll = new ScrollPane(mainContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPadding(new Insets(5));
        return scroll;
    }

    private VBox createBenchmarkInputSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label sectionTitle = new Label("Benchmark Configuration");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Matrix size
        HBox sizeBox = new HBox(10);
        sizeBox.setAlignment(Pos.CENTER_LEFT);
        Label sizeLabel = new Label("Matrix Size (n × n):");
        sizeLabel.setMinWidth(150);
        benchmarkSizeField = new TextField("512");
        benchmarkSizeField.setPrefWidth(100);
        sizeBox.getChildren().addAll(sizeLabel, benchmarkSizeField);

        // Preset size buttons
        HBox presetBox = new HBox(5);
        Button preset256 = new Button("256");
        preset256.setOnAction(e -> benchmarkSizeField.setText("256"));
        Button preset512 = new Button("512");
        preset512.setOnAction(e -> benchmarkSizeField.setText("512"));
        Button preset1024 = new Button("1024");
        preset1024.setOnAction(e -> benchmarkSizeField.setText("1024"));
        presetBox.getChildren().addAll(new Label("Presets:"), preset256, preset512, preset1024);

        // Threshold
        HBox thresholdBox = new HBox(10);
        thresholdBox.setAlignment(Pos.CENTER_LEFT);
        Label thresholdLabel = new Label("Threshold:");
        thresholdLabel.setMinWidth(150);
        benchmarkThresholdField = new TextField("64");
        benchmarkThresholdField.setPrefWidth(100);
        thresholdBox.getChildren().addAll(thresholdLabel, benchmarkThresholdField);

        // Algorithm selection
        HBox algorithmBox = new HBox(10);
        algorithmBox.setAlignment(Pos.CENTER_LEFT);
        Label algoLabel = new Label("Algorithm:");
        algoLabel.setMinWidth(150);
        benchmarkAlgorithmBox = new ComboBox<>();
        benchmarkAlgorithmBox.getItems().addAll(
                "Sequential",
                "ForkJoin Row-based",
                "ForkJoin Block-based"
        );
        benchmarkAlgorithmBox.getSelectionModel().select(0);
        benchmarkAlgorithmBox.setPrefWidth(200);
        algorithmBox.getChildren().addAll(algoLabel, benchmarkAlgorithmBox);

        // Compare all checkbox
        compareAllCheckBox = new CheckBox("Compare All Algorithms");
        compareAllCheckBox.setSelected(false);
        compareAllCheckBox.setOnAction(e -> {
            if (compareAllCheckBox.isSelected()) {
                benchmarkAlgorithmBox.setDisable(true);
            } else {
                benchmarkAlgorithmBox.setDisable(false);
            }
        });

        // Number of runs
        HBox runsBox = new HBox(10);
        runsBox.setAlignment(Pos.CENTER_LEFT);
        Label runsLabel = new Label("Number of Runs:");
        runsLabel.setMinWidth(150);
        runsSpinner = new Spinner<>(1, 20, 5);
        runsSpinner.setPrefWidth(80);
        runsBox.getChildren().addAll(runsLabel, runsSpinner);

        section.getChildren().addAll(
                sectionTitle,
                sizeBox,
                presetBox,
                thresholdBox,
                algorithmBox,
                compareAllCheckBox,
                runsBox
        );

        return section;
    }

    private HBox createBenchmarkButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        runBtn = new Button("Run Single Test");
        runBtn.setPrefWidth(150);
        runBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        runBtn.setOnAction(e -> runSingleBenchmark());

        benchmarkBtn = new Button("Run Full Benchmark");
        benchmarkBtn.setPrefWidth(150);
        benchmarkBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        benchmarkBtn.setOnAction(e -> runFullBenchmark());

        clearBtn = new Button("Clear Table");
        clearBtn.setPrefWidth(150);
        clearBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        clearBtn.setOnAction(e -> benchmarkData.clear());

        buttonBox.getChildren().addAll(runBtn, benchmarkBtn, clearBtn);

        return buttonBox;
    }

    private VBox createProgressSection() {
        VBox section = new VBox(5);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-style: italic;");

        section.getChildren().addAll(progressBar, statusLabel);

        return section;
    }

    private VBox createBenchmarkOutputSection() {
        VBox section = new VBox(5);

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label outputLabel = new Label("Benchmark Results:");
        outputLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Button exportBtn = new Button("Export to CSV");
        exportBtn.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white;");
        exportBtn.setOnAction(e -> exportToCSV());

        headerBox.getChildren().addAll(outputLabel, exportBtn);

        // Initialize table data - use simple observable list
        benchmarkData = FXCollections.observableArrayList();

        // Create table
        benchmarkTable = new TableView<>();
        benchmarkTable.setItems(benchmarkData);
        benchmarkTable.setPrefHeight(400);
        benchmarkTable.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");

        // Enable table updates
        benchmarkTable.setEditable(false);
        benchmarkTable.setFocusTraversable(false);

        // Ensure table is visible and updates properly
        benchmarkTable.setVisible(true);

        // Algorithm column
        TableColumn<BenchmarkResult, String> algorithmCol = new TableColumn<>("Algorithm");
        algorithmCol.setCellValueFactory(cellData -> cellData.getValue().algorithmProperty());
        algorithmCol.setPrefWidth(180);
        algorithmCol.setStyle("-fx-alignment: CENTER-LEFT;");
        algorithmCol.setCellFactory(column -> new TableCell<BenchmarkResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Color code algorithms
                    if (item.contains("Sequential")) {
                        setStyle("-fx-background-color: #ffebee; -fx-font-weight: bold;");
                    } else if (item.contains("Row")) {
                        setStyle("-fx-background-color: #e3f2fd; -fx-font-weight: bold;");
                    } else if (item.contains("Block")) {
                        setStyle("-fx-background-color: #e8f5e9; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Matrix Size column
        TableColumn<BenchmarkResult, Integer> sizeCol = new TableColumn<>("Matrix Size");
        sizeCol.setCellValueFactory(cellData -> cellData.getValue().matrixSizeProperty().asObject());
        sizeCol.setPrefWidth(120);
        sizeCol.setStyle("-fx-alignment: CENTER;");
        sizeCol.setCellFactory(column -> new TableCell<BenchmarkResult, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item + "×" + item);
                }
            }
        });

        // Threshold column
        TableColumn<BenchmarkResult, Integer> thresholdCol = new TableColumn<>("Threshold");
        thresholdCol.setCellValueFactory(cellData -> cellData.getValue().thresholdProperty().asObject());
        thresholdCol.setPrefWidth(100);
        thresholdCol.setStyle("-fx-alignment: CENTER;");
        thresholdCol.setCellFactory(column -> new TableCell<BenchmarkResult, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    BenchmarkResult result = getTableRow() != null && getTableRow().getItem() != null ?
                            getTableRow().getItem() : null;
                    if (result != null && result.getAlgorithm().startsWith("---")) {
                        setText("");
                    } else {
                        setText(String.valueOf(item));
                    }
                }
            }
        });

        // Runs column
        TableColumn<BenchmarkResult, Integer> runsCol = new TableColumn<>("Runs");
        runsCol.setCellValueFactory(cellData -> cellData.getValue().runsProperty().asObject());
        runsCol.setPrefWidth(70);
        runsCol.setStyle("-fx-alignment: CENTER;");
        runsCol.setCellFactory(column -> new TableCell<BenchmarkResult, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    BenchmarkResult result = getTableRow() != null && getTableRow().getItem() != null ?
                            getTableRow().getItem() : null;
                    if (result != null && result.getAlgorithm().startsWith("---")) {
                        setText("");
                    } else {
                        setText(String.valueOf(item));
                    }
                }
            }
        });

        // Time column
        TableColumn<BenchmarkResult, Double> timeCol = new TableColumn<>("Time (ms)");
        timeCol.setCellValueFactory(cellData -> cellData.getValue().timeMsProperty().asObject());
        timeCol.setPrefWidth(130);
        timeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        timeCol.setCellFactory(column -> new TableCell<BenchmarkResult, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    BenchmarkResult result = getTableRow() != null && getTableRow().getItem() != null ?
                            getTableRow().getItem() : null;
                    if (result != null && result.getAlgorithm().startsWith("---")) {
                        setText("");
                        setStyle("");
                    } else {
                        setText(String.format("%.2f", item));
                        // Color code based on performance
                        if (item < 100) {
                            setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                        } else if (item < 500) {
                            setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        });

        // Speedup column
        TableColumn<BenchmarkResult, Double> speedupCol = new TableColumn<>("Speedup");
        speedupCol.setCellValueFactory(cellData -> cellData.getValue().speedupProperty().asObject());
        speedupCol.setPrefWidth(120);
        speedupCol.setStyle("-fx-alignment: CENTER;");
        speedupCol.setCellFactory(column -> new TableCell<BenchmarkResult, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    BenchmarkResult result = getTableRow() != null && getTableRow().getItem() != null ?
                            getTableRow().getItem() : null;
                    if (result != null && result.getAlgorithm().startsWith("---")) {
                        setText("");
                    } else {
                        setText("-");
                    }
                    setStyle("");
                } else {
                    setText(String.format("%.2fx", item));
                    // Color code speedup
                    if (item >= 2.0) {
                        setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else if (item >= 1.5) {
                        setStyle("-fx-text-fill: #558b2f; -fx-font-weight: bold;");
                    } else if (item >= 1.0) {
                        setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    }
                }
            }
        });

        benchmarkTable.getColumns().addAll(algorithmCol, sizeCol, thresholdCol, runsCol, timeCol, speedupCol);

        // Enable column reordering and resizing
        benchmarkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Add alternating row colors and special styling for separators
        benchmarkTable.setRowFactory(tv -> new TableRow<BenchmarkResult>() {
            @Override
            protected void updateItem(BenchmarkResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    // Check if it's a separator row
                    if (item.getAlgorithm().startsWith("---")) {
                        setStyle("-fx-background-color: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        int index = getIndex();
                        if (index % 2 == 0) {
                            setStyle("-fx-background-color: #fafafa;");
                        } else {
                            setStyle("-fx-background-color: white;");
                        }
                    }
                }
            }
        });

        // Charts for visual comparison
        CategoryAxis perfXAxis = new CategoryAxis();
        NumberAxis perfYAxis = new NumberAxis();
        perfXAxis.setLabel("Algorithm");
        perfYAxis.setLabel("Time (ms)");
        performanceChart = new BarChart<>(perfXAxis, perfYAxis);
        performanceChart.setTitle("Latest Performance");
        performanceChart.setPrefHeight(240);
        performanceChart.setCategoryGap(20);

        CategoryAxis speedXAxis = new CategoryAxis();
        NumberAxis speedYAxis = new NumberAxis();
        speedXAxis.setLabel("Algorithm");
        speedYAxis.setLabel("Speedup (x)");
        speedupChart = new BarChart<>(speedXAxis, speedYAxis);
        speedupChart.setTitle("Latest Speedup");
        speedupChart.setPrefHeight(240);
        speedupChart.setCategoryGap(20);

        HBox chartsBox = new HBox(15, performanceChart, speedupChart);
        chartsBox.setPadding(new Insets(10, 0, 0, 0));

        section.getChildren().addAll(headerBox, benchmarkTable, chartsBox);

        return section;
    }

    /**
     * Update performance and speedup charts using the latest algorithm results.
     * We take the last three non-separator entries (if available) and plot them.
     */
    private void updateChartsWithLatestResults() {
        if (performanceChart == null || speedupChart == null) return;

        XYChart.Series<String, Number> perfSeries = new XYChart.Series<>();
        perfSeries.setName("Time (ms)");
        XYChart.Series<String, Number> speedSeries = new XYChart.Series<>();
        speedSeries.setName("Speedup");

        int added = 0;
        for (int i = benchmarkData.size() - 1; i >= 0 && added < 3; i--) {
            BenchmarkResult br = benchmarkData.get(i);
            if (br.getAlgorithm().startsWith("---")) continue; // skip separators
            perfSeries.getData().add(new XYChart.Data<>(br.getAlgorithm(), br.getTimeMs()));
            double speed = br.getSpeedup() > 0 ? br.getSpeedup() : 1.0; // baseline 1x if not provided
            speedSeries.getData().add(new XYChart.Data<>(br.getAlgorithm(), speed));
            added++;
        }

        performanceChart.getData().setAll(perfSeries);
        speedupChart.getData().setAll(speedSeries);
    }

    private void exportToCSV() {
        // Simple CSV export functionality
        StringBuilder csv = new StringBuilder();
        csv.append("Algorithm,Matrix Size,Threshold,Runs,Time (ms),Speedup\n");

        for (BenchmarkResult result : benchmarkData) {
            csv.append(String.format("%s,%dx%d,%d,%d,%.2f,%.2f\n",
                    result.getAlgorithm(),
                    result.getMatrixSize(),
                    result.getMatrixSize(),
                    result.getThreshold(),
                    result.getRuns(),
                    result.getTimeMs(),
                    result.getSpeedup() > 0 ? result.getSpeedup() : 0
            ));
        }

        // For now, just show in alert. Could be enhanced to save to file
        TextArea csvArea = new TextArea(csv.toString());
        csvArea.setEditable(false);
        ScrollPane scrollPane = new ScrollPane(csvArea);
        scrollPane.setPrefSize(600, 400);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("CSV Export");
        alert.setHeaderText("Benchmark Results (CSV Format)");
        alert.getDialogPane().setContent(scrollPane);
        alert.showAndWait();
    }

    private void runSingleBenchmark() {
        try {
            int n = Integer.parseInt(benchmarkSizeField.getText());
            int threshold = Integer.parseInt(benchmarkThresholdField.getText());
            int runs = runsSpinner.getValue();

            if (n <= 0 || threshold <= 0) {
                showError("Matrix size and threshold must be positive integers.");
                return;
            }

            if (compareAllCheckBox.isSelected()) {
                runComparisonBenchmark(n, threshold, runs);
            } else {
                String algo = benchmarkAlgorithmBox.getValue();
                runSingleAlgorithm(n, threshold, algo, runs);
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for matrix size and threshold.");
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runSingleAlgorithm(int n, int threshold, String algo, int runs) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    statusLabel.setText("Running " + algo + " with " + n + "x" + n + " matrices...");
                    progressBar.setVisible(true);
                    progressBar.setProgress(0);
                    runBtn.setDisable(true);
                    benchmarkBtn.setDisable(true);
                });

                MatrixMultiplier multiplier;
                switch (algo) {
                    case "ForkJoin Row-based" -> multiplier = new ForkJoinRowMultiplier(threshold);
                    case "ForkJoin Block-based" -> multiplier = new ForkJoinBlockMultiplier(threshold);
                    default -> multiplier = new SequentialMultiplier();
                }

                long totalTime = 0;
                for (int i = 0; i < runs; i++) {
                    Matrix A = Matrix.random(n, n);
                    Matrix B = Matrix.random(n, n);

                    long start = System.nanoTime();
                    multiplier.multiply(A, B);
                    long end = System.nanoTime();

                    totalTime += (end - start);

                    final int current = i + 1;
                    Platform.runLater(() -> {
                        progressBar.setProgress((double) current / runs);
                    });
                }

                double avgTimeMs = (totalTime / runs) / 1_000_000.0;

                Platform.runLater(() -> {
                    BenchmarkResult result = new BenchmarkResult(algo, n, threshold, runs, avgTimeMs, 0.0);
                    benchmarkData.add(result);
                    // Scroll to the newly added row
                    benchmarkTable.scrollTo(result);
                    benchmarkTable.getSelectionModel().select(result);
                    statusLabel.setText("Completed");
                    progressBar.setVisible(false);
                    runBtn.setDisable(false);
                    benchmarkBtn.setDisable(false);

                // Update charts with latest results
                updateChartsWithLatestResults();
                });

                return null;
            }
        };

        new Thread(task).start();
    }

    private void runComparisonBenchmark(int n, int threshold, int runs) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    statusLabel.setText("Running comparison benchmark...");
                    progressBar.setVisible(true);
                    progressBar.setProgress(0);
                    runBtn.setDisable(true);
                    benchmarkBtn.setDisable(true);
                });

                MatrixMultiplier sequential = new SequentialMultiplier();
                MatrixMultiplier forkJoinRow = new ForkJoinRowMultiplier(threshold);
                MatrixMultiplier forkJoinBlock = new ForkJoinBlockMultiplier(threshold);

                double[] times = new double[3];
                String[] names = {"Sequential", "ForkJoin Row-based", "ForkJoin Block-based"};
                MatrixMultiplier[] multipliers = {sequential, forkJoinRow, forkJoinBlock};

                for (int alg = 0; alg < 3; alg++) {
                    long totalTime = 0;
                    for (int i = 0; i < runs; i++) {
                        Matrix A = Matrix.random(n, n);
                        Matrix B = Matrix.random(n, n);

                        long start = System.nanoTime();
                        multipliers[alg].multiply(A, B);
                        long end = System.nanoTime();

                        totalTime += (end - start);

                        final int progress = (alg * runs + i + 1);
                        Platform.runLater(() -> {
                            progressBar.setProgress((double) progress / (3 * runs));
                        });
                    }
                    times[alg] = (totalTime / runs) / 1_000_000.0;
                }

                double seqTime = times[0];
                double rowSpeedup = seqTime / times[1];
                double blockSpeedup = seqTime / times[2];

                Platform.runLater(() -> {
                    // Add separator row
                    BenchmarkResult sep1 = new BenchmarkResult("--- Comparison (" + n + "×" + n + ") ---", n, threshold, runs, 0, 0);
                    benchmarkData.add(sep1);

                    // Add results
                    BenchmarkResult seqResult = new BenchmarkResult("Sequential", n, threshold, runs, times[0], 0.0);
                    BenchmarkResult rowResult = new BenchmarkResult("ForkJoin Row-based", n, threshold, runs, times[1], rowSpeedup);
                    BenchmarkResult blockResult = new BenchmarkResult("ForkJoin Block-based", n, threshold, runs, times[2], blockSpeedup);

                    benchmarkData.add(seqResult);
                    benchmarkData.add(rowResult);
                    benchmarkData.add(blockResult);

                    // Scroll to the last added result
                    benchmarkTable.scrollTo(blockResult);

                    statusLabel.setText("Comparison completed");
                    progressBar.setVisible(false);
                    runBtn.setDisable(false);
                    benchmarkBtn.setDisable(false);

                    // Update charts
                    updateChartsWithLatestResults();
                });

                return null;
            }
        };

        new Thread(task).start();
    }

    private void runFullBenchmark() {
        // Get values on JavaFX thread before starting background work
        final int threshold;
        final int runs;
        try {
            threshold = Integer.parseInt(benchmarkThresholdField.getText());
            runs = runsSpinner.getValue();
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for threshold.");
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    statusLabel.setText("Running full benchmark suite...");
                    progressBar.setVisible(true);
                    progressBar.setProgress(0);
                    runBtn.setDisable(true);
                    benchmarkBtn.setDisable(true);
                    benchmarkData.clear();
                });

                // Small delay to ensure UI updates
                Thread.sleep(100);

                int[] sizes = {256, 512, 1024};

                MatrixMultiplier sequential = new SequentialMultiplier();
                MatrixMultiplier forkJoinRow = new ForkJoinRowMultiplier(threshold);
                MatrixMultiplier forkJoinBlock = new ForkJoinBlockMultiplier(threshold);

                int totalTests = sizes.length * 3 * runs;
                int currentTest = 0;

                for (int size : sizes) {
                    double[] times = new double[3];
                    MatrixMultiplier[] multipliers = {sequential, forkJoinRow, forkJoinBlock};

                    for (int alg = 0; alg < 3; alg++) {
                        long totalTime = 0;
                        for (int i = 0; i < runs; i++) {
                            Matrix A = Matrix.random(size, size);
                            Matrix B = Matrix.random(size, size);

                            long start = System.nanoTime();
                            multipliers[alg].multiply(A, B);
                            long end = System.nanoTime();

                            totalTime += (end - start);
                            currentTest++;

                            final int progress = currentTest;
                            Platform.runLater(() -> {
                                progressBar.setProgress((double) progress / totalTests);
                            });
                        }
                        times[alg] = (totalTime / runs) / 1_000_000.0;
                    }

                    double seqTime = times[0];
                    double rowSpeedup = seqTime / times[1];
                    double blockSpeedup = seqTime / times[2];

                    // Create final variables for Platform.runLater
                    final int finalSize = size;
                    final double seqTimeFinal = times[0];
                    final double rowTimeFinal = times[1];
                    final double blockTimeFinal = times[2];
                    final double rowSpeedupFinal = rowSpeedup;
                    final double blockSpeedupFinal = blockSpeedup;
                    final int finalThreshold = threshold;
                    final int finalRuns = runs;

                    // Add results to table on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            // Add separator for this size
                            BenchmarkResult sep = new BenchmarkResult("--- Size: " + finalSize + "×" + finalSize + " ---",
                                    finalSize, finalThreshold, finalRuns, 0, 0);
                            benchmarkData.add(sep);

                            // Add results
                            BenchmarkResult seqResult = new BenchmarkResult("Sequential", finalSize, finalThreshold, finalRuns, seqTimeFinal, 0.0);
                            BenchmarkResult rowResult = new BenchmarkResult("ForkJoin Row-based", finalSize, finalThreshold, finalRuns, rowTimeFinal, rowSpeedupFinal);
                            BenchmarkResult blockResult = new BenchmarkResult("ForkJoin Block-based", finalSize, finalThreshold, finalRuns, blockTimeFinal, blockSpeedupFinal);

                            benchmarkData.add(seqResult);
                            benchmarkData.add(rowResult);
                            benchmarkData.add(blockResult);

                            // Ensure table is visible and update
                            benchmarkTable.setVisible(true);

                            // Scroll to the last added result
                            benchmarkTable.scrollTo(blockResult);

                            // Force table to refresh and show data
                            benchmarkTable.refresh();
                            benchmarkTable.requestLayout();

                            System.out.println("Added results for size " + finalSize + ", table now has " + benchmarkData.size() + " items");

                            // Update charts
                            updateChartsWithLatestResults();
                        } catch (Exception e) {
                            System.err.println("Error adding results to table: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });

                    // Small delay to ensure UI updates
                    Thread.sleep(50);
                }

                Platform.runLater(() -> {
                    statusLabel.setText("Full benchmark completed - " + benchmarkData.size() + " results");
                    progressBar.setVisible(false);
                    runBtn.setDisable(false);
                    benchmarkBtn.setDisable(false);

                    // Final refresh and scroll to top
                    if (!benchmarkData.isEmpty()) {
                        benchmarkTable.scrollTo(0);
                        benchmarkTable.refresh();
                    }

                    // Final chart refresh
                    updateChartsWithLatestResults();
                });

                return null;
            }
        };

        new Thread(task).start();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}