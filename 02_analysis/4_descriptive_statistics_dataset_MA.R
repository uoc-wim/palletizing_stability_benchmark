library(rjson)

#PATH_TO_DIRECTORY <- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/Dataset_Masterarbeit/AeJobs"
PATH_TO_DIRECTORY <- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/Data_1/2_AeJobs/Ulds_scenario_1"

FILES_PATTERN <- "*.json"

setwd(PATH_TO_DIRECTORY)

#Obtain ADAMS Results
getResults <- function() {
  results_df <- NULL
  results_df <- data.frame()
  
  results <- c()

  ############
  files <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=TRUE)

  for (file in files) {
    #read json
    data <- list(fromJSON(file = file))
    
    #assign names according to filename
    data[1][[1]]$name <- file
    
    #append
    results <- append(results, data)
  }

  
  
  results_df <- NULL
  results_df <- data.frame()
  
  for(result in results) {
    placedItems <- length(result[["ulds"]][[1]][["placedItems"]])
    
    row <- data.frame(matrix(ncol = 2, nrow = 1))
    row["name"] <- result$name
    row["placedItems"] <- placedItems
    
    results_df <- rbind(results_df, row)
  }
  return(results_df)
}

test = getResults()

hist(test$placedItems)
mean(test$placedItems)
sd(test$placedItems)