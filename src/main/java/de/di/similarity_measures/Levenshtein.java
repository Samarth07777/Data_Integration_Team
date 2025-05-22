package de.di.similarity_measures;

import java.util.Arrays;

public class Levenshtein implements SimilarityMeasure {

    private final boolean useDamerau;

    public Levenshtein(boolean useDamerau) {
        this.useDamerau = useDamerau;
    }

    @Override
    public double calculate(String input1, String input2) {
        input1 = input1 == null ? "" : input1;
        input2 = input2 == null ? "" : input2;

        String[] tokensA = input1.split("");
        String[] tokensB = input2.split("");

        return computeNormalizedDistance(tokensA, tokensB);
    }

    @Override
    public double calculate(String[] tokensA, String[] tokensB) {
        if (tokensA == null) tokensA = new String[0];
        if (tokensB == null) tokensB = new String[0];

        return computeNormalizedDistance(tokensA, tokensB);
    }

    private double computeNormalizedDistance(String[] tokensA, String[] tokensB) {
        int lenA = tokensA.length;
        int lenB = tokensB.length;

        if (lenA == 0 && lenB == 0) {
            return 1.0;
        }

        int[][] distance = new int[lenA + 1][lenB + 1];

        for (int i = 0; i <= lenA; i++) distance[i][0] = i;
        for (int j = 0; j <= lenB; j++) distance[0][j] = j;

        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = tokensA[i - 1].equals(tokensB[j - 1]) ? 0 : 1;

                int insertion = distance[i][j - 1] + 1;
                int deletion = distance[i - 1][j] + 1;
                int substitution = distance[i - 1][j - 1] + cost;

                distance[i][j] = Math.min(Math.min(insertion, deletion), substitution);

                if (useDamerau && i > 1 && j > 1
                        && tokensA[i - 1].equals(tokensB[j - 2])
                        && tokensA[i - 2].equals(tokensB[j - 1])) {
                    int transposition = distance[i - 2][j - 2] + cost;
                    distance[i][j] = Math.min(distance[i][j], transposition);
                }
            }
        }

        int rawDistance = distance[lenA][lenB];
        int maxLength = Math.max(lenA, lenB);

        return maxLength == 0 ? 1.0 : 1.0 - ((double) rawDistance / maxLength);
    }
}

