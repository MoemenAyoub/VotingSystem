import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface VotingService extends Remote {// Remote interface for RMI voting service

    boolean loginVoter(String username, String password) throws RemoteException;// Voter login

    boolean castVote(String username, String candidateName) throws RemoteException;// Cast a vote for a candidate

    Map<String, Integer> getResults() throws RemoteException;// Get current voting results

    boolean startVoting() throws RemoteException;// Start the voting process

    boolean stopVoting() throws RemoteException;// Stop the voting process

    boolean resetVotes() throws RemoteException;// Reset all votes

    boolean isVotingOpen() throws RemoteException;// Check if voting is currently open
}
