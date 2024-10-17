package com.wim.palletizing.model.physics;

import com.bulletphysics.collision.shapes.CollisionShape;

import static org.lwjgl.opengl.GL11.*;

public class PhysicsPlane extends AbstractPhysicsItem {
    public void draw() {
        glBegin(GL_QUADS);
        glColor4f(0.1f, 0.6f, 0.6f, 1);
        glVertex3f(-1000, 0, -1000);
        glVertex3f(-1000, 0, 1000);
        glVertex3f(1000, 0, 1000);
        glVertex3f(1000, 0, -1000);
        glEnd();
    }
    public PhysicsPlane() {
        super();
    }

    @Override
    public CollisionShape createCollisionShape() {
        return null;
    }
}
