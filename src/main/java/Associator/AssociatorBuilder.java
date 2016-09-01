package Associator;

import Main.Algorithm;
import Main.Metadata;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import weka.associations.*;
import weka.core.Attribute;
import weka.core.BinarySparseInstance;
import weka.core.Instances;

import java.io.File;
import java.util.*;

public class AssociatorBuilder {

    private HashMap<String, Integer> stringToIntegerHash = new HashMap<>();
    private HashMap<Integer, String> integerToStringHash = new HashMap<>();
    private String newestUuid;

    private BinarySparseInstance stringToInstance(HashMap<String, Integer> someTable, String newLine) {
        String[] features = newLine.split(",");
        int[] indices = new int[features.length];
        for (int i = 0; i < features.length; i++) {
            indices[i] = someTable.get(features[i]);
        }
        return new BinarySparseInstance(1, indices, someTable.size());
    }

    private TupleQueryResult getSkills(RepositoryConnection conn) {
        String queryString = "prefix skosxl: <http://www.w3.org/2008/05/skos-xl#>\n" +
                "prefix esco: <http://data.europa.eu/esco/model#>\n" +
                "prefix mu: <http://mu.semte.ch/vocabularies/core/>\n" +
                "\n" +
                "select DISTINCT ?skillUuid  where {\n" +
                "  graph <http://localhost:8890/DAV> {\n" +
                "    ?s a esco:Occupation.\n" +
                "    ?relation esco:isRelationshipFor ?s.\n" +
                "    ?relation esco:refersConcept ?skill.\n" +
                "    ?skill skosxl:prefLabel / skosxl:literalForm ?skilllabel.\n" +
                "    ?skill mu:uuid ?skillUuid.\n" +
                "    FILTER ( lang(?skilllabel) = \"en\" )\n" +
                "  }\n" +
                "}";
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
        ArrayList<Attribute> atts = new ArrayList<>();
        List<String> tempBooleanValues = new ArrayList<>();
        tempBooleanValues.add("0");
        tempBooleanValues.add("1");
        for (String skill : skills) {
            atts.add(new Attribute(skill, tempBooleanValues));
        }
        return atts;
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
        String queryString = payload.getQuery();
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        try {
            TupleQueryResult result = tupleQuery.evaluate();
            List<String> bindingNames = result.getBindingNames();
            TupleQueryResult skills = getSkills(conn);
            ArrayList<String> skillsAsStrings = skillsToStrings(skills);
            HashMap<String, Integer> someTable = createStringToIntegerHash(skillsAsStrings);
            HashMap<Integer, String> intToStringTable = createIntegerToStringHash(skillsAsStrings);
            Instances instanceList = new Instances("theData", skillsToAttribute(skillsAsStrings), skillsAsStrings.size());

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String newLine = bindingSet.getValue(bindingNames.get(0)).stringValue();
                BinarySparseInstance newestInstance = stringToInstance(someTable, newLine);
                instanceList.add(newestInstance);
            }
            result.close();
            conn.close();

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
            AssociatorWriter modelwriter = new AssociatorWriter();
            if (payload.getMethod().equals("triplestore")) {
                modelwriter.RDFtoTripleStore(rules, repo, metadata, uuid.toString());
            } else if (payload.getMethod().equals("native")) {
                modelwriter.toNativeFile(associator, uuid.toString());
            } else {
                loadHashMapsFromTripleStore(repo);
                List<FakeAssociationRule> filteredRules = reduceRules(realToFakeRules(rules));
                List<StringFakeAssociationRule> stringRules = intToStringRules(filteredRules);
                modelwriter.toRDFFile(stringRules, metadata, uuid.toString());
            }
            return metadata;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void fileToTripleStore(String fileName, Repository repo) {

        ValueFactory factory = repo.getValueFactory();
        String location = "http://localhost:8890/DAV";
        IRI context = factory.createIRI(location);
        File file = new File(fileName);
        RepositoryConnection con = repo.getConnection();
        try {
            double startTime = System.currentTimeMillis();
            con.add(file, "", RDFFormat.TURTLE, context);
            double endTime = System.currentTimeMillis();
            System.out.println(endTime - startTime);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            con.close();
        }
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
        List<String> skillsAsStrings = skillsToStrings(getSkills(conn));
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
