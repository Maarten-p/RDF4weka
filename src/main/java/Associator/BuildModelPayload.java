package Associator;

import Main.Algorithm;

/**
 * The expected json format for a request to build a model. (Can be either an Associator or a Classifier, might be split in the future)
 * Contains the algorithm with which to build a model, the options of the algorithm to use and the method of storage
 */
public class BuildModelPayload {

    private String[] options;
    private Algorithm algorithm;
    private String method;

    public String[] getOptions() {
        return options;
    }

    private void setOptions(String[] options) {
        this.options = options;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Checks whether the given payload is valid.
     *
     * @return true if valid, false if not valid
     */
    public boolean isValid() {
        Algorithm[] values = Algorithm.values();
        for (Algorithm value : values) {
            if (value == this.algorithm)
                return true;
        }
        return false;
    }

    /**
     * Inserts default values where possible if null values are given
     */
    public void insertDefaults() {

        if (algorithm == null)
            setAlgorithm(Algorithm.FPGROWTH);

        if (options == null) {
            String[] newOptions = new String[7];
            newOptions[0] = "-C";
            newOptions[1] = "0.85";
            newOptions[2] = "-M";
            newOptions[3] = "0.032";
            newOptions[4] = "-T";
            newOptions[5] = "0";
            newOptions[6] = "-S";
            setOptions(newOptions);
        }
    }

}
