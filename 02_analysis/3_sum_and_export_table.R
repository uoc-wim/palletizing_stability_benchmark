library(tidyverse)
library(xtable)
library(rjson)


DATASET_1 <- "Data_1/"
DATASET_3 <- "Data_3_Ramos/"

DATASETS <- c(DATASET_1, DATASET_3)

updatePaths <- function() {
  PATH_TO_FOLDER <<- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/"
  PATH_TO_DIRECTORY <<- paste0("/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/", CURRENT_DATASET, "5_Final_results")
  PATH_TO_CRITERIA <<- paste0(PATH_TO_FOLDER, "AssesmentCriteria_Output.json")
  FILES_PATTERN <<- "*.csv"
  setwd(PATH_TO_DIRECTORY)
}


makeCriteriaList <- function() {
  criteriaList <- cbind()
  criteria <- data <- list(fromJSON(file = PATH_TO_CRITERIA))[[1]]$assessmentCriteria
  for(criterion in criteria) {
    criteriaList <- rbind(criteriaList, criterion$criterionType)
  }
  return(criteriaList)
}


#Obtain ADAMS Results
getResults <- function() {
  ############
  files <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=FALSE)
  
  results_df <- NULL
  results_df <- data.frame()
  
  criteria <- makeCriteriaList()
  
  for (file in files) {
    data <- read_csv2(file)
    data_filtered <- filter(data, Approach %in% criteria)
    
    data_filtered$Scenario <- file
   
    results_df <- rbind(results_df, data_filtered)
  }
  return(results_df)
}


# Distil results
makeOutput <- function(results) {
  output_df <- NULL
  output_df <- data.frame()
  
  results$Approach[results$Approach == 'FullBaseSupportCriterion'] <- 'FBS'
  
  results$Approach[results$Approach == 'PacquayCriterion'] <- 'Paquay'
  
  results$Approach[results$Approach == 'PartialBaseSupportCriterion50'] <- 'PBS$_{(0.50)}$'
  results$Approach[results$Approach == 'PartialBaseSupportCriterion60'] <- 'PBS$_{(0.60)}$'
  results$Approach[results$Approach == 'PartialBaseSupportCriterion70'] <- 'PBS$_{(0.70)}$'
  results$Approach[results$Approach == 'PartialBaseSupportCriterion80'] <- 'PBS$_{(0.80)}$'
  results$Approach[results$Approach == 'PartialBaseSupportCriterion90'] <- 'PBS$_{(0.90)}$'

  results$Approach[results$Approach == 'ScienceOfStaticKrebsCriterion'] <- 'SME$_K$'
  results$Approach[results$Approach == 'ScienceOfStaticRamosCriterion'] <- 'SME'

  
  results$Approach[results$Approach == 'StaticLoadStabilityCriterionDelta1'] <-     'PS$_{(1.0)}$'
  results$Approach[results$Approach == 'StaticLoadStabilityCriterionDelta2'] <-     'PS$_{(2.0)}$'
  results$Approach[results$Approach == 'StaticLoadStabilityCriterionDelta3'] <-     'PS$_{(3.0)}$'
  results$Approach[results$Approach == 'StaticLoadStabilityCriterionDelta4'] <-     'PS$_{(4.0)}$'
  results$Approach[results$Approach == 'StaticLoadStabilityCriterionDelta5'] <-     'PS$_{(5.0)}$'
  
  #Evtl. S_1 instead of S1
  results$Scenario[results$Scenario == 'final_results_Ulds_scenario_1.csv'] <- 'S1'
  results$Scenario[results$Scenario == 'final_results_Ulds_scenario_2a.csv'] <- 'S2a'
  results$Scenario[results$Scenario == 'final_results_Ulds_scenario_2b.csv'] <- 'S2b'
  
  results <- rename(results, SS_S = Smaller_than_GT,
         SS_G = Greater_than_GT,
         SS_E = Equals_GT,
  )

  output_df <- cbind(results[,12]) # Scenario
  output_df <- cbind(output_df, results[,2]) # Approach
  output_df <- cbind(output_df, results[,5]) # Accuracy
  output_df <- cbind(output_df, results[,3]) # UE
  output_df <- cbind(output_df, results[,4]) # OE
  output_df <- cbind(output_df, round(results[,11], digits = 3)) # Runtime
  
  colnames(output_df) <- c("Scenario", "Approach", "Accuracy", "UE", "OE", "Runtime")
  
  output_df$Approach <- factor(output_df$Approach, levels = c("FBS", 
                                                              
                                                              "Paquay",
                                                              
                                                              "PBS$_{(0.50)}$",
                                                              "PBS$_{(0.60)}$",
                                                              "PBS$_{(0.70)}$",
                                                              "PBS$_{(0.80)}$",
                                                              "PBS$_{(0.90)}$",
                                                              
                                                              "SME$_K$",
                                                              "SME",

                                                              
                                                              "PS$_{(1.0)}$",
                                                              "PS$_{(2.0)}$", 
                                                              "PS$_{(3.0)}$", 
                                                              "PS$_{(4.0)}$", 
                                                              "PS$_{(5.0)}$"))
  
  return(output_df)
  
}

makeOutputError <- function(output) {
  error_df <- NULL
  error_df <- output[, c(2, 3, 5, 6)]

  colnames(error_df) <- c("Scenario", "Approach", "Smaller", "Greater")
  error_df$Smaller = error_df$Smaller
  error_df$Greater = error_df$Greater

  return(error_df)
}

