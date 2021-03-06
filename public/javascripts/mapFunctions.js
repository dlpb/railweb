/**
* Map and Layers put in global scope to improve performance
*
* defining layers at a more local method scope slows down performance of the page dramatically.
*/

let map;
let stationsLayer;
let metroLayer;
let locationsLayer;

function createMap() {
    stationsLayer = makeLayer(true);
    metroLayer = makeLayer(true)
    locationsLayer = makeLayer(true);

    map = new ol.Map({
        target: 'map',
        layers: [
            new ol.layer.Tile({
                source: new ol.source.OSM()
            }),
            locationsLayer,
            metroLayer,
            stationsLayer
        ],
        view: new ol.View({
            center: ol.proj.fromLonLat([-0.1279688, 51.5077286]),
            zoom: 5
        }),
        controls: ol.control.defaults().extend([
            new ol.control.FullScreen()
        ]),
    });
}

/**
* Add all points to the map
*/
function populatePoints(map, token) {
    jQuery.when(
        jQuery.ajax({
          url: "/api/location/map",
          headers: { "Authorization": "Bearer " + token }
        }),
        jQuery.ajax({
        url: "/api/visit/locations",
        headers: { "Authorization": "Bearer " + token }
        })
    )
    .done(function(locations, visits){
        locations[0].forEach(function(loc){
            loc.visited = findLocationVisit(loc, visits[0]);
            addLocation(loc);
        });
    });
    function findLocationVisit(loc, visits){
        return visits.includes(loc.id);
    }
}


 function addLocation(location){
    let lat = location.location.lat;
    let lon = location.location.lon;

    var locationPointFeature = new ol.Feature({
        geometry: new ol.geom.Point(ol.proj.fromLonLat([lon, lat])),
        name: location.name + ' (' + location.id + ')',
        type: "Location",
        operator: location.operator,
        id: location.id,
        visited: location.visited
    });
    locationPointFeature.setId(location.id);

    let icon = location.visited ?
        '/assets/images/dot-visited.png':
        '/assets/images/dot-not-visited.png'
    locationPointFeature.setStyle(new ol.style.Style({
        image: new ol.style.Icon(/** @type {olx.style.IconOptions} */({
            color: findTocData(location.operator).colour,
            crossOrigin: 'anonymous',
            src: icon
        })),
    }));
    if(location.orrStation){
        stationsLayer.getSource().addFeature(locationPointFeature);
    }
    else if(
        location.type === "Light Rail"
        || location.type === "Underground"
        || location.type === "Metro"
        || location.type === "Tram"){
            metroLayer.getSource().addFeature(locationPointFeature);
    }
    else {
        locationsLayer.getSource().addFeature(locationPointFeature);
    }
}
/**
* Add all connections to the map
*/
function populateConnections(map, token){
    jQuery.when(
        jQuery.ajax({
          url: "/api/route/map",
          headers: { "Authorization": "Bearer " + token }
        }),
        jQuery.ajax({
          url: "/api/visit/route",
          headers: { "Authorization": "Bearer " + token }
        })
    )
    .done(function(routes, visits){
        var vectorLineLayer = new ol.layer.Vector({'id':'lines'});
        var vectorLinksLineLayer = new ol.layer.Vector({'id':'links'});
        var vectorMetroLineLayer = new ol.layer.Vector({'id':'metro'});

        var vectorLine = new ol.source.Vector({});
        var vectorLinksLine = new ol.source.Vector({});
        var vectorMetroLine = new ol.source.Vector({});


        routes[0].forEach(function (connection) {

           const visited = findRouteVisit(connection, visits[0]);
           addRoute(connection, visited, vectorLine, vectorLineLayer, vectorLinksLine, vectorLinksLineLayer, vectorMetroLine, vectorMetroLineLayer);
        });

        map.addLayer(vectorLineLayer);
        map.addLayer(vectorLinksLineLayer);
        map.addLayer(vectorMetroLineLayer);

        vectorLinksLineLayer.setVisible(false);

    });
}

function addRoute(connection, visited, vectorLine, vectorLineLayer, vectorLinksLine, vectorLinksLineLayer, vectorMetroLine, vectorMetroLineLayer){
   connection.visited = visited;

   if(connection.srsCode == "Link"){
       addConnection(connection, vectorLinksLine, vectorLinksLineLayer, findTocData(connection.toc).colour);
   }
   else if(connection.type==="Light Rail"
        || connection.type === "Underground"
        || connection.type==="Metro"
        || connection.type==="Tram"){
            addConnection(connection, vectorMetroLine, vectorMetroLineLayer, findTocData(connection.toc).colour);
   }
   else{
       addConnection(connection, vectorLine, vectorLineLayer, findTocData(connection.toc).colour);
   }
}

function findRouteVisit(route, visits){
    const key = "from:" + route.from.id + "-to:" + route.to.id
    return visits.includes(key);
}


 function addConnection(connection, line, layer, colour){
    var points = [
        [connection.from.lon, connection.from.lat],
        [connection.to.lon, connection.to.lat]];

    for (var i = 0; i < points.length; i++) {
        points[i] = ol.proj.transform(points[i], 'EPSG:4326', 'EPSG:3857');
    }

    var featureLine = new ol.Feature({
        geometry: new ol.geom.LineString(points),
        type: "Route",
        from: connection.from.id,
        to: connection.to.id,
        name: connection.from.name + " - " + connection.to.name + ' (' + connection.from.id + ' - ' + connection.to.id + ')',
        visited: connection.visited,
        operator: connection.toc,

    });

    let dash = connection.visited ? [1,0]: [10, 10];
    featureLine.setStyle(new ol.style.Style({
        stroke: new ol.style.Stroke({
            color: colour,
            width: 3,
            lineDash: dash
        })
    }));

    line.addFeature(featureLine);
    layer.setSource(line);
}

