package de.di.schema_matching;

import de.di.schema_matching.structures.CorrespondenceMatrix;
import de.di.schema_matching.structures.SimilarityMatrix;

import java.util.Arrays;

public class SecondLineSchemaMatcher {

    /**
     * Uses cost minimization to convert similarity scores into a one-to-one correspondence matrix.
     * @param similarityMatrix Matrix representing similarity between source and target attributes.
     * @return A matrix representing binary attribute matchings.
     */
    public CorrespondenceMatrix match(SimilarityMatrix similarityMatrix) {
        double[][] similarityGrid = similarityMatrix.getMatrix();
        int rows = similarityGrid.length;
        int cols = similarityGrid[0].length;
        int size = Math.max(rows, cols);

        double[][] costGrid = new double[size][size];
        for (int i = 0; i < size; i++) {
            Arrays.fill(costGrid[i], 1.0);
            for (int j = 0; j < size; j++) {
                if (i < rows && j < cols) {
                    costGrid[i][j] = 1.0 - similarityGrid[i][j];
                }
            }
        }

        AssignmentEngine engine = new AssignmentEngine(costGrid);
        int[] matches = engine.solve();

        int[][] binaryMatrix = toBinaryMatrix(matches, similarityGrid);
        return new CorrespondenceMatrix(binaryMatrix, similarityMatrix.getSourceRelation(), similarityMatrix.getTargetRelation());
    }

    /**
     * Turns assignment result into binary matrix format.
     */
    private int[][] toBinaryMatrix(int[] assignments, double[][] simGrid) {
        int[][] binary = new int[simGrid.length][simGrid[0].length];
        for (int i = 0; i < simGrid.length; i++) {
            int assigned = assignments[i];
            if (assigned >= 0 && assigned < simGrid[0].length) {
                binary[i][assigned] = 1;
            }
        }
        return binary;
    }

    /**
     * Custom implementation of the Munkres (Hungarian) algorithm for optimal assignment.
     */
    static class AssignmentEngine {
        private final double[][] weights;
        private final int n;
        private final double[] rowAdjust, colAdjust;
        private final int[] rowToCol, colToRow;
        private final int[] parentTrace;
        private final double[] minSlack;
        private final int[] slackSource;
        private final boolean[] visitedRows;

        AssignmentEngine(double[][] weights) {
            this.n = weights.length;
            this.weights = new double[n][n];
            for (int i = 0; i < n; i++) {
                this.weights[i] = Arrays.copyOf(weights[i], n);
            }

            rowAdjust = new double[n];
            colAdjust = new double[n];
            rowToCol = new int[n];
            colToRow = new int[n];
            parentTrace = new int[n];
            minSlack = new double[n];
            slackSource = new int[n];
            visitedRows = new boolean[n];
            Arrays.fill(rowToCol, -1);
            Arrays.fill(colToRow, -1);
        }

        int[] solve() {
            initializeDualLabels();

            for (int i = 0; i < n; i++) {
                if (rowToCol[i] == -1) {
                    growAlternatingTree(i);
                }
            }

            return Arrays.copyOf(rowToCol, n);
        }

        private void initializeDualLabels() {
            Arrays.fill(colAdjust, Double.POSITIVE_INFINITY);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (weights[i][j] < colAdjust[j]) {
                        colAdjust[j] = weights[i][j];
                    }
                }
            }
        }

        private void growAlternatingTree(int rootRow) {
            Arrays.fill(parentTrace, -1);
            Arrays.fill(visitedRows, false);
            Arrays.fill(minSlack, Double.POSITIVE_INFINITY);

            int[] queue = new int[n];
            int qStart = 0, qEnd = 0;

            visitedRows[rootRow] = true;
            queue[qEnd++] = rootRow;

            for (int j = 0; j < n; j++) {
                minSlack[j] = weights[rootRow][j] - rowAdjust[rootRow] - colAdjust[j];
                slackSource[j] = rootRow;
            }

            while (true) {
                while (qStart < qEnd) {
                    int i = queue[qStart++];
                    for (int j = 0; j < n; j++) {
                        if (parentTrace[j] == -1) {
                            double reducedCost = weights[i][j] - rowAdjust[i] - colAdjust[j];
                            if (reducedCost < 1e-10) {
                                parentTrace[j] = i;
                                if (colToRow[j] == -1) {
                                    reconstructMatching(j);
                                    return;
                                }
                                int nextRow = colToRow[j];
                                if (!visitedRows[nextRow]) {
                                    visitedRows[nextRow] = true;
                                    queue[qEnd++] = nextRow;
                                    for (int k = 0; k < n; k++) {
                                        double slack = weights[nextRow][k] - rowAdjust[nextRow] - colAdjust[k];
                                        if (minSlack[k] > slack) {
                                            minSlack[k] = slack;
                                            slackSource[k] = nextRow;
                                        }
                                    }
                                }
                            } else if (minSlack[j] > reducedCost) {
                                minSlack[j] = reducedCost;
                                slackSource[j] = i;
                            }
                        }
                    }
                }

                double delta = Double.POSITIVE_INFINITY;
                for (int j = 0; j < n; j++) {
                    if (parentTrace[j] == -1 && minSlack[j] < delta) {
                        delta = minSlack[j];
                    }
                }

                for (int i = 0; i < n; i++) {
                    if (visitedRows[i]) {
                        rowAdjust[i] += delta;
                    }
                }
                for (int j = 0; j < n; j++) {
                    if (parentTrace[j] != -1) {
                        colAdjust[j] -= delta;
                    } else {
                        minSlack[j] -= delta;
                    }
                }

                for (int j = 0; j < n; j++) {
                    if (parentTrace[j] == -1 && minSlack[j] < 1e-10) {
                        parentTrace[j] = slackSource[j];
                        if (colToRow[j] == -1) {
                            reconstructMatching(j);
                            return;
                        }
                        int addRow = colToRow[j];
                        if (!visitedRows[addRow]) {
                            visitedRows[addRow] = true;
                            queue[qEnd++] = addRow;
                            for (int k = 0; k < n; k++) {
                                double slack = weights[addRow][k] - rowAdjust[addRow] - colAdjust[k];
                                if (minSlack[k] > slack) {
                                    minSlack[k] = slack;
                                    slackSource[k] = addRow;
                                }
                            }
                        }
                    }
                }
            }
        }

        private void reconstructMatching(int col) {
            while (col != -1) {
                int row = parentTrace[col];
                int prevCol = rowToCol[row];
                rowToCol[row] = col;
                colToRow[col] = row;
                col = prevCol;
            }
        }
    }
}
