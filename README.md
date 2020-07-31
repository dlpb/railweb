# Railweb

Railweb is a monolithic UI and Application for 
- Recording Station Visits
- Recording Track Visits
- Showing a map of visits
- Showing a reasonably detailed, but arbitrarily so, route map of the UK
- Showing timetables

New features may include
- Highlighting locations to visit based on some input
- Highlighting where a train will call / go
- Planning a route by stations you want to visit

## Why a monolith?
This application runs in Heroku, and in the free tier, there is a 30s warmup period and limited credits. A monolith reduces the amount of time and credits used to run the application

## Data Sources
- [Railweb Timetable Server](https://github.com/dlpb/railweb-timetable-server-java) for timetable data
- [Railweb admin](https://github.com/dlpb/railwebadmin) for generating static data files that are consumed by the application.
  - [locations.json](conf/data/static/locations.json), a list of all locations, TIPLOCs, and metadata
```json
  {
   "id" : "LIVST",
   "name" : "London Liverpool Street",
   "operator" : "RT",
   "type" : "Station",
   "location" : {
     "lat" : 51.518944,
     "lon" : -0.080939088,
     "county" : "London",
     "district" : "Greater London",
     "postcode" : "\"EC2M 7PY"
   },
   "nrInfo" : {
     "crp" : "",
     "route" : "Anglia",
     "srs" : "D.10",
     "changeTime" : "Set(None)",
     "interchangeType" : "3 - Large interchange point"
   },
   "orrStation" : true,
   "crs" : [ "LST" ],
   "tiploc" : [ "LIVST" ],
   "orrId" : "LST"
 }
```
  - [routes.json](conf/data/static/routes.json), a list of all routes, and metadata
  
```json
{
  "from" : {
    "lat" : 51.667074,
    "lon" : 0.38443642,
    "id" : "INGTSTN",
    "name" : "Ingatestone",
    "type" : "Station"
  },
  "to" : {
    "lat" : 51.630669,
    "lon" : 0.32985507,
    "id" : "SHENFLD",
    "name" : "Shenfield",
    "type" : "Station"
  },
  "toc" : "LE",
  "singleTrack" : "2",
  "electrification" : "OHLE",
  "speed" : "80-105",
  "srsCode" : "D.11",
  "type" : "NR",
  "distance" : 5528
}
```

This data was originally generated by [Railweb visits processing](https://github.com/dlpb/railvisits-processing), based on raw data from [Railweb Raw Data](https://github.com/dlpb/railvisits-raw-data).
This has been processed so much it is now managed by RailWeb admin.

It is possible to reproduce roughly by running the railweb visits processing from the raw data, in combination with the national rail raw data sources using [Railweb Network Rail Route Data Processing](https://github.com/dlpb/railweb-network-rail-route-data-processing)