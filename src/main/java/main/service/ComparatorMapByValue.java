package main.service;

import java.util.Comparator;
import java.util.Map;

public class ComparatorMapByValue implements Comparator<String> {
    Map<String, Integer> base;

    public ComparatorMapByValue(Map<String, Integer> base) {
        this.base = base;
    }

    public int compare(String a, String b) {
        if (a.equals(b)) {
            return 0;
        }
        if (base.get(a).compareTo(base.get(b)) == 0) {
            return a.compareTo(b);
        }
        return base.get(a).compareTo(base.get(b));
    }

}
