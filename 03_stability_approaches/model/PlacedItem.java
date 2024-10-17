package com.wim.palletizing.model.item;

import com.wim.palletizing.geometry.Dimensions;
import com.wim.palletizing.geometry.dim3.*;
import com.wim.palletizing.model.incompatibility.CommodityTypeSet;
import com.wim.palletizing.model.uld_properties.ULDProperties;
import com.wim.palletizing.packing_sequence.model.PackingSide;
import com.wim.palletizing.packing_sequence.packing_devices.AbstractPackingDevice;

import java.util.*;
import java.util.List;

/**
 *
 */
public class PlacedItem extends Item implements Comparable<PlacedItem>, Cloneable {


    public final Long id;

    public double floorLoad;

    //Algorithm Core
    public final int sequence;
    public final Shape3D shape;
    private final ItemCoordinates ITEM_COORDINATES;
    private EnvironmentRelations environmentRelations;

    //Assessment Only
    private PackingSequenceItem packingSequenceItem;
    private final ItemCommodities ITEM_COMMODITIES;


    //Constructors and copy-Constructors

    private PlacedItem(Long id, String itemLabel, double priority, double weight, Double loadCapacity, Dimensions rotationAxes, Shape3D shape,
                       ForkliftCapableAxes forkliftCapableAxes, int x, int y, int z, int sequence, Integer packingSequence, EnumSet<PackingSide> packingSides,
                       AbstractPackingDevice packingDevice, int numberOfPackers, double floorLoad, CommodityTypeSet commodityTypeSet, String shipmentLabel,
                       Point3D centerOfMass, double frictionStatic, double frictionDynamic, double restitution, String underpallet) {
        super(itemLabel, priority, weight, loadCapacity, rotationAxes, shipmentLabel, centerOfMass, frictionStatic, frictionDynamic, restitution, underpallet);
        this.id = id;
        this.shape = shape;
        this.floorLoad = floorLoad;

        this.sequence = sequence;

        this.ITEM_COORDINATES = new ItemCoordinates(x, y, z, shape);
        this.ITEM_COMMODITIES = new ItemCommodities(commodityTypeSet);
        this.packingSequenceItem = new PackingSequenceItem(forkliftCapableAxes, packingSequence,
                packingSides, packingDevice, numberOfPackers, this.ITEM_COORDINATES, itemLabel, shape);

        this.environmentRelations = new EnvironmentRelations();
    }

    public PlacedItem(String itemLabel, double priority, double weight, Double loadCapacity, Dimensions rotationAxes, Shape3D shape, ForkliftCapableAxes forkliftCapableAxes,
                      int x, int y, int z, int sequence, double floorLoad, CommodityTypeSet commodityTypeSet, String shipmentLabel, Point3D centerOfMass, double frictionStatic,
                      double frictionDynamic, double restitution, String underpallet) {
        this(null, itemLabel, priority, weight, loadCapacity, rotationAxes, shape, forkliftCapableAxes, x, y, z, sequence, null,
                null, null, -1, floorLoad, commodityTypeSet, shipmentLabel, centerOfMass, frictionStatic,
                frictionDynamic, restitution, underpallet);
    }

    public PlacedItem(SolutionItem solutionItem, int x, int y, int z, int sequence, double floorLoad) {
        this(solutionItem.itemLabel, solutionItem.priority, solutionItem.weight, solutionItem.loadCapacity, solutionItem.rotationAxes,
                solutionItem.rotatedShape, solutionItem.forkliftCapableAxes, x, y, z, sequence, floorLoad, solutionItem.commodityTypeSet,
                solutionItem.shipmentLabel, solutionItem.centerOfMass, solutionItem.frictionStatic, solutionItem.frictionDynamic, solutionItem.restitution,
                solutionItem.underpallet);
    }

