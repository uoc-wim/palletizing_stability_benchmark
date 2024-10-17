package com.wim.palletizing.model.physics;

import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import com.wim.palletizing.geometry.dim3.Box;
import com.wim.palletizing.model.StabilityAssessmentEvaluationConfiguration;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.model.item.PlacedItem;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PhysicsULDImpl extends PhysicsBox implements PhysicsULD {
    private UUID uuid;

    private ArrayList<AbstractPhysicsItem> itemListActive = new ArrayList<AbstractPhysicsItem>();
    private ArrayList<AbstractPhysicsItem> itemList = new ArrayList<AbstractPhysicsItem>();
    int sequence = 0;

    public PhysicsULDImpl(Box shape, ULD uld, Vector3f offset, UUID uuid,
                          StabilityAssessmentEvaluationConfiguration config) {
        super(shape, null, new Vector4f(1,1,1,1));
        this.uuid = uuid;

        for(PlacedItem placedItem : uld) {
            AbstractPhysicsItem item = AbstractPhysicsItemFactory.createPhysicsItem(placedItem, offset, config);
            this.addItem(item);
            this.itemList.add(item);
        }
        this.sequence = this.itemList.size();
    }

    public PhysicsULDImpl(Box shape, RigidBody body, ArrayList<AbstractPhysicsItem> itemList, Vector3f offset, UUID uuid ) {
        super(shape, null);
        this.body = body;
        this.uuid = uuid;
        for(AbstractPhysicsItem item : itemList) { //Adjust Transform accordung to ULD offset
            item.getStartingPosition().origin.add(offset);
            item.getBody().setWorldTransform(item.getStartingPosition());
        }
        this.itemListActive = itemList;
        this.itemList = (ArrayList<AbstractPhysicsItem>) List.copyOf(itemList);
    }

    @Override
    public void addItem(AbstractPhysicsItem item) {
        this.itemListActive.add(item);
    }

    @Override
    public AbstractPhysicsItem getItem(int index) {
        return this.itemListActive.get(index);
    }

    @Override
    public int getItemCount() {
        return this.itemListActive.size();
    }

    /**
     * Returns only the active items (that are currently part of the simulation)
     * @return
     */
    public List<AbstractPhysicsItem> getItemListActive() {
        return this.itemListActive;
    }

    /**
     * Also returns the non-active items
     * @return
     */
    public List<AbstractPhysicsItem> getItemListTotal() {
        return this.itemList;
    }

    @Override
    /**
     * Contains the sequence, that is placed in a stable manner
     * sequence = MAX_N -> all items are placed in a stable manner
     */
    public double getResult(int sequence) {
        return (double) sequence  / (this.getItemCount()); // itemCount == -1 is the highest sequence possible
    }

    public void resetAllPositionsAndVelocities() {
        this.resetPositionAndVelocity();

        for (AbstractPhysicsItem item : this.itemListActive) {
            item.resetPositionAndVelocity();
        }
    }

    public void removeItems(DiscreteDynamicsWorld physicsWorld) {
        this.resetAllPositionsAndVelocities();

        for (AbstractPhysicsItem item : this.itemListActive) {
            item.removeItemFromSimulation(physicsWorld);
        }
        this.itemListActive.clear();
        this.sequence = 0;
    }

    public void removeItem(DiscreteDynamicsWorld physicsWorld) {
        if(this.sequence <= 0) {
            return;
        }
        if(this.sequence > this.itemList.size()) {
            this.sequence--;
            return;
        }
        AbstractPhysicsItem item = this.itemList.get(sequence-1);
        this.itemListActive.remove(item);
        item.removeItemFromSimulation(physicsWorld);

        System.out.println(this.sequence-1);
        this.sequence--;
    }

    public void addItem(DiscreteDynamicsWorld physicsWorld) {
        if(sequence >= this.itemList.size()) {
            return;
        }
        AbstractPhysicsItem item = this.itemList.get(sequence);
        this.itemListActive.add(item);
        item.addItemToSimulation(physicsWorld);

        System.out.println(this.sequence);
        sequence++;
    }
    public void addItems(DiscreteDynamicsWorld physicsWorld) {
        for (AbstractPhysicsItem abstractPhysicsItem : this.itemListActive) {
            abstractPhysicsItem.addItemToSimulation(physicsWorld);
        }
    }

    private List<Double> getRotation(Quat4f q) {
        // TODO: Normalisieren? https://www.cbcity.de/tutorial-rotationsmatrix-und-quaternion-einfach-erklaert-in-din70000-zyx-konvention

        // roll (x-axis rotation)
        ArrayList<Double> angles = new ArrayList<Double>();
        double sinr_cosp = 2f * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1f - 2f * (q.x * q.x + q.y * q.y);
        double angle_x = Math.atan2(sinr_cosp, cosr_cosp);
        Double degree_x = Math.toDegrees(angle_x);

        // pitch (y-axis rotation)
        double sinp = Math.sqrt(1 + 2 * (q.w * q.y - q.x * q.z));
        double cosp = Math.sqrt(1 - 2 * (q.w * q.y - q.x * q.z)); // std::sqrt();
        double angle_y = 2 * Math.atan2(sinp, cosp) - (double)(Math.PI / 2f);
        Double degree_y = Math.toDegrees(angle_y);


        // yaw (z-axis rotation)
        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        double angle_z = Math.atan2(siny_cosp, cosy_cosp);
        Double degree_z = Math.toDegrees(angle_z);

        angles.add(degree_x);
        angles.add(degree_y);
        angles.add(degree_z);

        return angles;
    }

    private double calculateTranslation(AbstractPhysicsItem item) {
        Vector3f itemStartPosition = item.getStartingPosition().origin;
        Vector3f curPosition = item.getBody().getMotionState().getWorldTransform(new Transform()).origin;

        return Math.sqrt(Math.pow(itemStartPosition.x - curPosition.x, 2) + Math.pow(itemStartPosition.y - curPosition.y, 2) + Math.pow(itemStartPosition.z - curPosition.z, 2));
    }
    private double calculateMaxTranslationAbsolute(AbstractPhysicsItem item) {
        Vector3f itemStartPosition = item.getStartingPosition().origin;
        Vector3f curPosition = item.getBody().getMotionState().getWorldTransform(new Transform()).origin;

        return Math.max(curPosition.x - itemStartPosition.x, Math.max(curPosition.y - itemStartPosition.y,
                curPosition.z - itemStartPosition.z));

    }

    private double calculateMaxRotation(AbstractPhysicsItem item) {
        Quat4f rotationEnd = new Quat4f();
        item.getBody().getMotionState().getWorldTransform(new Transform()).getRotation(rotationEnd);
        List<Double> rotation = this.getRotation(rotationEnd);
        double max_rotation_on_any_axis = Math.abs(Collections.max(rotation));
        double min_rotation_on_any_axis = Math.abs(Collections.min(rotation));
        return Math.max(max_rotation_on_any_axis, min_rotation_on_any_axis);
    }

    public boolean hasUnstableItems(int sequence, float itemMovementEpsilon, float itemRotationEpsilon) {
        if(sequence >= this.getItemCount() ) {
            return true;
        }

        for(int i = 0; i <= sequence; i++) {
            AbstractPhysicsItem item = this.itemListActive.get(i);

            double maxRotation = this.calculateMaxRotation(item);
            double maxTranslation = this.calculateTranslation(item);

            //No typo, since we use absolute values for rotation
            if (maxTranslation >= itemMovementEpsilon || maxRotation > itemRotationEpsilon) {
                return true;
            }
        }
        return false;
    }
    public void removeULDFromWorld(DiscreteDynamicsWorld physicsWorld) {
        for(AbstractPhysicsItem item: this.itemListActive) {
            physicsWorld.removeRigidBody(item.getBody());
        }
        physicsWorld.removeRigidBody(this.getBody());
    }

    public double calclulateTotalMovement() {
        double totalMovement = 0;

        for (AbstractPhysicsItem uld : this.getItemListActive()) {
            totalMovement += uld.calculateTotalMovement();
        }
        return totalMovement;
    }

    public UUID getUuid() {
        return uuid;
    }

}
