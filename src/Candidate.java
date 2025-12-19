public class Candidate {
    private final String name;
    private final int votes;

    public Candidate(String name, int votes) {
        this.name = name;
        this.votes = votes;
    }

    public String getName() { return name; }
    public int getVotes() { return votes; }
}
/*JavaFX TableView cannot display a Map directly.

Your server returns results as:

Map<String, Integer>
candidate → votes


But TableView needs a list of objects, not a map.

So we convert:

"Alice" → 5
"Bob"   → 2
"Charlie" → 7


Into:

new Candidate("Alice", 5)
new Candidate("Bob", 2)
new Candidate("Charlie", 7)


And put them in an ObservableList for the table. */