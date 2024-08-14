library(rjson)
FILES_PATTERN <- "*.json"

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1)

DATASET_1 <- "Data_1/"
DATASET_3 <- "Data_3_Ramos/"
DATASETS <- c(DATASET_1)

MIN_ITEM_DIMENSIONS_X_Z <- 15
MIN_ITEM_DIMENSIONS_Y <- 15



#####################  Begin functions

updatePaths <- function() {
  PATH_TO_FOLDER <<- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/"
  PATH_TO_DATASET <<- paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_CRITERIA <<- paste0(PATH_TO_FOLDER, "AssesmentCriteria_R_output.json")
  PATH_TO_AERESULTS <<- paste0(PATH_TO_DATASET,"3_AeResults/", CURRENT_SCENARIO)
  PATH_TO_AEJOBS <<- paste0(PATH_TO_DATASET,"2_AeJobs/", CURRENT_SCENARIO)
  PATH_TO_GT <<- paste0(PATH_TO_DATASET, "4_Ground_truth/",CURRENT_SCENARIO,"/done")
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
    row["ADAMS"] <- result[1]$score
    
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

createResultsDf <- function(results) {
  results_df <- NULL
  results_df <- data.frame(matrix(ncol = 5, nrow = 0))
  
  GT_results <- addGT()
  
  for(assessment in results) {
    solution <- assessment
    assessmentCriteria = solution$assesmentEvaluationSet
    
    row <- data.frame(assessment$name)
    contour_label <- obtain_pallet_dimensions(assessment$name)
    
    ground_truth <- GT_results[GT_results$name == assessment$name,]$ADAMS
    
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
    
    results_df <- rbind(results_df, c(assessment$name, contour_label))
  }
  colnames(results_df) <- c("name","contour")
  
  #NOTE: AKE- Contour is not properly displayed
  
  # Convert the character vector to a factor
  factor_classes <- factor(results_df$contour)
  freq_table <- table(factor_classes)
  barplot(freq_table, main="Histogram of Classes", xlab="Class", ylab="Frequency", col="lightblue")
  View(freq_table)
  
  return(results_df)
}

obtain_pallet_dimensions <- function(file_name) {
  job_file_path <- paste0(PATH_TO_AEJOBS, '/', file_name)
  json_data <- list(fromJSON(file = job_file_path))
  
  pallet_properties = json_data[[1]][["ulds"]][[1]][["properties"]]
  bottom_area = pallet_properties[["bottomArea"]]
  contour_vertices = pallet_properties[["contourVertices"]]
  
  contour_label = pallet_properties[["contourLabel"]]
  result = c(contour_label)
  
  return(result)
}

#####################  Begin Script

startSingle <- function() {
  criteriaList <- makeCriteriaList()
  
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
}

startAll <- function() {
  for (dataset in DATASETS) {
    CURRENT_DATASET <<- dataset
    for (scenario in SCENARIOS) {
      CURRENT_SCENARIO <<- scenario
      print(paste0("Evaluating ", CURRENT_DATASET, " ", CURRENT_SCENARIO))
      updatePaths()
      startSingle()
    }
  }
  print("Finished!")
}

startAll()


