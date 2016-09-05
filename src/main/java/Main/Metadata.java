package Main;

/**
 * Describes the metadata of an algorithm run.
 */
public class Metadata {

    private long runTime;
    private String algorithm;
    private String query;
    private int totalRows;
    private String options;

    public Metadata(long runTime, String query, String algorithm, int totalRows, String options) {
        setQuery(query);
        setRunTime(runTime);
        setAlgorithm(algorithm);
        setTotalRows(totalRows);
        setOptions(options);
    }

    public long getRunTime() {
        return runTime;
    }

    private void setRunTime(long runTime) {
        this.runTime = runTime;
    }

    public String getQuery() {
        return query;
    }

    private void setQuery(String query) {
        this.query = query;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getTotalRows() {
        return totalRows;
    }

    private void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public String getOptions() {
        return options;
    }

    private void setOptions(String options) {
        this.options = options;
    }

}
