package com.wim.assessment.staticStability.baseSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.assessment.AbstractAssessmentCriterion;
import com.wim.assessment.AssessmentCriterionType;
import com.wim.model.ULD;
import com.wim.model.item.InputItemSet;
import com.wim.model.item.PlacedItem;
import com.wim.model.uld_properties.ULDProperties;
import org.springframework.data.annotation.PersistenceConstructor;

import javax.persistence.Entity;
import java.util.*;
import java.util.List;

@Entity
public class FullBaseSupportCriterion extends AbstractAssessmentCriterion {

    public static final AssessmentCriterionType type = AssessmentCriterionType.FULL_BASE_SUPPORT;

    protected double minBaseSupportFactor = 1.0d;
    public static final double EPSILON = 0.001d;

    @PersistenceConstructor
    private FullBaseSupportCriterion(Long id, double weight) {
        super(id, weight);
    }

    @JsonCreator
    public FullBaseSupportCriterion(@JsonProperty (value = "weight", required = true) double weight){
        this(null,weight);
    }

    /**
     * Hibernate workaround
     */
    @SuppressWarnings("unused")
    private FullBaseSupportCriterion()
    {
        this(null, -1);
    }

    @Override
    public AssessmentCriterionType getType() {
        return type;
    }

    @Override
    public void preProcessing(InputItemSet inputItemSet, HashMap enrichments, List<ULDProperties> uldPropertiesSet) {

    }

    @Override
    public double assess(Set<ULD> uldSet) {
        return super.assess(uldSet);
    }

    @Override
    protected double assessUld(ULD uld) {

        assureULDSupportStructureHasBeenCalculated(uld);

        int nItems = uld.getItemCount();
        if (nItems == 0) return 1.0;
        for (PlacedItem item: uld.getPlacedItemsSorted()){
            double baseSupportFactor = item.getBaseSupportFactor();
            if (Math.abs(baseSupportFactor - this.minBaseSupportFactor) <= EPSILON) continue;
            if (baseSupportFactor < this.minBaseSupportFactor) return (double) item.sequence / nItems;
        }
        return 1.0;
    }

    public static void assureULDSupportStructureHasBeenCalculated(ULD uld) {
        if (uld.getPlacedItems().isEmpty())
            return;

        PlacedItem testItem = uld.getPlacedItems().values().iterator().next();

        if (testItem.getEnvironmentRelations().getItemsBelow().isEmpty() && testItem.getEnvironmentRelations().getItemsOnTop().isEmpty())
            uld.calculateItemSupportStructure();
    }

    @Override
    public AbstractAssessmentCriterion copy() {
        return new FullBaseSupportCriterion(this.weight);
    }
}
