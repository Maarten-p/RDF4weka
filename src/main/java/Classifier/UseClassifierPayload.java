package Classifier;

import Main.Algorithm;

/**
 * This class represents the json payload that is expected when using the Classifier
 */
public class UseClassifierPayload {

    /**
     * A two-dimensional array in which every row represents an example to classify and every column represents a feature
     */
    private String[][] toClassify;
    /**
     * The algorithm to use
     */
    private Algorithm algorithm;
    /**
     * The identifier of the model which is given when creating the model
     */
    private String identifier;

    public String[][] getToClassify() {
        return toClassify;
    }

    private void setToClassify(String[][] toClassify) {
        this.toClassify = toClassify;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Checks whether this is a valid payload
     * @return
     * True if valid, false if not valid
     */
    public boolean isValid() {
        if (identifier == null || identifier.isEmpty())
            return false;
        Algorithm[] values = Algorithm.values();
        for (Algorithm value : values) {
            if (value == this.algorithm)
                return true;
        }
        return false;
    }

    /**
     * Insert default values where possible if a null value is received
     */
    public void insertDefaults() {
        if (toClassify == null) {
            setToClassify(new String[][]{{"1", "7"}, {"4", "5"}});
        }
        if (algorithm == null)
            setAlgorithm(Algorithm.RANDOMTREE);
    }
}
