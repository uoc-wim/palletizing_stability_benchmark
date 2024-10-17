package com.wim.assessment.staticStability.physicalSimulation.simulation;

import com.wim.palletizing.model.StabilityAssessmentEvaluationConfiguration;
import com.wim.palletizing.model.ULD;

import java.util.ArrayList;
import java.util.List;

public class ItemMovementPhysicalSimulation extends PhysicalSimulation {

    public ItemMovementPhysicalSimulation(ULD uld, StabilityAssessmentEvaluationConfiguration config) {
        super(uld, config);
    }

        public List simulateItemMovement(int resolution, float time) {
        int sequence = 0;
        List<List<Double>> results = new ArrayList<>();
        List<Double> itemMovements = new ArrayList<>();
        List<Double> times = new ArrayList<>();

        this.physicsWorld.addRigidBody(this.physicsULD.getItem(sequence).getBody());

        for (int i = 0; i < time * resolution; i++) {
            this.physicsWorld.stepSimulation((float) 1 / (resolution), 1,
                    1.f / (float) resolution);
            double movement = this.physicsULD.getItemListActive().get(0).calculateTotalMovement();
            itemMovements.add(movement);
            times.add((double) time * i * (float) 1 / (time * resolution));
        }
        results.add(times);
        results.add(itemMovements);
        return results;
    }
}