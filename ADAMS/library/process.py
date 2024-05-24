from importlib import reload
import rigid_body_service
import force_service
import results_service
import config

builder_rigid_body = reload(rigid_body_service)
force_service = reload(force_service)
results_service = reload(results_service)


# Cleans up the current ADAMS model if present
def delete_all(model):
    if model is None:
        return

    model.Analyses['Last_Run'].destroy()

    for measure in model.Measures:
        model.Measures[measure].destroy()
    for contact in model.Contacts:
        model.Contacts[contact].destroy()
    for simulation in model.Simulations:
        model.Simulations[simulation].destroy()
    for body in model.Parts:
        if body == "ground":
            continue
        for geometry in model.Parts[body].Geometries:
            model.Parts[body].Geometries[geometry].destroy()
        for marker in model.Parts[body].Markers:
            if marker.endswith('com'): #com marker is being deleted when part in destroyed -> avoids warning
                continue
            model.Parts[body].Markers[marker].destroy()

        model.Parts[body].destroy()

# Cleans up the current ADAMS model if present
def delete_model(model):
    if model is None:
        return
    model.destroy()
    del model


def start_simulation(model, from_sequence):
    simulation = model.Simulations.create(name='sim_' + str(from_sequence),
                                  end_time=config.END_TIME,
                                  number_of_steps=config.NUMBER_OF_STEPS)
    simulation.simulate()

def obtain_results(model, items):
    results = model.Analyses['Last_Run'].results
    deltas = dict()

    for item in items: # ORDERED Dictionary: TIME = Timestamp, Q = Value
        # len is equal for all measures, since it depends on the time
        steps_total = len(results[item.measures[config.KEY_TRANSLATION]["x"]]["TIME"].values)
        observations = []

        for step in range(steps_total):
            observation = results_service.make_output(results=results, item=item, step=step, contacts=model.Contacts)
            observations.append(observation)

        deltas[item.sequence] = {
            "itemLabel": item.name,
            "observations": observations
        }
    return deltas

# Reads out the items from the json and creates rigid bodies accordingly
def create_item_at_sequence(model, sequence, placed_items):
    placedItem = placed_items[sequence]
    rigidBody = builder_rigid_body.create_rigid_body_placed_item(model, placedItem)
    item = rigidBody["body"]
    item.cm.location = rigidBody["com_location"]
    item.sequence = sequence

    force_service.assign_contact_forces(model)

    return item

def evaluate_sequence(model, sequence, items):
    start_simulation(model, sequence)
    return obtain_results(model, items)
