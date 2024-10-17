package com.wim.palletizing.model.uld_properties;

import com.wim.palletizing.geometry.dim3.BoundingBox;
import com.wim.palletizing.model.item.SolutionItem;
import com.wim.palletizing.geometry.GeometryHelperForULDs;
import com.wim.palletizing.geometry.dim2.Point2D;
import com.wim.palletizing.geometry.dim3.Point3D;
import com.wim.palletizing.libs.quickhull3d.Point3d;
import com.wim.palletizing.libs.quickhull3d.QuickHull3D;
import com.wim.palletizing.model.item.GhostItem;
import com.wim.palletizing.packing_sequence.model.PackingSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.vecmath.Matrix4d;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Random;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Created by christian on 08.07.17.
 */
public abstract class ULDProperties
{
    public final Long id;

    public final String name;

    /**
     * Width of the pallet (x-direction)
     */
    public final int width;

    /**
     * Depth of the pallet (z-direction)
     */
    public final int depth;

    /**
     * Maximum height of the pallet (y-direction)
     */
    public final int maxHeight;

    /**
     * Maximum weight of the pallet
     */
    public final int maxWeight;

    public final String uldStatus;

    /**
     * Volume of the pallet (width * maxHeight * depth)
     */
    public final transient Long maxVolume;

// ----------------------------------------- ASIM DYN STABILITY PROPERTIES ------------------------------------------ \\
    /**
     * Maximum floor load of the pallet
     */
    public final int floorLoadLimit;

    public EnumSet<PackingSide> generalPackingSides;
    public EnumSet<PackingSide> forkliftPackingSides;

// ----------------------------------------------- NEW ULD PROPERTIES ----------------------------------------------- \\

    /**
     * The own weight of the ULD itself
     */
    protected int               standardWeight;
    protected String            iataCode;
    protected String            description;
    protected boolean           certified;

    public final List<Point3D> loadableContour;
    public final transient List<Point2D> loadableContourFront; // == points from loadableContour at z = z_max
    public final transient List<Point2D> loadableContourSide;  // == points from loadableContour at z = z_max
    public transient Point2D[] loadableContourFrontForCalculation; // == points from loadableContour at z = z_max
    public transient Point2D[] loadableContourSideForCalculation;  // == points from loadableContour at z = z_max
    public final transient double horizontal_inner_min_z;      // for loadable rectangle on zy-axis
    public final transient double horizontal_inner_max_z;      // for loadable rectangle on zy-axis

    /**
     * Needed for quick AABB tests with contour. Consider viewing a contour from front [xy-axis]. then try to fit a rectangle with height = maxHeight into it. this is what this rectangle describes
     */
    public final transient List<Point2D> rectangleInLoadableContourFaceVertical;

    /**
     * Needed for quick AABB tests with contour. Consider viewing a contour from front [xy-axis]. then try to fit a rectangle with width = maxWidth into it. this is what this rectangle describes
     */
    public final transient List<Point2D> rectangleInLoadableContourFaceHorizontal;

    protected transient Collection<GhostItem> ghostItems;

    // ------------------------------------ CONSTRUCTORS ------------------------------------ \\
    public ULDProperties(ULDPropertiesDTO uldPropertiesDTO)
    {
        this.id = uldPropertiesDTO.id;
        this.name = uldPropertiesDTO.name;
        this.width = uldPropertiesDTO.width;
        this.depth = uldPropertiesDTO.depth;
        this.maxHeight = uldPropertiesDTO.maxHeight;
        this.maxWeight = uldPropertiesDTO.maxWeight;
        this.standardWeight = uldPropertiesDTO.standardWeight;
        this.iataCode = uldPropertiesDTO.iataCode;
        this.description = uldPropertiesDTO.description;
        this.certified = uldPropertiesDTO.certified;
        this.floorLoadLimit = uldPropertiesDTO.floorLoadLimit;
        this.loadableContour = uldPropertiesDTO.getLoadableContour();
        this.generalPackingSides = PackingSide.decode(uldPropertiesDTO.generalPackingSides);
        this.forkliftPackingSides = PackingSide.decode(uldPropertiesDTO.forkliftPackingSides);

        this.uldStatus = uldPropertiesDTO.status;

        this.loadableContourFront = GeometryHelperForULDs.getFace(loadableContour, depth/2);
        this.loadableContourSide = GeometryHelperForULDs.getFaceZY(loadableContour);
        this.rectangleInLoadableContourFaceVertical = GeometryHelperForULDs.getVerticalRectangleInFace(loadableContourFront);
        this.rectangleInLoadableContourFaceHorizontal = GeometryHelperForULDs.getHorizontalRectangleInFace(loadableContourFront);
        this.horizontal_inner_min_z = loadableContourSide.stream().filter(point2D -> point2D.y == 0).map(point2D -> point2D.x).min(Double::compareTo).orElseThrow();
        this.horizontal_inner_max_z = loadableContourSide.stream().filter(point2D -> point2D.y == 0).map(point2D -> point2D.x).max(Double::compareTo).orElseThrow();
        maxVolume = calculateMaxVolume();
        setLoadableContourFrontForCalculation();
        setLoadableContourSideForCalculation();
    }



// ----------------------------------------------------- GETTERS ---------------------------------------------------- \\

