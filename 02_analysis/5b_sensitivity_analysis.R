library(rjson)
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

DELTA_TRANSLATION_ROTATION_BASELINE <- c(15)



#####################  Begin functions

updatePaths <- function(sensitivity_level) {
  PATH_TO_FOLDER <<- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/"
  PATH_TO_DATASET <<- paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_CRITERIA <<- paste0(PATH_TO_FOLDER, "AssesmentCriteria_R_output.json")
  PATH_TO_AERESULTS <<- paste0(PATH_TO_DATASET,"3_AeResults/", CURRENT_SCENARIO)
  PATH_TO_AEJOBS <<- paste0(PATH_TO_DATASET,"2_AeJobs/", CURRENT_SCENARIO)
  PATH_OUTPUT <<- PATH_TO_DATA <- paste0(PATH_TO_DATASET, "5_Final_results/sensitivity_analysis/", sensitivity_level)
  PATH_TO_GT <<- paste0(PATH_TO_DATASET, "4_Ground_truth/",CURRENT_SCENARIO,"/done_sensitivity_analysis/", sensitivity_level)
  PATH_TO_OUTPUT_DIRECTORY_SENSITIVITY <<-  paste0(PATH_TO_DATASET, "5_Final_results/sensitivity_analysis/")
  
  dir.create(PATH_TO_OUTPUT_DIRECTORY_SENSITIVITY, showWarnings = FALSE)
  dir.create(PATH_TO_GT, showWarnings = FALSE)
}

# This function creates a large Dataframe out of the Ground Truth values (scores, names)
addGT <- function() {
  setwd(PATH_TO_GT)
  
  files_gt_done <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=FALSE)
  
  ############
  
  results <- c()
  for (file in files_gt_done) {
    #read json
    data <- list(fromJSON(file = file))
    
    #assign names according to filename
    data[1][[1]]$name <- substring(file, 1, nchar(file) - 5) # due to double .json.json endings
    
    #append
    results <- append(results, data)
  }
  
  results_df <- NULL
  results_df <- data.frame(matrix(ncol = 0, nrow = 0))
  
  for(result in results) {
    row <- data.frame(matrix(ncol = 0, nrow = 1))
    row["name"] <- result$name
    row["ADAMS_MIN"] <- result[1]$score[1]
    row["ADAMS_MAX"] <- result[1]$score[2]
    results_df <- rbind(results_df, row)
  }
  return(results_df)
}
make_assessment_name  <- function(criterion) {
  if(startsWith(criterion$criterionType, "PartialBase")){
    return(paste0("PartialBaseSupportCriterion", criterion$alpha * 100))
  }
  if(startsWith(criterion$criterionType, "PhysicalSimulationCriterion")){
    return(paste0("StaticLoadStabilityCriterionDelta", criterion$epsilonTranslation))
  }
  return(criterion$criterionType)
}

createResultsDf <- function(results) {
  results_df <- NULL
  results_df <- data.frame(matrix(ncol = 5, nrow = 0))
  
  GT_results <- addGT()
  
  for(assessment in results) {
    solution <- assessment
    assessmentCriteria = solution$assesmentEvaluationSet
    
    row <- data.frame(assessment$name)
    for(assessmentCriterion in assessmentCriteria) {
      assessment_name <- make_assessment_name(assessmentCriterion$criterion)
      row[paste0(assessment_name,"_score")] <- assessmentCriterion$score
      row[paste0(assessment_name,"_runtime")] <- assessmentCriterion$runtimeInMs
    }
    
    ground_truth <- GT_results[GT_results$name == assessment$name,]$ADAMS_MIN
    
    mins <- obtain_min_item_dimension(assessment$name)
    min_item_width_depth <- mins[1]
    min_item_height <- mins[2]
    
    ## Apply filters ##
    if(length(ground_truth)==0) { # Filter all rows, where we dont haven an ADAMS GT value 
      next
    }
    if(min_item_width_depth < MIN_ITEM_DIMENSIONS_X_Z) {
      next
    }
    if(min_item_height < MIN_ITEM_DIMENSIONS_Y) {
      next
    }
    ## End Apply filters ##
    
    row["ADAMS_MIN"] <- ground_truth
    row["n_items"] <- obtain_n_items(assessment$name)
    
    results_df <- rbind(results_df, row)
  }
  return(results_df)
}

