package com.wim.assessment.staticStability.baseSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.assessment.AbstractAssessmentCriterion;
import com.wim.assessment.AssessmentCriterionType;
import com.wim.geometry.dim2.Point2D;
import com.wim.geometry.dim3.Box;
import com.wim.geometry.dim3.Point3D;
import com.wim.model.ULD;
import com.wim.model.item.InputItemSet;
import com.wim.model.item.PlacedItem;
import com.wim.model.uld_properties.ULDProperties;
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
import java.util.Set;

@Entity
public class PacquayCriterion extends AbstractAssessmentCriterion {

    public static final AssessmentCriterionType type = AssessmentCriterionType.PACQUAY;

    @PersistenceConstructor
    private PacquayCriterion(Long id, double weight) {
        super(id, weight);
    }

    @JsonCreator
    public PacquayCriterion(@JsonProperty (value = "weight", required = true) double weight){
        this(null,weight);
    }

    /**
     * Hibernate workaround
     */
    @SuppressWarnings("unused")
    private PacquayCriterion()
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

        Point2D corner1 = new Point2D(item.getItemCoordinates().getX(), item.getItemCoordinates().getZ());
        Point2D corner2 = new Point2D(item.getItemCoordinates().getX() + item.shape.getWidth(), item.getItemCoordinates().getZ());
        Point2D corner3 = new Point2D(item.getItemCoordinates().getX(),
                item.getItemCoordinates().getZ() + item.shape.getDepth());
        Point2D corner4 = new Point2D(item.getItemCoordinates().getX() + item.shape.getWidth(),
                item.getItemCoordinates().getZ() + item.shape.getDepth());

        int supportedNr = 0;
        if (this.isPointSupported(item, corner1))
            supportedNr++;
        if (this.isPointSupported(item, corner2))
            supportedNr++;
        if (this.isPointSupported(item, corner3))
            supportedNr++;
        if (this.isPointSupported(item, corner4))
            supportedNr++;

        return supportedNr >= 3;

    }
    private boolean isPointSupported(PlacedItem item, Point2D point2D) {
        List<Coordinate> supportPolygonCreationList = new ArrayList<>();


        for (PlacedItem itemBelow : item.getEnvironmentRelations().getItemsBelow()) {
            if ((itemBelow.getItemCoordinates().getX() <= point2D.x && point2D.x <= itemBelow.getItemCoordinates().getMaxX()) && (itemBelow.getItemCoordinates().getZ() <= point2D.y && point2D.y <= itemBelow.getItemCoordinates().getMaxZ())) {
                return true;
            }

            List<Point2D> contactPoints = item.shape.getContactPointsWithBottomShape(itemBelow.shape,
                    new Point3D(item.getItemCoordinates().getX(), item.getItemCoordinates().getY(),
                            item.getItemCoordinates().getZ()), new Point3D(itemBelow.getItemCoordinates().getX(),
                            itemBelow.getItemCoordinates().getY(), itemBelow.getItemCoordinates().getZ()));

            for (Point2D contactPoint2D: contactPoints){
                supportPolygonCreationList.add(new Coordinate(contactPoint2D.x, contactPoint2D.y));
            }
        }
        Coordinate[] supportPolygonCoordinates = new Coordinate[supportPolygonCreationList.size()];
        supportPolygonCoordinates = supportPolygonCreationList.toArray(supportPolygonCoordinates);

        GeometryFactory gf = new GeometryFactory();

        ConvexHull ch = new ConvexHull(supportPolygonCoordinates, gf);

        Geometry supportPolygon = ch.getConvexHull();

        Point forcePoint = gf.createPoint(new Coordinate(point2D.x, point2D.y));

        return supportPolygon.contains(forcePoint);
    }


    @Override
    public AbstractAssessmentCriterion copy() {
        return new PacquayCriterion(this.weight);
    }
}
