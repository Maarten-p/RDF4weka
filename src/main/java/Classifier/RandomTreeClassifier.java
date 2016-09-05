package Classifier;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

/**
 * A class for classifying examples using a loaded model
 */
public class RandomTreeClassifier {

    /**
     * The identifier of the model currently loaded into memory
     */
    private String identifier = null;

    /**
     * The model currently loaded into memory
     */
    private AbstractClassifier classifier;

    /**
     * The pre-parser attributes as given by the CLASSIFIER_DATA_QUERY
     */
    private ArrayList<String> oldheader;
    /**
     * The post-parser attributes as given by the ClassifierParser
     */
    private ArrayList<Attribute> newheader;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Loads the model from a native .model file
     * This is the fastest way but .model files can only be used by weka
     * @param payload
     * Contains the identifier of the model to load
     * @throws Exception
     * Couldn't load the model, the file referred by the identifier does not exist or another I/O error happened
     */
    public void loadModelFromNative(UseClassifierPayload payload) throws Exception {
        classifier = (AbstractClassifier) weka.core.SerializationHelper.read("/data/" + payload.getIdentifier() + ".model");
        oldheader = (ArrayList<String>) weka.core.SerializationHelper.read("/data/" + payload.getIdentifier() + "-oldheader.model");
        newheader = (ArrayList<Attribute>) weka.core.SerializationHelper.read("/data/" + payload.getIdentifier() + "-newheader.model");
    }

    /**
     * Converts an array of doubles, which represent an example, into an instance and calculates the probability of each class for it
     * @param doubles
     * An array of doubles, which represents an example
     * @return
     * The distribution of the probability that the example belongs to a certain class
     * @throws Exception
     * The classifier broke
     */
    private double[] classifyInstance(double[] doubles) throws Exception {
        Instance instance = new DenseInstance(1, doubles);
        instance.setDataset(new Instances("temp", newheader, newheader.size()));
        return classifier.distributionForInstance(instance);
    }

    /**
     * Classifies a list of examples
     * @param matrix
     * A two-dimensional array in which each row represents one example and each column one feature
     * @return
     * The distribution of classes for each example
     * @throws Exception
     * The classifier broke on one of the examples
     */
    private double[][] classifyInstances(double[][] matrix) throws Exception {
        double[][] results = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            results[i] = classifyInstance(matrix[i]);
        }
        return results;
    }

    /**
     * Converts a two-dimensional array of strings into a two-dimensional array of doubles by using the ClassifierParser.
     * The parser can convert one feature into multiple features.
     * @param matrix
     * A two-dimensional array in which each row represents one example and each column one pre-parser feature
     * @return
     * A two-dimensional array in which each row represents one example and each column one feature
     *
     */
    private double[][] parseStringMatrix(String[][] matrix) {

        double[][] results = new double[matrix.length][newheader.size() - 1];
        for (int i = 0; i < matrix.length; i++) {
            int temp = 0;
            for (int j = 0; j < matrix[0].length; j++) {
                Double[] parsedValues = ClassifierParser.parse(oldheader.get(j), matrix[i][j]);
                for (Double value : parsedValues) {
                    results[i][temp] = value;
                    temp += 1;
                }
            }
        }
        return results;
    }

    /**
     * Classifies a two-dimensional array in which each row represents one example and each column one pre-parser feature.
     * The array is first parsed and then each example gets classified.
     * @param matrix
     * A two-dimensional array in which each row represents one example and each column one pre-parser feature
     * @return
     * The distribution of classes for each example
     * @throws Exception
     * The classifier broke on one of the examples
     */
    public double[][] classifyString(String[][] matrix) throws Exception {
        return classifyInstances(parseStringMatrix(matrix));
    }

}
