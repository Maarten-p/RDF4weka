package Associator;

import Main.Metadata;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import weka.associations.AbstractAssociator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * A helper class that writes an AbstractAssociator to a file/triplestore
 */
class AssociatorWriter {

    /**
     * Writes an Associator to a file, using the given uuid as filename. This is the fastest method.
     *
     * @param someAssociator The model to write to file
     * @param uuid           The unique identifier of the model, which becomes the file name
     */
    void toNativeFile(AbstractAssociator someAssociator, String uuid) {
        try {
            weka.core.SerializationHelper.write("/data/" + uuid + ".model", someAssociator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes an Associator to a RDF file, using the given uuid as filename.
     * This is slower than a native file, but a RDF file can be used for other purposes.
     * Each statement is individually written to disk, he rules are NOT written to disk in batches, the code
     * should be made more straightforward using one loop.
     * @param rules
     * The rules to write to a RDF file
     * @param metadata
     * The metadata of the creation of the rules, which are also written to the file.
     * @param uuid
     * The identifier, which becomes the filename of the RDF file.
     */
    void toRDFFile(List<StringFakeAssociationRule> rules, Metadata metadata, String uuid) {

        File dataDir = new File("model.rdf");
        Repository repo = new SailRepository(new MemoryStore(dataDir));
        repo.initialize();
        ValueFactory factory = repo.getValueFactory();


        String ns = "http://mu.semte.ch/vocabularies/ext/weka-service/";
        String mu = "http://mu.semte.ch/vocabularies/core/";

        IRI UUID = factory.createIRI(mu, "uuid");
        IRI wekaService = factory.createIRI(ns, "wekaService/" + uuid);

        RDFWriter writer;
        try {
            FileOutputStream out = new FileOutputStream("/data/" + uuid + ".rdf");
            writer = Rio.createWriter(RDFFormat.TURTLE, out);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        writer.startRDF();
        writer.handleStatement(factory.createStatement(wekaService, UUID, factory.createLiteral(uuid)));

        IRI query = factory.createIRI(ns, "query");
        IRI runTime = factory.createIRI(ns, "runTime");
        IRI algorithm = factory.createIRI(ns, "algorithm");
        IRI totalTransactions = factory.createIRI(ns, "totalTransactions");
        IRI options = factory.createIRI(ns, "options");

        writer.handleStatement(factory.createStatement(wekaService, query, factory.createLiteral(metadata.getQuery())));
        writer.handleStatement(factory.createStatement(wekaService, runTime, factory.createLiteral(metadata.getRunTime())));
        writer.handleStatement(factory.createStatement(wekaService, algorithm, factory.createLiteral(metadata.getAlgorithm())));
        writer.handleStatement(factory.createStatement(wekaService, totalTransactions, factory.createLiteral(metadata.getTotalRows())));
        writer.handleStatement(factory.createStatement(wekaService, options, factory.createLiteral(metadata.getOptions())));

        IRI rule = factory.createIRI(ns, "rule");
        IRI from = factory.createIRI(ns, "from");
        IRI to = factory.createIRI(ns, "to");
        IRI premiseSupport = factory.createIRI(ns, "premiseSupport");
        IRI consequenceSupport = factory.createIRI(ns, "consequenceSupport");
        IRI totalSupport = factory.createIRI(ns, "totalSupport");
        int rulesPerWrite = 10000;
        Iterator<StringFakeAssociationRule> iterator = rules.iterator();
        for (int i = 0; i < Math.ceil(rules.size() * 1.0 / rulesPerWrite); i++) {
            int j = 0;
            System.out.println(i);
            while (iterator.hasNext() && j < rulesPerWrite) {
                StringFakeAssociationRule currentRule = iterator.next();
                com.eaio.uuid.UUID uuidRule = new com.eaio.uuid.UUID();
                IRI ruleInstance = factory.createIRI(ns, "rules/" + uuidRule.toString());
                writer.handleStatement(factory.createStatement(wekaService, rule, ruleInstance));
                writer.handleStatement(factory.createStatement(ruleInstance, UUID, factory.createLiteral(uuidRule.toString())));
                writer.handleStatement(factory.createStatement(ruleInstance, RDF.TYPE, rule));

                for (String premise : currentRule.getPremise()) {
                    writer.handleStatement(factory.createStatement(ruleInstance, from, factory.createLiteral(premise)));
                }
                for (String consequence : currentRule.getConsequence()) {
                    writer.handleStatement(factory.createStatement(ruleInstance, to, factory.createLiteral(consequence)));
                }
                writer.handleStatement(factory.createStatement(ruleInstance, premiseSupport, factory.createLiteral(currentRule.getPremiseSupport())));
                writer.handleStatement(factory.createStatement(ruleInstance, consequenceSupport, factory.createLiteral(currentRule.getConsequenceSupport())));
                writer.handleStatement(factory.createStatement(ruleInstance, totalSupport, factory.createLiteral(currentRule.getTotalSupport())));
                j += 1;
            }
        }
        writer.endRDF();

    }

    /**
     * Writes an Associator to a triplestore, using the given uuid in the name of the "root" node. This is slower than writing it to a file.
     * The rules are written in batches to make sure the query doesn't time out and to prevent memory problems.
     *
     * @param rules    The rules to write to the triplestore
     * @param repo     The repository to write to
     * @param metadata The metadata of the creation of the rules, which are also written to the file.
     * @param uuid     The identifier, which is used in the name of the "root" node.
     */
    void RDFtoTripleStore(List<StringFakeAssociationRule> rules, Repository repo, Metadata metadata, String uuid) {

        Model model = new LinkedHashModel();
        ValueFactory factory = repo.getValueFactory();
        String ns = "http://mu.semte.ch/vocabularies/ext/weka-service/";
        String mu = "http://mu.semte.ch/vocabularies/core/";

        IRI UUID = factory.createIRI(mu, "uuid");
        IRI wekaService = factory.createIRI(ns, "wekaService/" + uuid);
        model.add(wekaService, UUID, factory.createLiteral(uuid));

        IRI query = factory.createIRI(ns, "query");
        IRI runTime = factory.createIRI(ns, "runTime");
        IRI algorithm = factory.createIRI(ns, "algorithm");
        IRI totalTransactions = factory.createIRI(ns, "totalTransactions");
        IRI options = factory.createIRI(ns, "options");

        model.add(wekaService, query, factory.createLiteral(metadata.getQuery()));
        model.add(wekaService, runTime, factory.createLiteral(metadata.getRunTime()));
        model.add(wekaService, algorithm, factory.createLiteral(metadata.getAlgorithm()));
        model.add(wekaService, totalTransactions, factory.createLiteral(metadata.getTotalRows()));
        model.add(wekaService, options, factory.createLiteral(metadata.getOptions()));

        String location = System.getenv("GRAPH");
        IRI context = factory.createIRI(location);
        RepositoryConnection conn = repo.getConnection();
        conn.add(model, context);

        IRI rule = factory.createIRI(ns, "rule");
        IRI from = factory.createIRI(ns, "from");
        IRI to = factory.createIRI(ns, "to");
        IRI premiseSupport = factory.createIRI(ns, "premiseSupport");
        IRI consequenceSupport = factory.createIRI(ns, "consequenceSupport");
        IRI totalSupport = factory.createIRI(ns, "totalSupport");


        Iterator<StringFakeAssociationRule> iterator = rules.iterator();
        int rulesPerStep = 50;
        for (int i = 0; i < Math.ceil(rules.size() / ((double) rulesPerStep)); i++) {
            int j = 0;
            Model ruleModel = new LinkedHashModel();
            while (iterator.hasNext() && j < rulesPerStep) {
                StringFakeAssociationRule currentRule = iterator.next();
                com.eaio.uuid.UUID uuidRule = new com.eaio.uuid.UUID();
                IRI ruleInstance = factory.createIRI(ns, "rules/" + uuidRule.toString());
                ruleModel.add(wekaService, rule, ruleInstance);
                ruleModel.add(ruleInstance, UUID, factory.createLiteral(uuidRule.toString()));
                ruleModel.add(ruleInstance, RDF.TYPE, rule);

                for (String premise : currentRule.getPremise()) {
                    ruleModel.add(ruleInstance, from, factory.createLiteral(premise));
                }
                for (String consequence : currentRule.getConsequence()) {
                    ruleModel.add(ruleInstance, to, factory.createLiteral(consequence));
                }
                ruleModel.add(ruleInstance, premiseSupport, factory.createLiteral(currentRule.getPremiseSupport()));
                ruleModel.add(ruleInstance, consequenceSupport, factory.createLiteral(currentRule.getConsequenceSupport()));
                ruleModel.add(ruleInstance, totalSupport, factory.createLiteral(currentRule.getTotalSupport()));
                j += 1;
            }
            conn.add(ruleModel, context);
        }
        conn.close();
    }
}
