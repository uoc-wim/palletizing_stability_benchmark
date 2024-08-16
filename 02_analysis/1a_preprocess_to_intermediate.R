library(tidyverse)
library(rjson)
library(zoo)
library(ggplot2)
library(stringr)

FILES_PATTERN <- "*.json"

DATASET_1 <- "Data_1/"
DATASET_3 <- "Data_2/"
DATASETS <- c(DATASET_1, DATASET_2)

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

#Insert here the project root path. In R it is difficult to obtain the current file location
PATH_TO_ROOT <- "<Path_to_Project_Root>"

updatePaths <- function() {
  PATH_TO_FOLDER <<- paste0(PATH_TO_ROOT, "/Data/")
  
  PATH_TO_DATASET <<-  paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_GT <<-  paste0(PATH_TO_DATASET, "/4_Benchmark/", CURRENT_SCENARIO)
  APPENDIX_INPUT <<-  "/done_raw/"
  APPENDIX_OUTPUT_INTERMEDIATE <<-  "/done_intermediate/"
  APPENDIX_OUTPUT <<-  "/done/"
  
  
  PATH_TO_DIRECTORY_INPUT <<-  paste0(PATH_TO_GT, APPENDIX_INPUT)
  PATH_TO_OUTPUT_DIRECTORY <<-  paste0(PATH_TO_GT, APPENDIX_OUTPUT)
  PATH_TO_INTERMEDIATE_OUTPUT_DIRECTORY <<- paste0(PATH_TO_GT, APPENDIX_OUTPUT_INTERMEDIATE)
  
  setwd(PATH_TO_DIRECTORY_INPUT)
  unlink(paste0(PATH_TO_OUTPUT_DIRECTORY, "*"))
  dir.create(PATH_TO_INTERMEDIATE_OUTPUT_DIRECTORY, showWarnings = FALSE)
  dir.create(PATH_TO_OUTPUT_DIRECTORY, showWarnings = FALSE)
}

AUC <- function(x, y){
  return(sum(diff(x)*rollmean(y,2)))
}

getIntermediateResults <- function() {
  setwd(PATH_TO_DIRECTORY_INPUT)
  files <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=TRUE)
  i <- 0
  
  for (file in files) {
    setwd(PATH_TO_DIRECTORY_INPUT)
    #read json
    json <- list(fromJSON(file = file))[[1]]
    
    large_frame <- data.frame(matrix(ncol = 8, nrow = 0))
   
    
    for(sequence in json) { #todo: change this parameter for newer versions
      for(item in sequence$itemDeltas) {
        start_Position <- c(item$observations[[1]]$translation$x,
                            item$observations[[1]]$translation$y,
                            item$observations[[1]]$translation$z
        )
        start_Rotation <- c(item$observations[[1]]$rotation$x,
                            item$observations[[1]]$rotation$y,
                            item$observations[[1]]$rotation$z
        )
        
        diffs <- data.frame()
        
        for(observation in item$observations) {
          abs_translation_x <- abs(start_Position[1] - observation$translation$x)
          abs_translation_y <- abs(start_Position[2] - observation$translation$y)
          abs_translation_z <- abs(start_Position[3] - observation$translation$z)
          
          abs_rotation_x <- abs(observation$rotation$x)
          abs_rotation_y <- abs(observation$rotation$y)
          abs_rotation_z <- abs(observation$rotation$z)
          
          diffs <- rbind(diffs, c(observation$time, abs_translation_x, abs_translation_y, abs_translation_z,
                                  abs_rotation_x, abs_rotation_y,abs_rotation_z) )
        }
        
        
        colnames(diffs) <- c("time", "diff_translation_x","diff_translation_y", "diff_translation_z",
                             "diff_rotation_x","diff_rotation_y","diff_rotation_z")
        
        row <- c(sequence$sequence, item$itemLabel, 
                 AUC(diffs$time, diffs$diff_translation_x),AUC(diffs$time, diffs$diff_translation_y),AUC(diffs$time, diffs$diff_translation_z),
                 AUC(diffs$time, diffs$diff_rotation_x),AUC(diffs$time, diffs$diff_rotation_y), AUC(diffs$time, diffs$diff_rotation_z), 
                 tail(diffs$diff_translation_x, n=1), tail(diffs$diff_translation_y, n=1), tail(diffs$diff_translation_z, n=1), # last timestep - first timestep
                 tail(diffs$diff_rotation_x, n=1), tail(diffs$diff_rotation_y, n=1), tail(diffs$diff_rotation_z, n=1),
                 euclidean(tail(diffs$diff_translation_x, n=1), tail(diffs$diff_translation_y, n=1), tail(diffs$diff_translation_z, n=1)),
                 euclidean(tail(diffs$diff_rotation_x, n=1), tail(diffs$diff_rotation_y, n=1), tail(diffs$diff_rotation_z, n=1))
        )
        large_frame <- rbind(large_frame, row)
      }
    }
    colnames(large_frame) <- c("sequence", "itemLabel", "AUC_Trans_x", "AUC_Trans_y", "AUC_Trans_z", "AUC_Rot_x","AUC_Rot_y", "AUC_Rot_z", "Trans_x", "Trans_y", "Trans_z", "Rot_x", "Rot_y", "Rot_z", "Euclidean_Translation", "Euclidean_Rotation")
    
    writeIntermediateOutput(large_frame, file)
    
    print(paste0(i, " ", file))
    i <- i+1
  }
  return()
}

euclidean <- function(a, b, c) sqrt(sum((a^2 + b^2 + c^2)))

writeIntermediateOutput <- function(intermediate_results, fileName) {
  setwd(PATH_TO_INTERMEDIATE_OUTPUT_DIRECTORY)
  outputFormat <- toJSON(intermediate_results)
  write(outputFormat, fileName)
}

startIntermediate <- function() {
  for (dataset in DATASETS) {
    CURRENT_DATASET <<- dataset
    for (scenario in SCENARIOS) {
      CURRENT_SCENARIO <<- scenario
      print(paste0("Evaluating [", CURRENT_DATASET, "] / [", CURRENT_SCENARIO,"]"))
      updatePaths()
      getIntermediateResults()
    }
  }  
}

startIntermediate()