package com.wim.assessment.staticStability.sme;

import com.wim.palletizing.assessment.staticStability.sme.AssessmentScoring.AssessmentScorer;
import com.wim.palletizing.assessment.staticStability.sme.AssessmentScoring.LowestSequenceScoring;
import com.wim.palletizing.assessment.staticStability.sme.ForceLogic.ForceItemManager;
import com.wim.palletizing.assessment.staticStability.sme.model.ForceItemDTO;
import com.wim.palletizing.model.item.PlacedItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.TopologyException;

import java.util.Arrays;
import java.util.List;

public class ScienceOfStaticAssessmentService {

    private static final Logger logger = LogManager.getLogger();


    AssessmentScorer assessmentScorer = new LowestSequenceScoring();

    public double assesItemList(List<PlacedItem> itemList) {

        ForceItemManager fiM = new ForceItemManager(itemList.size());


        Boolean[][] stabilityMatrix = new Boolean[itemList.size()][itemList.size()];

        try {
            for (int i = 0; i < itemList.size(); i++) {

                stabilityMatrix[i] = testItemsStability(itemList.get(i), fiM);

                if (assessmentScorer.shouldOptimize()) {
                    fiM.setMaxSequence(assessmentScorer.optimizeMaxSequence(stabilityMatrix[i]));
                }
            }
        } catch (TopologyException e) {
            logger.warn("A item with " + e.getMessage() + " of the set is wrongly shaped, can't be handled by library");
            return -1;
        } catch (IllegalArgumentException e){
            logger.warn(e.getMessage() + ". No stability assessment possible");
            return -1;
        }

        return assessmentScorer.scoreStabilityMatrix(stabilityMatrix);

    }

    /**
     * Tests for all sequences the item is already placed if it is stable.
     *
     * @param item the item which stability is tested
     * @param fiM  The forceItemManager used to create the forceItems
     * @return an array of booleans in the size of the maxSequence, representing the stability of the item per sequence
     * with the following mapping: true -> stable, false -> unstable, null -> item not placed in the sequence
     */
    private Boolean[] testItemsStability(PlacedItem item, ForceItemManager fiM) {
        //for bottom item stability is guaranteed and does not need to be checked
        if (item.isBottomItem()) {
            Boolean[] stableBooleans = new Boolean[fiM.getMaxSequence()];
            Arrays.fill(stableBooleans, true);
            return stableBooleans;
        }

        ForceItemDTO forceItem = fiM.getOrCreateForceItemFromPlacedItem(item);

        return forceItem.getSTABILITY_PER_SEQUENCE();
    }



}
