package de.di.similarity_measures;

import de.di.similarity_measures.helper.MinHash;
import de.di.similarity_measures.helper.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LocalitySensitiveHashing implements SimilarityMeasure {

    private final Tokenizer chunker;
    private final boolean countDuplicates;
    private final List<MinHash> hashTools;

    public LocalitySensitiveHashing(final Tokenizer tokenizer, final boolean bagMode, final int hashCount) {
        if (tokenizer.getTokenSize() < hashCount) {
            throw new IllegalArgumentException("Tokenizer must support at least as many hash functions.");
        }

        this.chunker = tokenizer;
        this.countDuplicates = bagMode;
        this.hashTools = new ArrayList<>();
        for (int i = 0; i < hashCount; i++) {
            hashTools.add(new MinHash(i));
        }
    }

    @Override
    public double calculate(final String input1, final String input2) {
        String[] tokens1 = chunker.tokenize(input1 == null ? "" : input1);
        String[] tokens2 = chunker.tokenize(input2 == null ? "" : input2);
        return calculate(tokens1, tokens2);
    }

    @Override
    public double calculate(final String[] tokens1, final String[] tokens2) {
        String[] sig1 = new String[hashTools.size()];
        String[] sig2 = new String[hashTools.size()];

        for (int i = 0; i < hashTools.size(); i++) {
            sig1[i] = hashTools.get(i).hash(tokens1);
            sig2[i] = hashTools.get(i).hash(tokens2);
        }

        int matches = 0;
        for (int i = 0; i < sig1.length; i++) {
            if (Objects.equals(sig1[i], sig2[i])) {
                matches++;
            }
        }

        if (countDuplicates) {
            // Bag mode: union = both signature lengths combined
            return (double) matches / (sig1.length + sig2.length);
        } else {
            // Set mode: union = signature length (since no duplicates)
            int unionSize = sig1.length;
            int effectiveUnion = unionSize == matches ? unionSize : unionSize + (sig1.length - matches);
            return (double) matches / effectiveUnion;
        }
    }
}