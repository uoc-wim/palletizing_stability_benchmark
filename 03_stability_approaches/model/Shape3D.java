package com.wim.palletizing.geometry.dim3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.wim.palletizing.geometry.Dimensions;
import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.geometry.dim2.Shape2D;
import com.wim.palletizing.geometry.dim2.Shape2DWithOffset;
import com.wim.palletizing.helper.Pair;
import com.wim.palletizing.helper.Persistent;

import javax.persistence.*;

import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.util.UniqueCoordinateArrayFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Supertype for all types of three-dimensional shapes ({@link Box}es, {@link Cylinder}s, etc.)
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "shapeType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Box.class, name = "Box"),
        @JsonSubTypes.Type(value = Cylinder.class, name = "Cylinder"),
        @JsonSubTypes.Type(value = PolygonPrism.class, name = "PolygonPrism"),
        @JsonSubTypes.Type(value = LShape3D.class, name = "LShape")
})
public abstract class Shape3D implements Persistent, Comparable<Shape3D> {
    /**
     * The maximum point count of any implementation of {@link Shape3D}
     */
    public static final int MAX_POINT_COUNT = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public final Long id;

    @Transient
    @JsonIgnore
    public final Shape3DType type;

    @Column(nullable = false)
    public final int rotationState;

    /**
     * Cache for {@link #getVolume()}
     */
    @Transient
    private Double volume = null;

    /**
     * Cache for {@link #getPoints()}
     */
    @Transient
    private Point3D[] points = null;

    /**
     * Cache for {@link #getBoundingBox()}
     */
    @Transient
    private BoundingBox boundingBox = null;

    @Transient
    private Shape2D baseShape = null;

