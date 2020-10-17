package models.route

case class Route(
                from: RoutePoint,
                to: RoutePoint,
                toc: String,
                singleTrack: String,
                electrification: String,
                speed: String,
                srsCode: String,
                `type`: String,
                distance: Long = 0
                )

case class RoutePoint(
                     lat: Double,
                     lon: Double,
                     id: String,
                     name: String,
                     `type`: String
                     )











