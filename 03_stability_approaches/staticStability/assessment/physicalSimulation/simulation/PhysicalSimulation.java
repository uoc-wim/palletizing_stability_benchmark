package com.wim.assessment.staticStability.physicalSimulation.simulation;

import com.bulletphysics.collision.broadphase.*;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.wim.palletizing.model.StabilityAssessmentEvaluationConfiguration;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.model.physics.AbstractPhysicsItem;
import com.wim.palletizing.model.physics.AbstractPhysicsItemFactory;
import com.wim.palletizing.model.physics.PhysicsULDImpl;

import javax.vecmath.Vector3f;
import java.util.UUID;

public class PhysicalSimulation {

    DiscreteDynamicsWorld physicsWorld;
    PhysicsULDImpl physicsULD;
    AbstractPhysicsItem physicsGround;

    protected PhysicalSimulation() {}

    public PhysicalSimulation(ULD uld, StabilityAssessmentEvaluationConfiguration config) {
        this.physicsWorld = initPhysicsWorld(config.broadPhase);

        this.physicsGround = AbstractPhysicsItemFactory.createRigidBodyGround(config.restitution, config.friction);
        this.physicsWorld.addRigidBody(this.physicsGround.getBody());

        try {
            ULD uld_scaled = PhysicalSimulationHelper.scaleULD(uld, config.itemScalingFactor);
            this.physicsULD =
                    AbstractPhysicsItemFactory.createULD(uld_scaled.properties.width * config.itemScalingFactor,
                    uld_scaled.properties.depth * config.itemScalingFactor, uld_scaled,
                    new Vector3f(),
                    UUID.randomUUID(),
                    config);
        } catch (Exception e){
            System.err.println("Exception!" + e);
        }
    }

    /**
     *
     * @param time - Seconds per Item
     * @param resolution - Internal Clock for number of intermediate steps
     * @param epsilonTranslation - Maximum threshold (translational) until an item is marked as unstable
     * @param epsilonRotation - Maximum threshold (rotational) until an item is marked as untable in every 3 axes
     *
     * @return score
     */
    public double simulate(float epsilonTranslation, int resolution, float time, float epsilonRotation) {
        int sequence = 0;

        while (sequence < this.physicsULD.getItemCount()) {
            this.physicsWorld.addRigidBody(this.physicsULD.getItem(sequence).getBody());


            this.physicsWorld.stepSimulation(time, (int) (time * resolution), //Does this produce errors?
                    1.f / (float) resolution);

            if (this.physicsULD.hasUnstableItems(sequence, epsilonTranslation, epsilonRotation)) {
                return this.physicsULD.getResult(sequence);
            }
            this.physicsULD.resetAllPositionsAndVelocities();
            sequence++;
        }
        return 1.0;
    }

    protected DiscreteDynamicsWorld initPhysicsWorld(String broadPhaseLabel) {
        BroadphaseInterface broadphase = null;
        switch (broadPhaseLabel) {
            case "simple": {broadphase = new SimpleBroadphase(); break;}
            case "dvbt": {broadphase = new DbvtBroadphase(); break;}
            case "axissweep": {broadphase = new AxisSweep3(new Vector3f(-2,-2,-2), new Vector3f(15,15,15));break;}
            case "axissweep32": {broadphase = new AxisSweep3_32(new Vector3f(-2,-2,-2), new Vector3f(15,15,15));break;}
            default: broadphase = new DbvtBroadphase();
        }

        DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver(); //only this one available

        DiscreteDynamicsWorld physicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver,
                collisionConfiguration);
        physicsWorld.setGravity(new Vector3f(0f, -9.8f, 0f));

        return physicsWorld;
    }
}
