package com.sorting.visualizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 700;
    private static final int BAR_COUNT = 60;

    // Sorting algorithms names
    private static final String ALG_BUBBLE = "Bubble Sort";
    private static final String ALG_INSERTION = "Insertion Sort";
    private static final String ALG_SELECTION = "Selection Sort";
    private static final String ALG_MERGE = "Merge Sort";
    private static final String ALG_QUICK = "Quick Sort";

    private Visualizer visualizer;
    private VBox labelBox;

    @Override
    public void start(Stage stage) {

        // --- CENTER: Canvas ---
        visualizer = new Visualizer(BAR_COUNT);
        Canvas canvas = visualizer.getCanvas();

        // --- LEFT: Value labels ---
        labelBox = new VBox(4);
        labelBox.setPadding(new Insets(6));
        labelBox.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #cccccc;");

        ScrollPane labelScroll = new ScrollPane(labelBox);
        labelScroll.setFitToWidth(true);
        labelScroll.setPrefWidth(120);

        // --- BOTTOM: Controls ---
        ComboBox<String> algoDropdown = new ComboBox<>();
        algoDropdown.getItems().addAll(ALG_BUBBLE, ALG_INSERTION, ALG_SELECTION, ALG_MERGE, ALG_QUICK);
        algoDropdown.setValue(ALG_BUBBLE);

        Button shuffleBtn = new Button("Shuffle");
        Button sortBtn = new Button("Sort");
        ToggleButton orientationToggle = new ToggleButton("Horizontal");
        ToggleButton pauseBtn = new ToggleButton("Pause/Resume");

        // Speed slider
        Slider speedSlider = new Slider(1, 50, 18);
        speedSlider.setPrefWidth(150);
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> visualizer.setSpeed(newV.doubleValue()));

        // Bar Count slider
        Slider barCountSlider = new Slider(10, 200, BAR_COUNT);
        barCountSlider.setPrefWidth(150);
        barCountSlider.setShowTickMarks(true);
        barCountSlider.setShowTickLabels(true);
        barCountSlider.setMajorTickUnit(30);
        barCountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            visualizer.setBarCount(newVal.intValue());
            visualizer.shuffle();
            updateLabels();
        });

        // Shuffle button
        shuffleBtn.setOnAction(e -> {
            visualizer.shuffle();
            updateLabels();
        });

        // Sort button
        sortBtn.setOnAction(e -> {
            Runnable sortTask;
            String chosenAlgo = algoDropdown.getValue();

            switch (chosenAlgo) {
                case ALG_INSERTION -> sortTask = visualizer::insertionSort;
                case ALG_SELECTION -> sortTask = visualizer::selectionSort;
                case ALG_MERGE -> sortTask = visualizer::mergeSort;
                case ALG_QUICK -> sortTask = visualizer::quickSort;
                default -> sortTask = visualizer::bubbleSort;
            }

            // Disable controls while sorting (except pause)
            setControlsDisabled(true, algoDropdown, shuffleBtn, sortBtn, orientationToggle, barCountSlider);
            pauseBtn.setDisable(false); // allow pause

            new Thread(() -> {
                sortTask.run();
                Platform.runLater(() -> setControlsDisabled(false, algoDropdown, shuffleBtn, sortBtn, orientationToggle, barCountSlider));
            }).start();
        });

        // Horizontal toggle
        orientationToggle.setOnAction(e -> {
            visualizer.setHorizontal(orientationToggle.isSelected());
            visualizer.shuffle();
            updateLabels();
        });

        // Pause/Resume toggle
        pauseBtn.setOnAction(e -> visualizer.setPaused(pauseBtn.isSelected()));

        HBox controls = new HBox(12, algoDropdown, shuffleBtn, sortBtn, orientationToggle, pauseBtn, speedSlider, barCountSlider);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER);

        // Layout
        BorderPane root = new BorderPane();
        root.setLeft(labelScroll);
        root.setCenter(canvas);
        root.setBottom(controls);

        visualizer.setOnDraw(this::updateLabels);

        visualizer.shuffle();
        updateLabels();

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setTitle("Sorting Visualizer");
        stage.setScene(scene);
        stage.show();
    }

    private void setControlsDisabled(boolean disabled, Control... controls) {
        for (Control c : controls) c.setDisable(disabled);
    }

    private void updateLabels() {
        int[] currentValues = visualizer.getBarsCopy();
        Platform.runLater(() -> {
            labelBox.getChildren().clear();
            for (int i = 0; i < currentValues.length; i++) {
                Label label = new Label("arr[" + i + "] = " + currentValues[i]);
                label.setPrefWidth(110);
                labelBox.getChildren().add(label);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
