library(tidyverse)
library(rjson)
library(zoo)

FILES_PATTERN <- "*.json"

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1) #c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

DATASET_1 <- "Data_1/"
DATASET_2 <- "Data_2_Masterarbeit/"
DATASET_3 <- "Data_3_Ramos/"

DATASETS <- c(DATASET_2)#c(DATASET_1, DATASET_2, DATASET_3)

DELTA <- 5
ANGLE <- 5

updatePaths <- function() {
  PATH_TO_FOLDER <<- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/"
  PATH_TO_DATASET <<-  paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_GT <<-  paste0(PATH_TO_DATASET, "/4_Ground_truth/",CURRENT_SCENARIO)
  APPENDIX_INPUT <<-  "/done_raw/"
  APPENDIX_OUTPUT_INTERMEDIATE <<-  "/done_intermediate/"
  APPENDIX_OUTPUT <<-  "/done/"
  
  
  PATH_TO_DIRECTORY_INPUT <<-  paste0(PATH_TO_GT, APPENDIX_INPUT)
  PATH_TO_OUTPUT_DIRECTORY <<-  paste0(PATH_TO_GT, APPENDIX_OUTPUT)
  PATH_TO_INTERMEDIATE_OUTPUT_DIRECTORY <<- paste0(PATH_TO_GT, APPENDIX_OUTPUT_INTERMEDIATE)
  
  setwd(PATH_TO_DIRECTORY_INPUT)
  unlink(paste0(PATH_TO_OUTPUT_DIRECTORY, "*"))
  dir.create(PATH_TO_OUTPUT_DIRECTORY, showWarnings = FALSE)
}

obtainSequenceForGivenDelta <- function(result) {
  deltas <- result$deltas
  delta <- NULL
  for(delta in deltas) {
    itemDeltas <- delta$itemDeltas
    for(itemDelta in itemDeltas){
      if(itemDelta$euclid_distance > DELTA) {
        return(delta$sequence / result$maxSequence)
      }
    }
  }
  return (1)
}

readResults <- function() {
  setwd(PATH_TO_DIRECTORY_INPUT)
  files <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=TRUE)
  
  results_df <- NULL
  results_df <- data.frame()
  
  results <- c()
  for (file in files) {
    #read json
    data <- list(fromJSON(file = file))
    
    #assign names according to filename
    data[1][[1]]$name <- file
    
    #append
    results <- append(results, data)
  }
  return(results)
}

#Obtain ADAMS Results
getResults <- function() {
  ############
  results <- readResults()
  
  results_df <- NULL
  results_df <- data.frame()
  
  for(result in results) {
    row <- data.frame(matrix(ncol = 2, nrow = 1))
    row$name <- result$name
    row$score <- obtainSequenceForGivenDelta(result)
    
    results_df <- rbind(results_df, row)
  }
  return(results_df)
}

getDeltas <- function() {
  ############
  results <- readResults()
  results_df <- NULL
  results_df <- data.frame()
  
  for(result in results) {
    row <- data.frame(matrix(ncol = 0, nrow = 1))
    row$name <- result$name
    deltas <- result$deltas
    delta <- NULL
    
    for(delta in deltas) {
      itemDeltas <- delta$itemDeltas
      for(itemDelta in itemDeltas){
        row$delta <- itemDelta$euclid_distance
        results_df <- rbind(results_df, row)
      }
    }
  }
  return(results_df)
}

writeOutput <- function(results) {
  setwd(PATH_TO_OUTPUT_DIRECTORY)
  
  for (row in 1:nrow(results)) {
    name <- results[row, "name"]
    scoreResult <- results[row, "score"]
    
    score <- c(scoreResult)
    df <- data.frame(score)
    
    outputFormat <- toJSON(df)
    write(outputFormat, paste0(name))
  }
}

startAll <- function(delta) {
  DELTA <<- delta
  
  for (dataset in DATASETS) {
    CURRENT_DATASET <<- dataset
    for (scenario in SCENARIOS) {
      CURRENT_SCENARIO <<- scenario
      print(paste0("Evaluating Data [", CURRENT_DATASET, "] Scenario [", CURRENT_SCENARIO,"]"))
      updatePaths()
      test <- getResults()
      writeOutput(test)
    }
  }
}

startAll(DELTA)

##### Plotting

#hist(test$score)
#all_deltas <- getDeltas()

#sum(test$score)
#mean(test$score)
#sd(test$score)
#summary(test$score)

#sum(all_deltas$delta)
#mean(all_deltas$delta)
#sd(all_deltas$delta)
#summary(all_deltas$delta)

#write.csv2(all_deltas, "../All_deltas.csv")
#View(test$deltas[[1]]$itemDeltas$"0"$observations)