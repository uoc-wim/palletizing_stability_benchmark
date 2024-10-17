package com.wim.palletizing.model.physics;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import com.wim.palletizing.geometry.dim3.Shape3D;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.Random;

public abstract class AbstractPhysicsItem {
    Transform startingPosition;

    protected RigidBody body;
    Vector3f dimension;
    Shape3D shape;
    Vector4f startingColor;
    Vector4f color;
    String itemLabel;

    public AbstractPhysicsItem(Shape3D shape, String itemLabel) {
        this.shape = shape;
        this.color = this.getRandomColor();
        this.startingColor = this.color;
        this.itemLabel = itemLabel;
    }
    public AbstractPhysicsItem(Shape3D shape, String itemLabel, Vector4f color) {
        this(shape, itemLabel);
        this.color = color;
        this.startingColor = this.color;
    }

    public AbstractPhysicsItem() {
        this.color = this.getRandomColor();
    }

    public abstract void draw();

    public void drawItem() {
        this.draw();
    }

    public abstract CollisionShape createCollisionShape();

    public RigidBody getBody() {
        return body;
    }

    public void setGhostBoxColor(){
        color = new Vector4f(255,255,255,0.7f);
    }

    public void setBody(RigidBody body) {
        this.body = body;
    }

    public void setStartingPosition(Transform startingPosition) {
        this.startingPosition = startingPosition;
    }

    public Transform getStartingPosition() {
        return this.startingPosition;
    }

    public Vector3f getDimension() {
        return this.dimension;
    }

    private Vector4f getRandomColor() {
        Random rand = new Random();
        double number = rand.nextInt(8 + 1);
        float r, g, b;
        if (number < 1) { //blue
            r = 0;
            g = 0;
            b = 255;
        }
        else if (number < 2) { //yellow
            r = 255;
            g = 255;
            b = 0;
        } else if (number < 3) { //rot
            r = 255;
            g = 0;
            b = 0;
        }else if (number < 4) { //orange
            r = 255;
            g = 135;
            b = 51;
        }else if (number < 5) { //pink
            r = 153;
            g = 0;
            b = 153;
        }else if (number < 6) { //gruen
            r = 0;
            g = 204;
            b = 0;
        }else if (number < 7) { //gruen
            r = 0;
            g = 0;
            b = 250;
        } else {  //lila
            r = 153;
            g = 0;
            b = 255;
        }
        return new Vector4f(r, g, b, 1.f);
    }

    public void setColor(Vector4f color) {
        this.color = color;
    }

    public void resetPositionAndVelocity() {
        this.getBody().setWorldTransform(this.startingPosition);
        if(!this.getBody().isStaticObject())
            this.getBody().setLinearVelocity(new Vector3f(0,0,0));
        this.getBody().activate();
        this.color = this.startingColor;
    }

    public double calculateTotalMovement() {
        Transform currentPosition = new Transform();
        this.body.getWorldTransform(currentPosition);
        return Math.sqrt(
                Math.pow(currentPosition.origin.x - this.startingPosition.origin.x,2)
                + Math.pow(currentPosition.origin.y - this.startingPosition.origin.y,2)
                + Math.pow(currentPosition.origin.z - this.startingPosition.origin.z,2));
    }
    public void addItemToSimulation(DiscreteDynamicsWorld physicsWorld) {
        physicsWorld.addRigidBody(this.getBody());
    }
    public void removeItemFromSimulation(DiscreteDynamicsWorld physicsWorld) {
        physicsWorld.removeRigidBody(this.getBody());
    }
}

