package com.wim.assessment.staticStability.sme.ForceLogic.ForceCalculation;

import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.geometry.dim3.Point3D;
import com.wim.palletizing.helper.Pair;

/**
 * @author Frederick Gamer
 * created Feburary 2022
 *
 * Service to calculate the gravitationForce
 */
public class GravitationForceCalculator {


    /**
     * Using the centerOfMass and the g-force the gravitation force in point and magnitude is calculated
     * @param cOM the centerOfMass
     * @param weight the weight of the element
     * @return the gravitationForce with actingPoint and magnitude
     */
    public static Pair<Point2D, Double> calculateGravitationForce(Point3D cOM, double weight) {

        double g = 9.81;
        double force = Math.round(weight * g * 100) / 100.0;

        return new Pair<>(new Point2D(cOM.x, cOM.z), force);
    }

}
