library(rjson)
FILES_PATTERN <- "*.json"

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

DATASET_1 <- "Data_1/"
DATASET_3 <- "Data_3_Ramos/"
DATASETS <- c(DATASET_1, DATASET_3)


#####################  Begin functions

updatePaths <- function() {
  PATH_TO_FOLDER <<- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/"
  PATH_TO_DATASET <<- paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_CRITERIA <<- paste0(PATH_TO_FOLDER, "AssesmentCriteria_R_output.json")
  PATH_TO_AERESULTS <<- paste0(PATH_TO_DATASET,"3_AeResults/", CURRENT_SCENARIO)
  PATH_TO_AEJOBS <<- paste0(PATH_TO_DATASET,"2_AeJobs/", CURRENT_SCENARIO)
  PATH_OUTPUT <<- PATH_TO_DATA <- paste0(PATH_TO_DATASET, "5_Final_results/")
  PATH_TO_GT <<- paste0(PATH_TO_DATASET, "4_Ground_truth/",CURRENT_SCENARIO,"/done")
  PATH_TO_REBUTTAL <<- paste0(PATH_OUTPUT, "rebuttal")
  PATH_TO_REBUTTAL_SCENARIO <<- paste0(PATH_TO_REBUTTAL, "/", CURRENT_SCENARIO)
}

addGT <- function() {
  setwd(PATH_TO_GT)

  files_aeresults <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=FALSE)
  
  ############
  
  results <- c()
  for (file in files_aeresults) {
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
    solution <- result[1]
    assessmentCriteria = solution[[1]]
    
    row <- data.frame(matrix(ncol = 0, nrow = 1))
    row["name"] <- result$name
    row["ADAMS"] <- solution$score
    
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
    
    ground_truth <- GT_results[GT_results$name == assessment$name,]$ADAMS
    if(length(ground_truth)==0) { # Filter all rows, where we dont haven an ADAMS GT value 
      next
    }
    
    row["ADAMS"] <- ground_truth
    
    results_df <- rbind(results_df, row)
  }
  return(results_df)
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
  
  i_smaller_GT <- which(round(results_df[score], 3) < round(results_df["ADAMS"], 3))
  i_greater_GT <- which(round(results_df[score], 3) > round(results_df["ADAMS"], 3))
  i_equals_GT <- which(round(results_df[score], 3) == round(results_df["ADAMS"], 3))
  
  Smaller_GT <- results_df[i_smaller_GT, ]
  Greater_GT <- results_df[i_greater_GT, ]
  Equals_GT <- results_df[i_equals_GT, ]
  
  copyOutcomeToFolder(head(Greater_GT), "OE", approach)
  copyOutcomeToFolder(head(Smaller_GT), "UE", approach)
  
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
  
  #hist(results_df$ADAMS)
  return(RESULTS)
}

summarizeResults <- function(results_df, criteriaList) {
  final_results <- data.frame()
  for(criterion in criteriaList) {
    final_results <- rbind(final_results, calculateApproachClassificationModel3Some(results_df, criterion))
  }
  return(final_results)
}

copyOutcomeToFolder <- function (Outcome_df, folder_name, approach){
  PATH <- paste0(PATH_OUTPUT, "rebuttal/", CURRENT_SCENARIO, "/", approach, "/", folder_name)
  dir.create(paste0(PATH_OUTPUT, "rebuttal/", CURRENT_SCENARIO), showWarnings = FALSE)
  dir.create(paste0(PATH_OUTPUT, "rebuttal/", CURRENT_SCENARIO,  "/", approach), showWarnings = FALSE)
  
  wd <- getwd()
  setwd(PATH_TO_AEJOBS)
  for (file_name in Outcome_df$assessment.name) {
    dir.create(paste0(PATH), showWarnings = FALSE)
    file_found <- list.files(PATH_TO_AEJOBS, pattern = file_name)
    file.copy(file_found, PATH)
  }
  setwd(wd)
  write.csv2(Outcome_df, paste0(PATH,"_",approach,"_",CURRENT_SCENARIO,".csv"))
}

#####################  Begin Script

startSingle <- function() {
  criteriaList <- makeCriteriaList()
  
  setwd(PATH_TO_DATASET)
  
  dir.create(PATH_OUTPUT, showWarnings = FALSE)
  dir.create(paste0(PATH_OUTPUT, "rebuttal/"), showWarnings = FALSE)
  unlink(paste0(PATH_TO_REBUTTAL_SCENARIO, "/*"),recursive = T, force = T)
  
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
  write.csv2(final_results, paste0("final_results_",CURRENT_SCENARIO,".csv"))
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


