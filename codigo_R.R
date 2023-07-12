rm(list=ls())
options(repos = list(CRAN="http://cran.rstudio.com/"))
my_packages <- c("openxlsx", "readr", "xlsx", "jsonlite", "dplyr")                                        # Specify your packages
not_installed <- my_packages[!(my_packages %in% installed.packages()[ , "Package"])]    # Extract not installed packages
if(length(not_installed)) install.packages(not_installed) 
library(openxlsx)
library(readr)
library(xlsx)
library(jsonlite)
library(dplyr)



#1. TABLA CHECKS----
setwd("C:/WS-TFM/fair_ontologies/salida")
num_files_datos <- length(list.files( pattern = "O_datos"))
num_files_datos
i <- 1:num_files_datos

#CAMBIAR EL RANGO DE I
for(valor in i){
  
  if(valor==1){  #PRIMER VALOR DEL RANGO
    nombre_ficheros <- paste0("O_datos",valor)
    
    Oi  <- jsonlite::fromJSON(txt=paste0(nombre_ficheros,".json"))
    Oi_df_aux <- as.data.frame(Oi)
    Oi_df_total <- data.frame(ontologia=nombre_ficheros[1],
                              var_union = paste0("O",valor)) %>% 
      cbind(Oi_df_aux)
    if(length(colnames(Oi_df_total)) == 12){
      Oi_df_total <- Oi_df_total %>% select(- reference_resources)
    }
    
  }else{
    nombre_ficheros <- paste0("O_datos",valor)
    
    Oi <-  jsonlite::fromJSON(txt=paste0(nombre_ficheros,".json"))
    Oi_df_aux <- as.data.frame(Oi)
    Oi_df <- data.frame(ontologia=nombre_ficheros[1],
                        var_union = paste0("O",valor)) %>% 
      cbind(Oi_df_aux)
    
    Oi_df_total <- Oi_df_total %>% rbind(Oi_df)
    
  }
}

#2. TABLA INICIAL ----

num_files_cabecera <- length(list.files( pattern = "O_cabecera"))
i <- 1:num_files_cabecera

for(valor in i){
  
  if(valor==1){  #PRIMER VALOR DEL RANGO
    nombre_ficheros <- paste0("O_cabecera",valor)
    
    Oi <- jsonlite::fromJSON(txt=paste0(nombre_ficheros,".json"))
    Oi_df_aux <- as.data.frame(Oi)
    Oi_df_inicial <- data.frame(ontologia=nombre_ficheros[1],
                                var_union = paste0("O",valor)) %>% 
      cbind(Oi_df_aux)
    
    
  }else{
    nombre_ficheros <- paste0("O_cabecera",valor)
    
    Oi <- jsonlite::fromJSON(txt=paste0(nombre_ficheros,".json"))
    Oi_df_aux <- as.data.frame(Oi)
    Oi_df <- data.frame(ontologia=nombre_ficheros[1],
                        var_union = paste0("O",valor))%>% 
      cbind(Oi_df_aux)
    
    Oi_df_inicial <- Oi_df_inicial %>% rbind(Oi_df)
    
  }
}


#3. UNIÓN TABLAS ----

tabla_union <- Oi_df_inicial %>% 
  select(-ontologia) %>% 
  left_join(Oi_df_total %>% select(-ontologia), by="var_union") %>% 
  rename(ontologia = var_union)

# 4. CONTEOS VARIABLES ----

#_4.1. Status ----

i <- 1:num_files_datos

for(valor in i){
  
  nombre_ficheros <- paste0("O",valor)
  
  if(valor == 1){
    
    tabla_status <- tabla_union %>% 
      filter(ontologia == nombre_ficheros) %>% 
      count(status) %>% 
      as.data.frame() %>% 
      cbind(ontologia = nombre_ficheros)
    
  }else{
    
    tabla_aux <- tabla_union %>% 
      filter(ontologia == nombre_ficheros) %>% 
      count(status) %>% 
      as.data.frame() %>% 
      cbind(ontologia = nombre_ficheros)
    
    tabla_status <- tabla_status %>% rbind(tabla_aux)
    
  }
  
}

tabla_status <- tabla_status[, c(3,1,2)] %>% 
  rename(conteo=n)

#_4.2. Category id ----

i <- 1:num_files_datos

for(valor in i){
  
  nombre_ficheros <- paste0("O",valor)
  
  if(valor == 1){
    
    tabla_categoria <- tabla_union %>% 
      filter(ontologia == nombre_ficheros) %>% 
      count(category_id) %>% 
      as.data.frame() %>% 
      cbind(ontologia = nombre_ficheros)
    
  }else{
    
    tabla_aux <- tabla_union %>% 
      filter(ontologia == nombre_ficheros) %>% 
      count(category_id) %>% 
      as.data.frame() %>% 
      cbind(ontologia = nombre_ficheros)
    
    tabla_categoria <- tabla_categoria %>% rbind(tabla_aux)
    
  }
  
  
}

