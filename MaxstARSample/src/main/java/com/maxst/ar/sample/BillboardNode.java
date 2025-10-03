package com.maxst.ar.sample;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class BillboardNode extends Node {

    @Override
    public void onUpdate(FrameTime frameTime) {
        if (getScene() == null || getScene().getCamera() == null) return;

        Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
        Vector3 nodePosition = getWorldPosition();

        Vector3 direction = Vector3.subtract(cameraPosition, nodePosition);
        direction.y = 0; // Lock rotation to Y-axis only (optional)

        Quaternion lookRotation = Quaternion.lookRotation(direction.normalized(), Vector3.up());
        setWorldRotation(lookRotation);
    }
}

