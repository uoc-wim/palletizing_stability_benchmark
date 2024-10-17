package com.wim.assessment.staticStability.sme.ForceLogic.ForceCalculation;

import com.wim.palletizing.assessment.staticStability.sme.helper.LinearEquationSystem;
import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.helper.Pair;
import com.wim.palletizing.model.item.PlacedItem;

import java.util.*;

/**
 * @author Frederick Gamer
 * created February 2022
 * Service to calculate the reactionForces at the reactionPoints of an item
 */
public class ReactionForceCalculator {

    /**
     * Calculates the reactionForces for each startSequence from when the item is placed to the maxSequence,
     * grouped and mapped to the item they act on and mapped to each startSequence
     *
     * @param startSequence                  the sequence from which on the reactionForces are calculated
     * @param maxSequence               the sequence until which the reactionForces are calculated
     * @param reactionPoints            the points along with the item of the reactionForce
     * @param resultantForcePerSequence the resultantForces of all sequences that should be calculated
     * @return the reactionForce(Point and Magnitude) grouped as list and mapped to the item they act on, again mapped
     * to the sequence they are active in
     */
    protected static Map<Integer, Map<String, List<Pair<Point2D, Double>>>>
    calculateReactionForces(int startSequence, int maxSequence, List<Pair<Point2D, PlacedItem>> reactionPoints,
                            Map<Integer, Pair<Point2D, Double>> resultantForcePerSequence) {
        Map<Integer, Map<String, List<Pair<Point2D, Double>>>> reactionForcesPerSequence = new HashMap<>();
        for (int i = startSequence; i < maxSequence; i++) {
            Map<String, List<Pair<Point2D, Double>>> reactionForces =
                    ReactionForceCalculator.calculateReactionForcesForSequence(i,
                    reactionPoints, resultantForcePerSequence.get(i));
            if (reactionForces != null) {
                reactionForcesPerSequence.put(i, reactionForces);
            }
        }

        return reactionForcesPerSequence;
    }

    /**
     * For the given sequence  the given resultantForce is distributed to
     * the reactionPoints existing at that sequence depending on the distance of the reacting points.
     * If no calculation is possible, the item is expected to be unstable and this state is saved in the object
     * The resulting reactionForces are returned.
     *
     * @param sequence       the current sequence number for which the reactionForces are calculated
     * @param reactionPoints the points along with the item of the reactionForce
     * @param resultantForce the resultantForce (Point and magnitude) of the item
     * @return the reactionForce(Point and Magnitude) grouped as list and mapped to the item they act on
     */
    private static Map<String, List<Pair<Point2D, Double>>>
    calculateReactionForcesForSequence(int sequence, List<Pair<Point2D, PlacedItem>> reactionPoints,
                                       Pair<Point2D, Double> resultantForce) {
        if(reactionPoints.size() == 0){
            return null;
        }


        //Sort the reactionPoints based on the distance to the resultantPoint
        ReactionForceCalculator.sortReactionPointsByDistanceToResultantForce(reactionPoints, resultantForce);
        // Creates a LES using the distances between reactionPoints and the resultantForce
        LinearEquationSystem les = ReactionForceCalculator.createLES(sequence, reactionPoints, resultantForce);

        LinearEquationSystem.Solubility solubility = les.solveLesAsPossible();

        switch (solubility) {
            case UNIQUE_SOLUTION:
                return ReactionForceCalculator.readReactionForcesFromLES(les, reactionPoints);
            case INFINITE_SOLUTIONS:
                return ReactionForceCalculator.handleIndeterminateSystem(les, reactionPoints, resultantForce);
            case NO_SOLUTION:
                return null;
            default:
                throw new IllegalArgumentException("Feedback of LES-solving can't be handled");
        }
    }

    /**
     * Sort the reactionPoints based on the distance to the resultantPoint
     *
     * @param reactionPoints to be sorted
     * @param resultantForce given the point the distance is calculated to
     */
    private static void sortReactionPointsByDistanceToResultantForce(List<Pair<Point2D,
            PlacedItem>> reactionPoints, Pair<Point2D, Double> resultantForce) {
        reactionPoints.sort((o1, o2) -> {
            double distanceO1 = resultantForce.first.distance(o1.first);
            double distanceO2 = resultantForce.first.distance(o2.first);
            return Double.compare(distanceO1, distanceO2);
        });
    }