    public PlacedItem(PlacedItemDTO placedItemDTO) {
        this(placedItemDTO.id, placedItemDTO.itemLabel, placedItemDTO.priority, placedItemDTO.weight, placedItemDTO.loadCapacity,
                placedItemDTO.rotationAxes, placedItemDTO.shape, placedItemDTO.forkliftCapableAxes, placedItemDTO.x, placedItemDTO.y,
                placedItemDTO.z, placedItemDTO.sequence, placedItemDTO.packingSequence, PackingSide.decode(placedItemDTO.packingSides),
                placedItemDTO.packingDevice, placedItemDTO.numberOfPackers, placedItemDTO.floorLoad, placedItemDTO.commodityTypeSet, placedItemDTO.shipmentLabel,
                placedItemDTO.centerOfMass, placedItemDTO.frictionStatic, placedItemDTO.frictionDynamic, placedItemDTO.restitution, placedItemDTO.underpallet);
    }

    // Override Methods

    @Override
    public String toString() {
        return this.itemLabel + this.ITEM_COORDINATES.toString();
    }

    /**
     * Compares this {@link PlacedItem} with the given one by their {@link #sequence}.
     *
     * @param pi2 {@link PlacedItem} to compare <code>this</code> with
     * @return 1, if this {@link PlacedItem} comes after the given one<br>
     * 0, else
     */
    @Override
    public int compareTo(PlacedItem pi2) {
        return (sequence > pi2.sequence) ? 1 : -1;
    }

    public boolean isBottomItem() {
        return this.ITEM_COORDINATES.isBottomItem();
    }

    public PlacedItemDTO toDTO() {
        return new PlacedItemDTO(this.id, this.itemLabel, this.priority, this.weight,
                this.loadCapacity,
                this.rotationAxes, this.shape,
                this.ITEM_COORDINATES.getX(), this.ITEM_COORDINATES.getY(), this.ITEM_COORDINATES.getZ(),
                this.sequence, this.packingSequenceItem.getSequence(), this.packingSequenceItem.getPackingSides(),
                this.packingSequenceItem.getPreferredPackingSide(),this.packingSequenceItem.getPackingDevice(),
                this.packingSequenceItem.getNumberOfPackers(),this.floorLoad,
                this.packingSequenceItem.getForkliftCapableAxes(), this.getItemCommodities().getCommodityTypeSet(),
                this.shipmentLabel, this.centerOfMass, this.frictionStatic, this.frictionDynamic, this.restitution, this.underpallet
        );

    }


    //Environment Relations

    /**
     * Assumes <b>this</b> item to be <b>below</b> and other item to be placed on top of this item --> What might
     * the
     * min-y-level
     * be?
     * @param otherItemShape
     * @param otherItemX
     * @param otherItemZ
     * @return
     */
    public int getMinPossibleYForOtherShape(Shape3D otherItemShape, int otherItemX, int otherItemZ) {
        return EnvironmentRelationService.getMinPossibleYForOtherShape(otherItemShape, otherItemX, otherItemZ,
                this.shape, this.ITEM_COORDINATES.getX(), this.ITEM_COORDINATES.getZ(), this.ITEM_COORDINATES.getY(),
                this.ITEM_COORDINATES.getMaxY());
    }

    /**
     * Compares this item to an otherPlacedItem. Determines, if this item is supported by other item or vice versa
     * @param otherPlacedItem
     */
    public void calculateAndStoreSupportRelation(PlacedItem otherPlacedItem) {

        //Calculate the min y coordinate (either the lower item's y-level or 0) in
        //case of overlap or non-overlap; assume this item bottom first
        int minYOtherItem = this.getMinPossibleYForOtherShape(otherPlacedItem.shape, otherPlacedItem.ITEM_COORDINATES.getX(),
                otherPlacedItem.ITEM_COORDINATES.getZ());

        //Check if this items stands on other item (reverse to lines above)
        int minYThisItem = otherPlacedItem.getMinPossibleYForOtherShape(this.shape, this.ITEM_COORDINATES.getX(),
                this.ITEM_COORDINATES.getZ());

        // No corridor intersection; non-overlap
        if (minYOtherItem == 0)
            return;

        // other item has been placed above and right on top of this item (other item min-y from other item and item
        // coordinates match; it is not far above)
        else if (minYOtherItem == otherPlacedItem.ITEM_COORDINATES.getY()) {
            otherPlacedItem.getEnvironmentRelations().addItemBelow(this);
            this.getEnvironmentRelations().addItemOnTop(otherPlacedItem);
        }
        // this item has been placed directly on top
        else if (minYThisItem == this.ITEM_COORDINATES.getY()) {
            otherPlacedItem.getEnvironmentRelations().addItemOnTop(this);
            this.getEnvironmentRelations().addItemBelow(otherPlacedItem);
        }
    }

