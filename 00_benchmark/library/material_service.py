import config

# This service manages the ADAMS materials. In general, we use a
# single material (which can be extended in the future)

# Creates the single material (cardboard)
def create_material(model, name, density):
    return model.Materials.create(
        name = name,
        density = density,
        orthotropic_constants = [config.MATERIAL_YOUNGS_MODULUS,
                                 config.MATERIAL_G_xz,
                                 config.MATERIAL_G_xy]
    )
def create_material_aluminium(model):
    return model.Materials.create(
        name = config.MATERIAL_GROUND_NAME,
        density = config.MATERIAL_GROUND_DENSITY,
        poissons_ratio = config.MATERIAL_GROUND_POISSONS_RATIO,
        youngs_modulus = config.MATERIAL_GROUND_YOUNGS_MODULUS
    )

def get_material_aluminium(model):
    if( config.MATERIAL_GROUND_NAME not in model.Materials.keys()):
        create_material_aluminium(model)
    return model.Materials[config.MATERIAL_GROUND_NAME]


# Returns the material if exists, creates it first otherwise
def make_material(model, name, density):
    if( name not in model.Materials.keys()):
        create_material(model, name, density)
    return model.Materials[name]