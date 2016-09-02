package Associator;

import java.util.List;

class FakeAssociationRule {

    private List<Integer> premise;
    private List<Integer> consequence;
    private int premiseSupport;
    private int consequenceSupport;
    private int totalSupport;

    FakeAssociationRule(List<Integer> premise, List<Integer> consequence, int premiseSupport, int consequenceSupport, int totalSupport) {
        setPremise(premise);
        setConsequence(consequence);
        setPremiseSupport(premiseSupport);
        setConsequenceSupport(consequenceSupport);
        setTotalSupport(totalSupport);
    }

    List<Integer> getPremise() {
        return premise;
    }

    private void setPremise(List<Integer> premise) {
        this.premise = premise;
    }

    List<Integer> getConsequence() {
        return consequence;
    }

    private void setConsequence(List<Integer> consequence) {
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
