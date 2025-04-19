package taskone;

import java.util.List;
import java.util.ArrayList;

class StringList {
    // make list private for encapsulation
    private List<String> strings = new ArrayList<String>();

    // synchronized method to add a string (keep thread-safe)
    public synchronized void add(String str) {
        int pos = strings.indexOf(str);
        if (pos < 0) {
            strings.add(str);
        }
    }

    // sychronized method to check contain (keep thread-safe)
    public synchronized boolean contains(String str) {
        return strings.indexOf(str) >= 0;
    }

    // sychronized method to get size (keep thread-safe)
    public synchronized int size() {
        return strings.size();
    }

    // sychronized method to return string representation (keep thread-safe)
    public synchronized String toString() {
        return strings.toString();
    }
}