renameApproaches <- function(approach) {
  levels(approach)[levels(approach)=="Paquay"] <- "PBS_3"
  levels(approach)[levels(approach)=="SME$_K$"] <- "SME_K"
  levels(approach)[levels(approach)=="PBS$_{(0.80)}$"] <- "PBS_0.8"
  
  levels(approach)[levels(approach)=="PBS$_{(0.50)}$"] <- "PBS_0.5"
  levels(approach)[levels(approach)=="PBS$_{(0.70)}$"] <- "PBS_0.7"
  levels(approach)[levels(approach)=="PBS$_{(0.90)}$"] <- "PBS_0.9"
  levels(approach)[levels(approach)=="PS$_{(1.0)}$"] <- "PS_1"
  levels(approach)[levels(approach)=="PS$_{(5.0)}$"] <- "PS_5"
  
  
  # Set the desired order of the approaches
  desired_order <- c("FBS", "PBS_0.8", "PBS_0.5", "SME", "PS_1", "PS_5")
  
  # Convert the Approach column to a factor with the specified order
  result <- factor(approach, levels = desired_order)
  
  return(result)
}

#### Plotting
makePlotAccuracy2 <- function(output, num_approaches) {
  approaches_selected = c(1, 3, 6, 7, 10, 14)
  
  # refers to dataset, approach, accuracy
  columns_selected <- c(2, 3, 4)

  
  data <- output[c(approaches_selected, approaches_selected + num_approaches, approaches_selected+2*num_approaches), columns_selected]

  data$Approach <- renameApproaches(data$Approach)
  colnames(data) <- c("Scenario", "Approach", "Accuracy")
  
  
  plot <- ggplot(data=data, aes(x=Approach, y=Accuracy, fill=Scenario, width = 0.4)) +
    geom_bar(stat="identity", position=position_dodge()) +
    scale_y_continuous(limits = c(0, 1)) +
    theme(legend.position = c(0.15, 0.87),
          axis.text=element_text(size=14),
          axis.title=element_text(size=14,face="bold"),
          legend.text = element_text(size=14),
          legend.title = element_text(size = 14, face="bold"))
  PLOT <<- plot
  
  ggsave("accuracy_comparison.png", 
         plot,
         width = 6,
         height = 6)
  }

makePlotErrorsUnderestimations <- function(errors, num_approaches) {
  approaches_selected = c(1, 6, 3, 7, 10, 14)
  data <- errors[c(approaches_selected, approaches_selected + num_approaches, approaches_selected + 2 * num_approaches),]
  row.names(data) <- NULL
  data$Approach <- renameApproaches(data$Approach)

  plot <- ggplot(data=data, aes(x=Approach, y=Smaller, fill=Scenario, width = 0.4)) +
    geom_bar(stat="identity", position=position_dodge()) +
    scale_y_continuous(limits = c(0, 1)) +
    labs(y = "Underestimation",)+
    theme(legend.position = c(0.15, 0.87),
          axis.text=element_text(size=14),
          axis.title=element_text(size=14,face="bold"),
          legend.text = element_text(size=14),
          legend.title = element_text(size = 14, face="bold"))
  PLOT <<- plot
  
  ggsave("error_comparison_underestimations.png", 
         plot,
         width = 6.0,
         height = 6)
  
}

makePlotErrorsOverestimations <- function(errors, num_approaches) {
  approaches_selected = c(1, 6, 3, 7, 10, 14)
  data <- errors[c(approaches_selected, approaches_selected + num_approaches, approaches_selected + 2*num_approaches),]
  row.names(data) <- NULL
  
  data$Approach <- renameApproaches(data$Approach)
  
  plot <- ggplot(data=data, aes(x=Approach, y=Greater, fill=Scenario, width = 0.4)) +
    geom_bar(stat="identity", position=position_dodge()) +
    scale_y_continuous(limits = c(0, 1)) +
    labs(y = "Overestimation",)+
    theme(legend.position = c(0.15, 0.87),
          axis.text=element_text(size=14),
          axis.title=element_text(size=14,face="bold"),
          legend.text = element_text(size=14),
          legend.title = element_text(size = 14, face="bold"))
  PLOT <<- plot
  
  ggsave("error_comparison_overestimations.png", 
         plot,
         width = 6.0,
         height = 6)
}


### Writing Results
writeResults <- function(table) {
  table_copy <- table
  
  table_copy[4] <- table_copy[4]* 100
  table_copy[5] <- table_copy[5]* 100
  table_copy[6] <- table_copy[6]* 100
  
  print(xtable(table_copy, type = "latex",
               caption = paste0("Computational experiments results ", substr(CURRENT_DATASET, 0,4), " ", substr(CURRENT_DATASET, 6,6)),
               # align = "p{1.5cm}p{1.5cm}p{1cm}p{1cm}p{1cm}p{2cm}p{2cm}",
               label = "testTable",
               digits=c(0,0,0,0,2,2,2,3)
        ),
  file = "final_results_summarized.tex", 
  sanitize.text.function = function(x) {x}, 
  include.rownames = FALSE, #Don't print rownames
  caption.placement = "top", #"top", NULL,
  size="\\fontsize{9pt}{10pt}\\tiny",
  )
  
}

startSingle <- function(output) {
  writeResults(output)
  makePlotAccuracy2(output, 14)
  errors <- makeOutputError(output)
  makePlotErrorsUnderestimations(errors, 14)
  makePlotErrorsOverestimations(errors, 14)
}



startAll <- function() {
  for (dataset in DATASETS) {
    CURRENT_DATASET <<- dataset
    print(paste0("Evaluating ", CURRENT_DATASET))
    updatePaths()
    
    results_df <- getResults()
    output <- makeOutput(results = results_df)
    
    Dataset = c(substr(CURRENT_DATASET, 6, 6))
    output <- cbind(Dataset, output)
    startSingle(output)
  }
  
}

PLOT <- NULL

startAll()

PLOT
