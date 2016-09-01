package Main;

import Associator.AssociatorBuilder;
import Associator.BuildModelPayload;
import Associator.FrequentItemSetCalculator;
import Associator.UseAssociatorPayload;
import Classifier.RandomTreeBuilder;
import Classifier.RandomTreeClassifier;
import Classifier.UseClassifierPayload;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import java.util.Objects;

import static spark.Spark.get;
import static spark.Spark.post;

public class Main {

    /**
     * The Associator.FrequentItemSetCalculator that is used to determine the frequent item set of the given strings
     */
    private static FrequentItemSetCalculator itemSetCalculator = null;
    private static RandomTreeClassifier randomTreeClassifier = null;

    public static void main(String[] args) {

        String sparqlEndpoint = "http://localhost:8890/sparql";
        SPARQLRepository repo = new SPARQLRepository(sparqlEndpoint);
        repo.initialize();

        /*
         * A request to build a list of AssociationRules from a list of lists of elements. The JSON payload consists of a query that retrieves the data,
         * the algorithm that weka has to use on the data, the options given to the algorithm and a flag
         * 'writeToTriplestore' that decides if the resulting list is queried to the database or written to a file.
         * Default values will be inserted if some of the values of the payload are null. A message is returned when the task is finished.
         */
        post("/build_model", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            response.header("Content-Type:", "application/vnd.api+json");
            if (!request.headers("Content-Type").equals("application/vnd.api+json")) {
                response.status(415);
                return "";
            }
            if (!request.headers("Accept").equals("application/vnd.api+json")) {
                response.status(406);
                return "";
            }
            try {
                BuildModelPayload payload = mapper.readValue(request.body(), BuildModelPayload.class);
                payload.insertDefaults();
                if (!payload.isValid()) {
                    response.status(400);
                    return mapper.writeValueAsString("This is not a valid payload");
                }
                response.status(201);
                response.type("application/json");
                AssociatorBuilder builder = new AssociatorBuilder();
                Metadata metadata = builder.buildModel(repo, payload);
                String uuid = builder.getNewestUuid();
                ObjectNode objectNode1 = mapper.createObjectNode();
                ObjectNode objectNode2 = mapper.createObjectNode();
                objectNode1.replace("data", objectNode2);
                objectNode2.put("id", uuid);
                objectNode2.put("metadata", mapper.writeValueAsString(metadata));
                return objectNode1.toString();
            } catch (JsonParseException | JsonMappingException f) {
                response.status(400);
                f.printStackTrace();
                return mapper.writeValueAsString("This is not a valid JSON input");
            }
        });

