package com.wim.palletizing.model.physics;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.wim.palletizing.geometry.dim3.*;
import com.wim.palletizing.model.StabilityAssessmentEvaluationConfiguration;
import com.wim.palletizing.model.ULD;
import com.wim.palletizing.model.item.PlacedItem;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.UUID;

public class AbstractPhysicsItemFactory {
    public static AbstractPhysicsItem createPhysicsItem(PlacedItem placedItem, Vector3f offset,
                                                        StabilityAssessmentEvaluationConfiguration config) {
        CollisionShape shape = null;
        MotionState motionState = null;
        RigidBody body = null;
        RigidBodyConstructionInfo constructionInfo;
        AbstractPhysicsItem item = null;

        Vector3f inertia = new Vector3f(0, 0, 0);

        //Bullet places the bodies with their center at (0,0,0)
        Vector3f itemPosition = new Vector3f(
                (float) placedItem.getItemCoordinates().getX() * config.itemScalingFactor,
                (float) placedItem.getItemCoordinates().getY() * config.itemScalingFactor + config.itemHeightOffset,
                (float) placedItem.getItemCoordinates().getZ() * config.itemScalingFactor);
        itemPosition.add(offset);

        Transform transform = new Transform();
        transform.setIdentity();

        switch (placedItem.shape.type) {
            case BOX: {
                item = new PhysicsBox((Box) placedItem.shape, placedItem.itemLabel);
                shape = item.createCollisionShape();
                shape.setMargin(config.collisionMargin);
                itemPosition.add(item.getDimension());
                break;
            }
            case POLYGON_PRISM: {
                item = new PhysicsPolygonPrism((PolygonPrism) placedItem.shape, placedItem.itemLabel);
                shape = item.createCollisionShape();
                Vector3f positionDisplacement = item.getDimension();
                itemPosition.add(positionDisplacement);
                break;
            }
            case L_SHAPE: {
                LShape3D lshape = (LShape3D) placedItem.shape;
                item = new PhysicsLShape(lshape, placedItem.weight, placedItem.itemLabel);
                transform = ((PhysicsLShape) item).getPrincipalAxisTransform();
                shape = item.createCollisionShape();
                break;
            }
            case CYLINDER: {
                item = new PhysicsCylinder((Cylinder) placedItem.shape, placedItem.itemLabel);
                shape = item.createCollisionShape();
                itemPosition.add(item.getDimension());
                break;
            }
        }

        shape.calculateLocalInertia((float) placedItem.weight, inertia);

        /* Center of Mass */
        Point3D com = placedItem.centerOfMass;
        Vector3f com_vector = new Vector3f((float)com.x, (float)com.y, (float)com.z);

        Transform centerOfMassTransformFromItemToCoM = new Transform();
        Transform centerOfMassTransformFromCoMToItem = new Transform();

        centerOfMassTransformFromItemToCoM.setIdentity();
        centerOfMassTransformFromCoMToItem.setIdentity();

        centerOfMassTransformFromItemToCoM.origin.set(com_vector.x, com_vector.y, com_vector.z);
        centerOfMassTransformFromCoMToItem.origin.set(-com_vector.x, -com_vector.y, -com_vector.z);

        CompoundShape coMOffset = new CompoundShape();
        coMOffset.addChildShape(centerOfMassTransformFromCoMToItem, shape);
        coMOffset.calculateLocalInertia((float) placedItem.weight, inertia);

        //place transform, s.t. it points to the absolute coordinates of the com = middle of the compound shape
        transform.origin.add(centerOfMassTransformFromItemToCoM.origin);
        transform.origin.add(itemPosition);

        motionState = new DefaultMotionState(transform);
        constructionInfo = new RigidBodyConstructionInfo((float) placedItem.weight, motionState, coMOffset, inertia);

        //Parameterize
        constructionInfo.restitution = config.restitution;
        constructionInfo.friction = config.friction;
        constructionInfo.linearDamping = config.linearDamping;
        constructionInfo.angularDamping = config.angularDamping;

        body = new RigidBody(constructionInfo);

        body.activate();

        item.setBody(body);
        item.setStartingPosition(transform);

        return item;
    }

    public static PhysicsULDImpl createULD(float uldWidth, float uldDepth, ULD uld, Vector3f offset, UUID uldID,
                                           StabilityAssessmentEvaluationConfiguration config) {

        PhysicsULDImpl physicsUld = new PhysicsULDImpl(new Box(uldWidth,
                config.itemHeightOffset, uldDepth), uld, offset, uldID, config);

        CollisionShape shape = physicsUld.createCollisionShape();
        MotionState motionState = null;
        RigidBody body = null;
        RigidBodyConstructionInfo constructionInfo;

        Transform transform = new Transform();
        transform.setIdentity();

        Vector3f itemPosition = new Vector3f(uldWidth / 2.f,
                config.itemHeightOffset / 2.f,
                uldDepth / 2.f);

        itemPosition.add(offset);

        transform.origin.add(itemPosition);
        Vector3f inertia = new Vector3f(0, 0, 0);
        shape.calculateLocalInertia(200.f, inertia);
        motionState = new DefaultMotionState(transform);
        constructionInfo = new RigidBodyConstructionInfo(0, motionState, shape, inertia);

        //Parameterize
        constructionInfo.restitution = 0.0f;
        constructionInfo.friction = 0.9f;
        constructionInfo.linearDamping = 0.04f;
        constructionInfo.angularDamping = 0.0f;

        body = new RigidBody(constructionInfo);

        physicsUld.setBody(body);
        physicsUld.setStartingPosition(transform);
        return physicsUld;
    }


    public static AbstractPhysicsItem createRigidBodyGround(float restitution, float friction) {
        Transform transform = new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), new Vector3f(0, 0, 0), 1.0f));
        MotionState groundMotionState = new DefaultMotionState(transform);

        CollisionShape groundShape = new StaticPlaneShape(new Vector3f(0, 1, 0), 0);

        RigidBodyConstructionInfo groundBodyConstructionInfo = new RigidBodyConstructionInfo(0, groundMotionState, groundShape, new Vector3f(0, 0, 0));
        groundBodyConstructionInfo.restitution = restitution;
        groundBodyConstructionInfo.friction = friction;
        RigidBody groundRigidBody = new RigidBody(groundBodyConstructionInfo);

        AbstractPhysicsItem physicsGround = new PhysicsPlane();
        physicsGround.setBody(groundRigidBody);
        return physicsGround;
    }
}
