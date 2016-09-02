package Classifier;

import java.util.List;

public class ClassifierParser {


    static Double[] parse(String varName, String value) {
        return new Double[]{Double.parseDouble(value)};
    }

    static List<String> transformHeader(List<String> headers) {
        return headers;
    }

}
