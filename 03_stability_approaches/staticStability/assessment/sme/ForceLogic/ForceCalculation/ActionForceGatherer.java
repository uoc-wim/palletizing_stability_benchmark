package com.wim.assessment.staticStability.sme.ForceLogic.ForceCalculation;

import com.wim.palletizing.assessment.staticStability.sme.model.ForceItemDTO;
import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.helper.Pair;

import java.util.*;

/**
 * @auhor Frederick Gamer
 * created February 2022
 * Service to gather the actionForces for an forceItem
 */
public class ActionForceGatherer {

    /**
     * Gathers all action forces acting on the item for the given sequence range.
     * Runs for each sequence through the items onTop and takes their reactionForces acting on this item
     *
     * @param itemLabel the label identifying the item for which the actionForce is gathered
     * @param startSequence the sequence from which on the forces are calculated
     * @param forceItemsOnTop the forceItemDTOs from which forces are gathered
     * @param maxSequence the sequence until which the forces are calculated
     * @return the actionForces (point and magnitude) mapped to each itemLabel and to the sequence
     */
    protected static Map<Integer, List<Pair<Point2D, Double>>> gatherRelatedActionForcesFromTopItems(
            String itemLabel, int startSequence, List<ForceItemDTO> forceItemsOnTop, int maxSequence) {
        if (forceItemsOnTop.isEmpty())
            return new HashMap<>();

        Map<Integer, List<Pair<Point2D, Double>>> actionForcesPerSequence = new HashMap<>();

        for (int s = startSequence; s < maxSequence; s++) {
            List<Pair<Point2D, Double>> actionForces = new ArrayList<>();

            for (ForceItemDTO forceItem : forceItemsOnTop) {

                actionForces.addAll(ActionForceGatherer.getReactionForceForItemAndSequence(forceItem, itemLabel,
                        s));
            }
            actionForcesPerSequence.put(s, actionForces);

        }
        return actionForcesPerSequence;
    }

    /**
     * Returns the reactionForces (Point and Magnitude) of this sequence acting on the given item.
     * Returns an empty list if no exist
     *
     * @param forceItem the forceItemDTO from which forces are gathered
     * @param itemLabel the label identifying the item  for which the actionForce is gathered
     * @param sequence the sequence for which the force is gathered
     * @return the actionForces (point and magnitude)
     */
    private static Collection<? extends Pair<Point2D, Double>> getReactionForceForItemAndSequence(
            ForceItemDTO forceItem, String itemLabel, int sequence) {
        if (forceItem.getREACTION_FORCES_PER_SEQUENCE().containsKey(sequence))
            if (forceItem.getREACTION_FORCES_PER_SEQUENCE().get(sequence).containsKey(itemLabel))
                return forceItem.getREACTION_FORCES_PER_SEQUENCE().get(sequence).get(itemLabel);

        return new ArrayList<>();
    }
}
