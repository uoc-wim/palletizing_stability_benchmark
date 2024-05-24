adams = 0

def set_adams(Adams):
    global adams
    adams = Adams

def search_for_element(key):
    global adams
    if(adams is None) or (adams == 0):
        print("Error! You have to set Adams first using setAdams(Adams)")
    return adams.stoo(key)