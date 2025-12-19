import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
//It waits silently for clients to call RMI methods
public class VotingServer {
    public static void main(String[] args) {
        try {
            /*If the server is started with arguments:
                java VotingServer Trump Biden Obama
                → those names become candidates.
                If no arguments → use default: Alice, Bob, Charlie. */
            List<String> candidates = new ArrayList<>();// List to hold candidate names
            if (args.length > 0) {// Check if there are command-line arguments
                // args are candidate names
                candidates.addAll(Arrays.asList(args));
            } else {
                candidates.add("Alice");
                candidates.add("Bob");
                candidates.add("Charlie");
            }
            // Create object the voting service implementation with candidates
            VotingServiceImpl service = new VotingServiceImpl(candidates);

            // Create RMI registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // Bind the service
            registry.rebind("VotingService", service);

            System.out.println("VotingServer started, candidates: " + candidates);
            System.out.println("RMI registry running on port 1099. Waiting for clients...");
            System.out.println("Candidates: " + candidates);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
