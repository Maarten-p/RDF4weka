package Classifier;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;

import java.util.List;

/**
 * A class for writing AbstractClassifiers to file.
 */
class ClassifierWriter {

    /**
     * Writes the AbstractClassifier to a .model file, which is the native format for weka.
     * This is faster and uses less disk space than converting to RDF, but it can only be read by weka.
     * The headers need to be written to disk too since they are not included in the .model but needed to use the classifier.
     * @param classifier
     * The AbstractClassifier to write to disk.
     * @param newHeader
     * The parsed header, using ClassifierParser
     * @param oldHeader
     * The pre-parsed header, as originally received from the query.
     * @param uuid
     * The unique identifier, which is used in the name of the files.
     */
    void toNativeFile(AbstractClassifier classifier, List<Attribute> newHeader, List<String> oldHeader, String uuid) {
        try {
            weka.core.SerializationHelper.write("/data/" + uuid + ".model", classifier);
            weka.core.SerializationHelper.write("/data/" + uuid + "-oldheader.model", oldHeader);
            weka.core.SerializationHelper.write("/data/" + uuid + "-newheader.model", newHeader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
