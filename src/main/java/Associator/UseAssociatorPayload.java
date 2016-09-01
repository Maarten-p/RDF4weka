package Associator;

import Main.Algorithm;

import java.util.ArrayList;
import java.util.List;

public class UseAssociatorPayload {

    private List<String> skills;
    private Algorithm algorithm;
    private String identifier;
    private String method;

    List<String> getSkills() {
        return skills;
    }

    void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public boolean isValid() {
        if (identifier == null || identifier.isEmpty())
            return false;
        Algorithm[] values = Algorithm.values();
        for (Algorithm value : values) {
            if (value == this.algorithm)
                return true;
        }
        return false;
    }

    public void insertDefaults() {
        if (skills == null) {
            List<String> testString = new ArrayList<>();
            testString.add("1a3660c2-011c-4e96-9b1d-529afc305428");
            testString.add("69f23426-9279-4fe6-a283-24c2aa4c855d");
            testString.add("8d4271ca-c9fd-40b3-875f-15f78332a49e");
            testString.add("334e3e49-fb02-4051-809a-f06adfdc1c40");
            testString.add("bf7a45c4-0d72-4e6e-9da7-127b2b6656b7");
            testString.add("7530bc44-f09d-4dd2-8911-92b98333e442");
            setSkills(testString);
        }
        if (algorithm == null)
            setAlgorithm(Algorithm.FPGROWTH);
    }

}
