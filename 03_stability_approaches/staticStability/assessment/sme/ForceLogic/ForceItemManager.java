package com.wim.assessment.staticStability.sme.ForceLogic;

import com.wim.palletizing.assessment.staticStability.sme.ForceLogic.ForceCalculation.ForceItemCalculation;
import com.wim.palletizing.assessment.staticStability.sme.model.ForceItemDTO;
import com.wim.palletizing.model.item.PlacedItem;

import java.util.List;

/**
 * @auhor Frederick Gamer
 * created February 2022
 *
 * Class to manage the access to and creation of ForceItems
 */
public class ForceItemManager {

    private final ForceItemRepository forceItemRepository;

    private int maxSequence;


    public ForceItemManager(int maxSequence) {
        this.maxSequence = maxSequence;

        forceItemRepository = new ForceItemRepository();
    }

    /**
     * Checks if a related ForceItem already exists. Create a ForceItem based on this placedItem if not.
     * Creates all forceItems, this forceItem needs as well.
     * @param placedItem the related item
     * @return a forceItemDto, representing the forces for the given item
     */
    public ForceItemDTO getOrCreateForceItemFromPlacedItem(PlacedItem placedItem) {

        if (forceItemRepository.relatedForceItemExists(placedItem))
            return forceItemRepository.getRelatedForceItem(placedItem);


        this.createAndSaveRelatedForceItem(placedItem);


        return forceItemRepository.getRelatedForceItem(placedItem);
    }

    /**
     * Checks if for all items on top a forceItem exists
     *
     * @param placedItem the item which topItems are used
     */
    private void assureForceItemsOnTopAlreadyExist(PlacedItem placedItem) {
        for (PlacedItem placedItemOnTop : placedItem.getEnvironmentRelations().getItemsOnTop()) {
            if (!forceItemRepository.relatedForceItemExists(placedItemOnTop)) {
                this.createAndSaveRelatedForceItem(placedItemOnTop);
            }
        }
    }

    /**
     * Creates and saves a forceItem for the related placedItem
     * @param placedItem representing the related item
     */
    private void createAndSaveRelatedForceItem(PlacedItem placedItem){
        assureForceItemsOnTopAlreadyExist(placedItem);


        ForceItemDTO forceItem = createRelatedForceItem(placedItem);

        forceItemRepository.addForceItem(forceItem);
    }

    /**
     * Creates a forceItem by calculating all acting forces for the given one
     * @param placedItem the related item
     * @return a forceItem, containing the force for the given item
     */
    private ForceItemDTO createRelatedForceItem(PlacedItem placedItem) {
        List<ForceItemDTO> forceItemsOnTop = forceItemRepository.getForceItemsOnTop(placedItem);

        return ForceItemCalculation.createForceItem(placedItem, forceItemsOnTop, this.maxSequence);
    }

    public int getMaxSequence() {
        return maxSequence;
    }

    public void setMaxSequence(int maxSequence) {
        this.maxSequence = maxSequence;
    }
}
