package Associator;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import weka.associations.AbstractAssociator;
import weka.associations.AssociationRule;
import weka.associations.Item;

import java.io.File;
import java.util.*;

/**
 * This class loads a list of AssociationRules from a file or a triplestore. It can then utilize those rules
 * on a given list of strings to determine the frequent items.
 */
public class FrequentItemSetCalculator {

    /**
     * The loaded rules. FakeAssociationRules are used to save memory and to facilitate calculating the frequent items.
     * They utilize integers to store the premise and consequence instead of strings.
     */
    private List<FakeAssociationRule> loadedRules = new ArrayList<>();
    /**
     * Contains either an UUID that specifies the location of the rules in the triplestore or a file to load the rules from.
     */
    private String identifier = null;
    /**
     * This HashMap is used to convert AssocationRules into FakeAssociationRules to save memory and to facilitate calculating the frequent items.
     */
    private HashMap<String, Integer> stringToIntegerHash = new HashMap<>();
    /**
     * This HashMap is used to convert the found frequent items back into strings that represent their UUID
     */
    private HashMap<Integer, String> integerToStringHash = new HashMap<>();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns an string which describes the amount of rules of each length that are currently loaded into memory.
     * The first element is the amount of rules of length 1, and so on.
     */
    public String analyse() {
        List<Integer> lengthOfRules = new ArrayList<>(Collections.nCopies(60, 0));
        for (FakeAssociationRule rule : loadedRules) {
            int premise = rule.getPremise().size() + rule.getConsequence().size();
            lengthOfRules.set(premise - 1, lengthOfRules.get(premise - 1) + 1);
        }
        return lengthOfRules.toString();
    }

    public List<String> getAllRelatedRules(UseAssociatorPayload payload) {

        List<String> skills = payload.getSkills();
        List<Integer> skillsAsInt = convertStringToInteger(skills);
        List<String> relatedRules = new ArrayList<>();
        for (FakeAssociationRule loadedRule : loadedRules) {
            for (Integer testRule : skillsAsInt) {
                if (loadedRule.getPremise().contains(testRule) || loadedRule.getConsequence().contains(testRule)) {
                    relatedRules.add(loadedRule.toString());
                    break;
                }
            }
        }
        return relatedRules;
    }