    /**
     * Creates a LES using all reactionPoints and the resultant Force of the given Sequence
     * The LES contains on the left-hand-side all distances between the reactionForces (each on X and Z) and on the
     * right-hand-side the distance to the resultantForce times the magnitude of the force
     *
     * @param sequence       the sequence setting which reactionPoints to consider
     * @param reactionPoints the reactionPoints used to create the LES
     * @param resultantForce the resultantForce(Point and magnitude) used to create the LES
     * @return a LES containing all distance equations
     */
    private static LinearEquationSystem createLES(int sequence, List<Pair<Point2D, PlacedItem>> reactionPoints,
                                                            Pair<Point2D, Double> resultantForce) {
        //The left-hand side matrix, containing all the distance from one reactionPoint to the other
        //Each column contains the coefficients of one variable that is to solve
        double[][] lhs = new double[reactionPoints.size() * 2][reactionPoints.size()];

        //The right-hand side matrix, containing the value the lhs should achieve
        //Value is the magnitude of the resultant force times the distance to the specific reactionForcePoint
        double[] rhs = new double[reactionPoints.size() * 2];

        //Create matrix (2n x n) for n reactionForcePoints
        for (int i = 0; i < reactionPoints.size(); i++) {

            //Skips all reactionForcePoints not existing placed at the given sequence
            if (reactionPoints.get(i).second.sequence > sequence)
                continue;

            //Calculate moments on x-Axis (2i) and z-Axis (2i+1)
            for (int j = 0; j < reactionPoints.size(); j++) {

                //Skips reactionForcePoints not placed yet or if it's the same then in outer loop, array contains 0 per default
                if (reactionPoints.get(i).second.sequence > sequence || i == j)
                    continue;

                //distance on x
                lhs[2 * i][j] = reactionPoints.get(i).first.x - reactionPoints.get(j).first.x;
                //distance on z-axis
                lhs[2 * i + 1][j] = reactionPoints.get(i).first.y - reactionPoints.get(j).first.y;
            }
            //distance of resultant Force on x-axis
            rhs[2 * i] = (reactionPoints.get(i).first.x - resultantForce.first.x)
                    * resultantForce.second;
            rhs[2 * i] = Math.round(rhs[2 * i] * 100) / 100.0;
            //distance of resultant Force on y-axis
            rhs[2 * i + 1] = (reactionPoints.get(i).first.y - resultantForce.first.y)
                    * resultantForce.second;
            rhs[2 * i + 1] = Math.round(rhs[2 * i + 1] * 100) / 100.0;

        }

        return new LinearEquationSystem(lhs, rhs);
    }

    /**
     * Matching the results of the linear equation system to the reactionPoints to
     * create reactionForces with points and magnitude mapped to the item the act on
     *
     * @param les            the solved linear equation system,
     * @param reactionPoints the reactionPoints the LES is solved for
     * @return the reactionForces
     */
    private static Map<String, List<Pair<Point2D, Double>>> readReactionForcesFromLES(
            LinearEquationSystem les, List<Pair<Point2D, PlacedItem>> reactionPoints) {
        double[] forceSolution = les.getSolution();

        //Save the reactionForces (Point and Magnitude) along with the corresponding item
        Map<String, List<Pair<Point2D, Double>>> reactionForces = new HashMap<>();

        for (int i = 0; i < reactionPoints.size(); i++) {
            if (!reactionForces.containsKey(reactionPoints.get(i).second.itemLabel))
                reactionForces.put(reactionPoints.get(i).second.itemLabel, new LinkedList<>());
            reactionForces.get(reactionPoints.get(i).second.itemLabel).add(new Pair<>(reactionPoints.get(i).first,
                    forceSolution[i]));
        }

        return reactionForces;
    }

