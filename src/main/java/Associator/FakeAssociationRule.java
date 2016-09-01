package Associator;

import java.util.List;

public class FakeAssociationRule {

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

    public List<Integer> getPremise() {
        return premise;
    }

    public void setPremise(List<Integer> premise) {
        this.premise = premise;
    }

    public List<Integer> getConsequence() {
        return consequence;
    }

    public void setConsequence(List<Integer> consequence) {
        this.consequence = consequence;
    }

    public int getPremiseSupport() {
        return premiseSupport;
    }

    public void setPremiseSupport(int premiseSupport) {
        this.premiseSupport = premiseSupport;
    }

    public int getConsequenceSupport() {
        return consequenceSupport;
    }

    public void setConsequenceSupport(int consequenceSupport) {
        this.consequenceSupport = consequenceSupport;
    }

    public int getTotalSupport() {
        return totalSupport;
    }

    public void setTotalSupport(int totalSupport) {
        this.totalSupport = totalSupport;
    }

    public String toString() {
        return getPremise().toString() + "->" + getConsequence().toString() + " " + getPremiseSupport() + " " + getConsequenceSupport() + " " + getTotalSupport();
    }
}
