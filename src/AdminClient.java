import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.*;
import java.rmi.registry.*;
import java.util.*;
import java.util.concurrent.*;

public class AdminClient extends Application {

    private VotingService service;
    private Label statusLabel;
    private Button startBtn, stopBtn, resetBtn;
    private TableView<Candidate> table;
    private ScheduledExecutorService scheduler;//used to periodically fetch results

    @Override
    public void start(Stage stage) {

        statusLabel = new Label("Not connected");
        startBtn = new Button("Start Voting");
        stopBtn = new Button("Stop Voting");
        resetBtn = new Button("Reset Votes");

        startBtn.setDisable(true);
        stopBtn.setDisable(true);
        resetBtn.setDisable(true);



        //used to start voting
        startBtn.setOnAction(e -> runInBackground(() -> {// Background thread
            try {
                boolean ok = service.startVoting();
                Platform.runLater(() -> statusLabel.setText("Voting started: " + ok)); //before voting is OPEN 
            } catch (Exception ex) {}
        }));

        stopBtn.setOnAction(e -> runInBackground(() -> {
            try {
                boolean ok = service.stopVoting();
                Platform.runLater(() -> statusLabel.setText("Voting stopped: " + ok)); //before voting is CLOSED 
            } catch (Exception ex) {}
        }));

        resetBtn.setOnAction(e -> runInBackground(() -> {
            try {
                boolean ok = service.resetVotes();
                Platform.runLater(() -> statusLabel.setText("Votes reset: " + ok));// when reset it show and then return old status
            } catch (Exception ex) {}
        }));

        // Table to show results 
        //<Candidate, String> means the row type is Candidate and the column shows a String property taken from Candidate.
        table = new TableView<>();//Creates a TableView whose rows will hold objects of type Candidate : Each row = one Candidate object.
        TableColumn<Candidate, String> nameCol = new TableColumn<>("Candidate"); 
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));//get candidate name
        TableColumn<Candidate, Integer> votesCol = new TableColumn<>("Votes");
        votesCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getVotes()).asObject());//get candidate votes
        table.getColumns().addAll(nameCol, votesCol);

        VBox root = new VBox(10,
                new HBox(10, startBtn, stopBtn, resetBtn),
                statusLabel,
                new Label("Live Results:"),
                table
        );
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root, 400, 400));
        stage.setTitle("Admin Panel");
        stage.show();

        connectToServer();
    }



    private void connectToServer() {
        new Thread(() -> {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                service = (VotingService) registry.lookup("VotingService");

                Platform.runLater(() -> {// Update UI on JavaFX Application Thread
                    statusLabel.setText("Connected ✓");
                    startBtn.setDisable(false);
                    stopBtn.setDisable(false);
                    resetBtn.setDisable(false);
                });
                //we want to repeatedly fetch live results without blocking. A scheduler is a clean, tested pattern for periodic tasks.
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        Map<String, Integer> results = service.getResults();
                        boolean open = service.isVotingOpen();

                        Platform.runLater(() -> {
                            updateResults(results);
                            statusLabel.setText("Voting is " + (open ? "OPEN" : "CLOSED"));
                        });

                    } catch (Exception ex) {}
                }, 0, 2, TimeUnit.SECONDS); //every 2 seconds

            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Connection failed"));
            }
        }).start();// Start a new thread to connect to the server
    }


    // Update the results table with new data
    private void updateResults(Map<String, Integer> results) {
        ObservableList<Candidate> items = FXCollections.observableArrayList();//Create a new list and populate it
        for (var entry : results.entrySet()) {//convert map to Candidate objects
            items.add(new Candidate(entry.getKey(), entry.getValue()));//add each candidate to the list
        }
        table.setItems(items);//set the table's items to the new list
    }



    private void runInBackground(Runnable r) {// Utility to run a task in background
        new Thread(r).start();// Start a new thread to run the task
    }


    // Clean up resources when the application stops
    @Override
    public void stop() throws Exception {
        if (scheduler != null) scheduler.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
