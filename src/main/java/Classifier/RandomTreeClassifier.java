package Classifier;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

public class RandomTreeClassifier {

    private String identifier = null;

    private AbstractClassifier classifier;

    private ArrayList<String> oldheader;
    private ArrayList<Attribute> newheader;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void loadModelFromNative(UseClassifierPayload payload) throws Exception {
        classifier = (AbstractClassifier) weka.core.SerializationHelper.read(payload.getIdentifier() + ".model");
        oldheader = (ArrayList<String>) weka.core.SerializationHelper.read(payload.getIdentifier() + "-oldheader.model");
        newheader = (ArrayList<Attribute>) weka.core.SerializationHelper.read(payload.getIdentifier() + "-newheader.model");
    }

    private double[] classifyInstance(double[] doubles) throws Exception {
        Instance instance = new DenseInstance(1, doubles);
        instance.setDataset(new Instances("temp", newheader, newheader.size()));
        return classifier.distributionForInstance(instance);
    }

    private double[][] classifyInstances(double[][] matrix) throws Exception {
        double[][] results = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            results[i] = classifyInstance(matrix[i]);
        }
        return results;
    }

    private double[][] parseStringMatrix(String[][] matrix) throws Exception {

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

    public double[][] classifyString(String[][] matrix) throws Exception {
        return classifyInstances(parseStringMatrix(matrix));
    }

}
