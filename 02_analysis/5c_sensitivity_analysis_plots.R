library(rjson)
library(readr)
library(ggplot2)

FILES_PATTERN <- "*.csv"

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)

DATASET_1 <- "Data_1/"
DATASET_3 <- "Data_3_Ramos/"
DATASETS <- c(DATASET_1, DATASET_3)

DELTA_TRANSLATION_ROTATION_BASELINE <- c(5,10,15)

#####################  Begin functions

updatePaths <- function(sensitivity_level) {
  PATH_TO_FOLDER <<- "/Users/philippmazur/IdeaProjects/criteriaevaluationpipeline/"
  PATH_TO_DATASET <<- paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_CRITERIA <<- paste0(PATH_TO_FOLDER, "AssesmentCriteria_R_output.json")
  PATH_TO_AERESULTS <<- paste0(PATH_TO_DATASET,"3_AeResults/", CURRENT_SCENARIO)
  PATH_TO_AEJOBS <<- paste0(PATH_TO_DATASET,"2_AeJobs/", CURRENT_SCENARIO)
  PATH_SENSITVITY_RESULTS <<- PATH_TO_DATA <- paste0(PATH_TO_DATASET, "5_Final_results/sensitivity_analysis/", sensitivity_level)
  PATH_TO_GT <<- paste0(PATH_TO_DATASET, "4_Ground_truth/", CURRENT_SCENARIO, "/done_sensitivity_analysis/", sensitivity_level)
  PATH_TO_SENSITIVITY <<- paste0(PATH_TO_DATASET, "5_Final_results/sensitivity_analysis/")
  setwd(PATH_TO_SENSITIVITY)
}

renameApproaches <- function(data) {
  data$Approach[data$Approach == 'FullBaseSupportCriterion'] <- 'FBS'
  data$Approach[data$Approach == 'PacquayCriterion'] <- 'Paquay'
  data$Approach[data$Approach == 'PartialBaseSupportCriterion50'] <- 'PBS_0.5'
  data$Approach[data$Approach == 'PartialBaseSupportCriterion60'] <- 'PBS_0.6'
  data$Approach[data$Approach == 'PartialBaseSupportCriterion70'] <- 'PBS_0.7'
  data$Approach[data$Approach == 'PartialBaseSupportCriterion80'] <- 'PBS_0.8'
  data$Approach[data$Approach == 'PartialBaseSupportCriterion90'] <- 'PBS_0.9'
  data$Approach[data$Approach == 'ScienceOfStaticKrebsCriterion'] <- 'SME_K'
  data$Approach[data$Approach == 'ScienceOfStaticRamosCriterion'] <- 'SME'
  data$Approach[data$Approach == 'StaticLoadStabilityCriterionDelta1'] <-     'PS_1'
  data$Approach[data$Approach == 'StaticLoadStabilityCriterionDelta2'] <-     'PS_2'
  data$Approach[data$Approach == 'StaticLoadStabilityCriterionDelta3'] <-     'PS_3'
  data$Approach[data$Approach == 'StaticLoadStabilityCriterionDelta4'] <-     'PS_4'
  data$Approach[data$Approach == 'StaticLoadStabilityCriterionDelta5'] <-     'PS_5'

  return(data$Approach)
}

