package Classifier;

import java.util.List;

/**
 * A class that parses the values received from the query to values that will be used by the classifier.
 * The functions can be expanded at will, just run startup.sh after making your changes or run the docker without command.
 */
public class ClassifierParser {

    /**
     * Parses individual values to the values that will be received by the algorithm. Each value can be mapped to an array of doubles.
     * Example mapping: a -> a1,a2  b -> b  c -> c1,c2 ==> [a, b, c] --> [a1, a2, b, c1, c2]
     * @param varName
     * The name of the variable of which the value is a member as received from the query
     * @param value
     * The value of the variable as received from the query
     * @return
     * Returns an array of doubles which will be used by the classifier.
     */
    static Double[] parse(String varName, String value) {
        return new Double[]{Double.parseDouble(value)};
    }

    /**
     * Transforms the names of the variables as received from the query into new names for the new variables.
     * Example mapping: [a, b, c] --> [a1, a2, b, c1, c2]
     * @param headers
     * The names of the variables as received from the query
     * @return
     * The names of the variables that will be used for the classifier
     */
    static List<String> transformHeader(List<String> headers) {
        return headers;
    }

}
