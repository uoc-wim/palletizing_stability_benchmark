package com.wim.assessment.staticStability.sme.CriteriasToCompare;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.palletizing.assessment.AbstractAssessmentCriterion;
import com.wim.palletizing.assessment.AssessmentCriterionType;
import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.geometry.dim3.Box;
import com.wim.palletizing.geometry.dim3.Point3D;
import com.wim.palletizing.helper.Pair;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.model.item.InputItemSet;
import com.wim.palletizing.model.item.PlacedItem;
import com.wim.palletizing.model.uld_properties.ULDProperties;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.PersistenceConstructor;

import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Entity
public class ScienceOfStaticRamosCriterion extends AbstractAssessmentCriterion {

    public static final AssessmentCriterionType type = AssessmentCriterionType.SCIENCE_OF_STATIC_RAMOS;

    @PersistenceConstructor
    private ScienceOfStaticRamosCriterion(Long id, double weight) {
        super(id, weight);
    }

    @JsonCreator
    public ScienceOfStaticRamosCriterion(@JsonProperty(value = "weight", required = true) double weight) {
        this(null, weight);
    }

    /**
     * Hibernate workaround
     */
    @SuppressWarnings("unused")
    private ScienceOfStaticRamosCriterion() {
        this(null, -1);
    }


    protected double assessUld(ULD uld) {
        if (uld.isEmpty())
            return 1.0;

        // First, calculate all environment relations of every item (Items on top or bottom)
        this.calculateULDSupportStructure(uld);

        //Only applicable to boxes, check for non-boxes
        for (PlacedItem item : uld.getPlacedItemsSorted())
            if (!(item.shape instanceof Box))
                return -1;

        int sequence = 0;
        for (PlacedItem item : uld.getPlacedItemsSorted()) {
            if (!this.isStableWithItemAtSequence(item, uld, sequence)) {
                break;
            }
            sequence++;
        }
        return (double) sequence / uld.getPlacedItemsSorted().size();
    }

    private void calculateULDSupportStructure(ULD uld) {
        //Take the first item of the ULD and look, if there is a support structure present (dummy check)
        PlacedItem item = uld.getPlacedItems().values().iterator().next();

        // if support structure has not been calculated, calculate it (is the case if no items are below and above)
        if (item.getEnvironmentRelations().getItemsBelow().isEmpty() && item.getEnvironmentRelations().getItemsOnTop().isEmpty())
            uld.calculateItemSupportStructure();
    }

    /**
     * Detects instability at a given sequence
     * @param item
     * @param uld
     * @param sequence
     * @returns True if the sequence is stable; False if it is unstable
     */
    private boolean isStableWithItemAtSequence(PlacedItem item, ULD uld, int sequence) {
        if (item.isBottomItem())
            return true;

        // We first calculate the resulting force point of this item
        Point2D resultantForcePoint = this.getResultantForcePointRegardingSequenceStep(item, sequence);

        //Test if this item is stable; resultant force point is not supported -> unstable
        if (!this.isResultantForcePointSupported(item, resultantForcePoint))
            return false;

        //Test for all items below if they are still stable
        return this.testForItemsDirectAndIndirectBelow(item, uld, sequence);
    }


    /**
     *
     * @param item
     * @param uld
     * @param sequence
     * @returns true if all items below are still stable, false otherwise
     */
    private boolean testForItemsDirectAndIndirectBelow(PlacedItem item, ULD uld, int sequence) {
        for (PlacedItem itemBelow : item.getEnvironmentRelations().getItemsBelow()) {
            if (!this.isStableWithItemAtSequence(itemBelow, uld, sequence))
                return false;

            if (!this.testForItemsDirectAndIndirectBelow(itemBelow, uld, sequence))
                return false;
        }

        return true;
    }

