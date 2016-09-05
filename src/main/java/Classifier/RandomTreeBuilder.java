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

public class RandomTreeBuilder {

    private String newestUuid;

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
            System.out.println(test.toSummaryString());
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
