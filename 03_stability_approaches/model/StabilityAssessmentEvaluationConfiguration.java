package com.wim.palletizing.model;

public class StabilityAssessmentEvaluationConfiguration {
    public float epsilonTranslation = 0.1f;
    public float epsilonRotation = 10;

    public int resolution = 60;

    public float time = 3.f;

    public float restitution = 0.2f;
    public float friction = 0.5f;
    public float linearDamping = 0f;
    public float angularDamping = 0f;

    public float collisionMargin = 0.01f;

    public String broadPhase = "Dbvt";

    public float itemScalingFactor = 0.01f;
    public float itemHeightOffset = 0.f;


    public StabilityAssessmentEvaluationConfiguration(float epsilonTranslation, int resolution, float time,
                                                      float epsilonRotation, float restitution, float friction,
                                                      float linearDamping, float angularDamping,
                                                      float collisionMargin, String broadPhase,
                                                      float itemScalingFactor, float itemHeightOffset) {
        this.epsilonTranslation = epsilonTranslation;
        this.epsilonRotation = epsilonRotation;
        this.resolution = resolution;
        this.time = time;

        this.restitution = restitution;
        this.friction = friction;
        this.linearDamping = linearDamping;
        this.angularDamping = angularDamping;

        this.collisionMargin = collisionMargin;
        this.broadPhase = broadPhase;

        this.itemScalingFactor = itemScalingFactor;
        this.itemHeightOffset = itemHeightOffset;
    }
    public StabilityAssessmentEvaluationConfiguration(StabilityAssessmentEvaluationConfiguration configuration) {
        this.epsilonTranslation = configuration.epsilonTranslation;
        this.epsilonRotation = configuration.epsilonRotation;
        this.resolution = configuration.resolution;
        this.time = configuration.time;

        this.restitution = configuration.restitution;
        this.friction = configuration.friction;
        this.linearDamping = configuration.linearDamping;
        this.angularDamping = configuration.angularDamping;

        this.collisionMargin = configuration.collisionMargin;
        this.broadPhase = configuration.broadPhase;
        this.itemScalingFactor = configuration.itemScalingFactor;
        this.itemHeightOffset = configuration.itemHeightOffset;
    }
    protected StabilityAssessmentEvaluationConfiguration() {}
}