    private boolean isResultantForcePointSupported(PlacedItem item, Point2D resultantForcePoint) {
        List<Coordinate> supportPolygonCreationList = new ArrayList<>();


        for (PlacedItem itemBelow : item.getEnvironmentRelations().getItemsBelow()) {
            if ((itemBelow.getItemCoordinates().getX() <= resultantForcePoint.x && resultantForcePoint.x <= itemBelow.getItemCoordinates().getMaxX()) && (itemBelow.getItemCoordinates().getZ() <= resultantForcePoint.y && resultantForcePoint.y <= itemBelow.getItemCoordinates().getMaxZ())) {
                return true;
            }

            List<Point2D> contactPoints = item.shape.getContactPointsWithBottomShape(itemBelow.shape,
                    new Point3D(item.getItemCoordinates().getX(), item.getItemCoordinates().getY(),
                            item.getItemCoordinates().getZ()), new Point3D(itemBelow.getItemCoordinates().getX(),
                            itemBelow.getItemCoordinates().getY(), itemBelow.getItemCoordinates().getZ()));

            for (Point2D contactPoint2D: contactPoints){
                supportPolygonCreationList.add(new Coordinate(contactPoint2D.x, contactPoint2D.y));
            }

//            supportPolygonCreationList.add(new Coordinate(Math.max(itemBelow.getItemCoordinates().getX(), item.getItemCoordinates().getX()), Math.max(itemBelow.getItemCoordinates().getZ(),
//                    item.getItemCoordinates().getZ())));
//            supportPolygonCreationList.add(new Coordinate(Math.max(itemBelow.getItemCoordinates().getX(), item.getItemCoordinates().getX()), Math.min(itemBelow.getItemCoordinates().getMaxZ(),
//                    item.getItemCoordinates().getMaxZ())));
//            supportPolygonCreationList.add(new Coordinate(Math.min(itemBelow.getItemCoordinates().getMaxX(), item.getItemCoordinates().getMaxX()), Math.max(itemBelow.getItemCoordinates().getZ(),
//                    item.getItemCoordinates().getZ())));
//            supportPolygonCreationList.add(new Coordinate(Math.min(itemBelow.getItemCoordinates().getMaxX(), item.getItemCoordinates().getMaxX()),
//                    Math.min(itemBelow.getItemCoordinates().getMaxZ(), item.getItemCoordinates().getMaxZ())));
        }
        Coordinate[] supportPolygonCoordinates = new Coordinate[supportPolygonCreationList.size()];
        supportPolygonCoordinates = supportPolygonCreationList.toArray(supportPolygonCoordinates);

        GeometryFactory gf = new GeometryFactory();

        ConvexHull ch = new ConvexHull(supportPolygonCoordinates, gf);

        Geometry supportPolygon = ch.getConvexHull();

        Point forcePoint = gf.createPoint(new Coordinate(resultantForcePoint.x, resultantForcePoint.y));

        return supportPolygon.contains(forcePoint);
    }

    /**
     * For any item in the arrangement, that is relevant at sequence sequence, determine resultant force poin
     * @param item
     * @param sequence
     * @return
     */
    private Point2D getResultantForcePointRegardingSequenceStep(PlacedItem item, int sequence) {

        // Has the structure <Point2D, Force>
        Pair<Point2D, Double> resultantPair = new Pair<>(new Point2D(0, 0), 0.0);

        // First, the resultant force point is simply the center of gravity of the current item, since we have no
        // other influences (resultantPair is just initialized)
        resultantPair = this.addItemGravitationalForce(item, resultantPair);

        // Iterate through all items that lie on top of this item
        for (PlacedItem itemOnTop : item.getEnvironmentRelations().getItemsOnTop()) {
            // Check, if the item on top is relevant for the calculation of this sequence
            // In other words, only consider items relevant to this sequence
            if (itemOnTop.sequence <= sequence) {

                // We know there must be an influence, since item is at the top. Now add influence of item on top to
                // our item
                resultantPair = addItemGravitationalForce(itemOnTop, resultantPair);
            }
        }

        double x = resultantPair.first.x / resultantPair.second;
        double z = resultantPair.first.y / resultantPair.second;

        return new Point2D(x, z);
    }

    /**
     * Determine item force point and add to some stride (resultantPair), if present
     * @param item
     * @param resultantPair
     * @returns the cumulative resultant item force point and the cumulative weight
     */
    private Pair<Point2D, Double> addItemGravitationalForce(PlacedItem item, Pair<Point2D, Double> resultantPair) {
        Pair<Point2D, Double> getItemCoMWeightedForce = this.getItemCoMAndForce(item);

        // Add the location of the absolute CoM times the force
        double x = resultantPair.first.x + getItemCoMWeightedForce.first.x;
        double z = resultantPair.first.y + getItemCoMWeightedForce.first.y;

        // Add up entire force, do not split up force (?)
        double resultantForce = resultantPair.second + getItemCoMWeightedForce.second;

        // Resulting point has absolute coordinates
        return new Pair<>(new Point2D(x, z), resultantForce);
    }

    /**
     * Determines the force point of an item in absolute coordinates
     * @param item
     * @return
     */
    private Pair<Point2D, Double> getItemCoMAndForce(PlacedItem item) {
        double g = 9.81;
        Double force = item.weight * g;

        assert item.centerOfMass != null;

        // Calculate the absolute X/Z-Coordinates of the item's CoM
        double absoluteLocationCoM_X =
                item.getItemCoordinates().getX() + ((Box) item.shape).width * 0.5 + item.centerOfMass.x;
        double absoluteLocationCoM_Z =
                item.getItemCoordinates().getZ() + ((Box) item.shape).depth * 0.5 + item.centerOfMass.z;

        // Is this correct? Does the absoluteForce increase with x/z??
        double absoluteForceAtCoM_X = absoluteLocationCoM_X * force;
        double absoluteForceAtCoM_Z = absoluteLocationCoM_Z * force;

        return new Pair<>(new Point2D(absoluteForceAtCoM_X, absoluteForceAtCoM_Z), force);
    }


    @Override
    public AssessmentCriterionType getType() {
        return type;
    }

    @Override
    public void preProcessing(InputItemSet inputItemSet, HashMap enrichments, List<ULDProperties> uldPropertiesSet) {
        //Nothing to preprocess
    }


    @Override
    public AbstractAssessmentCriterion copy() {
        return new ScienceOfStaticRamosCriterion(this.weight);
    }

}