    public boolean isCuboid(){
        return this instanceof CuboidULDProperties;
    }

    private String getTypeAsString(){
        if (this instanceof PalletWithContourProperties)
                return "Pallet";
        else if (this instanceof ContainerProperties)
                return "Container";
        else if (this instanceof  CuboidULDProperties)
                return "Cube";
        else
                return "Unknown ULD Type: " + this.getClass();
    }

// ------------------------------------------------ ABSTRACT METHODS ------------------------------------------------ \\

    @Override
    public abstract boolean equals(Object o);
    @Override
    public abstract int hashCode();

    protected abstract void initializeGhostItems();

// ----------------------------------------------------- METHODS ---------------------------------------------------- \\


    /**
     * Static factory method (alternative to a constructor) of this class. So that the correct child class of ULDProperties
     * can be instantiated. This factory method creates an object of one of the following three classes
     * <i>(which are children of ULDProperties)</i>: <br />
     * - {@link CuboidULDProperties} <br />
     * - {@link PalletWithContourProperties} <br />
     * - {@link ContainerProperties}<br />
     * @param uldPropertiesDTO
     * @return A new ULDProperties object
     */
    public static ULDProperties create(ULDPropertiesDTO uldPropertiesDTO) {
        // cube
        if (uldPropertiesDTO instanceof CuboidULDPropertiesDTO)
            return new CuboidULDProperties(uldPropertiesDTO);

        // pallet
        else if (uldPropertiesDTO instanceof PalletWithContourPropertiesDTO)
            return new PalletWithContourProperties(uldPropertiesDTO);

        // container
        else if (uldPropertiesDTO instanceof ContainerPropertiesDTO)
            return new ContainerProperties(uldPropertiesDTO);

        LogManager.getLogger().error("Unknown child class of {}! Defaulting to {}",
                uldPropertiesDTO.getClass().getSimpleName(),
                CuboidULDProperties.class.getSimpleName());

        return new CuboidULDProperties(uldPropertiesDTO);
    }

    private void setLoadableContourFrontForCalculation() {

            this.loadableContourFrontForCalculation = new Point2D[this.loadableContourFront.size()];
            int i = 0;
            for (Point2D p : this.loadableContourFront) {
                if (p.y < this.maxHeight)
                    this.loadableContourFrontForCalculation[i] = new Point2D(p.x, p.y - 0.1); // -0.1 so intersectiondetection wont interfer at bottom or at sidewings
                else
                    this.loadableContourFrontForCalculation[i] = new Point2D(p.x, p.y);
                ++i;
            }

    }

    private void setLoadableContourSideForCalculation() {
        this.loadableContourSideForCalculation = new Point2D[this.loadableContourSide.size()];
        int i = 0;
        for (Point2D p : this.loadableContourSide) {
            if (p.y < this.maxHeight)
                this.loadableContourSideForCalculation[i] = new Point2D(p.x, p.y - 0.1); // -0.1 so intersectiondetection wont interfer at bottom or at sidewings
            else
                this.loadableContourSideForCalculation[i] = new Point2D(p.x, p.y);
            ++i;
        }
    }


