library(rjson)
FILES_PATTERN <- "*.json"

DATASET_1 <- "Data_1/"
DATASET_2 <- "Data_2/"
DATASETS <- c(DATASET_1, DATASET_2)

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

# insert here the minimal dimensions for item width, height, depth in cm
MIN_ITEM_DIMENSIONS <- 10

# min number of items on the ULD
MIN_NUM_ITEMS <- 2

# levels of epsilons to include in the sensitivity analysis
EPSILON_LEVELS_TRANSLATION_ROTATION <- c(5, 10, 15)


# insert here the project root path. In R it is difficult to obtain the current file location
PATH_TO_ROOT <- "<Path_to_Project_Root>"


#####################  Begin functions

updatePaths <- function(sensitivity_level) {
  PATH_TO_FOLDER <<- paste0(PATH_TO_ROOT, "/Data/")
  PATH_TO_DATASET <<- paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  
  PATH_TO_ANALYSIS <<- paste0(PATH_TO_ROOT, "/02_analysis")
  PATH_TO_CRITERIA <<- paste0(PATH_TO_ANALYSIS, "/0_assessment_criteria/AssesmentCriteria_mapping.json")
  
  PATH_TO_AERESULTS <<- paste0(PATH_TO_DATASET,"3_AeResults/", CURRENT_SCENARIO)
  PATH_TO_AEJOBS <<- paste0(PATH_TO_DATASET,"2_AeJobs/", CURRENT_SCENARIO)
  
  PATH_OUTPUT <<- PATH_TO_DATA <- paste0(PATH_TO_DATASET, "5_Final_results/sensitivity_analysis/", sensitivity_level)
  PATH_TO_BENCHMARK <<- paste0(PATH_TO_DATASET, "4_Benchmark/",CURRENT_SCENARIO,"/done_sensitivity_analysis/", sensitivity_level)
  PATH_TO_OUTPUT_DIRECTORY_SENSITIVITY <<-  paste0(PATH_TO_DATASET, "5_Final_results/sensitivity_analysis/")
  
  dir.create(PATH_TO_OUTPUT_DIRECTORY_SENSITIVITY, showWarnings = FALSE)
  dir.create(PATH_TO_BENCHMARK, showWarnings = FALSE)
}

# This function creates a large Dataframe out of the Ground Truth values (scores, names)
addBenchmark <- function() {
  setwd(PATH_TO_BENCHMARK)
  
  files_benchmark_done <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=FALSE)
  
  ############
  
  results <- c()
  for (file in files_benchmark_done) {
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
    row["ADAMS"] <- result[1]$score[1]
    results_df <- rbind(results_df, row)
  }
  return(results_df)
}

makeAssessmentName  <- function(criterion) {
  if(startsWith(criterion$criterionType, "PartialBase")){
    return(paste0("PartialBaseSupportCriterion", criterion$alpha * 100))
  }
  if(startsWith(criterion$criterionType, "PhysicalSimulationCriterion")){
    return(paste0("PhysicalSimulationCriterion", criterion$epsilonTranslation))
  }
  return(criterion$criterionType)
}

createResultsDf <- function(results) {
  results_df <- NULL
  results_df <- data.frame(matrix(ncol = 5, nrow = 0))
  
  benchmark_results <- addBenchmark()
  
  for(assessment in results) {
    solution <- assessment
    assessment_criteria = solution$assesmentEvaluationSet
    
    row <- data.frame(assessment$name)
    for(assessment_criterion in assessment_criteria) {
      assessment_name <- makeAssessmentName(assessment_criterion$criterion)
      row[paste0(assessment_name,"_score")] <- assessment_criterion$score
      row[paste0(assessment_name,"_runtime")] <- assessment_criterion$runtimeInMs
    }
    
    ground_truth <- benchmark_results[benchmark_results$name == assessment$name,]$ADAMS
    
    mins <- obtainMinItemDimension(assessment$name)
    n_items <- obtainN(assessment$name)
    
    min_item_width_depth <- mins[1]
    min_item_height <- mins[2]
    
    ## Apply filters ##
    if(length(ground_truth)==0) { # Filter all rows, where we dont haven an ADAMS GT value 
      next
    }
    if(min_item_width_depth < MIN_ITEM_DIMENSIONS) {
      next
    }
    if(min_item_height < MIN_ITEM_DIMENSIONS) {
      next
    }
    if(n_items < MIN_NUM_ITEMS) {
      next
    }
    ## End Apply filters ##
    
    row["Benchmark"] <- ground_truth
    row["n_items"] <- n_items
    
    results_df <- rbind(results_df, row)
  }
  return(results_df)
}

obtainN <- function(file_name) {
  job_file_path <- paste0(PATH_TO_AEJOBS, '/', file_name)
  json_data <- list(fromJSON(file = job_file_path))
  
  items = length(json_data[[1]][["ulds"]][[1]][["placedItems"]])
  return(items)
}
obtainMinItemDimension <- function(file_name) {
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
    criteria_name <- makeAssessmentName(criterion)
    criteriaList <- rbind(criteriaList, criteria_name)
  }
  return(criteriaList)
}

calculateApproachClassification <- function(results_df, approach) {
  score <- paste0(approach, "_score")
  runtime <- paste0(approach, "_runtime")
  
  n_smaller_than_benchmark <- which(round(results_df[score], 3) < round(results_df["Benchmark"], 3))
  n_larger_than_benchmark <- which(round(results_df[score], 3) > round(results_df["Benchmark"], 3))
  n_equals_benchmark <- which(round(results_df[score], 3) == round(results_df["Benchmark"], 3))
  
  n <- length(n_equals_benchmark) + length(n_smaller_than_benchmark) + length(n_larger_than_benchmark)
  
  accuracy <- length(n_equals_benchmark) / n
  ue <- length(n_smaller_than_benchmark) / n
  oe <- length(n_larger_than_benchmark) / n
  
  result <- data.frame(
    Approach = approach,
    Accuracy = accuracy,
    UE = ue,
    OE = oe,
    Runtime = mean(results_df[runtime][,1]),
    N = n
  )
  return(result)
}

summarizeResults <- function(results_df, criteria_list) {
  final_results <- data.frame()
  for(criterion in criteria_list) {
    final_results <- rbind(final_results, calculateApproachClassification(results_df, criterion))
  }
  return(final_results)
}

#####################  Begin Script

startSingle <- function(sensitivity_level) {
  criteria_list <- makeCriteriaList()
  
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
  final_results <- summarizeResults(results_df, criteria_list)
  
  setwd(PATH_OUTPUT)
  write.csv2(final_results, paste0("final_results_", sensitivity_level, "_",CURRENT_SCENARIO,".csv"))
}

startAll <- function() {
  for (dataset in DATASETS) {
    CURRENT_DATASET <<- dataset
    for (scenario in SCENARIOS) {
      for(sensitivity_level in EPSILON_LEVELS_TRANSLATION_ROTATION) {
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


