package de.di.data_profiling;

import de.di.Relation;
import de.di.data_profiling.structures.AttributeList;
import de.di.data_profiling.structures.PositionListIndex;
import de.di.data_profiling.structures.UCC;

import java.util.*;

public class UCCProfiler {

    /**
     * Identifies all minimal unique column sets (UCCs) in the given relation.
     *
     * @param relation Input relation to analyze for unique column combinations.
     * @return List of all valid UCCs found.
     */
    public List<UCC> profile(Relation relation) {
        int colCount = relation.getAttributes().length;
        List<UCC> discoveredUCCs = new ArrayList<>();
        Set<Set<Integer>> knownUniqueSets = new HashSet<>();
        Map<Set<Integer>, PositionListIndex> pendingCombinations = new HashMap<>();

        // Initial scan: check all single-column uniqueness
        for (int col = 0; col < colCount; col++) {
            AttributeList singleAttr = new AttributeList(col);
            PositionListIndex pli = new PositionListIndex(singleAttr, relation.getColumns()[col]);

            if (pli.isUnique()) {
                discoveredUCCs.add(new UCC(relation, singleAttr));
                knownUniqueSets.add(Collections.singleton(col));
            } else {
                Set<Integer> colSet = new HashSet<>();
                colSet.add(col);
                pendingCombinations.put(colSet, pli);
            }
        }

        // Multi-column uniqueness search (level-wise)
        int targetSize = 2;
        while (!pendingCombinations.isEmpty()) {
            Map<Set<Integer>, PositionListIndex> nextLevel = new HashMap<>();

            List<Map.Entry<Set<Integer>, PositionListIndex>> pendingList = new ArrayList<>(pendingCombinations.entrySet());
            int n = pendingList.size();

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Set<Integer> merged = tryCombine(pendingList.get(i).getKey(), pendingList.get(j).getKey());
                    if (merged == null || merged.size() != targetSize || containsUniqueSubset(merged, knownUniqueSets)) {
                        continue;
                    }

                    PositionListIndex combinedPLI = pendingList.get(i).getValue()
                            .intersect(pendingList.get(j).getValue());

                    if (combinedPLI.isUnique()) {
                        discoveredUCCs.add(new UCC(relation, new AttributeList(toArray(merged))));
                        knownUniqueSets.add(merged);
                    } else {
                        nextLevel.put(merged, combinedPLI);
                    }
                }
            }

            pendingCombinations = nextLevel;
            targetSize++;
        }

        return discoveredUCCs;
    }

    /**
     * Merges two attribute index sets if possible.
     * Ensures sets can be merged in level-wise UCC discovery.
     */
    private Set<Integer> tryCombine(Set<Integer> first, Set<Integer> second) {
        if (first.size() != second.size()) return null;

        List<Integer> l1 = new ArrayList<>(first);
        List<Integer> l2 = new ArrayList<>(second);

        Collections.sort(l1);
        Collections.sort(l2);

        for (int i = 0; i < l1.size() - 1; i++) {
            if (!l1.get(i).equals(l2.get(i))) return null;
        }

        Set<Integer> merged = new HashSet<>(first);
        merged.addAll(second);
        return merged;
    }

    /**
     * Checks whether the candidate attribute set has a proper subset that is already known as unique.
     */
    private boolean containsUniqueSubset(Set<Integer> candidate, Set<Set<Integer>> knownUCCs) {
        for (Set<Integer> uniqueSet : knownUCCs) {
            if (candidate.containsAll(uniqueSet) && candidate.size() > uniqueSet.size()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Utility method: converts Set<Integer> to int[] for AttributeList constructor.
     */
    private int[] toArray(Set<Integer> indexSet) {
        return indexSet.stream().mapToInt(Integer::intValue).toArray();
    }
}
