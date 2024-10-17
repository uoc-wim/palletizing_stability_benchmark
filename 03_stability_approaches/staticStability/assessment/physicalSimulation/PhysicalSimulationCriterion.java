package com.wim.assessment.staticStability.physicalSimulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.palletizing.assessment.AbstractAssessmentCriterion;
import com.wim.palletizing.assessment.AssessmentCriterionType;
import com.wim.palletizing.assessment.staticStability.physicalSimulation.simulation.PhysicalSimulation;
import com.wim.palletizing.model.StabilityAssessmentEvaluationConfiguration;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.model.item.InputItemSet;
import com.wim.palletizing.model.uld_properties.ULDProperties;
import org.springframework.data.annotation.PersistenceConstructor;

import javax.persistence.Entity;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by Philipp Mazur for TR_C on 25.09.2023
 */
@Entity
public class PhysicalSimulationCriterion extends AbstractAssessmentCriterion
{
    public static final AssessmentCriterionType type = AssessmentCriterionType.PHYSICAL_SIMULATION;
    protected transient StabilityAssessmentEvaluationConfiguration configuration;
    @PersistenceConstructor
    private PhysicalSimulationCriterion(Long id, double weight)
    {
        super(id, weight);
    }

    //@JsonCreator
    public PhysicalSimulationCriterion(@JsonProperty(value = "weight", required = true) double weight,
                                       @JsonProperty(value = "stabilityConfiguration", required = true) StabilityAssessmentEvaluationConfiguration configuration)
    {
        this(null, weight);
        this.configuration = configuration;
    }

    /**
     * Hibernate workaround
     */
    @SuppressWarnings("unused")
    private PhysicalSimulationCriterion()
    {
        this(null, -1);
    }



    @Override
    public AssessmentCriterionType getType()
    {
        return type;
    }

    @Override
    public void preProcessing(InputItemSet inputItemSet, HashMap enrichments, List<ULDProperties> uldPropertiesSet)
    {
    }

    @Override
    public double assess(Set<ULD> uldSet) {
        return super.assess(uldSet);
    }

    @Override
    protected double assessUld(ULD uld) {
        if(uld.isEmpty())
            return 1.0;
        PhysicalSimulation simulation = new PhysicalSimulation(uld, this.configuration);
        return simulation.simulate(this.configuration.epsilonTranslation,
                        this.configuration.resolution,
                        this.configuration.time, this.configuration.epsilonRotation);
    }
    @Override
    public AbstractAssessmentCriterion copy() {
        return new PhysicalSimulationCriterion(this.weight, this.configuration);
    }



    // Public Setter vor JSON Construction
    public float getEpsilonTranslation() {
        return this.configuration.epsilonTranslation;
    }

    public int getResolution() {
        return this.configuration.resolution;
    }

    public float getTime() {
        return this.configuration.time;
    }

    public float getEpsilonRotation() {
        return this.configuration.epsilonRotation;
    }

    public float getRestitution() {
        return this.configuration.restitution;
    }

    public float getFriction() {
        return this.configuration.friction;
    }

    public float getLinearDamping() {
        return this.configuration.linearDamping;
    }

    public float getAngularDamping() {
        return this.configuration.angularDamping;
    }

    public float getCollisionMargin() {return this.configuration.collisionMargin;}
    public String getBroadphase() {return this.configuration.broadPhase;}

    public float getItemScalingFactor() {return this.configuration.itemScalingFactor;}
    public float getItemHeightOffset() {return this.configuration.itemHeightOffset;}
}

