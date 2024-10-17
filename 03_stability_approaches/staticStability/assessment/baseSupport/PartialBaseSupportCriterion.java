package com.wim.assessment.staticStability.baseSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.assessment.AbstractAssessmentCriterion;
import com.wim.assessment.AssessmentCriterionType;

public class PartialBaseSupportCriterion extends FullBaseSupportCriterion {

    public static final AssessmentCriterionType type = AssessmentCriterionType.PARTIAL_BASE_SUPPORT;

    private double alpha = 0.5;

    @JsonCreator
    public PartialBaseSupportCriterion(@JsonProperty(value = "weight", required = true) double weight,
                                       @JsonProperty(value = "alpha", required = true) double alpha) {
        super(weight);
        this.alpha = alpha;
        this.minBaseSupportFactor = this.alpha;
    }

    @Override
    public AbstractAssessmentCriterion copy() {
        return new PartialBaseSupportCriterion(this.weight, this.alpha);
    }

    @Override
    public AssessmentCriterionType getType() {
        return AssessmentCriterionType.PARTIAL_BASE_SUPPORT;
    }

    public double getAlpha() {
        return alpha;
    }
}
