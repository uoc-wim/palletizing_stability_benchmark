package com.wim.assessment.staticStability.sme.ForceLogic;

import com.wim.palletizing.assessment.staticStability.sme.model.ForceItemDTO;
import com.wim.palletizing.model.item.PlacedItem;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Frederick Gamer
 * created February 2022
 * Class to store and access ForceItems through their related PlacedItem (the global model item)
 */
@Validated
public class ForceItemRepository {

    Map<PlacedItem, ForceItemDTO> piToFiMap;

    public ForceItemRepository() {
        piToFiMap = new HashMap<>();
    }

    /**
     * Adds the forceItem to the repository
     * @param forceItem the forceItem to add
     */
    public void addForceItem(@NotNull ForceItemDTO forceItem) {
        piToFiMap.put(forceItem.getPLACED_ITEM(), forceItem);
    }

    /**
     * Queries the ForceItem for each placedItem on top of the given placedItem
     * @param placedItem used to determine the topItems
     * @return a List of the forceItems on Top
     */
    public List<ForceItemDTO> getForceItemsOnTop(@NotNull PlacedItem placedItem) {
        List<ForceItemDTO> forceItemsOnTop = new ArrayList<>();

        for (PlacedItem placedItemOnTop : placedItem.getEnvironmentRelations().getItemsOnTop()) {
            if (piToFiMap.containsKey(placedItemOnTop))
                forceItemsOnTop.add(piToFiMap.get(placedItemOnTop));

            else {
                throw new IllegalStateException("Tried to get ForceItem which has not been created yet");
            }
        }
        return forceItemsOnTop;
    }


    /**
     * Returns weather a forceItem, related to this placedItem exists
     * @param placedItem the placedItem related to the forceItem
     * @return the related forceItem
     */
    public boolean relatedForceItemExists(@NotNull PlacedItem placedItem) {
        return piToFiMap.containsKey(placedItem);
    }

    public ForceItemDTO getRelatedForceItem(@NotNull PlacedItem placedItem) {
        return piToFiMap.get(placedItem);
    }
}
