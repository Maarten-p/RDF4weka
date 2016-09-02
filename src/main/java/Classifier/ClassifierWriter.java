package Classifier;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;

import java.util.List;

class ClassifierWriter {

    void toNativeFile(AbstractClassifier classifier, List<Attribute> newHeader, List<String> oldHeader, String uuid) {
        try {
            weka.core.SerializationHelper.write(uuid + ".model", classifier);
            weka.core.SerializationHelper.write(uuid + "-oldheader.model", oldHeader);
            weka.core.SerializationHelper.write(uuid + "-newheader.model", newHeader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
