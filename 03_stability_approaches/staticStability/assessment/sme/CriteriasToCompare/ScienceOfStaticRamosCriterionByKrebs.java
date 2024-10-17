package com.wim.assessment.staticStability.sme.CriteriasToCompare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Entity;

import com.wim.palletizing.assessment.AbstractAssessmentCriterion;
import com.wim.palletizing.assessment.AssessmentCriterionType;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.PersistenceConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.geometry.dim3.Box;
import com.wim.palletizing.helper.Pair;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.model.item.InputItemSet;
import com.wim.palletizing.model.item.PlacedItem;
import com.wim.palletizing.model.uld_properties.ULDProperties;

@Entity
public class ScienceOfStaticRamosCriterionByKrebs extends AbstractAssessmentCriterion {

    public static final AssessmentCriterionType type = AssessmentCriterionType.SCIENCE_OF_STATIC_RAMOS_BY_KREBS;

    @PersistenceConstructor
    private ScienceOfStaticRamosCriterionByKrebs(Long id, double weight) {
        super(id, weight);
    }

    @JsonCreator
    public ScienceOfStaticRamosCriterionByKrebs(@JsonProperty(value = "weight", required = true) double weight) {
        this(null, weight);
    }

    /**
     * Hibernate workaround
     */
    @SuppressWarnings("unused")
    private ScienceOfStaticRamosCriterionByKrebs() {
        this(null, -1);
    }


    @Override
    protected double assessUld(ULD uld) {
        if (uld.isEmpty())
            return 1.0;

        assureULDSupportStructureHasBeenCalculated(uld);

        for (PlacedItem item : uld.getPlacedItemsSorted())
            if (isNoBox(item))
                return -1;

        int counter = 0;
        for (PlacedItem item : uld.getPlacedItemsSorted()) {
            if (!isStableWithItem(item)) {
                break;
            }
            counter++;
        }
        return (double) counter / uld.getPlacedItemsSorted().size();
    }

    private boolean isNoBox(PlacedItem item) {
        return !(item.shape instanceof Box);
    }

    private void assureULDSupportStructureHasBeenCalculated(ULD uld) {
        PlacedItem testItem = uld.getPlacedItems().values().iterator().next();

        if (testItem.getEnvironmentRelations().getItemsBelow().isEmpty() && testItem.getEnvironmentRelations().getItemsOnTop().isEmpty())
            uld.calculateItemSupportStructure();
    }

    private boolean isStableWithItem(PlacedItem item) {
        if (item.isBottomItem())
            return true;
        Point2D resultantForcePoint = getResultantForcePoint(item);

        //Test if this item is stable
        if (!isResultantForcePointSupported(item, resultantForcePoint))
            return false;

        //Test for all items below if they are still stable
        for (PlacedItem itemBelow : item.getEnvironmentRelations().getItemsBelow())
            if (!isStableWithItem(itemBelow))
                return false;


        return true;
    }

    private boolean isResultantForcePointSupported(PlacedItem item, Point2D resultantForcePoint) {
        List<Coordinate> supportPolygonCreationList = new ArrayList<>();


        for (PlacedItem itemBelow : item.getEnvironmentRelations().getItemsBelow()) {
            if ((itemBelow.getItemCoordinates().getX() <= resultantForcePoint.x && resultantForcePoint.x <= itemBelow.getItemCoordinates().getMaxX()) && (itemBelow.getItemCoordinates().getZ() <= resultantForcePoint.y && resultantForcePoint.y <= itemBelow.getItemCoordinates().getMaxZ())) {
                return true;
            }

            supportPolygonCreationList.add(new Coordinate(Math.max(itemBelow.getItemCoordinates().getX(),
                    item.getItemCoordinates().getX()), Math.max(itemBelow.getItemCoordinates().getZ(),
                    item.getItemCoordinates().getZ())));
            supportPolygonCreationList.add(new Coordinate(Math.max(itemBelow.getItemCoordinates().getX(),
                    item.getItemCoordinates().getX()), Math.min(itemBelow.getItemCoordinates().getMaxZ(),
                    item.getItemCoordinates().getMaxZ())));
            supportPolygonCreationList.add(new Coordinate(Math.min(itemBelow.getItemCoordinates().getMaxX(),
                    item.getItemCoordinates().getMaxX()), Math.max(itemBelow.getItemCoordinates().getZ(),
                    item.getItemCoordinates().getZ())));
            supportPolygonCreationList.add(new Coordinate(Math.min(itemBelow.getItemCoordinates().getMaxX(),
                    item.getItemCoordinates().getMaxX()),
                    Math.min(itemBelow.getItemCoordinates().getMaxZ(), item.getItemCoordinates().getMaxZ())));
        }
        Coordinate[] supportPolygonCoordinates = new Coordinate[supportPolygonCreationList.size()];
        supportPolygonCoordinates = supportPolygonCreationList.toArray(supportPolygonCoordinates);

        GeometryFactory gf = new GeometryFactory();

        ConvexHull ch = new ConvexHull(supportPolygonCoordinates, gf);

        Geometry supportPolygon = ch.getConvexHull();

        Point forcePoint = gf.createPoint(new Coordinate(resultantForcePoint.x, resultantForcePoint.y));

        return supportPolygon.contains(forcePoint);
    }

    private Point2D getResultantForcePoint(PlacedItem item) {

        Pair<Point2D, Double> resultantPair = new Pair<>(new Point2D(0, 0), 0.0);

        resultantPair = this.addItemGravitationalForce(item, resultantPair);

        for (PlacedItem itemOnTop : item.getEnvironmentRelations().getItemsOnTop()) {
            resultantPair = addItemGravitationalForce(itemOnTop, resultantPair);
        }
        double x = resultantPair.first.x / resultantPair.second;
        double z = resultantPair.first.y / resultantPair.second;

        return new Point2D(x, z);
    }

    private Pair<Point2D, Double> addItemGravitationalForce(PlacedItem item, Pair<Point2D, Double> resultantPair) {
        double g = 9.81;
        Double force = item.weight * g;

        double x = resultantPair.first.x + (item.getItemCoordinates().getX() + ((Box) item.shape).width * 0.5) * force;
        double z = resultantPair.first.y + (item.getItemCoordinates().getZ() + ((Box) item.shape).depth * 0.5) * force;

        double resultantForce = resultantPair.second + force;

        return new Pair<>(new Point2D(x, z), resultantForce);
    }

    @Override
    public AssessmentCriterionType getType() {
        return type;
    }

    @Override
    public void preProcessing(InputItemSet inputItemSet, HashMap enrichments, List<ULDProperties> uldPropertiesSet) {
        //No preprocessing needed
    }

    @Override
    public AbstractAssessmentCriterion copy() {
        return new ScienceOfStaticRamosCriterionByKrebs(this.weight);
    }


}
