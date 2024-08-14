from importlib import reload

import adams_service
import config
import builder_measure

adams_service = reload(adams_service)
config = reload(config)
builder_measure = reload(builder_measure)

def create_geometry_tuples(model):
    geometry_tuples = []
    for key, rigidBody in model.Parts.items():
        for geometry in rigidBody.Geometries:
            for key2, rigidBody2 in model.Parts.items():
                for geometry2 in rigidBody2.Geometries:
                    if (geometry == 'ground_Geometry' and geometry2 == 'ULD_Geometry') or (geometry == 'ULD_Geometry' and geometry2 == 'ground_Geometry'):
                        continue
                    if geometry == geometry2 : # excludes self matches
                        continue
                    if ([geometry, geometry2] in geometry_tuples) or ([geometry2, geometry] in geometry_tuples ) :
                        continue
                    geometry_tuples.append([geometry, geometry2])
    return geometry_tuples

# defines contact forces between every object and every other object + floor
def assign_contact_forces(model):

    geometry_tuples = create_geometry_tuples(model)

    # Should be n (=Number of placed items) over k (=2)

    for geometry_tuple in geometry_tuples:

        # search geometries in the current model
        geometry = adams_service.search_for_element(geometry_tuple[0])
        geometry2 = adams_service.search_for_element(geometry_tuple[1])

        forceName = 'contact_'+geometry_tuple[0]+'_'+geometry_tuple[1]

        if forceName in model.Contacts:
            continue

        # create friction contacts in both ways
        contactForce = model.Contacts.createSolidToSolid(name=forceName,
                                                    i_geometry=[geometry],
                                                    j_geometry=[geometry2],
                                                    coulomb_friction="on",
                                                    mu_static=config.STATIC_FRICTION,
                                                    mu_dynamic=config.DYNAMIC_FRICTION,
                                                    stiction_transition_velocity=config.STICTION_TRANSITION_VELOCITY,
                                                    friction_transition_velocity=config.FRICTION_TRANSITION_VELOCITY
                                                    )

        contactForce.stiffness = config.STIFFNESS
        contactForce.damping = config.DAMPING
        contactForce.exponent = config.EXPONENT
        contactForce.dmax = config.DMAX

        builder_measure.create_force_measures(model, forceName, contactForce)

