package com.wim.assessment.staticStability.physicalSimulation.simulation;

import com.wim.palletizing.geometry.dim3.Box;
import com.wim.palletizing.geometry.dim3.Point3D;
import com.wim.palletizing.geometry.dim3.Shape3D;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.model.item.PlacedItem;

import java.util.ArrayList;
import java.util.Collection;

public class PhysicalSimulationHelper {
    public static ULD scaleULD(ULD uld, float scalingFactor) throws Exception {
        Collection<PlacedItem> placedItems = new ArrayList<>();
        for (PlacedItem item: uld.getPlacedItemsSorted()) {
            PlacedItem itemScaled = PhysicalSimulationHelper.scaleSingleItem(item, scalingFactor);
            placedItems.add(itemScaled);
        }
        return new ULD(uld.properties, placedItems);
    }
    public static PlacedItem scaleSingleItem(PlacedItem item, Float scalingFactor) throws Exception {
        Shape3D scaledShape = PhysicalSimulationHelper.scaleSingleShape(item.shape, scalingFactor);
        double scaledWeight = item.weight;
        int scaledX = (int) (item.getItemCoordinates().getX());
        int scaledY = (int) (item.getItemCoordinates().getY());
        int scaledZ = (int) (item.getItemCoordinates().getZ());

        Point3D scaledCoM = new Point3D(item.centerOfMass.x * scalingFactor, item.centerOfMass.y * scalingFactor,
                item.centerOfMass.z * scalingFactor);

        return new PlacedItem(item.itemLabel, item.priority, scaledWeight,
                item.loadCapacity, item.rotationAxes, scaledShape, null,
                scaledX, scaledY, scaledZ, item.sequence, -1, null,
                item.shipmentLabel,  scaledCoM, item.frictionStatic, item.frictionDynamic,
                item.restitution, null);
    }
    public static Shape3D scaleSingleShape(Shape3D shape, Float scalingFactor) throws Exception {
        if(!(shape instanceof Box))
            throw new Exception("Not a Box!");
        Box box = (Box) shape;
        return new Box(box.width * scalingFactor, box.height * scalingFactor, box.depth * scalingFactor);
    }
}
