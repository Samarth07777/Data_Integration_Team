package de.di.data_profiling.structures;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class PositionListIndex {

    private final AttributeList attributes;
    private final List<IntArrayList> valueGroups;
    private final int[] recordToGroup;

    public PositionListIndex(final AttributeList attributes, final String[] columnValues) {
        this.attributes = attributes;
        this.valueGroups = buildGroups(columnValues);
        this.recordToGroup = mapRecordsToGroups(this.valueGroups, columnValues.length);
    }

    public PositionListIndex(final AttributeList attributes, final List<IntArrayList> groups, final int totalRecords) {
        this.attributes = attributes;
        this.valueGroups = groups;
        this.recordToGroup = mapRecordsToGroups(groups, totalRecords);
    }

    private List<IntArrayList> buildGroups(final String[] values) {
        Map<String, IntArrayList> buckets = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            buckets.computeIfAbsent(values[i], k -> new IntArrayList()).add(i);
        }
        return buckets.values().stream()
                .filter(group -> group.size() > 1)
                .collect(Collectors.toList());
    }

    private int[] mapRecordsToGroups(final List<IntArrayList> groups, int totalSize) {
        int[] mapping = new int[totalSize];
        Arrays.fill(mapping, -1);
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            for (int recordId : groups.get(groupIndex)) {
                mapping[recordId] = groupIndex;
            }
        }
        return mapping;
    }

    public boolean isUnique() {
        return this.valueGroups.isEmpty();
    }

    public int size() {
        return this.recordToGroup.length;
    }

    public PositionListIndex intersect(PositionListIndex other) {
        List<IntArrayList> intersectedGroups = findOverlapGroups(this.valueGroups, other.recordToGroup);
        AttributeList mergedAttributes = this.attributes.union(other.getAttributes());
        return new PositionListIndex(mergedAttributes, intersectedGroups, this.size());
    }

    private List<IntArrayList> findOverlapGroups(List<IntArrayList> sourceGroups, int[] otherRecordGroupMap) {
        Map<String, IntArrayList> groupingMap = new HashMap<>();

        for (IntArrayList group : sourceGroups) {
            for (int record : group) {
                int otherGroupId = otherRecordGroupMap[record];
                if (otherGroupId == -1) continue;

                // Construct a flat composite key to identify intersections
                String key = group.hashCode() + "-" + otherGroupId;
                groupingMap.computeIfAbsent(key, k -> new IntArrayList()).add(record);
            }
        }

        // Keep only valid (stripped) clusters with size > 1
        return groupingMap.values().stream()
                .filter(g -> g.size() > 1)
                .collect(Collectors.toList());
    }
}
