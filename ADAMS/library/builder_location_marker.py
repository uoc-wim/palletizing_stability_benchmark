## Location Marker functions
# Defines the location of the geometry
def create_location_marker_for_box(rigid_body, label, coordinates):
    final_label = label + '_MARKER'
    if final_label in rigid_body.Markers:
        return rigid_body.Markers[final_label]
    return rigid_body.Markers.create(name=final_label, location = [coordinates["x"], coordinates["y"], coordinates["z"]], orientation = [0, 0, 0])

    # Important to rotate cylinder (y=90)
def createLocationMarkerForRigidBodyCylinder(model, rigidBody, label, coordinates, radius, height):
    return rigidBody.Markers.create(name=label+'_MARKER',location = [coordinates["x"] + radius, coordinates["y"] + height,coordinates["z"] + radius], orientation = [0,90,0])

    # Important to set positional values AFTER rigid body generation (not here)
def createLocationMarkerForRigidBodyPolygon(model, rigidBody, label, coordinates):
    return rigidBody.Markers.create(name=label+'_MARKER',location = [0,0,0], orientation = [0,0,0])

#TODO
def createLocationMarkerForRigidBodyLShape(model, rigidBody, label, coordinates, radius, height):
    return 0
