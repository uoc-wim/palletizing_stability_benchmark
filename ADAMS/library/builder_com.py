def calculate_com(center_of_geometry, item_com_specification):
    x = center_of_geometry["x"] + item_com_specification["x"]
    y = center_of_geometry["y"] + item_com_specification["y"]
    z = center_of_geometry["z"] + item_com_specification["z"]
    return [x,y,z]

def calculate_and_attach_com(center_of_geometry, item_com_specification, rigid_body, label):
    final_label = label+'_MARKER_com'
    center_of_mass = calculate_com(center_of_geometry, item_com_specification)
    if final_label in rigid_body.Markers:
        return [center_of_mass, rigid_body.Markers[final_label]]

    # Calculate and attach the center of mass
    center_of_mass_marker = rigid_body.Markers.create(name=final_label)
    rigid_body.cm = center_of_mass_marker

    return [center_of_mass, center_of_mass_marker]