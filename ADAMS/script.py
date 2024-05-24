# imports
import sys
#from os import listdir, os.rename
import os

# IMPORTANT such that python searches this directory for modules
sys.path.append('./library')
sys.path.append('./')

from importlib import reload

import config
import file_manager
import process
import adams_service
import rigid_body_service

# Reload modules, since in ADAMS without reload no change is notified
config = reload(config)
file_manager = reload(file_manager)
process = reload(process)
adams_service = reload(adams_service)
builder_rigid_body = reload(rigid_body_service)

# sets up ground, gravity forces, ..
def initialize_model(fileName):
    model = Adams.Models.create(name=fileName)
    model.Forces.createGravity(name='GRAVITY', xyz_component_gravity=config.GRAVITY)

    i=Adams.defaults.model.settings.integrator  # returns model integrator settings object
    i.type=config.INTEGRATOR_TYPE    # important: integrator type should be hht (or: newmark) vs. gstif/wstif, see documentation
    i.error=config.INTEGRATOR_ERROR   # set integration error

    s=Adams.defaults.model.settings.solver
    s.threads=config.NUM_THREADS

    d = Adams.defaults
    d.units.length = config.UNITS_LENGTH
    d.units.mass = config.UNITS_MASS
    d.units.time = config.UNITS_TIME
    d.units.angle = config.UNITS_ANGLE

    adams_service.set_adams(Adams)
    builder_rigid_body.create_ground(model)

    return model

def get_current_model():
    return Adams.getCurrentModel()


def obtain_current_filepath_and_name():
    for root, dirs, files in os.walk(config.PATH_TO_JSON, topdown=False):
        for name in files:
            return [os.path.join(root, name), name]

def start_process():
    file_path, file_name = obtain_current_filepath_and_name()
    job = file_manager.read_json(file_path)
    result = simulate_single(file_name, job)
    os.rename(file_path, file_path.replace("jobs", "jobs_done"))
    file_manager.write_json(file_path.replace("jobs", "results") + ".json", result)

def simulate_single(file, job):
    process.delete_model(get_current_model())
    model = initialize_model(file)
    dryRun(job, model)

    model = initialize_model(file)
    return simulate(job, model, 0)

# Main simulation method.
def simulate(job, model, start_index, end_index = 1000):
    max_sequence = len(job["ulds"][0]["placedItems"])
    placed_items = sorted(job["ulds"][0]["placedItems"], key= lambda x : x['sequence'] )

    i = start_index

    deltaList = []
    items = []

    ending_sequence = end_index
    if(max_sequence < end_index):
        ending_sequence = max_sequence

    while i < ending_sequence:
        items.append(process.create_item_at_sequence(model, i, placed_items))
        results = process.evaluate_sequence(model, i, items)

        deltaList.append({
            "sequence": i,
            "itemDeltas": results
        })
        i = i + 1

    process.delete_all(model)
    return deltaList

def dryRun(job, model):
    simulate(job, model, 0, 2)
    process.delete_model(model)

start_process()