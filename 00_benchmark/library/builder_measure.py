import config
## Measurement Function
def create_measures(model, label, location_marker, center_of_mass_marker, rigidBody):
    translational = create_translational_measures(model, label, location_marker)
    rotational = create_orientational_measures(model, label, location_marker)
    other_measures = create_object_measures(model, label, rigidBody)

    return {
        config.KEY_TRANSLATION: translational,
        config.KEY_ROTATION: rotational,
        config.KEY_ANGULAR_MOMENTUM_ABOUT_CM: other_measures[config.KEY_ANGULAR_MOMENTUM_ABOUT_CM],
        config.KEY_CM_ANGULAR_VELOCITY: other_measures[config.KEY_CM_ANGULAR_VELOCITY],
        config.KEY_CM_ANGULAR_ACCELERATION: other_measures[config.KEY_CM_ANGULAR_ACCELERATION],
        config.KEY_CM_ACCELERATION: other_measures[config.KEY_CM_ACCELERATION],
        config.KEY_CM_POSITION: other_measures[config.KEY_CM_POSITION],
        config.KEY_CM_VELOCITY: other_measures[config.KEY_CM_VELOCITY],
    }

def create_translational_measures(model, label, location_marker):
    final_label_x =  label+'_TRANSL_x'
    final_label_y =  label+'_TRANSL_y'
    final_label_z =  label+'_TRANSL_z'

    if not final_label_x in model.Measures:
        model.Measures.createPoint(name=label+'_TRANSL_x', point=location_marker, characteristic='translational_displacement', component='x_component', create_measure_display=False)
    if not final_label_y in model.Measures:
        model.Measures.createPoint(name=label+'_TRANSL_y', point=location_marker, characteristic='translational_displacement', component='y_component', create_measure_display=False)
    if not final_label_z in model.Measures:
        model.Measures.createPoint(name=label+'_TRANSL_z', point=location_marker, characteristic='translational_displacement', component='z_component', create_measure_display=False)

    return {
        "x": final_label_x,
        "y": final_label_y,
        "z": final_label_z
    }

def create_orientational_measures(model, label, location_marker):
    final_label_x =  label+'_ORIENT_x'
    final_label_y =  label+'_ORIENT_y'
    final_label_z =  label+'_ORIENT_z'

    if not final_label_x in model.Measures:
        model.Measures.createOrient(name= label+'_ORIENT_x', to_frame=location_marker, characteristic='yaw_pitch_roll', component='angle_3_component', create_measure_display=False)
    if not final_label_y in model.Measures:
        model.Measures.createOrient(name= label+'_ORIENT_y', to_frame=location_marker, characteristic='yaw_pitch_roll', component='angle_2_component', create_measure_display=False)
    if not final_label_z in model.Measures:
        model.Measures.createOrient(name= label+'_ORIENT_z', to_frame=location_marker, characteristic='yaw_pitch_roll', component='angle_1_component', create_measure_display=False)

    return {
        "x": final_label_x,
        "y": final_label_y,
        "z": final_label_z
    }

def create_object_measures(model, label, rigidBody):
    result = dict()
    axes = dict()

    for measure in config.OBJECT_MEASUREMENT_KEY:
        for axis in ["x", "y", "z"]:
            final_label =  label + '_' + measure + '_' + axis
            if not final_label in model.Measures:
                model.Measures.createObject(name=final_label, object=rigidBody, characteristic=measure, component=axis+'_component', create_measure_display=False)
            axes.update({axis: final_label})
            result.update({measure: axes})
        axes = dict()
    return result

def create_force_measures(model, label, force):
    model.Measures.createObject(name= label+'_FORCE_x', object=force, characteristic='element_force', component='x_component', create_measure_display=False)
    model.Measures.createObject(name= label+'_FORCE_y', object=force, characteristic='element_force', component='y_component', create_measure_display=False)
    model.Measures.createObject(name= label+'_FORCE_z', object=force, characteristic='element_force', component='z_component', create_measure_display=False)

    model.Measures.createObject(name= label+'_TORQUE_x', object=force, characteristic='element_torque', component='x_component', create_measure_display=False)
    model.Measures.createObject(name= label+'_TORQUE_y', object=force, characteristic='element_torque', component='y_component', create_measure_display=False)
    model.Measures.createObject(name= label+'_TORQUE_z', object=force, characteristic='element_torque', component='z_component', create_measure_display=False)