    private void printDebugInfo() {
        Logger logger = LogManager.getLogger();

        logger.debug("Using {} \"{} [{}]\" with [ width: \"{}\" | depth: \"{}\" | height: \"{}\" ] :", getTypeAsString(), name, iataCode, width, depth, maxHeight);

        logger.debug("This ULD has the following (loadable) front vertices:");
        printVertexList(loadableContourFront,  'x');

        // pretty print two-dimensional face
        final short printHeight = 10;
        final short printWidth = printHeight * 3;
        final short padding = 5;
        final double width = GeometryHelperForULDs.getWidth2D(loadableContourFront);
        final double height = GeometryHelperForULDs.getHeight2D(loadableContourFront);
        final double max = Math.max(width, height);
        List<Point2D> miniFace = loadableContourFront.stream().map(point -> {
            double x = Math.floor(point.x/max*printWidth);
            double y = Math.floor(point.y/max*printHeight);
            return new Point2D(x, y);
        }).collect(Collectors.toList());

        logger.debug(" " + "-".repeat(printWidth + padding*2 + 1));

        for (int i = Math.min(printHeight, miniFace.stream().map(p -> (int) p.y).max(Integer::compareTo).orElse(-2)) + 1; i >= 0 ; i--){
            StringBuilder line = new StringBuilder("| " + " ".repeat(printWidth + padding*2) + "|");
            List<Point2D> pointsInLine = new LinkedList<>();
            for (int j = 0; j < miniFace.size() ; j++) {
                Point2D p = miniFace.get(j);
                int x = (int) p.x + padding;
                int y = (int) p.y;
                if (y == i){
                    pointsInLine.add(loadableContourFront.get(j));
                    line.replace(x+1, x+2, "X");
                }
            }

            line.append("   ");
            for (Point2D point : pointsInLine){
                line.append("   " + point.toString() + " | ");
            }

            logger.debug(line);
        }
        logger.debug("| "+" ".repeat(printWidth + padding*2) +"|");
        logger.debug(" " + "-".repeat(printWidth +padding*2 + 1));

        logger.debug("Its inner vertical rectangle (for quick AABB) has these vertices:");
        rectangleInLoadableContourFaceVertical.forEach(point2D -> logger.debug(String.format("| x: %-4.0f | y: %-4.0f |", point2D.x, point2D.y)));

        logger.debug("Its inner horizontal rectangle (for quick AABB) has these vertices:");
        rectangleInLoadableContourFaceHorizontal.forEach(point2D -> logger.debug(String.format("| x: %-4.0f | y: %-4.0f |", point2D.x, point2D.y)));

        logger.debug("This ULD has the following (loadable) side vertices:");
        printVertexList(loadableContourSide, 'z');

        logger.debug("inner_min_z: {}, inner_max_z: {}", horizontal_inner_min_z, horizontal_inner_max_z);

        logger.debug("Volume");
        logger.debug("{} {} {} cm³", !isCuboid() ? "├>" : "└>", String.format("%-43s" ,"Exact Volume of Convex Hull:") , DecimalFormat.getIntegerInstance().format(maxVolume));
        if (!isCuboid())
            logger.debug("└> {} {} cm³", String.format("%-43s" ,"Volume formula 'w * d * h' would've given:") , DecimalFormat.getIntegerInstance().format(width*depth*maxHeight));

    }

    protected void printVertexList(Collection<Point2D> area, char xOrZ_Axis){
        Logger logger = LogManager.getLogger();
        area.forEach(point2D -> logger.debug(String.format("| %c: %-4.0f | y: %-4.0f |", xOrZ_Axis, point2D.x, point2D.y)));
    }

    protected void printVertexListForPyhtonScript(){
        Logger logger = LogManager.getLogger();
        loadableContour.forEach(point2D -> logger.debug(String.format("[%-4.0f, %-4.0f, %-4.0f],", point2D.x, point2D.y, point2D.z)));
    }

    /**
     * This method returns dummy / ghost items for contours with gaps in the bottom.
     * The dummies can then be used for placing items on top of the wings for example
     * @return a Set with GhostItems
     */
    public Collection<GhostItem> getGhostItems(){
        if (ghostItems == null)
            ghostItems = new LinkedList<>();
        return ghostItems;
    }

