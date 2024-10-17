package com.wim.assessment.staticStability.sme.ForceLogic.ForceCalculation;

import com.wim.palletizing.assessment.staticStability.sme.ForceLogic.ItemStabilityAssessor;
import com.wim.palletizing.assessment.staticStability.sme.model.ForceItemDTO;
import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.helper.Pair;
import com.wim.palletizing.model.item.PlacedItem;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @auhor Frederick Gamer
 * created February 2022
 * service to create ForceItemDTOs
 */
@Validated
public class ForceItemCalculation {


    /**
     * Trigger services to calculate the forceItemInformation.
     * @param placedItem the element representing the item which ForceItem is calculated
     * @param forceItemsOnTop the forceItems being on Top of this item
     * @param maxSequence the sequence until which the forces are calculated
     * @return the forceItem for the given placedItem
     */
    public static ForceItemDTO createForceItem(@NotNull PlacedItem placedItem, List<ForceItemDTO> forceItemsOnTop,
                                               int maxSequence) {

        RequierementCheckService.asserCOMIsCorrect(placedItem.itemLabel, placedItem.shape.getWidth(),
                placedItem.shape.getHeight(),
                placedItem.shape.getDepth(),
                placedItem.centerOfMass);


        Pair<Point2D, Double> gravitationForce =
                GravitationForceCalculator.calculateGravitationForce(placedItem.getAbsoluteCenterOfMassPoint(),
                        placedItem.weight);

        Map<Integer, List<Pair<Point2D, Double>>> actionForcesPerSequence =
                ActionForceGatherer.gatherRelatedActionForcesFromTopItems(placedItem.itemLabel,
                        placedItem.sequence, forceItemsOnTop, maxSequence);

        Map<Integer, Pair<Point2D, Double>> resultantForcePerSequence =
                ResultantForceCalculator.calculateResultantForcesForAllSequences(placedItem.sequence, maxSequence,
                        gravitationForce, actionForcesPerSequence);

        List<Pair<Point2D, PlacedItem>> reactionPoints = ReactionPointCalculator.calculateReactionPoints(placedItem,
                new ArrayList<>(placedItem.getEnvironmentRelations().getItemsBelow()));


        Map<Integer, Map<String, List<Pair<Point2D, Double>>>> reactionForcesPerSequence =
                ReactionForceCalculator.calculateReactionForces(placedItem.sequence, maxSequence, reactionPoints,
                        resultantForcePerSequence);

        Boolean[] stabilityPerSequence =
                ItemStabilityAssessor.getStabilityFromReactionForcesPerSequence(placedItem.sequence,
                maxSequence, reactionForcesPerSequence);


        return new ForceItemDTO(placedItem, gravitationForce, reactionPoints, actionForcesPerSequence,
                resultantForcePerSequence, reactionForcesPerSequence, stabilityPerSequence);
    }
}
