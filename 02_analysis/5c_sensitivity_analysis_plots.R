library(rjson)
library(readr)
library(ggplot2)

FILES_PATTERN <- "*.csv"

SCENARIO_1 <- "Ulds_scenario_1"
SCENARIO_2a <- "Ulds_scenario_2a"
SCENARIO_2b <- "Ulds_scenario_2b"
#SCENARIOS <- c(SCENARIO_1, SCENARIO_2a, SCENARIO_2b)
SCENARIOS <- c(SCENARIO_2b)


DATASET_1 <- "Data_1/"
DATASET_2 <- "Data_2/"
DATASETS <- c(DATASET_1)

DELTA_TRANSLATION_ROTATION_BASELINE <- c(5, 10, 15)

APPROACHES_IN_FIGURES <- c("FBS", "PBS_0.8", "PBS_0.5", "SME", "PS_1", "PS_5")

#Insert here the project root path. In R it is difficult to obtain the current file location
#PATH_TO_ROOT <- "<Path_to_Project_Root>"
PATH_TO_ROOT <- "/Users/philippmazur/IdeaProjects/palletizing_stability_benchmark"


#####################  Begin functions

updatePaths <- function(sensitivity_level) {
  PATH_TO_FOLDER <<- paste0(PATH_TO_ROOT, "/Data/")
  PATH_TO_DATASET <<- paste0(PATH_TO_FOLDER, CURRENT_DATASET)
  PATH_TO_CRITERIA <<- paste0(PATH_TO_FOLDER, "AssesmentCriteria_R_output.json")
  
  PATH_TO_AERESULTS <<- paste0(PATH_TO_DATASET,"3_AeResults/", CURRENT_SCENARIO)
  PATH_TO_AEJOBS <<- paste0(PATH_TO_DATASET,"2_AeJobs/", CURRENT_SCENARIO)
  PATH_SENSITVITY_RESULTS <<- PATH_TO_DATA <- paste0(PATH_TO_DATASET, "5_Final_results/sensitivity_analysis/", sensitivity_level)
  
  PATH_TO_BENCHMARK <<- paste0(PATH_TO_DATASET, "4_Benchmark/", CURRENT_SCENARIO, "/done_sensitivity_analysis/", sensitivity_level)
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
  data$Approach[data$Approach == 'PhysicalSimulationCriterion1'] <-     'PS_1'
  data$Approach[data$Approach == 'PhysicalSimulationCriterion2'] <-     'PS_2'
  data$Approach[data$Approach == 'PhysicalSimulationCriterion3'] <-     'PS_3'
  data$Approach[data$Approach == 'PhysicalSimulationCriterion4'] <-     'PS_4'
  data$Approach[data$Approach == 'PhysicalSimulationCriterion5'] <-     'PS_5'

  return(data$Approach)
}

makePlot <- function(output, dataset, scenario) {
  output <- output[output$Approach %in% APPROACHES_IN_FIGURES,]
  
  #data <- data[c(approaches_selected, approaches_selected + nrow(data) / 3, approaches_selected + 2 * (nrow(data) / 3)),]
  #data$Approach <- renameApproaches(data)
  
  accuracy <- output$Accuracy
  ue <- output$UE
  oe <- output$OE
  final_data <- data.frame()
  
  for(i in 1:nrow(output)) {
    row <- output[i,]
    approach_name <- paste0(row$Approach, "_" , row$sensitivity_level)
    row_accuracy <- c(row$Approach, approach_name, row$sensitivity_level, "Accuracy", row$Accuracy)
    row_ue <- c(row$Approach, approach_name, row$sensitivity_level, "UE", row$UE)
    row_oe <- c(row$Approach, approach_name, row$sensitivity_level, "OE",  row$OE)
    
    final_data <- rbind(final_data, row_accuracy)
    final_data <- rbind(final_data, row_ue)
    final_data <- rbind(final_data, row_oe)
  }
   
  names(final_data) <- c("Approach_Original", "Approach", "Sensitivity_Level", "Category", "Data")
  custom_colors <- c("Accuracy" = "#2ca02c", "UE" = "#1f77b4", "OE" = "#ff7f0e")
  final_data$Data <- as.numeric(final_data$Data) * 100
  final_data$Category <- factor(final_data$Category, levels = c("Accuracy", "UE", "OE")) # The order is FROM THE TOP!!!
  final_data$Sensitivity_Level <- factor(final_data$Sensitivity_Level, levels = DELTA_TRANSLATION_ROTATION_BASELINE)
  final_data$Approach_Original <- factor(final_data$Approach_Original, levels = APPROACHES_IN_FIGURES)

  plot <- ggplot(final_data, aes(x=Sensitivity_Level, y=Data, fill = Category), width = 0.3) +
    geom_bar(stat = "identity", position=position_stack(revers=TRUE), alpha = 0.8) + #IMPORTANT: MUST BE TRUE!!
    scale_y_continuous(limits = c(0.0, 100.1), breaks = seq(0, 100, by = 25)) + #100.1 due to an error when set to 100
    coord_flip() +
    scale_fill_manual(values = custom_colors) +
    facet_wrap(~ Approach_Original, scales = "free_y", strip.position = "left", ncol = 1) + # Facet to separate groups
    labs(x = "Approach", y = "Percentage") +
    theme(
      legend.position = "top",
      panel.background = element_rect(fill = "#f2f2f2"), # background color of panel
      panel.grid.major.y = element_blank(), # remove main grid lines
      panel.grid.minor = element_blank(), # remove minor grid line
      axis.line = element_blank(), # remove axis lines
      axis.text = element_text(size=16, color = "black"), # axis color
      axis.title = element_text(size=16, face="bold", color = "black"), # axis color
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
  
  large_frame <- large_frame[c("sensitivity_level", "Approach", "UE", "OE", "Accuracy")]
  return(large_frame)
}

startAll <- function() {
  for (dataset in DATASETS) {
    for (scenario in SCENARIOS) {
      CURRENT_DATASET <<- dataset
      CURRENT_SCENARIO <<- scenario
      
      output <- data.frame()
      
      for(sensitivity_level in DELTA_TRANSLATION_ROTATION_BASELINE) {
        updatePaths(sensitivity_level)
        output <- rbind(output, getResults(sensitivity_level, scenario))
      }
      output$Approach <- renameApproaches(output)
      makePlot(output, dataset, scenario)
    }
  }
}

PLOT <- NULL

startAll()

PLOT