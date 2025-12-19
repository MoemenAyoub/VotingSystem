import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class VotingServiceImpl extends UnicastRemoteObject implements VotingService {

    private final ConcurrentHashMap<String, AtomicInteger> votes = new ConcurrentHashMap<>();// Candidate name -> vote count
    private final Set<String> votersVoted = ConcurrentHashMap.newKeySet();// Set of usernames who have voted
    private volatile boolean votingOpen = false;

    private final ReentrantLock lock = new ReentrantLock();// Lock for synchronizing critical sections

    // Username -> Password registered users
    private final Map<String, String> registeredUsers = new HashMap<>();



    public VotingServiceImpl(List<String> candidates) throws RemoteException {
        super();// used to export the remote object

       //For every candidate c, it adds an entry to the votes map with key c and value new AtomicInteger(0)
        for (String c : candidates) {//initialize candidates with 0 votes
            votes.put(c, new AtomicInteger(0));
        }

        // Example users (pre-registered)
        registeredUsers.put("Moemen", "1234");
        registeredUsers.put("Mohamad", "1111");
        registeredUsers.put("Elias", "pass");
    }



    @Override
    public boolean loginVoter(String username, String password) throws RemoteException {
        if (username == null || password == null ||
            username.isBlank() || password.isBlank()) {
            System.out.println("Login failed: empty fields");
            return false;
        }

        if (!registeredUsers.containsKey(username)) {
            System.out.println("Login failed: user not found -> " + username);
            return false;
        }

        if (!registeredUsers.get(username).equals(password)) {
            System.out.println("Login failed: wrong password for -> " + username);
            return false;
        }

        System.out.println("Login success: " + username);
        return true;
    }



    @Override
    public boolean castVote(String username, String candidateName) throws RemoteException {
        lock.lock();
        try {
            if (!votingOpen) {
                System.out.println("Vote rejected (voting closed) from: " + username);
                return false;
            }

            if (votersVoted.contains(username)) {
                System.out.println("Vote rejected (already voted): " + username);
                return false;
            }

            AtomicInteger counter = votes.get(candidateName);
            if (counter == null) {
                System.out.println("Vote rejected (unknown candidate)");
                return false;
            }

            counter.incrementAndGet();// Increment vote count atomically
            votersVoted.add(username);

            System.out.println("Vote accepted: " + username + " -> " + candidateName);
            return true;

        } finally {
            lock.unlock();
        }
    }



//"Alice" -> AtomicInteger(5)
//"Bob"   -> AtomicInteger(2)
//snapshot becomes:
//"Alice" -> 5
//"Bob"   -> 2
    @Override
    public Map<String, Integer> getResults() throws RemoteException {
        Map<String, Integer> snapshot = new HashMap<>();//Create an empty map to store the results "Alice" -> 5
        for (var entry : votes.entrySet()) {//iterate over each entry in the votes map
            snapshot.put(entry.getKey(), entry.getValue().get());//Put each candidate and its vote count into the snapshot  
        }
        return snapshot;
    }


    @Override
    public boolean startVoting() throws RemoteException {
        lock.lock();// users the lock to ensure that only one thread can modify the votingOpen state at a time
        try {
            if (votingOpen) return false;
            votingOpen = true;
            System.out.println("Voting started.");
            return true;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public boolean stopVoting() throws RemoteException {
        lock.lock();
        try {
            if (!votingOpen) return false;
            votingOpen = false;
            System.out.println("Voting stopped.");
            return true;
        } finally {
            lock.unlock();
        }
    }

    
    @Override
    public boolean resetVotes() throws RemoteException {
        lock.lock();
        try {
            for (AtomicInteger ai : votes.values()) {
                ai.set(0);
            }
            votersVoted.clear();
            System.out.println("Votes reset.");
            return true;
        } finally {
            lock.unlock();
        }
    }



    //
    @Override
    public boolean isVotingOpen() throws RemoteException {
        return votingOpen;//
    }
}