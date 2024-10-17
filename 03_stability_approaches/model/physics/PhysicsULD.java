package com.wim.palletizing.model.physics;

import java.util.List;

public interface PhysicsULD {
    void addItem(AbstractPhysicsItem item);
    AbstractPhysicsItem getItem(int index);
    int getItemCount();
    double getResult(int sequence);
}
