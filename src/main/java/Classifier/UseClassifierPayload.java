package Classifier;

import Main.Algorithm;

public class UseClassifierPayload {

    private String[][] toClassify;
    private Algorithm algorithm;
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

    public void insertDefaults() {
        if (toClassify == null) {
            setToClassify(new String[][]{{"1", "7"}, {"4", "5"}});
        }
        if (algorithm == null)
            setAlgorithm(Algorithm.RANDOMTREE);
    }
}
