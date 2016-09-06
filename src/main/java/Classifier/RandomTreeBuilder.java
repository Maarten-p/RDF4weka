package Classifier;

import Associator.BuildModelPayload;
import Main.Algorithm;
import Main.Metadata;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The builder for the RandomTree algorithm. In the future this should extend the ClassifierBuilder class, which does not yet exist.
 * It builds a RandomTree model from a query and calls ClassifierWriter to write it to disk.
 */
public class RandomTreeBuilder {

    /**
     * The uuid of the most recently build model, to be given to the user so they can identify the model.
     */
    private String newestUuid;

    /**
     * Creates a list of instances from data from the database and uses them to build a classifier. It then writes this model + headers to disk as a native .model file.
     *
     * @param repo    The repository that contains the data to be queried
     * @param payload Contains the algorithm and its options to use, and the method for storing the resulting model. Given by the user from the frontend.
     *                The method is currently unnecessary for a classifier since storing to disk is the only option
     * @return Returns a Metadata object with metadata information about the construction of the model
     */
    public Metadata buildModel(Repository repo, BuildModelPayload payload) {
        RepositoryConnection conn = repo.getConnection();
        String queryString = System.getenv("CLASSIFIER_DATA_QUERY");
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        try {
            TupleQueryResult result = tupleQuery.evaluate();
            List<String> names = result.getBindingNames();
            List<String> transformedNames = ClassifierParser.transformHeader(names);
            Instances instanceList = new Instances("theData", stringsToAttributes(transformedNames), transformedNames.size());
            instanceList.setClassIndex(transformedNames.size() - 1);

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                instanceList.add(bindingSetToInstance(bindingSet, names));
            }
            result.close();
            conn.close();

            Algorithm algorithm = payload.getAlgorithm();
            AbstractClassifier classifier;
            //can be expanded with other Classifier algorithms
            switch (algorithm) {
                case RANDOMTREE: {
                    classifier = new RandomTree();
                    break;
                }
                default:
                    throw new IllegalArgumentException();
            }
            String[] options = payload.getOptions();
            classifier.setOptions(options);
            long startTime = System.currentTimeMillis();
            System.out.println(instanceList.toSummaryString());
            classifier.buildClassifier(instanceList);
            long endTime = System.currentTimeMillis();
            long runtime = endTime - startTime;
            String concatenatedOptions = "";
            for (String optionValue : options) {
                concatenatedOptions += optionValue;
            }
            Metadata metadata = new Metadata(runtime, queryString, algorithm.toString(), instanceList.size(), concatenatedOptions);
            com.eaio.uuid.UUID uuid = new com.eaio.uuid.UUID();
            setNewestUuid(uuid.toString());
            Evaluation test = new Evaluation(instanceList);
            test.evaluateModel(classifier, instanceList);
            ClassifierWriter writer = new ClassifierWriter();
            writer.toNativeFile(classifier, stringsToAttributes(transformedNames), names, uuid.toString());
            return metadata;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Instance bindingSetToInstance(BindingSet bindingset, List<String> names) {

        List<Double> tempList = new ArrayList<>();
        for (int i = 0; i < names.size() - 1; i++) {
            Double[] tempArray = ClassifierParser.parse(names.get(i), (bindingset.getValue(names.get(i))).stringValue());
            tempList.addAll(Arrays.asList(tempArray));
        }
        double[] attList = new double[tempList.size() + 1];
        for (int i = 0; i < tempList.size(); i++) {
            attList[i] = tempList.get(i);
        }
        attList[tempList.size()] = Integer.parseInt(bindingset.getValue(names.get(names.size() - 1)).stringValue());
        return new DenseInstance(1, attList);
    }

    private ArrayList<Attribute> stringsToAttributes(List<String> strings) {
        ArrayList<Attribute> atts = new ArrayList<>();
        for (int i = 0; i < strings.size() - 1; i++) {
            atts.add(new Attribute(strings.get(i), Attribute.NUMERIC));
        }
        List<String> fvClassVal = new ArrayList<>();
        fvClassVal.add("yes");
        fvClassVal.add("no");
        Attribute ClassAttribute = new Attribute(strings.get(strings.size() - 1), fvClassVal);
        atts.add(ClassAttribute);
        return atts;
    }

    public String getNewestUuid() {
        return newestUuid;
    }

    private void setNewestUuid(String newestUuid) {
        this.newestUuid = newestUuid;
    }
}