    /**
     * Method to calculate exactly the volume of the convex hull of loadableContour (i.e. a convex polyhedra)
     * @return the volume
     */
    private Long calculateMaxVolume() {
        if (isCuboid()) return (long) (width * depth * maxHeight);

        if (loadableContour == null || loadableContour.isEmpty()) {
            LogManager.getLogger().error("ULD {} doesn't have a loadable contour. Can't calculate exact volume. Using formula \"w*d*h\" instead", name);
            return (long) (width * depth * maxHeight);
        }

        double[] coords = new double[loadableContour.size() * 3];
        int i = 0;
        for (Point3D p : loadableContour) {
            coords[i * 3] = p.x;
            coords[i * 3 + 1] = p.y;
            coords[i * 3 + 2] = p.z;
            ++i;
        }

        // create convex hull as a workaround
        QuickHull3D quickHull3D = new QuickHull3D(coords);

        // get triangles from all faces
        quickHull3D.triangulate();
        int[][] triangles = quickHull3D.getFaces(QuickHull3D.CLOCKWISE); // [face_id][vertex_id]

        Point3d[] hullVertices = quickHull3D.getVertices();


        long volume = 0;

        // reference point within center of loadableContour
        double x4 = (GeometryHelperForULDs.getMaxX_2d(loadableContourFront) - GeometryHelperForULDs.getMinX_2d(loadableContourFront)) / 2;
        double y4 = (GeometryHelperForULDs.getMaxY_2d(loadableContourFront) - GeometryHelperForULDs.getMinY_2d(loadableContourFront)) / 2;
        double z4 = depth / 2d;

        // calculate volume for each Tetrahedron
        for (i = 0; i < triangles.length; i++) {

            int[] triangle = triangles[i];
            double x1 =  hullVertices[triangle[0]].x;
            double y1 =  hullVertices[triangle[0]].y;
            double z1 =  hullVertices[triangle[0]].z;
            double x2 =  hullVertices[triangle[1]].x;
            double y2 =  hullVertices[triangle[1]].y;
            double z2 =  hullVertices[triangle[1]].z;
            double x3 =  hullVertices[triangle[2]].x;
            double y3 =  hullVertices[triangle[2]].y;
            double z3 =  hullVertices[triangle[2]].z;

            double det = new Matrix4d(
                    new double[]{
                            x1, x2, x3, x4,
                            y1, y2, y3, y4,
                            z1, z2, z3, z4,
                            1 , 1 , 1 , 1
                    }).determinant();

            volume += Math.abs(det / 6d);
        }

        return volume;
    }

    /**
     * Used in solution generators, to determine x and z for solution item within this uld
     * @param solutionItem
     * @param setNewValues
     * @param random
     * @return
     */
    public SolutionItem determineXZValuesForSolutionItem(SolutionItem solutionItem, boolean setNewValues, Random random) {
        final BoundingBox boundingBox = solutionItem.rotatedShape.getBoundingBox();
        // for x dimension:
        boolean fittingUldDimension = boundingBox.getWidth() < width;
        // placement can be kept - if you do not want a change (!setNewValues) and the item fits on that position
        boolean fittingItemPlacement = !setNewValues && solutionItem.x + boundingBox.getWidth() < width;

        // placement can not be kept and is gonna change - if change is required (!fittingItemPlacement) && placement possible (fittingUldDimension)
        if (!fittingItemPlacement && fittingUldDimension)
            solutionItem.x = random.nextInt(width - boundingBox.getWidth() + 1); // + 1 since the bound is exclusive
        else if (!fittingItemPlacement && !fittingUldDimension) {
            //solutionItem.x = random.nextInt(width + 1);
            solutionItem.used = false;

        }
        // else (fittingItemPlacement - which implies fittingUld) -> nothing to change
        // same for z dimension:
        fittingUldDimension = boundingBox.getDepth() < depth;
        fittingItemPlacement = !setNewValues && solutionItem.z + boundingBox.getDepth() < depth;
        if (!fittingItemPlacement && fittingUldDimension)
            solutionItem.z = random.nextInt(depth - boundingBox.getDepth() + 1);
        else if (!fittingItemPlacement && !fittingUldDimension) {
            //solutionItem.z =  random.nextInt(depth + 1);
            solutionItem.used = false;
        }
        // else (fittingItemPlacement => && fittingUld) -> nothing to change
        return solutionItem;

    }
}
