library(tidyverse)
library(rjson)
library(zoo)
library(ggplot2)
library(stringr)

FILES_PATTERN <- "*.json"

#Insert here the project root path. In R it is difficult to obtain the current file location
#PATH_TO_ROOT <- "<Path_to_Project_Root>"
PATH_TO_ROOT <- "/Users/philippmazur/IdeaProjects/palletizing_stability_benchmark"

# insert here the job file name you want to demonstrate
JOB_TO_DEMONSTRATE <- "Data/Data_1/4_Benchmark/Ulds_scenario_2b/done_raw/Data_1_Ulds_scenario_2b_LH8048-28NOV15-FRA-LAX_pmc_md11f_md-2.json.json"

PLOTS <- c("max_trans_x", "max_trans_y", "max_trans_z","max_rot_x","max_rot_y", "max_rot_z")

AUC <- function(x, y){
  return(sum(diff(x)*rollmean(y,2)))
}

getIntermediateResults <- function(done_raw_file) {
  max_values <<- data.frame(matrix(ncol = 3, nrow = 0))
  
  json <- list(fromJSON(file = done_raw_file))[[1]]
  large_frame <- data.frame(matrix(ncol = 8, nrow = 0))
  
  for(sequence in json) { #todo: change this parameter for newer versions
    for(item in sequence$itemDeltas) {
      start_Position <- c(item$observations[[1]]$translation$x,
                          item$observations[[1]]$translation$y,
                          item$observations[[1]]$translation$z
      )
      start_Rotation <- c(item$observations[[1]]$rotation$x,
                          item$observations[[1]]$rotation$y,
                          item$observations[[1]]$rotation$z
      )
      
      diffs <- data.frame()
      
      for(observation in item$observations) {
        abs_translation_x <- abs(start_Position[1] - observation$translation$x)
        abs_translation_y <- abs(start_Position[2] - observation$translation$y)
        abs_translation_z <- abs(start_Position[3] - observation$translation$z)
        
        abs_rotation_x <- abs(observation$rotation$x)
        abs_rotation_y <- abs(observation$rotation$y)
        abs_rotation_z <- abs(observation$rotation$z)
        
        diffs <- rbind(diffs, c(observation$time, abs_translation_x, abs_translation_y, abs_translation_z,
                                abs_rotation_x, abs_rotation_y,abs_rotation_z) )
      }
      
      
      colnames(diffs) <- c("time", "diff_translation_x","diff_translation_y", "diff_translation_z",
                           "diff_rotation_x","diff_rotation_y","diff_rotation_z")
    
      max_values <<- rbind(max_values, c(sequence$sequence, item$itemLabel, max(diffs$diff_translation_x),
                                         max(diffs$diff_translation_y), max(diffs$diff_translation_z),
                                         max(diffs$diff_rotation_x),max(diffs$diff_rotation_y),
                                         max(diffs$diff_rotation_z)))
    }
  }
  return(max_values)
}


euclidean <- function(a, b, c) sqrt(sum((a^2 + b^2 + c^2)))

shortendLegendTitles <- function(max_values) {
  #insert here the item label string that needs to be removed for the legend, e.g.:
  #Class_3_Instance_15_ULD_0_
  max_values$Item <- str_replace(max_values$Item, "000_", "")
  return(max_values)
}

make_plots <- function(max_values, single_plot) {
  plot <<- ggplot(data=max_values, aes(x=as.numeric(sequence), as.numeric(.data[[single_plot]]), group=Item)) +
    geom_line(aes(color=Item)) +
    labs(y = single_plot, x="Sequence") +
    theme(text = element_text(size = 32),
          legend.position = "right",
          legend.text = element_text(size = 26),  # Reduce legend text size
          legend.title = element_text(size = 30),
          legend.key.height = unit(1.2, "cm"),  # Specifically increase height for vertical space
          
    ) +
    guides(color = guide_legend(ncol = 2)) +  # Set the legend to two columns
    ylim(0, 50)
  plot
  
  ggsave(paste0("demonstration_", single_plot, ".png"), plot, width = 10, height = 6)
}
startIntermediate <- function() {
  PATH_TO_FOLDER <<- paste0(PATH_TO_ROOT)
  setwd(PATH_TO_FOLDER)
  max_values <- getIntermediateResults(JOB_TO_DEMONSTRATE)
  
  colnames(max_values) <- c("sequence", "Item","max_trans_x", "max_trans_y",
                            "max_trans_z","max_rot_x","max_rot_y", "max_rot_z")
  
  max_values <- shortendLegendTitles(max_values)
  
  for (single_plot in PLOTS) {
    make_plots(max_values, single_plot)
  }
}


startIntermediate()