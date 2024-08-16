library(tidyverse)
library(rjson)
library(zoo)

FILES_PATTERN <- "*.json"

DATASET_1 <- "Data_1/"
DATASET_3 <- "Data_2/"
DATASETS <- c(DATASET_1, DATASET_2)

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

# Insert here the maximum thresholds for translation (max) in x-, y-, and z-axes and the rotation (max) in either direction
EPSILON_TRANSLATION <- 10
EPSILON_ROTATION <- 10

#Insert here the project root path. In R it is difficult to obtain the current file location
PATH_TO_ROOT <- "<Path_to_Project_Root>"

updatePaths <- function() {
  PATH_TO_FOLDER <<- paste0(PATH_TO_ROOT, "/Data/")
  PATH_TO_DATASET <<-  paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_BENCHMARK <<-  paste0(PATH_TO_DATASET, "/4_Benchmark/", CURRENT_SCENARIO)
  
  APPENDIX_INPUT <<-  "/done_intermediate/"
  APPENDIX_OUTPUT <<-  "/done/"
  
  PATH_TO_DIRECTORY_INPUT <<-  paste0(PATH_TO_BENCHMARK, APPENDIX_INPUT)
  PATH_TO_OUTPUT_DIRECTORY <<-  paste0(PATH_TO_BENCHMARK, APPENDIX_OUTPUT)
  
  setwd(PATH_TO_DIRECTORY_INPUT)
  unlink(paste0(PATH_TO_OUTPUT_DIRECTORY, "*"))
  dir.create(PATH_TO_OUTPUT_DIRECTORY, showWarnings = FALSE)
}

obtainSequenceForGivenDelta <- function(data) {
  for (k in 1:nrow(data)) {
    data_row <- data[k,]
    
    if(isUnstable(data_row)) {
      return(data_row$sequence)
    }
  }
  return(max(as.numeric(data$sequence)) + 1)
}

isUnstable <- function(row) {
    return(as.numeric(row$Trans_x) > EPSILON_TRANSLATION |
             as.numeric(row$Trans_y) > EPSILON_TRANSLATION |
             as.numeric(row$Trans_z) > EPSILON_TRANSLATION |
             as.numeric(row$Rot_x) > EPSILON_ROTATION |
             as.numeric(row$Rot_y) > EPSILON_ROTATION |
             as.numeric(row$Rot_z) > EPSILON_ROTATION )
}

readResults <- function() {
  setwd(PATH_TO_DIRECTORY_INPUT)
  files <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=TRUE)
  
  data_all <- data.frame()
  for (file in files) {
    setwd(PATH_TO_DIRECTORY_INPUT)
    #read json
    data <- data.frame(list(fromJSON(file = file))[[1]])
    
    unstable_sequence <- as.numeric(obtainSequenceForGivenDelta(data))
    n_items <- max(as.numeric(data$sequence)) + 1

    output <- data.frame()
    name <- file
    score <- unstable_sequence / n_items
    
    output <- rbind(output, c(score))
    colnames(output) <- c("score")
    
    writeOutput(output, file)
  }
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
      CURRENT_SCENARIO <<- scenario
      print(paste0("Evaluating Data [", CURRENT_DATASET, "] Scenario [", CURRENT_SCENARIO,"]"))
      updatePaths()
      readResults()
    }
  }
}

startAll()
print("Finished!")