    /**
     * Calcualtes its own center and adds the centerOfMass
     * @return the 3d point of the center of mass
     */
    public Point3D getAbsoluteCenterOfMassPoint() {
        return new Point3D(ITEM_COORDINATES.getCenterX() + centerOfMass.x, ITEM_COORDINATES.getCenterY() + centerOfMass.y,
                ITEM_COORDINATES.getCenterZ() + centerOfMass.z);
    }

	public double getBaseSupportFactor(){
        double supportFactor = 0;

        //this simulates the base support the item would get by the floor
        if (this.getItemCoordinates().getY() ==  0){
            Box floor = new Box(shape.getBoundingBox().getWidth(), 5, shape.getBoundingBox().getDepth());
            supportFactor += shape.getBaseSupportFactor(floor, 0, -floor.height, 0);
        }

		for (PlacedItem itemBelow: getEnvironmentRelations().getItemsBelow()){
			if (itemBelow.sequence < this.sequence)
				supportFactor += shape.getBaseSupportFactor(itemBelow.shape, itemBelow.getItemCoordinates().getX() - this.getItemCoordinates().getX(), itemBelow.getItemCoordinates().getY() - this.getItemCoordinates().getY(), itemBelow.getItemCoordinates().getZ() - this.getItemCoordinates().getZ());
		}
		return supportFactor;
	}


    /**
     * Checks if the {@link PlacedItem} is directly supported by this {@link PlacedItem}
     *
     * @param placedItem item on top
     * @return true if the item is directly supported by this item
     * @author Tabea Janssen
     * Date 06.08.2019
     */
    public boolean isItemOnTop(PlacedItem placedItem) {
        return EnvironmentRelationService.isItemOnTop(this.ITEM_COORDINATES,
                this.shape, placedItem.ITEM_COORDINATES, placedItem.shape);
    }

    public boolean checkForNeighborhood(PlacedItem placedItem) {
        return EnvironmentRelationService.checkForNeighborhood(this.ITEM_COORDINATES,
         this.shape, placedItem.ITEM_COORDINATES, placedItem.shape);
    }

    public boolean isBehind(PlacedItem item) {
        return EnvironmentRelationService.isBehind(this.ITEM_COORDINATES, item.getItemCoordinates());
    }

    public boolean isInFrontOf(PlacedItem item) {
        return EnvironmentRelationService.isInFrontOf(this.ITEM_COORDINATES, item.getItemCoordinates());
    }

    public boolean isRightNeighborOf(PlacedItem item) {
        return EnvironmentRelationService.isRightNeighborOf(this.ITEM_COORDINATES, item.getItemCoordinates());
    }

    public boolean isLeftNeighborOf(PlacedItem item) {
        return EnvironmentRelationService.isLeftNeighborOf(this.ITEM_COORDINATES, item.getItemCoordinates());
    }

    public boolean isAbove(PlacedItem item) {
        return EnvironmentRelationService.isAbove(this.ITEM_COORDINATES, item.getItemCoordinates());
    }

    /**
     * Checks if an item is placed directly next to an already placed item or a pallet edge in x direction
     *
     * @param placedItems the items that are already placed on the pallet
     * @param palletWidth the width of the pallet in height level of the item
     * @return true if the item's x-position is defined by a pallet edge or another item
     */
    public boolean hasXSupport(List<PlacedItem> placedItems, double palletWidth) {
        return EnvironmentRelationService.hasXSupport(this.ITEM_COORDINATES, this.shape, placedItems,  palletWidth);
    }

