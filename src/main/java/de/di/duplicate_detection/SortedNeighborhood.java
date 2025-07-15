package de.di.duplicate_detection;

import de.di.Relation;
import de.di.duplicate_detection.structures.AttrSimWeight;
import de.di.duplicate_detection.structures.Duplicate;
import de.di.similarity_measures.Jaccard;
import de.di.similarity_measures.Levenshtein;
import de.di.similarity_measures.helper.Tokenizer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

public class SortedNeighborhood {

    @Data
    @AllArgsConstructor
    private static class Record {
        private int index;
        private String[] values;
    }

    // Original method signature for backward compatibility
    public Set<Duplicate> detectDuplicates(Relation relation, int[] sortingKeys,
                                           int windowSize, RecordComparator recordComparator) {
        // Use default threshold of 0.85
        return detectDuplicates(relation, sortingKeys, windowSize, recordComparator, 0.85);
    }

    // New method with threshold parameter
    public Set<Duplicate> detectDuplicates(Relation relation, int[] sortingKeys,
                                           int windowSize, RecordComparator recordComparator,
                                           double threshold) {
        Set<Duplicate> duplicates = new HashSet<>();

        // Create indexable records
        Record[] records = new Record[relation.getRecords().length];
        for (int i = 0; i < relation.getRecords().length; i++)
            records[i] = new Record(i, relation.getRecords()[i]);

        // Process each sorting key
        for (int sortingKey : sortingKeys) {
            // Sort by current key
            Arrays.sort(records, (r1, r2) -> {
                String val1 = r1.values[sortingKey];
                String val2 = r2.values[sortingKey];
                return val1.compareTo(val2);
            });

            // Compare records within window
            for (int i = 0; i < records.length; i++) {
                for (int j = i + 1; j < Math.min(i + windowSize + 1, records.length); j++) {
                    double similarity = recordComparator.compare(records[i].values, records[j].values);
                    if (similarity >= threshold) {
                        int minIndex = Math.min(records[i].index, records[j].index);
                        int maxIndex = Math.max(records[i].index, records[j].index);
                        duplicates.add(new Duplicate(minIndex, maxIndex, similarity, relation));
                    }
                }
            }
        }

        return duplicates;
    }

    public static RecordComparator suggestRecordComparatorFor(Relation relation) {
        List<AttrSimWeight> attrSimWeights = new ArrayList<>(relation.getAttributes().length);
        double threshold = 0.85;

        for (int i = 0; i < relation.getAttributes().length; i++) {
            String attributeName = relation.getAttributes()[i].toLowerCase();
            double weight = 1.0 / relation.getAttributes().length;

            if (attributeName.contains("name") || attributeName.contains("title")) {
                attrSimWeights.add(new AttrSimWeight(i, new Levenshtein(true), weight));
            }
            else if (attributeName.contains("address") || attributeName.contains("street")) {
                attrSimWeights.add(new AttrSimWeight(i, new Jaccard(new Tokenizer(3, true), true), weight));
            }
            else if (attributeName.contains("email")) {
                attrSimWeights.add(new AttrSimWeight(i, new Jaccard(new Tokenizer(1, true), true), weight));
            }
            else {
                attrSimWeights.add(new AttrSimWeight(i, new Jaccard(new Tokenizer(3, true), true), weight));
            }
        }

        return new RecordComparator(attrSimWeights, threshold);
    }
}