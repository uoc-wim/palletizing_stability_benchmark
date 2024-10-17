package com.wim.assessment.staticStability.sme.helper;

import java.util.Arrays;
import java.util.List;

/**
 * Implements 2D Matrix operations to solve liner equation system.
 * Oriented on class org.locationtech.jts.math.Matrix by Martin Davies but can handle nxm matrices
 *
 * @author Frederick Gamer Created January 2022
 */
public class LinearEquationSystem {

    private final double ROUNDING_ERROR_EPSILON = 0.01;
    private final double ROUNDING_FACTOR = 10000.0;
    private final double RHS_ROUNDING_THRESHOLD;
    private final double[] LHS_ROUNDING_THRESHOLD;

    private final double[][] LHS;
    private final double[] RHS;
    private double[][] lhsReduced;
    private double[] rhsReduced;


    private double[] solution;

    private int rankCoefficientMatr = -1, rankAugmentedMatr = -1;

    public LinearEquationSystem(double[][] lhs, double[] rhs) {
        if (lhs.length != rhs.length || rhs.length == 0 || lhs[0].length < 1 || lhs[0].length > lhs.length)
            throw new IllegalArgumentException("Matrix is incorrectly sized");
        this.LHS = lhs;
        this.RHS = rhs;


        RHS_ROUNDING_THRESHOLD = getMaxOfArray(rhs) * ROUNDING_ERROR_EPSILON;
        LHS_ROUNDING_THRESHOLD = getMaxValues(lhs);
    }

    private double[] getMaxValues(double[][] multiArray) {

        double[] maxValues = new double[multiArray[0].length];

        double[][] transformedArray = new double[multiArray[0].length][multiArray.length];
        for (int i = 0; i < multiArray.length; i++)
            for (int j = 0; j < multiArray[i].length; j++)
                transformedArray[j][i] = multiArray[i][j];


        for (int i = 0; i < transformedArray.length; i++) {
            maxValues[i] = getMaxOfArray(transformedArray[i]) * ROUNDING_ERROR_EPSILON;
        }
        return maxValues;
    }

    private double getMaxOfArray(double[] array) {
        return Arrays.stream(array).map(Math::abs).max().getAsDouble();
    }

    /**
     * Tries to solve the LES. If LES has a unique solution, this solution is calculated.
     * If LES has infinite solutions, the matrix is reduced to its minimum enchelonForm.
     *
     * @return the solubility of the LES
     */
    public Solubility solveLesAsPossible() {
        transformToEchelonFormAndGetRank();

        if (hasUniqueSolution()) {
            reduceLES();
            solveMatrixInEchelonForm();
            if (isSolutionInvalid())
                return Solubility.NO_SOLUTION;
            return Solubility.UNIQUE_SOLUTION;
        } else if (hasSolution()) {
            reduceLES();
            return Solubility.INFINITE_SOLUTIONS;
        }
        return Solubility.NO_SOLUTION;
    }

    private boolean isSolutionInvalid() {
        for (double v : solution)
            if (v < 0)
                return true;
        return false;
    }

    /**
     * Returns only the columns specified by the indices of the reduced LHS
     *
     * @param indices specifying which columns to return
     * @return specific columns of the lhs
     */
    public double[][] getSpecificColumnsOfLHS(List<Integer> indices) {
        double[][] lhsReducedSpec = new double[this.lhsReduced.length][indices.size()];

        for (int i = 0; i < this.lhsReduced.length; i++) {
            for (int j = 0; j < indices.size(); j++) {
                lhsReducedSpec[i][j] = this.lhsReduced[i][indices.get(j)];
            }
        }
        return lhsReducedSpec;
    }

    /**
     * Transforms the matrix (left-hand side (lhs) and right-hand side (rhs) into the echelon form
     * using gaussian elimination
     */
    private void transformToEchelonFormAndGetRank() {

        int n = this.RHS.length;
        int m = this.LHS[0].length;

        // Use Gaussian Elimination with partial pivoting.
        // Iterate over each column
        for (int i = 0; i < m; i++) {
            // Find the largest pivot of this column in the rows below the current one.
            int maxElementRow = i;
            for (int j = i + 1; j < n; j++)
                if (Math.abs(this.LHS[j][i]) > Math.abs(this.LHS[maxElementRow][i]))
                    maxElementRow = j;

            if (!notZeroWithRoundingErrorInLHS(maxElementRow, i)) {
                if (!restOfLHSisZero(maxElementRow, i))
                    continue;
                //rank of coefficient matrix = i
                this.rankCoefficientMatr = i;

                for (int k = i; k < n; k++)
                    if (notZeroWithRoundingErrorInRHS(k)) {
                        //rank of augmented matrix is at least i+1
                        this.rankAugmentedMatr = i + 1;

                        return;
                    }

                //augmentedMatrix has same rank
                this.rankAugmentedMatr = i;

                //no further operations possible
                return;
            }

            // Exchange current row and maxElementRow in A and b.
            swapRows(this.LHS, i, maxElementRow);
            swapRows(this.RHS, i, maxElementRow);

            // Eliminate using row i
            //Run through all columns below i
            for (int j = i + 1; j < n; j++) {
                //identify factor to reduce colum to zero
                double rowFactor = this.LHS[j][i] / this.LHS[i][i];
                if (rowFactor == 0)
                    continue;
                //multiply rest of column with it
                for (int k = m - 1; k >= i; k--)
                    //value is rounded due to floating point error
                    this.LHS[j][k] =
                            Math.round((this.LHS[j][k] - this.LHS[i][k] * rowFactor) * ROUNDING_FACTOR) / ROUNDING_FACTOR;


                //multiply rhs as well
                this.RHS[j] = Math.round((this.RHS[j] - this.RHS[i] * rowFactor) * ROUNDING_FACTOR) / ROUNDING_FACTOR;
            }
        }
        //Matrix in enchelon Form and the first m rows are no zero-rows ->
        this.rankCoefficientMatr = m;
        for (int i = m; i < n; i++)
            if (notZeroWithRoundingErrorInRHS(i)) {
                //rank of augmented matrix is at least i+1
                this.rankAugmentedMatr = m + 1;
                return;
            }
        this.rankAugmentedMatr = m;
    }