tabla_categoria <- tabla_categoria[, c(3,1,2)] %>% 
  rename(conteo = n)

#_4.3. Principio ----

i <- 1:num_files_datos

for(valor in i){
  
  nombre_ficheros <- paste0("O",valor)
  
  if(valor == 1){
    
    tabla_principio <- tabla_union %>% 
      filter(ontologia == nombre_ficheros) %>% 
      count(principle_id) %>% 
      as.data.frame() %>% 
      cbind(ontologia = nombre_ficheros)
    
  }else{
    
    tabla_aux <- tabla_union %>% 
      filter(ontologia == nombre_ficheros) %>% 
      count(principle_id) %>% 
      as.data.frame() %>% 
      cbind(ontologia = nombre_ficheros)
    
    tabla_principio <- tabla_principio %>% rbind(tabla_aux)
    
  }
  
  
}

tabla_principio <- tabla_principio[, c(3,1,2)] %>% 
  rename(conteo = n)

#_ 4.4. Licencia ----

tabla_licencia <- Oi_df_inicial %>% 
  select(ontologia, ontology_license)

#_ 4.5. Test ----

tabla_test <- tabla_union %>% 
  select(ontologia, id, total_passed_tests) %>% 
  mutate(pasa_test = ifelse(total_passed_tests >=1, 1, 0))

tabla_pasa_test <- tabla_union %>% 
  select(ontologia, id, total_passed_tests) %>% 
  mutate(pasa_test = ifelse(total_passed_tests >=1, 1, 0)) %>% 
  filter(pasa_test == 1) %>% 
  count(id) %>% 
  rename(n_ontologias_passed_test = n)



#5.GRAFICOS----

# _5.1. Categoria----

categoria <- table(Oi_df_total$category_id)
jpeg(file="plot_categoria.jpeg")
barplot <- barplot(categoria,
                   main = "Gráfico de categorias"
                   ,ylim = c(0, num_files_datos*100), border = "white",col = c("#CAFF70", "#97FFFF", "#C1FFC1", "#7FFFD4"))
text(barplot, categoria + 200, labels = categoria)
border = "white"
col = c("#CAFF70", "#97FFFF", "#C1FFC1", "#7FFFD4")
dev.off()

#_5.2. ID Principal----

principal_id <- table(Oi_df_total$principle_id)
jpeg(file="plot_id_principal.jpeg")
barplot_p <- barplot(principal_id,
                     main = "Gráfico principle id"
                     ,ylim = c(0, num_files_datos*100), border = "white",col = c("#CAFF70", "#97FFFF", "#C1FFC1", "#7FFFD4"
                                                                  ,"#90EE90","#00FA9A","#48D1CC","#66CDAA"
                                                                  ,"#ADFF2F","#FFF68F","#FFFF00"))
text(barplot_p, principal_id + 200, labels = principal_id)
dev.off()

#_5.3. STATUS----

estado <- table(Oi_df_total$status)
jpeg(file="plot_estado.jpeg")
barplot_st <- barplot(estado,
                      main = "Gráfico Estados"
                      ,ylim = c(0, num_files_datos*100), border = "white",col = c("#CAFF70", "#97FFFF"))
text(barplot_st, estado + 300 , labels = estado)
dev.off()

#_5.3. Test----

test <- table(tabla_union %>% 
  select(ontologia, id, total_passed_tests) %>% 
  mutate(pasa_test = ifelse(total_passed_tests >=1, 1, 0)) %>% 
  filter(pasa_test == 1) %>% 
  select(id))

jpeg(file="plot_test.jpeg")
barplot_st <- barplot(test, las=2, cex.names=.5,
                      main = "Gráfico Test"
                      ,ylim = c(0, 15), border = "white",col = c("#CAFF70", "#97FFFF"))


text(barplot_st, test + 1, labels = test)
dev.off()


# 6. EXPORTAR FICHEROS ----

#Guardamos los archivos en la carpeta que queremos convertir a zip
write.csv(tabla_union, "ontologias.csv")
write.csv(tabla_test, "C:/WS-TFM/fair_ontologies/pruebas_test/test.csv")
write.csv(tabla_pasa_test, "C:/WS-TFM/fair_ontologies/pruebas_test/n_ontologias_test.csv")

#Creamos el zip
zip_files <- list.files( pattern = ".csv$|.jpeg$")
zip::zip(zipfile = "zipOntologias.zip", files = zip_files)

