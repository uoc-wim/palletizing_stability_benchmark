package com.wim.assessment.staticStability.sme.CriteriasToCompare;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wim.palletizing.assessment.AbstractAssessmentCriterion;
import com.wim.palletizing.assessment.AssessmentCriterionType;
import org.springframework.data.annotation.PersistenceConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wim.palletizing.geometry.dim3.Box;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.model.item.InputItemSet;
import com.wim.palletizing.model.item.PlacedItem;
import com.wim.palletizing.model.uld_properties.ULDProperties;

import javax.persistence.Entity;

@Entity
public class ScienceOfStaticKrebsCriterion extends AbstractAssessmentCriterion {

    private final double MIN_SUPPORT_FACTOR = 0.75d;
    private final double EPSILON = 0.001d;


    public static final AssessmentCriterionType type = AssessmentCriterionType.SCIENCE_OF_STATIC_KREBS;

    @PersistenceConstructor
    private ScienceOfStaticKrebsCriterion(Long id, double weight) {
        super(id, weight);
    }

    @JsonCreator
    public ScienceOfStaticKrebsCriterion(@JsonProperty(value = "weight", required = true) double weight) {
        this(null, weight);
    }

    /**
     * Hibernate workaround
     */
    @SuppressWarnings("unused")
    private ScienceOfStaticKrebsCriterion() {
        this(null, -1);
    }


    @Override
    protected double assessUld(ULD uld) {
        if (uld.isEmpty())
            return 1.0;

        for (PlacedItem item : uld.getPlacedItemsSorted())
            if (isNoBox(item))
                return -1;

        Map<String, List<PlacedItem>> itemsInCorridorBelow = assureULDSupportStructureHasBeenCalculated(uld);


        int counter = 0;
        for (PlacedItem item : uld.getPlacedItemsSorted()) {
            if (!isItemStable(item, itemsInCorridorBelow.get(item.itemLabel))) {
                break;
            }
            counter++;
        }
        return (double) counter / uld.getPlacedItemsSorted().size();
    }

    private boolean isNoBox(PlacedItem item) {
        return !(item.shape instanceof Box);
    }

    private Map<String, List<PlacedItem>> assureULDSupportStructureHasBeenCalculated(ULD uld) {


        PlacedItem testItem = uld.getPlacedItems().values().iterator().next();

        if (testItem.getEnvironmentRelations().getItemsBelow().isEmpty() && testItem.getEnvironmentRelations().getItemsOnTop().isEmpty())
            uld.calculateItemSupportStructure();

        return calculateItemsInCorridor(uld);

    }

    private Map<String, List<PlacedItem>> calculateItemsInCorridor(ULD uld) {
        Map<String, List<PlacedItem>> itemsInCorridorBelow = new HashMap<>();
        LinkedList<PlacedItem> pIList = new LinkedList<>(uld.getPlacedItems().values());


        for (int i = 0; i < pIList.size(); i++) {
            for (int j = i + 1; j < pIList.size(); j++) {

                PlacedItem p1 = pIList.get(i);
                PlacedItem p2 = pIList.get(j);

                int theoreticalY = p1.getMinPossibleYForOtherShape(p2.shape, p2.getItemCoordinates().getX(),
                        p2.getItemCoordinates().getZ());

                //No corridor intersection
                if (theoreticalY == 0)
                    continue;

                //Item directly on top
                if (theoreticalY == p2.getItemCoordinates().getY())
                    continue;

                //Item p1 in corridor below p2
                if (p2.getItemCoordinates().getY() > theoreticalY) {
                    if (!itemsInCorridorBelow.containsKey(p2.itemLabel))
                        itemsInCorridorBelow.put(p2.itemLabel, new LinkedList<>());
                    itemsInCorridorBelow.get(p2.itemLabel).add(p1);

                    continue;
                }

                theoreticalY = p2.getMinPossibleYForOtherShape(p1.shape, p1.getItemCoordinates().getX(),
                        p1.getItemCoordinates().getZ());

                //Item directly on top
                if (theoreticalY == p1.getItemCoordinates().getY())
                    continue;

                //Item p2 in corridor below p1
                if (p1.getItemCoordinates().getY() > theoreticalY) {
                    if (!itemsInCorridorBelow.containsKey(p1.itemLabel))
                        itemsInCorridorBelow.put(p1.itemLabel, new LinkedList<>());
                    itemsInCorridorBelow.get(p1.itemLabel).add(p2);
                }
            }
        }

        return itemsInCorridorBelow;
    }

    private boolean isItemStable(PlacedItem item, List<PlacedItem> itemsInCorridorBelow) {
        if (item.isBottomItem())
            return true;

        //Krebs checks for flying item(item with no support beneath). not possible by GA construction and therefore
        // skipped

        //Theoretically also for MinimalSupport should be checked
        if (isItemStableAfterKrebs(item, itemsInCorridorBelow)) {
            return isPartiallySupported(item);
        } else
            return false;
    }

    private boolean isPartiallySupported(PlacedItem item) {
        return ! (item.getBaseSupportFactor() < MIN_SUPPORT_FACTOR - EPSILON);
    }


    private boolean isItemStableAfterKrebs(PlacedItem item, List<PlacedItem> itemsInCorridorBelow) {

        double center_x = item.getItemCoordinates().getX() + ((Box) item.shape).width * 0.5;
        double center_z = item.getItemCoordinates().getZ() + ((Box) item.shape).depth * 0.5;

        if (itemsInCorridorBelow == null)
            itemsInCorridorBelow = new ArrayList<>();

        List<PlacedItem> allItemsSomehowBelow = Stream.concat(item.getEnvironmentRelations().getItemsBelow().stream(),
                        itemsInCorridorBelow.stream())
                .collect(Collectors.toList());


        return constructLevelsAndControlStabilityWithItems(center_x, center_z, allItemsSomehowBelow);


    }

    private boolean constructLevelsAndControlStabilityWithItems(double center_x, double center_z,
                                                                List<PlacedItem> itemsList) {

        for (PlacedItem itemA : itemsList) {
            int minX = itemA.getItemCoordinates().getX();
            int maxX = itemA.getItemCoordinates().getMaxX();
            int minZ = itemA.getItemCoordinates().getZ();
            int maxZ = itemA.getItemCoordinates().getMaxZ();

            for (PlacedItem itemB : itemsList) {
                if (itemA.equals(itemB))
                    continue;

                //If itemB lies in same level, find "outest" edges
                if (itemB.getItemCoordinates().getMaxY() >= itemA.getItemCoordinates().getMaxY() && itemB.getItemCoordinates().getY() < itemA.getItemCoordinates().getMaxY()) {

                    minX = Math.min(minX, itemB.getItemCoordinates().getX());
                    maxX = Math.max(maxX, itemB.getItemCoordinates().getMaxX());
                    minZ = Math.min(minZ, itemB.getItemCoordinates().getZ());
                    maxZ = Math.max(maxZ, itemB.getItemCoordinates().getMaxZ());
                }
            }

            if (center_x <= minX || center_x >= maxX || center_z <= minZ || center_z >= maxZ) {
                return false;
            }
        }

        return true;
    }

    @Override
    public AssessmentCriterionType getType() {
        return type;
    }

    @Override
    public void preProcessing(InputItemSet inputItemSet, HashMap enrichments, List<ULDProperties> uldPropertiesSet) {

    }

    @Override
    public AbstractAssessmentCriterion copy() {
        return new ScienceOfStaticKrebsCriterion(this.weight);
    }

}
