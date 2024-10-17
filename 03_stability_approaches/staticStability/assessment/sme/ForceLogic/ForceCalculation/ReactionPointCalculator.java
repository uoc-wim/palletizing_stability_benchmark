package com.wim.assessment.staticStability.sme.ForceLogic.ForceCalculation;

import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.geometry.dim3.Point3D;
import com.wim.palletizing.helper.Pair;
import com.wim.palletizing.model.item.PlacedItem;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Frederick Gamer
 * created February 2022
 * Service to determine the reactionPoints between to items
 */
public class ReactionPointCalculator {

    /**
     * Calculates all reactionForcePoints with all items directly below the item.
     * Uses the complete build ULD for that
     *
     * @param placedItem the item to calculate the reactionPoints for
     * @return the points, along with the item on which reactionForces acts
     */
    protected static List<Pair<Point2D, PlacedItem>> calculateReactionPoints(PlacedItem placedItem,
                                                                             List<PlacedItem> itemsBelow) {
        List<Pair<Point2D, PlacedItem>> reactionForcePoints_local = new LinkedList<>();

        for (PlacedItem itemBelow : itemsBelow) {
            reactionForcePoints_local.addAll(ReactionPointCalculator.getReactionPointsWithOtherItem(placedItem,
                    itemBelow));
        }

        return reactionForcePoints_local;
    }

    /**
     * Using the shape3D of own item and given item the point(s) where forces act
     * are determined. From the perspective of this item this force is called
     * reactionForces
     *
     * @param centralItem the item for which the reactionForce is calculated
     * @param itemBelow   a placedItem that is expected to be directly below
     * @return Collection<? extends Pair < Point2D, PlacedItem>> a list of pairs of
     * the reactionPoints and the associated item
     */
    private static Collection<? extends Pair<Point2D, PlacedItem>> getReactionPointsWithOtherItem(PlacedItem centralItem,
                                                                                                  PlacedItem itemBelow) {

        List<Pair<Point2D, PlacedItem>> reactionForcePoints = new LinkedList<>();

        List<Point2D> contactPoints = centralItem.shape.getContactPointsWithBottomShape(itemBelow.shape,
                new Point3D(centralItem.getItemCoordinates().getX(), centralItem.getItemCoordinates().getY(),
                        centralItem.getItemCoordinates().getZ()), new Point3D(itemBelow.getItemCoordinates().getX(),
                        itemBelow.getItemCoordinates().getY(), itemBelow.getItemCoordinates().getZ()));

        for (Point2D contactPoint : contactPoints) {
            reactionForcePoints.add(new Pair<>(contactPoint, itemBelow));
        }


        return reactionForcePoints;
    }
}