makePlot <- function(data, dataset, scenario) {
  approaches_selected = c(1, 3, 6, 7, 10, 14)
  
  data <- data[c(approaches_selected, approaches_selected + nrow(data) / 3, approaches_selected + 2 * (nrow(data) / 3)),]
  data$Approach <- renameApproaches(data)
  
  accuracy <- data$Equals_GT_percent
  ue <- data$Smaller_than_GT_percent
  oe <- data$Greater_than_GT_percent
  final_data <- data.frame()
  
  for(i in 1:nrow(data)) {
    row <- data[i,]
    approach_name <- paste0(row$Approach, "_" , row$sensitivity_level)
    row_accuracy <- c(row$Approach, approach_name, row$sensitivity_level, "Accuracy", row$Equals_GT_percent)
    row_ue <- c(row$Approach, approach_name, row$sensitivity_level, "UE", row$Smaller_than_GT_percent)
    row_oe <- c(row$Approach, approach_name, row$sensitivity_level, "OE",  row$Greater_than_GT_percent)
    
    final_data <- rbind(final_data, row_accuracy)
    final_data <- rbind(final_data, row_ue)
    final_data <- rbind(final_data, row_oe)
  }
   
   names(final_data) <- c("Approach_Original", "Approach", "Sensitivity_Level", "Category", "Data")
  
  desired_order_approaches <- c("FBS", "PBS_0.8", "PBS_0.5", "SME", "PS_1", "PS_5")
  custom_colors <- c("Accuracy" = "#2ca02c", "UE" = "#1f77b4", "OE" = "#ff7f0e")
  final_data$Data <- as.numeric(final_data$Data)

  final_data$Category <- factor(final_data$Category, levels = c("Accuracy", "UE", "OE")) # The order is FROM THE TOP!!!
  final_data$Sensitivity_Level <- factor(final_data$Sensitivity_Level, levels = DELTA_TRANSLATION_ROTATION_BASELINE)
  


  final_data$Approach_Original <- factor(final_data$Approach_Original, levels = desired_order_approaches)

  plot <- ggplot(final_data, aes(x=Sensitivity_Level, y=Data, fill = Category), width = 0.3) +
    geom_bar(stat = "identity", position=position_stack(revers=TRUE), alpha = 0.8) + #IMPORTANT: MUST BE TRUE!!
    scale_y_continuous(limits = c(0.0, 100.1), breaks = seq(0, 100, by = 25)) + #100.1 due to an error when set to 100
    coord_flip() +
    scale_fill_manual(values = custom_colors) +
    facet_wrap(~ Approach_Original, scales = "free_y", strip.position = "left", ncol = 1) + # Facet to separate groups
    labs(x = "Approach", y = "Percentage") +
    theme(
      legend.position = "top",
      panel.background = element_rect(fill = "#f2f2f2"), # Hintergrundfarbe des Panels
      panel.grid.major.y = element_blank(), # Entfernen der Hauptgitterlinien
      panel.grid.minor = element_blank(), # Entfernen der Hauptgitterlinien
      axis.line = element_blank(), # Entfernen der Achsenlinien
      axis.text = element_text(size=16, color = "black"), # Achsentextfarbe
      axis.title = element_text(size=16, face="bold", color = "black"), # Achsentitel-Farbe
      strip.background = element_rect(fill = "#e0dede"), # Background color of facet labels
      legend.text = element_text(size=16),
      legend.title = element_text(size = 16, face="bold"),
      strip.text = element_text(size=13)
    )
  

  PLOT <<- plot
  plot_name <- paste0("sensitivity_analysis_",dataset,"_",scenario,".png")
  plot_name <- gsub("/", "", plot_name)
  ggsave(plot_name, 
         plot,
         width = 6.0,
         height = 6)
}

getResults <- function(sensitivity_level, scenario) {
  large_frame <- data.frame()
  path = paste0(PATH_SENSITVITY_RESULTS,"/final_results_",sensitivity_level,"_",scenario,".csv")
  
  data <- read_csv2(path)
  data <- cbind(sensitivity_level, data)
  large_frame <- rbind(large_frame, data)
  
  large_frame <- large_frame[c("sensitivity_level", "Approach", "Smaller_than_GT_percent", "Greater_than_GT_percent", "Equals_GT_percent")]
  return(large_frame)
}

startAll <- function() {
  for (dataset in DATASETS) {
    for (scenario in SCENARIOS) {
      CURRENT_DATASET <<- dataset
      CURRENT_SCENARIO <<- scenario
      
      results_df <- data.frame()
      
      for(sensitivity_level in DELTA_TRANSLATION_ROTATION_BASELINE) {
        updatePaths(sensitivity_level)
        results_df <- rbind(results_df, getResults(sensitivity_level, scenario))
      }
      makePlot(results_df, dataset, scenario)
    }
  }
}

PLOT <- NULL

startAll()

PLOT