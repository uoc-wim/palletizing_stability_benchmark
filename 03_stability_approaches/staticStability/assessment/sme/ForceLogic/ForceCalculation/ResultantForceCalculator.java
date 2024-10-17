package com.wim.assessment.staticStability.sme.ForceLogic.ForceCalculation;

import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.helper.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Frederick Gamer
 * created February 2022
 * Service to calculate the resultantForce of an item
 */
public class ResultantForceCalculator {

    /**
     * Calculates the resultingForce of the item for all sequences from when the item is placed until the given sequence.
     * Uses the given gravitationForce and the given actionForces of each sequence for the computation
     *
     * @param sequence                the sequence from which on the resultingForce is calculated
     * @param maxSequence             the sequence until the resultingForce is calculated
     * @param gravitationForce        the gravitationForce of the item
     * @param actionForcesPerSequence the map containing the actionForces of every sequence the resultingForce
     *                                should be calculated for
     * @return the calculated resultantForce (Point and Magnitude) mapped to the sequences
     */
    protected static Map<Integer, Pair<Point2D, Double>>
    calculateResultantForcesForAllSequences(int sequence, int maxSequence, Pair<Point2D,
            Double> gravitationForce, Map<Integer, List<Pair<Point2D, Double>>> actionForcesPerSequence) {

        Map<Integer, Pair<Point2D, Double>> resultantForcePerSequenceLocal = new HashMap<>();

        for (int i = sequence; i < maxSequence; i++)
            resultantForcePerSequenceLocal.put(i, ResultantForceCalculator.calculateResultantForce(gravitationForce,
                    actionForcesPerSequence.containsKey(i) ? actionForcesPerSequence.get(i) : new LinkedList<>() {
                    }));

        return resultantForcePerSequenceLocal;
    }

    /**
     * For a given sequence the resultant force is calculated by the weighted mean
     * of the points of actionForces and gravitationForce and summing up their
     * forces
     *
     * @param gravitationForce the gravitationForce(Point and magnitude) of the item
     * @param actionForces     the actionForces acting on the item of one sequence
     * @return the calculated resultant force consisting of the point2d and the magnitude as double
     */
    private static Pair<Point2D, Double> calculateResultantForce(Pair<Point2D,
            Double> gravitationForce, List<Pair<Point2D, Double>> actionForces) {
        //Add gravitation force of item
        double resultantForce = gravitationForce.second;

        double x = gravitationForce.first.x * gravitationForce.second;
        double y = gravitationForce.first.y * gravitationForce.second;

        //Add all action forces (if any exist)
        for (Pair<Point2D, Double> actionForce : actionForces) {
            resultantForce += actionForce.second;

            x += actionForce.first.x * actionForce.second;
            y += actionForce.first.y * actionForce.second;
        }

        x = Math.round(x / resultantForce * 100) / 100.0;
        y = Math.round(y / resultantForce * 100) / 100.0;

        //Divide by overall magnitude to get non-weighted point
        Point2D resultantForcePoint = new Point2D(x, y);

        return new Pair<>(resultantForcePoint, resultantForce);
    }

}