        post("/build-tree", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();

            response.header("Content-Type:", "application/vnd.api+json");
            if (!request.headers("Content-Type").equals("application/vnd.api+json")) {
                response.status(415);
                return "";
            }
            if (!request.headers("Accept").equals("application/vnd.api+json")) {
                response.status(406);
                return "";
            }
            try {
                BuildModelPayload payload = mapper.readValue(request.body(), BuildModelPayload.class);
                payload.insertDefaults();
                if (!payload.isValid()) {
                    response.status(400);
                    return writer.writeValueAsString("This is not a valid payload");
                }
                response.status(201);
                response.type("application/json");
                RandomTreeBuilder builder = new RandomTreeBuilder();
                Metadata metadata = builder.buildModel(repo, payload);
                String uuid = builder.getNewestUuid();
                ObjectNode objectNode1 = mapper.createObjectNode();
                ObjectNode objectNode2 = mapper.createObjectNode();
                ObjectNode objectNode3 = mapper.createObjectNode();
                JsonNode node = mapper.valueToTree(metadata);
                objectNode1.replace("data", objectNode2);
                objectNode2.put("id", uuid);
                objectNode2.replace("attributes", objectNode3);
                objectNode3.replace("metadata", node);

                return objectNode1.toString();
            } catch (JsonParseException | JsonMappingException f) {
                response.status(400);
                f.printStackTrace();
                return mapper.writeValueAsString("This is not a valid JSON input");
            }
        });

        /*
         * A request to determine the frequent itemset of a given list of strings. The JSON payload consists of a list of
         * strings on which to apply the AssociationRules, the algorithm that created the AssociationRules, a flag
         * 'readFromTriplestore' that decides if the AssociationRules are read from a file or a triplestore and an
         * identifier which is either a UUID that identifies the set of rules in the triplestore or the location of
         * the file to read. Default values will be inserted if some of the values of the payload are null.
         * If the identifier is not equal to the identifier of the last request, the rules are loaded from the file or
         * triplestore. They are then stored in memory for the next request.
         */
        post("/input", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            response.header("Content-Type:", "application/vnd.api+json");
            if (!request.headers("Content-Type").equals("application/vnd.api+json")) {
                response.status(415);
                return "";
            }
            if (!request.headers("Accept").equals("application/vnd.api+json")) {
                response.status(406);
                return "";
            }
            if (itemSetCalculator == null)
                itemSetCalculator = new FrequentItemSetCalculator();
            try {
                UseAssociatorPayload payload = mapper.readValue(request.body(), UseAssociatorPayload.class);
                payload.insertDefaults();
                response.status(200);
                response.type("application/json");
                if (Objects.equals(payload.getIdentifier(), itemSetCalculator.getIdentifier())) {
                    return mapper.writeValueAsString(itemSetCalculator.getFrequentItems(payload));
                } else {
                    try {
                        itemSetCalculator.loadModel(repo, payload);
                        itemSetCalculator.setIdentifier(payload.getIdentifier());
                        return mapper.writeValueAsString(itemSetCalculator.getFrequentItems(payload));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        return mapper.writeValueAsString("This is not a valid algorithm");
                    }
                }
            } catch (JsonParseException | JsonMappingException f) {
                response.status(400);
                f.printStackTrace();
                return mapper.writeValueAsString("This is not a valid JSON input");
            }
        });

        post("/classify", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
            response.header("Content-Type:", "application/vnd.api+json");
            if (!request.headers("Content-Type").equals("application/vnd.api+json")) {
                response.status(415);
                return "";
            }
            if (!request.headers("Accept").equals("application/vnd.api+json")) {
                response.status(406);
                return "";
            }
            if (randomTreeClassifier == null)
                randomTreeClassifier = new RandomTreeClassifier();
            try {
                UseClassifierPayload payload = mapper.readValue(request.body(), UseClassifierPayload.class);
                payload.insertDefaults();
                response.status(200);
                response.type("application/json");
                if (Objects.equals(payload.getIdentifier(), randomTreeClassifier.getIdentifier())) {
                    return writer.writeValueAsString(randomTreeClassifier.classifyString(payload.getToClassify()));
                } else {
                    try {
                        randomTreeClassifier.loadModelFromNative(payload);
                        randomTreeClassifier.setIdentifier(payload.getIdentifier());
                        return writer.writeValueAsString(randomTreeClassifier.classifyString(payload.getToClassify()));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        return mapper.writeValueAsString("This is not a valid algorithm");
                    }
                }
            } catch (JsonParseException | JsonMappingException f) {
                response.status(400);
                f.printStackTrace();
                return mapper.writeValueAsString("This is not a valid JSON input");
            }
        });

        /*
         * A request to analyse the rules that are currently loaded into memory.
         * Returns an string which describes the amount of rules of each length that are currently loaded into memory.
         * The first element is the amount of rules of length 1, and so on.
         */
        get("/analyse", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            response.header("Content-Type:", "application/vnd.api+json");
            if (!request.headers("Content-Type").equals("application/vnd.api+json")) {
                response.status(415);
                return "";
            }
            if (!request.headers("Accept").equals("application/vnd.api+json")) {
                response.status(406);
                return "";
            }
            if (itemSetCalculator == null || itemSetCalculator.getIdentifier() == null) {
                return mapper.writeValueAsString("There is no dataset to analyse yet");
            }
            return mapper.writeValueAsString(itemSetCalculator.analyse());
        });

        post("/relatedrules", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            response.header("Content-Type:", "application/vnd.api+json");
            if (!request.headers("Content-Type").equals("application/vnd.api+json")) {
                response.status(415);
                return "";
            }
            if (!request.headers("Accept").equals("application/vnd.api+json")) {
                response.status(406);
                return "";
            }
            if (itemSetCalculator == null || itemSetCalculator.getIdentifier() == null) {
                return mapper.writeValueAsString("There is no dataset to analyse yet");
            }
            try {
                UseAssociatorPayload payload = mapper.readValue(request.body(), UseAssociatorPayload.class);
                payload.insertDefaults();
                response.status(200);
                response.type("application/json");
                return mapper.writeValueAsString(itemSetCalculator.getAllRelatedRules(payload));
            } catch (JsonParseException | JsonMappingException f) {
                response.status(400);
                f.printStackTrace();
                return mapper.writeValueAsString("This is not a valid JSON input");
            }

        });

    }


}