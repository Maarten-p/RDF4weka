package Associator;

import Main.Algorithm;
import Main.Metadata;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import weka.associations.*;
import weka.core.Attribute;
import weka.core.BinarySparseInstance;
import weka.core.Instances;

import java.util.*;

public class AssociatorBuilder {

    /**
     * These hashmaps are used to save memory and cpu cycles. When loading the AssociationRules each unique UUID is
     * mapped to an integer. All calculations are done on these integers. When a result needs to be returned the
     * integers are converted back into strings.
     */
    private HashMap<String, Integer> stringToIntegerHash = new HashMap<>();
    private HashMap<Integer, String> integerToStringHash = new HashMap<>();

    /**
     * The UUID of the last build model so that it can be given to the user.
     */
    private String newestUuid;

    /**
     * Converts a string, consisting of comma-separated uuid strings, into an instance that can be used by weka. It uses
     * a hashmap to convert the strings into a sparse vector.
     *
     * @param stringToSparseHash The hashmap which converts each uuid string into the appropriate indice in the sparse vector
     * @param newLine            A string consisting of comma-separated uuid strings which represent a row in the data
     * @return A new instance for weka
     */
    private BinarySparseInstance stringToInstance(HashMap<String, Integer> stringToSparseHash, String newLine) {
        String[] features = newLine.split(",");
        int[] indices = new int[features.length];
        for (int i = 0; i < features.length; i++) {
            indices[i] = stringToSparseHash.get(features[i]);
        }
        return new BinarySparseInstance(1, indices, stringToSparseHash.size());
    }


    private TupleQueryResult getAttributes(RepositoryConnection conn) {
        String queryString = System.getenv("ATTRIBUTES_QUERY");
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        return tupleQuery.evaluate();
    }

    private ArrayList<String> skillsToStrings(TupleQueryResult skills) {
        ArrayList<String> strings = new ArrayList<>();
        List<String> bindingNames = skills.getBindingNames();
        while (skills.hasNext()) {
            BindingSet bindingSet = skills.next();
            String string = bindingSet.getValue(bindingNames.get(0)).stringValue();
            strings.add(string);
        }
        return strings;
    }

    private ArrayList<Attribute> skillsToAttribute(List<String> skills) {
        ArrayList<Attribute> attrs = new ArrayList<>();
        List<String> tempBooleanValues = new ArrayList<>();
        tempBooleanValues.add("0");
        tempBooleanValues.add("1");
        for (String skill : skills) {
            attrs.add(new Attribute(skill, tempBooleanValues));
        }
        return attrs;
    }

    private HashMap<String, Integer> createStringToIntegerHash(List<String> skills) {
        HashMap<String, Integer> someTable = new HashMap<>();
        for (int i = 0; i < skills.size(); i++) {
            someTable.put(skills.get(i), i);
        }
        return someTable;
    }

    private HashMap<Integer, String> createIntegerToStringHash(List<String> skills) {
        HashMap<Integer, String> someTable = new HashMap<>();
        for (int i = 0; i < skills.size(); i++) {
            someTable.put(i, skills.get(i));
        }
        return someTable;
    }