    protected Shape3D(Long id, Shape3DType type, int rotationState) {
        this.id = id;
        this.type = type;
        this.rotationState = rotationState;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * @return the 2-dimensional shape defining this 3-dimensional shape
     */
    @JsonIgnore
    public Shape2D getBaseShape() {
        if (baseShape == null)
            synchronized (this) {
                if (baseShape == null)
                    baseShape = createBaseShapeCache();
            }

        return baseShape;
    }

    /**
     * @return the 2-dimensional shape defining this 3-dimensional shape
     */
    protected abstract Shape2D createBaseShapeCache();

    /**
     * @return the shape's volume in cubic millimeters
     */
    @JsonIgnore
    public double getVolume() {
        if (volume == null)
            synchronized (this) {
                if (volume == null) {
                    volume = createVolumeCache();
                    if (volume < 0)
                        throw new IllegalStateException("Volume < 0 detected!!!");
                }
            }

        return volume;
    }

    /**
     * @return the shape's volume in cubic millimeters
     */
    protected abstract double createVolumeCache();

    /**
     * @return all the shape's points
     */
    @JsonIgnore
    public Point3D[] getPoints() {
        if (points == null)
            synchronized (this) {
                if (points == null)
                    points = createPointCache();
            }

        return points;
    }

    /**
     * @return all the shape's points
     */
    protected abstract Point3D[] createPointCache();

    /**
     * @return the smallest possible {@link BoundingBox} surrounding this item
     */
    @JsonIgnore
    public BoundingBox getBoundingBox() {
        if (boundingBox == null)
            synchronized (this) {
                if (boundingBox == null)
                    boundingBox = createBoundingBoxCache();
            }

        return boundingBox;
    }

    /**
     * @return the smallest possible {@link BoundingBox} surrounding this item
     */
    protected abstract BoundingBox createBoundingBoxCache();

    /**
     * Apply the new rotationState by rotating the shape from the old {@link #rotationState} to the given one
     *
     * @param rotationState new rotation state (between 0 and 1)
     * @param rotationAxes  allowed axes to rotate by
     * @return rotated {@link Shape3D}
     */
    public abstract Shape3D rotate(double rotationState, Dimensions rotationAxes);

    /**
     * Get the length of the longest side of the shape
     *
     * @return int
     * @author Tabea Janssen
     */
    @JsonIgnore
    public int getLongestSide() {
        return this.getBoundingBox().getLongestSide();
    }

    //TODO: Check call cases and react specifically in childs (should became abstract)
    @JsonIgnore
    public int getWidth() {
        return this.getBoundingBox().getWidth();
    }

    @JsonIgnore
    public int getHeight() {
        return this.getBoundingBox().getHeight();
    }

    @JsonIgnore
    public int getDepth() {
        return this.getBoundingBox().getDepth();
    }

    public abstract Shape3D copy();

    @Override
    public abstract int compareTo(Shape3D o);

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Shape3D)) {
            throw new IllegalArgumentException(String.format("Could not compare Shape3D to other Object from class " +
                    "%s!", object.getClass()));
        }
        return this.compareTo((Shape3D) object) == 0;
    }

    protected List<Point2D> transformCoordinatesToPoints(Coordinate[] contactCors) {
        return transformCoordinatesToPoints(Arrays.asList(contactCors));
    }

    protected List<Point2D> transformCoordinatesToPoints(List<Coordinate> contactCors) {
        List<Point2D> corList = new ArrayList<>();
        for (Coordinate cor : contactCors) {
            corList.add(new Point2D(Math.round(cor.x * 100) / 100.0, Math.round(cor.y * 100) / 100.0));
        }

        return corList;
    }

    protected List<Point2D> getIntersectionPointsBetweenTwoGeoms(Geometry geom, Geometry otherGeom) {
        Geometry contactGeom = otherGeom.intersection(geom);

        UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();

        contactGeom.convexHull().apply(filter);

        Coordinate[] corContact = filter.getCoordinates();


        return this.transformCoordinatesToPoints(corContact);
    }

    public enum Shape3DType {
        BOX, CYLINDER, POLYGON_PRISM, L_SHAPE
    }


    public List<Point2D> getContactPointsWithBottomShape(Shape3D otherShape, Point3D ownOffset, Point3D otherOffset) {
        Geometry geom = this.getBaseShape().createGeometry(new Point2D(ownOffset.x, ownOffset.z));

        if (!geom.isValid()) {
            throw new TopologyException("shape id: " + this.id);
        }

        return otherShape.getContactShapeWithOther2dGeometryFromTop(geom, ownOffset.y, otherOffset);
    }

    protected abstract List<Point2D> getContactShapeWithOther2dGeometryFromTop(Geometry otherGeom,
                                                                               double yCordOtherGeom,
                                                                               Point3D ownOffset);


    protected Coordinate[] reduceToMinMaxPoints(Coordinate[] corContact) {
        Coordinate[] reducedCors = new Coordinate[4];
        if (corContact.length <= 4)
            return corContact;
        double xMin = Double.MAX_VALUE, zMin = Double.MAX_VALUE,
                xMax = Double.MIN_VALUE, zMax = Double.MIN_VALUE;

        for (Coordinate coordinate : corContact) {
            if (coordinate.x < xMin) {
                xMin = coordinate.x;
                reducedCors[0] = coordinate;
            } if (coordinate.y < zMin) {
                zMin = coordinate.y;
                reducedCors[1] = coordinate;
            } if (coordinate.x > xMax) {
                xMax = coordinate.x;
                reducedCors[2] = coordinate;
            } if (coordinate.y > zMax) {
                zMax = coordinate.y;
                reducedCors[3] = coordinate;
            }
        }

        return reducedCors;
    }

    /**
     * @param shape   the {@link Shape3D} that may give support to the base of this {@link Shape3D}
     * @param offsetX how much shape is shifted in x direction in relation to this {@link Shape3D} position
     * @param offsetY how much shape is shifted in y direction in relation to this {@link Shape3D} position
     * @param offsetZ how much shape is shifted in z direction in relation to this {@link Shape3D} position
     * @return percentage of the base surface supported of this {@link Shape3D} by shape {@link Shape3D}
     */
    public abstract double getBaseSupportFactor(Shape3D shape, double offsetX, double offsetY, double offsetZ);

    /**
     * @param shape   the {@link Shape2D} that may cover the top of this {@link Shape3D}
     * @param offsetX how much shape is shifted in x direction in relation to this {@link Shape3D} position
     * @param offsetY how much shape is shifted in y direction in relation to this {@link Shape3D} position
     * @param offsetZ how much shape is shifted in z direction in relation to this {@link Shape3D} position
     * @return area of this {@link Shape3D} top surface that is covered by shape {@link Shape2D}
     */
    public abstract double getTopCoverageArea(Shape2D shape, double offsetX, double offsetY, double offsetZ);

}
