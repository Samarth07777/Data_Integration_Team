package de.di.similarity_measures;

import de.di.similarity_measures.helper.Tokenizer;
import lombok.AllArgsConstructor;

import java.util.*;

@AllArgsConstructor
public class Jaccard implements SimilarityMeasure {

    private final Tokenizer tokenGenerator;
    private final boolean allowDuplicates;

    @Override
    public double calculate(String text1, String text2) {
        text1 = text1 == null ? "" : text1;
        text2 = text2 == null ? "" : text2;

        String[] units1 = tokenGenerator.tokenize(text1);
        String[] units2 = tokenGenerator.tokenize(text2);

        return computeSimilarity(units1, units2);
    }

    @Override
    public double calculate(String[] terms1, String[] terms2) {
        terms1 = terms1 == null ? new String[0] : terms1;
        terms2 = terms2 == null ? new String[0] : terms2;

        return computeSimilarity(terms1, terms2);
    }

    private double computeSimilarity(String[] firstTokens, String[] secondTokens) {
        if (firstTokens.length == 0 && secondTokens.length == 0) {
            return 1.0;
        }

        if (allowDuplicates) {
            // Multiset (bag) semantics
            Map<String, Integer> map1 = countTokens(firstTokens);
            Map<String, Integer> map2 = countTokens(secondTokens);

            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(map1.keySet());
            allKeys.addAll(map2.keySet());

            int overlap = 0;
            int total = 0;

            for (String token : allKeys) {
                int freq1 = map1.getOrDefault(token, 0);
                int freq2 = map2.getOrDefault(token, 0);

                overlap += Math.min(freq1, freq2);
                total += freq1 + freq2;
            }

            return total == 0 ? 0.0 : (double) overlap / total;

        } else {
            // Set semantics
            Set<String> setA = new HashSet<>(Arrays.asList(firstTokens));
            Set<String> setB = new HashSet<>(Arrays.asList(secondTokens));

            Set<String> shared = new HashSet<>(setA);
            shared.retainAll(setB);

            Set<String> combined = new HashSet<>(setA);
            combined.addAll(setB);

            return combined.isEmpty() ? 0.0 : (double) shared.size() / combined.size();
        }
    }

    private Map<String, Integer> countTokens(String[] tokens) {
        Map<String, Integer> countMap = new HashMap<>();
        for (String token : tokens) {
            countMap.put(token, countMap.getOrDefault(token, 0) + 1);
        }
        return countMap;
    }
}