package Associator;

import java.util.List;

public class StringFakeAssociationRule {

    private List<String> premise;
    private List<String> consequence;
    private int premiseSupport;
    private int consequenceSupport;
    private int totalSupport;
    private int totalTransactions;

    StringFakeAssociationRule(List<String> premise, List<String> consequence, int premiseSupport, int consequenceSupport, int totalSupport) {
        setPremise(premise);
        setConsequence(consequence);
        setPremiseSupport(premiseSupport);
        setConsequenceSupport(consequenceSupport);
        setTotalSupport(totalSupport);
    }

    public List<String> getPremise() {
        return premise;
    }

    public void setPremise(List<String> premise) {
        this.premise = premise;
    }

    public List<String> getConsequence() {
        return consequence;
    }

    public void setConsequence(List<String> consequence) {
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
