package com.wim.palletizing.model.physics;

import com.bulletphysics.collision.broadphase.BroadphaseNativeType;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.CompoundShapeChild;
import com.bulletphysics.linearmath.Transform;
import com.wim.palletizing.geometry.dim3.Box;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

public class PhysicsBox extends AbstractPhysicsItem {
    public PhysicsBox(Box shape, String itemLabel) {
        super(shape, itemLabel);
    }

    public PhysicsBox(Box shape, String itemLabel, Vector4f color) {
        super(shape, itemLabel, color);
    }

    public CollisionShape createCollisionShape() {
        Box box = (Box) shape;
        Vector3f dim = new Vector3f((float) box.width / 2.0f, (float) box.height / 2.0f, (float) box.depth / 2.0f);
        this.dimension = dim;
        return new BoxShape(dim);
    }
}
