package tucker;

/** The columns the dashboard can sort by. */
public enum SortKey {
    DATE("Date"), TIME("Time"), NAME("Name"), ADDRESS("Address");

    public final String label;

    SortKey(String label) {
        this.label = label;
    }
}
