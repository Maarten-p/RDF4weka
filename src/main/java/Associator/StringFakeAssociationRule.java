package Associator;

import java.util.List;

/**
 * A class for AssociationRules that use Strings instead of Items
 */
class StringFakeAssociationRule {

    private List<String> premise;
    private List<String> consequence;
    private int premiseSupport;
    private int consequenceSupport;
    private int totalSupport;

    StringFakeAssociationRule(List<String> premise, List<String> consequence, int premiseSupport, int consequenceSupport, int totalSupport) {
        setPremise(premise);
        setConsequence(consequence);
        setPremiseSupport(premiseSupport);
        setConsequenceSupport(consequenceSupport);
        setTotalSupport(totalSupport);
    }

    List<String> getPremise() {
        return premise;
    }

    private void setPremise(List<String> premise) {
        this.premise = premise;
    }

    List<String> getConsequence() {
        return consequence;
    }

    private void setConsequence(List<String> consequence) {
        this.consequence = consequence;
    }

    int getPremiseSupport() {
        return premiseSupport;
    }

    private void setPremiseSupport(int premiseSupport) {
        this.premiseSupport = premiseSupport;
    }

    int getConsequenceSupport() {
        return consequenceSupport;
    }

    private void setConsequenceSupport(int consequenceSupport) {
        this.consequenceSupport = consequenceSupport;
    }

    int getTotalSupport() {
        return totalSupport;
    }

    private void setTotalSupport(int totalSupport) {
        this.totalSupport = totalSupport;
    }

    public String toString() {
        return getPremise().toString() + "->" + getConsequence().toString() + " " + getPremiseSupport() + " " + getConsequenceSupport() + " " + getTotalSupport();
    }
}