    /**
     * Loads the model into two global HashMaps and a global list of FakeAssociationRules. Can load model either from
     * a file or from a triplestore, and can load model either from the APriori algorithm or the FPGrowth algorithm.
     *
     * @param repo    The repository to load the rules from, if they are loaded from the triplestore
     * @param payload Is either a FromTripleStoreInputPayload if model is loaded from triplestore or a FromFileInputPayload if model is loaded from file.
     */
    public void loadModel(Repository repo, UseAssociatorPayload payload) {

        if (payload.getMethod().equals("triplestore")) {
            loadModelFromTripleStore(repo, payload);
        } else if (payload.getMethod().equals("native")) {
            try {
                loadModelFromNative(payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                loadModelFromRDFFile(payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    private void loadModelFromNative(UseAssociatorPayload payload) throws Exception {
        AssociatorBuilder builder = new AssociatorBuilder();
        List<AssociationRule> rules = builder.getRules((AbstractAssociator) weka.core.SerializationHelper.read(payload.getIdentifier() + ".model"));
        List<FakeAssociationRule> newRules = new ArrayList<>();
        for (AssociationRule rule : rules) {
            newRules.add(new FakeAssociationRule(
                    convertItemToString(rule.getPremise()),
                    convertItemToString(rule.getConsequence()), rule.getPremiseSupport(), rule.getConsequenceSupport(), rule.getTotalSupport()));
        }
        loadedRules = newRules;
    }

    private List<Integer> convertItemToString(Collection<Item> list) {
        List<Integer> convertedList = new ArrayList<>();
        for (Item item : list) {
            String string = item.toString().split("=")[0];
            if (stringToIntegerHash.containsKey(string))
                convertedList.add(stringToIntegerHash.get(string));
            else {
                int size = stringToIntegerHash.size();
                stringToIntegerHash.put(string, size);
                integerToStringHash.put(size, string);
                convertedList.add(size);
            }
        }
        return convertedList;
    }

    /**
     * @param payload
     * @throws Exception
     */
    private void loadModelFromRDFFile(UseAssociatorPayload payload) throws Exception {
        File dataDir = new File(payload.getIdentifier() + ".rdf");
        Repository repo = new SailRepository(new MemoryStore());
        repo.initialize();
        ValueFactory factory = repo.getValueFactory();

        RepositoryConnection con = repo.getConnection();
        con.add(dataDir, "", RDFFormat.TURTLE);
        RepositoryResult<Statement> statements = con.getStatements(null, null, null);
        Model model = QueryResults.asModel(statements);

        String ns = "http://mu.semte.ch/vocabularies/ext/weka-service/";
        IRI from = factory.createIRI(ns, "from");
        IRI to = factory.createIRI(ns, "to");

        skillsToHash(model, from, false);
        skillsToHash(model, to, true);
        loadRulesFromRDF(payload, repo);
    }

    private void skillsToHash(Model model, IRI filter, boolean checkIfContains) {
        Iterator<Value> iterator = model.filter(null, filter, null).objects().iterator();
        int i = checkIfContains ? stringToIntegerHash.size() : 0;
        while (iterator.hasNext()) {
            String nextSkill = iterator.next().stringValue();
            if (!checkIfContains || !stringToIntegerHash.containsKey(nextSkill)) {
                stringToIntegerHash.put(nextSkill, i);
                integerToStringHash.put(i, nextSkill);
                i += 1;
            }
        }
    }

    private void loadModelFromTripleStore(Repository repo, UseAssociatorPayload payload) {
        loadHashMapsFromTripleStore(repo);
        loadRulesFromRDF(payload, repo);
    }

    private void loadHashMapsFromTripleStore(Repository repo) {
        RepositoryConnection conn = repo.getConnection();
        List<String> skillsAsStrings = skillsToStrings(getSkills(conn));
        stringToIntegerHash = createStringToIntegerHash(skillsAsStrings);
        integerToStringHash = createIntegerToStringHash(skillsAsStrings);
        conn.close();
    }

    private void loadRulesFromRDF(UseAssociatorPayload payload, Repository repo) {
        RepositoryConnection conn = repo.getConnection();
        String runUuid = payload.getIdentifier();
        String queryString = "prefix ns: <http://mu.semte.ch/vocabularies/ext/weka-service/>\n" +
                "prefix weka: <http://mu.semte.ch/vocabularies/ext/weka-service/wekaService/>\n" +
                "select ?o (group_concat(distinct ?fr; separator=\",\") as ?from) (group_concat(distinct ?to; separator=\",\") as ?to) ?mettype ?presup ?consup ?totsup " +
                "where " +
                "{weka:" + runUuid + " ns:rule ?o.\n" +
                "?o ns:from ?fr.\n" +
                "?o ns:to ?to.\n" +
                "?o ns:premiseSupport ?presup.\n" +
                "?o ns:consequenceSupport ?consup.\n" +
                "?o ns:totalSupport ?totsup }\n" +
                "GROUP BY ?o ?mettype ?presup ?consup ?totsup";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        List<FakeAssociationRule> rules = new ArrayList<>();
        try {
            TupleQueryResult result = tupleQuery.evaluate();
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String from = bindingSet.getValue("from").stringValue();
                String to = bindingSet.getValue("to").stringValue();
                String presup = bindingSet.getValue("presup").stringValue();
                String consup = bindingSet.getValue("consup").stringValue();
                String totsup = bindingSet.getValue("totsup").stringValue();
                String[] fromList = from.split(",");
                String[] toList = to.split(",");
                List<Integer> premiseAsIntegers = new ArrayList<>();
                for (String premise : fromList) {
                    Integer convertedValue = stringToIntegerHash.get(premise);
                    premiseAsIntegers.add(convertedValue);
                }
                List<Integer> consequenceAsIntegers = new ArrayList<>();
                for (String consequence : toList) {
                    Integer convertedValue = stringToIntegerHash.get(consequence);
                    consequenceAsIntegers.add(convertedValue);
                }
                FakeAssociationRule newRule = new FakeAssociationRule(premiseAsIntegers, consequenceAsIntegers,
                        Integer.valueOf(presup), Integer.valueOf(consup), Integer.valueOf(totsup));
                rules.add(newRule);
            }
            result.close();
            conn.close();
            loadedRules = rules;
            System.out.println(loadedRules.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public List<FrequentItem> getFrequentItems(UseAssociatorPayload payload) {

        List<String> skills = payload.getSkills();
        List<Integer> skillsAsInt = convertStringToInteger(skills);
        List<FrequentItem> scores = new ArrayList<>();
        for (FakeAssociationRule rule : loadedRules) {
            if (skillsAsInt.containsAll(rule.getPremise())) {
                for (Integer consequence : rule.getConsequence()) {
                    if (!skillsAsInt.contains(consequence)) {
                        scores.add(new FrequentItem(rule.getTotalSupport(), integerToStringHash.get(consequence)));
                    }
                }
            }
        }
        Collections.sort(scores);
        return new ArrayList<>(new LinkedHashSet<>(scores));
    }


    private List<Integer> convertStringToInteger(List<String> skills) {
        List<Integer> skillsAsInteger = new ArrayList<>();
        for (String skill : skills) {
            skillsAsInteger.add(stringToIntegerHash.get(skill));
        }
        return skillsAsInteger;
    }
}
