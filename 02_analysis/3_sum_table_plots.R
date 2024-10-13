library(tidyverse)
library(xtable)
library(rjson)

FILES_PATTERN <<- "*.csv"

DATASET_1 <- "Data_1/"
DATASET_2 <- "Data_2/"
DATASETS <- c(DATASET_1, DATASET_2)

# insert here the project root path. In R it is difficult to obtain the current file location
PATH_TO_ROOT <- "<Path_to_Project_Root>"

APPROACHES_IN_FIGURES <- c("FBS", "PBS_0.8", "PBS_0.5", "SME", "PS_1", "PS_5")

updatePaths <- function() {
  PATH_TO_FOLDER <<- paste0(PATH_TO_ROOT, "/Data/")
  PATH_TO_ANALYSIS <<- paste0(PATH_TO_ROOT, "/02_analysis")
  PATH_TO_DIRECTORY <<- paste0(PATH_TO_FOLDER, "/", CURRENT_DATASET, "5_Final_results")
  setwd(PATH_TO_DIRECTORY)
}

#Obtain ADAMS Results
getResults <- function() {
  files <- list.files(pattern=FILES_PATTERN, full.names=FALSE, recursive=FALSE)
  
  results_df <- NULL
  results_df <- data.frame()

  for (file in files) {
    data <- read_csv2(file)
    data$Scenario <- file
    results_df <- rbind(results_df, data)
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
  results$Approach[results$Approach == 'PhysicalSimulationCriterion1'] <-     'PS$_{(1.0)}$'
  results$Approach[results$Approach == 'PhysicalSimulationCriterion2'] <-     'PS$_{(2.0)}$'
  results$Approach[results$Approach == 'PhysicalSimulationCriterion3'] <-     'PS$_{(3.0)}$'
  results$Approach[results$Approach == 'PhysicalSimulationCriterion4'] <-     'PS$_{(4.0)}$'
  results$Approach[results$Approach == 'PhysicalSimulationCriterion5'] <-     'PS$_{(5.0)}$'
  
  results$Scenario[results$Scenario == 'final_results_Ulds_scenario_1.csv'] <- 'S1'
  results$Scenario[results$Scenario == 'final_results_Ulds_scenario_2a.csv'] <- 'S2a'
  results$Scenario[results$Scenario == 'final_results_Ulds_scenario_2b.csv'] <- 'S2b'

  output <- cbind(results$Scenario)
  output <- cbind(output, results$Approach)
  output <- cbind(output, as.numeric(results$Accuracy))
  output <- cbind(output, as.numeric(results$UE))
  output <- cbind(output, as.numeric(results$OE))
  output <- cbind(output, round(results$Runtime, digits = 3))
  colnames(output) <- c("Scenario", "Approach", "Accuracy", "UE", "OE", "Runtime")
  
  output_df <- data.frame(output)
  
  # make numeric 
  output_df$Accuracy <- as.numeric(output_df$Accuracy)
  output_df$UE <- as.numeric(output_df$UE)
  output_df$OE <- as.numeric(output_df$OE)
  
  
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
makePlots <- function(output) {
  selected_rows <- output[output$Approach %in% APPROACHES_IN_FIGURES,]

  # Plot Accuracy
  plot <- ggplot(data=selected_rows, aes(x=Approach, y=Accuracy, fill=Scenario, width = 0.4)) +
    geom_bar(stat="identity", position=position_dodge()) +
    scale_y_continuous(limits = c(0, 1)) +
    theme(legend.position = c(0.15, 0.87),
          axis.text=element_text(size=14),
          axis.title=element_text(size=14,face="bold"),
          legend.text = element_text(size=14),
          legend.title = element_text(size = 14, face="bold"))
  ggsave("accuracy_comparison.png", 
         plot,
         width = 6,
         height = 6)
  
  # Plot UE
 plot <- ggplot(data=selected_rows, aes(x=Approach, y=UE, fill=Scenario, width = 0.4)) +
    geom_bar(stat="identity", position=position_dodge()) +
    scale_y_continuous(limits = c(0, 1)) +
    labs(y = "Underestimation",)+
    theme(legend.position = c(0.15, 0.87),
          axis.text=element_text(size=14),
          axis.title=element_text(size=14,face="bold"),
          legend.text = element_text(size=14),
          legend.title = element_text(size = 14, face="bold"))

  ggsave("error_comparison_underestimations.png", 
         plot,
         width = 6.0,
         height = 6)
  
  # Plot OE
  plot <- ggplot(data=selected_rows, aes(x=Approach, y=OE, fill=Scenario, width = 0.4)) +
    geom_bar(stat="identity", position=position_dodge()) +
    scale_y_continuous(limits = c(0, 1)) +
    labs(y = "Overestimation",)+
    theme(legend.position = c(0.15, 0.87),
          axis.text=element_text(size=14),
          axis.title=element_text(size=14,face="bold"),
          legend.text = element_text(size=14),
          legend.title = element_text(size = 14, face="bold"))
  
  ggsave("error_comparison_overestimations.png", 
         plot,
         width = 6.0,
         height = 6)
  
  }

### Writing Results
writeResultsTable <- function(output_df) {
  output_df$Accuracy <- output_df$Accuracy * 100
  output_df$UE <- output_df$UE * 100
  output_df$OE <- output_df$OE * 100
  
  print(xtable(output_df, type = "latex",
               caption = paste0("Computational experiments results ", substr(CURRENT_DATASET, 0,4), " ", substr(CURRENT_DATASET, 6,6)),
               label = "table",
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
  writeResultsTable(output)
  output$Approach <- renameApproaches(output$Approach)
  makePlots(output)
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

startAll()