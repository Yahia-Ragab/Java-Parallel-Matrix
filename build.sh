#!/bin/bash

# Build script for Matrix Multiplication Project with JavaFX

echo "Building Matrix Multiplication Project..."

# Check if JavaFX is available
JAVAFX_PATH=""
if [ -d "/usr/share/openjfx/lib" ]; then
    JAVAFX_PATH="/usr/share/openjfx/lib"
elif [ -d "/usr/lib/jvm/java-17-openjfx" ]; then
    JAVAFX_PATH="/usr/lib/jvm/java-17-openjfx/lib"
elif [ -n "$JAVAFX_HOME" ]; then
    JAVAFX_PATH="$JAVAFX_HOME/lib"
fi

# Compile
if [ -n "$JAVAFX_PATH" ]; then
    echo "Using JavaFX at: $JAVAFX_PATH"
    javac -d . --module-path "$JAVAFX_PATH" --add-modules javafx.controls \
        matrix/*.java algorithms/*.java MatrixBenchmark.java MatrixGUI.java Main.java
else
    echo "JavaFX not found in standard locations."
    echo "Attempting to compile without JavaFX (GUI will not work)..."
    javac -d . matrix/*.java algorithms/*.java MatrixBenchmark.java
    echo "Compiled core classes. To use GUI, install JavaFX:"
    echo "  sudo apt-get install openjfx libopenjfx-java"
fi

if [ $? -eq 0 ]; then
    echo "Build successful!"
else
    echo "Build failed!"
    exit 1
fi

