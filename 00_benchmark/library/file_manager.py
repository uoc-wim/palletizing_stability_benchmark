import json

def read_json(filePathImport):
    with open(filePathImport) as json_file:
        job_json = json.load(json_file)
        return job_json

def write_json(fileName, data):
    print("Writing Final Results!")
    with open(fileName, 'w') as f:
        json.dump(data, f)
