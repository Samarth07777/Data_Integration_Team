package de.di.data_profiling;

import de.di.Relation;
import de.di.data_profiling.structures.IND;

import java.util.*;

/**
 * Responsible for detecting unary inclusion dependencies (INDs) between attributes across relations.
 */
public class INDProfiler {

    /**
     * Triggers the IND discovery process on the given relations.
     *
     * @param datasets       List of relations to evaluate.
     * @param includeNary    Flag to indicate if n-ary INDs should be included (not supported).
     * @return List of detected INDs.
     */
    public List<IND> profile(List<Relation> datasets, boolean includeNary) {
        if (includeNary) {
            throw new UnsupportedOperationException("N-ary IND discovery is not implemented.");
        }

        List<IND> indResults = new ArrayList<>();

        for (Relation baseRelation : datasets) {
            int baseColCount = baseRelation.getAttributes().length;

            for (int baseIndex = 0; baseIndex < baseColCount; baseIndex++) {
                Set<String> baseData = collectValues(baseRelation.getColumns()[baseIndex]);

                for (Relation candidateRelation : datasets) {
                    int candidateColCount = candidateRelation.getAttributes().length;

                    for (int candidateIndex = 0; candidateIndex < candidateColCount; candidateIndex++) {
                        // Exclude self-check
                        if (isSameColumn(baseRelation, baseIndex, candidateRelation, candidateIndex)) {
                            continue;
                        }

                        Set<String> candidateData = collectValues(candidateRelation.getColumns()[candidateIndex]);

                        if (isSubset(baseData, candidateData)) {
                            indResults.add(new IND(baseRelation, baseIndex, candidateRelation, candidateIndex));
                        }
                    }
                }
            }
        }

        return indResults;
    }

    /**
     * Extracts a set of distinct values from a given column.
     *
     * @param column Raw string array from a relation.
     * @return Set containing all unique entries.
     */
    private Set<String> collectValues(String[] column) {
        Set<String> valuePool = new HashSet<>();
        for (String entry : column) {
            if (entry != null) {
                valuePool.add(entry);
            }
        }
        return valuePool;
    }

    /**
     * Verifies if a set is completely included in another.
     *
     * @param smaller The potential subset.
     * @param larger  The potential superset.
     * @return true if all elements in smaller exist in larger.
     */
    private boolean isSubset(Set<String> smaller, Set<String> larger) {
        if (smaller.isEmpty()) return false;  // Ignore empty column INDs
        return larger.containsAll(smaller);
    }

    /**
     * Determines if two columns from two relations are actually the same (to skip self-comparison).
     *
     * @param relA First relation.
     * @param idxA Column index in first relation.
     * @param relB Second relation.
     * @param idxB Column index in second relation.
     * @return true if they are the same column in the same relation.
     */
    private boolean isSameColumn(Relation relA, int idxA, Relation relB, int idxB) {
        return relA == relB && idxA == idxB;
    }
}
