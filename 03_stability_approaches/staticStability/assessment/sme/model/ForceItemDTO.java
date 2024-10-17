package com.wim.assessment.staticStability.sme.model;

import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.helper.Pair;
import com.wim.palletizing.model.item.PlacedItem;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Validated
public class ForceItemDTO {


    /**
     * The placedItem the ForceItem represents
     */
    private final PlacedItem PLACED_ITEM;

    /**
     * The force and its acting point of the gravitation of the item
     */
    private final Pair<Point2D, Double> GRAVITATION_FORCE;

    /**
     * The acting points along with the corresponding items below of the reaction forces
     */
    private final List<Pair<Point2D, PlacedItem>> REACTION_POINTS;

    /**
     * The action forces and acting points , grouped as list for every
     * sequenceStep
     */
    private final Map<Integer, List<Pair<Point2D, Double>>> ACTION_FORCES_PER_SEQUENCE;

    /**
     * The resultant force magnitude and its acting point
     * for every sequenceStep
     */
    private final Map<Integer, Pair<Point2D, Double>> RESULTANT_FORCE_PER_SEQUENCE;

    /**
     * The reactionForces (Point and magnitude) mapped to the reacting item and the sequence
     */
    private final Map<Integer, Map<String, List<Pair<Point2D, Double>>>> REACTION_FORCES_PER_SEQUENCE;

    /**
     * Containing true for every sequence the item is stable in, false if unstable and null if it doesn't exist
     */
    private final Boolean[] STABILITY_PER_SEQUENCE;

    public ForceItemDTO(@NotNull PlacedItem placedItem,
                        Pair<Point2D, Double> gravitationForce, List<Pair<Point2D, PlacedItem>> reactionPoints,
                        Map<Integer, List<Pair<Point2D, Double>>> actionForcesPerSequence, Map<Integer,
            Pair<Point2D, Double>> resultantForcePerSequence, Map<Integer, Map<String, List<Pair<Point2D,
            Double>>>> reactionForcesPerSequence, Boolean[] stabilityPerSequence) {
        this.PLACED_ITEM = placedItem;
        this.GRAVITATION_FORCE = gravitationForce;
        this.REACTION_POINTS = reactionPoints;
        this.ACTION_FORCES_PER_SEQUENCE = actionForcesPerSequence;
        this.RESULTANT_FORCE_PER_SEQUENCE = resultantForcePerSequence;
        this.REACTION_FORCES_PER_SEQUENCE = reactionForcesPerSequence;
        this.STABILITY_PER_SEQUENCE = stabilityPerSequence;
    }


    public PlacedItem getPLACED_ITEM() {
        return PLACED_ITEM;
    }

    public Pair<Point2D, Double> getGRAVITATION_FORCE() {
        return GRAVITATION_FORCE;
    }

    public List<Pair<Point2D, PlacedItem>> getREACTION_POINTS() {
        return REACTION_POINTS;
    }

    public Map<Integer, List<Pair<Point2D, Double>>> getACTION_FORCES_PER_SEQUENCE() {
        return ACTION_FORCES_PER_SEQUENCE;
    }

    public Map<Integer, Pair<Point2D, Double>> getRESULTANT_FORCE_PER_SEQUENCE() {
        return RESULTANT_FORCE_PER_SEQUENCE;
    }

    public Map<Integer, Map<String, List<Pair<Point2D, Double>>>> getREACTION_FORCES_PER_SEQUENCE() {
        return REACTION_FORCES_PER_SEQUENCE;
    }

    public Boolean[] getSTABILITY_PER_SEQUENCE() {
        return STABILITY_PER_SEQUENCE;
    }
}