function addSingleConnection(connection){
    var vectorLineLayer = new ol.layer.Vector({'id':'lines'});
    var vectorLine = new ol.source.Vector({});

    var points = [
            [connection.from.lon, connection.from.lat],
            [connection.to.lon, connection.to.lat]];

    for (var i = 0; i < points.length; i++) {
        points[i] = ol.proj.transform(points[i], 'EPSG:4326', 'EPSG:3857');
    }

    var featureLine = new ol.Feature({
        geometry: new ol.geom.LineString(points)

    });
    const colour = findSrsData(connection.srs).colour;

    featureLine.setStyle(new ol.style.Style({
        stroke: new ol.style.Stroke({
            color: colour,
            width: 10,
        })
    }));

    vectorLine.addFeature(featureLine);
    vectorLineLayer.setSource(vectorLine);
    map.addLayer(vectorLineLayer);
}


/**
* Info Box
*/
function registerMapHandler(map){
    map.on('click', function (evt) {
        var feature = map.forEachFeatureAtPixel(evt.pixel,
            function (feature) {
                return feature;
            });

        if (feature) {
            let content = document.getElementById("content-placeholder");

            jQuery(content).empty();
            console.log(feature);
            console.log(feature.get("visited"));

            let name = feature.get('name');
            let id = feature.get("id");
            let type = feature.get('type');
            let visited = feature.get('visited') ? "Visited" : "Not yet visited";
            let operator = feature.get('operator');
            let from = feature.get('from') || "";
            let to = feature.get('to') || "";

            var url = '';
            if(type==='Route'){
                url="/route/detail/" + feature.get('from') + "/" +  feature.get('to');
            }
            else if(type==='Location'){
                url="/location/detail/" +  feature.get('id');
            }

            jQuery(content).append(infoBox(name, type, visited, operator, id, url, from, to, feature));
        }
    });
}


function infoBox(info, type, data, toc, id, url, from, to, feature){
    var infoTemplate = document.getElementsByTagName("template")[0];
    var infoElement = infoTemplate.content.querySelector("div");
    var renderedInfoElement = document.importNode(infoElement, true);

    renderedInfoElement.querySelector("slot[name=info]").textContent = info;
    renderedInfoElement.querySelector("slot[name=type]").textContent = type;
    renderedInfoElement.querySelector("slot[name=data]").textContent = data;
    renderedInfoElement.querySelector("#visit-form #location").value = id;
    renderedInfoElement.querySelector("a").setAttribute("href", url);


    let tocData = findTocData(toc);
    renderedInfoElement.querySelector("slot[name=tocBadge]").textContent = tocData.name;
    renderedInfoElement.querySelector("#tocBadge").style.backgroundColor = tocData["colour"];
    renderedInfoElement.querySelector("#tocBadge").style.color = tocData["textColour"];

    renderedInfoElement.querySelector("#visit").onclick = function() {
        if(type === 'Route') {
            jQuery.ajax({
              type: "POST",
              url: '/api/visit/route',
              data: {
                'csrfToken': renderedInfoElement.querySelector('#visit-form').children["visit-form"][0].value,
                'Authorization': renderedInfoElement.querySelector('#visit-form #Authorization').value,
                'from': from,
                'to': to
              },
              success: function() {
                let btn = renderedInfoElement.querySelector("#visit");
                btn.classList.remove('btn-primary');
                btn.classList.add('btn-success');
                renderedInfoElement.querySelector("#data").textContent = "Visited";

                feature.setStyle(new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: feature.getStyle().getStroke().color_,
                        width: 3,
                    })
                }));
              }
            });
        }
        else if(type === 'Location'){
            jQuery.ajax({
              type: "POST",
              url: '/api/visit/location',
              data: {
                'csrfToken': renderedInfoElement.querySelector('#visit-form').children["visit-form"][0].value,
                'Authorization': renderedInfoElement.querySelector('#visit-form #Authorization').value,
                'location': id
              },
              success: function() {
               let btn = renderedInfoElement.querySelector("#visit");
               btn.classList.remove('btn-primary');
               btn.classList.add('btn-success');
               renderedInfoElement.querySelector("#data").textContent = "Visited";

               let icon = '/assets/images/dot-visited.png';

               feature.setStyle(new ol.style.Style({
                   image: new ol.style.Icon(/** @type {olx.style.IconOptions} */({
                       color: feature.getStyle().image_.color_,
                       crossOrigin: 'anonymous',
                       src: icon
                   })),
               }));
              }
            });
        }
    }

    return renderedInfoElement;
}
/**
* Helper functions
*/
 function makeLayer(visibility) {
    return new ol.layer.Vector({
        style: function (feature) {
            return feature.get('style');
        },
        source: new ol.source.Vector(),
        visible: visibility
    });
}