    private boolean notZeroWithRoundingErrorInRHS(int index) {
        if (this.RHS[index] == 0.0)
            return false;
        return Math.abs(this.RHS[index]) > Math.abs(RHS_ROUNDING_THRESHOLD);
    }

    private boolean notZeroWithRoundingErrorInLHS(int rowIndex, int colIndex) {
        if (this.LHS[rowIndex][colIndex] == 0.0)
            return false;
        return Math.abs(this.LHS[rowIndex][colIndex]) > Math.abs(LHS_ROUNDING_THRESHOLD[colIndex]);
    }

    /**
     * Checks if the rest of the lhs is any non-zero value left
     *
     * @param startRow    row in which test starts
     * @param startColumn column in which test starts
     * @return true if only 0 values exist, else false
     */
    private boolean restOfLHSisZero(int startRow, int startColumn) {
        for (int row = startRow; row < this.LHS.length; row++) {
            for (int col = startColumn; col < this.LHS[row].length; col++) {
                if (notZeroWithRoundingErrorInLHS(row, col))
                    return false;
            }
        }
        return true;
    }

    /**
     * Reduce the LES by deleting all null-value columns
     */
    private void reduceLES() {
        this.lhsReduced = new double[this.rankCoefficientMatr][this.LHS[0].length];
        this.rhsReduced = new double[this.rankCoefficientMatr];

        System.arraycopy(this.LHS, 0, lhsReduced, 0, this.rankCoefficientMatr);
        System.arraycopy(this.RHS, 0, rhsReduced, 0, this.rankCoefficientMatr);
    }

    /**
     * Solves the LES using back-substitution.
     * Expects the Matrix to be in nxn with a rank of n
     */
    private void solveMatrixInEchelonForm() {
        int n = this.rhsReduced.length;
        if (this.lhsReduced.length != n || this.lhsReduced[0].length != n)
            throw new IllegalStateException("Matrix is incorrectly sized");

        this.solution = new double[n];
        for (int j = n - 1; j >= 0; j--) {
            double t = 0.0;
            for (int k = j + 1; k < n; k++)
                t += this.lhsReduced[j][k] * this.solution[k];
            this.solution[j] =
                    Math.round(((this.rhsReduced[j] - t) / this.lhsReduced[j][j]) * ROUNDING_FACTOR) / ROUNDING_FACTOR;
        }
    }

    /**
     * Test the LES has solutions (could be infinite many).
     * Should not be called before rank has been calculated
     *
     * @return true if at least one solution exist, else false
     */
    private boolean hasSolution() {

        if (rankCoefficientMatr == -1 && rankAugmentedMatr == -1) {
            throw new IllegalStateException("LES has not been calculated yet");
        }
        return rankAugmentedMatr == rankCoefficientMatr;
    }

    /**
     * Test if the LES has a unique solution
     * Should not be called before rank has been calculated
     *
     * @return true if exactly one solution exist, else false
     */
    private boolean hasUniqueSolution() {
        return hasSolution() && this.LHS[0].length == rankCoefficientMatr;
    }

    /**
     * Swaps row i and j in array m
     *
     * @param m the array containing the rows
     * @param i index of first row
     * @param j index of first row
     */
    private void swapRows(double[][] m, int i, int j) {
        if (i == j)
            return;
        for (int col = 0; col < m[0].length; col++) {
            double temp = m[i][col];
            m[i][col] = m[j][col];
            m[j][col] = temp;
        }
    }

    /**
     * Swaps row i and j in array m
     *
     * @param m the array containing the rows
     * @param i index of first row
     * @param j index of first row
     */
    private void swapRows(double[] m, int i, int j) {
        if (i == j)
            return;
        double temp = m[i];
        m[i] = m[j];
        m[j] = temp;
    }

    //Getter and Setter
    public double[] getRhsReduced() {
        return rhsReduced;
    }

    public double[] getSolution() {
        return solution;
    }

    public enum Solubility {
        UNIQUE_SOLUTION,
        INFINITE_SOLUTIONS,
        NO_SOLUTION
    }
}
