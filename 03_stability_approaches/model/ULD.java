package com.wim.palletizing.model;

import com.wim.palletizing.geometry.dim3.Point3D;
import com.wim.palletizing.geometry.dim3.Polyhedron;
import com.wim.palletizing.geometry.dim3.Shape3D;
import com.wim.palletizing.libs.quickhull3d.QuickHull3D;
import com.wim.palletizing.model.item.PlacedItem;
import com.wim.palletizing.model.item.PlacedItemDTO;
import com.wim.palletizing.model.uld_properties.*;
import com.wim.palletizing.packing_sequence.model.PackingSide;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by christian on 08.07.17.
 */
public class ULD implements Iterable<PlacedItem> {
	public final Long id;

	/**
	 * Stores the uld metadata
	 */
	public final ULDProperties properties;

	/**
	 * Items which are placed on this uld
	 */
	private final HashMap<String, PlacedItem> placedItems;

	public Map<String, PlacedItem> getPlacedItems() {
		return this.placedItems;
	}

	private final List<PlacedItem> placedItemsSorted;

	public List<PlacedItemStacking> placedItemStackings;

	public double weightedScore = 0;
	/**
	 * Outer hull
	 */
	private Polyhedron hull;

	public ULD(ULDProperties properties) {
		id = null;
		this.properties = properties;
		placedItems = new HashMap<>();
		placedItemsSorted = new ArrayList<>(0);
		placedItemStackings = new ArrayList<>(0);
	}

	public ULD(ULDProperties properties, Collection<PlacedItem> placedItems) {
		id = null;
		this.properties = properties;
		this.placedItems = new HashMap<>();
		placedItemsSorted = placedItems.stream().sorted().collect(Collectors.toList());
		for (PlacedItem placedItem : placedItemsSorted)
			this.placedItems.put(placedItem.itemLabel, placedItem);
		placedItemStackings = new ArrayList<>(0);

	}

	public ULD(ULDProperties properties, ULDUnderConstruction ULDUnderConstruction) {
		id = null;
		this.properties = properties;
		placedItems = ULDUnderConstruction.placedItems;
		placedItemsSorted = placedItems.values().stream().sorted().collect(Collectors.toList());
		placedItemStackings = new ArrayList<>(0);

	}

	public ULD(ULDDTO ulddto) {
		id = ulddto.id;
		if (ulddto.properties instanceof CuboidULDPropertiesDTO) {
			properties = new CuboidULDProperties(ulddto.properties);
		} else if (ulddto.properties instanceof ContainerPropertiesDTO)
			properties = new ContainerProperties(ulddto.properties);
		else if (ulddto.properties instanceof PalletWithContourPropertiesDTO)
			properties = new PalletWithContourProperties(ulddto.properties);
		else {
			LogManager.getLogger().error("Unknown Type of ULDPropertiesDTO. Using {} as fallback",
					CuboidULDPropertiesDTO.class.getSimpleName());
			properties = new CuboidULDProperties(ulddto.properties);
		}

		placedItems = new HashMap<>();
		placedItemsSorted = new ArrayList<>(ulddto.placedItems.length);
		for (PlacedItemDTO placedItemDTO : ulddto.placedItems) {
			PlacedItem pi = new PlacedItem(placedItemDTO);
			placedItems.put(placedItemDTO.itemLabel, pi);
			placedItemsSorted.add(pi);
		}
		placedItemStackings = new ArrayList<>(0);
	}

	@Override
	public Iterator<PlacedItem> iterator() {
		return placedItemsSorted.iterator();
	}

	public ListIterator<PlacedItem> listIterator(int startingIndex) {
		return placedItemsSorted.listIterator(startingIndex);
	}

	public Stream<PlacedItem> stream() {
		return placedItemsSorted.stream();
	}

	public PlacedItem getItem(String itemLabel) {
		return placedItems.get(itemLabel);
	}

	public int getItemCount() {
		return placedItemsSorted.size();
	}

	public boolean isEmpty() {
		return placedItemsSorted.isEmpty();
	}

