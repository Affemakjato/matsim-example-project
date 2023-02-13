install.packages("rgeos", "tmap")

library(tidyverse)
library(lubridate)
library(sf)
library(rgeos)
library(dplyr)
library(tmap)
library(ggplot2)

trips_import = read.csv2("..\\Master\\advancedMATsim\\manhattan_trips.csv", header=TRUE, sep = ";")

rand_2000_trips <- trips_import[sample(nrow(trips_import), size=2000), ]
write.csv2(rand_2000_trips, "..\\Master\\advancedMATsim\\manhattan_trips_2000.csv", row.names=FALSE)

brks <- c(0,1,1001,2001,3001,4001,5001)
nyc_shape_manhattan <- st_read("taxi_zones.shp") %>%  filter(borough == "Manhattan")
nyc_shape_manhattan %>% left_join(trips_import, by = c("LocationID" = "PULocationID")) %>% 
  group_by(zone) %>% tally() %>% tm_shape() + tm_polygons(col = "n", 
                                                          title ="n of pick ups", 
                                                          breaks = brks)

nyc_shape_manhattan_with_n_do <- nyc_shape_manhattan %>% 
  left_join(trips_import, by = c("LocationID" = "DOLocationID")) %>% 
  group_by(zone) %>% tally() 
nyc_shape_manhattan_with_n_do <- nyc_shape_manhattan_with_n_do[-67,]

tm_shape(nyc_shape_manhattan_with_n_do) + tm_polygons(col ="n", 
                                                      title = "n of drop offs", 
                                                      breaks = brks)

drt_leg_drt <- read.csv2("..\\Master\\advancedMATsim\\MATSim\\scenarios\\Nyc-drt-scenario\\output\\5000v_100percent_run00\\ITERS\\it.0\\0.drt_legs_drt.csv", header=TRUE, sep = ";")

drt_leg_drt$fromCoord <- st_as_sf(drt_leg_drt, coords=c("fromX", "fromY"))$geometry %>%
  st_set_crs(2263)
drt_leg_drt$toCoord <- st_as_sf(drt_leg_drt, coords=c("toX", "toY"))$geometry %>%
  st_set_crs(2263)

nyc_shape_manhattan_with_n <- nyc_shape_manhattan
nyc_shape_manhattan_with_n$n_from <- lengths(st_intersects(nyc_shape_manhattan_with_n, drt_leg_drt$fromCoord))
nyc_shape_manhattan_with_n$n_to <- lengths(st_intersects(nyc_shape_manhattan_with_n, drt_leg_drt$toCoord))

tm_shape(nyc_shape_manhattan_with_n) + tm_polygons(col = "n_from", 
                                                   title = "n of pick ups", 
                                                   breaks = brks)
tm_shape(nyc_shape_manhattan_with_n) + tm_polygons(col = "n_to", 
                                                   title = "n of drop offs", 
                                                   breaks = brks)

drt_rejections <- read.csv2("..\\Master\\advancedMATsim\\MATSim\\scenarios\\Nyc-drt-scenario\\output\\5000v_100percent_run00\\ITERS\\it.0\\0.drt_rejections_drt.csv", header=TRUE, sep = ";")

drt_trips_hours <- data.frame(Hour = as.integer(as.numeric(drt_leg_drt$departureTime)/3600), 
                              Type = "Accepted")
drt_rejections_hours <- data.frame(Hour = as.integer(as.numeric(drt_rejections$time)/3600), 
                                   Type = "Rejected")
drt_rejections_trips_hours <- add_row(drt_rejections_hours, 
                                      Hour = drt_trips_hours$Hour, 
                                      Type = drt_trips_hours$Type)

trips_import$Hour <- hour(as.POSIXct(trips_import$"tpep_pickup_datetime", 
                                             format = "%Y-%m-%d %H:%M:%S"))

ggplot(trips_import, aes(x=Hour, fill = "red")) + geom_bar()+
  scale_fill_manual(values=c( "#253E6B"))+
  coord_cartesian(ylim=c(0,7000))
ggplot(drt_trips_hours, aes(x=Hour, fill = "red")) + geom_bar( )+
  scale_fill_manual(values=c( "#253E6B"))+
  coord_cartesian(ylim=c(0,7000))
ggplot(drt_rejections_trips_hours, aes(x=Hour, fill = Type)) + 
  geom_bar(position = position_stack(reverse = TRUE)) +
  coord_cartesian(ylim=c(0,7000))+
  scale_fill_manual(values=c( "#253E6B", "#ED7D31"))


  