### Geometry Functions
def create_geometry_box(rigidBody, label, corner_marker, width, height, depth):
    final_label = label+'_Geometry'
    if final_label in rigidBody.Geometries:
        return rigidBody.Geometries[final_label]
    return rigidBody.Geometries.createBlock(name=final_label, corner_marker=corner_marker, x=width, y=height, z=depth)

def createGeometryCylinder(rigidBody, label, center_marker, height, radius):
    return rigidBody.Geometries.createCylinder(name=label+'_Geometry', center_marker=center_marker, radius = radius, length = height)

def createGeometryPolygon(rigidBody, label, reference_marker, points, height):
    basePoints = []
    for point2D in points:
        basePoints.append(point2D["x"])
        basePoints.append(point2D["y"])
        basePoints.append(0.0)

    #Append first point again, s.t. it closes
    basePoints.append(points[0]["x"])
    basePoints.append(points[0]["y"])
    basePoints.append(0.0)

    extrusion = rigidBody.Geometries.createExtrusion(name=label+'_Geometry', reference_marker=reference_marker,
                                                 analytical = True, #TODO
                                                 length_along_z_axis=height,
                                                 points_for_profile=basePoints
                                                 )

    return extrusion

def createGeometryLShape(rigidBody, label, reference_marker, thirdDimensionLength, cutOutPosition, width, core_width, height, core_height, orientation):
    basePoints = []

    if cutOutPosition == "TOP_RIGHT":
        basePoints.append(0)
        basePoints.append(0)
        basePoints.append(0)

        basePoints.append(width)
        basePoints.append(0)
        basePoints.append(0)

        basePoints.append(width)
        basePoints.append(core_height)
        basePoints.append(0)

        basePoints.append(core_width)
        basePoints.append(core_height)
        basePoints.append(0)

        basePoints.append(core_width)
        basePoints.append(height)
        basePoints.append(0)

        basePoints.append(0)
        basePoints.append(height)
        basePoints.append(0)

        basePoints.append(0)
        basePoints.append(0)
        basePoints.append(0)
    elif cutOutPosition == "TOP_LEFT":
        basePoints.append(0)
        basePoints.append(0)
        basePoints.append(0)

        basePoints.append(width)
        basePoints.append(0)
        basePoints.append(0)

        basePoints.append(width)
        basePoints.append(height)
        basePoints.append(0)

        basePoints.append(width - core_width)
        basePoints.append(height)
        basePoints.append(0)

        basePoints.append(width - core_width)
        basePoints.append(core_height)
        basePoints.append(0)

        basePoints.append(0)
        basePoints.append(core_height)
        basePoints.append(0)

        basePoints.append(0)
        basePoints.append(0)
        basePoints.append(0)
    elif cutOutPosition == "BOTTOM_LEFT":
        basePoints.append(width - core_width)
        basePoints.append(0)
        basePoints.append(0)

        basePoints.append(width)
        basePoints.append(0)
        basePoints.append(0)

        basePoints.append(width)
        basePoints.append(height)
        basePoints.append(0)

        basePoints.append(0)
        basePoints.append(height)
        basePoints.append(0)

        basePoints.append(0)
        basePoints.append(height - core_height)
        basePoints.append(0)

        basePoints.append(width - core_width)
        basePoints.append(height - core_height)
        basePoints.append(0)

        basePoints.append(width - core_width)
        basePoints.append(0)
        basePoints.append(0)
    elif cutOutPosition == "BOTTOM_RIGHT":
        basePoints.append(0)
        basePoints.append(0)
        basePoints.append(0)

        basePoints.append(core_width)
        basePoints.append(0)
        basePoints.append(0)

        basePoints.append(core_width)
        basePoints.append(height - core_height)
        basePoints.append(0)

        basePoints.append(width)
        basePoints.append(height - core_height)
        basePoints.append(0)

        basePoints.append(width)
        basePoints.append(height)
        basePoints.append(0)

        basePoints.append(0)
        basePoints.append(height)
        basePoints.append(0)

        basePoints.append(0)
        basePoints.append(0)
        basePoints.append(0)

    extrusion = rigidBody.Geometries.createExtrusion(name=label+'_Geometry', reference_marker=reference_marker,
                                                     analytical = True,
                                                     length_along_z_axis=thirdDimensionLength,
                                                     points_for_profile=basePoints
                                                     )

    return extrusion
