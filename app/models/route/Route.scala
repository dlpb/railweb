package models.route


case class Route(
                from: RoutePoint,
                to: RoutePoint,
                toc: String,
                srsCode: String,
                `type`: String,
                distance: Long = 0,
                travelTimeInSeconds: Long
                )

case class RoutePoint(
                     lat: Double,
                     lon: Double,
                     id: String,
                     name: String
                     )


case class RouteDetail(
  from: DetailedRoutePoint,
  to: DetailedRoutePoint,
  singleTrack: String,
  electrification: String,
  `type`: String,
  speed: String
                      )

case class DetailedRoutePoint(
  lat: Double,
  lon: Double,
  id: String,
  name: String,
  `type`: String
                             )







