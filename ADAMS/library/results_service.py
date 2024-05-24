import config

def evaluate_single_measure_XYZ(key, results, item, step):
    return {
        "x": results[item.measures[key]["x"]]["Q"].values[step],
        "y": results[item.measures[key]["y"]]["Q"].values[step],
        "z": results[item.measures[key]["z"]]["Q"].values[step],
    }

def evaluate_single_measure_contact(contact, results, step):
    return {
        "name": contact,
        "force": {
            "x": results[contact + "_FORCE_x"]["Q"].values[step],
            "y": results[contact + "_FORCE_y"]["Q"].values[step],
            "z": results[contact + "_FORCE_z"]["Q"].values[step],
        },
        "torque": {
            "x": results[contact + "_TORQUE_x"]["Q"].values[step],
            "y": results[contact + "_TORQUE_y"]["Q"].values[step],
            "z": results[contact + "_TORQUE_z"]["Q"].values[step],
        }
    }

def make_output(results, item, step, contacts):
    output = {
        "time": results[item.measures[config.KEY_TRANSLATION]["x"]]["TIME"].values[step],
    }
    contact_outputs = []

    # store measurements
    for measurment_key in config.ALL_MEASUREMENT_KEYS:
        output[measurment_key] = evaluate_single_measure_XYZ(key=measurment_key, results=results, step= step, item=item)

    # store contacts
    for contact in contacts:
        if not item.name in contacts[contact].name:
            continue
        contact_outputs.append(evaluate_single_measure_contact(contact = contacts[contact].name, results=results, step=step))
    output[config.KEY_CONTACTS] = contact_outputs

    return output
