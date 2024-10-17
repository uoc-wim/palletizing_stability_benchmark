package com.wim.assessment.staticStability.sme.ForceLogic.ForceCalculation;

import com.wim.palletizing.geometry.dim3.Point3D;

public class RequierementCheckService {
    public static void asserCOMIsCorrect(String label, int width, int height, int depth, Point3D centerOfMass) {
        boolean correct = true;
        if(Math.abs(width)/2.0 < Math.abs(centerOfMass.x))
            correct = false;
        else if (Math.abs(height)/2.0 < Math.abs(centerOfMass.y))
            correct = false;
        else if (Math.abs(depth)/2.0 < Math.abs(centerOfMass.z))
            correct = false;

        if(! correct)
            throw new IllegalArgumentException("COM is outside of Element "+label);
    }
}