    /**
     * Checks if an item is placed directly next to an already placed item or a pallet edge in z direction
     *
     * @param palletDepth the depth of the pallet in height level of this item
     * @param placedItems the items that are already placed on the pallet
     * @return true if the item's z-position is defined by a pallet edge or another item
     */
    public boolean hasZSupport(List<PlacedItem> placedItems, double palletDepth) {
        return EnvironmentRelationService.hasZSupport(this.ITEM_COORDINATES, this.shape, placedItems, palletDepth);
    }


    //Loadable Item

    /**
     * Placeholder to check if the {@link PlacedItem} has a "No-visual-contact"-incompatibility with {@param item2}
     *
     * @param item2 another {@link PlacedItem}
     * @return true if the items should not have visual contact
     * @autor Tabea Janssen
     */
    public boolean shouldNotHaveVisualContactWith(PlacedItem item2) {
        return false; //Todo: Insert real input data from incompatibilities here
    }

    //Metrics concerning this item, but enriched with logic

    /**
     * Get the length of the longest side of the item
     *
     * @return int
     * @author Tabea Janssen
     */
    public int getLongestSide() {
        return this.shape.getLongestSide();
    }

    // Metrics concerning layout/list of placed items, to be moved to loadability item

    /**
     * Checks if thwo {@link PlacedItem}s have visual contact on the pallet when {@param usedItems} are already on the pallet
     *
     * @param item      another {@link PlacedItem}
     * @param usedItems a set of {@link PlacedItem}s that are already placed on the pallet
     * @return true if the items have visual contact, that means, no other items block the field of view between the items or the field of view is only partially blocked
     * @author Tabea Janssen
     */
    public boolean hasVisualContactWith(PlacedItem item, List<PlacedItem> usedItems) {
        return this.packingSequenceItem.hasVisualContactWith(item, usedItems);
    }

    /**
     * Calculates the distance between the palletizer's feet and the {@link PlacedItem}'s position with {@param placedItems} already on the pallet
     *
     * @param side        the {@link PackingSide} from which the palletizer approaches the {@link PlacedItem}'s posittion
     * @param placedItems set of {@link PlacedItem}s that are already placed on the pallet
     * @return the distance in x- or z- direction depending on {@param side} between the palletizer's feet and the {@link PlacedItem}'s nearest edge to the palletizer
     * @author Tabea Janssen
     */
    public int getArmsReach(PackingSide side, List<PlacedItem> placedItems) {
        return this.packingSequenceItem.getArmsReach(side, placedItems);
    }

    public int calculateDistanceToPalletEdge(PackingSide side, ULDProperties uldProperties) {
        return this.packingSequenceItem.calculateDistanceToPalletEdge(side, uldProperties);
    }

    public boolean hasOverhangingItemAbove(List<PlacedItem> usedItems, ULDProperties uldProperties) {
        return this.packingSequenceItem.hasOverhangingItemAbove(usedItems, uldProperties);
    }

    public double calculateMinEdgeDistance(ULDProperties uldProperties) {
        return this.packingSequenceItem.calculateMinEdgeDistance(uldProperties);
    }

    public double calculateMaxEdgeDistance(ULDProperties uldProperties) {
        return this.packingSequenceItem.calculateMaxEdgeDistance(uldProperties);
    }


    //Getter and Setter
    public ItemCommodities getItemCommodities() {
        return this.ITEM_COMMODITIES;
    }

    public ItemCoordinates getItemCoordinates() {
        return this.ITEM_COORDINATES;
    }

    public EnvironmentRelations getEnvironmentRelations() {
        return environmentRelations;
    }

    public void setEnvironmentRelations(EnvironmentRelations environmentRelations) {
        this.environmentRelations = environmentRelations;
    }

    public PackingSequenceItem getPackingSequenceItem() {
        return packingSequenceItem;
    }

    public void setPackingSequenceItem(PackingSequenceItem packingSequenceItem) {
        this.packingSequenceItem = packingSequenceItem;
    }

}