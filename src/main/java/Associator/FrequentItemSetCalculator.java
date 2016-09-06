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
     * Contains an UUID that either specifies the location of the rules in the triplestore or the file to load the rules from.
     * Used to check whether the requested model is already loaded into memory
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

    /**
     * Get all rules that contain one of the attributes given in the payload.
     *
     * @param payload Contains a list of attributes
     * @return All rules that contain one of the given attributes, as strings
     */
    public List<String> getAllRelatedRules(UseAssociatorPayload payload) {

        List<String> attributes = payload.getattributes();
        List<Integer> attributesAsInt = convertStringToInteger(attributes);
        List<String> relatedRules = new ArrayList<>();
        for (FakeAssociationRule loadedRule : loadedRules) {
            for (Integer testRule : attributesAsInt) {
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

        switch (payload.getMethod()) {
            case ("triplestore"):
                loadModelFromTripleStore(repo, payload);
                break;
            case ("native"):
                try {
                    loadModelFromNative(payload);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case ("RDFFile"):
                try {
                    loadModelFromRDFFile(payload);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Loads a model from a native .model file. The hashmaps are created in the convertItemToInteger function
     * @param payload
     * Contains the identifier by which to find the right file
     * @throws Exception
     * Could not read the file
     */
    private void loadModelFromNative(UseAssociatorPayload payload) throws Exception {
        AssociatorBuilder builder = new AssociatorBuilder();
        List<AssociationRule> rules = builder.getRules((AbstractAssociator) weka.core.SerializationHelper.read("/data/" + payload.getIdentifier() + ".model"));
        List<FakeAssociationRule> newRules = new ArrayList<>();
        for (AssociationRule rule : rules) {
            newRules.add(new FakeAssociationRule(
                    convertItemToInteger(rule.getPremise()),
                    convertItemToInteger(rule.getConsequence()), rule.getPremiseSupport(), rule.getConsequenceSupport(), rule.getTotalSupport()));
        }
        loadedRules = newRules;
    }

    /**
     * Converts a Weka Item to an integer. Also adds the string/integer to a int->string
     * and string->int hashmap, which is used to conserve memory.
     * @param list
     * The collection of items to change to integers
     * @return
     * A list of integers, the integers can be converted into strings by the hashmaps.
     */
    private List<Integer> convertItemToInteger(Collection<Item> list) {
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
     * Loads a model from a .RDF file. This is slower than loading from a native file and faster than loading from a triplestore.
     * It first loads the file into memory and uses other methods to extract the rules and hashmaps.
     *
     * @param payload
     * Contains the identifier of the rdf file.
     * @throws Exception
     * Could not read the rdf file
     */
    private void loadModelFromRDFFile(UseAssociatorPayload payload) throws Exception {
        File dataDir = new File("/data/" + payload.getIdentifier() + ".rdf");
        Repository repo = new SailRepository(new MemoryStore());
        repo.initialize();
        ValueFactory factory = repo.getValueFactory();

        // There might be a better way to do this
        RepositoryConnection con = repo.getConnection();
        con.add(dataDir, "", RDFFormat.TURTLE);
        RepositoryResult<Statement> statements = con.getStatements(null, null, null);
        Model model = QueryResults.asModel(statements);

        String ns = "http://mu.semte.ch/vocabularies/ext/weka-service/";
        IRI from = factory.createIRI(ns, "from");
        IRI to = factory.createIRI(ns, "to");

        attributesToHash(model, from, false);
        attributesToHash(model, to, true);
        loadRulesFromRDF(payload, repo);
    }

    /**
     * Builds the hashmaps from the model (which is stored in memory)
     * @param model
     * The model, stored in memory
     * @param filter
     * The IRI on which to filter to extract the right attributes
     * @param checkIfContains
     * false if this is the first time the hashmaps are build, true if this is an expansion of the hashmaps
     */
    private void attributesToHash(Model model, IRI filter, boolean checkIfContains) {
        Iterator<Value> iterator = model.filter(null, filter, null).objects().iterator();
        int i = checkIfContains ? stringToIntegerHash.size() : 0;
        while (iterator.hasNext()) {
            String nextattribute = iterator.next().stringValue();
            if (!checkIfContains || !stringToIntegerHash.containsKey(nextattribute)) {
                stringToIntegerHash.put(nextattribute, i);
                integerToStringHash.put(i, nextattribute);
                i += 1;
            }
        }
    }

    /**
     * Loads the model from a triplestore. The slowest method.
     * @param repo
     * The repository where the triplestore is located
     * @param payload
     * Contains the identifier of the rules in the triplestore
     */
    private void loadModelFromTripleStore(Repository repo, UseAssociatorPayload payload) {
        loadHashMapsFromTripleStore(repo);
        loadRulesFromRDF(payload, repo);
    }

    /**
     * Builds the hashmaps by quering all the attributes from the triplestore and going over them
     * @param repo
     * The repository where the triplestore is located
     */
    private void loadHashMapsFromTripleStore(Repository repo) {
        RepositoryConnection conn = repo.getConnection();
        List<String> attributesAsStrings = attributesToStrings(getAttributes(conn));
        stringToIntegerHash = createStringToIntegerHash(attributesAsStrings);
        integerToStringHash = createIntegerToStringHash(attributesAsStrings);
        conn.close();
    }

    /**
     * Loads the rules from an RDF repository, can be either a triplestore or a file
     * @param payload
     * Contains the identifier of the rules
     * @param repo
     * The repository from which to load the rules, can be either a triplestore or a rdf file
     */
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the attributes from a repositoryconnection. The query is stored as a environmental variable. (which is set by the dockerfile)
     * @param conn
     * The connection from which to get the attributes
     * @return
     * The attributes as a TupleQueryResult
     */
    private TupleQueryResult getAttributes(RepositoryConnection conn) {
        String queryString = System.getenv("ATTRIBUTES_QUERY");
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        return tupleQuery.evaluate();
    }

    private ArrayList<String> attributesToStrings(TupleQueryResult attributes) {
        ArrayList<String> strings = new ArrayList<>();
        List<String> bindingNames = attributes.getBindingNames();
        while (attributes.hasNext()) {
            BindingSet bindingSet = attributes.next();
            String string = bindingSet.getValue(bindingNames.get(0)).stringValue();
            strings.add(string);
        }
        return strings;
    }

    private HashMap<String, Integer> createStringToIntegerHash(List<String> attributes) {
        HashMap<String, Integer> someTable = new HashMap<>();
        for (int i = 0; i < attributes.size(); i++) {
            someTable.put(attributes.get(i), i);
        }
        return someTable;
    }

    private HashMap<Integer, String> createIntegerToStringHash(List<String> attributes) {
        HashMap<Integer, String> someTable = new HashMap<>();
        for (int i = 0; i < attributes.size(); i++) {
            someTable.put(i, attributes.get(i));
        }
        return someTable;
    }

    /**
     * Finds all frequent items of a given list of attributes. The attributes are first converted to
     * ints to save memory and since comparision of Integers is much faster.
     * For each rule, which is loaded in memory, it is checked if the given list of attributes contains the premise.
     * If yes all the consequences that are not yet contained in the resulting list are added to it.
     * Finally the list is sorted, duplicates are removed and it is returned.
     * @param payload
     * Contains the attributes for which to calculate the frequent items.
     * @return
     * The list of frequent items, sorted on confidence
     */
    public List<FrequentItem> getFrequentItems(UseAssociatorPayload payload) {

        List<String> attributes = payload.getattributes();
        List<Integer> attributesAsInt = convertStringToInteger(attributes);
        List<FrequentItem> scores = new ArrayList<>();
        for (FakeAssociationRule rule : loadedRules) {
            if (attributesAsInt.containsAll(rule.getPremise())) {
                for (Integer consequence : rule.getConsequence()) {
                    if (!attributesAsInt.contains(consequence)) {
                        scores.add(new FrequentItem(rule.getTotalSupport(), integerToStringHash.get(consequence)));
                    }
                }
            }
        }
        Collections.sort(scores);
        return new ArrayList<>(new LinkedHashSet<>(scores));
    }


    private List<Integer> convertStringToInteger(List<String> attributes) {
        List<Integer> attributesAsInteger = new ArrayList<>();
        for (String attribute : attributes) {
            attributesAsInteger.add(stringToIntegerHash.get(attribute));
        }
        return attributesAsInteger;
    }
}
