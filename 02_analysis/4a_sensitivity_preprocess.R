library(tidyverse)
library(rjson)
library(zoo)

FILES_PATTERN <- "*.json"


DATASET_1 <- "Data_1/"
DATASET_2 <- "Data_2/"
DATASETS <- c(DATASET_1, DATASET_2)

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

# insert here the epsilon translation and rotation for the sensitivity anaylsis
EPSILON_LEVELS_TRANSLATION_ROTATION <- c(5, 10, 15)

# insert here the project root path. In R it is difficult to obtain the current file location
PATH_TO_ROOT <- "<Path_to_Project_Root>"

updatePaths <- function(sensitivity_level) {
  PATH_TO_FOLDER <<- paste0(PATH_TO_ROOT, "/Data/")
  PATH_TO_DATASET <<-  paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_BENCHMARK <<-  paste0(PATH_TO_DATASET, "/4_Benchmark/",CURRENT_SCENARIO)
  APPENDIX_INPUT <<-  "/done_intermediate/"
  APPENDIX_OUTPUT <<-  paste0("/done_sensitivity_analysis/", sensitivity_level)
  APPENDIX_OUTPUT_SENSITIVITY <<- "/done_sensitivity_analysis/"
  
  PATH_TO_DIRECTORY_INPUT <<-  paste0(PATH_TO_BENCHMARK, APPENDIX_INPUT)
  PATH_TO_OUTPUT_DIRECTORY <<-  paste0(PATH_TO_BENCHMARK, APPENDIX_OUTPUT)
  PATH_TO_OUTPUT_DIRECTORY_SENSITIVITY <<-  paste0(PATH_TO_BENCHMARK, APPENDIX_OUTPUT_SENSITIVITY)
  
  setwd(PATH_TO_DIRECTORY_INPUT)
  unlink(paste0(PATH_TO_OUTPUT_DIRECTORY, "*"))
  dir.create(PATH_TO_OUTPUT_DIRECTORY_SENSITIVITY, showWarnings = FALSE)
  dir.create(PATH_TO_OUTPUT_DIRECTORY, showWarnings = FALSE)
}

obtainSequenceForGivenDelta <- function(data, delta_translation_rotation) {
  for (k in 1:nrow(data)) {
    data_row <- data[k,]
    
    if(isUnstable(data_row, delta_translation_rotation)) {
      return(data_row$sequence)
    }
  }
  return(max(as.numeric(data$sequence)) + 1)
}

isUnstable <- function(row, delta_translation_rotation) {
  return(as.numeric(row$Trans_x) > delta_translation_rotation |
           as.numeric(row$Trans_y) > delta_translation_rotation |
           as.numeric(row$Trans_z) > delta_translation_rotation |
           as.numeric(row$Rot_x) > delta_translation_rotation |
           as.numeric(row$Rot_y) > delta_translation_rotation |
           as.numeric(row$Rot_z) > delta_translation_rotation )
}

readResults <- function(sensitivity_level) {
  setwd(PATH_TO_DIRECTORY_INPUT)
  files <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=TRUE)
  
  data_all <- data.frame()
  for (file in files) {
    setwd(PATH_TO_DIRECTORY_INPUT)
    #read json
    data <- data.frame(list(fromJSON(file = file))[[1]])
    output <- data.frame()
    n_items <- max(as.numeric(data$sequence)) + 1
    name <- file
    
    unstable_sequence <- as.numeric(obtainSequenceForGivenDelta(data, sensitivity_level))
    score <- unstable_sequence / n_items
    output <- rbind(output, c(score))
    
    colnames(output) <- c("score")
    
    writeOutput(output, file)
  }
  
  return()
}

writeOutput <- function(results, name) {
  setwd(PATH_TO_OUTPUT_DIRECTORY)
  score <- c(results)
  df <- data.frame(score)
  
  outputFormat <- toJSON(df)
  write(outputFormat, paste0(name))
}

startAll <- function() {
  for (dataset in DATASETS) {
    CURRENT_DATASET <<- dataset
    for (scenario in SCENARIOS) {
      for(sensitivity_level in EPSILON_LEVELS_TRANSLATION_ROTATION) {
        CURRENT_SCENARIO <<- scenario
        print(paste0("Evaluating Data [", CURRENT_DATASET, "] Scenario [", CURRENT_SCENARIO,"]"))
        updatePaths(sensitivity_level)
        readResults(sensitivity_level)  
      }
    }
  }
}

startAll()
print("Finished!")