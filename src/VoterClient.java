import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.*;
import java.rmi.registry.*;
import java.util.*;

public class VoterClient extends Application {

    // As we say in the class the components are outside but private
    private VotingService service;//service is an RMI remote object — the client’s reference (stub) to the server's VotingServiceImpl
    private TextField usernameField;
    private PasswordField passwordField;
    private ComboBox<String> candidatesBox;//dropdown box for candidates
    private Label statusLabel;
    private Button voteBtn;

    @Override
    public void start(Stage stage) {

        usernameField = new TextField();
        usernameField.setPromptText("Username");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        candidatesBox = new ComboBox<>();
        candidatesBox.setDisable(true);

        voteBtn = new Button("Vote");
        voteBtn.setDisable(true);

        statusLabel = new Label("Not connected");// if server is off

        voteBtn.setOnAction(e -> sendVote());

        VBox root = new VBox(10,
                new HBox(10, new Label("Username:"), usernameField),
                new HBox(10, new Label("Password:"), passwordField),
                new HBox(10, new Label("Candidate:"), candidatesBox),
                voteBtn,
                statusLabel
        );

        root.setPadding(new Insets(10));
        stage.setScene(new Scene(root, 400, 250));
        stage.setTitle("Voter Client");
        stage.show();

        connectToServer();
    }



    private void connectToServer() { // Connect to RMI server (Lab)
        new Thread(() -> {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                service = (VotingService) registry.lookup("VotingService");

                Platform.runLater(() -> statusLabel.setText("Connected ✓"));

                loadCandidates();

            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Connection failed"));
            }
        }).start();
    }


    //when connected, load candidates from server
    private void loadCandidates() { 
        new Thread(() -> {// Background thread
            try {
                Map<String, Integer> results = service.getResults(); // results = { "Alice"=3, "Bob"=5, "Charlie"=1 }

                Platform.runLater(() -> {
                    candidatesBox.getItems().clear();//clear any old items 
                    candidatesBox.getItems().addAll(results.keySet()); //keySet are candidate names only from the map
                    candidatesBox.setDisable(false);
                    voteBtn.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error loading candidates"));
            }
        }).start();
    }



    //send vote to server
    private void sendVote() {
        String username = usernameField.getText().trim(); //trim to remove spaces
        String password = passwordField.getText();
        String candidate = candidatesBox.getValue();

        if (candidate == null) {
            statusLabel.setText("Select a candidate");
            return;
        }

        new Thread(() -> { // Do RMI work here (background thread) 
            try {
                boolean logged = service.loginVoter(username, password);

                if (!logged) {
                    Platform.runLater(() ->
                        statusLabel.setText("Login failed (wrong username/password)")
                    );
                    return;
                }

                boolean ok = service.castVote(username, candidate);// Send vote to server

                Platform.runLater(() -> { // Update UI here (JavaFX thread)
                    if (ok)
                        statusLabel.setText("Vote cast for " + candidate);
                    else
                        statusLabel.setText("Vote failed (already voted or closed)");
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                    statusLabel.setText("Error: " + e.getMessage())
                );
            }
        }).start();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
