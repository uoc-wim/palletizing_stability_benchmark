library(tidyverse)
library(rjson)
library(zoo)
library(ggplot2)

FILES_PATTERN <- "*.json"

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

DATASET_1 <- "Data_1/"
DATASET_3 <- "Data_3_Ramos/"
DATASETS <- c(DATASET_1, DATASET_3)

MIN_ITEM_DIMENSIONS_X_Z <- 15
MIN_ITEM_DIMENSIONS_Y <- 15

updatePaths <- function() {
  PATH_TO_FOLDER <<- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/"
  PATH_TO_DATASET <<-  paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_GT <<-  paste0(PATH_TO_DATASET, "/4_Ground_truth/",CURRENT_SCENARIO)
  PATH_TO_RESULTS <<- paste0(PATH_TO_DATASET, "5_Final_results")
  PATH_TO_MOVEMENT_ANALYSIS <<- paste0(PATH_TO_RESULTS, "/movement_analysis")
  APPENDIX_INPUT <<-  "/done_intermediate/"
  PATH_TO_DIRECTORY_INPUT <<-  paste0(PATH_TO_GT, APPENDIX_INPUT)
  
  setwd(PATH_TO_DIRECTORY_INPUT)
  dir.create(PATH_TO_MOVEMENT_ANALYSIS, showWarnings = FALSE)
}

# data = 1 layout
obtainSequenceForGivenDelta <- function(data) {
  vec <- data.frame()
  for (k in 1:nrow(data)) { # for every sequence
    row <- data[k,]
    
    vec <- rbind(vec, c(as.numeric(row$Trans_x),
             as.numeric(row$Trans_y),
             as.numeric(row$Trans_z),
             as.numeric(row$Rot_x),
             as.numeric(row$Rot_y),
             as.numeric(row$Rot_z)))
  }
  colnames(vec) <- c("trans_x", "trans_y", "trans_z", "rot_x", "rot_y", "rot_z")
  return(vec)
}


readResults <- function() {
  setwd(PATH_TO_DIRECTORY_INPUT)
  files <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=TRUE)
  
  data_all <- data.frame()
  
  for (file in files) {
    #read json
    data <- data.frame(list(fromJSON(file = file))[[1]])
    output <- data.frame()
    name <- file
    
    movements <- obtainSequenceForGivenDelta(data)
    
    data_all <- rbind(data_all, movements)
  }
  
  colnames(data_all) <- c("trans_x", "trans_y", "trans_z", "rot_x", "rot_y", "rot_z")
  
  return(data_all)
}

writeOutput <- function(results, dataset, scenario) {
  setwd(PATH_TO_MOVEMENT_ANALYSIS)
  name = paste0("movement_analysis_", dataset, "_", scenario, ".csv")
  name <- gsub("/", "", name)
  write.csv(results, name)
}

CURRENT_DATASET <- "Data_1/"
CURRENT_SCENARIO <- "Ulds_scenario_1"

updatePaths()
setwd(PATH_TO_MOVEMENT_ANALYSIS)
csv <- read.csv("movement_analysis_Data_1_Ulds_scenario_1.csv")
h <- hist(csv$trans_x)

# Plot with log-scaled y-axis
plot(h$mids, log10(h$counts), type = "h", main = "Histogram with Log-Scaled Y-Axis", 
     xlab = "Values", ylab = "Log10(Frequency)", lwd = 10, lend = 1)

# # Daten in Intervalle aufteilen
# # Erstellen Sie 5 Intervalle (Bins)
# breaks <- seq(min(csv$trans_x), max(csv$trans_x), length.out = 100)
# intervals <- cut(csv$trans_x, breaks = breaks, include.lowest = TRUE)
# 
# # Häufigkeit in jedem Intervall berechnen
# interval_table <- table(intervals)
# 
# # DataFrame für Boxplot erstellen
# df_interval <- data.frame(
#   Interval = names(interval_table),
#   Frequency = as.numeric(interval_table)
# )


#min <- csv$trans_x[csv$trans_x <= 0.1]
#hist(csv$trans_x)
  
print("Finished!")