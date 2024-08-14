library(tidyverse)
library(rjson)
library(zoo)

FILES_PATTERN <- "*.json"

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

DATASET_1 <- "Data_1/"
DATASET_3 <- "Data_3_Ramos/"
DATASET_Abstimmung <- "Data_Messinstrumentabstimmung/"

DATASETS <- c(DATASET_3)

DELTA_TRANSLATION <- 10
DELTA_ROTATION <- 10

updatePaths <- function() {
  PATH_TO_FOLDER <<- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/"
  PATH_TO_DATASET <<-  paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_GT <<-  paste0(PATH_TO_DATASET, "/4_Ground_truth/",CURRENT_SCENARIO)
  APPENDIX_INPUT <<-  "/done_intermediate/"
  APPENDIX_OUTPUT <<-  "/done/"
  
  
  PATH_TO_DIRECTORY_INPUT <<-  paste0(PATH_TO_GT, APPENDIX_INPUT)
  PATH_TO_OUTPUT_DIRECTORY <<-  paste0(PATH_TO_GT, APPENDIX_OUTPUT)
  
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
    return(as.numeric(row$Trans_x) > DELTA_TRANSLATION |
             as.numeric(row$Trans_y) > DELTA_TRANSLATION |
             as.numeric(row$Trans_z) > DELTA_TRANSLATION |
             as.numeric(row$Rot_x) > DELTA_ROTATION |
             as.numeric(row$Rot_y) > DELTA_ROTATION |
             as.numeric(row$Rot_z) > DELTA_ROTATION )
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
    #data_all<- rbind(data_all, data)
  }
  #hist(as.numeric(data_all$AUC_Trans_y))
  #printSummary(data_all)
  #View(data_all)
  
  return()
}

printSummary <- function(data_all) {
  print("AUC_Translation X: ")
  print(summary(as.numeric(data_all$AUC_Trans_x)))
  print("AUC_Translation y: ")
  print(summary(as.numeric(data_all$AUC_Trans_y)))
  print("AUC_Translation z: ")
  print(summary(as.numeric(data_all$AUC_Trans_z)))
  print("AUC_Rotation X: ")
  print(summary(as.numeric(data_all$AUC_Rot_x)))
  print("AUC_Rotation Y: ")
  print(summary(as.numeric(data_all$AUC_Rot_y)))
  print("AUC_Rotation Z: ")
  print(summary(as.numeric(data_all$AUC_Rot_z)))
  
  print("Translation X: ")
  print(summary(as.numeric(data_all$Trans_x)))
  print("Translation y: ")
  print(summary(as.numeric(data_all$Trans_y)))
  print("Translation z: ")
  print(summary(as.numeric(data_all$Trans_z)))
  print("Rotation X: ")
  print(summary(as.numeric(data_all$Rot_x)))
  print("Rotation Y: ")
  print(summary(as.numeric(data_all$Rot_y)))
  print("Rotation Z: ")
  print(summary(as.numeric(data_all$Rot_z)))
  
  print("Euclidean Translation: ")
  print(summary(as.numeric(data_all$Euclidean_Translation)))
  print("Euclidean Rotation: ")
  print(summary(as.numeric(data_all$Euclidean_Rotation)))
  
  hist(as.numeric(data_all$Euclidean_Translation))
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