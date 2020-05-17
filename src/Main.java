import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Application;

import java.io.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>Main</>
 * <h1>Constraints programming final project visual UI</h1>
 * <p>This program takes in a model, a data file and the path to minizinc and displays the solution in a visually
 * friendly manner, as well as allowing the user to pick which solver to use for the execution of the model </p>
 *
 * @author david
 * @version 1.0.0 April 27, 2020
 */
public class Main extends Application {
    private TableView<String[]> tableView = new TableView<>();
    private String modelPath = "";
    private String dataPath = "";
    private String minizincPath = "";
    private String solver = "Chuffed";
    private ArrayList<String> rawData = new ArrayList<>(1);
    private ArrayList<ArrayList<String>> processedData = new ArrayList<>();
    private int numberOfActors, numberOfScenes, cost, timeShared = -1;
    private Label timeSharedLabel;
    private Button selectDataButton, selectModelButton, minizincPathButton;
    private boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    private Label currentPathLabel;

    /**
     * Saves to a text file the directories to use as model and data
     */
    private void savePaths() {
        String path = System.getProperty("user.dir");
        if (isWindows)
            path = path.concat("\\paths.txt");
        else
            path = path.concat("/paths.txt");
        String content = modelPath + "\n" + dataPath + "\n" + minizincPath;
        try {
            Files.write(Paths.get(path), content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads from a text file the directories to use as model and data
     */
    private void readPaths() {
        try {
            String path = System.getProperty("user.dir");
            if (isWindows)
                path = path.concat("\\paths.txt");
            else
                path = path.concat("/paths.txt");

            if (Files.exists(Paths.get(path))) {
                List<String> allLines = Files.readAllLines(Paths.get(path));
                if (!(allLines.size() == 0)) {
                    System.out.println(allLines.size());
                    modelPath = allLines.get(0);
                    dataPath = allLines.get(1);
                    minizincPath = allLines.get(2);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates the columns for the table view
     *
     * @param columns the number of columns that the table view will have
     */
    private void addColsToTable(int columns) {
        //950, 600
        TableColumn<String[], String> actorsCol = new TableColumn<>("Scene");
        TableColumn<String[], String> costCol = new TableColumn<>("Cost");
        actorsCol.setCellValueFactory((p) -> {
            String[] x = p.getValue();
            return new SimpleStringProperty(x[0]);
        });
        actorsCol.setMaxWidth(950 * 0.1);
        actorsCol.setMinWidth(950 * 0.1);
        costCol.setMaxWidth(950 * 0.1);
        costCol.setMinWidth(950 * 0.1);
        tableView.getColumns().add(actorsCol);
        for (int i = 1; i < columns + 1; i++) {
            TableColumn<String[], String> col = new TableColumn<>(processedData.get(2).get(i - 1));
            int finalI = i;
            col.setCellValueFactory((p) -> {
                String[] x = p.getValue();
                return new SimpleStringProperty(x[finalI]);
            });
            col.setMaxWidth((950 * 0.8) / (double) columns);
            col.setMinWidth((950 * 0.8) / (double) columns);
            col.setSortable(false);
            tableView.getColumns().add(col);
        }

        costCol.setCellValueFactory((p) -> {
            String[] x = p.getValue();
            return new SimpleStringProperty(x[columns + 1]);
        });
        costCol.setSortable(false);
        tableView.getColumns().add(costCol);
    }

    /**
     * Takes in the output of the minizinc model and parses it
     *
     * @param string takes a string and adds everything inside the [ ] to rawData
     */
    private void parseBrackets(String string) {
        Pattern p = Pattern.compile("(?<=\\[)[^]]*(?=])");
        Matcher m = p.matcher(string);
        while (m.find()) {
            rawData.add(m.group(0));
        }
    }

    /**
     * Takes the rawData and parses it
     */
    private void parseCommas() {
        processedData = new ArrayList<>();
        for (String rawDatum : rawData) {
            processedData.add(new ArrayList<String>(Arrays.asList(rawDatum.split(","))));
        }
    }

    /**
     * Generates the rows of the table view with the data provided from the output of the minizinc model
     */
    private void fillTable() {
        parseCommas();
        numberOfActors = Integer.parseInt(processedData.get(0).get(0).trim());
        numberOfScenes = Integer.parseInt(processedData.get(0).get(1).trim());
        cost = Integer.parseInt(processedData.get(0).get(2).trim());
        if (processedData.get(0).size() == 4) {
            timeShared = Integer.parseInt(processedData.get(0).get(3).trim());
            timeSharedLabel.setText("Time Shared " + timeShared);
            timeSharedLabel.setVisible(true);
        } else {
            timeSharedLabel.setVisible(false);
        }

        ArrayList<ArrayList<String>> temporalData = new ArrayList<>(1);
        for (int i = 0; i < numberOfActors; i++) {
            temporalData.add(new ArrayList<String>());
            temporalData.get(i).add("Actor" + (i + 1));
            for (int j = i * numberOfScenes; j < numberOfScenes * (i + 1); ++j)
                temporalData.get(i).add(processedData.get(1).get(j));
            temporalData.get(i).add(processedData.get(4).get(i));
        }

        temporalData.add(new ArrayList<String>());
        temporalData.get(temporalData.size() - 1).add("Duration");
        for (int i = 0; i < processedData.get(3).size(); ++i)
            temporalData.get(temporalData.size() - 1).add(processedData.get(3).get(i));
        temporalData.get(temporalData.size() - 1).add(cost + "");


        addColsToTable(numberOfScenes);
        String[][] data = new String[temporalData.size()][];
        for (int i = 0; i < temporalData.size(); i++) {
            ArrayList<String> row = temporalData.get(i);
            String[] copy = new String[row.size()];
            for (int j = 0; j < row.size(); j++)
                copy[j] = row.get(j);
            data[i] = copy;
        }
        tableView.getItems().addAll(Arrays.asList(data));
    }

    /**
     * @param buttons sets from 0 to many buttons to a certain width and height
     */
    private void setButtonSize(Button... buttons) {
        for (Button button : buttons) {
            button.setMinSize(175, 40);
            button.setMaxSize(175, 40);
        }
    }

    private void showPath(Button button, String path) {
        button.setOnMouseEntered(e -> {
            currentPathLabel.setText(path);
            currentPathLabel.setVisible(true);
        });

        button.setOnMouseExited(e -> currentPathLabel.setVisible(false));
    }

    private void runMinizinc(StackPane root) {
        System.out.println(solver + "\n" + minizincPath + "\n" + modelPath + "\n" + dataPath);
        try {
            if (!(modelPath.trim().isEmpty() || dataPath.trim().isEmpty() || minizincPath.trim().isEmpty())) {
                Process process;
                if (isWindows) {
                    process = Runtime.getRuntime()
                            .exec(minizincPath + " --solver " + solver + " " + modelPath + " " + dataPath);
                } else {
                    process = Runtime.getRuntime()
                            .exec(minizincPath + " --solver " + solver + " " + modelPath + " " + dataPath);
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                rawData = new ArrayList<>(1);
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    if (line.charAt(0) == '[')
                        parseBrackets(line);
                }

                int exitCode = process.waitFor();
                assert exitCode == 0;
                tableView = new TableView<>();
                tableView.setMinSize(950, 550);
                tableView.setPrefSize(950, 550);
                tableView.setMaxSize(955, 560);
                root.getChildren().addAll(tableView);
                StackPane.setAlignment(tableView, Pos.CENTER_RIGHT);
                fillTable();
                tableView.setVisible(true);
            } else {
                //Todo err message
            }

        } catch (Exception f) {
            f.printStackTrace();
        }
    }

    private void selectModelPath() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select model file .mzn: ");

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            if (selectedFile.getAbsolutePath().substring(selectedFile.getAbsolutePath().length() - 4).equals(".mzn")) {
                modelPath = selectedFile.getAbsolutePath();
                selectModelButton.getStyleClass().remove("button-unselected");
                showPath(selectModelButton, modelPath);
                System.out.println(selectedFile.getAbsolutePath());
            } else {
                //Todo alert box
                selectModelButton.getStyleClass().remove("button-unselected");
                selectModelButton.getStyleClass().add("button-unselected");
            }
        }
    }

    private void selectDataPath() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select data file .dzn ");

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            if (selectedFile.getAbsolutePath().substring(selectedFile.getAbsolutePath().length() - 4).equals(".dzn")) {
                dataPath = selectedFile.getAbsolutePath();
                selectDataButton.getStyleClass().remove("button-unselected");
                showPath(selectDataButton, dataPath);
                System.out.println(selectedFile.getAbsolutePath());
            } else {
                //Todo alert box
                selectModelButton.getStyleClass().remove("button-unselected");
                selectDataButton.getStyleClass().add("button-unselected");
            }
        }
    }

    private void selectMinizincPath() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select minizinc path ");
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            if (isWindows) {
                if (selectedFile.getAbsolutePath().substring(selectedFile.getAbsolutePath().length() - 4).equals(".exe")) {
                    minizincPath = selectedFile.getAbsolutePath();
                    minizincPathButton.getStyleClass().remove("button-unselected");
                    showPath(minizincPathButton, minizincPath);
                    System.out.println(selectedFile.getAbsolutePath());
                } else {
                    //Todo alert box
                    minizincPathButton.getStyleClass().remove("button-unselected");
                    minizincPathButton.getStyleClass().add("button-unselected");
                }
            } else {
                if (selectedFile.getAbsolutePath().length() == 0) {
                    minizincPath = selectedFile.getAbsolutePath();
                    minizincPathButton.getStyleClass().remove("button-unselected");
                    showPath(minizincPathButton, minizincPath);
                    System.out.println(selectedFile.getAbsolutePath());
                } else {
                    //Todo alert box
                    minizincPathButton.getStyleClass().remove("button-unselected");
                    minizincPathButton.getStyleClass().add("button-unselected");
                }
            }
        }
    }

    /**
     * Renders the main window of the program
     *
     * @param window the program stage
     */
    @Override
    public void start(Stage window) {

        tableView.setVisible(false);
        readPaths();

        StackPane root = new StackPane();
        root.setMinSize(1200, 675);
        root.setMaxSize(1200, 675);
        root.setPrefSize(1200, 675);
        root.setPadding(new Insets(25.0, 25.0, 10.0, 0));
        root.setStyle("-fx-background-color: #161E2D;");

        VBox infoPane = new VBox();
        infoPane.setMinSize(200, 675);
        infoPane.setMaxSize(200, 675);
        infoPane.setSpacing(10.0);

        currentPathLabel = new Label();
        currentPathLabel.getStyleClass().add("label-path");
        currentPathLabel.setVisible(false);

        selectModelButton = new Button("Select model .mzn");
        if (modelPath.trim().isEmpty())
            selectModelButton.getStyleClass().add("button-unselected");
        selectModelButton.setOnAction(e -> selectModelPath());

        selectDataButton = new Button("Select data file .dzn");
        if (dataPath.trim().isEmpty())
            selectDataButton.getStyleClass().add("button-unselected");
        selectDataButton.setOnAction(e -> selectDataPath());

        minizincPathButton = new Button("Select minizinc path");
        if (minizincPath.trim().isEmpty())
            minizincPathButton.getStyleClass().add("button-unselected");
        minizincPathButton.setOnAction(e -> selectMinizincPath());

        String[] solvers = new String[]{"Chuffed", "Gecode"};
        ComboBox<String> solverComboBox = new ComboBox<>(FXCollections.observableArrayList(solvers));
        solverComboBox.setOnAction(e -> solver = solverComboBox.getValue().trim());
        solverComboBox.setMinSize(175, 40);
        solverComboBox.setMaxSize(175, 40);
        solverComboBox.setValue(solver);

        Button runButton = new Button("Run model");
        runButton.setOnAction(e -> runMinizinc(root));

        Button savePathsButton = new Button("Save paths");
        savePathsButton.setOnAction(e -> savePaths());

        timeSharedLabel = new Label(timeShared + ""); //timeShared
        timeSharedLabel.setMaxSize(175, 40);
        timeSharedLabel.setMinSize(175, 40);
        timeSharedLabel.setVisible(false);

        setButtonSize(selectModelButton, selectDataButton, minizincPathButton, runButton, savePathsButton);
        showPath(selectModelButton, modelPath);
        showPath(selectDataButton, dataPath);
        showPath(minizincPathButton, minizincPath);

        infoPane.getChildren().addAll(solverComboBox, selectModelButton, selectDataButton, minizincPathButton, runButton, savePathsButton, timeSharedLabel);
        infoPane.setAlignment(Pos.CENTER);
        root.getChildren().addAll(infoPane, tableView, currentPathLabel);

        tableView.setMinSize(950, 600);
        tableView.setPrefSize(950, 600);
        tableView.setMaxSize(955, 610);

        StackPane.setAlignment(infoPane, Pos.CENTER_LEFT);
        StackPane.setAlignment(tableView, Pos.CENTER_RIGHT);
        StackPane.setAlignment(currentPathLabel, Pos.BOTTOM_LEFT);

        Scene rootScene = new Scene(root);
        rootScene.getStylesheets().add("style.css");

        window.setScene(rootScene);
        window.setHeight(675);
        window.setWidth(1200);
        window.setTitle("Constraints");
        window.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

