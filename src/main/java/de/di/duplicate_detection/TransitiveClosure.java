package de.di.duplicate_detection;

import de.di.Relation;
import de.di.duplicate_detection.structures.Duplicate;

import java.util.HashSet;
import java.util.Set;

public class TransitiveClosure {

    public Set<Duplicate> calculate(Set<Duplicate> inputPairs) {
        Set<Duplicate> result = new HashSet<>();

        if (inputPairs == null || inputPairs.isEmpty()) {
            return result;
        }

        Relation relationRef = inputPairs.iterator().next().getRelation();
        int total = relationRef.getRecords().length;

        // Represent graph with adjacency info
        boolean[][] linked = new boolean[total][total];

        // Parse record IDs from Duplicate.toString()
        for (Duplicate d : inputPairs) {
            String info = d.toString(); // e.g., Duplicate(1.000000: 1, 4)
            int colon = info.indexOf(":");
            int comma = info.indexOf(",", colon);
            int end = info.indexOf(")", comma);

            int a = Integer.parseInt(info.substring(colon + 1, comma).trim());
            int b = Integer.parseInt(info.substring(comma + 1, end).trim());

            linked[a][b] = true;
            linked[b][a] = true;
        }

        boolean[] visited = new boolean[total];

        for (int node = 0; node < total; node++) {
            if (!visited[node]) {
                Set<Integer> component = new HashSet<>();
                traverse(node, linked, visited, component);

                Integer[] members = component.toArray(new Integer[0]);
                for (int i = 0; i < members.length; i++) {
                    for (int j = i + 1; j < members.length; j++) {
                        result.add(new Duplicate(members[i], members[j], 1.0, relationRef));
                    }
                }
            }
        }

        return result;
    }

    private void traverse(int current, boolean[][] graph, boolean[] seen, Set<Integer> group) {
        seen[current] = true;
        group.add(current);

        for (int neighbor = 0; neighbor < graph.length; neighbor++) {
            if (graph[current][neighbor] && !seen[neighbor]) {
                traverse(neighbor, graph, seen, group);
            }
        }
    }
}

