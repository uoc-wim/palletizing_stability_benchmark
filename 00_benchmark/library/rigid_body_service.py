from importlib import reload

import builder_geometry
import builder_location_marker
import builder_com
import material_service
import builder_measure
import config

builder_geometry = reload(builder_geometry)
builder_location_marker = reload(builder_location_marker)
material_service = reload(material_service)
builder_measure = reload(builder_measure)
builder_com = reload(builder_com)

def create_rigid_body(model, label):
    if label in model.Parts:
        return model.Parts[label]
    return model.Parts.createRigidBody(name=label, ground_part=False)


def create_rigid_body_ULD(dimensions, model):
    ground = model.Parts['ground']
    uldMarker = ground.Markers.create(name='ULD_MARKER',location = [-750,-0.5 + config.HEIGHT_FACTOR,-750], orientation = [0,0,0])
    uldBody = ground.Geometries.createBlock(name='ULD_Geometry', corner_marker=uldMarker,  x=dimensions[0], y=0.4, z=dimensions[1])
    uldBody.mass = 1000
    uldBody.material_type = material_service.get_material_aluminium(model)

def create_rigid_body_placed_item(model, item):
    shape = item["shape"]

    x = item["x"]
    y = item["y"] + config.HEIGHT_FACTOR
    z = item["z"]
    weight = item["weight"]

    coordinates = {"x": x, "y": y, "z": z}

    label = item["itemLabel"].replace("-","_")
    centerOfGeometry = {"x": 0, "y": 0, "z": 0}

    rigid_body = create_rigid_body(model, label)
    locationMarker = 0

    if shape["shapeType"] == "Box":
        width = shape["width"]
        depth = shape["depth"]
        height = shape["height"]

        centerOfGeometry = {"x": x + 0.5 * width, "y": y + 0.5 * height, "z": z + 0.5 * depth}
        locationMarker = builder_location_marker.create_location_marker_for_box(rigid_body, label, coordinates)
        builder_geometry.create_geometry_box(rigid_body, label, locationMarker, width, height, depth)

    # TODO
    elif shape["shapeType"] == "Cylinder":
        height = shape["height"]
        radius = shape["radius"]

        centerOfGeometry = {"x": x + radius, "y": y + 0.5 * height, "z": z + radius}
        locationMarker = builder_location_marker.createLocationMarkerForRigidBodyCylinder(model, rigid_body, label, coordinates, radius, height)
        builder_geometry.createGeometryCylinder(rigid_body, label, locationMarker, height, radius)

    # TODO
    elif shape["shapeType"] == "PolygonPrism":
        height = shape["height"]
        basePolygon = shape["basePolygon"]
        basePolygonScaled = []

        # scale each coordinate
        for point2D in basePolygon:
            basePolygonScaled.append({"x": point2D["x"], "y": point2D["y"]})

        max_x = 0
        max_y = 0
        centerOfGeometry = {"x": x + max_x/2, "y": y + 0.5 * height, "z": z + max_y/2}
        locationMarker = builder_location_marker.createLocationMarkerForRigidBodyPolygon(model, rigid_body, label, coordinates)
        builder_geometry.createGeometryPolygon(rigid_body, label, locationMarker, basePolygonScaled, height)

        #must be set after rigid body creation (I dont know why)
        locationMarker.orientation = [0,90,0]
        locationMarker.location = [x, height + y, z]

    # TODO
    elif shape["shapeType"] == "LShape":
        baseShape = shape["baseLShape"]
        width = baseShape["width"]
        core_width = baseShape["coreWidth"]
        height = baseShape["height"]
        core_height = baseShape["coreHeight"]
        cutOutPosition = baseShape["cutOutPosition"]

        thirdDimensionLength = shape["thirdDimensionLength"]
        orientation = shape["orientation"]

        centerOfGeometry = {"x": x + 0.5 * width, "y": y + 0.5 * height, "z": z + 0.5 * thirdDimensionLength}
        locationMarker = builder_location_marker.createLocationMarkerForRigidBodyPolygon(model, rigid_body, label, coordinates)
        builder_geometry.createGeometryLShape(rigid_body, label, locationMarker, thirdDimensionLength, cutOutPosition, width, core_width, height, core_height, orientation )

        # must be set after rigid body creation (I dont know why)
        locationMarker.location = [x, y, z]
        if orientation == "YZ":
            locationMarker.orientation = [90.0, 90.0, 270.0]
            locationMarker.location = [locationMarker.location[0], locationMarker.location[1], locationMarker.location[2] +  width]
        elif orientation == "XZ":
            locationMarker.orientation = [0.0, 270.0, 0.0]
            locationMarker.location = [locationMarker.location[0], locationMarker.location[1], locationMarker.location[2] +  width]

    # Calculate and attach the center of mass
    [center_of_mass, center_of_mass_marker] = builder_com.calculate_and_attach_com(centerOfGeometry, item["centerOfMass"], rigid_body, label)

    rigid_body.mass = weight
    density = calculate_density(item["weight"], item["shape"]["volume"])
    rigid_body.material_type = material_service.make_material(model, label + "_material", density)

    # Set start coordinates to values of location marker, since they might be altered by createLocationMarkerForRigidBodyBox
    # They are being compared later to the final coordinates
    startCoordinates = {
        "x": locationMarker.location[0],
        "y": locationMarker.location[1],
        "z": locationMarker.location[2]
    }

    # attach measurements
    rigid_body.measures = builder_measure.create_measures(model, label, locationMarker, center_of_mass_marker, rigid_body)
    rigid_body.position = {"start": startCoordinates }

    return {"body": rigid_body, "com_location": center_of_mass}

def create_ground(model):
    ground = model.Parts['ground']
    groundMarker = ground.Markers.create(name='GROUND_MARKER',location = [-500,5.5,-500], orientation = [0,0,0])
    ground.Geometries.createBlock(name='ground_Geometry',
                                  corner_marker=groundMarker,
                                  x=2000,
                                  y=1,
                                  z=2000,
                                  color_name='CadetBlue'
                                  )
    create_rigid_body_ULD([1500, 1500], model)

# weight in kg, volume in cm^3
def calculate_density(weight, volume):
    return weight/volume