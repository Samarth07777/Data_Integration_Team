package de.di.schema_matching;

import de.di.Relation;
import de.di.schema_matching.structures.SimilarityMatrix;
import de.di.similarity_measures.Jaccard;
import de.di.similarity_measures.helper.Tokenizer;

public class FirstLineSchemaMatcher {

    public SimilarityMatrix match(Relation relA, Relation relB) {
        String[][] attrA = relA.getColumns();
        String[][] attrB = relB.getColumns();

        int lenA = attrA.length;
        int lenB = attrB.length;

        double[][] similarityScores = new double[lenA][lenB];

        // Create tokenizer and similarity metric with alternate config values
        int tokenizerMode = 1;               // or some other enum/int if available
        boolean filterTokens = true;
        Tokenizer splitter = new Tokenizer(tokenizerMode, filterTokens);

        boolean normalize = false;
        Jaccard similarityChecker = new Jaccard(splitter, normalize);

        // Pairwise comparison of attribute columns
        for (int aIdx = 0; aIdx < lenA; aIdx++) {
            String[] colA = attrA[aIdx];

            for (int bIdx = 0; bIdx < lenB; bIdx++) {
                String[] colB = attrB[bIdx];

                double sim = similarityChecker.calculate(colA, colB);
                similarityScores[aIdx][bIdx] = sim;
            }
        }

        return new SimilarityMatrix(similarityScores, relA, relB);
    }
}
