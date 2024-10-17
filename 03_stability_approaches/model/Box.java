package com.wim.palletizing.geometry.dim3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.palletizing.geometry.Dimensions;
import com.wim.palletizing.geometry.StandardRotationService;
import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.geometry.dim2.Rectangle;
import com.wim.palletizing.geometry.dim2.Shape2D;
import com.wim.palletizing.helper.Pair;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.UniqueCoordinateArrayFilter;
import org.springframework.data.annotation.PersistenceConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.*;

/**
 * Item with the shape of a geometrical cuboid, defined by {@link #width}, {@link #height} and {@link #depth}
 */
@Entity
public class Box extends Shape3D {
    /**
     * The box's width (x-direction)
     */
    @Column(nullable = false)
    public final double width;

    /**
     * The box's height (y-direction)
     */
    @Column(nullable = false)
    public final double height;

    /**
     * The box's depth (z-direction)
     */
    @Column(nullable = false)
    public final double depth;

    @PersistenceConstructor
    private Box(Long id, double width, double height, double depth, int rotationState) {
        super(id, Shape3DType.BOX, rotationState);

        if(width < 0 || height < 0 || depth < 0) throw new IllegalArgumentException("Invalid arguments for Box, should be > 0!");

        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    /**
     * Hibernate workaround
     */
    @SuppressWarnings("unused")
    private Box() {
        this(null, 0, 0, 0, -1);
    }

    @JsonCreator
    public Box(@JsonProperty(value = "width", required = true) int width, @JsonProperty(value = "height", required = true) int height, @JsonProperty(value = "depth", required = true) int depth, @JsonProperty(value = "rotationState", defaultValue = "0") int rotationState) {
        this(null, width, height, depth, rotationState);
    }

    public Box(double width, double height, double depth) {
        this(null, width, height, depth, 0);
    }
    
    public Box(double width, double height, double depth, int rotationState) {
        this(null, width, height, depth, rotationState);
    }

    @Override
    protected Shape2D createBaseShapeCache() {
        return new Rectangle(width, depth);
    }

    @Override
    protected double createVolumeCache() {
        return getBaseShape().getArea() * height;
    }

    @Override
    protected Point3D[] createPointCache() {
        Point3D[] points = new Point3D[8];
        points[0] = new Point3D(0, 0, 0);
        points[1] = new Point3D(0, 0, depth);
        points[2] = new Point3D(0, height, depth);
        points[3] = new Point3D(0, height, 0);
        points[4] = new Point3D(width, height, 0);
        points[5] = new Point3D(width, 0, 0);
        points[6] = new Point3D(width, 0, depth);
        points[7] = new Point3D(width, height, depth);
        return points;
    }

    @Override
    protected BoundingBox createBoundingBoxCache() {
        return new BoundingBox(width, height, depth);
    }

    @Override
    public Box rotate(double rotationState, Dimensions rotationAxes) {
        return StandardRotationService.createRotatedBox(rotationState, rotationAxes, this);
    }

    @Override
    public Shape3D copy() {
        return new Box(width, height, depth, rotationState);
    }

    @Override
    public int compareTo(Shape3D o) {
        if(!(o instanceof Box)) {
            throw new IllegalArgumentException(String.format("Could not compare Box to other Shapetype %s!", o.getClass()));
        }
        Box oBox = (Box) o;
        if (this.width == oBox.width && this.height == oBox.height && this.depth == oBox.depth) {
            return 0;
        } else if (this.width * this.height * this.depth > oBox.width * oBox.height * oBox.depth) {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    protected List<Point2D> getContactShapeWithOther2dGeometryFromTop(Geometry otherGeom, double yCordOtherGeom,
                                                                      Point3D ownOffset) {

        if(yCordOtherGeom != ownOffset.y + this.height){
            return new ArrayList<>();
        }


        Geometry geom = this.getBaseShape().createGeometry(new Point2D(ownOffset.x, ownOffset.z));

        return this.getIntersectionPointsBetweenTwoGeoms(geom, otherGeom);



    }

    @Override
    public double getBaseSupportFactor(Shape3D shape, double offsetX, double offsetY, double offsetZ) {
        return shape.getTopCoverageArea(getBaseShape(), -offsetX, -offsetY, -offsetZ) / getBaseShape().getArea();
    }

    @Override
    public double getTopCoverageArea(Shape2D shape, double offsetX, double offsetY, double offsetZ) {
        Shape2D topShape = this.getBaseShape();
        return (offsetY != this.height)
                ? 0
                : shape.getIntersectionArea(topShape, new Point2D(-offsetX, -offsetZ));
    }
}
