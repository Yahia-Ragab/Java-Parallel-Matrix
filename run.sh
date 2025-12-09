#!/bin/bash

# Run script for Matrix Multiplication Project with JavaFX

# Check if JavaFX is available
JAVAFX_PATH=""
if [ -d "/usr/share/openjfx/lib" ]; then
    JAVAFX_PATH="/usr/share/openjfx/lib"
elif [ -d "/usr/lib/jvm/java-17-openjfx" ]; then
    JAVAFX_PATH="/usr/lib/jvm/java-17-openjfx/lib"
elif [ -n "$JAVAFX_HOME" ]; then
    JAVAFX_PATH="$JAVAFX_HOME/lib"
fi

# Run
if [ -n "$JAVAFX_PATH" ] && [ -f "Main.class" ]; then
    echo "Launching GUI application..."
    java --module-path "$JAVAFX_PATH" --add-modules javafx.controls Main
elif [ -f "MatrixBenchmark.class" ]; then
    echo "Running console benchmark (GUI not available)..."
    java MatrixBenchmark
else
    echo "Error: Classes not found. Please run ./build.sh first"
    exit 1
fi