obtain_n_items <- function(file_name) {
  job_file_path <- paste0(PATH_TO_AEJOBS, '/', file_name)
  json_data <- list(fromJSON(file = job_file_path))
  
  items = length(json_data[[1]][["ulds"]][[1]][["placedItems"]])
  return(items)
}
obtain_min_item_dimension <- function(file_name) {
  job_file_path <- paste0(PATH_TO_AEJOBS, '/', file_name)
  json_data <- list(fromJSON(file = job_file_path))
  
  items = json_data[[1]][["ulds"]][[1]][["placedItems"]]
  min_width_depth = 100
  min_height = 100
  
  for (item in items) {
    item_width = item[["shape"]][["width"]] 
    item_depth = item[["shape"]][["depth"]] 
    item_height = item[["shape"]][["height"]] 
    
    
    if(item_width < min_width_depth) {
      min_width_depth = item_width
    }
    if(item_depth < min_width_depth) {
      min_width_depth = item_depth
    }
    if(item_height < min_height) {
      min_height = item_height
    }
  }
  result = c(min_width_depth, min_height)
  
  return(result)
}


makeCriteriaList <- function() {
  criteriaList <- cbind()
  criteria <- data <- list(fromJSON(file = PATH_TO_CRITERIA))[[1]]$assessmentCriteria
  for(criterion in criteria) {
    criteria_name <- make_assessment_name(criterion)
    criteriaList <- rbind(criteriaList, criteria_name)
  }
  return(criteriaList)
}

calculateApproachClassificationModel3Some <- function(results_df, approach) {
  
  score <- paste0(approach, "_score")
  runtime <- paste0(approach, "_runtime")
  
  i_smaller_GT <- which(round(results_df[score], 3) < round(results_df["ADAMS_MIN"], 3))
  i_greater_GT <- which(round(results_df[score], 3) > round(results_df["ADAMS_MIN"], 3))
  i_equals_GT <- which(round(results_df[score], 3) == round(results_df["ADAMS_MIN"], 3))
  
  Smaller_GT <- results_df[i_smaller_GT, ]
  Greater_GT <- results_df[i_greater_GT, ]
  Equals_GT <- results_df[i_equals_GT, ]
  
  n <- length(i_equals_GT) + length(i_smaller_GT) + length(i_greater_GT)
  accuracy <- length(i_equals_GT) / n
  ue <- length(i_smaller_GT) / n
  oe <- length(i_greater_GT) / n
  
  RESULTS <- data.frame(
    Approach = approach,
    Smaller_than_GT = ue,
    Greater_than_GT = oe,
    Equals_GT = length(i_equals_GT)/n,
    Accuracy = accuracy,
    N = n,
    Smaller_than_GT_percent = round(length(i_smaller_GT) / n, 4) * 100,
    Greater_than_GT_percent = round(length(i_greater_GT) / n, 4) * 100,
    Equals_GT_percent = round(length(i_equals_GT) / n, 4) * 100,
    runtime = mean(results_df[runtime][,1])
  )
  return(RESULTS)
}

summarizeResults <- function(results_df, criteriaList) {
  final_results <- data.frame()
  for(criterion in criteriaList) {
    final_results <- rbind(final_results, calculateApproachClassificationModel3Some(results_df, criterion))
  }
  ## In case we want to compute mean and SD
  #final_results <- cbind(final_results,mean(results_df$n_items))
  #final_results <- cbind(final_results,sd(results_df$n_items))
  return(final_results)
}

#####################  Begin Script

startSingle <- function(sensitivity_level) {
  criteriaList <- makeCriteriaList()
  
  setwd(PATH_TO_DATASET)
  
  dir.create(PATH_OUTPUT, showWarnings = FALSE)
  
  # First: Obtain AeResults
  setwd(PATH_TO_AERESULTS)
  files_aeresults <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=FALSE)
  
  results <- c()
  for (file in files_aeresults) {
    #read json
    data <- list(fromJSON(file = file))
    
    #assign names according to filename
    data[1][[1]]$name <- file
    
    #append
    results <- append(results, data)
  }
  print(paste0("Evaluating ", length(files_aeresults), " files in AeResults"))
  results_df <- createResultsDf(results)
  final_results <- summarizeResults(results_df, criteriaList = criteriaList)
  
  setwd(PATH_OUTPUT)
  write.csv2(final_results, paste0("final_results_", sensitivity_level, "_",CURRENT_SCENARIO,".csv"))
}

startAll <- function() {
  for (dataset in DATASETS) {
    CURRENT_DATASET <<- dataset
    for (scenario in SCENARIOS) {
      for(sensitivity_level in DELTA_TRANSLATION_ROTATION_BASELINE) {
        CURRENT_SCENARIO <<- scenario
        print(paste0("Evaluating ", CURRENT_DATASET, " ", CURRENT_SCENARIO))
        updatePaths(sensitivity_level)
        startSingle(sensitivity_level)
      }
    }
  }
  print("Finished!")
}

startAll()


