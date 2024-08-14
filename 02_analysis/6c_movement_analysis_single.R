library(tidyverse)
library(rjson)
library(zoo)
library(ggplot2)
library(stringr)


FILES_PATTERN <- "*.json"

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1)

DATASET_1 <- "Data_1/"
DATASET_2 <- "Data_2/"
DATASET_3 <- "Data_3/"

DATASETS <- c(DATASET_1)

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
        #max_values <<- rbind(max_values, c(sequence$sequence, item$itemLabel, max(diffs$diff_translation_x),
        #                                   max(diffs$diff_translation_y), max(diffs$diff_translation_z),
        #                                   max(diffs$diff_rotation_x),max(diffs$diff_rotation_y),
        #                                   max(diffs$diff_rotation_z)))
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




colnames(plot_frame) <- c("time", "diff_translation_x","diff_translation_y", "diff_translation_z",
                          "diff_rotation_x","diff_rotation_y","diff_rotation_z", "itemLabel", "sequence")
colnames(max_values) <- c("sequence", "item","max_trans_x", "max_trans_y",
                          "max_trans_z","max_rot_x","max_rot_y", "max_rot_z")


calculateAUC <- function(delta) {
  frame <- data.frame()
  
  start_Position <- c(test$deltas[[delta]]$itemDeltas$"0"$observations[[1]]$translation$x,
                      test$deltas[[delta]]$itemDeltas$"0"$observations[[1]]$translation$y,
                      test$deltas[[delta]]$itemDeltas$"0"$observations[[1]]$translation$z
  )
  start_Rotation <- c(test$deltas[[delta]]$itemDeltas$"0"$observations[[1]]$rotation$x,
                      test$deltas[[delta]]$itemDeltas$"0"$observations[[1]]$rotation$y,
                      test$deltas[[delta]]$itemDeltas$"0"$observations[[1]]$rotation$z
  )
  
  for(observation in test$deltas[[delta]]$itemDeltas$"0"$observations) {
    row <- round(c(observation$time, 
                   observation$translation$x, observation$translation$y, observation$translation$z,
                   
                   #start_Position[1] - observation$translation$x, start_Position[2] - observation$translation$y, start_Position[3] - observation$translation$z,
                   abs(start_Position[1] - observation$translation$x), abs(start_Position[2] - observation$translation$y), abs(start_Position[3] - observation$translation$z),
                   
                   observation$rotation$x, observation$rotation$y, observation$rotation$z,
                   abs(start_Rotation[1] - observation$rotation$x), abs(start_Rotation[2] - observation$rotation$y), abs(start_Rotation[3] - observation$rotation$z),
                   
                   
                   observation$angular_momentum_about_cm$x, observation$angular_momentum_about_cm$y, observation$angular_momentum_about_cm$z,
                   
                   observation$cm_angular_velocity$x, observation$cm_angular_velocity$y, observation$cm_angular_velocity$z,
                   observation$cm_angular_acceleration$x, observation$cm_angular_acceleration$y, observation$cm_angular_acceleration$z,
                   observation$cm_acceleration$x, observation$cm_acceleration$y, observation$cm_acceleration$z,
                   observation$cm_position$x, observation$cm_position$y, observation$cm_position$z,
                   observation$cm_velocity$x, observation$cm_velocity$y, observation$cm_velocity$z,
                   observation$translational_momentum$x, observation$translational_momentum$y, observation$translational_momentum$z
    ),3)
    frame <- rbind(frame, row)
  }
  colnames(frame) <- c("time", 
                       "translation_x", "translation_y", "translation_z",
                       "translation_Delta_x", "translation_Delta_y", "translation_Delta_z",
                       "rotation_x", "rotation_y", "rotation_z",
                       "rotation_Delta_x", "rotation_Delta_y", "rotation_Delta_z",
                       "angular_momentum_about_cm_x", "angular_momentum_about_cm_y", "angular_momentum_about_cm_z",
                       "cm_angular_velocity_x", "cm_angular_velocity_y", "cm_angular_velocity_z",
                       "cm_angular_acceleration_x", "cm_angular_acceleration_y", "cm_angular_acceleration_z",
                       "cm_acceleration_x", "cm_acceleration_y", "cm_acceleration_z",
                       "cm_position_x", "cm_position_y", "cm_position_z",
                       "cm_velocity_x", "cm_velocity_y", "cm_velocity_z",
                       "translational_momentum_x", "translational_momentum_y", "translational_momentum_z"
  )  
}
