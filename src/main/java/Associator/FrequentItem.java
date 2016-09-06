package Associator;

/**
 * Stores the name of an attribute and its score, used when determining the frequent item set.
 */
public class FrequentItem implements Comparable<FrequentItem> {
    private int score;
    private String item;

    FrequentItem(int score, String item) {
        this.score = score;
        this.item = item;
    }

    @Override
    public int compareTo(FrequentItem o) {
        return score < o.score ? 1 : score > o.score ? -1 : 0;
    }

    public int getScore() {
        return score;
    }

    //don't change this to private, it is needed by the Main class to convert FrequentItems into JsonNodes. Intellij is lying.
    public String getItem() {
        return item;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!FrequentItem.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final FrequentItem other = (FrequentItem) obj;
        return (other.getItem().equals(getItem()));
    }

    @Override
    public int hashCode() {
        return item.hashCode();
    }
}