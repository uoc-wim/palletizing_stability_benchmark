package com.wim.assessment.staticStability.physicalSimulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.palletizing.assessment.AssessmentCriterionType;
import com.wim.palletizing.assessment.staticStability.physicalSimulation.simulation.VisualizedPhysicalSimulation;
import com.wim.palletizing.model.StabilityAssessmentEvaluationConfiguration;
import com.wim.palletizing.model.ULD;

public class VisualizedPhysicalSimulationCriterion extends PhysicalSimulationCriterion {

    public static final AssessmentCriterionType type = AssessmentCriterionType.PHYSICAL_SIMULATION_VISUALIZED;

    public VisualizedPhysicalSimulationCriterion(@JsonProperty(value = "weight", required = true) double weight,
                                                 @JsonProperty(value = "stabilityConfiguration", required = true) StabilityAssessmentEvaluationConfiguration configuration)
    {
        super(weight, configuration);
    }
    @Override
    protected double assessUld(ULD uld) {
        VisualizedPhysicalSimulation vis = new VisualizedPhysicalSimulation(uld, this.configuration);
        vis.simulate(this.configuration.epsilonTranslation,
                this.configuration.resolution,
                this.configuration.time, this.configuration.epsilonRotation);
        return 0;
    }

}