    public Metadata buildModel(Repository repo, BuildModelPayload payload) throws IllegalArgumentException {
        RepositoryConnection conn = repo.getConnection();
        String queryString = System.getenv("ASSOCIATOR_DATA_QUERY");
        System.out.println(System.getenv("ASSOCIATOR_DATA_QUERY"));
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        try {
            TupleQueryResult result = tupleQuery.evaluate();
            List<String> bindingNames = result.getBindingNames();
            TupleQueryResult skills = getAttributes(conn);
            ArrayList<String> skillsAsStrings = skillsToStrings(skills);
            HashMap<String, Integer> DenseToSparseHash = createStringToIntegerHash(skillsAsStrings);
            Instances instanceList = new Instances("theData", skillsToAttribute(skillsAsStrings), skillsAsStrings.size());

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String newLine = bindingSet.getValue(bindingNames.get(0)).stringValue();
                BinarySparseInstance newestInstance = stringToInstance(DenseToSparseHash, newLine);
                instanceList.add(newestInstance);
            }
            result.close();
            conn.close();

            System.out.println(instanceList.size());
            Algorithm algorithm = payload.getAlgorithm();
            AbstractAssociator associator;
            switch (algorithm) {
                case APRIORI: {
                    associator = new Apriori();
                    break;

                }
                case FPGROWTH: {
                    associator = new FPGrowth();
                    break;
                }
                default:
                    throw new IllegalArgumentException();
            }
            String[] options = payload.getOptions();
            associator.setOptions(options);
            long startTime = System.currentTimeMillis();
            associator.buildAssociations(instanceList);
            long endTime = System.currentTimeMillis();
            long runtime = endTime - startTime;
            String concatenatedOptions = "";
            for (String optionValue : options) {
                concatenatedOptions += optionValue;
            }
            List<AssociationRule> rules = getRules(associator);

            Metadata metadata = new Metadata(runtime, queryString, algorithm.toString(), rules.size(), concatenatedOptions);
            com.eaio.uuid.UUID uuid = new com.eaio.uuid.UUID();
            setNewestUuid(uuid.toString());
            AssociatorWriter modelWriter = new AssociatorWriter();
            switch (payload.getMethod()) {
                case ("triplestore"):
                    modelWriter.RDFtoTripleStore(rules, repo, metadata, uuid.toString());
                    break;
                case ("native"):
                    modelWriter.toNativeFile(associator, uuid.toString());
                    break;
                case ("RDFFile"):
                    loadHashMapsFromTripleStore(repo);
                    List<FakeAssociationRule> filteredRules = reduceRules(realToFakeRules(rules));
                    List<StringFakeAssociationRule> stringRules = intToStringRules(filteredRules);
                    modelWriter.toRDFFile(stringRules, metadata, uuid.toString());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            return metadata;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    List<AssociationRule> getRules(AbstractAssociator associatior) throws IllegalArgumentException {
        if (associatior instanceof FPGrowth) {
            FPGrowth subClass = (FPGrowth) associatior;
            return subClass.getAssociationRules().getRules();
        } else if (associatior instanceof Apriori) {
            Apriori subClass = (Apriori) associatior;
            return subClass.getAssociationRules().getRules();
        } else
            throw new IllegalArgumentException();

    }

    private void loadHashMapsFromTripleStore(Repository repo) {
        RepositoryConnection conn = repo.getConnection();
        List<String> skillsAsStrings = skillsToStrings(getAttributes(conn));
        stringToIntegerHash = createStringToIntegerHash(skillsAsStrings);
        integerToStringHash = createIntegerToStringHash(skillsAsStrings);
        conn.close();
    }

    private List<FakeAssociationRule> realToFakeRules(List<AssociationRule> rules) {
        List<FakeAssociationRule> newRules = new ArrayList<>();
        for (AssociationRule rule : rules) {
            List<Integer> premiseAsIntegers = new ArrayList<>();
            for (Item premise : rule.getPremise()) {
                Integer convertedValue = stringToIntegerHash.get(premise.toString().split("=")[0]);
                premiseAsIntegers.add(convertedValue);
            }
            List<Integer> consequenceAsIntegers = new ArrayList<>();
            for (Item consequence : rule.getConsequence()) {
                Integer convertedValue = stringToIntegerHash.get(consequence.toString().split("=")[0]);
                consequenceAsIntegers.add(convertedValue);
            }
            FakeAssociationRule newRule = new FakeAssociationRule(premiseAsIntegers, consequenceAsIntegers,
                    rule.getPremiseSupport(), rule.getConsequenceSupport(), rule.getTotalSupport());
            newRules.add(newRule);
        }
        return newRules;
    }

    private List<StringFakeAssociationRule> intToStringRules(List<FakeAssociationRule> rules) {
        List<StringFakeAssociationRule> newRules = new ArrayList<>();
        for (FakeAssociationRule rule : rules) {
            List<String> premiseAsStrings = new ArrayList<>();
            for (Integer premise : rule.getPremise()) {
                String convertedValue = integerToStringHash.get(premise);
                premiseAsStrings.add(convertedValue);
            }
            List<String> consequenceAsStrings = new ArrayList<>();
            for (Integer consequence : rule.getConsequence()) {
                String convertedValue = integerToStringHash.get(consequence);
                consequenceAsStrings.add(convertedValue);
            }
            StringFakeAssociationRule newRule = new StringFakeAssociationRule(premiseAsStrings, consequenceAsStrings,
                    rule.getPremiseSupport(), rule.getConsequenceSupport(), rule.getTotalSupport());
            newRules.add(newRule);
        }
        return newRules;
    }

    private List<FakeAssociationRule> reduceRules(List<FakeAssociationRule> oldRules) {

        HashMap<List<Integer>, List<Integer>> someTable = new HashMap<>();
        for (int i = 0; i < oldRules.size(); i++) {
            List<Integer> currentPremise = oldRules.get(i).getPremise();
            if (someTable.containsKey(currentPremise)) {
                someTable.get(currentPremise).add(i);
            } else {
                List<Integer> newList = new ArrayList<>();
                newList.add(i);
                someTable.put(currentPremise, newList);
            }
        }
        List<FakeAssociationRule> newRules = new ArrayList<>();
        someTable.forEach((k, v) -> {
            Set<Integer> newConsequence = new HashSet<>();
            int minConSupport = Integer.MAX_VALUE;
            int minTotSupport = Integer.MAX_VALUE;
            for (int index : v) {
                FakeAssociationRule currentRule = oldRules.get(index);
                if (currentRule.getConsequenceSupport() < minConSupport)
                    minConSupport = currentRule.getConsequenceSupport();
                if (currentRule.getTotalSupport() < minTotSupport)
                    minTotSupport = currentRule.getTotalSupport();
                List<Integer> consequenceToAdd = oldRules.get(index).getConsequence();
                for (Integer consequence : consequenceToAdd) {
                    newConsequence.add(consequence);
                }
            }
            List<Integer> newConsequenceList = new ArrayList<>();
            newConsequenceList.addAll(newConsequence);
            newRules.add(new FakeAssociationRule(k, newConsequenceList, oldRules.get(v.get(0)).getPremiseSupport(), minConSupport, minTotSupport));
        });
        return newRules;
    }

    public String getNewestUuid() {
        return newestUuid;
    }

    private void setNewestUuid(String newestUuid) {
        this.newestUuid = newestUuid;
    }
}