	public boolean isCuboid() {
		return properties.isCuboid();
	}

	public List<PlacedItem> getPlacedItemsSorted() {
		return placedItemsSorted;
	}
	public List<PlacedItem> getPlacedItemsSortedTopDown(){
		List<PlacedItem> placedItemsTopDown = placedItemsSorted.subList(0, placedItemsSorted.size());

		Collections.reverse(placedItemsTopDown);

		return placedItemsTopDown;
	}

	public Polyhedron getHull() {
		if (hull == null)
			synchronized (this) {
				if (hull == null) {
					if (placedItems.isEmpty())
						return null;

					QuickHull3D hull = new QuickHull3D();

					ArrayList<Double> points = new ArrayList<>(placedItems.size() * Shape3D.MAX_POINT_COUNT * 3);

                    for (PlacedItem pi : placedItems.values())
                        for (Point3D point : pi.shape.getPoints())
                        {
                            points.add(pi.getItemCoordinates().getX() + point.x);
                            points.add(pi.getItemCoordinates().getY() + point.y);
                            points.add(pi.getItemCoordinates().getZ() + point.z);
                        }

					double[] pointsArray = points.stream().mapToDouble(d -> d).toArray();
					hull.build(pointsArray);
					this.hull = new Polyhedron(hull);
				}
			}

		return hull;
	}

    public static boolean deepEquals(ULD firstULD, ULD secondULD)
    {
        for (PlacedItem placedItemFirstULD : firstULD)
        {
            PlacedItem placedItemSecondULD = secondULD.getItem(placedItemFirstULD.itemLabel);
            if (placedItemSecondULD == null || placedItemFirstULD.getItemCoordinates().getX() != placedItemSecondULD.getItemCoordinates().getX() || placedItemFirstULD.getItemCoordinates().getY() != placedItemSecondULD.getItemCoordinates().getY() || placedItemFirstULD.getItemCoordinates().getZ() != placedItemSecondULD.getItemCoordinates().getZ() || placedItemFirstULD.shape.rotationState != placedItemSecondULD.shape.rotationState)
                return false;
        }
        return true;
    }

	public int getNumberOfSupportedSides(PlacedItem item) {
		int numberOfSupportedSides = 0;
		Iterator<PlacedItem> iter = this.iterator();
		PlacedItem pi;
		EnumSet<PackingSide> supportedSides = EnumSet.noneOf(PackingSide.class);
		while (iter.hasNext()) {
			pi = iter.next();
			if (pi.isRightNeighborOf(item))
				supportedSides.add(PackingSide.RIGHT);
			else if (pi.isLeftNeighborOf(item))
				supportedSides.add(PackingSide.LEFT);
			else if (pi.isBehind(item))
				supportedSides.add(PackingSide.BACK);
			else if (pi.isInFrontOf(item))
				supportedSides.add(PackingSide.FRONT);
		}
		return supportedSides.size();
	}

	public void calculateItemSupportStructure() {
		LinkedList<PlacedItem> placedItems = new LinkedList<PlacedItem>(this.placedItems.values());

		// Outgoing from a single item sequence i, calculate all subsequent items j. Checks in both directions (time
		// invariant/agnostic)
		for (int i = 0; i < placedItems.size()-1; i++) {
			for(int j = i+1; j < placedItems.size(); j++) {
				placedItems.get(i).calculateAndStoreSupportRelation(placedItems.get(j));
		}
	}
	}

    /**
     * Checks which items are direct neighbors on the ULD
     */
    public void calculateNeighbors() {
        List<PlacedItem> checkedItems = new ArrayList<>();
        for (PlacedItem item : this.placedItemsSorted) {
            for (PlacedItem otherItem : this.placedItemsSorted) {
                if(!checkedItems.contains(otherItem) && !item.equals(otherItem) &&
                        item.checkForNeighborhood(otherItem)) {
                    item.getEnvironmentRelations().addItemToNeighbor(otherItem);
                    otherItem.getEnvironmentRelations().addItemToNeighbor(item);
                }
            }
            checkedItems.add(item);
        }
    }
}
