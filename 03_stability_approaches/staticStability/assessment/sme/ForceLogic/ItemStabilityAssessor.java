package com.wim.assessment.staticStability.sme.ForceLogic;

import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.helper.Pair;

import java.util.List;
import java.util.Map;

/**
 * @author Frederick Gamer
 * created February 2022
 * <p>
 * Service to assess in which sequence a item was stable, instable or not placed
 */
public class ItemStabilityAssessor {


    /**
     * Asses the stability of an item for each sequence based on the fact if item has reactionForces in the sequence
     *
     * @param startSequence             the sequence to start checking
     * @param maxSequence               the sequence until which it is checked
     * @param reactionForcesPerSequence the reactionForces of the item for which the stability is assessed
     * @return an array containing for each sequence true, if the item is stable, false if unstable and null if not
     * placed
     */
    public static Boolean[] getStabilityFromReactionForcesPerSequence(int startSequence, int maxSequence, Map<Integer
            , Map<String, List<Pair<Point2D, Double>>>> reactionForcesPerSequence) {
        Boolean[] stabilityPerSequence = new Boolean[maxSequence];

        for (int i = startSequence; i < maxSequence; i++) {
            stabilityPerSequence[i] = reactionForcesPerSequence.containsKey(i);
        }
        return stabilityPerSequence;
    }
}