    /**
     * Determines three reactionPoints out of all which form a stable support and calculates the system for them
     *
     * @param les            the indeterminate equation system
     * @param reactionPoints all reaction points
     * @param resultantForce the resultantForce of the item
     * @return the calculated reactionForces or an empty list, if no calculation is possible
     */
    private static Map<String, List<Pair<Point2D, Double>>> handleIndeterminateSystem(
            LinearEquationSystem les, List<Pair<Point2D, PlacedItem>> reactionPoints,
            Pair<Point2D, Double> resultantForce) {

        //Check if all points belong to one item and distribute force equally if so
//        if (ReactionForceCalculator.reactionPointsBelongToOneItem(reactionPoints)) {
//            return ReactionForceCalculator.distributeForceEqually(reactionPoints, resultantForce);
//        }

        //Otherwise, the three points that are closed to the reactionForce and stable are determined

        //As only items being in different quadrant can be a stable support, the quadrants are used to speed the
        //process up
        int[] reactionPointQuadrants = ReactionForceCalculator.deriveQuadrantsOfReactionPoints(reactionPoints,
                resultantForce);
        Map<String, List<Pair<Point2D, Double>>> reactionForces;

        for (int i = 2; i < reactionPoints.size(); i++) {
            for (int c1 = 0; c1 < i; c1++) {
                if (reactionPointQuadrants[c1] == reactionPointQuadrants[i])
                    continue;
                for (int c2 = c1 + 1; c2 < i; c2++) {
                    if (reactionPointQuadrants[c2] == reactionPointQuadrants[i] ||
                            reactionPointQuadrants[c2] == reactionPointQuadrants[c1])
                        continue;

                    reactionForces = ReactionForceCalculator.tryCalculatingReactionForcesForTriple(reactionPoints,
                            Arrays.asList(c1, c2, i), les);

                    if (reactionForces != null) {
                        return reactionForces;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if all reactionPoints belong to the same item
     *
     * @param reactionPoints the points checked
     * @return true, if all reactionPoints belong to the same item. false else
     */
    private static boolean reactionPointsBelongToOneItem(List<Pair<Point2D, PlacedItem>> reactionPoints) {

        Pair<Point2D, PlacedItem> reactionPoint1 = reactionPoints.get(0);
        for (Pair<Point2D, PlacedItem> reactionPoint2 : reactionPoints) {
            if (!reactionPoint1.second.equals(reactionPoint2.second))
                return false;
        }
        return true;
    }

    /**
     * Creates the reactionForces by distributing the resultant Force equally to the reactionPoints
     *
     * @param reactionPoints the list of points
     * @param resultantForce the force distributed to the points
     * @return the reactionForces
     */
    private static Map<String, List<Pair<Point2D, Double>>> distributeForceEqually(
            List<Pair<Point2D, PlacedItem>> reactionPoints, Pair<Point2D, Double> resultantForce) {
        Map<String, List<Pair<Point2D, Double>>> reactionForces = new HashMap<>();

        for (int i = 0; i < reactionPoints.size(); i++) {
            if (!reactionForces.containsKey(reactionPoints.get(i).second.itemLabel))
                reactionForces.put(reactionPoints.get(i).second.itemLabel, new LinkedList<>());
            double force = Math.round(resultantForce.second / reactionPoints.size() * 100) / 100.0;
            reactionForces.get(reactionPoints.get(i).second.itemLabel).add(new Pair<>(reactionPoints.get(i).first, force));
        }

        return reactionForces;
    }

    /**
     * Deriving a positional quadrant for each reactionPoint depending on it's position to the resultantForce point:
     * x<= & y<=  => quadrant 0
     * x<= & y>   => quadrant 1
     * x> & y<=   => quadrant 2
     * x> & y>    => quadrant 3
     *
     * @param reactionPoints the points the quadrant is derived for
     * @param resultantForce giving the point the points are rated relative to
     * @return an array of ints, showing the positional quadrant of the reaction Points
     */
    private static int[] deriveQuadrantsOfReactionPoints(List<Pair<Point2D,
            PlacedItem>> reactionPoints, Pair<Point2D, Double> resultantForce) {
        int[] reactionPointQuadrant = new int[reactionPoints.size()];

        for (int i = 0; i < reactionPoints.size(); i++) {
            if (reactionPoints.get(i).first.x >= resultantForce.first.x)
                reactionPointQuadrant[i] += 2;
            if (reactionPoints.get(i).first.y >= resultantForce.first.y)
                reactionPointQuadrant[i] += 1;
        }
        return reactionPointQuadrant;
    }

    /**
     * Test if system is stable for three points and solves system if so
     *
     * @param reactionPoints all reactionPoints existing
     * @param indices        the indices of the reactionPoints to consider
     * @param lesLarge       the LES containing all reactionPoints
     * @return the reactionForces if possible to calculate or null
     */
    private static Map<String, List<Pair<Point2D, Double>>> tryCalculatingReactionForcesForTriple(List<Pair<Point2D,
            PlacedItem>> reactionPoints, List<Integer> indices, LinearEquationSystem lesLarge) {

        LinearEquationSystem lesTriple = new LinearEquationSystem(lesLarge.getSpecificColumnsOfLHS(indices),
                lesLarge.getRhsReduced());

        if (lesTriple.solveLesAsPossible() == LinearEquationSystem.Solubility.UNIQUE_SOLUTION)
            return readReactionForcesFromLES(lesTriple,
                    ReactionForceCalculator.getSpecificReactionPoints(reactionPoints, indices));

        return null;
    }

    /**
     * Creates a new list only containing the reactionPoints specified by the indices
     *
     * @param reactionPoints a list containing the reactionPoints to filter
     * @param indices        containing the indices of the reactionPoints to filter
     * @return a list with only the reactionPoints with the indices given
     */
    private static List<Pair<Point2D, PlacedItem>> getSpecificReactionPoints(
            List<Pair<Point2D, PlacedItem>> reactionPoints, List<Integer> indices) {
        List<Pair<Point2D, PlacedItem>> reducedReactionPoints = new ArrayList<>();

        for (int i : indices) {
            reducedReactionPoints.add(reactionPoints.get(i));
        }
        return reducedReactionPoints;
    }

}
