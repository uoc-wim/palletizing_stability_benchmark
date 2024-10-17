package com.wim.assessment.staticStability.physicalSimulation.simulation;

import com.wim.palletizing.assessment.AssessmentCriterionType;
import com.wim.palletizing.model.StabilityAssessmentEvaluationConfiguration;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.physics.VisualizationLWGL3;

public class VisualizedPhysicalSimulation extends PhysicalSimulation {

    public static final AssessmentCriterionType type = AssessmentCriterionType.PHYSICAL_SIMULATION_VISUALIZED;

    public VisualizedPhysicalSimulation(ULD uld, StabilityAssessmentEvaluationConfiguration config) {
        super(uld, config);
    }
    @Override
    public double simulate(float epsilonTranslation, int resolution, float time, float epsilonRotation) {
        new VisualizationLWGL3(this.physicsULD.getItemListTotal(), this.physicsWorld, physicsULD);
        return 0;
    }
}
