package com.knowvault.util;

import com.knowvault.model.Note;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class TextAnalyzer {
    private static final Set<String> STOP_WORDS = Set.of("the","a","an","and","or","to","of","in","is","for","with","on","that","this");
    private TextAnalyzer() { }

    public static Map<String, Long> frequentTopics(Collection<Note> notes, int limit) {
        Predicate<String> useful = word -> word.length() > 2 && !STOP_WORDS.contains(word);
        Map<String, Long> grouped = notes.stream()
                .flatMap(n -> Arrays.stream((n.getTitle() + " " + n.getContent()).toLowerCase(Locale.ROOT).split("[^a-z0-9+#]+")))
                .filter(useful).collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        Map<String, Long> snapshot = new HashMap<>(grouped);
        return snapshot.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(a,b)->a,LinkedHashMap::new));
    }
}
